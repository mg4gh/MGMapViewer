package mg.mgmap.activity.mgmap.features.shareloc;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.model.WriteablePointModel;
import mg.mgmap.generic.model.WriteablePointModelImpl;
import mg.mgmap.generic.util.basic.MGLog;

public class LocationMessage {

    static String toMessage(PointModel pm){
        String s =  String.format(Locale.ENGLISH, "%.6f:%.6f:%.1f:%d", pm.getLat(), pm.getLon(), pm.getEle(), pm.getTimestamp());
        MGLog.si("\""+s+"\"");

        byte[] hash = s.getBytes();
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        MGLog.si(hexString.toString());
        return s;
    }

    static PointModel fromMessage(String message){
        String[] parts = message.split(":");
        if (parts.length >= 4){
            double lat = Double.parseDouble(parts[0]);
            double lon = Double.parseDouble(parts[1]);
            float ele = Float.parseFloat(parts[2]);
            long timestamp = Long.parseLong(parts[3]);
            WriteablePointModel wpm = new WriteablePointModelImpl(lat, lon);
            wpm.setEle(ele);
            wpm.setTimestamp(timestamp);
            return wpm;
        }
        return null;
    }
}
