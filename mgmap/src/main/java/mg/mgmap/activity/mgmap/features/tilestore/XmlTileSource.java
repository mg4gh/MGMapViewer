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
package mg.mgmap.activity.mgmap.features.tilestore;

import org.mapsforge.core.model.Tile;
import org.mapsforge.map.layer.download.tilesource.AbstractTileSource;

import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import mg.mgmap.generic.util.basic.MGLog;

public class XmlTileSource extends AbstractTileSource {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    final XmlTileSourceConfig config;

    public XmlTileSource(XmlTileSourceConfig config){
        super(config.hostnames, config.port);
        this.config = config;
    }

    @Override
    public int getParallelRequestsLimit() {
        return config.parallelRequestsLimit;
    }

    @Override
    public URL getTileUrl(Tile tile) throws MalformedURLException {
        return getTileUrl(tile.zoomLevel, tile.tileX, tile.tileY);
    }

    private URL getTileUrl(byte zoomLevel, int tileX, int tileY) throws MalformedURLException {
        String urlPart = config.urlPart;
        urlPart = urlPart.replace("{x}", ""+tileX);
        urlPart = urlPart.replace("{y}", ""+tileY);
        urlPart = urlPart.replace("{z}", ""+zoomLevel);
        URL url = new URL(config.protocol, getHostName(), config.port, urlPart);
        mgLog.d("url="+url);
        return url;
    }


    @Override
    public byte getZoomLevelMax() {
        return config.zoomLevelMax;
    }

    @Override
    public byte getZoomLevelMin() {
        return config.zoomLevelMin;
    }

    @Override
    public boolean hasAlpha() {
        return true;
    }

    public URLConnection getURLConnection(byte zoomLevel, int tileX, int tileY) throws Exception{
        URL url = getTileUrl(zoomLevel, tileX, tileY);
        URLConnection conn = url.openConnection();
        if (config.connectTimeout > 0){
            conn.setConnectTimeout( config.connectTimeout );
        }
        if (config.connRequestProperties != null){
            for (String key : config.connRequestProperties.keySet()){
                conn.setRequestProperty(key,config.connRequestProperties.get(key));
            }
        }
        return conn;
    }

}
