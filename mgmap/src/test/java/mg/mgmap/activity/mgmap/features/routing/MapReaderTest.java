package mg.mgmap.activity.mgmap.features.routing;

import androidx.annotation.NonNull;

import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.core.util.MercatorProjection;

import java.io.File;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import mg.mgmap.generic.model.BBox;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.util.basic.MGLog;

public class MapReaderTest {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());
    private final static long WATER = 0x80_00000000L;

    record ZoomInterval(byte baseZoom, byte minZoom, byte maxZoom, long posSubfile, long sizeSubfile){}
    static class TileMeta{
        int tileX,tileY;
        boolean water;
        long offset;
        int length;

        public TileMeta(int tileX, int tileY, boolean water, long offset){
            this.tileX = tileX;
            this.tileY = tileY;
            this.water = water;
            this.offset = offset;
        }

        void setNextOffset(long nextOffset){
            long longLength = nextOffset - offset;
            assert (longLength < Integer.MAX_VALUE);
            this.length = (int)longLength;
        }

        @NonNull
        @Override
        public String toString() {
            return String.format(Locale.ENGLISH,"tileX=%05d tileY=%05d water=%s offset=%08x size=%08x",tileX,tileY,water?"Y":"N",offset,length);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private TreeMap<Integer, TileMeta> getTileMetaMap(File mapFile) throws Exception {
        TreeMap<Integer, TileMeta> tileMetaMap = new TreeMap<>();

        try (FileInputStream fis = new FileInputStream(mapFile)) {
            FileChannel fc = fis.getChannel();

            byte[] magic = new byte[20];
            fis.read(magic);
            Assert.assertEquals("mapsforge binary OSM", new String(magic));
            byte[] baHeaderLength = new byte[4];
            fis.read(baHeaderLength);
            ByteBuffer bbHeaderLength = ByteBuffer.wrap(baHeaderLength);
            bbHeaderLength.order(ByteOrder.BIG_ENDIAN);
            int headerLength = bbHeaderLength.getInt();
            mgLog.d("headerLength=" + headerLength + " (0x" + Integer.toHexString(headerLength).toUpperCase() + ")");
            byte[] baHeader = new byte[headerLength];
            fis.read(baHeader);
            ByteBuffer bbHeader = ByteBuffer.wrap(baHeader);
            bbHeader.order(ByteOrder.BIG_ENDIAN);
            Assert.assertEquals(5, bbHeader.getInt()); // file version
            long fileSize = bbHeader.getLong();
            mgLog.d("fileSize=" + fileSize + " (0x" + Long.toHexString(fileSize).toUpperCase() + ")");
            long creationDate = bbHeader.getLong();
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH.mm.ss.SSS");
            mgLog.d("creationDate=" + sdf.format(new Date(creationDate)));
            BBox bBox = new BBox()
                    .extend(PointModelImpl.createFromLaLo(bbHeader.getInt(), bbHeader.getInt()))
                    .extend(PointModelImpl.createFromLaLo(bbHeader.getInt(), bbHeader.getInt()));
            mgLog.d("bbox=" + bBox);
            short tileSize = bbHeader.getShort();
            mgLog.d("tileSize=" + tileSize);
            String projection = readString(bbHeader);
            Assert.assertEquals("Mercator", projection);
            byte flags = bbHeader.get();
            mgLog.d("flags=" + flags + " (0x" + Integer.toHexString(flags & 0xFF).toUpperCase() + ")");
            boolean debugFlag = ((flags & 0x80) != 0); // 1. bit (mask 0x80): flag for existence of debug information
            mgLog.d("debugFlag=" + debugFlag);
            if ((flags & 0x40) != 0) { // 2. bit (mask 0x40): flag for existence of the map start position field
                PointModel pmStart = PointModelImpl.createFromLaLo(bbHeader.getInt(), bbHeader.getInt());
                mgLog.d("pmStart=" + pmStart);
            }
            if ((flags & 0x20) != 0) { // 3. bit (mask 0x20): flag for existence of the start zoom level field
                byte zoomStart = bbHeader.get();
                mgLog.d("zoomStart=" + zoomStart);
            }
            if ((flags & 0x10) != 0) { // 4. bit (mask 0x10): flag for existence of the language(s) preference field
                String lang = readString(bbHeader);
                mgLog.d("lang=" + lang);
            }
            if ((flags & 0x08) != 0) { // 5. bit (mask 0x08): flag for existence of the comment field
                String comment = readString(bbHeader);
                mgLog.d("comment=" + comment);
            }
            if ((flags & 0x04) != 0) { // 6. bit (mask 0x04): flag for existence of the created by field
                String createdBy = readString(bbHeader);
                mgLog.d("createdBy=" + createdBy);
            }
            String[] poiTags = new String[bbHeader.getShort()];
            for (int i = 0; i < poiTags.length; i++) {
                poiTags[i] = readString(bbHeader);
            }
            mgLog.d("poiTags[" + poiTags.length + "]=" + Arrays.asList(poiTags));
            String[] wayTags = new String[bbHeader.getShort()];
            for (int i = 0; i < wayTags.length; i++) {
                wayTags[i] = readString(bbHeader);
            }
            mgLog.d("wayTags[" + wayTags.length + "]=" + Arrays.asList(wayTags));

            ZoomInterval[] zoomIntervals = new ZoomInterval[bbHeader.get()];
            for (int i = 0; i < zoomIntervals.length; i++) {
                zoomIntervals[i] = new ZoomInterval(bbHeader.get(), bbHeader.get(), bbHeader.get(), bbHeader.getLong(), bbHeader.getLong());
            }
            mgLog.d("zoomIntervals[" + zoomIntervals.length + "]=" + Arrays.asList(zoomIntervals));

            long totalSize = 20 + 4 + headerLength; // magicLength + headerLength +header
            for (ZoomInterval zoomInterval : zoomIntervals) {
                Assert.assertEquals(totalSize, zoomInterval.posSubfile);
                totalSize += zoomInterval.sizeSubfile;
            }
            Assert.assertEquals(totalSize, fileSize);


            for (ZoomInterval zoomInterval : zoomIntervals) {
                byte zoomLevel = zoomInterval.baseZoom;
                long mapSize = MercatorProjection.getMapSize(zoomLevel, tileSize);
                int tileXMin = MercatorProjection.pixelXToTileX(MercatorProjection.longitudeToPixelX(bBox.minLongitude, mapSize), zoomLevel, tileSize);
                int tileXMax = MercatorProjection.pixelXToTileX(MercatorProjection.longitudeToPixelX(bBox.maxLongitude, mapSize), zoomLevel, tileSize);
                int tileYMin = MercatorProjection.pixelYToTileY(MercatorProjection.latitudeToPixelY(bBox.maxLatitude, mapSize), zoomLevel, tileSize); // min and max reversed for tiles
                int tileYMax = MercatorProjection.pixelYToTileY(MercatorProjection.latitudeToPixelY(bBox.minLatitude, mapSize), zoomLevel, tileSize);

                int numTiles = (tileXMax - tileXMin + 1) * (tileYMax - tileYMin + 1);
                mgLog.d(() -> "Subfile baseZoom=" + zoomLevel + " with tileXMin=" + tileXMin + " tileYMin=" + tileYMin + " and tileXMax=" + tileXMax + " tileYMax=" + tileYMax +
                        " - numTiles=" + numTiles);

                int subfileHeaderSize = (debugFlag ? 16 : 0) + numTiles * 5;
                ByteBuffer bbSubfileHeader = ByteBuffer.wrap(new byte[subfileHeaderSize]);
                fc.read(bbSubfileHeader, zoomInterval.posSubfile);
                bbSubfileHeader.rewind();
                if (debugFlag) {
                    byte[] debugData = new byte[16];
                    bbSubfileHeader.get(debugData);
                    Assert.assertEquals("+++IndexStart+++".getBytes(), debugData);
                }
                long base = zoomInterval.posSubfile;
                TileMeta lastTileMeta = null;
                int blobkCnt = 0;
                for (int tileY = tileYMin; tileY <= tileYMax; tileY++) {
                    for (int tileX = tileXMin; tileX <= tileXMax; tileX++) {
                        long subOffset = readTileOffset(bbSubfileHeader);
//                        mgLog.d("  new Offset: "+Long.toHexString(subOffset)+" "+blobkCnt);
                        boolean water = (subOffset & WATER) != 0;
                        subOffset &= ~WATER;
                        if ((lastTileMeta != null)) {
                            lastTileMeta.setNextOffset(base + subOffset);
                        }
                        TileMeta tileMeta = new TileMeta(tileX, tileY, water, base + subOffset);
                        tileMetaMap.put(getKey(tileX, tileY), tileMeta);
                        lastTileMeta = tileMeta;
//                        mgLog.d("  add: " + tileMeta);
                        blobkCnt++;
                    }
                }
//                lastTileMeta.setNextOffset(base + zoomInterval.sizeSubfile);
            }
        }
        return tileMetaMap;
    }

    @Test
    public void _00_test() throws Exception{

        System.out.println("Hello MapTest");
        MGLog.logConfig.put("mg.mgmap", MGLog.Level.DEBUG);
        MGLog.setUnittest(true);
        mgLog.evaluateLevel();

        mgLog.d("Hallo MapTest logging");

        PointModelUtil.init(32);

        File mapFile = new File("src/test/assets/map_local/Ruegen_oam.osm.map"); // !!! map is not uploaded to git (due to map size)
        mgLog.d(mapFile.getAbsolutePath() + " " + mapFile.exists());

        TreeMap<Integer, TileMeta> tileMetaMap = getTileMetaMap(mapFile);

        for (int key : tileMetaMap.keySet()){
            mgLog.d(tileMetaMap.get(key));
        }
    }

    @Test
    public void _01_test() throws Exception{

        System.out.println("Hello MapTest");
        MGLog.logConfig.put("mg.mgmap", MGLog.Level.DEBUG);
        MGLog.setUnittest(true);
        mgLog.evaluateLevel();

        mgLog.d("Hallo MapTest logging");

        PointModelUtil.init(32);

        File mapFile = new File("src/test/assets/map_local/Ruegen_oam.osm.map"); // !!! map is not uploaded to git (due to map size)
        mgLog.d(mapFile.getAbsolutePath() + " " + mapFile.exists());

        TreeMap<Integer, TileMeta> tileMetaMap = getTileMetaMap(mapFile);

        int tileXMin = 8800;
        int tileYMin = 5220;
        int tileXMax = 8809;
        int tileYMax = 5229;
//        for (int tileY = tileYMin; tileY <= tileYMax; tileY++) {
//            for (int tileX = tileXMin; tileX <= tileXMax; tileX++) {
//                mgLog.d(tileMetaMap.get(getKey(tileX,tileY)));
//            }
//        }

        AtomicLong longSum = new AtomicLong(0L);
        try (FileInputStream fis = new FileInputStream(mapFile);
             ExecutorService executor = Executors.newFixedThreadPool(1)){
            long nanoNow = System.nanoTime();
            FileChannel fc = fis.getChannel();
            for (int tileY = tileYMin; tileY <= tileYMax; tileY++) {
                for (int tileX = tileXMin; tileX <= tileXMax; tileX++) {
//                    mgLog.d(tileMetaMap.get(getKey(tileX,tileY)));
                    int key = getKey(tileX,tileY);
                    executor.submit(()->{
                        try {
                            TileMeta tm = tileMetaMap.get(key);
                            MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_ONLY,tm.offset,tm.length);
                            long sum = 0;
                            while (mbb.hasRemaining()){
                                sum += (mbb.get() & 0xff);
                            }
//                            mgLog.d(tm+" sum="+sum);
                            longSum.addAndGet(sum);
                        } catch (Exception e){
                            mgLog.e(e.getMessage(),e);
                        }
                    });
                }
            }
            executor.shutdown();
            boolean res = executor.awaitTermination(5000, TimeUnit.MILLISECONDS);
            long nanoNow2 = System.nanoTime();
            mgLog.d("duration="+(nanoNow2-nanoNow)+" res="+res+" sum="+longSum.longValue());

        }
    }
    @Test
    public void _02_test() throws Exception{

        System.out.println("Hello MapTest");
        MGLog.logConfig.put("mg.mgmap", MGLog.Level.DEBUG);
        MGLog.setUnittest(true);
        mgLog.evaluateLevel();

        mgLog.d("Hallo MapTest logging");

        PointModelUtil.init(32);

        File mapFile = new File("src/test/assets/map_local/Ruegen_oam.osm.map"); // !!! map is not uploaded to git (due to map size)
        mgLog.d(mapFile.getAbsolutePath() + " " + mapFile.exists());

        TreeMap<Integer, TileMeta> tileMetaMap = getTileMetaMap(mapFile);

        int tileXMin = 8800;
        int tileYMin = 5220;
        int tileXMax = 8809;
        int tileYMax = 5229;
//        for (int tileY = tileYMin; tileY <= tileYMax; tileY++) {
//            for (int tileX = tileXMin; tileX <= tileXMax; tileX++) {
//                mgLog.d(tileMetaMap.get(getKey(tileX,tileY)));
//            }
//        }

        long longSum = 0;
        try (FileInputStream fis = new FileInputStream(mapFile)){
            long nanoNow = System.nanoTime();
            FileChannel fc = fis.getChannel();
            for (int tileY = tileYMin; tileY <= tileYMax; tileY++) {
                for (int tileX = tileXMin; tileX <= tileXMax; tileX++) {
//                    mgLog.d(tileMetaMap.get(getKey(tileX,tileY)));
                    int key = getKey(tileX,tileY);
                    TileMeta tm = tileMetaMap.get(key);
                    MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_ONLY,tm.offset,tm.length);
                    long sum = 0;
                    while (mbb.hasRemaining()){
                        sum += (mbb.get() & 0xff);
                    }
//                            mgLog.d(tm+" sum="+sum);
                    longSum += sum;
                }
            }
            long nanoNow2 = System.nanoTime();
            mgLog.d("duration="+(nanoNow2-nanoNow)+" res="+true+" sum="+longSum);

        }
    }
    @Test
    public void _03_test() throws Exception{

        System.out.println("Hello MapTest");
        MGLog.logConfig.put("mg.mgmap", MGLog.Level.DEBUG);
        MGLog.setUnittest(true);
        mgLog.evaluateLevel();

        mgLog.d("Hallo MapTest logging");

        PointModelUtil.init(32);

        File mapFile = new File("src/test/assets/map_local/Ruegen_oam.osm.map"); // !!! map is not uploaded to git (due to map size)
        mgLog.d(mapFile.getAbsolutePath() + " " + mapFile.exists());

        TreeMap<Integer, TileMeta> tileMetaMap = getTileMetaMap(mapFile);

        int tileXMin = 8800;
        int tileYMin = 5220;
        int tileXMax = 8809;
        int tileYMax = 5229;
//        for (int tileY = tileYMin; tileY <= tileYMax; tileY++) {
//            for (int tileX = tileXMin; tileX <= tileXMax; tileX++) {
//                mgLog.d(tileMetaMap.get(getKey(tileX,tileY)));
//            }
//        }

        long longSum = 0;
        try (FileInputStream fis = new FileInputStream(mapFile)){
            long nanoNow = System.nanoTime();
            FileChannel fc = fis.getChannel();
            for (int tileY = tileYMin; tileY <= tileYMax; tileY++) {
                for (int tileX = tileXMin; tileX <= tileXMax; tileX++) {
//                    mgLog.d(tileMetaMap.get(getKey(tileX,tileY)));
                    int key = getKey(tileX,tileY);
                    TileMeta tm = tileMetaMap.get(key);
                    byte[] buf = new byte[tm.length];
                    ByteBuffer bb = ByteBuffer.wrap(buf);
                    fc.position(tm.offset);
                    fc.read(bb);
                    long sum = 0;
                    for (int i=0; i<tm.length; i++){
                        sum += buf[i] & 0xFF;
                    }

//                    bb.rewind();
//                    while (bb.hasRemaining()){
//                        sum += (bb.get() & 0xff);
//                    }
//                            mgLog.d(tm+" sum="+sum);
                    longSum += sum;
                }
            }
            long nanoNow2 = System.nanoTime();
            mgLog.d("duration="+(nanoNow2-nanoNow)+" res="+true+" sum="+longSum);

        }
    }
    @Test
    public void _04_test() throws Exception{

        System.out.println("Hello MapTest");
        MGLog.logConfig.put("mg.mgmap", MGLog.Level.DEBUG);
        MGLog.setUnittest(true);
        mgLog.evaluateLevel();

        mgLog.d("Hallo MapTest logging");

        PointModelUtil.init(32);

        File mapFile = new File("src/test/assets/map_local/Ruegen_oam.osm.map"); // !!! map is not uploaded to git (due to map size)
        mgLog.d(mapFile.getAbsolutePath() + " " + mapFile.exists());

        TreeMap<Integer, TileMeta> tileMetaMap = getTileMetaMap(mapFile);

        int tileXMin = 8800;
        int tileYMin = 5220;
        int tileXMax = 8809;
        int tileYMax = 5229;
//        for (int tileY = tileYMin; tileY <= tileYMax; tileY++) {
//            for (int tileX = tileXMin; tileX <= tileXMax; tileX++) {
//                mgLog.d(tileMetaMap.get(getKey(tileX,tileY)));
//            }
//        }

        long longSum = 0;
        try (RandomAccessFile raf = new RandomAccessFile(mapFile, "r")){
            long nanoNow = System.nanoTime();
            for (int tileY = tileYMin; tileY <= tileYMax; tileY++) {
                for (int tileX = tileXMin; tileX <= tileXMax; tileX++) {
//                    mgLog.d(tileMetaMap.get(getKey(tileX,tileY)));
                    int key = getKey(tileX,tileY);
                    TileMeta tm = tileMetaMap.get(key);
                    raf.seek(tm.offset);
                    byte[] buf = new byte[tm.length];
                    raf.read(buf);
                    long sum = 0;
                    for (int i=0; i<tm.length; i++){
                        sum += buf[i] & 0xFF;
                    }
//                            mgLog.d(tm+" sum="+sum);
                    longSum += sum;
                }
            }
            long nanoNow2 = System.nanoTime();
            mgLog.d("duration="+(nanoNow2-nanoNow)+" res="+true+" sum="+longSum);

        }
    }


    static int readUnsignedInt(ByteBuffer bb){
        int res = 0;
        boolean cont = true;
        int shift = 0;
        while (cont){
            byte b = bb.get();
            cont = ((b & 0x80) != 0);
            res +=  (b & 0x7F)<<shift;
            shift += 7;
        }
        return res;
    }
    static int readSignedInt(ByteBuffer bb){
        int res = 0;
        boolean cont = true;
        boolean negative = false;
        int shift = 0;
        while (cont){
            byte b = bb.get();
            cont = ((b & 0x80) != 0);
            if (cont){
                res +=  (b & 0x7F)<<shift;
                shift += 7;
            } else {
                res +=  (b & 0x3F)<<shift;
                negative = ((b & 0x40) != 0);
            }
        }
        return negative?-res:res;
    }
    static String readString(ByteBuffer bb){
        int length = readUnsignedInt(bb);
        byte[] string = new byte[length];
        bb.get(string);
        return new String(string, StandardCharsets.UTF_8);
    }

    static long readTileOffset(ByteBuffer bb){
        long res = 0;
        for (int i=0; i<5; i++){ // fix size 5 bytes
            byte b = bb.get();
            int v = b & 0xff;
            res = (res<<8) + v;
        }
        return res;
    }

    static int getKey(int tileX,int tileY){
        return ( tileX <<16) + tileY;
    }

}
