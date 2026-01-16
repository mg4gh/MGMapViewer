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
package mg.mgmap.activity.mgmap.features.search.provider;

import android.content.SharedPreferences;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Locale;

import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.activity.mgmap.features.search.FSSearch;
import mg.mgmap.activity.mgmap.features.search.DegreeUtil;
import mg.mgmap.activity.mgmap.features.search.SearchProvider;
import mg.mgmap.activity.mgmap.features.search.SearchRequest;
import mg.mgmap.activity.mgmap.features.search.SearchResult;
import mg.mgmap.activity.mgmap.features.search.SearchView;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.util.basic.MGLog;

@SuppressWarnings("unused") // usage is via reflection
public class GeoLatLong extends SearchProvider {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private boolean autoCenter = false;

    @Override
    protected void init(MGMapActivity activity, FSSearch fsSearch, SearchView searchView, SharedPreferences preferences) {
        super.init(activity, fsSearch, searchView, preferences);
        String sAutoCenter = getSearchConfig().getProperty("autoCenter");
        if (sAutoCenter != null){
            try {
                autoCenter = Boolean.parseBoolean(sAutoCenter);
            } catch (NumberFormatException e) {
                mgLog.e(e);
            }
        }
    }

    @Override
    public void doSearch(SearchRequest request) {

        ArrayList<SearchResult> resList = new ArrayList<>();

        if (request.text.isEmpty()){ // reverse request
            if (request.actionId == 0){
                setResults(request, request.pos, resList);
                setSearchText(String.format(Locale.ENGLISH,"%f, %f", request.pos.getLat(), request.pos.getLon()));
            }
        } else { // forward request
            PointModel pm = tryForwardSearch(request);
            setResults(request, pm, resList);
        }
        publishResult(request, resList);
        if (autoCenter && (!resList.isEmpty())){
            fsSearch.setSearchResult(resList.get(0).pos);
        }
    }

    static PointModel tryForwardSearch(SearchRequest request){
        String text = request.text.trim().replaceAll("([EWNS]) ","$1");
        String[] words = text.split("[\\s,]+");
        int idx = 0;
        if (words[idx].isEmpty()){
            idx++;
        }
        double lat = 0, lon = 0;
        try {
            if (words.length > idx){
                lat = DegreeUtil.degree2double(true, words[idx]);
            }
        } catch (NumberFormatException e) {
            try {
                lat = DegreeUtil.doubleDegree2double(true, words[idx]);
            } catch (NumberFormatException e1) {
                mgLog.e(e.getMessage());
            }
        }
        idx++;
        try {
            if (words.length > idx){
                lon = DegreeUtil.degree2double(false, words[idx]);
            }
        } catch (NumberFormatException e) {
            try {
                lon = DegreeUtil.doubleDegree2double(false, words[idx]);
            } catch (NumberFormatException e1) {
                mgLog.e(e.getMessage());
            }
        }
        return new PointModelImpl(lat,lon);
    }

    static void setResults(SearchRequest request, PointModel pm, ArrayList<SearchResult> results){
        String res1 = String.format(Locale.ENGLISH,"Lat=%2.6f, Long=%2.6f", pm.getLat(), pm.getLon());
        results.add( new SearchResult(request, res1, pm));
        String res2 = String.format(Locale.ENGLISH,"Lat=%s, Long=%s", DegreeUtil.double2Degree(true, pm.getLat(), true), DegreeUtil.double2Degree(false, pm.getLon(), true));
        results.add( new SearchResult(request, res2, pm));
        String res3 = String.format(Locale.ENGLISH,"Lat=%s, Long=%s", DegreeUtil.double2Degree(true, pm.getLat(), false), DegreeUtil.double2Degree(false, pm.getLon(), false));
        results.add( new SearchResult(request, res3, pm));
    }

    static boolean validate(PointModel pm){
        if (pm.getLat() == 0) return false;
        if (pm.getLon() == 0) return false;
        int lat = (int)pm.getLat();
        int latFraction = (int)( (pm.getLat() - lat)*1000000 );
        if ((lat < -90) || (90 < lat)) return false;
        if (latFraction == 0) return false;
        int lon = (int)pm.getLon();
        int lonFraction = (int)( (pm.getLon() - lon)*1000000 );
        if ((lon < -180) || (180 < lon)) return false;
        if (lonFraction == 0) return false;
        return true;
    }

}
