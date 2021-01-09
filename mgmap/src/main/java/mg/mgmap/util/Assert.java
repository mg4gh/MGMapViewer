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

import android.util.Log;

import mg.mgmap.BuildConfig;
import mg.mgmap.MGMapApplication;

/**
 * Private Assert implementation.
 */
public class Assert {


    public static void check(boolean condition) {
        if (BuildConfig.DEBUG && !condition){
            Log.w(MGMapApplication.LABEL, NameUtil.context()+ " Assert violation.");
//            throw new RuntimeException("Assert violation.");
        }
    }
    public static void check(boolean condition, String description) {
        if (BuildConfig.DEBUG && !condition){
            Log.w(MGMapApplication.LABEL, NameUtil.context()+ " Assert violation.");
//            throw new RuntimeException("Assert violation: "+description);
        }
    }




}
