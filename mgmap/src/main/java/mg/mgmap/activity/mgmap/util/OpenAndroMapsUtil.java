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
package mg.mgmap.activity.mgmap.util;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.util.ArrayList;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.application.util.PersistenceManager;
import mg.mgmap.generic.util.BgJob;
import mg.mgmap.generic.util.Zipper;
import mg.mgmap.generic.util.basic.NameUtil;

public class OpenAndroMapsUtil {

    public static ArrayList<BgJob> createBgJobsFromIntentUriMap(PersistenceManager persistenceManager, Uri uri) throws Exception {
        ArrayList<BgJob> jobs = new ArrayList();
        BgJob job = new BgJob(){
            @Override
            protected void doJob() throws Exception {
                Zipper zipper = new Zipper(null);
                String s1 = uri.toString();
                Log.i(MGMapApplication.LABEL, NameUtil.context()+"  s1="+s1);
                String s2 = s1.replaceFirst("mf-v4-map", ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)?"https":"http"));
//                String s2 = s1.replaceFirst("mf-v4-map://download.openandromaps.org/", ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)?"https":"http")+"://ftp.gwdg.de/pub/misc/openstreetmap/openandromaps/");
                Log.i(MGMapApplication.LABEL, NameUtil.context()+"  s2="+s2);
                URL url = new URL(s2);
                FilenameFilter filter = new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        if (name.endsWith("map")) return true;
                        if (name.endsWith("poi")) return true;
                        return false;
                    }
                };
                zipper.unpack(url, persistenceManager.getMapsforgeDir(), filter, this);

            }
        };
        jobs.add(job);
        return jobs;
    }

    public static ArrayList<BgJob> createBgJobsFromIntentUriTheme(PersistenceManager persistenceManager, Uri uri) throws Exception {
        ArrayList<BgJob> jobs = new ArrayList();
        BgJob job = new BgJob() {
            @Override
            protected void doJob() throws Exception {
                super.doJob();
                Zipper zipper = new Zipper(null);
                String s1 = uri.toString();
                String s2 = s1.replaceFirst("mf-theme", "https");
//                  URL url = new URL(s2);  //unfortunately this doesn't work
//                URL url = new URL(((Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)?"https":"http")+"://www.openandromaps.org/wp-content/users/tobias/Elevate.zip");
                URL url = new URL("https://www.openandromaps.org/wp-content/users/tobias/Elevate.zip");
                zipper.unpack(url, persistenceManager.getThemesDir(), null, this);
            }
        };
        jobs.add(job);
        return jobs;
    }

    public static ArrayList<BgJob> createBgJobsFromAssetTheme(PersistenceManager persistenceManager, AssetManager assetManager) {
        ArrayList<BgJob> jobs = new ArrayList();
        BgJob job = new BgJob() {
            @Override
            protected void doJob() throws Exception {
                super.doJob();
                Zipper zipper = new Zipper(null);
//                AssetFileDescriptor fd = assetManager.openFd("Elevate.zip");
                this.setMax(1500);
                this.setText("Unzip theme Elevate.zip");
                this.setProgress(0);
                zipper.unpack(assetManager.open("Elevate.zip"), persistenceManager.getThemesDir(), null, this);
            }
        };
        jobs.add(job);
        return jobs;
    }

}
