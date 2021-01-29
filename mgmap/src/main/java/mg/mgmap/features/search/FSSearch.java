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
package mg.mgmap.features.search;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.RelativeLayout;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;

import java.util.Observer;

import mg.mgmap.MGMapActivity;
import mg.mgmap.MGMapApplication;
import mg.mgmap.FeatureService;
import mg.mgmap.R;
import mg.mgmap.features.search.provider.Nominatim;
import mg.mgmap.model.PointModel;
import mg.mgmap.model.PointModelImpl;
import mg.mgmap.model.WriteablePointModel;
import mg.mgmap.util.FullscreenUtil;
import mg.mgmap.util.NameUtil;
import mg.mgmap.util.Pref;
import mg.mgmap.util.PointModelUtil;
import mg.mgmap.view.ExtendedTextView;
import mg.mgmap.view.MVLayer;

public class FSSearch extends FeatureService {

    private static final long T_HIDE_KEYBOARD = 10000; // in ms => 10s
    private static final long NO_POS = PointModelUtil.NO_POS; // default invalid position for prefShowPos

    private final SearchView searchView;
    private SearchProvider searchProvider = null;
    private SearchControlLayer scl = null; // feature control layer to manage feature specific events

    private final Pref<Boolean> prefSearchOn = getPref(R.string.FSSearch_qc_searchOn, false);
    private final Pref<Boolean> prefShowSearchResult = getPref(R.string.FSSearch_qc_showSearchResult, false);
    private final Pref<Boolean> prefShowSearchResultEnabled = new Pref<>(false);
    private final Pref<Long> prefShowPos = getPref(R.string.FSSearch_pref_SearchPos, NO_POS);

    public FSSearch(MGMapActivity mmActivity) {
        super(mmActivity);
        prefSearchOn.setValue(false);

        searchView = new SearchView(mmActivity.getApplicationContext(), this);
        searchView.init(mmActivity);
        RelativeLayout mainView = mmActivity.findViewById(R.id.mainView);
        mainView.addView(searchView);

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
        setSearchProvider(new Nominatim());

        Observer showPositionObserver = (o, arg) -> {
            unregisterAll();
            if (prefShowSearchResult.getValue()){
                showSearchPos();
            }
        };
        prefShowSearchResult.addObserver(showPositionObserver);
        prefShowPos.addObserver(showPositionObserver);

        prefShowPos.addObserver((o, arg) -> prefShowSearchResultEnabled.setValue(prefShowPos.getValue() != NO_POS));

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
                try {
                    String sProvider = getSharedPreferences().getString(getResources().getString(R.string.preference_choose_search_key), "Nominatim");
                    setSearchProvider( (SearchProvider) Class.forName("mg.mgmap.features.search.provider."+sProvider).newInstance() );
                } catch (Exception e) {
                    Log.e(MGMapApplication.LABEL, NameUtil.context(),e);
                }

                searchView.setVisibility( View.VISIBLE );
                searchView.setFocusable(true);
                searchView.requestFocus();
                showKeyboard();
                scl = new SearchControlLayer();
                register(scl, false);
                triggerTTHideKeyboard();
            }
        } else {
            if (searchView.getVisibility() == View.VISIBLE){
                hideKeyboard();
                searchView.setVisibility( View.INVISIBLE );
                searchView.setFocusable(false);
                searchView.resetSearchResults();
                unregister(scl, false);
            }
        }
    }


    public class SearchControlLayer extends MVLayer {

        @Override
        protected boolean onTap(WriteablePointModel point) {
            return false;
        }

        @Override
        public boolean onLongPress(LatLong tapLatLong, Point layerXY, Point tapXY) {
            PointModel pos = new PointModelImpl(tapLatLong);
            Log.i(MGMapApplication.LABEL, NameUtil.context()+" rev Geocode: pos="+pos);
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

    private void setSearchProvider(SearchProvider searchProvider){
        this.searchProvider = searchProvider;
        searchProvider.init( getActivity(),this, searchView, getSharedPreferences());
    }

    private void doSearch(String text, int actionId){
        long timestamp = System.currentTimeMillis();
        triggerTTHideKeyboard();
        Log.i(MGMapApplication.LABEL, NameUtil.context()+" text="+text+" actionId="+actionId+" timestamp="+timestamp);
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
            Log.i(MGMapApplication.LABEL, NameUtil.context()+" "+pos);
            getMapViewUtility().setMapViewPosition(pos);
            register(new PointViewSearch(pos).setRadius(10));
            register(new PointViewSearch(pos).setRadius(1));
        }
    }

    private void hideKeyboard(){
        InputMethodManager inputMethodManager = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        View focus = getActivity().getCurrentFocus();
        if((inputMethodManager != null) && (focus != null)){
            inputMethodManager.hideSoftInputFromWindow(focus.getWindowToken(), 0);
            FullscreenUtil.enforceState(getActivity());
        }
    }

    private void showKeyboard(){
        InputMethodManager inputMethodManager = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        View focus = getActivity().getCurrentFocus();
        if((inputMethodManager != null) && (focus != null)){
            inputMethodManager.showSoftInput(focus, 0);
        }
    }

    public void setSearchResult(PointModel pmSearchResult) {
        Log.i(MGMapApplication.LABEL, NameUtil.context()+" "+pmSearchResult);
        prefShowPos.setValue(pmSearchResult.getLaLo());
        prefShowSearchResult.setValue(true);
    }
}
