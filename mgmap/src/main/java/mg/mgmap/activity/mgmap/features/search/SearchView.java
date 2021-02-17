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

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.R;
import mg.mgmap.activity.mgmap.util.CC;
import mg.mgmap.activity.mgmap.util.ZoomOCL;

public class SearchView extends LinearLayout {

    FSSearch fsSearch = null;
    MGMapActivity activity = null;
    Context context;

    public SearchView(Context context, FSSearch fsSearch) {
        super(context);
        this.context = context;
        this.fsSearch = fsSearch;
    }

    EditText searchText = null;
    ArrayList<TextView> searchResults = new ArrayList<>();
    private static final int NUM_SEARCH_RESULTS = 5;

    void init(MGMapActivity activity){
        this.activity = activity;
        setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setOrientation(LinearLayout.VERTICAL);
        setVisibility(INVISIBLE);



        searchText = new EditText(context);
        searchText.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        searchText.setHint("Search Text");
        searchText.setHintTextColor(CC.getColor(R.color.GRAY100_A150));
        searchText.setSingleLine(true);
        searchText.setSelectAllOnFocus(true);
        this.addView(searchText);

        for (int i=0; i<NUM_SEARCH_RESULTS; i++){
            TextView textView = new TextView(context);
            LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            llParams.setMargins(5,0,5,5);
            textView.setLayoutParams(llParams);
            this.addView(textView);
            searchResults.add(textView);

        }
        resetSearchResults();
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
                tv.setBackgroundColor(CC.getColor( R.color.WHITE_A150 ));

                float scaleFactor = activity.getMapsforgeMapView().getModel().displayModel.getScaleFactor();
                ZoomOCL ocl = new ZoomOCL(scaleFactor){
                    private boolean showLong=true;
                    @Override
                    public void onDoubleClick(View view) {
                        if (sr.longResultText != null){
                            if (showLong){
                                tv.setText(sr.longResultText);
                            } else {
                                tv.setText(sr.resultText);
                            }
                            showLong = !showLong;
                        }
                    }
                };
                ocl.setToMillis(2500);
                tv.setOnClickListener(ocl);
                tv.setOnLongClickListener(v -> {
                    fsSearch.setSearchResult(sr.pos);
                    return true;
                });
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
        tv.setText("");
        tv.setTextSize(1);
        tv.setOnClickListener(null);
        tv.setBackgroundColor(CC.getColor( R.color.TRANSPARENT ));
    }

}
