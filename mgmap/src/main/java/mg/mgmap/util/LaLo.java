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
package mg.mgmap.util;

/** Provide conversion between microdegree and degree */
public class LaLo {

    public static int d2md(double d){
        return degrees2microdegrees(d);
    }
    private static int degrees2microdegrees(double degrees){
        return (int)Math.round(degrees*1000000);
    }
    public static double md2d(int md){
        return microdegrees2degrees(md);
    }
    private static double microdegrees2degrees(int microdegrees){
        return (microdegrees/1000000.0);
    }

}