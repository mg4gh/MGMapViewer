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
package mg.mgmap.activity.mgmap;

import android.content.Context;
import android.content.SharedPreferences;

import org.mapsforge.core.util.Parameters;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.TileLayer;
import org.mapsforge.map.layer.cache.InMemoryTileCache;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.cache.TwoLevelTileCache;
import org.mapsforge.map.layer.download.TileDownloadLayer;
import org.mapsforge.map.layer.hills.DemFolder;
import org.mapsforge.map.layer.hills.DemFolderFS;
import org.mapsforge.map.layer.hills.HiResClasyHillShading;
import org.mapsforge.map.layer.hills.HillsRenderConfig;
import org.mapsforge.map.layer.hills.MemoryCachingHgtReaderTileSource;
import org.mapsforge.map.layer.hills.ShadingAlgorithm;
import org.mapsforge.map.layer.hills.StandardClasyHillShading;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.layer.tilestore.TileStoreLayer;
import org.mapsforge.map.rendertheme.XmlRenderTheme;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import mg.mgmap.activity.mgmap.view.Grid;
import mg.mgmap.activity.mgmap.view.HgtGridView;
import mg.mgmap.R;
import mg.mgmap.generic.util.basic.IOUtil;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.application.util.PersistenceManager;
import mg.mgmap.activity.mgmap.features.tilestore.XmlTileSource;
import mg.mgmap.activity.mgmap.features.tilestore.XmlTileSourceConfig;
import mg.mgmap.activity.mgmap.features.tilestore.XmlTileSourceConfigReader;
import mg.mgmap.activity.mgmap.features.tilestore.MGTileStore;
import mg.mgmap.activity.mgmap.features.tilestore.MGTileStoreLayer;
import mg.mgmap.generic.util.Pref;

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

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    public static final String XML_CONFIG_NAME = "config.xml";
    public static final String KEY_SEPARATOR = ": ";
    public static final String WORLD_MAP = "world.map";

    public static ArrayList<String> getMapLayerKeys(Context context){
        ArrayList<String> mapLayerKeys = new ArrayList<>();
        int[] prefIds = new int[]{
                R.string.Layers_pref_chooseMap1_key,
                R.string.Layers_pref_chooseMap2_key,
                R.string.Layers_pref_chooseMap3_key,
                R.string.Layers_pref_chooseMap4_key,
                R.string.Layers_pref_chooseMap5_key};
        for (int id : prefIds){
            mapLayerKeys.add( context.getResources().getString( id ));
        }
        return mapLayerKeys;
    }


    public enum Types { MAPSFORGE, MAPSTORES, MAPONLINE, MAPGRID }
    private final HashMap<String, Layer> mapLayers = new HashMap<>();

    private final MGMapActivity activity;
    private final PersistenceManager persistenceManager;
    private final XmlRenderTheme xmlRenderTheme;

    public MGMapLayerFactory(MGMapActivity activity) {
        this.activity = activity;
        persistenceManager = activity.getMGMapApplication().getPersistenceManager();
        xmlRenderTheme = activity.getRenderTheme();

        SharedPreferences sharedPreferences = activity.getSharedPreferences();
        for (String prefKey : getMapLayerKeys()) {
            String defaultValue = prefKey.equals(activity.getString(R.string.Layers_pref_chooseMap2_key))?"MAPSFORGE: all":"none";
            String key = sharedPreferences.getString(prefKey, defaultValue);
            sharedPreferences.edit().putString(prefKey, key).apply(); // so if pref was not existing, then default value "none" is now set - this helps to prevent recreate activity on initial set
        }

        try {
            if (sharedPreferences.getBoolean(WORLD_MAP,true)){
                IOUtil.copyStreams(activity.getAssets().open(WORLD_MAP), new FileOutputStream(new File(activity.getMGMapApplication().getPersistenceManager().getMapsforgeDir(), WORLD_MAP)));
                sharedPreferences.edit().putBoolean(WORLD_MAP, false).apply();
            }
        } catch (IOException e) {
            mgLog.e(e);
        }
    }

    public ArrayList<String> getMapLayerKeys() {
        return getMapLayerKeys(activity);
    }

    /** create a Layer object from corresponding key */
    public Layer getMapLayer(String key){
        Layer layer = null;
        MapView mapView = activity.getMapsforgeMapView();
        SharedPreferences sharedPreferences = activity.getSharedPreferences();
        try {
            if (mapLayers.containsKey(key)){
                return mapLayers.get(key);
            }

            String[] keypart = key.split(KEY_SEPARATOR);
            Types type;
            String entry;
            if (keypart.length != 2) return null;
            try {
                type = Types.valueOf(keypart[0]);
                entry = keypart[1];
            } catch (Exception e){
                mgLog.e(e);
                return null;
            }

            File mapsDir = persistenceManager.getMapsDir();
            File typeDir = new File(mapsDir, type.name().toLowerCase());
            File entryFile = new File(typeDir, entry);
            switch (type){
                case MAPSFORGE:
                    String language = sharedPreferences.getString(activity.getResources().getString(R.string.preferences_language_key), "");

                    TileCache msfTileCache = AndroidUtil.createTileCache(activity, "trl_"+key,
                            mapView.getModel().displayModel.getTileSize(), 1.0f,
                            mapView.getModel().frameBufferModel.getOverdrawFactor() *1.5, false);
                    activity.addTileCache(msfTileCache);

                    MapDataStore mds;
                    if (entryFile.isDirectory()){ // represents multi map
                        String policyKey = "policy";
                        MultiMapDataStore.DataPolicy policy = MultiMapDataStore.DataPolicy.FAST_DETAILS;
                        Properties props = new Properties();
                        File configFile = new File(entryFile, "config.properties");
                        if (configFile.exists()){ // ... with config.properties
                            try (FileInputStream fis = new FileInputStream(configFile)){
                                props.load( fis ); // ... that are loaded into props
                            }
                        } else { // without config.properties
                            String[] msfNames = new File(mapsDir, Types.MAPSFORGE.name().toLowerCase()).list();
                            if (msfNames != null){
                                for (int i=0; i<msfNames.length; i++){
                                    if (msfNames[i].endsWith(".map")) {
                                        props.put("map" + i, msfNames[i]); // ... map files form mapsforge base dir are placed in props
                                    }
                                }
                            }
                        }
                        if (props.containsKey(policyKey)){
                            try {
                                policy = MultiMapDataStore.DataPolicy.valueOf(props.getProperty(policyKey));
                            } catch (IllegalArgumentException e) {
                                mgLog.e(e.getMessage());
                            }
                        }
                        MultiMapDataStore mmds = new MultiMapDataStore(policy);
                        boolean first = true;
                        for (Object propKey : props.keySet()){ // now props are added as MapDataStore
                            if (propKey.toString().startsWith("map")){
                                File mapsforgeFile = new File(typeDir, ""+props.get(propKey));
                                if (mapsforgeFile.exists()){
                                    MapDataStore mapFile = new MapFileWithBorder(mapsforgeFile, language);
                                    String prioKey = propKey.toString().replace("map","prio");
                                    if (props.containsKey(prioKey)){
                                        try {
                                            mapFile.setPriority(Integer.parseInt(props.getProperty(prioKey)));
                                        } catch (NumberFormatException e) {
                                            mgLog.e(e.getMessage());
                                        }
                                    }
                                    mmds.addMapDataStore(mapFile, first, first);
                                    activity.getMapDataStoreUtil().register(Types.MAPSFORGE.name()+ KEY_SEPARATOR +mapsforgeFile.getName(), mapFile);
                                    first = false;
                                }
                            }
                        }
                        mds = mmds;
                    } else { // entryFile is File
                        mds = new MapFileWithBorder(entryFile, language);
                        activity.getMapDataStoreUtil().register(key, mds);
                    }

                    HillsRenderConfig hillsConfig = null;
                    if (activity.getPrefCache().get(R.string.preferences_hill_shading_key, false).getValue()){
                        DemFolder hgtFolder = new DemFolderFS(persistenceManager.getHgtDir());
                        ShadingAlgorithm shadingAlgorithm = (activity.getPrefCache().get(R.string.preferences_hill_shading_hiRes_key, false).getValue())?new HiResClasyHillShading():new StandardClasyHillShading();
                        MemoryCachingHgtReaderTileSource hillTileSource = new MemoryCachingHgtReaderTileSource(hgtFolder, shadingAlgorithm, AndroidGraphicFactory.INSTANCE);
                        hillsConfig = new HillsRenderConfig(hillTileSource);
                        // call after setting/changing parameters, walks filesystem for DEM metadata
                        hillsConfig.indexOnThread();
                    }
                    TileRendererLayer tileRendererLayer = new TileRendererLayer(
                            msfTileCache,  mds,
                            mapView.getModel().mapViewPosition,
                            true, true, false,
                            AndroidGraphicFactory.INSTANCE,
                            hillsConfig
                    );
                    tileRendererLayer.setAlpha(1f);
                    tileRendererLayer.setXmlRenderTheme(xmlRenderTheme);
                    layer = tileRendererLayer;
                    break;
                case MAPSTORES:
                    MGTileStore tileStore = MGTileStore.createTileStore(entryFile, activity.getMGMapApplication().getAssets());
                    InMemoryTileCache memoryTileCache = new InMemoryTileCache(AndroidUtil.getMinimumCacheSize(activity,
                            mapView.getModel().displayModel.getTileSize(),
                            mapView.getModel().frameBufferModel.getOverdrawFactor()*1.5, 1.0f));
                    TileCache mstTileCache = new TwoLevelTileCache(memoryTileCache, tileStore);
                    activity.addTileCache(mstTileCache);
                    tileStore.registerCache(mstTileCache);

                    TileStoreLayer tileStoreLayer = new MGTileStoreLayer(tileStore, mstTileCache,
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

                    TileCache onlTileCache = AndroidUtil.createTileCache(activity, key,
                            mapView.getModel().displayModel.getTileSize(), 1.0f,
                            mapView.getModel().frameBufferModel.getOverdrawFactor()*1.5, false);
                    activity.addTileCache(onlTileCache);

                    layer = new TileDownloadLayer(onlTileCache,
                            mapView.getModel().mapViewPosition, xmlTileSource,
                            AndroidGraphicFactory.INSTANCE);
                    break;
                case MAPGRID:
                    try{
                        if ("hgt".equals(entry)){
                            layer = new HgtGridView(activity.application, AndroidGraphicFactory.INSTANCE, mapView.getModel().displayModel);
                        } else {
                            Properties properties = new Properties();
                            properties.load(new FileInputStream(entryFile));
                            Map<Byte, Double> spacingConfig = new HashMap<>();
                            for (Object propName : properties.keySet()){
                                if (propName instanceof String sPropName){
                                    if (sPropName.toLowerCase().startsWith("zoomlevel")){
                                        int idx =  9;
                                        if (sPropName.toLowerCase().startsWith("zoomlevel_")) idx =10;
                                        byte zl = Byte.parseByte(sPropName.substring(idx));
                                        double value = Double.parseDouble( properties.getProperty(sPropName) );
                                        spacingConfig.put(zl, value);
                                    }
                                }
                            }
                            layer = new Grid(AndroidGraphicFactory.INSTANCE, mapView.getModel().displayModel, spacingConfig);
                        }
                    } catch (Exception e){
                        mgLog.e(e);
                    }
                    break;
            }
            if (layer != null){
                mapLayers.put(key, layer);
            }

            if (layer instanceof TileLayer<?> alphaLayer) {
                Pref<Float> prefAlpha = activity.getPrefCache().get("alpha_"+key,1.0f);
                prefAlpha.addObserver((e) -> {
                    alphaLayer.setAlpha(prefAlpha.getValue());
                    if (alphaLayer.isVisible() && (prefAlpha.getValue()==0)){
                        alphaLayer.setVisible(false);
                    } else if (!alphaLayer.isVisible() && (prefAlpha.getValue()>0)){
                        alphaLayer.setVisible(true);
                    } else {
                        alphaLayer.requestRedraw();
                    }
                    mgLog.d(String.format(Locale.ENGLISH,"TileLayer visibility: %s visibility=%b alpha=%.2f",key,alphaLayer.isVisible(),prefAlpha.getValue()));
                });
            }
            if (layer instanceof Grid alphaLayer) {
                Pref<Float> prefAlpha = activity.getPrefCache().get("alpha_"+key,0.2f);
                prefAlpha.addObserver((e) -> {
                    alphaLayer.setAlpha(prefAlpha.getValue());
                    if (alphaLayer.isVisible() && (prefAlpha.getValue()==0)){
                        alphaLayer.setVisible(false);
                    } else if (!alphaLayer.isVisible() && (prefAlpha.getValue()>0)){
                        alphaLayer.setVisible(true);
                    } else {
                        alphaLayer.requestRedraw();
                    }
                    mgLog.d(String.format(Locale.ENGLISH,"Grid visibility: %s visibility=%b alpha=%.2f",key,alphaLayer.isVisible(),prefAlpha.getValue()));
                });
            }
        } catch (Exception e) {
            mgLog.e(e);
        }
        return layer;
    }

    public boolean hasAlpha(String key){
        return hasAlpha(getMapLayer(key));
    }
    public boolean hasAlpha(Layer layer){
        return (layer instanceof TileLayer) || (layer instanceof Grid);
    }

    public Map<String, Layer> getMapLayers(){
        return mapLayers;
    }
}
