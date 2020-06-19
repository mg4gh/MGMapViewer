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

import mg.mapviewer.MGMapActivity;
import mg.mapviewer.MGMapApplication;
import mg.mapviewer.MGMicroService;
import mg.mapviewer.R;
import mg.mapviewer.features.search.provider.Nominatim;
import mg.mapviewer.model.PointModel;
import mg.mapviewer.model.PointModelImpl;
import mg.mapviewer.model.WriteablePointModel;
import mg.mapviewer.util.NameUtil;
import mg.mapviewer.view.MVLayer;

public class MSSearch extends MGMicroService {

    private SearchView searchView;
    private SearchProvider searchProvider = null;
    private MSSControlLayer msscl = null; // feature control layer to manage feature specific events

    private static final long T_HIDE_KEYBOARD = 10000; // in ms => 10s


    public MSSearch(MGMapActivity mmActivity) {
        super(mmActivity);

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
    }

    @Override
    protected void start() {
        getApplication().searchOn.addObserver(refreshObserver);
        doRefresh();
    }

    @Override
    protected void stop() {
        getApplication().searchOn.deleteObserver(refreshObserver);
    }

    @Override
    protected void doRefresh() {
        boolean visibility = getApplication().searchOn.getValue();
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
                unregisterAll();
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
        searchProvider.init(this, searchView, getSharedPreferences());
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

    void showSearchPos(PointModel pos){
        unregisterAll();
        register(new PointViewSearch(pos).setRadius(10));
        register(new PointViewSearch(pos).setRadius(1));
    }


    private void hideKeyboard(){
        InputMethodManager inputMethodManager = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        View focus = getActivity().getCurrentFocus();
        if((inputMethodManager != null) && (focus != null)){
            inputMethodManager.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        }
    }

    private void showKeyboard(){
        InputMethodManager inputMethodManager = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        View focus = getActivity().getCurrentFocus();
        if((inputMethodManager != null) && (focus != null)){
            inputMethodManager.showSoftInput(focus, 0);
        }
    }
}
