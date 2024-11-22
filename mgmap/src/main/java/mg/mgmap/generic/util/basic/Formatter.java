/*
 * Copyright 2017 - 2022 mg4gh
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

import java.util.Locale;

import mg.mgmap.generic.model.PointModel;

public class Formatter {

    public static final SDFSynced SDF = new SDFSynced("yyyyMMdd_HHmmss", Locale.GERMANY);
    public static final SDFSynced SDF1a = new SDFSynced("dd.MM.yyyy", Locale.GERMANY);
    public static final SDFSynced SDF1b = new SDFSynced("dd.MM.yy", Locale.GERMANY);
    public static final SDFSynced SDF1c = new SDFSynced("dd.MM", Locale.GERMANY);
    public static final SDFSynced SDF2 = new SDFSynced("HH:mm", Locale.GERMANY);
    public static final SDFSynced SDF3 = new SDFSynced("HH:mm:ss.SSS", Locale.GERMANY);
    public static final SDFSynced SDF_TL = new SDFSynced("dd.MM.yyyy_HH:mm:ss", Locale.GERMANY);
    public static final SDFSynced SDF_LOG = new SDFSynced("yyyy-MM-dd HH:mm:ss.SSS", Locale.GERMANY);
    public static final SDFSynced SDF_GPX = new SDFSynced("yyyy-MM-dd_HH:mm:ss", Locale.GERMANY);

    public enum FormatType {FORMAT_TIME, FORMAT_DISTANCE, FORMAT_DURATION, FORMAT_DATE, FORMAT_INT, FORMAT_HEIGHT, FORMAT_STRING, FORMAT_TIMESTAMP, FORMAT_FILE_SIZE, FORMAT_FILE_SIZE_DIR, FORMAT_FILE_TS}

    private final FormatType formatType;

    // cache width of digits, space, etc
    static private float lastTextSize = 0;
    static private float lastDigitWidth = 0;
    static private float lastSpaceWidth = 0;
    static private float lastPointWidth = 0;
    static private float lastColonWidth = 0;
    static private float lastmWidth = 0;
    static private float lastkmWidth = 0;


    public Formatter(FormatType formatType){
        this.formatType = formatType;
    }

    public String format(Object value){
        return format(formatType, value);
    }

    static private void initLastWidth(Paint paint){
        if (lastTextSize != paint.getTextSize()){
            lastTextSize = paint.getTextSize();
            lastDigitWidth = paint.measureText("0");
            lastSpaceWidth = paint.measureText(" ");
            lastPointWidth = paint.measureText(".");
            lastColonWidth = paint.measureText(":");
            lastmWidth = paint.measureText("m");
            lastkmWidth = paint.measureText("km");
        }
    }

    static private int getIntDigits(double d){
        int digits = 1;
        int i = (int)d;
        while (i >= 10){
            i = i/10;
            digits++;
        }
        return digits;
    }

    public static String format(FormatType formatType, Object value) {
        return format(formatType,value, null, 0);
    }
    public static String format(FormatType formatType, Object value, Paint paint, int availableWidth) {
        String text = "";
        if (paint != null){
            initLastWidth(paint);
        }
        if (formatType == FormatType.FORMAT_TIME) {
            long millis = (Long) value;
            if (millis > 0) {
                text = SDF2.format(millis);
            }
        } else if (formatType == FormatType.FORMAT_DATE) {
            long millis = (Long) value;
            if (millis > 0) {
                SDFSynced sdf = SDF1c;
                if (6*lastDigitWidth+2*lastPointWidth < availableWidth){
                    if (8*lastDigitWidth+2*lastPointWidth < availableWidth){
                        sdf = SDF1a;
                    } else {
                        sdf = SDF1b;
                    }
                }
                text = sdf.format(millis);
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
                String distanceFormat = "%.1f km";
                if ((paint!=null) && (availableWidth>0)) {
                    int intDigits = getIntDigits(distance/1000);
                    if ((intDigits + 2) * lastDigitWidth + lastPointWidth + lastSpaceWidth + lastkmWidth < availableWidth) {
                        distanceFormat = "%.2f km";
                    } else if ((intDigits + 1) * lastDigitWidth + lastPointWidth + lastSpaceWidth + lastkmWidth < availableWidth) {
                        distanceFormat = "%.1f km";
                    } else if ((intDigits + 1) * lastDigitWidth + lastPointWidth + lastkmWidth < availableWidth) {
                        distanceFormat = "%.1fkm";
                    } else if ((intDigits + 1) * lastDigitWidth + lastPointWidth < availableWidth) {
                        distanceFormat = "%.1f";
                    } else {
                        distanceFormat = "%.0f";
                    }
                }
                text = String.format(Locale.ENGLISH, distanceFormat, distance / 1000.0);
            }
        } else if (formatType == FormatType.FORMAT_HEIGHT) {
            float height = (Float) value;
            if (Math.abs(height) >= Math.abs(PointModel.NO_ELE)){
                text = "";
            } else {
                String heightFormat = "%.1f m";
                if ((paint!=null) && (availableWidth>0)) {
                    int intDigits = getIntDigits(height);
                    if ((intDigits) * lastDigitWidth + lastSpaceWidth + lastmWidth < availableWidth) {
                        heightFormat = "%.0f m";
                    } else if ((intDigits) * lastDigitWidth + lastmWidth < availableWidth) {
                        heightFormat = "%.0fm";
                    } else {
                        heightFormat = "%.0f";
                    }
                }
                text = String.format(Locale.ENGLISH, heightFormat, height);
            }
        } else if (formatType == FormatType.FORMAT_STRING) {
            text = value.toString();
        } else if (formatType == FormatType.FORMAT_DURATION) {
            long duration = (Long) value;
            long seconds = duration / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            seconds -= minutes * 60;
            minutes -= hours * 60;
            if ((paint!=null) && (availableWidth>0)){
                if ( (getIntDigits(hours)+4)*lastDigitWidth + 2*lastColonWidth < availableWidth ){
                    text = String.format(Locale.ENGLISH, "%d:%02d:%02d", hours, minutes, seconds);
                }
            }
            if (text.isEmpty()) {
                text = String.format(Locale.ENGLISH, "%d:%02d", hours, minutes);
            }
        } else if (formatType == FormatType.FORMAT_FILE_SIZE) {
            long size = (Long) value;
            if (size > 1000000000){
                text = String.format(Locale.ENGLISH,"%.1f GB",size/1000000000.0);
            } else if (size > 1000000){
                text = String.format(Locale.ENGLISH,"%.1f MB",size/1000000.0);
            } else if (size > 1000){
                text = String.format(Locale.ENGLISH,"%.1f KB",size/1000.0);
            } else {
                text = size+" B";
            }
        } else if (formatType == FormatType.FORMAT_FILE_SIZE_DIR) {
            long size = (Long) value;
            if (size == 0){
                text = "empty";
            } else if (size == 1){
                text = "1 entry";
            } else {
                text = size+" entries";
            }
        } else if (formatType == FormatType.FORMAT_FILE_TS) {
            long timestamp = (Long) value;
            text = SDF1b.format(timestamp)+" "+SDF2.format(timestamp);
        }
        return text;
    }
}
