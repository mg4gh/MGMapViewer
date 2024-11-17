/*
 * Copyright 2017 - 2022 mg4gh
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package mg.mgmap.activity.filemgr;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.ControlView;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.application.util.PersistenceManager;
import mg.mgmap.generic.util.FullscreenUtil;
import mg.mgmap.generic.util.KeyboardUtil;
import mg.mgmap.generic.util.Observer;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.PrefCache;
import mg.mgmap.generic.util.basic.IOUtil;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.util.hints.HintShareReceived;
import mg.mgmap.generic.view.DialogView;
import mg.mgmap.generic.view.ExtendedTextView;
import mg.mgmap.generic.view.VUtil;


public class FileManagerActivity extends AppCompatActivity {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private Context context = null;
    private MGMapApplication application = null;
    private PersistenceManager persistenceManager = null;

    private FileManagerEntryAdapter fileManagerEntryAdapter = null;
    PrefCache prefCache = null;
    private final ArrayList<FileManagerEntryModel> allEntries = new ArrayList<>();

    private Pref<String> prefPwd;


    private Pref<Boolean> prefFullscreen;
    private final Pref<Boolean> prefSelectAllEnabled = new Pref<>(Boolean.TRUE);
    private final Pref<Boolean> prefSelectNoneEnabled = new Pref<>(Boolean.TRUE);
    private final Pref<Boolean> prefEditEnabled = new Pref<>(Boolean.TRUE);
    private final Pref<Boolean> prefOpenEnabled = new Pref<>(Boolean.TRUE);
    private final Pref<Boolean> prefShareEnabled = new Pref<>(Boolean.TRUE);
    private final Pref<Boolean> prefSaveEnabled = new Pref<>(Boolean.FALSE); // use for save shared files
    private final Pref<Boolean> prefDeleteEnabled = new Pref<>(Boolean.TRUE);
    private final Pref<Boolean> prefBackEnabled = new Pref<>(Boolean.TRUE);

    private final ArrayList<Uri> shareUris = new ArrayList<>();



    public MGMapApplication getMGMapApplication() {
        return application;
    }

    final Handler timer = new Handler();
    final Runnable ttReworkState = () -> FileManagerActivity.this.runOnUiThread(this::reworkState);

    final Observer reworkObserver = (e) -> {
        timer.removeCallbacks(ttReworkState);
        timer.postDelayed(ttReworkState,30);
    };

    @SuppressLint({"SourceLockedOrientationActivity", "NotifyDataSetChanged"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mgLog.d();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setContentView(R.layout.file_mgr_activity);
        application = (MGMapApplication)getApplication();
        context = this;
        persistenceManager = application.getPersistenceManager();

        prefCache = new PrefCache(context);
        File fAppPwd = persistenceManager.getAppDir();
//        String basePwd = persistenceManager.getBaseDir().getAbsolutePath();
        prefPwd = prefCache.get(R.string.MGMapApplication_pref_Pwd, fAppPwd.getAbsolutePath());
        prefPwd.addObserver((Observer) evt -> {
            File fPwd = new File(prefPwd.getValue());
            if (!fPwd.exists()){
                fPwd = fAppPwd;
            }
            LinearLayout headline = findViewById(R.id.fileMgrHeadline);
            HorizontalScrollView headScroll = findViewById(R.id.fileMgrHeadScroll);

            allEntries.clear();
            headline.removeAllViews();

            File dir = fPwd;
            while ((dir!=null) && !dir.equals(fAppPwd.getParentFile())){
                final File fDir = dir;
                ExtendedTextView etv = VUtil.createControlETV(headline);
                etv.setId(View.generateViewId());
                etv.setText(dir.getName());
                etv.setOnClickListener(v -> prefPwd.setValue(fDir.getAbsolutePath()));
                dir = dir.getParentFile();
            }
            headScroll.post(() -> headScroll.fullScroll(HorizontalScrollView.FOCUS_RIGHT));

            File[] fEntries = fPwd.listFiles();
            if (fEntries != null){
                Arrays.sort(fEntries);
                ArrayList<File> dirs = new ArrayList<>();
                ArrayList<File> files = new ArrayList<>();
                for (File f : fEntries) {
                    if (f.isDirectory()){
                        dirs.add(f);
                    } else {
                        files.add(f);
                    }
                }
                dirs.addAll(files);
                for (File f : dirs){
                    FileManagerEntryModel fileManagerEntryModel = new FileManagerEntryModel();
                    fileManagerEntryModel.setFile(f);
                    fileManagerEntryModel.getSelected().setValue(false);
                    allEntries.add(fileManagerEntryModel);
                }
            }
            fileManagerEntryAdapter.notifyDataSetChanged();
            reworkObserver.propertyChange(null);
        });


        prefFullscreen = prefCache.get(R.string.FSControl_qcFullscreenOn, true);
        prefFullscreen.addObserver((e) -> FullscreenUtil.enforceState(FileManagerActivity.this, prefFullscreen.getValue()));

        RecyclerView recyclerView = findViewById(R.id.fileMgrEntries);
        fileManagerEntryAdapter = new FileManagerEntryAdapter(this, allEntries);
        recyclerView.setAdapter(fileManagerEntryAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setPadding(0,0,0, ControlView.dp(40));

        LinearLayout headUp = findViewById(R.id.fileMgrHeadUp);

        VUtil.createQuickControlETV(headUp, true)
                .setData(R.drawable.file_mgr_up)
                .setNameAndId(R.id.fileMgr_up)
                .setOnClickListener(v -> {
                    File fPwd = new File(prefPwd.getValue());
                    if (!fAppPwd.equals(fPwd)){
                        prefPwd.setValue(Objects.requireNonNull(fPwd.getParentFile()).getAbsolutePath());
                    }
                });



        ViewGroup qcs = findViewById(R.id.ts_qc);
        VUtil.createQuickControlETV(qcs,true)
                .setData(R.drawable.file_mgr_dir)
                .setNameAndId(R.id.fileMgr_mi_dir_add)
                .setOnClickListener(createAddDirOCL());
        VUtil.createQuickControlETV(qcs,true)
                .setData(R.drawable.file_mgr_file)
                .setNameAndId(R.id.fileMgr_mi_file_add)
                .setOnClickListener(createAddFileOCL());

        VUtil.createQuickControlETV(qcs,true)
                .setData(prefEditEnabled,R.drawable.edit,R.drawable.edit2)
                .setNameAndId(R.id.fileMgr_mi_edit)
                .setOnClickListener(createEditOCL());

        VUtil.createQuickControlETV(qcs,true)
                .setData(prefOpenEnabled,R.drawable.show2,R.drawable.show)
                .setNameAndId(R.id.fileMgr_mi_show)
                .setOnClickListener(createOpenOCL());

        VUtil.createQuickControlETV(qcs,true)
                .setData(prefShareEnabled,R.drawable.share2,R.drawable.share)
                .setNameAndId(R.id.stat_mi_share)
                .setOnClickListener(createShareOCL());
        VUtil.createQuickControlETV(qcs,true)
                .setData(prefSaveEnabled,R.drawable.save2,R.drawable.save)
                .setNameAndId(R.id.stat_mi_save)
                .setOnClickListener(createSaveOCL());
        VUtil.createQuickControlETV(qcs,true)
                .setData(prefDeleteEnabled,R.drawable.delete2,R.drawable.delete)
                .setNameAndId(R.id.stat_mi_delete)
                .setOnClickListener(createDeleteOCL());
        VUtil.createQuickControlETV(qcs,true)
                .setData(R.drawable.back)
                .setNameAndId(R.id.stat_mi_back)
                .setOnClickListener(createBackOCL());

        if (!getIntent().toString().contains(" act=android.intent.action.MAIN cat=[android.intent.category.LAUNCHER] ")){
            onNewIntent(getIntent());
        }

    }

    @SuppressLint("Range")
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mgLog.i(intent);
        if (intent != null){
            mgLog.d("intent action="+intent.getAction());
            mgLog.d("intent categories="+intent.getCategories());
            mgLog.d("intent scheme="+intent.getScheme());
            mgLog.d("intent data="+intent.getData());
            mgLog.d("intent type="+intent.getType());
            if (intent.getData()!=null)
                mgLog.d("intent data.path="+intent.getData().getPath());
            if (intent.getData()!=null)
                mgLog.d("intent data.host="+intent.getData().getHost());
            mgLog.d("intent flags="+ Integer.toHexString( intent.getFlags() ));


            shareUris.clear();
            if (Intent.ACTION_SEND.equals(intent.getAction()) ) {
                Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (uri != null) {
                    shareUris.add(uri);
                }
            }
            if (Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction()) ) {
                ArrayList<Uri> intentUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                if (intentUris != null) {
                    shareUris.addAll(intentUris);
                }
            }
            if (!shareUris.isEmpty()){
                application.getHintUtil().showHint( new HintShareReceived(this, shareUris.size()) );
            }

        }
    }



    @Override
    protected void onResume() {
        super.onResume();
        mgLog.d();

        prefPwd.onChange();
//        refreshAllEntries();
        prefFullscreen.onChange();
        reworkObserver.propertyChange(null);
    }

    @Override
    protected void onPause() {
        mgLog.d();
        allEntries.clear();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        ViewGroup qcs = findViewById(R.id.ts_qc);
        qcs.removeAllViews();
        prefCache.cleanup();
        FileManagerEntryView.cleanup();
    }


    void bind(FileManagerEntryView view, FileManagerEntryModel model){
        view.getLlRight().setOnLongClickListener(v -> {
            model.getSelected().toggle();
            reworkObserver.propertyChange(null);
            return true;
        });
        view.getEtvSelected().setOnLongClickListener(v -> {
            model.getSelected().toggle();
            reworkObserver.propertyChange(null);
            return true;
        });
        view.getLlRight().setOnClickListener(v -> {
            if (!getSelectedEntries(null).isEmpty()){
                model.getSelected().toggle();
            } else if (model.getFile().isDirectory()){
                prefPwd.setValue(model.getFile().getAbsolutePath());
            } else {
                openFile(model.getFile());
            }
            reworkObserver.propertyChange(null);
        });
        view.getEtvSelected().setOnClickListener(v -> {
            model.getSelected().toggle();
            reworkObserver.propertyChange(null);
        });
    }



    private void reworkState(){
        boolean[] flags = new boolean[8];
        ArrayList<FileManagerEntryModel> selectedEntries = getSelectedEntries(flags);

        prefSelectAllEnabled.setValue(selectedEntries.size() < allEntries.size());
        prefSelectNoneEnabled.setValue(!selectedEntries.isEmpty());
        prefEditEnabled.setValue(selectedEntries.size() == 1);
        prefOpenEnabled.setValue((selectedEntries.size() == 1) && (flags[0]));
        prefShareEnabled.setValue(!selectedEntries.isEmpty() && (flags[0]));
        prefSaveEnabled.setValue(!shareUris.isEmpty());
        prefDeleteEnabled.setValue((!selectedEntries.isEmpty()) && (!flags[2]));
        prefBackEnabled.setValue(true);
    }

    /**
     * @param flags
     *          0 - true, if all files
     *          1 - true, if all directories
     *          2 - true, contains at least one not empty subdirectory
     * @return selected entries
     */
    ArrayList<FileManagerEntryModel> getSelectedEntries(boolean[] flags){
        if (flags != null){
            flags[0] = true;
            flags[1] = true;
            flags[2] = false;
        }
        ArrayList<FileManagerEntryModel> list = new ArrayList<>();
        for (FileManagerEntryModel fileManagerEntryModel : allEntries){
            if (fileManagerEntryModel.isSelected()){
                list.add(fileManagerEntryModel);
                if (flags != null){
                    if (fileManagerEntryModel.getFile().isDirectory()){
                        flags[0] = false;
                        try (Stream<Path> subDirEntries = Files.list(fileManagerEntryModel.getFile().toPath())){
                            flags[2] |= subDirEntries.iterator().hasNext();
                        } catch (IOException ioException){
                            mgLog.e(ioException);
                        }
                    } else {
                        flags[1] = false;
                    }
                }
            }
        }
        return list;
    }

    private View.OnClickListener createAddDirOCL(){
        return v -> editFile(new File(prefPwd.getValue()), null, true);
    }

    private View.OnClickListener createAddFileOCL(){
        return v -> editFile(new File(prefPwd.getValue()), null, false);
    }

    private View.OnClickListener createEditOCL(){
        return v -> {
            if (prefEditEnabled.getValue()){
                final ArrayList<FileManagerEntryModel> entries = getSelectedEntries(null);
                if (entries.size() == 1){
                    File oldFile = entries.get(0).getFile();
                    editFile(oldFile.getParentFile(), oldFile, false);
                }
            }
        };
    }

    private void editFile(File parent, File oldFile, boolean bDir){

        final EditText etFileName = new EditText(this);
        etFileName.setText((oldFile==null)?"":oldFile.getName());
        etFileName.setSelectAllOnFocus(true);
        InputFilter filter = (source, start, end, dest, dStart, dEnd) -> {
            for (int i = start; i < end; i++) {
                if ("\\?%*:|\"<>,;=\n".indexOf(source.charAt(i)) >= 0){
//                                etFileName.setError("Not allowed characters: /\\?%*:|\"<>.,;=<LF>");
                    return "";
                }
            }
            return null;
        };
        etFileName.setFilters(new InputFilter[] { filter });

        DialogView dialogView = this.findViewById(R.id.dialog_parent);
        dialogView.lock(() -> dialogView
                .setTitle((oldFile==null)?("Create new "+(bDir?"directory":"file")):"Rename File")
                .setMessage((oldFile==null)?"":"Old name: "+oldFile.getName())
                .setContentView(etFileName)
                .setPositive("OK", evt -> {
                    String newName = etFileName.getText().toString();
                    mgLog.i(getTitle()+" oldFile="+((oldFile==null)?"null":("\""+oldFile.getName()+"\"")+ "newName=\""+newName+"\" bDir="+bDir));
                    if ((oldFile==null) || !newName.equals(oldFile.getName())){
                        File fNewFile = new File(parent, newName);
                        if (fNewFile.exists()){
                            Toast.makeText(context, "Action failed, name already exists: "+newName, Toast.LENGTH_LONG).show();
                        } else{
                            boolean bRes;
                            if (oldFile != null){
                                bRes = oldFile.renameTo(fNewFile);
                                mgLog.d("rename result: "+bRes);
                            } else if (bDir){
                                application.getPersistenceManager().createIfNotExists(parent, newName);
                            } else {
                                application.getPersistenceManager().createFileIfNotExists(parent, newName);
                            }
                            prefPwd.changed();
                        }
                    }
                })
                .setNegative("Cancel", null)
                .show());
    }

    private View.OnClickListener createOpenOCL(){
        return v -> {
            ArrayList<FileManagerEntryModel> models = getSelectedEntries(null);
            if (models.size() == 1){
                File file = models.get(0).getFile();
                if (!file.isDirectory()){
                    openFile(file);
                }
            }
        };
    }

    @SuppressLint("ClickableViewAccessibility")
    private void openFile(File file){
        Intent intent = new Intent(Intent.ACTION_VIEW );
        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
//                    intent.setDataAndType(uri,"*/*");
        if (file.getName().endsWith(".gpx")){
            intent.setDataAndType(uri,"text/gpx");
        } else if (file.getName().endsWith(".xml")){
            intent.setDataAndType(uri,"text/xml");
        } else if (file.getName().endsWith(".json")){
            intent.setDataAndType(uri,"text/json");
        } else if (file.getName().endsWith(".properties")){
            intent.setDataAndType(uri,"text/properties");
        } else if (file.getName().endsWith(".txt")){
            intent.setDataAndType(uri,"text/txt");
        } else if (file.getName().endsWith(".cfg")){
            intent.setDataAndType(uri,"text/cfg");
        } else {
            intent.setData(uri);
        }
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
        );

        boolean tinyEditorUsable = ((intent.getType()!=null) && (intent.getType().startsWith("text")) && (file.length() < 10000));
        boolean tryTinyEditor = tinyEditorUsable && prefCache.get(R.string.preferences_fileMgr_preferIntEditor_key, true).getValue();

        if (!tryTinyEditor){
            mgLog.d("intent action="+intent.getAction());
            mgLog.d("intent categories="+intent.getCategories());
            mgLog.d("intent scheme="+intent.getScheme());
            mgLog.d("intent data="+intent.getData());
            mgLog.d("intent type="+intent.getType());
            mgLog.d("intent flags="+ Integer.toHexString( intent.getFlags() ));
            mgLog.i(intent);
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                mgLog.e(e.getMessage());
                tryTinyEditor = tinyEditorUsable;
            }
        }

        if (tryTinyEditor){
            try (FileInputStream fis = new FileInputStream(file)){
                byte[] buf = new byte[fis.available()];
                int length = fis.read(buf);
                String oldString = new String(buf,0,length);
                String editTitle = "MGMapViewer tiny editor";
                String editMessage = "Edit "+file.getName();

                mgLog.i("editTitle="+editTitle+" oldString="+oldString);
                final EditText etContent = new EditText(this);
                etContent.setGravity(Gravity.TOP | Gravity.START);
                etContent.setOnClickListener(v -> {
                    mgLog.i("etContent clicked");
                    etContent.setFocusable(true);
                    etContent.requestFocus();
                    KeyboardUtil.showKeyboard(etContent);
                });
                etContent.setBackgroundColor(0x00000000);
                etContent.setInputType(etContent.getInputType() | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

                final HorizontalScrollView hsv = new HorizontalScrollView(this);
                hsv.addView(etContent);
                hsv.setMinimumHeight(VUtil.dp(250));
                final ScrollView svEditText = new ScrollView(this);
                svEditText.addView(hsv);
                LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, VUtil.dp(250));
                svEditText.setLayoutParams(llParams);
                svEditText.setBackgroundColor(0xFFFFFFFF);
                etContent.setText(oldString);
                etContent.requestFocus();

                View.OnTouchListener mScrollViewTouchListener = (v, event) -> {
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        etContent.performClick();
                    }
                    return false;
                };
                hsv.setOnTouchListener(mScrollViewTouchListener);
                svEditText.setOnTouchListener(mScrollViewTouchListener);

                DialogView dialogView = this.findViewById(R.id.dialog_parent);
                dialogView.lock(() -> dialogView
                        .setTitle(editTitle)
                        .setMessage(editMessage)
                        .setContentView(svEditText)
                        .setPositive("Save", evt -> {
                            String newString = etContent.getText().toString();
                            mgLog.i("editTitle="+editTitle+" newString="+newString);
                            try (FileOutputStream fos = new FileOutputStream(file)){
                                fos.write(newString.getBytes());
                            } catch (IOException e){
                                mgLog.e(e);
                            }
                        })
                        .setNegative("Cancel", null)
                        .show());

            } catch (IOException e){
                mgLog.e(e);
            }
        } // tryTinyEditor
    }

    private View.OnClickListener createShareOCL(){
        return v -> {
            if (prefShareEnabled.getValue()){
                ArrayList<FileManagerEntryModel> entries = getSelectedEntries(null);
                if (!entries.isEmpty()){
                    Intent sendIntent;
                    String title = "Share ...";
                    if (entries.size() == 1){
                        File file = entries.get(0).getFile();
                        sendIntent = new Intent(Intent.ACTION_SEND);
                        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
                        sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
                        sendIntent.setClipData(ClipData.newRawUri("", uri));
                        title = "Share "+file.getName()+" to ...";
                    } else {
                        ArrayList<Uri> uris = new ArrayList<>();
                        ClipData clipData = ClipData.newRawUri("", null);
                        for(FileManagerEntryModel model : entries){
                            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", model.getFile());
                            uris.add( uri );
                            clipData.addItem(new ClipData.Item(uri));
                        }
                        sendIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                        sendIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
                        sendIntent.setClipData(clipData);
                    }
                    sendIntent.setType("*/*");
                    sendIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(sendIntent, title));
                }
            }
        };
    }

    @SuppressLint("Range")
    private View.OnClickListener createSaveOCL(){
        return v -> {
            ContentResolver contentResolver = application.getContentResolver();
            File fDir = new File(prefPwd.getValue());
            while (fDir.isDirectory() &&  !shareUris.isEmpty()){
                Uri uri = shareUris.remove(0);
                mgLog.i("uri: " + uri);

                try {
                    try (Cursor cursor = contentResolver.query(uri, null, null, null, null)) {
                        if (cursor != null && cursor.moveToFirst()) {
                            try (InputStream is = contentResolver.openInputStream(uri)){
                                if (is != null){
                                    String filename = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                                    String size = cursor.getString(cursor.getColumnIndex(OpenableColumns.SIZE));
                                    mgLog.i("filename=" + filename + " size=" + size);
                                    File fNew = new File(fDir, filename);
                                    if (fNew.exists()){
                                        mgLog.w("File already exists: " + fNew.getAbsolutePath());
                                        Toast.makeText(this, filename+" already exists",Toast.LENGTH_SHORT).show();
                                    } else {
                                        IOUtil.copyStreams(is, Files.newOutputStream(Paths.get(prefPwd.getValue(),filename)));
                                    }
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    mgLog.e(e);
                }
            }
            if (fDir.getAbsolutePath().startsWith( application.getPersistenceManager().getTrackGpxDir().getAbsolutePath())){ // fDir equals or is subPath of gpx dir
                new Thread(() -> application.checkCreateLoadMetaData(true)).start();
            }
            prefPwd.changed();

        };
    }

    private View.OnClickListener createDeleteOCL(){
        return v -> {
            if (prefDeleteEnabled.getValue()){
                ArrayList<FileManagerEntryModel> entries = getSelectedEntries(null);
                String msg = entries.stream().map(e->(e.getFile().getName()+"\n")).collect(Collectors.joining());
//                String msg = getNames(trackLogs, false).toString();
                DialogView dialogView = this.findViewById(R.id.dialog_parent);
                dialogView.lock(() -> dialogView
                        .setTitle("Delete")
                        .setMessage(msg)
                        .setPositive("OK", evt -> {
                            mgLog.i("confirm delete for list \""+msg+"\"");
                            for(FileManagerEntryModel entry : entries){
                                boolean res = entry.getFile().delete();
                                mgLog.d("delete file "+entry.getFile().getName()+ " result: "+res);
                            }
                            prefPwd.changed();
                        })
                        .setNegative("Cancel", null)
                        .show());
            }
        };
    }

    private View.OnClickListener createBackOCL(){
        return v -> FileManagerActivity.this.onBackPressed();
    }



}

