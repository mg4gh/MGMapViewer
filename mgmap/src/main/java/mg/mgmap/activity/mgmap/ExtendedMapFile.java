package mg.mgmap.activity.mgmap;

import androidx.annotation.NonNull;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tag;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.datastore.MapReadResult;
import org.mapsforge.map.datastore.Way;
import org.mapsforge.map.reader.MapFile;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import mg.mgmap.generic.model.BBox;
import mg.mgmap.generic.model.MultiPointModel;
import mg.mgmap.generic.model.MultiPointModelImpl;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.model.WriteableMultiPointModel;
import mg.mgmap.generic.util.basic.MGLog;

public class ExtendedMapFile extends MapFile {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    protected final String id;
    protected final String filename;
    private final MultiPointModel mpmBorder;
    private record Relation(long lalo1, long lalo2){
        long other(long lalo){
            return (lalo==lalo1)?lalo2:((lalo==lalo2)?lalo2:new PointModelImpl().getLaLo());
        }
    }

    private static final byte ZOOM_LEVEL = 4;
    private static final int TILE_SIZE = MapViewerBase.TILE_SIZE;


    public ExtendedMapFile(String id, File file, String language){
        super(file, language);
        this.id = id;
        this.filename = file.getName();

        mgLog.d("init ExtendedMapFile with "+file.getName());
        long mapSize = MercatorProjection.getMapSize(ZOOM_LEVEL, TILE_SIZE);
        int tileXMin = MercatorProjection.pixelXToTileX( MercatorProjection.longitudeToPixelX( boundingBox().minLongitude , mapSize) , ZOOM_LEVEL, TILE_SIZE);
        int tileXMax = MercatorProjection.pixelXToTileX( MercatorProjection.longitudeToPixelX( boundingBox().maxLongitude , mapSize) , ZOOM_LEVEL, TILE_SIZE);
        int tileYMin = MercatorProjection.pixelYToTileY( MercatorProjection.latitudeToPixelY( boundingBox().maxLatitude , mapSize) , ZOOM_LEVEL, TILE_SIZE); // min and max reversed for tiles
        int tileYMax = MercatorProjection.pixelYToTileY( MercatorProjection.latitudeToPixelY( boundingBox().minLatitude , mapSize) , ZOOM_LEVEL, TILE_SIZE);
        Tile upperLeft = new Tile(tileXMin, tileYMin, ZOOM_LEVEL, TILE_SIZE);
        Tile lowerRight = new Tile(tileXMax, tileYMax, ZOOM_LEVEL, TILE_SIZE);

        MapReadResult result = readMapData(upperLeft, lowerRight);
        ArrayList<Relation> innerBorder = new ArrayList<>();
        for (Way way : result.ways){
            for (Tag tag : way.tags){
                if (tag.key.equals("boundary") && tag.value.equals("map_inner")){
                    LatLong[] latLongs = way.latLongs[0];
                    long lastLalo = new PointModelImpl(latLongs[0]).getLaLo();
                    for (int i=1; i<latLongs.length; i++){
                        long lalo = new PointModelImpl(latLongs[i]).getLaLo();
                        innerBorder.add(new Relation(lastLalo, lalo));
                        lastLalo = lalo;
                    }
                }
            }
        }
        WriteableMultiPointModel mpm = null;
        if (!innerBorder.isEmpty()){
            mpm = new MultiPointModelImpl();
            long current = innerBorder.get(0).lalo1();
            PointModel pmCurrent = PointModelImpl.createFromLaLo(current);
            PointModel pmPrev = null;
            mpm.addPoint(PointModelImpl.createFromLaLo(current));
            while (!innerBorder.isEmpty()){
                Relation relation = null;
                for (Relation rel : innerBorder){
                    if (relation == null){ // not yet found a relation
                        for (long lalo : List.of(rel.lalo1,rel.lalo2)){
                            if (lalo == current){
                                relation = rel;
                                pmPrev = pmCurrent;
                                current = rel.other(lalo);
                                pmCurrent = PointModelImpl.createFromLaLo(current);
                                mpm.addPoint(pmCurrent);
                                break;
                            }
                        }
                    } else {
                        break;
                    }
                }
                if ((relation == null) && (pmPrev != null)) { // no relation with "current" found -> check for overlapping relations
                    BBox bbCurrent = new BBox().extend(pmCurrent).extend(112);
                    for (Relation rel : innerBorder) {
                        PointModel pmLalo1 = PointModelImpl.createFromLaLo(rel.lalo1);
                        PointModel pmLalo2 = PointModelImpl.createFromLaLo(rel.lalo2);
                        if (bbCurrent.contains(pmLalo1)) {
                            if (PointModelUtil.isOverlappingLine(pmPrev, pmCurrent, pmLalo1, pmLalo2, 1)) {
                                relation = rel;
                                mpm.removePoint(pmCurrent);
                                current = rel.other(rel.lalo2);
                                pmCurrent = pmLalo2;
                                mpm.addPoint(pmCurrent);
                                break;
                            }
                        }
                        if (bbCurrent.contains(pmLalo2)) {
                            if (PointModelUtil.isOverlappingLine(pmPrev, pmCurrent, pmLalo2, pmLalo1, 1)) {
                                relation = rel;
                                mpm.removePoint(pmCurrent);
                                current = rel.other(rel.lalo1);
                                pmCurrent = pmLalo1;
                                mpm.addPoint(pmCurrent);
                                break;
                            }
                        }
                    }
                }
                if (relation == null){
                    mpm = null;
                    mgLog.d("no successor found for ="+ PointModelImpl.createFromLaLo(current));
                    break;
                } else {
                    innerBorder.remove(relation);
                }
            }
        }
        this.mpmBorder = mpm;
        setPriority(mpmBorder==null?0: mpmBorder.size());
        mgLog.d("init ExtendedMapFile with "+file.getName()+" finished. mpmBorder="+(mpmBorder!=null)+" priority="+getPriority());
    }

    public String getId(){
        return id;
    }

    @Override
    public boolean supportsFullTile(Tile tile) {
        return super.supportsFullTile(tile);
    }

    public boolean hasInnerBorder() {
        return mpmBorder != null;
    }

    public boolean isInInnerBorder(Tile tile){
        return isInInnerBorder(tile.getBoundingBox().minLatitude, tile.getBoundingBox().maxLatitude, tile.getBoundingBox().minLongitude, tile.getBoundingBox().maxLongitude);
    }
    public boolean isInInnerBorder(Tile upperLeft,Tile lowerRight){
        BoundingBox boundingBox = upperLeft.getBoundingBox().extendBoundingBox(lowerRight.getBoundingBox());
        return isInInnerBorder(boundingBox.minLatitude, boundingBox.maxLatitude, boundingBox.minLongitude, boundingBox.maxLongitude);
    }
    public boolean isInInnerBorder(double minLatitude, double maxLatitude, double minLongitude, double maxLongitude){
        for (PointModel corner : List.of(new PointModelImpl(maxLatitude, minLongitude),
                new PointModelImpl(maxLatitude, maxLongitude),
                new PointModelImpl(minLatitude, maxLongitude),
                new PointModelImpl(minLatitude, minLongitude))) {
            if (!PointModelUtil.pointInPolygon(corner, mpmBorder)) return false;
        }
        for (PointModel pm : mpmBorder){ // check if a border point is inside the polygon
            if ((minLatitude < pm.getLat()) && (pm.getLat() < maxLatitude) && (minLongitude < pm.getLon()) && (pm.getLon() < maxLongitude)) return false;
        }
        return true;
    }

    @NonNull
    @Override
    public String toString() {
        return this.getClass().getSimpleName()
                +" id=\""+id+"\""
                +" filename=\""+filename+"\""
                +" borderNodes="+((mpmBorder==null)?0:mpmBorder.size())
                +" priority="+getPriority();
    }
}
