package mg.mgmap.application.util;

import java.util.Locale;

public interface HgtProvider {

    byte[] getHgtBuf(String hgtName);

    static String getHgtName(int iLat, int iLon) {
        return String.format(Locale.GERMANY, "%s%02d%S%03d", (iLat > 0) ? "N" : "S", Math.abs(iLat), (iLon > 0) ? "E" : "W", Math.abs(iLon));
    }

    static int getLower(double d) {
        int shift = 1000;
        return ((int)(d+shift) - shift);
    }

}
