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
package mg.mgmap.features.tilestore;

import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;

import java.io.InputStream;
import java.util.ArrayList;


public class XmlTileSourceConfigReader {




    private XmlPullParser pullParser = new KXmlParser();


    private String getStringAttribute(String name) {
        int n = pullParser.getAttributeCount();
        for (int i = 0; i < n; i++) {
            if (pullParser.getAttributeName(i).equals(name)) {
                return pullParser.getAttributeValue(i);
            }
        }
        return null;
    }

    @SuppressWarnings({"ConstantConditions"})
    public XmlTileSourceConfig parseXmlTileSourceConfig(String filename, InputStream inputStream) throws Exception{
        XmlTileSourceConfig config = null;
        String text = null;
        String qName;
        ArrayList<String> hostnameList = null;

        pullParser.setInput(inputStream, null);

        int eventType = pullParser.getEventType();
        do {
            qName = pullParser.getName();
            if (eventType == XmlPullParser.START_DOCUMENT) {
                config = new XmlTileSourceConfig(filename);
            } else if (eventType == XmlPullParser.START_TAG) {
                if ("hostnames".equals(qName)) {
                    hostnameList = new ArrayList<>();
                }
                text = null;

            } else if (eventType == XmlPullParser.END_TAG) {
                if ("hostname".equals(qName)) {
                    hostnameList.add(text);
                }
                if ("port".equals(qName)) {
                    config.port = Integer.parseInt(text);
                    if ((config.port <= 0) || (config.port > 65535)){
                        throw new IllegalArgumentException("port out of range. "+config.port);
                    }
                }
                if ("urlPart".equals(qName)) {
                    config.urlPart = text;
                }
                if ("parallelRequestsLimit".equals(qName)) {
                    config.parallelRequestsLimit = Integer.parseInt(text);
                    if ((config.parallelRequestsLimit <= 0) || (config.parallelRequestsLimit > 50)){
                        throw new IllegalArgumentException("parallelRequestsLimit out of range. "+config.parallelRequestsLimit);
                    }
                }
                if ("protocol".equals(qName)) {
                    config.protocol = text;
                }
                if ("zoomLevelMin".equals(qName)) {
                    config.zoomLevelMin = Byte.parseByte(text);
                    if ((config.zoomLevelMin <= 0) || (config.zoomLevelMin > 10)){
                        throw new IllegalArgumentException("zoomLevelMin out of range. "+config.zoomLevelMin);
                    }
                }
                if ("zoomLevelMax".equals(qName)) {
                    config.zoomLevelMax = Byte.parseByte(text);
                    if ((config.zoomLevelMax <= 0) || (config.zoomLevelMax > 30)){
                        throw new IllegalArgumentException("zoomLevelMax out of range. "+config.zoomLevelMax);
                    }
                }
                if ("ttl".equals(qName)) {
                    config.ttl = Long.parseLong(text);
                }
            } else if (eventType == XmlPullParser.TEXT) {
                text = (text==null)?pullParser.getText():text+pullParser.getText();
            }
            eventType = pullParser.next();
        } while (eventType != XmlPullParser.END_DOCUMENT);
        config.hostnames = new String[hostnameList.size()];
        hostnameList.toArray(config.hostnames);
        return config;
    }

}
