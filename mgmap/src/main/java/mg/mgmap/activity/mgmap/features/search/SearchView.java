/*
 * Copyright 2017 - 2021 mg4gh
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
package mg.mgmap.activity.mgmap.features.search;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import mg.mgmap.activity.mgmap.ControlView;
import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.R;
import mg.mgmap.generic.util.CC;
import mg.mgmap.activity.mgmap.util.ZoomOCL;
import mg.mgmap.generic.util.ExtendedClickListener;

public class SearchView extends LinearLayout {

    FSSearch fsSearch = null;
    MGMapActivity activity = null;
    final Context context;

    public SearchView(Context context, FSSearch fsSearch) {
        super(context);
        this.context = context;
        this.fsSearch = fsSearch;
    }

    EditText searchText = null;
    ImageView posBasedSearchIcon = null;
    ImageView keyboardIconView = null;
    final ArrayList<TextView> searchResults = new ArrayList<>();
    private static final int NUM_SEARCH_RESULTS = 5;

    @SuppressLint("DiscouragedApi")
    void init(MGMapActivity activity){
        this.activity = activity;
        setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setOrientation(LinearLayout.VERTICAL);
        setVisibility(INVISIBLE);

        LinearLayout searchTextLine = new LinearLayout(context);
        searchTextLine.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        searchTextLine.setOrientation(LinearLayout.HORIZONTAL);
        this.addView(searchTextLine);

        {
            posBasedSearchIcon = new ImageView(context);
            posBasedSearchIcon.setImageResource(R.drawable.search_pos_dis);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ControlView.dp(40),ControlView.dp(32));
            params.gravity = Gravity.CENTER_VERTICAL | Gravity.START;
            posBasedSearchIcon.setLayoutParams(params);
            posBasedSearchIcon.setOnClickListener(v -> fsSearch.prefPosBasedSearch.toggle());
            searchTextLine.addView(posBasedSearchIcon);
        }
        {
            searchText = new EditText(context);
            searchText.setId(R.id.search_edit_text);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.weight = 10;
            searchText.setLayoutParams(params);
            searchText.setHint("Search Text");
            searchText.setHintTextColor(CC.getColor(R.color.CC_GRAY100_A150));
            searchText.setSingleLine(true);
            searchText.setSelectAllOnFocus(true);
            searchTextLine.addView(searchText);
        }
        {
            keyboardIconView = new ImageView(context);
            keyboardIconView.setImageResource(R.drawable.keyboard1);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ControlView.dp(40),ControlView.dp(32));
            params.gravity = Gravity.CENTER_VERTICAL;
            keyboardIconView.setLayoutParams(params);
            searchTextLine.addView(keyboardIconView);
        }

        for (int i=0; i<NUM_SEARCH_RESULTS; i++){
            TextView textView = new TextView(context);
            textView.setId( this.getResources().getIdentifier("search_result"+(i+1), "id", activity.getPackageName()) );
            LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            llParams.setMargins(5,0,5,5);
            textView.setLayoutParams(llParams);
            this.addView(textView);
            textView.setMinHeight(ControlView.dp(40));
            searchResults.add(textView);

        }
        resetSearchResults();
    }

    void setPosBasedSearchIcon(boolean posBasedSearch, boolean locationBasedSearchOn){
        posBasedSearchIcon.setImageResource(locationBasedSearchOn?(posBasedSearch?R.drawable.search_pos2:R.drawable.search_pos1):R.drawable.search_pos_dis);
    }

    public SearchView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.context = context;
    }


    public void setResList(ArrayList<SearchResult>  srs){
        activity.runOnUiThread(() -> setResListUI(srs));
    }

    public void setResListUI(ArrayList<SearchResult>  srs){
        for (int i=0; i<NUM_SEARCH_RESULTS;i++){
            TextView tv = searchResults.get(i);
            if (i<srs.size()){
                SearchResult sr = srs.get(i);
                tv.setText(sr.resultText);
                tv.setTextSize(20);
                tv.setBackgroundColor(CC.getColor( R.color.CC_WHITE_A150));

                float scaleFactor = activity.getMapsforgeMapView().getModel().displayModel.getScaleFactor();
                tv.setOnClickListener( new ExtendedClickListener(){
                    private boolean showLong=true;
                    @Override
                    public void onClick(View v) {
                        doubleClickTimeout = fsSearch.showSearchDetails()?250:10;
                        super.onClick(v);
                    }
                    @Override
                    public void onSingleClick(View view) {
                        fsSearch.setSearchResult(sr.pos);
                    }
                    @Override
                    public void onDoubleClick(View view) {
                        if (fsSearch.showSearchDetails() && (sr.longResultText != null)){
                            if (showLong){
                                tv.setText(sr.longResultText);
                            } else {
                                tv.setText(sr.resultText);
                            }
                            showLong = !showLong;
                        }
                    }
                });

                tv.setOnLongClickListener( new View.OnLongClickListener(){
                    final ZoomOCL zoomOCL = new ZoomOCL(scaleFactor);
                    @Override
                    public boolean onLongClick(View v) {
                        zoomOCL.setToMillis(2500);
                        zoomOCL.onSingleClick(v);
                        return true;
                    }
                });
                tv.setVisibility(VISIBLE);
            } else {
                resetSearchResult(tv);
            }
        }

    }


    void resetSearchResults() {
        for (TextView tv : searchResults){
            resetSearchResult(tv);
        }
    }

    void resetSearchResult(TextView tv) {
        tv.setVisibility(INVISIBLE);
    }

}
