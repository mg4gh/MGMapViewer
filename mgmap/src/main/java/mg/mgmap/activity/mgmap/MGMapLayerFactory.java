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
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.layer.tilestore.TileStoreLayer;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.XmlRenderTheme;

import java.io.File;
import java.io.FileInputStream;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import mg.mgmap.activity.mgmap.view.Grid;
import mg.mgmap.activity.mgmap.view.HgtGridView;
import mg.mgmap.R;
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

    public MGMapLayerFactory(MGMapActivity activity){
        this.activity = activity;
        persistenceManager = activity.getMGMapApplication().getPersistenceManager();
        xmlRenderTheme = activity.getRenderTheme();
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

            String[] keypart = key.split(": ");
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

                    MapDataStore mapFile = new MapFile(entryFile, language);
                    TileRendererLayer tileRendererLayer = new TileRendererLayer(
                            msfTileCache,  mapFile,
                            mapView.getModel().mapViewPosition,
                            true, true, false,
                            AndroidGraphicFactory.INSTANCE
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
