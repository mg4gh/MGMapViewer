package mg.mgmap.activity.mgmap.features.search;

import org.junit.Test;

import java.lang.invoke.MethodHandles;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.util.basic.MGLog;

public class GeoTest {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    @Test
    public void _00_geoSearch() {

        MGLog.logConfig.put("mg.mgmap", MGLog.Level.DEBUG);
        MGLog.setUnittest(true);

        String sPos = match( "geo:54.317003,13.350003?z=16&q=54.317003,13.350003");
        mgLog.d(sPos);
        sPos = match( "geo:0,0?z=16&q=54.317003,13.350003");
        mgLog.d(sPos);

    }

    /** @noinspection RegExpRedundantEscape, ReassignedVariable, DataFlowIssue */
    private String match(String sUri){
        // possible patterns are (according to https://developer.android.com/guide/components/intents-common#java)
        // geo:latitude,longitude
        // geo:latitude,longitude?z=zoom
        // geo:0,0?q=lat,lng(label)
        // geo:0,0?q=my+street+address

        String d = "(\\-?\\d*\\.?\\d+)";
        Pattern p1 = Pattern.compile("geo:"+d+","+d);
        Pattern p2 = Pattern.compile("geo:"+d+","+d+"\\?(q="+d+", ?"+d+"&)?z=([12]?[0-9])(&q="+d+", ?"+d+")?");
        Pattern p3 = Pattern.compile("geo:"+d+","+d+"\\?q="+d+", ?"+d+"(\\(([^\\)]+)\\))?");
        Pattern p4 = Pattern.compile("geo:0,0\\?q=(.*)");

        double lat = PointModel.NO_LAT_LONG;
        double lon = PointModel.NO_LAT_LONG;
        byte zoom = 0; // no zoom
        String label = "";
        String qString = null;
        Matcher m = p1.matcher(sUri);
        if (m.matches()){
            mgLog.i("p1 matched");
            lat = Double.parseDouble(m.group(1));
            lon = Double.parseDouble(m.group(2));
        } else {
            m = p2.matcher(sUri);
            if (m.matches()){
                mgLog.i("p2 matched");
                lat = Double.parseDouble(m.group(1));
                lon = Double.parseDouble(m.group(2));
                if ((lat == 0) && (lon == 0) && (m.group(4) != null) && (m.group(5) != null)){
                    lat = Double.parseDouble(m.group(4));
                    lon = Double.parseDouble(m.group(5));
                }
                zoom = Byte.parseByte(m.group(6));
                zoom = (byte)Math.max(Math.min(zoom, 22), 6);
            } else {
                m = p3.matcher(sUri);
                if (m.matches()){
                    mgLog.i("p3 matched");
                    lat = Double.parseDouble(m.group(3));
                    lon = Double.parseDouble(m.group(4));
                    if ((m.groupCount()>=6) && (m.group(6)!=null)){
                        label = m.group(6);
                    }
                } else {
                    m = p4.matcher(sUri);
                    if (m.matches()){
                        mgLog.i("p4 matched");
                        qString = m.group(1);
                    }
                }
            }
        }
        SearchPos searchPos = new SearchPos(lat,lon);
        searchPos.setZoom(zoom);
        searchPos.setLabel(label);

        return searchPos+" "+qString;
    }

}
