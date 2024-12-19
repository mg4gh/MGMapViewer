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
package mg.mgmap.activity.settings;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.ListPreference;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.activity.mgmap.MGMapLayerFactory;
import mg.mgmap.generic.util.basic.MGLog;

public class MapLayerListPreference extends ListPreference {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private final HashMap<MGMapLayerFactory.Types, FilenameFilter> filters = new HashMap<>();

    public MapLayerListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        initFilters();
        this.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        String[] maps = getAvailableMapLayers(context);
        setEntries(maps);
        setEntryValues(maps);
        setDefaultValue(maps[0]);
    }

    private void initFilters(){
        filters.put(MGMapLayerFactory.Types.MAPSFORGE, (dir, name) -> new File(dir,name).isDirectory() || name.endsWith(".map"));
        filters.put(MGMapLayerFactory.Types.MAPSTORES, (dir, name) -> (new File(dir,name).isDirectory()));
        filters.put(MGMapLayerFactory.Types.MAPONLINE, (dir, name) -> {
            File fStore = new File(dir,name);
            File fConfig = new File(fStore,MGMapLayerFactory.XML_CONFIG_NAME);
            return (fStore.isDirectory() && fConfig.exists());
        });
        filters.put(MGMapLayerFactory.Types.MAPGRID,(dir, name) -> {
            boolean res = !(new File(dir,name).isDirectory());
            res &= name.endsWith(".properties");
            return res;
        });
    }

    /** Returns a list of available map layers */
    public String[] getAvailableMapLayers(Context context){
        String[] resa = new String[]{"none"};
        if (context.getApplicationContext() instanceof MGMapApplication application) {
            File mapsDir = application.getPersistenceManager().getMapsDir();
            ArrayList<String> res = new ArrayList<>();
            res.add(resa[0]);
            for (MGMapLayerFactory.Types type: MGMapLayerFactory.Types.values()){
                File typeDir = new File(mapsDir, type.name().toLowerCase());
                String[] entries = typeDir.list(filters.get(type));
                if (entries != null){
                    Arrays.sort(entries);
                    for (String entry : entries){
                        res.add(type+": "+entry);
                    }
                }
            }
            res.add(MGMapLayerFactory.Types.MAPGRID.name()+": "+"hgt");
            resa = res.toArray(resa);
        }
        return resa;
    }



    @Override
    protected void onClick() {
        mgLog.i("key="+getKey()+" value="+getValue());
        super.onClick();
    }
}
