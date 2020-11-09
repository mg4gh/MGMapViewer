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
package mg.mapviewer.view;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.layer.Layer;

import mg.mapviewer.model.PointModel;
import mg.mapviewer.model.PointModelImpl;
import mg.mapviewer.model.WriteablePointModel;
import mg.mapviewer.model.WriteablePointModelImpl;
import mg.mapviewer.util.MapViewUtility;

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
    public final synchronized void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) {
//        Log.i(MGMapApplication.LABEL, NameUtil.context() + " "+this);
        this.topLeftPoint = topLeftPoint;
        mapSize = MercatorProjection.getMapSize(zoomLevel, displayModel.getTileSize());
        if (mapViewUtility != null) {
            dx = (canvas.getDimension().width - mapViewUtility.getMapViewDimension().width) / 2;
            dy = (canvas.getDimension().height - mapViewUtility.getMapViewDimension().height) / 2;
        }

        doDraw(boundingBox,zoomLevel,canvas,topLeftPoint);
    }

    protected void doDraw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) { }

    protected double x2lon(double x){
        return MercatorProjection.pixelXToLongitude(topLeftPoint.x + dx + x, mapSize);
    }
    protected double y2lat(double y){
        return MercatorProjection.pixelYToLatitude(topLeftPoint.y + dy + y, mapSize);
    }
    protected int lon2x(double lon){
        return (int)(MercatorProjection.longitudeToPixelX(lon, mapSize) - topLeftPoint.x);
    }
    protected int lat2y(double lat){
        return (int)(MercatorProjection.latitudeToPixelY(lat, mapSize) - topLeftPoint.y);
    }


    @Override
    public boolean onTap(LatLong tapLatLong, Point layerXY, Point tapXY) {
        return onTap(new WriteablePointModelImpl(tapLatLong.latitude, tapLatLong.longitude));
    }

    protected boolean onTap(WriteablePointModel point){
        return false;
    }


    private DragData dragData = null;

    @Override
    public boolean onScroll(float scrollX1, float scrollY1, float scrollX2, float scrollY2) {
        if (dragData != null){
            if (!dragData.checkDragXY(scrollX1,scrollY1)){
                dragData.reset();
                dragData.setDragXY(scrollX1,scrollY1);
                PointModel pmStartScroll = new WriteablePointModelImpl(y2lat(scrollY1), x2lon(scrollX1));
                if (checkDrag(pmStartScroll, dragData)){
                    dragData.dragOrigin = pmStartScroll;
                }
            }

            if (dragData.dragOrigin != null){
                PointModel pmCurrent = new PointModelImpl(y2lat(scrollY2), x2lon(scrollX2));
                handleDrag(pmCurrent, dragData);
                return true;
            }
        }
        return false;
    }

    public class DragData{
        private float dragX;
        private float dragY;
        private PointModel dragOrigin = null;
        private Object dragObject = null;

        private DragData(){
            reset();
        }

        private boolean checkDragXY(float scrollX, float scrollY){
            return ((dragX == scrollX) && (dragY == scrollY));
        }
        private void setDragXY(float scrollX, float scrollY){
            dragX = scrollX;
            dragY = scrollY;
        }
        public PointModel getDragOrigin() {
            return dragOrigin;
        }

        public void setDragObject(Object object){
            dragObject = object;
        }
        public <T> T getDragObject(Class<T> tClass){
            return (tClass.isInstance(dragObject))?(T) dragObject:null;
        }
        public Object getDragObject(){
            return dragObject;
        }

        private void reset(){
            dragX = Float.MIN_VALUE;
            dragY = Float.MIN_VALUE;
            dragOrigin = null;
            dragObject = null;
        }
    }

    protected boolean checkDrag(PointModel pmStart, DragData dragData){
        return false;
    }
    protected void handleDrag(PointModel pmCurrent, DragData dragData){}

    protected void setDragging() {
        if (dragData == null) {
            dragData = new DragData();
        }
    }

    @Override
    public boolean onLongPress(LatLong tapLatLong, Point layerXY, Point tapXY) {
        return onLongPress(new PointModelImpl(tapLatLong));
    }

    protected boolean onLongPress(PointModel pm){
        return false;
    }
}
