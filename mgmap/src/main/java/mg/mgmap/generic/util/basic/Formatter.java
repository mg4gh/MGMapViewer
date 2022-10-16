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
package mg.mgmap.generic.util.basic;

import android.graphics.Paint;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.model.PointModel;

public class Formatter {

    public static final SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.GERMANY);
    public static final SimpleDateFormat SDF1a = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY);
    public static final SimpleDateFormat SDF1b = new SimpleDateFormat("dd.MM.yy", Locale.GERMANY);
    public static final SimpleDateFormat SDF1c = new SimpleDateFormat("dd.MM", Locale.GERMANY);
    public static final SimpleDateFormat SDF2 = new SimpleDateFormat("HH:mm", Locale.GERMANY);
    public static final SimpleDateFormat SDF3 = new SimpleDateFormat("HH:mm:ss.SSS", Locale.GERMANY);

    public enum FormatType {FORMAT_TIME, FORMAT_DISTANCE, FORMAT_DURATION, FORMAT_DATE, FORMAT_INT, FORMAT_HEIGHT, FORMAT_STRING, FORMAT_TIMESTAMP}

    private final FormatType formatType;

    public Formatter(FormatType formatType){
        this.formatType = formatType;
    }

    public String format(Object value){
        return format(formatType, value);
    }

    public static String format(FormatType formatType, Object value) {
        return format(formatType,value, null, 0);
    }
    public static String format(FormatType formatType, Object value, Paint paint, int availableWidth) {
        String text = "";
        if (formatType == FormatType.FORMAT_TIME) {
            long millis = (Long) value;
            if (millis > 0) {
                text = SDF2.format(millis);
            }
        } else if (formatType == FormatType.FORMAT_DATE) {
            long millis = (Long) value;
            if (millis > 0) {
                ArrayList<SimpleDateFormat> formats = new ArrayList<>();
                formats.add(SDF1a);formats.add(SDF1b);formats.add(SDF1c);
                if ((paint!=null) && (availableWidth>0)){
                    for (SimpleDateFormat sfdFormat : formats){
                        String textCandidate = sfdFormat.format(millis);
                        Log.v(MGMapApplication.LABEL, NameUtil.context()+ "measuredFormat=\""+sfdFormat.toPattern()+"\" text=\""+text+"\""+" measuredWidth="+paint.measureText( textCandidate )+" availableWidth="+availableWidth);
                        if (paint.measureText( textCandidate ) <= availableWidth){
                            text = textCandidate;
                            Log.v(MGMapApplication.LABEL, NameUtil.context()+ "selectedFormat=\""+sfdFormat.toPattern()+"\" text=\""+text+"\"");
                            break;
                        }
                    }
                }
                if ("".equals(text)) {
                    text = formats.get(1).format(millis);
                }
            }
        } else if (formatType == FormatType.FORMAT_TIMESTAMP) {
            long millis = (Long) value;
            if (millis > 0) {
                text = SDF3.format(millis);
            }
        } else if (formatType == FormatType.FORMAT_INT) {
            int iValue = (Integer) value;
            text = Integer.toString(iValue);
        } else if (formatType == FormatType.FORMAT_DISTANCE) {
            double distance = (Double) value;
            if (distance < 0){
                text = "";
            } else {
                if ((paint!=null) && (availableWidth>0)){
                    String[] formats = new String[]{"%.2f km", "%.1f km", "%.1fkm", "%.1f", "%.0f"};
                    for (String format : formats){
                        String textCandidate = String.format(Locale.ENGLISH, format, distance / 1000.0);
                        Log.v(MGMapApplication.LABEL, NameUtil.context()+ "measuredFormat=\""+format+"\" text=\""+text+"\""+" measuredWidth="+paint.measureText( textCandidate )+" availableWidth="+availableWidth);
                        if (paint.measureText( textCandidate ) <= availableWidth){
                            text = textCandidate;
                            Log.v(MGMapApplication.LABEL, NameUtil.context()+ "selectedFormat=\""+format+"\" text=\""+text+"\"");
                            break;
                        }
                    }
                }
                if ("".equals(text)) {
                    text = String.format(Locale.ENGLISH, "%.1f km", distance / 1000.0);
                }
            }
        } else if (formatType == FormatType.FORMAT_HEIGHT) {
            float height = (Float) value;
            if (Math.abs(height) >= Math.abs(PointModel.NO_ELE)){
                text = "";
            } else {
                if ((paint!=null) && (availableWidth>0)){
                    String[] formats = new String[]{"%.1f m", "%.0f m", "%.0fm", "%.0f"};
                    for (String format : formats){
                        String textCandidate = String.format(Locale.ENGLISH, format, height);
                        Log.v(MGMapApplication.LABEL, NameUtil.context()+ "measuredFormat=\""+format+"\" text=\""+text+"\""+" measuredWidth="+paint.measureText( textCandidate )+" availableWidth="+availableWidth);
                        if (paint.measureText( textCandidate ) <= availableWidth){
                            text = textCandidate;
                            Log.v(MGMapApplication.LABEL, NameUtil.context()+ "selectedFormat=\""+format+"\" text=\""+text+"\"");
                            break;
                        }
                    }
                }
                if ("".equals(text)) {
                    text = String.format(Locale.ENGLISH, "%.1f km", height);
                }
            }
         } else if (formatType == FormatType.FORMAT_STRING) {
            Log.v(MGMapApplication.LABEL, NameUtil.context()+ "measuredFormat=String text=\""+text+"\""+" measuredWidth="+paint.measureText( text )+" availableWidth="+availableWidth);
            text = value.toString();
        } else if (formatType == FormatType.FORMAT_DURATION) {
            long duration = (Long) value;
            long seconds = duration / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            seconds -= minutes * 60;
            minutes -= hours * 60;
            String textCandidate1 = String.format(Locale.ENGLISH, "%d:%02d:%02d", hours, minutes, seconds);
            String textCandidate2 = String.format(Locale.ENGLISH, "%d:%02d", hours, minutes);

            if ((paint!=null) && (availableWidth>0)){
                if (paint.measureText( textCandidate1 ) <= availableWidth){
                    text = textCandidate1;
                    Log.v(MGMapApplication.LABEL, NameUtil.context()+ "selectedFormat=%d:%02d:%02d text=\""+text+"\"");
                }
            }
            if ("".equals(text)) {
                text = textCandidate2;
                Log.v(MGMapApplication.LABEL, NameUtil.context()+ "selectedFormat=%d:%02d text=\""+text+"\"");
            }
        }

//        // if text is still too large, make font smaller - will be restored on layout change
//        if ((paint!=null) && (availableWidth>0) ){
//            while ((paint.measureText(text) > availableWidth) && (paint.getTextSize()>20)){
//                paint.setTextSize( paint.getTextSize() * 0.95f );
//                Log.v(MGMapApplication.LABEL, NameUtil.context()+ " text=\""+text+"\""+" measuredWidth="+paint.measureText( text )+" availableWidth="+availableWidth);
//            }
//        }

        return text;
    }
}
