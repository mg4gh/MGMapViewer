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
package mg.mapviewer.util;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import mg.mapviewer.MGMapApplication;
import mg.mapviewer.model.TrackLog;

/** Utility for extra tasks */
public class ExtrasUtil {



//    public static void generate(BoundingBox bb){
//        Log.i(MGMapApplication.LABEL, NameUtil.context() + " generate: Min- and Max-TileIndex values for BoundingBox " + bb);
//        int tileSize = 512;
//        for (byte zoomLevel=10; zoomLevel<= 16; zoomLevel++){
//            long mapSize = MercatorProjection.getMapSize(zoomLevel, tileSize);
//            int tileXMin = MercatorProjection.pixelXToTileX( MercatorProjection.longitudeToPixelX( bb.minLongitude , mapSize) , zoomLevel, tileSize);
//            int tileXMax = MercatorProjection.pixelXToTileX( MercatorProjection.longitudeToPixelX( bb.maxLongitude , mapSize) , zoomLevel, tileSize);
//            int tileYMin = MercatorProjection.pixelYToTileY( MercatorProjection.latitudeToPixelY( bb.maxLatitude , mapSize) , zoomLevel, tileSize); // min and max reversed for tiles
//            int tileYMax = MercatorProjection.pixelYToTileY( MercatorProjection.latitudeToPixelY( bb.minLatitude , mapSize) , zoomLevel, tileSize);
//
//            Log.i(MGMapApplication.LABEL, NameUtil.context() +" "+String.format(Locale.ENGLISH,"dls %d %d %d %d %d",zoomLevel,tileXMin, tileXMax, tileYMin, tileYMax));
//        }
//    }




    public static  void downloadTest(){
        File mapstores = new File(PersistenceManager.getInstance().getMapsDir(), "mapstores");
        if (mapstores.exists() && mapstores.isDirectory()){
            File requestDownload = new File(mapstores, "request");
            if (requestDownload.exists() && requestDownload.isDirectory()){
                File sample = new File(requestDownload, "sample.curl");
                if (sample.exists()){
                    try {
                        BufferedReader in = new BufferedReader(new FileReader(sample));
                        String line = in.readLine();
                        in.close();

                        saveFile(line, requestDownload, (byte)10, 534, 349);
                    } catch (Exception e) {
                        Log.e(MGMapApplication.LABEL, NameUtil.context(),e);
                    }
                }
            }
        }
    }

    public static void saveFile(final String surl, final File destinationDir, final byte zoom, final int xtile, final int ytile) throws IOException {
        new Thread(){
            @Override
            public void run() {
                try {
                    URL url = null;
                    URLConnection conn = null;

                    {
                        Log.i(MGMapApplication.LABEL, NameUtil.context()+" URL="+surl);
                        Log.i(MGMapApplication.LABEL, NameUtil.context()+" destinationDir="+destinationDir.getAbsolutePath());
                        String[] purl = surl.split(" -H ");
                        for (String x : purl){
                            Log.i(MGMapApplication.LABEL, NameUtil.context()+" x="+x);
                            String[] y = x.replaceAll("'$","").replaceAll(".*'","").split(": ");
                            if (y.length == 2){
                                Log.i(MGMapApplication.LABEL, NameUtil.context()+" \""+y[0]+"\"=\""+y[1]+"\"");
                                conn.setRequestProperty(y[0],y[1]);
                            } else {
                                Log.i(MGMapApplication.LABEL, NameUtil.context()+" "+y[0]);
                                String[] z = y[0].split("/");
                                if (z.length >= 4){
                                    url = new URL(String.format("%s/%s/%s/%s/%s/%s/%d/%d/%d.png?px=256",z[0],z[1],z[2],z[3],"ride","purple",zoom,xtile,ytile));
                                    Log.i(MGMapApplication.LABEL, NameUtil.context()+" "+url);
                                    conn = url.openConnection();
                                }
                            }

                        }
                    }
                    PersistenceManager pm = PersistenceManager.getInstance();
                    File zoomDir = pm.createIfNotExists(destinationDir,Byte.toString(zoom));
                    File xDir = pm.createIfNotExists(zoomDir,Integer.toString(xtile));
                    File yFile = new File(xDir,Integer.toString(ytile)+".png");

//                    URL url = new URL("https://heatmap-external-c.strava.com/tiles-auth/ride/purple/10/534/349.png?px=256");
//                    URLConnection conn = url.openConnection();
//                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:75.0) Gecko/20100101 Firefox/75.0");
//                    conn.setRequestProperty("Accept", "*/*' -H 'Accept-Language: de,en-US;q=0.7,en;q=0.3");
//                    conn.setRequestProperty("Referer", "https://www.strava.com");
//                    conn.setRequestProperty("Origin", "https://www.strava.com");
//                    conn.setRequestProperty("Connection", "keep-alive");
//                    conn.setRequestProperty("Cookie", "_strava4_session=mst9hpuico4o4cv0apl4onq55b0dqe6j; sp=f8417f3e-0fc5-40f3-aeaa-834718e67ab9; CloudFront-Policy=eyJTdGF0ZW1lbnQiOiBbeyJSZXNvdXJjZSI6Imh0dHBzOi8vaGVhdG1hcC1leHRlcm5hbC0qLnN0cmF2YS5jb20vKiIsIkNvbmRpdGlvbiI6eyJEYXRlTGVzc1RoYW4iOnsiQVdTOkVwb2NoVGltZSI6MTU4NzQ5ODEyMX0sIkRhdGVHcmVhdGVyVGhhbiI6eyJBV1M6RXBvY2hUaW1lIjoxNTg2Mjc0MTIxfX19XX0_; CloudFront-Key-Pair-Id=APKAIDPUN4QMG7VUQPSA; CloudFront-Signature=EaZvrLY8lYP8BBPf8UPqAAfc7VUbJwn7nK~Eg7R2zHshbMCHvIm0wMEENferbbjxkO52cEAci2Yci95kiPOFjy1xuPiNosQqw0JRfIzC4KyVnGaIXfJN84~ZZwIxes08heRVIUAe3vE3Ltz39N~evCaRr5jH73sLJfhgwVq8iCC9WUtjr1m1BMpjo5iykpFQVRXCdtATiDnqurZFy3259Y2MaMpAawbbI0-hQZgOlI5KSBwfKWNcta7Ld3Qr4wG0EJl8yFkNksV9JcWa0CRrZ2U1ySg0V6HCw4wjwSAifXuNJ09cxnRiIQInidH~FNmoDo6mCgwtiKgw0AaG5Xa2ww__");

                    InputStream is = conn.getInputStream();
                    OutputStream os = new FileOutputStream(yFile);

                    byte[] b = new byte[2048];
                    int length;

                    while ((length = is.read(b)) != -1) {
                        os.write(b, 0, length);
                    }

                    is.close();
                    os.close();
                } catch (IOException e) {
                    Log.e(MGMapApplication.LABEL, NameUtil.context(),e);
                }

            }
        }.start();



    }

    public static void checkCreateMeta(){
        Log.i(MGMapApplication.LABEL, NameUtil.context() );

        try {
            List<String> candidates = PersistenceManager.getInstance().getGpxNames();
            candidates.removeAll( PersistenceManager.getInstance().getMetaNames() );

            for (String name : candidates){
                TrackLog trackLog = new GpxImporter().parseTrackLog(name, PersistenceManager.getInstance().openGpxInput(name));
                MetaDataUtil.createMetaData(trackLog);
                MetaDataUtil.writeMetaData(PersistenceManager.getInstance().openMetaOutput(name), trackLog);

            }
        } catch (Exception e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(),e);
        }
    }
}
