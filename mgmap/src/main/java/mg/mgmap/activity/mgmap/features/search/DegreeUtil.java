package mg.mgmap.activity.mgmap.features.search;

import android.annotation.SuppressLint;

import java.util.Locale;

public class DegreeUtil {

    public static double degree2double(boolean isLatitude, String gms) throws NumberFormatException{
        if (gms == null) throw new NumberFormatException();

        int degree;
        int negative = 1;
        double minute = 0;
        double second = 0;

        gms = gms.trim();

        if ((isLatitude && gms.startsWith("N")) || (!isLatitude && gms.startsWith("E"))){
            gms = gms.substring(1);
        }
        if ((isLatitude && gms.startsWith("S")) || (!isLatitude && gms.startsWith("W"))){
            gms = gms.substring(1);
            negative = -1;
        }
        gms = gms.trim();
        gms = gms.replaceFirst("[dg]","°");
        gms = gms.replaceFirst("m","'");
        gms = gms.replaceFirst("s","''");

        String[] degrees = gms.split("°");
        if (degrees.length > 2) throw new NumberFormatException();
        degree = Integer.parseInt(degrees[0]);
        if ((degree < 0) || ( (isLatitude?90:180) <= degree)) throw new NumberFormatException();
        if (degrees.length == 2) { // assume also minutes
            String[] minutes = degrees[1].split("'");
            if (minutes.length == 1){ // only minutes ==> then also fractions of minutes are allowed
                minute = Double.parseDouble(minutes[0]);
                if ((minute < 0) || (60 <= minute)) throw new NumberFormatException();
            } else { // assume also seconds, but accept no fraction for minutes
                minute = Integer.parseInt(minutes[0]);
                if ((minute < 0) || (60 <= minute)) throw new NumberFormatException();
                second = Double.parseDouble(minutes[1]);
                if ((second < 0) || (60 <= second)) throw new NumberFormatException();
                if (minutes.length > 4) throw new NumberFormatException();
                if ((minutes.length > 2) && ((!minutes[2].isEmpty()) || (!minutes[3].isEmpty()))) throw new NumberFormatException();
            }
        }
        return negative * (degree + ( minute / 60.0 ) + ( second / 3600.0 ) );
    }

    public static String double2Degree(boolean isLatitude, double value, boolean onlyMinutes){
        if ((isLatitude?90:180) <= Math.abs(value)) throw new NumberFormatException();
        String prefix = isLatitude?((value < 0)?"S":"N"):((value < 0)?"W":"E");
        value = Math.abs(value);
        double degree = Math.floor(value);
        double valueMinutes = (value -degree)*60;
        if (onlyMinutes){
            String format = isLatitude?"%s %02d°%2.3f'":"%s %03d°%2.3f'";
            return String.format(Locale.ENGLISH, format, prefix, (int)degree, valueMinutes );
        } else {
            double minutes = Math.floor(valueMinutes);
            double seconds = Math.min((valueMinutes -minutes) * 60, 59.99 );
            String format = isLatitude?"%s %02d°%d'%2.2f''":"%s %03d°%d'%2.2f''";
            return String.format(Locale.ENGLISH, format, prefix, (int)degree, (int)minutes, seconds );
        }
    }

    public static double doubleDegree2double(boolean isLatitude, String doubleDegree){
        double degree = Double.parseDouble(doubleDegree);
        if ((isLatitude?90:180) <= Math.abs(degree)) throw new NumberFormatException();
        return degree;
    }


    @SuppressWarnings("RedundantStringFormatCall")
    @SuppressLint("DefaultLocale")
    public static void main(String[] args) {
        System.out.println( String.format("%2.6f", DegreeUtil.degree2double(true,"19") ));
        System.out.println( String.format("%2.6f", DegreeUtil.degree2double(true,"N19°") ));
        System.out.println( String.format("%2.6f", DegreeUtil.degree2double(true,"S19°") ));
        System.out.println( String.format("%2.6f", DegreeUtil.degree2double(true,"19°30'") ));
        System.out.println( String.format("%2.6f", DegreeUtil.degree2double(true,"19°30'23'' ") ));
        System.out.println( String.format("%2.6f", DegreeUtil.degree2double(true,"19°30'23.25'' ") ));
        System.out.println( String.format("%2.6f", DegreeUtil.degree2double(true,"19°30'23.26'' ") ));
        System.out.println( String.format("%2.6f", DegreeUtil.degree2double(true,"18g30m23.26s") ));
        double x1 = DegreeUtil.degree2double(true,"19g30m23.26s") ;
        System.out.println( double2Degree(true,x1, false) );
        System.out.println( double2Degree(false,x1, false) );

        System.out.println( String.format("%2.6f", DegreeUtil.degree2double(false,"19°30'23'' ") ));
        System.out.println( String.format("%2.6f", DegreeUtil.degree2double(false,"91°30'23'' ") ));
        System.out.println( String.format("%2.6f", DegreeUtil.degree2double(true,"22°30m23'' ") ));

        System.out.println(":"+" ab c".split(" ")[0]+":");
        System.out.println(":"+"E 03".replaceAll("([EWNS]) ","$1"));
    }
}
