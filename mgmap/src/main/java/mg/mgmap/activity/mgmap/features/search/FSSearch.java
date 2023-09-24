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

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.RelativeLayout;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;

import java.lang.invoke.MethodHandles;

import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.activity.mgmap.FeatureService;
import mg.mgmap.R;
import mg.mgmap.activity.mgmap.view.ControlMVLayer;
import mg.mgmap.generic.model.BBox;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.model.WriteablePointModel;
import mg.mgmap.generic.util.FullscreenUtil;
import mg.mgmap.generic.util.KeyboardUtil;
import mg.mgmap.generic.util.Observer;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.view.DialogView;
import mg.mgmap.generic.view.ExtendedTextView;

public class FSSearch extends FeatureService {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private static final long T_HIDE_KEYBOARD = 10000; // in ms => 10s
    private static final long NO_POS = PointModelUtil.NO_POS; // default invalid position for prefShowPos

    private final SearchView searchView;
    private SearchProvider searchProvider = null;
    private SearchControlLayer scl = null; // feature control layer to manage feature specific events

    private final Pref<Boolean> prefSearchOn = getPref(R.string.FSSearch_qc_searchOn, false);
    private final Pref<String> prefSearchProvider = getPref(R.string.preference_choose_search_key, "Graphhopper");
    private final Pref<Boolean> prefShowSearchResult = getPref(R.string.FSSearch_qc_showSearchResult, false);
    private final Pref<Boolean> prefShowSearchResultEnabled = new Pref<>(false);
    private final Pref<Long> prefShowPos = getPref(R.string.FSSearch_pref_SearchPos, NO_POS);
    private final Pref<Boolean> prefPosBasedSearch = getPref(R.string.FSSearch_pref_PosBasedSearch, true);
    private final Pref<Boolean> prefSearchResultDetails = getPref(R.string.FSSearch_pref_SearchDetails_key, false);

    public FSSearch(MGMapActivity mmActivity) {
        super(mmActivity);
        prefSearchOn.setValue(false);
        setSearchProvider();

        searchView = new SearchView(mmActivity.getApplicationContext(), this);
        searchView.init(mmActivity);
        searchView.setPosBasedSearchIcon(prefPosBasedSearch.getValue());
        RelativeLayout mainView = mmActivity.findViewById(R.id.mainView);
        mainView.addView(searchView);
        getControlView().variableVerticalOffsetViews.add(searchView);

        EditText searchText = searchView.searchText;
        searchText.setSelectAllOnFocus(true);
        searchText.setOnEditorActionListener((tv, actionId, event) -> {
            doSearch(tv.getText().toString().trim(), actionId);
            FullscreenUtil.enforceState(getActivity());
            return true;
        });
        searchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                doSearch(searchText.getText().toString().trim(), -1);
            }
        });
        searchText.setOnClickListener(v -> triggerTTHideKeyboard());

        prefPosBasedSearch.addObserver(e -> {
            searchView.setPosBasedSearchIcon(prefPosBasedSearch.getValue());
            if (prefSearchOn.getValue()){
                doSearch(searchText.getText().toString().trim(), -1);
            }
        });

        Observer showPositionObserver = (e) -> {
            unregisterAll();
            if (prefShowSearchResult.getValue()){
                showSearchPos();
            }
        };
        prefShowSearchResult.addObserver(showPositionObserver);
        prefShowPos.addObserver(showPositionObserver);

        prefShowPos.addObserver((e) -> prefShowSearchResultEnabled.setValue(prefShowPos.getValue() != NO_POS));

        if (getPref(R.string.MGMapApplication_pref_Restart, true).getValue()){
            prefShowSearchResult.setValue(false);
            prefShowPos.setValue(NO_POS);
        }
        prefSearchOn.addObserver(refreshObserver);
        prefShowPos.onChange();
        prefShowSearchResult.onChange();
    }

    @Override
    public ExtendedTextView initQuickControl(ExtendedTextView etv, String info){
        super.initQuickControl(etv,info);
        if ("group_search".equals(info)){
            etv.setData(prefSearchOn, prefShowSearchResult, R.drawable.group_search1,R.drawable.group_search2,R.drawable.group_search3,R.drawable.group_search4);
            etv.setPrAction(new Pref<>(false));
        } else if ("search".equals(info)){
            etv.setData(prefSearchOn,R.drawable.search1b,R.drawable.search);
            etv.setPrAction(prefSearchOn);
            etv.setHelp(r(R.string.FSRecording_qcSearch_help)).setHelp(r(R.string.FSRecording_qcSearch_help1),r(R.string.FSRecording_qcSearch_help2));
        } else if ("searchRes".equals(info)){
            etv.setData(prefShowSearchResult,R.drawable.search_res2,R.drawable.search_res1);
            etv.setPrAction(prefShowSearchResult);
            etv.setDisabledData(prefShowSearchResultEnabled, R.drawable.search_res3);
            etv.setHelp(r(R.string.FSRecording_qcSearchRes_help)).setHelp(r(R.string.FSRecording_qcSearchRes_help1),r(R.string.FSRecording_qcSearchRes_help2));
        } else if ("posBasedSearch".equals(info)){
            etv.setData(prefPosBasedSearch,R.drawable.search_pos2,R.drawable.search_pos1);
            etv.setPrAction(prefPosBasedSearch);
            etv.setHelp(r(R.string.FSRecording_qcPosBasedSearch_help)).setHelp(r(R.string.FSRecording_qcPosBasedSearch_help1),r(R.string.FSRecording_qcPosBasedSearch_help2));
        }
        return etv;
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshObserver.onChange();
    }

    @Override
    protected void onPause() {
        super.onPause();
        getTimer().removeCallbacks(ttHideKeyboard);
    }

    @Override
    protected void doRefreshResumedUI() {
        boolean visibility = prefSearchOn.getValue();
        changeVisibility(visibility);
        getControlView().setDashboardVisibility(!visibility);
    }

    private void changeVisibility(boolean targetVisibility){
        if (targetVisibility){
            if (searchView.getVisibility() == View.INVISIBLE){
                setSearchProvider();
                searchView.setVisibility( View.VISIBLE );
                searchView.setFocusable(true);
                searchView.requestFocus();
                showKeyboard();
                scl = new SearchControlLayer();
                register(scl);
                triggerTTHideKeyboard();
            }
        } else {
            if (searchView.getVisibility() == View.VISIBLE){
                hideKeyboard();
                searchView.setVisibility( View.INVISIBLE );
                searchView.setFocusable(false);
                searchView.resetSearchResults();
                unregisterAllControl();
            }
        }
    }


    public class SearchControlLayer extends ControlMVLayer<Object> {

        @Override
        protected boolean onTap(WriteablePointModel point) {
            return false;
        }

        @Override
        public boolean onLongPress(LatLong tapLatLong, Point layerXY, Point tapXY) {
            PointModel pos = new PointModelImpl(tapLatLong);
            mgLog.i("rev Geocode: pos="+pos);
            long timestamp = System.currentTimeMillis();
            searchProvider.doSearch(new SearchRequest("",0,timestamp, pos, getActivity().getMapViewUtility().getZoomLevel() ));
            hideKeyboard();
            return true;
        }
    }


    private final Runnable ttHideKeyboard = this::hideKeyboard;
    private void triggerTTHideKeyboard(){
        getTimer().removeCallbacks(ttHideKeyboard);
        getTimer().postDelayed(ttHideKeyboard, T_HIDE_KEYBOARD);
    }

    private void setSearchProvider(){
        try {
            this.searchProvider = (SearchProvider) Class.forName("mg.mgmap.activity.mgmap.features.search.provider."+prefSearchProvider.getValue()).newInstance();
        } catch (Exception e) {
            mgLog.e(e);
        }
        searchProvider.init( getActivity(),this, searchView, getSharedPreferences());
    }

    private void doSearch(String text, int actionId){
        long timestamp = System.currentTimeMillis();
        triggerTTHideKeyboard();
        mgLog.i("text="+text+" actionId="+actionId+" timestamp="+timestamp);
        PointModel pos = new PointModelImpl( getActivity().getMapViewUtility().getCenter() );
        searchProvider.doSearch(new SearchRequest(text, actionId, timestamp, pos, getActivity().getMapViewUtility().getZoomLevel() ));
        if (actionId == EditorInfo.IME_ACTION_GO
                || actionId == EditorInfo.IME_ACTION_DONE
                || actionId == EditorInfo.IME_ACTION_NEXT
                || actionId == EditorInfo.IME_ACTION_SEND
                || actionId == EditorInfo.IME_ACTION_SEARCH ) {
            hideKeyboard();
        }
    }

    void showSearchPos(){
        long lalo = prefShowPos.getValue();
        if ((lalo != 0) && (lalo != NO_POS)){
            PointModel pos = PointModelImpl.createFromLaLo(lalo);
            mgLog.i(pos);
            getMapViewUtility().setMapViewPosition(pos);
            register(new PointViewSearch(pos).setRadius(10));
            register(new PointViewSearch(pos).setRadius(1));
        }
    }

    private void hideKeyboard(){
        KeyboardUtil.hideKeyboard(searchView.searchText);
    }

    private void showKeyboard(){
        KeyboardUtil.showKeyboard(searchView.searchText);
    }

    public void setSearchResult(PointModel pmSearchResult) {
        if (activity.getMapDataStoreUtil().getMapDataStore(new BBox().extend(pmSearchResult)) == null){
            mgLog.w("outside of map: "+pmSearchResult);
//            Toast.makeText(getActivity(),"Search result outside map",Toast.LENGTH_LONG).show();
            DialogView dialogView = activity.findViewById(R.id.dialog_parent);
            dialogView.lock(() -> dialogView
                    .setTitle("Warning")
                    .setMessage("Search result outside mapsforge map")
                    .setLogPrefix("Search")
                    .setPositive("Locate anyway", evt -> setSearchResult2(pmSearchResult))
                    .setNegative("Cancel",null)
                    .show());
        } else {
            setSearchResult2(pmSearchResult);
        }
    }
    public void setSearchResult2(PointModel pmSearchResult) {
        mgLog.i(pmSearchResult);
        prefShowPos.setValue(pmSearchResult.getLaLo());
        if (prefShowSearchResult.getValue()){
            prefShowSearchResult.onChange(); // even if result is already shown, this triggers to center the result
        } else {
            prefShowSearchResult.setValue(true);
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isPosBasedSearch(){
        return prefPosBasedSearch.getValue();
    }
    public boolean showSearchDeatils(){
        return prefSearchResultDetails.getValue();
    }

}
