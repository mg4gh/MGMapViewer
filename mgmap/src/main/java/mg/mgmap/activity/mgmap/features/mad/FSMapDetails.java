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
package mg.mgmap.activity.mgmap.features.mad;

import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.TreeMap;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.FeatureService;
import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.activity.mgmap.MapViewerBase;
import mg.mgmap.activity.mgmap.view.BoxView;
import mg.mgmap.activity.mgmap.view.ControlMVLayer;
import mg.mgmap.activity.mgmap.view.MultiMultiPointView;
import mg.mgmap.activity.mgmap.view.MultiPointView;
import mg.mgmap.generic.model.BBox;
import mg.mgmap.generic.model.MultiPointModel;
import mg.mgmap.generic.model.MultiPointModelImpl;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.model.WriteablePointModel;
import mg.mgmap.generic.util.CC;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.basic.LaLo;
import mg.mgmap.generic.util.basic.MGLog;

public class FSMapDetails extends FeatureService {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private static final Paint PAINT_MAD_STROKE =  CC.getStrokePaint(R.color.CC_RED_A150, 3);
    public static final Paint PAINT_GRAD_ALL_STROKE = CC.getStrokePaint(R.color.CC_GRAY100_A150, 3);

    public class MadControlLayer extends ControlMVLayer<Object> {
        @Override
        public boolean onTap(WriteablePointModel pmTap) {
            unregisterAll();
            final boolean[] res = {false};
            if (verifyMapDetailsVisibility()) {
                new Thread(() -> res[0] = showMapDetails( pmTap )).start();
            }
            return res[0];
        }
    }

    record ZoomInterval(byte baseZoom, byte minZoom, byte maxZoom, long posSubfile, long sizeSubfile){}

    private MadControlLayer controlLayer = null;

    private final Pref<Integer> prefZoomLevel = getPref(R.string.FSBeeline_pref_ZoomLevel, 15);
    private final static long WATER = 0x80_00000000L;

    private boolean active;
    private final byte zoomLevel = 14;
    private final int tileSize = MapViewerBase.TILE_SIZE;
    private RandomAccessFile raf;
    private boolean debugFlag = false;
    private ZoomInterval[] zoomIntervals;
    String[] poiTags = null;
    String[] wayTagKeys = null;
    String[] wayTagValues = null;


    private TreeMap<Integer, TileMeta> tileMetaMap;

    public FSMapDetails(MGMapActivity mmActivity) {
        super(mmActivity);

        active = getApplication().getPrefCache().get("FSMapDetails.active", false).getValue();
        mgLog.d("FSMapDetails active="+active);
        if (active){
            mgLog.d("start FSMapDetails");

            prefZoomLevel.addObserver(refreshObserver);
            File mapFile = new File(getApplication().getPersistenceManager().getMapsforgeDir(),"ruegen.osm.map"); // !!! map is not uploaded to git (due to map size)
            active = mapFile.exists();
            mgLog.d(mapFile.getAbsolutePath() + " " + active);
            if (active){
                try {
                    tileMetaMap = getTileMetaMap(mapFile);
                    raf = new RandomAccessFile(mapFile, "r");
                } catch (Exception e) {
                    mgLog.e(e.getMessage(),e);
                    active = false;
                }
            }
        }
    }

    @Override
    protected void onResume() {
        if (active){
            controlLayer = new MadControlLayer();
            register(controlLayer);

//            MultiPointModelImpl mpm = new MultiPointModelImpl();
//            mpm.addPoint( new PointModelImpl(49.404680, 8.753337) );
//            mpm.addPoint( new PointModelImpl(49.406142, 8.753165) );
//            MultiPointView mpv =new MultiPointView(mpm, PAINT_MAD_STROKE);
//            register(mpv);
        }
    }

    @Override
    protected void onPause() {
        unregisterAllControl();
        unregisterAll();
        controlLayer = null;
    }

    @Override
    protected void doRefreshResumedUI() {
        if (!verifyMapDetailsVisibility()){
            unregisterAll();
        }
    }

    // MapDetails shall be visible as long as zoom level is more than 13
    private boolean verifyMapDetailsVisibility(){
        return prefZoomLevel.getValue() > 13;
    }

    private boolean showMapDetails(PointModel pmTap){
        if (active){
            try {
                if (zoomLevel == prefZoomLevel.getValue()){ // in a first step do only on native zoom level 14
                    ZoomInterval zoomInterval = null;
                    for (ZoomInterval zi : zoomIntervals) {
                        if (zi.minZoom <= zoomLevel && zi.maxZoom >= zoomLevel) {
                            zoomInterval = zi;
                            break;
                        }
                    }

                    Tile tile = getTile(pmTap, zoomLevel);
                    BBox tbb = BBox.fromBoundingBox( tile.getBoundingBox() );

                    BoxView boxView = new BoxView(tbb, PAINT_MAD_STROKE);
                    register(boxView);


                    int key = getKey(tile.tileX,tile.tileY);
                    TileMeta tm = tileMetaMap.get(key);
                    if (tm == null) return false;
//                    assert tm != null;
                    raf.seek(tm.offset);
                    byte[] buf = new byte[tm.length];
                    raf.read(buf);
                    ByteBuffer bb = ByteBuffer.wrap(buf);
                    bb.order(ByteOrder.BIG_ENDIAN);

                    if (debugFlag) {
                        byte[] debugData = new byte[32];
                        bb.get(debugData);
                        assert(new String(debugData).startsWith( String.format(Locale.ENGLISH,"###TileStart%d,%d###",tile.tileX,tile.tileY) ));
                    }

                    assert zoomInterval != null;
                    int zoomLevels = zoomInterval.maxZoom - zoomInterval.minZoom +1;
                    int[] numPois = new int[zoomLevels];
                    int[] numWays = new int[zoomLevels];
                    int np = 0;
                    int nw = 0;
                    for (int i=0; i<zoomLevels; i++){
                        np += readUnsignedInt(bb);
                        nw += readUnsignedInt(bb);
                        numPois[i] = np;
                        numWays[i] = nw;
                    }
                    int wayOffset = readUnsignedInt(bb);
                    int wayPosition = bb.position() + wayOffset;

                    int numPoi = numPois[zoomLevel-zoomInterval.minZoom];
                    int numWay = numWays[zoomLevel-zoomInterval.minZoom];
                    mgLog.d("numPoi="+numPoi+" numWay="+numWay);

                    bb.position(wayPosition);

                    for (int i=0; i<numWay; i++){
                        processWay(tbb,bb);
                    }





                }
            } catch (IOException e) {
                mgLog.e(e.getMessage(),e);
                active = false;
            }


        }
        return false;
    }
    private Tile getTile(PointModel pos, byte zoomLevel){
        long mapSize = MercatorProjection.getMapSize(zoomLevel, tileSize);
        int tileX = MercatorProjection.pixelXToTileX( MercatorProjection.longitudeToPixelX( pos.getLon() , mapSize) , zoomLevel, tileSize);
        int tileY = MercatorProjection.pixelYToTileY( MercatorProjection.latitudeToPixelY( pos.getLat() , mapSize) , zoomLevel, tileSize);
        return new Tile(tileX, tileY, zoomLevel, tileSize);
    }



    void processWay(BBox tbb, ByteBuffer bb){ // assume position points to start of way
        boolean ele = false;
        mgLog.d("process way pos=0x"+Integer.toHexString( bb.position() ));
        if (debugFlag) {
            byte[] debugData = new byte[32];
            bb.get(debugData);
            assert(new String(debugData).startsWith( "---WayStart" ));
        }
        int waySize = readUnsignedInt(bb);
        int nextWayPosition = bb.position() + waySize;

        short subTileBitmap = bb.getShort();
        byte special = bb.get();
        int numWayTags = special & 0x0F;
        ArrayList<Integer> tagIds = new ArrayList<>();
        for (int i=0; i<numWayTags; i++){
            int tagId = readUnsignedInt(bb);
            tagIds.add(tagId);
        }
        StringBuilder sb = new StringBuilder("Tags: ");
        for (int i=0; i<numWayTags; i++){
            int tagId = tagIds.get(i);
            String tagValue = wayTagValues[tagId];
            if ((tagValue.length() == 2) && (tagValue.charAt(0) == '%')){
                if (tagValue.charAt(1) == 'b'){
                    tagValue = String.valueOf(bb.get());
                } else if (tagValue.charAt(1) == 'i'){
                    if (wayTagKeys[tagId].contains(":colour")){
                        tagValue = "#"+Integer.toHexString( bb.getInt() );
                    } else {
                        tagValue = String.valueOf(bb.getInt());
                    }
                } else if (tagValue.charAt(1) == 'f'){
                    tagValue = String.valueOf(bb.getFloat());
                } else if (tagValue.charAt(1) == 'h'){
                    tagValue = String.valueOf(bb.getShort());
                } else if (tagValue.charAt(1) == 's'){
                    tagValue = readString(bb);
                }
            }
            if (tagValue.contains("elevation")) ele = true;


            sb.append(wayTagKeys[tagId]).append("=").append(tagValue);
            if (i<numWayTags-1) sb.append(", ");
        }
        mgLog.d(sb.toString());
        byte flags = bb.get();
        StringBuilder props = new StringBuilder();
        if ((flags & 0x80) !=  0){
            props.append("name=").append(readString(bb)).append(" ");
        }
        if ((flags & 0x40) !=  0){
            props.append("hnr=").append(readString(bb)).append(" ");
        }
        if ((flags & 0x20) !=  0){
            props.append("ref=").append(readString(bb)).append(" ");
        }
        if ((flags & 0x10) !=  0){
            int latDiff = readSignedInt(bb);
            int lonDiff = readSignedInt(bb);
            props.append("relLabelPos=").append(latDiff).append(",").append(lonDiff).append(" ");
        }
        int numWayDataBlocks = 1;
        if ((flags & 0x08) !=  0){
            numWayDataBlocks = readUnsignedInt(bb);
        }
        boolean doubleDeltaEncoded = ((flags & 0x04) !=  0);

        for (int wdb=0; wdb<numWayDataBlocks; wdb++){
            int wayCoordBlocks = readUnsignedInt(bb);
            if (wayCoordBlocks == 1){
                MultiPointModel mpm = decodeWayBlock(tbb, bb, doubleDeltaEncoded);
                MultiPointView mpv = new MultiPointView(mpm, ele?PAINT_GRAD_ALL_STROKE:PAINT_MAD_STROKE);
                register(mpv);
                mgLog.d("1: "+mpm2string(mpm));
            } else {
                ArrayList<MultiPointModel> mmpm = new ArrayList<>();
                for (int i=0; i<wayCoordBlocks; i++){
                    MultiPointModel mpm = decodeWayBlock(tbb, bb, doubleDeltaEncoded);
                    mmpm.add(mpm);
                    mgLog.d((i+1)+": "+mpm2string(mpm));
                }
                MultiMultiPointView mmpv = new MultiMultiPointView(mmpm, ele?PAINT_GRAD_ALL_STROKE:PAINT_MAD_STROKE);
                register(mmpv);
            }
        }
        bb.position(nextWayPosition);
    }


    private MultiPointModel decodeWayBlock(BBox tbb, ByteBuffer bb, boolean doubleDeltaEncoded){
        int numBlockNodes = readUnsignedInt(bb);
        MultiPointModelImpl mpm = new MultiPointModelImpl();
        int nodeLa = LaLo.d2md(tbb.maxLatitude) + readSignedInt(bb);
        int nodeLo = LaLo.d2md(tbb.minLongitude) + readSignedInt(bb);
        mpm.addPoint(PointModelImpl.createFromLaLo(nodeLa,nodeLo));
        int deltaLa = 0;
        int deltaLo = 0;

        for (int i=1; i<numBlockNodes; i++) {
            if (doubleDeltaEncoded){
                deltaLa += readSignedInt(bb);
                deltaLo += readSignedInt(bb);
                nodeLa += deltaLa;
                nodeLo += deltaLo;
            } else {
                nodeLa += readSignedInt(bb);
                nodeLo += readSignedInt(bb);
            }
            mpm.addPoint(PointModelImpl.createFromLaLo(nodeLa,nodeLo));
        }
        return mpm;
    }










    @SuppressWarnings("ResultOfMethodCallIgnored")
    private TreeMap<Integer, TileMeta> getTileMetaMap(File mapFile) throws Exception {
        TreeMap<Integer, TileMeta> tileMetaMap = new TreeMap<>();

        try (FileInputStream fis = new FileInputStream(mapFile)) {
            FileChannel fc = fis.getChannel();

            byte[] magic = new byte[20];
            fis.read(magic);
            assert( "mapsforge binary OSM".equals( new String(magic) ));
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
            assert(bbHeader.getInt() == 5); // file version
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
            assert("Mercator".equals(projection));
            byte flags = bbHeader.get();
            mgLog.d("flags=" + flags + " (0x" + Integer.toHexString(flags & 0xFF).toUpperCase() + ")");
            debugFlag = ((flags & 0x80) != 0); // 1. bit (mask 0x80): flag for existence of debug information
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
            poiTags = new String[bbHeader.getShort()];
            for (int i = 0; i < poiTags.length; i++) {
                poiTags[i] = readString(bbHeader);
            }
            mgLog.d("poiTags[" + poiTags.length + "]=" + Arrays.asList(poiTags));
            int numWayTags = bbHeader.getShort();
            wayTagKeys = new String[numWayTags];
            wayTagValues = new String[numWayTags];
            for (int i = 0; i < wayTagKeys.length; i++) {
                String wayTag = readString(bbHeader);
                mgLog.d(i+": "+wayTag);
                String[] tagParts = wayTag.split("=");
                wayTagKeys[i] = tagParts[0];
                wayTagValues[i] = tagParts[1];
            }
            mgLog.d("wayTagKeys[" + wayTagKeys.length + "]=" + Arrays.asList(wayTagKeys));
            mgLog.d("wayTagValues[" + wayTagValues.length + "]=" + Arrays.asList(wayTagValues));

            zoomIntervals = new ZoomInterval[bbHeader.get()];
            for (int i = 0; i < zoomIntervals.length; i++) {
                zoomIntervals[i] = new ZoomInterval(bbHeader.get(), bbHeader.get(), bbHeader.get(), bbHeader.getLong(), bbHeader.getLong());
            }
            mgLog.d("zoomIntervals[" + zoomIntervals.length + "]=" + Arrays.asList(zoomIntervals));

            long totalSize = 20 + 4 + headerLength; // magicLength + headerLength +header
            for (ZoomInterval zoomInterval : zoomIntervals) {
                assert(totalSize == zoomInterval.posSubfile);
                totalSize += zoomInterval.sizeSubfile;
            }
            assert(totalSize == fileSize);


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
                    assert("+++IndexStart+++".equals(new String(debugData)));
                }
                long base = zoomInterval.posSubfile;
                TileMeta lastTileMeta = null;
                for (int tileY = tileYMin; tileY <= tileYMax; tileY++) {
                    for (int tileX = tileXMin; tileX <= tileXMax; tileX++) {
                        long subOffset = readTileOffset(bbSubfileHeader);
                        boolean water = (subOffset & WATER) != 0;
                        subOffset &= ~WATER;
                        if ((lastTileMeta != null)) {
                            lastTileMeta.setNextOffset(base + subOffset);
                        }
                        TileMeta tileMeta = new TileMeta(tileX, tileY, water, base + subOffset);
                        tileMetaMap.put(getKey(tileX, tileY), tileMeta);
                        lastTileMeta = tileMeta;
                    }
                }
                if ((lastTileMeta != null)) {
                    lastTileMeta.setNextOffset(base + zoomInterval.sizeSubfile);
                }
            }
        }
        return tileMetaMap;
    }

    int readUnsignedInt(ByteBuffer bb){
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
    int readSignedInt(ByteBuffer bb){
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
    String readString(ByteBuffer bb){
        int length = readUnsignedInt(bb);
        byte[] string = new byte[length];
        bb.get(string);
        return new String(string, StandardCharsets.UTF_8);
    }

    long readTileOffset(ByteBuffer bb){
        long res = 0;
        for (int i=0; i<5; i++){ // fix size 5 bytes
            byte b = bb.get();
            int v = b & 0xff;
            res = (res<<8) + v;
        }
        return res;
    }

    int getKey(int tileX,int tileY){
        return ( tileX <<16) + tileY;
    }



    String mpm2string(MultiPointModel mpm){
        StringBuilder sb = new StringBuilder("Size="+mpm.size());
        for (PointModel pm : mpm){
            sb.append(" ").append(pm);
        }
        return sb.toString();
    }
}
