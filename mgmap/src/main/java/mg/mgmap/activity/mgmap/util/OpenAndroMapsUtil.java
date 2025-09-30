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

import java.io.FilenameFilter;
import java.net.URL;

import mg.mgmap.application.util.PersistenceManager;
import mg.mgmap.generic.util.BgJob;
import mg.mgmap.generic.util.Zipper;

public class OpenAndroMapsUtil {

    public static BgJob createBgJobsFromIntentUriMap(PersistenceManager persistenceManager, URL url)  {
        return new BgJob(){
            @Override
            protected void doJob() throws Exception {
                Zipper zipper = new Zipper(null);
                FilenameFilter filter = (dir, name) -> (name.endsWith("map") );
                zipper.unpack(url, persistenceManager.getMapsforgeDir(), filter, this);
            }
        };
    }

    public static BgJob createBgJobsFromIntentUriTheme(PersistenceManager persistenceManager, URL url)  {
        return new BgJob() {
            @Override
            protected void doJob() throws Exception {
                super.doJob();
                Zipper zipper = new Zipper(null);
                zipper.unpack(url, persistenceManager.getThemesDir(), null, this);
            }
        };
    }

}
