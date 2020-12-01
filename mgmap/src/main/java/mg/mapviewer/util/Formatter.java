package mg.mapviewer.util;

import java.text.SimpleDateFormat;
import java.util.Locale;

import mg.mapviewer.model.PointModel;

public class Formatter {

    private static final SimpleDateFormat sdf2 = new SimpleDateFormat(" HH:mm", Locale.GERMANY);

    public enum FormatType {FORMAT_TIME, FORMAT_DISTANCE, FORMAT_DURATION, FORMAT_DAY, FORMAT_INT, FORMAT_HEIGHT, FORMAT_STRING};

    private FormatType formatType;

    public Formatter(FormatType formatType){
        this.formatType = formatType;
    }

    public String format(Object value){
        return format(formatType, value);
    }

    public static String format(FormatType formatType, Object value) {
        String text = "";
        if (formatType == FormatType.FORMAT_TIME) {
            long millis = (Long) value;
            if (millis > 0) {
                text = sdf2.format(millis);
            }
        } else if (formatType == FormatType.FORMAT_INT) {
            int iValue = (Integer) value;
            text = Integer.toString(iValue);
        } else if (formatType == FormatType.FORMAT_DISTANCE) {
            double distance = (Double) value;
            text = (distance == 0) ? "" : String.format(Locale.ENGLISH, " %.2f km", distance / 1000.0);
        } else if (formatType == FormatType.FORMAT_HEIGHT) {
            float height = (Float) value;
            text = (height == PointModel.NO_ELE) ? "" : String.format(Locale.ENGLISH, " %.1f m", height);
        } else if (formatType == FormatType.FORMAT_STRING) {
            text = value.toString();
        } else if (formatType == FormatType.FORMAT_DURATION) {
            long duration = (Long) value;
            long seconds = duration / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            seconds -= minutes * 60;
            minutes -= hours * 60;
            text = String.format(Locale.ENGLISH, "%d:%02d:%02d", hours, minutes, seconds);
        }
        return text;
    }
}
