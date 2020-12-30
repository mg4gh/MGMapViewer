/*
 * Copyright 2017 - 2020 mg4gh
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
package mg.mapviewer.features.search;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;

import java.util.Observable;
import java.util.Observer;
import java.util.UUID;

import mg.mapviewer.MGMapActivity;
import mg.mapviewer.MGMapApplication;
import mg.mapviewer.MGMicroService;
import mg.mapviewer.R;
import mg.mapviewer.features.search.provider.Nominatim;
import mg.mapviewer.model.PointModel;
import mg.mapviewer.model.PointModelImpl;
import mg.mapviewer.model.WriteablePointModel;
import mg.mapviewer.util.NameUtil;
import mg.mapviewer.util.MGPref;
import mg.mapviewer.view.ExtendedTextView;
import mg.mapviewer.view.MVLayer;
import mg.mapviewer.view.PrefTextView;

public class MSSearch extends MGMicroService {

    private static final long T_HIDE_KEYBOARD = 10000; // in ms => 10s

    private final SearchView searchView;
    private SearchProvider searchProvider = null;
    private MSSControlLayer msscl = null; // feature control layer to manage feature specific events

    private final MGPref<Boolean> prefSearchOn = MGPref.get(R.string.MSSearch_qc_searchOn, false);
    private final MGPref<Boolean> prefShowSearchResult = MGPref.get(R.string.MSSearch_qc_showSearchResult, false);
    private final MGPref<Boolean> prefShowSearchResultEnabled = new MGPref<Boolean>(UUID.randomUUID().toString(),false,false);
    private final MGPref<Long> prefShowPos = MGPref.get(R.string.MSSearch_pref_SearchPos, 0l);
    private final MGPref<Boolean> prefFullscreen = MGPref.get(R.string.MSFullscreen_qc_On, true);

//    private PrefTextView ptvSearchRes = null;

    public MSSearch(MGMapActivity mmActivity) {
        super(mmActivity);
        prefSearchOn.setValue(false);

        searchView = new SearchView(mmActivity.getApplicationContext(), this);
        searchView.init(mmActivity);
        RelativeLayout mainView = mmActivity.findViewById(R.id.mainView);
        mainView.addView(searchView);

        EditText searchText = searchView.searchText;
        searchText.setSelectAllOnFocus(true);
        searchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView tv, int actionId, KeyEvent event) {
                doSearch(tv.getText().toString().trim(), actionId);
                checkFullscreen();
                return true;
            }
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
        setSearchProvider(new Nominatim());

        Observer showPositionObserver = new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                unregisterAll();
                if (prefShowSearchResult.getValue()){
                    showSearchPos();
                }
            }
        };
        prefShowSearchResult.addObserver(showPositionObserver);
        prefShowPos.addObserver(showPositionObserver);

        prefShowPos.addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                prefShowSearchResultEnabled.setValue(prefShowPos.getValue() != 0l);
            }
        });

        if (MGPref.get(R.string.MGMapApplication_pref_Restart, true).getValue()){
            prefShowPos.setValue(0l);
        }
        prefShowSearchResult.onChange();

    }

    @Override
    public ExtendedTextView initQuickControl(ExtendedTextView etv, String info){
        if ("group_search".equals(info)){
            etv.setData(prefSearchOn, prefShowSearchResult, R.drawable.group_search1,R.drawable.group_search2,R.drawable.group_search3,R.drawable.group_search4);
            etv.setPrAction(MGPref.anonymous(false));
        } else if ("search".equals(info)){
            etv.setData(prefSearchOn,R.drawable.search,R.drawable.search1b);
            etv.setPrAction(prefSearchOn);
        } else if ("searchRes".equals(info)){
            etv.setData(prefShowSearchResult,R.drawable.search_res1,R.drawable.search_res2);
            etv.setPrAction(prefShowSearchResult);
            etv.setDisabledData(prefShowSearchResultEnabled, R.drawable.search_res3);
        } else if ("help_search".equals(info)){

        }
        return etv;
    }

    @Override
    protected void onResume() {
        super.onResume();
        prefSearchOn.addObserver(refreshObserver);
        refreshObserver.onChange();
    }

    @Override
    protected void onPause() {
        super.onPause();
        prefSearchOn.deleteObserver(refreshObserver);
    }

    @Override
    protected void doRefresh() {
        boolean visibility = prefSearchOn.getValue();
        changeVisibility(visibility);
        getControlView().setDashboardVisibility(!visibility);
    }

    private void changeVisibility(boolean targetVisibility){
        if (targetVisibility){
            if (searchView.getVisibility() == View.INVISIBLE){
                try {
                    String sProvider = getSharedPreferences().getString(getResources().getString(R.string.preference_choose_search_key), "Nominatim");
                    setSearchProvider( (SearchProvider) Class.forName("mg.mapviewer.features.search.provider."+sProvider).newInstance() );
                } catch (Exception e) {
                    Log.e(MGMapApplication.LABEL, NameUtil.context(),e);
                }

                searchView.setVisibility( View.VISIBLE );
                searchView.setFocusable(true);
                searchView.requestFocus();
                showKeyboard();
                msscl = new MSSControlLayer();
                register(msscl, false);
                triggerTTHideKeyboard();
            }
        } else {
            if (searchView.getVisibility() == View.VISIBLE){
                searchView.setVisibility( View.INVISIBLE );
                searchView.setFocusable(false);
                searchView.resetSearchResults();
                unregister(msscl, false);
            }
        }
    }


    public class MSSControlLayer extends MVLayer {

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


    private Runnable ttHideKeyboard = new Runnable() {
        @Override
        public void run() {
            hideKeyboard();
        }
    };
    private void triggerTTHideKeyboard(){
        getTimer().removeCallbacks(ttHideKeyboard);
        getTimer().postDelayed(ttHideKeyboard, T_HIDE_KEYBOARD);
    }

    private void setSearchProvider(SearchProvider searchProvider){
        this.searchProvider = searchProvider;
        searchProvider.init( getApplication(),this, searchView, getSharedPreferences());
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
        if (lalo != 0){
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
            checkFullscreen();
        }
    }

    private void showKeyboard(){
        InputMethodManager inputMethodManager = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        View focus = getActivity().getCurrentFocus();
        if((inputMethodManager != null) && (focus != null)){
            inputMethodManager.showSoftInput(focus, 0);
        }
    }

    private void checkFullscreen(){
        prefFullscreen.onChange();
    }

    public void setSearchResult(PointModel pmSearchResult) {
        Log.i(MGMapApplication.LABEL, NameUtil.context()+" "+pmSearchResult);
        prefShowPos.setValue(pmSearchResult.getLaLo());
        prefShowSearchResult.setValue(true);
    }
}
