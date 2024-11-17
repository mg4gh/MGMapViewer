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
import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.ControlView;
import mg.mgmap.generic.util.CC;
import mg.mgmap.generic.util.Observer;
import mg.mgmap.generic.util.basic.Formatter;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.view.ExtendedTextView;

@SuppressLint("ViewConstructor")
public class FileManagerEntryView extends LinearLayout {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private static final ArrayList<FileManagerEntryView> boundViews = new ArrayList<>();
    public FileManagerEntryModel fileManagerEntryModel = null;
    private final Observer selectedObserver;

    private final ExtendedTextView etvSelected;
    private final ExtendedTextView etvName;
    private final ExtendedTextView etvSize;
    private final ExtendedTextView etvTimestamp;

    private final LinearLayout llRight;
    private final LinearLayout llBottom;



    private int dp(float dp){
        return ControlView.dp(dp);
    }

    static private final Handler timer = new Handler();

    public FileManagerEntryView(Context context){
        super(context);

        {
            this.setId(View.generateViewId());
            this.setOrientation(HORIZONTAL);
            this.setPadding(0, dp(2),0,0);
            this.setLayoutParams(new LinearLayout.LayoutParams(-1,-2));
        }
        {
            etvSelected = new ExtendedTextView(context);
            LinearLayout.LayoutParams lpSelected = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT);
            lpSelected.setMargins(dp(0.8f),dp(0.8f),dp(0.8f),dp(0.8f));
            lpSelected.weight = 10;
            etvSelected.setLayoutParams(lpSelected);
            etvSelected.setPadding(dp(6.0f),dp(3.0f),dp(6.0f),dp(0.0f));
            etvSelected.setData(R.drawable.select_off,R.drawable.select_on);
            etvSelected.setDrawableSize(dp(24));
            etvSelected.setCompoundDrawablePadding(dp(6.0f));
            this.addView(etvSelected);
        }
        {
            llRight = new LinearLayout(context);
            llRight.setOrientation(VERTICAL);
            LinearLayout.LayoutParams lpRight =  new LinearLayout.LayoutParams( 0, ViewGroup.LayoutParams.WRAP_CONTENT);
            lpRight.weight = 90;
            llRight.setLayoutParams(lpRight);
            this.addView(llRight);
        }
        {
            etvName = new ExtendedTextView(context);
            LinearLayout.LayoutParams lpName = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lpName.setMargins(dp(0.8f),dp(0.8f),dp(0.8f),0);
            etvName.setLayoutParams(lpName);
            etvName.setPadding(dp(6.0f),dp(5.0f),dp(6.0f),dp(0.0f));
            etvName.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
            etvName.setFormat(Formatter.FormatType.FORMAT_STRING);
            etvName.setGravity(Gravity.START);
            etvName.setMaxLines(5);
            etvName.setTextColor(CC.getColor(R.color.CC_WHITE));
            etvName.setDrawableSize(dp(24));
            etvName.setCompoundDrawablePadding( dp(6.0f));
            llRight.addView(etvName);
        }
        {
            llBottom = new LinearLayout(context);
            llBottom.setOrientation(HORIZONTAL);
            llBottom.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
            llRight.addView(llBottom);
        }
        {
            etvSize = new ExtendedTextView(context);
            LinearLayout.LayoutParams lpSize = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
            lpSize.weight = 50;
            lpSize.setMargins(dp(0.8f),0,0,dp(0.8f));
            etvSize.setLayoutParams(lpSize);
            etvSize.setPadding(dp(36.0f),dp(0.0f),dp(6.0f),dp(3.0f));
            etvSize.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            etvSize.setFormat(Formatter.FormatType.FORMAT_FILE_SIZE);
            etvSize.setGravity(Gravity.START);
            etvSize.setLines(1);
            etvSize.setTextColor(CC.getColor(R.color.CC_WHITE));
            llBottom.addView(etvSize);
        }
        {
            etvTimestamp = new ExtendedTextView(context);
            LinearLayout.LayoutParams lpTimestamp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
            lpTimestamp.weight = 50;
            lpTimestamp.setMargins(0,0,dp(0.8f),dp(0.8f));
            etvTimestamp.setLayoutParams(lpTimestamp);
            etvTimestamp.setPadding(dp(6.0f),dp(0.0f),dp(6.0f),dp(3.0f));
            etvTimestamp.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            etvTimestamp.setFormat(Formatter.FormatType.FORMAT_FILE_TS);
            etvTimestamp.setGravity(Gravity.END);
            etvTimestamp.setLines(1);
            etvTimestamp.setTextColor(CC.getColor(R.color.CC_WHITE));
            llBottom.addView(etvTimestamp);
        }

        selectedObserver = (e) -> {
            FileManagerEntryModel model = fileManagerEntryModel; // concurrent Thread may set fileManagerEntryModel to null
            if (model != null){
                FileManagerEntryView.this.setViewtreeColor(FileManagerEntryView.this,  getColorIdForTrackLog(model));
            }
        };
     }

    public void bind(FileManagerEntryModel fileManagerEntryModel) {
        this.fileManagerEntryModel = fileManagerEntryModel;
        String name = fileManagerEntryModel.getFile().getName();
        mgLog.d("bind_data "+name);

        etvSelected.setName("SEL_"+name);
        etvName.setName("NAM_"+name);

        etvSelected.setData(fileManagerEntryModel.getSelected());
        etvName.setValue(name);
        etvName.setData(fileManagerEntryModel.getFile().isDirectory()?R.drawable.file_mgr_dir:R.drawable.file_mgr_file);

        File file = fileManagerEntryModel.getFile();
        if (file.isDirectory()){
            etvSize.setFormat(Formatter.FormatType.FORMAT_FILE_SIZE_DIR);
            String[] dirEntries = file.list();
            etvSize.setValue( (long)(dirEntries==null?0:dirEntries.length));
        } else {
            etvSize.setFormat(Formatter.FormatType.FORMAT_FILE_SIZE);
            etvSize.setValue( file.length() );
        }
        etvTimestamp.setValue( file.lastModified() );

        setViewtreeColor(FileManagerEntryView.this, getColorIdForTrackLog(fileManagerEntryModel));
        fileManagerEntryModel.getSelected().addObserver(selectedObserver);
        boundViews.add(this);
        hack();
    }


    public void unbind(){

        mgLog.i(fileManagerEntryModel.getFile().getAbsolutePath());
        clearReferences();
        boundViews.remove(this);
    }

    private void clearReferences(){
        mgLog.i("unbind_data "+fileManagerEntryModel.getFile().getAbsolutePath());
        etvSelected.cleanup();
        etvSelected.setOnClickListener(null);
        etvSelected.setOnLongClickListener(null);
        llRight.setOnClickListener(null);
        llRight.setOnLongClickListener(null);
        fileManagerEntryModel.getSelected().setValue(false);
        fileManagerEntryModel = null;
        setOnClickListener(null);     // OCL created during bind in FileManagerEntryAdapter/FileManagerActivity
        setOnLongClickListener(null); // OCL created during bind in FileManagerEntryAdapter/FileManagerActivity
    }


    private void setViewtreeColor(View view, int colorId){
        if (view instanceof TextView){
            view.setBackgroundColor(getResources().getColor( colorId, getContext().getTheme()) );
        }

        if (view instanceof ViewGroup viewGroup){
            for (int idx=0; idx<viewGroup.getChildCount(); idx++){
                setViewtreeColor(viewGroup.getChildAt(idx), colorId);
            }
        }
    }


    private int getColorIdForTrackLog(FileManagerEntryModel fileManagerEntryModel){
        return fileManagerEntryModel.isSelected()?R.color.CC_GRAY100_A150 :R.color.CC_GRAY100_A100;
    }



    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (fileManagerEntryModel != null){
            hack();
        }
    }

    // For some unknown reason the name field shows initially only one line (and only for those in the first screen).
    // This workaround triggers a refresh for long track names
    private void hack(){
        if ((fileManagerEntryModel != null) && (etvName != null)){
            if (fileManagerEntryModel.getFile().getName().length() > 26){
                mgLog.d("hack triggered for "+fileManagerEntryModel.getFile().getName());
                timer.postDelayed(() -> ((Activity)getContext()).runOnUiThread(() -> {
                    if (fileManagerEntryModel != null) {
                        mgLog.d("hack executed for "+fileManagerEntryModel.getFile().getName());
                        String suffix = etvName.getText().toString().endsWith(" ")?"":" ";
                        etvName.setValue(fileManagerEntryModel.getFile().getName() + suffix);
                    }
                }), 50);
            }
        }
    }

    /* cleanup Views that are bound  */
    public static void cleanup(){
        for (FileManagerEntryView trackStatisticView : boundViews){
            trackStatisticView.clearReferences();
        }
        boundViews.clear();
    }

    public ExtendedTextView getEtvSelected() {
        return etvSelected;
    }

    public LinearLayout getLlRight() {
        return llRight;
    }
}
