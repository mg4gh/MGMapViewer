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
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.ControlView;
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
    public final ExtendedTextView etvName;


    private int dp(float dp){
        return ControlView.dp(dp);
    }

    static private final Handler timer = new Handler();

    public FileManagerEntryView(Context context){
        super(context);

        this.setId(View.generateViewId());
        this.setOrientation(VERTICAL);
        this.setPadding(0, dp(2),0,0);
        this.setLayoutParams(new LinearLayout.LayoutParams(-1,-2));

        LinearLayout llEntry = new LinearLayout(context);
        llEntry.setOrientation(HORIZONTAL);
        llEntry.setLayoutParams(new LayoutParams(-1, -2));

        this.addView(llEntry);
        etvSelected = createETV(llEntry,10).setFormat(Formatter.FormatType.FORMAT_STRING).setData(R.drawable.select_off,R.drawable.select_on);
        etvName = createETV(llEntry,90).setFormat(Formatter.FormatType.FORMAT_STRING);
        etvName.setMaxLines(5);


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
        etvName.setOnClickListener(null);
        etvName.setOnLongClickListener(null);
        fileManagerEntryModel.getSelected().setValue(false);
        fileManagerEntryModel = null;
        setOnClickListener(null);     // OCL created during bind in FileManagerEntryAdapter/FileManagerActivity
        setOnLongClickListener(null); // OCL created during bind in FileManagerEntryAdapter/FileManagerActivity
    }


    public ExtendedTextView createETV(ViewGroup viewGroup, float weight) {
        ExtendedTextView etv = new ExtendedTextView(getContext()).setDrawableSize(dp(24)); // Need activity context for Theme.AppCompat (Otherwise we get error messages)
        viewGroup.addView(etv);

        TableRow.LayoutParams params = new TableRow.LayoutParams(0, RelativeLayout.LayoutParams.MATCH_PARENT);
        int margin = dp(0.8f);
        params.setMargins(margin,margin,margin,margin);
        params.weight = weight;
        etv.setLayoutParams(params);

        int padding = dp(6.0f);
        etv.setPadding(padding, padding, padding, padding);
        int drawablePadding = dp(6.0f);
        etv.setCompoundDrawablePadding(drawablePadding);
        Drawable drawable = ResourcesCompat.getDrawable(getContext().getResources(), R.drawable.quick2, getContext().getTheme());
        if (drawable != null) drawable.setBounds(0, 0, etv.getDrawableSize(), etv.getDrawableSize());
        etv.setCompoundDrawables(null,null,null,null);
        etv.setText("");
        etv.setTextColor(getContext().getColor(R.color.CC_WHITE));
        etv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        etv.setMaxLines(1);
        return etv;
    }

    private void setViewtreeColor(View view, int colorId){
        if (view instanceof TextView){
            view.setBackgroundColor(getResources().getColor( colorId, getContext().getTheme()) );
        }

        if (view instanceof ViewGroup){
            ViewGroup viewGroup = (ViewGroup)view;
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

    public ExtendedTextView getEtvName() {
        return etvName;
    }
}
