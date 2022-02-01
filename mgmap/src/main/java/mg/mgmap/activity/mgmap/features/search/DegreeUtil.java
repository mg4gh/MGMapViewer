package mg.mgmap.activity.mgmap.features.search;

import java.util.Locale;

public class DegreeUtil {

    public static double degree2double(boolean isLatitude, String gms) throws NumberFormatException{
        if (gms == null) throw new NumberFormatException();

        int degree = 0, minute = 0, negative = 1;
        double second = 0;

        gms = gms.trim();

        if ((isLatitude && gms.endsWith("N")) || (!isLatitude && gms.endsWith("E"))){
            gms = gms.substring(0,gms.length()-1);
        }
        if ((isLatitude && gms.endsWith("S")) || (!isLatitude && gms.endsWith("W"))){
            gms = gms.substring(0,gms.length()-1);
            negative = -1;
        }
        gms = gms.replaceFirst("[dg]","°");
        gms = gms.replaceFirst("[m]","'");
        gms = gms.replaceFirst("[s]","''");

        String[] degrees = gms.split("°");
        if (degrees.length > 2) throw new NumberFormatException();
        degree = Integer.parseInt(degrees[0]);
        if ((degree < 0) || ( (isLatitude?90:180) <= degree)) throw new NumberFormatException();
        if (degrees.length == 2) { // assume also minutes
            String[] minutes = degrees[1].split("'");
            minute = Integer.parseInt(minutes[0]);
            if ((minute < 0) || (60 <= minute)) throw new NumberFormatException();
            if (minutes.length >= 2){ // assume also seconds
                second = Double.parseDouble(minutes[1]);
                if ((second < 0) || (60 <= second)) throw new NumberFormatException();
                if (minutes.length > 4) throw new NumberFormatException();
                if ((minutes.length > 2) && ((minutes[2].length() > 0) || (minutes[3].length() > 0))) throw new NumberFormatException();
            }
        }
        return negative * (degree + ( minute / 60.0 ) + ( second / 3600.0 ) );
    }

    public static String double2Degree(boolean isLatitude, double lat){
        if ((isLatitude?90:180) <= Math.abs(lat)) throw new NumberFormatException();
        String suffix = isLatitude?((lat < 0)?"S":"N"):((lat < 0)?"W":"E");
        lat = Math.abs(lat);
        double degree = Math.floor(lat);
        double latMinutes = (lat -degree)*60;
        double minutes = Math.floor(latMinutes);
        double seconds = Math.min((latMinutes -minutes) * 60, 59.99 );
        return String.format(Locale.ENGLISH, "%d°%d'%2.2f''%s",(int)degree, (int)minutes, seconds, suffix);
    }


    public static void main(String[] args) {
        System.out.println( String.format("%2.6f", DegreeUtil.degree2double(true,"19") ));
        System.out.println( String.format("%2.6f", DegreeUtil.degree2double(true,"19°N") ));
        System.out.println( String.format("%2.6f", DegreeUtil.degree2double(true,"19°S") ));
        System.out.println( String.format("%2.6f", DegreeUtil.degree2double(true,"19°30'") ));
        System.out.println( String.format("%2.6f", DegreeUtil.degree2double(true,"19°30'23'' ") ));
        System.out.println( String.format("%2.6f", DegreeUtil.degree2double(true,"19°30'23.25'' ") ));
        System.out.println( String.format("%2.6f", DegreeUtil.degree2double(true,"19°30'23.26'' ") ));
        System.out.println( String.format("%2.6f", DegreeUtil.degree2double(true,"18g30m23.26s") ));
        double x1 = DegreeUtil.degree2double(true,"19g30m23.26s") ;
        System.out.println( double2Degree(true,x1) );
        System.out.println( double2Degree(false,x1) );

        System.out.println( String.format("%2.6f", DegreeUtil.degree2double(false,"19°30'23'' ") ));
        System.out.println( String.format("%2.6f", DegreeUtil.degree2double(false,"91°30'23'' ") ));
        System.out.println( String.format("%2.6f", DegreeUtil.degree2double(true,"22°30m23'' ") ));
    }
}
