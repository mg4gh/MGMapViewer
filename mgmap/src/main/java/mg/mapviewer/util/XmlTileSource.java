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
package mg.mapviewer.util;

import android.util.Log;

import org.mapsforge.core.model.Tile;
import org.mapsforge.map.layer.download.tilesource.AbstractTileSource;

import java.net.MalformedURLException;
import java.net.URL;

import mg.mapviewer.MGMapApplication;

public class XmlTileSource extends AbstractTileSource {

    XmlTileSourceConfig config;

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
        String urlPart = config.urlPart;
        urlPart = urlPart.replace("{x}", ""+tile.tileX);
        urlPart = urlPart.replace("{y}", ""+tile.tileY);
        urlPart = urlPart.replace("{z}", ""+tile.zoomLevel);
        URL url = new URL(config.protocol, getHostName(), config.port, urlPart);
        Log.i(MGMapApplication.LABEL, NameUtil.context()+ " url="+url);
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
        return false;
    }

}
