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

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.application.util.PersistenceManager;
import mg.mgmap.generic.util.basic.NameUtil;

/**
 * Utility to catch uncaught exception and to protocol the in a stacktrace file.
 * Try to write the stacktrace to the log folder, otherwise to the download path.
 */
public class TopExceptionHandler implements Thread.UncaughtExceptionHandler {

    private final Thread.UncaughtExceptionHandler defaultUEH;
    private final PersistenceManager persistenceManager;

    public TopExceptionHandler(PersistenceManager persistenceManager) {
        this.defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
        this.persistenceManager = persistenceManager;
    }

    public void uncaughtException(Thread t, Throwable e) {
        StackTraceElement[] arr = e.getStackTrace();
        String report = e.toString()+"\n\n";
        report += "--------- Stack trace ---------\n\n";
        for (StackTraceElement ste : arr) {
            report += "    "+ste.toString()+"\n";
        }
        report += "-------------------------------\n\n";

        // If the exception was thrown in a background thread inside
        // AsyncTask, then the actual exception can be found with getCause

        report += "--------- Cause ---------\n\n";
        Throwable cause = e.getCause();
        if(cause != null) {
            report += cause.toString() + "\n\n";
            arr = cause.getStackTrace();
            for (StackTraceElement ste : arr) {
                report += "    "+ste.toString()+"\n";
            }
        }
        report += "-------------------------------\n\n";


        try {
            File dir = null;
            try {
                if (persistenceManager.getLogDir().canWrite()){
                    dir = persistenceManager.getLogDir();
                } else {
                    dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                }
            } catch (Exception ex) {
                Log.e(MGMapApplication.LABEL, NameUtil.context());
            }
            if (dir != null){
                File file = new File(dir,"stacktrace_"+System.currentTimeMillis()+".txt");
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(report.getBytes());
                fos.close();
            }
        } catch (IOException ex) {
            Log.e(MGMapApplication.LABEL, NameUtil.context());
        }
        defaultUEH.uncaughtException(t, e);
    }
}