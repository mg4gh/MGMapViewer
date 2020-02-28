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

import android.content.Context;
import android.util.DisplayMetrics;

import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.android.view.MapView;

import mg.mapviewer.model.BBox;

/**
 * Utility for functionality related to the mapsforge MapView.
 */
public class MapViewUtility {

    public static final float DEFAULT_TRACK_WIDTH = 4;

    private Context context;
    private MapView mapView;

    public MapViewUtility(Context context, MapView mapView){
        this.context = context;
        this.mapView = mapView;
    }

    public void zoomForBoundingBox(BBox bBox){
        int tileSize = this.mapView.getModel().displayModel.getTileSize();
        Dimension dimension = this.mapView.getModel().mapViewDimension.getDimension();
        if (dimension == null){
            DisplayMetrics dm = context.getResources().getDisplayMetrics();
            dimension = new Dimension(dm.widthPixels,dm.heightPixels);
        }

        long mapSize = MercatorProjection.getMapSize((byte) 0, tileSize);
        double pixelXMax = MercatorProjection.longitudeToPixelX(bBox.maxLongitude, mapSize);
        double pixelXMin = MercatorProjection.longitudeToPixelX(bBox.minLongitude, mapSize);
        double zoomX = -Math.log(Math.abs(pixelXMax - pixelXMin) / dimension.width) / Math.log(2);
        double pixelYMax = MercatorProjection.latitudeToPixelY(bBox.maxLatitude, mapSize);
        double pixelYMin = MercatorProjection.latitudeToPixelY(bBox.minLatitude, mapSize);
        double zoomY = -Math.log(Math.abs(pixelYMax - pixelYMin) / dimension.height) / Math.log(2);
        double zoom = Math.floor(Math.min(zoomX, zoomY));
        byte bzoom = (byte)Math.min( Math.max(zoom,0), Byte.MAX_VALUE);
        this.mapView.getModel().mapViewPosition.setMapPosition(new MapPosition( bBox.getCenter(), bzoom));
    }

    public double getCloseThreshouldForZoomLevel(){
        int currentZoomLevel = this.mapView.getModel().mapViewPosition.getZoomLevel();
        double closeThreshouldForZoomLevel = PointModelUtil.getCloseThreshold();
        closeThreshouldForZoomLevel = closeThreshouldForZoomLevel / (1<<9);
        closeThreshouldForZoomLevel = closeThreshouldForZoomLevel * (1<<(25-currentZoomLevel));
        return closeThreshouldForZoomLevel;
    }

    public boolean isClose(double distance){
        return distance < getCloseThreshouldForZoomLevel();
    }


    public Dimension getMapViewDimension(){
        return mapView.getDimension();
    }

    public BBox getMapViewBBox(){
        return BBox.fromBoundingBox(mapView.getBoundingBox());
    }

    public double getZoomLevel() {
        return this.mapView.getModel().mapViewPosition.getZoomLevel();
    }

    public float getTrackWidth(){
        return DEFAULT_TRACK_WIDTH * mapView.getModel().displayModel.getScaleFactor();
    }
}
