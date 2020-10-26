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
package mg.mapviewer;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.mapsforge.core.util.Parameters;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.cache.InMemoryTileCache;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.cache.TwoLevelTileCache;
import org.mapsforge.map.layer.download.TileDownloadLayer;
import org.mapsforge.map.layer.overlay.Grid;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.layer.tilestore.TileStoreLayer;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.XmlRenderTheme;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import mg.mapviewer.util.NameUtil;
import mg.mapviewer.util.PersistenceManager;
import mg.mapviewer.features.tilestore.XmlTileSource;
import mg.mapviewer.features.tilestore.XmlTileSourceConfig;
import mg.mapviewer.features.tilestore.XmlTileSourceConfigReader;
import mg.mapviewer.features.tilestore.MGTileStore;
import mg.mapviewer.features.tilestore.MGTileStoreLayer;

/** The MGMapLayerFactory provides a list of keys of available map layers and it is able to create a map layer instance for a given key.
 * Available map layer have one of the following types:
 * <ul>
 *     <li>Mapsforge: A mapsforge layer is given by a map file in the mapsforge subdirectory. The name of the file correspond to the name of the layer.</li>
 *     <li>Mapstores: A mapstore layer is given by a subdirectory below the mapstores subdirectory. This directory name provides the name for the mapstore layer. The structure of this directory follows the typical tile store hierarchy:
 *     ./storeName/zoomLevel/y-tile/x-tile.png</li>
 *     <li>Maponline: A maponline layer is give by a description file in the maponline directory. The description contains the schema for an online tile store, similar to the local tilestores.</li>
 *     <li>Mapgrid: A mapgrid layer is give by a description file in the mapgrid directory. The description contains the grid spacing for different zoom level.</li>
 * </ul>
 */
public class MGMapLayerFactory {

    public static final String XML_CONFIG_NAME = "config.xml";
    public enum Types { MAPSFORGE, MAPSTORES, MAPONLINE, MAPGRID }
    public static HashMap<Types, FilenameFilter> filters = new HashMap<>();
    public static HashMap<String, Layer> mapLayers = new HashMap<>();

    static {
        filters.put(Types.MAPSFORGE, new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                boolean res = !(new File(dir,name).isDirectory());
                res &= name.endsWith(".map") || name.endsWith(".ref");
                return res;
            }
        });
        filters.put(Types.MAPSTORES, new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return (new File(dir,name).isDirectory());
            }
        });
        filters.put(Types.MAPONLINE, new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                File fStore = new File(dir,name);
                File fConfig = new File(fStore,XML_CONFIG_NAME);
                return (fStore.isDirectory() && fConfig.exists());
            }
        });
//        FilenameFilter xml = new FilenameFilter() {
//            @Override
//            public boolean accept(File dir, String name) {
//                boolean res = !(new File(dir,name).isDirectory());
//                res &= name.endsWith(".xml");
//                return res;
//            }
//        };
//        filters.put(Types.MAPONLINE,xml);
        FilenameFilter prop = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                boolean res = !(new File(dir,name).isDirectory());
                res &= name.endsWith(".properties");
                return res;
            }
        };
        filters.put(Types.MAPGRID,prop);
    }

    private static Context context = null;
    private static MGMapActivity activity = null;
    private static MapView mapView = null;
    private static SharedPreferences sharedPreferences = null;
    private static XmlRenderTheme xmlRenderTheme = null;


    /** Returns a list of available map layers */
    public static String[] getAvailableMapLayers(){
        String[] resa = new String[]{"none"};
        File mapsDir = PersistenceManager.getInstance().getMapsDir();
        ArrayList<String> res = new ArrayList<>();
        res.add(resa[0]);
        for (Types type: Types.values()){
            File typeDir = new File(mapsDir, type.name().toLowerCase());
            String[] entries = typeDir.list(filters.get(type));
            Arrays.sort(entries);
            for (String entry : entries){
                res.add(type+": "+entry);
            }
        }
        return res.toArray(resa);
    }

    /** create a Layer object from corresponding key */
    public static Layer getMapLayer(String key){
        Layer layer = null;
        try {
            if (mapLayers.keySet().contains(key)){
                return mapLayers.get(key);
            }

            String[] keypart = key.split(": ");
            Types type = null;
            String entry = null;
            if (keypart.length != 2) return null;
            try {
                type = Types.valueOf(keypart[0]);
                entry = keypart[1];
            } catch (Exception e){
                Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
                return null;
            }

            layer = null;
            File mapsDir = PersistenceManager.getInstance().getMapsDir();
            File typeDir = new File(mapsDir, type.name().toLowerCase());
            File entryFile = new File(typeDir, entry);
            switch (type){
                case MAPSFORGE:
                    String language = sharedPreferences.getString(context.getResources().getString(R.string.preferences_language_key), "");

                    TileCache tileCache = AndroidUtil.createTileCache(context, "trl_"+key,
                            mapView.getModel().displayModel.getTileSize(), 1.0f,
                            mapView.getModel().frameBufferModel.getOverdrawFactor() *1.5, false);
                    activity.addTileCache(tileCache);

                    if (entry.endsWith(".ref")){
                        if (activity.requestReadPermissions()){
                            mapLayers.put(key, null);
                        }

                        BufferedReader in = new BufferedReader(new FileReader(entryFile));
                        String line = in.readLine();
                        entryFile = new File(line);
                    }
                    MapDataStore mapFile = new MapFile(entryFile, language);
                    TileRendererLayer tileRendererLayer = new TileRendererLayer(
                            tileCache,  mapFile,
                            mapView.getModel().mapViewPosition,
                            true, true, false,
                            AndroidGraphicFactory.INSTANCE
                    );
                    tileRendererLayer.setAlpha(1f);
                    tileRendererLayer.setXmlRenderTheme(xmlRenderTheme);
                    layer = tileRendererLayer;
                    break;
                case MAPSTORES:
                    MGTileStore tileStore = MGTileStore.createTileStore(entryFile);
                    InMemoryTileCache memoryTileCache = new InMemoryTileCache(AndroidUtil.getMinimumCacheSize(context,
                            mapView.getModel().displayModel.getTileSize(),
                            mapView.getModel().frameBufferModel.getOverdrawFactor(), 1.0f));
                    tileCache = new TwoLevelTileCache(memoryTileCache, tileStore);
                    activity.addTileCache(tileCache);


                    TileStoreLayer tileStoreLayer = new MGTileStoreLayer(tileStore, tileCache,
                            mapView.getModel().mapViewPosition, AndroidGraphicFactory.INSTANCE, true);
                    tileStoreLayer.setAlpha(tileStore.getDefaultAlpha());
                    tileStoreLayer.setParentTilesRendering(Parameters.ParentTilesRendering.OFF);
                    layer = tileStoreLayer;
                    break;
                case MAPONLINE:
                    File fXmlConfig = new File(entryFile,XML_CONFIG_NAME);
                    XmlTileSourceConfig config = new XmlTileSourceConfigReader().parseXmlTileSourceConfig(entry, new FileInputStream(fXmlConfig));
                    XmlTileSource xmlTileSource = new XmlTileSource(config);
                    xmlTileSource.setUserAgent("mapsforge-samples-android");

                    TileCache onlTileCache = AndroidUtil.createTileCache(context, key,
                            mapView.getModel().displayModel.getTileSize(), 1.0f,
                            mapView.getModel().frameBufferModel.getOverdrawFactor(), false);
                    activity.addTileCache(onlTileCache);

                    layer = new TileDownloadLayer(onlTileCache,
                            mapView.getModel().mapViewPosition, xmlTileSource,
                            AndroidGraphicFactory.INSTANCE);
                    break;
                case MAPGRID:
                    try{
                        Properties properties = new Properties();
                        properties.load(new FileInputStream(entryFile));
                        Map<Byte, Double> spacingConfig = new HashMap<>();
                        for (Object propName : properties.keySet()){
                            if (propName instanceof String){
                                String sPropName = (String) propName;
                                if (sPropName.toLowerCase().startsWith("zoomlevel")){
                                    int idx =  9;
                                    if (sPropName.toLowerCase().startsWith("zoomlevel_")) idx =10;
                                    byte zl = Byte.parseByte(sPropName.substring(idx));
                                    double value = Double.parseDouble( properties.getProperty(sPropName) );
                                    spacingConfig.put(zl, value);
                                }
                            }
                        }
                        Grid gridLayer = new Grid(AndroidGraphicFactory.INSTANCE, mapView.getModel().displayModel, spacingConfig);
                        layer = gridLayer;
                    } catch (Exception e){
                        Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
                    }
                    break;
            }
            if (layer != null){
                mapLayers.put(key, layer);
            }
        } catch (Exception e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context()+" "+e.getMessage());
        }
        Log.i(MGMapApplication.LABEL, NameUtil.context());
        return layer;
    }





    public static Context getContext() {
        return context;
    }

    public static void setContext(Context context) {
        MGMapLayerFactory.context = context;
    }

    public static MGMapActivity getActivity() {
        return activity;
    }

    public static void setActivity(MGMapActivity activity) {
        MGMapLayerFactory.activity = activity;
    }

    public static MapView getMapView() {
        return mapView;
    }

    public static void setMapView(MapView mapView) {
        MGMapLayerFactory.mapView = mapView;
    }

    public static SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }

    public static void setSharedPreferences(SharedPreferences sharedPreferences) {
        MGMapLayerFactory.sharedPreferences = sharedPreferences;
    }

    public static XmlRenderTheme getXmlRenderTheme() {
        return xmlRenderTheme;
    }

    public static void setXmlRenderTheme(XmlRenderTheme xmlRenderTheme) {
        MGMapLayerFactory.xmlRenderTheme = xmlRenderTheme;
    }
}
