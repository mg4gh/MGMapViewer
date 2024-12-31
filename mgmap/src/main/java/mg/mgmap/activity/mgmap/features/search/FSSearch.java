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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.datastore.MapDataStore;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mg.mgmap.BuildConfig;
import mg.mgmap.activity.mgmap.ControlView;
import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.activity.mgmap.FeatureService;
import mg.mgmap.R;
import mg.mgmap.activity.mgmap.util.MapViewUtility;
import mg.mgmap.activity.mgmap.view.ControlMVLayer;
import mg.mgmap.activity.settings.SearchProviderListPreference;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.util.FullscreenUtil;
import mg.mgmap.generic.util.KeyboardUtil;
import mg.mgmap.generic.util.Observer;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.view.DialogView;
import mg.mgmap.generic.view.ExtendedTextView;

public class FSSearch extends FeatureService {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private static final long T_HIDE_KEYBOARD = 10000; // in ms => 10s

    private final SearchView searchView;
    private SearchProvider searchProvider = null;
    private SearchControlLayer scl = null; // feature control layer to manage feature specific events

    private final Pref<Boolean> prefSearchOn = getPref(R.string.FSSearch_qc_searchOn, false);
    private final Pref<String> prefSearchProvider = getPref(R.string.preference_choose_search_key, "Graphhopper");
    private final Pref<Boolean> prefShowSearchResult = getPref(R.string.FSSearch_qc_showSearchResult, false);
    private final Pref<Boolean> prefShowSearchResultEnabled = new Pref<>(false);
    private final Pref<String> prefSearchPos = getPref(R.string.FSSearch_pref_SearchPos2, "");
    final Pref<Boolean> prefPosBasedSearch = getPref(R.string.FSSearch_pref_PosBasedSearch, false);
    private final Pref<Boolean> prefSearchResultDetails = getPref(R.string.FSSearch_pref_SearchDetails_key, false);
    private final Pref<Boolean> prefReverseSearchOn = getPref(R.string.FSSearch_reverseSearchOn, false);
    private final Pref<Boolean> prefLocationBasedSearchOn = getPref(R.string.FSSearch_locationBasedSearchOn, false);
    private final Pref<Boolean> prefQCSelectSearchProvider = new Pref<>(false);


    public FSSearch(MGMapActivity mmActivity) {
        super(mmActivity);
        prefSearchOn.setValue(false);
        setSearchProvider();
        //noinspection ConstantValue
        if (BuildConfig.FLAVOR.equals("mg4gh")){
            prefReverseSearchOn.setValue(true);
            prefLocationBasedSearchOn.setValue(true);
        }

        searchView = new SearchView(mmActivity.getApplicationContext(), this);
        searchView.init(mmActivity);
        searchView.setPosBasedSearchIcon(prefPosBasedSearch.getValue(), prefLocationBasedSearchOn.getValue());
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
        searchText.setOnClickListener(v -> {
            showKeyboard();
            triggerTTHideKeyboard();
        });

        prefPosBasedSearch.addObserver(e -> {
            searchView.setPosBasedSearchIcon(prefPosBasedSearch.getValue(), prefLocationBasedSearchOn.getValue());
            if (prefSearchOn.getValue()){
                doSearch(searchText.getText().toString().trim(), EditorInfo.IME_ACTION_SEARCH);
            }
        });

        Observer showPositionObserver = (e) -> {
            unregisterAll();
            if (prefShowSearchResult.getValue()){
                showSearchPos();
            }
        };
        prefShowSearchResult.addObserver(showPositionObserver);
        prefSearchPos.addObserver(showPositionObserver);

        prefSearchPos.addObserver((e) -> prefShowSearchResultEnabled.setValue(!prefSearchPos.getValue().isEmpty()));

        if (getPref(R.string.MGMapApplication_pref_Restart, true).getValue()){
            prefShowSearchResult.setValue(false);
            prefSearchPos.setValue("");
        }
        prefSearchOn.addObserver(refreshObserver);
        prefSearchPos.onChange();
        prefShowSearchResult.onChange();
        prefQCSelectSearchProvider.addObserver(v->selectSearchProvider());
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
            etv.setDisabledData(prefLocationBasedSearchOn, R.drawable.search_pos_dis);
            etv.setHelp(r(R.string.FSRecording_qcPosBasedSearch_help)).setHelp(r(R.string.FSRecording_qcPosBasedSearch_help1),r(R.string.FSRecording_qcPosBasedSearch_help2));
        } else if ("selpro".equals(info)){
            etv.setData(R.drawable.settings);
            etv.setHelp("Select search provider");
            etv.setPrAction(prefQCSelectSearchProvider);
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
        public boolean onLongPress(LatLong tapLatLong, Point layerXY, Point tapXY) {
            if (prefReverseSearchOn.getValue()){
                PointModel pos = new PointModelImpl(tapLatLong);
                mgLog.i("rev Geocode: pos="+pos);
                long timestamp = System.currentTimeMillis();
                searchProvider.doSearch(new SearchRequest("",0,timestamp, pos, getActivity().getMapViewUtility().getZoomLevel() ));
                hideKeyboard();
                return true;
            }
            return false;
        }
    }


    private final Runnable ttHideKeyboard = this::hideKeyboard;
    private void triggerTTHideKeyboard(){
        getTimer().removeCallbacks(ttHideKeyboard);
        getTimer().postDelayed(ttHideKeyboard, T_HIDE_KEYBOARD);
    }

    private void setSearchProvider(){
        try {
            this.searchProvider = (SearchProvider) Class.forName("mg.mgmap.activity.mgmap.features.search.provider."+prefSearchProvider.getValue()).getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            mgLog.e(e);
        }
        searchProvider.init( getActivity(),this, searchView, getSharedPreferences());
    }

    private void doSearch(String text, int actionId){
        if (text.length() < 3) return; // text too short
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
        String sSearchPos = prefSearchPos.getValue();
        mgLog.i(sSearchPos);
        SearchPos searchPos = SearchPos.fromJsonString(sSearchPos);
        if ((searchPos != null) && (searchPos.getLat() != PointModel.NO_LAT_LONG) && (searchPos.getLon() != PointModel.NO_LAT_LONG)){
            getMapViewUtility().setCenter(searchPos);
            byte zoom = searchPos.getZoom();
            if ((MapViewUtility.ZOOM_LEVEL_MIN <= zoom) && (zoom <= MapViewUtility.ZOOM_LEVEL_MAX)){
                getMapView().getModel().mapViewPosition.setZoomLevel(zoom);
            }
            register(new PointViewSearch(searchPos).setRadius(10).setText(searchPos.label));
            register(new PointViewSearch(searchPos).setRadius(1));
        }
    }

    void hideKeyboard(){
        KeyboardUtil.hideKeyboard(searchView.searchText);
        searchView.keyboardIconView.setImageResource(R.drawable.keyboard1);
        searchView.keyboardIconView.setOnClickListener(v->showKeyboard());
    }

    void showKeyboard(){
        KeyboardUtil.showKeyboard(searchView.searchText);
        searchView.keyboardIconView.setImageResource(R.drawable.keyboard2);
        searchView.keyboardIconView.setOnClickListener(v->hideKeyboard());
    }

    public void setSearchResult(PointModel pmSearchResult) {
        setSearchResult(new SearchPos(pmSearchResult));
    }
    public void setSearchResult(SearchPos spSearchResult) {
        boolean outside = true;
        LatLong latLongSearchResult = spSearchResult.getLatLong();
        for (MapDataStore mds : getActivity().getMapLayerFactory().getMapDataStoreMap().keySet()) {
            if (mds.boundingBox().contains(latLongSearchResult)){
                outside = false;
                break;
            }
        }
        if (outside){
            mgLog.w("outside of map: "+spSearchResult);
            DialogView dialogView = activity.findViewById(R.id.dialog_parent);
            dialogView.lock(() -> dialogView
                    .setTitle("Warning")
                    .setMessage("Search result outside mapsforge map")
                    .setLogPrefix("Search")
                    .setPositive("Locate anyway", evt -> setSearchResult2(spSearchResult))
                    .setNegative("Cancel",null)
                    .show());
        } else {
            setSearchResult2(spSearchResult);
        }
    }
    public void setSearchResult2(SearchPos spSearchResult) {
        mgLog.i(spSearchResult);
        String sSearchResult = spSearchResult.toJsonString();
        if (prefSearchPos.getValue().equals(sSearchResult)){
            prefSearchPos.onChange();
        } else {
            prefSearchPos.setValue(sSearchResult);
        }
        prefShowSearchResult.setValue(true);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isPosBasedSearch(){
        return prefPosBasedSearch.getValue() && prefLocationBasedSearchOn.getValue();
    }
    public boolean showSearchDetails(){
        return prefSearchResultDetails.getValue();
    }


    /** @noinspection RegExpRedundantEscape */
    public void processGeoIntent(String sUri){
        mgLog.i("sUri="+sUri);

        // possible patterns are (according to https://developer.android.com/guide/components/intents-common#java)
        // geo:latitude,longitude
        // geo:latitude,longitude?z=zoom
        // geo:0,0?q=lat,lng(label)
        // geo:0,0?q=my+street+address

        String d = "(\\-?\\d*\\.?\\d+)";
        Pattern p1 = Pattern.compile("geo:"+d+","+d);
        Pattern p2 = Pattern.compile("geo:"+d+","+d+"\\?z=([12]?[0-9])");
        Pattern p3 = Pattern.compile("geo:"+d+","+d+"\\?q="+d+","+d+"(\\(([^\\)]+)\\))?");
        Pattern p4 = Pattern.compile("geo:0,0\\?q=(.*)");

        double lat = PointModel.NO_LAT_LONG;
        double lon = PointModel.NO_LAT_LONG;
        byte zoom = 0; // no zoom
        String label = "";
        String qString = null;
        Matcher m = p1.matcher(sUri);
        if (m.matches()){
            mgLog.i("p1 matched");
            lat = Double.parseDouble(m.group(1));
            lon = Double.parseDouble(m.group(2));
        } else {
            m = p2.matcher(sUri);
            if (m.matches()){
                mgLog.i("p2 matched");
                lat = Double.parseDouble(m.group(1));
                lon = Double.parseDouble(m.group(2));
                zoom = Byte.parseByte(m.group(3));
                zoom = (byte)Math.max(Math.min(zoom, 22), 6);
            } else {
                m = p3.matcher(sUri);
                if (m.matches()){
                    mgLog.i("p3 matched");
                    lat = Double.parseDouble(m.group(3));
                    lon = Double.parseDouble(m.group(4));
                    if ((m.groupCount()>=6) && (m.group(6)!=null)){
                        label = m.group(6);
                    }
                } else {
                    m = p4.matcher(sUri);
                    if (m.matches()){
                        mgLog.i("p4 matched");
                        qString = m.group(1);
                    }
                }
            }
        }
        if ((lat != PointModel.NO_LAT_LONG) && (lon != PointModel.NO_LAT_LONG)){
            SearchPos searchPos = new SearchPos(lat,lon);
            searchPos.setZoom(zoom);
            searchPos.setLabel(label);
            setSearchResult(searchPos);
        } else {
            if ((qString != null) && (!qString.isEmpty())){
                prefPosBasedSearch.setValue(false);
                setSearchProvider();
                searchView.searchText.setText(qString);
                searchProvider.doSearch(new SearchRequest(qString,EditorInfo.IME_ACTION_SEND, System.currentTimeMillis(), new PointModelImpl(),0 ));
            }
        }
    }

    private void selectSearchProvider(){
        Context context = getActivity();
        DialogView dialogView = getActivity().findViewById(R.id.dialog_parent);

        RadioGroup radioGroup = new RadioGroup(context);
        String[] searchProviders = SearchProviderListPreference.getSearchProviders(getApplication());
        ArrayList<Integer> checkedList = new ArrayList<>();
        for (String searchProvider : searchProviders) {
            RadioButton rb1 = new RadioButton(context);
            if ((searchProvider != null) && (!searchProvider.isEmpty())) {
                rb1.setText(searchProvider);
                rb1.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
                rb1.setId(searchProvider.hashCode());
                rb1.setPadding(ControlView.dp(10), ControlView.dp(10), ControlView.dp(10), ControlView.dp(10));
                radioGroup.addView(rb1);
            }
        }
        checkedList.add(0, prefSearchProvider.getValue().hashCode());
        radioGroup.check(checkedList.get(0));
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> checkedList.add(0, checkedId));

        dialogView.lock(() -> dialogView
                .setTitle("Select Search Provider")
                .setContentView(radioGroup)
                .setPositive("OK", evt -> {
                    for (String searchProvider : searchProviders) {
                        if (searchProvider.hashCode() == checkedList.get(0)){
                            prefSearchProvider.setValue(searchProvider);
                            setSearchProvider();
                            if (prefSearchOn.getValue()){
                                doSearch(searchView.searchText.getText().toString().trim(), EditorInfo.IME_ACTION_SEARCH);
                            }

                            break;
                        }
                    }
                })
                .setNegative("Cancel", null)
                .show());

    }
}
