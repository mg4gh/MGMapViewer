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
package mg.mgmap.activity.mgmap.view;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rotation;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.layer.Layer;

import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.model.WriteablePointModel;
import mg.mgmap.generic.model.WriteablePointModelImpl;
import mg.mgmap.activity.mgmap.util.MapViewUtility;

public abstract class MVLayer extends Layer {

    private MapViewUtility mapViewUtility = null;

    public MapViewUtility getMapViewUtility() {
        return mapViewUtility;
    }

    public void setMapViewUtility(MapViewUtility mapViewUtility) {
        this.mapViewUtility = mapViewUtility;
    }


    protected Point topLeftPoint;
    private long mapSize;
    private int dx = 0;
    private int dy = 0;


    @Override
    public final synchronized void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint, Rotation rotation) {
        this.topLeftPoint = topLeftPoint;
        mapSize = MercatorProjection.getMapSize(zoomLevel, displayModel.getTileSize());
        if (mapViewUtility != null) {
            dx = (canvas.getDimension().width - mapViewUtility.getMapViewDimension().width) / 2;
            dy = (canvas.getDimension().height - mapViewUtility.getMapViewDimension().height) / 2;

            if (mapViewUtility.getTrackVisibility()){
                doDraw(boundingBox,zoomLevel,canvas,topLeftPoint);
            }
        }
    }

    protected void doDraw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) { }

    protected double x2lon(double x){
        return MercatorProjection.pixelXToLongitude(topLeftPoint.x + dx + x, mapSize);
    }
    protected double y2lat(double y){
        return MercatorProjection.pixelYToLatitude(topLeftPoint.y + dy + y, mapSize);
    }
    protected int lon2x(double lon){
        return (int)(MercatorProjection.longitudeToPixelX(lon, mapSize) - dx -topLeftPoint.x);
    }
    protected int lat2y(double lat){
        return (int)(MercatorProjection.latitudeToPixelY(lat, mapSize) - dy - topLeftPoint.y);
    }

    protected int lon2canvasX(double lon){
        return (int)(MercatorProjection.longitudeToPixelX(lon, mapSize) - topLeftPoint.x);
    }
    protected int lat2canvasY(double lat){
        return (int)(MercatorProjection.latitudeToPixelY(lat, mapSize) - topLeftPoint.y);
    }


    @Override
    public boolean onTap(LatLong tapLatLong, Point layerXY, Point tapXY) {
        return onTap(new WriteablePointModelImpl(tapLatLong.latitude, tapLatLong.longitude));
    }

    protected boolean onTap(WriteablePointModel point){
        return false;
    }


    @Override
    public boolean onLongPress(LatLong tapLatLong, Point layerXY, Point tapXY) {
        return onLongPress(new PointModelImpl(tapLatLong));
    }

    protected boolean onLongPress(PointModel pm){
        return false;
    }
}
