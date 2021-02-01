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
package mg.mgmap.util;

import java.text.SimpleDateFormat;
import java.util.Locale;

import mg.mgmap.model.PointModel;

public class Formatter {

    public static final SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.GERMANY);
    public static final SimpleDateFormat SDF1 = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY);
    public static final SimpleDateFormat SDF2 = new SimpleDateFormat("HH:mm", Locale.GERMANY);

    public enum FormatType {FORMAT_TIME, FORMAT_DISTANCE, FORMAT_DURATION, FORMAT_DATE, FORMAT_INT, FORMAT_HEIGHT, FORMAT_STRING};

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
                text = SDF2.format(millis);
            }
        } else if (formatType == FormatType.FORMAT_DATE) {
            long millis = (Long) value;
            if (millis > 0) {
                text = SDF1.format(millis);
            }
        } else if (formatType == FormatType.FORMAT_INT) {
            int iValue = (Integer) value;
            text = Integer.toString(iValue);
        } else if (formatType == FormatType.FORMAT_DISTANCE) {
            double distance = (Double) value;
            if (distance < 0){
                text = "";
            } else {
                text = String.format(Locale.ENGLISH, " %.2f" + ((distance < 100000) ? " " : "") + "km", distance / 1000.0);
            }
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
