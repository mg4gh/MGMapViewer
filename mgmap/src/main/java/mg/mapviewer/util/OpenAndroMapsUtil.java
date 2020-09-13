package mg.mapviewer.util;

import android.content.Context;
import android.net.Uri;
import android.os.Build;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import mg.mapviewer.MGMapApplication;

public class OpenAndroMapsUtil {

    public static ArrayList<BgJob> createBgJobsFromIntentUri(Uri uri) throws Exception{
        ArrayList<BgJob> jobs = new ArrayList(){ };
        if (uri.toString().startsWith("mf-v4-map")){ // assume this is a map download
            BgJob job = new BgJob(){
                @Override
                protected void doJob() throws Exception {
                    Zipper zipper = new Zipper(null);
                    String s1 = uri.toString();
                    String s2 = s1.replaceFirst("mf-v4-map://download.openandromaps.org/", ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)?"https":"http")+"://ftp.gwdg.de/pub/misc/openstreetmap/openandromaps/");
                    URL url = new URL(s2);
                    InputStream is = url.openStream();
                    FilenameFilter filter = new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String name) {
                            if (name.endsWith("map")) return true;
                            if (name.endsWith("poi")) return true;
                            return false;
                        }
                    };
                    zipper.unpack(is, PersistenceManager.getInstance().getMapsforgeDir(), filter);

                }
            };
            jobs.add(job);
        } else if (uri.toString().startsWith("mf-theme")) { // assume this is a theme download
            BgJob job = new BgJob() {
                @Override
                protected void doJob() throws Exception {
                    super.doJob();
                    Zipper zipper = new Zipper(null);
                    String s1 = uri.toString();
                    String s2 = s1.replaceFirst("mf-theme", "https");
//                  URL url = new URL(s2);  //unfortunately this doesn't work
                    URL url = new URL(((Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)?"https":"http")+"://www.openandromaps.org/wp-content/users/tobias/Elevate.zip");
                    InputStream is = url.openStream();
                    zipper.unpack(is, PersistenceManager.getInstance().getThemesDir(), null);
                }
            };
            jobs.add(job);
        }
        return jobs;
    }
}
