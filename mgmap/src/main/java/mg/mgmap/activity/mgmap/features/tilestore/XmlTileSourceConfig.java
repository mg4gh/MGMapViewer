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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class XmlTileSourceConfig {

    public record AutoFill(String urlPattern, String id, String value){}

    String name;
    String[] hostnames;
    String protocol;
    int port;
    byte zoomLevelMin;
    byte zoomLevelMax;
    int parallelRequestsLimit;
    long ttl;
    String urlPart;
    Map<String, String> connRequestProperties = null;
    int connectTimeout = -1;
    boolean storeTypeFiles = false;

    String cookiesDomain;
    String cookiesURL;
    String[] cookiesRequired;
    ArrayList<AutoFill> autoFills = new ArrayList<>();

    public XmlTileSourceConfig(String name){
        this.name = name;
    }

    public void setConnRequestProperty(String key, String value){
        if (connRequestProperties == null){
            connRequestProperties = new HashMap<>();
        }
        connRequestProperties.put(key,value);
    }
}
