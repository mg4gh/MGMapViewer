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

import android.content.Context;
import android.graphics.Point;
import android.util.DisplayMetrics;

import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.scalebar.DefaultMapScaleBar;
import org.mapsforge.map.scalebar.MapScaleBar;

import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.generic.model.BBox;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.util.basic.MGLog;

/**
 * Utility for functionality related to the mapsforge MapView.
 */
public class MapViewUtility {

    public static final byte ZOOM_LEVEL_MIN = 1;
    public static final byte ZOOM_LEVEL_MAX = 24;
    public static final float DEFAULT_TRACK_WIDTH = 4;

    final private Context context;
    final private MapView mapView;

    public MapViewUtility(Context context, MapView mapView) {
        this.context = context;
        this.mapView = mapView;
    }

    public void zoomForBoundingBox(BBox bBox) {
        int tileSize = this.mapView.getModel().displayModel.getTileSize();
        Dimension dimension = this.mapView.getModel().mapViewDimension.getDimension();
        if (dimension == null) {
            DisplayMetrics dm = context.getResources().getDisplayMetrics();
            dimension = new Dimension(dm.widthPixels, dm.heightPixels);
        }

        long mapSize = MercatorProjection.getMapSize((byte) 0, tileSize);
        double pixelXMax = MercatorProjection.longitudeToPixelX(bBox.maxLongitude, mapSize);
        double pixelXMin = MercatorProjection.longitudeToPixelX(bBox.minLongitude, mapSize);
        double zoomX = -Math.log(Math.abs(pixelXMax - pixelXMin) / dimension.width) / Math.log(2);
        double pixelYMax = MercatorProjection.latitudeToPixelY(bBox.maxLatitude, mapSize);
        double pixelYMin = MercatorProjection.latitudeToPixelY(bBox.minLatitude, mapSize);
        double zoomY = -Math.log(Math.abs(pixelYMax - pixelYMin) / dimension.height) / Math.log(2);
        double zoom = Math.floor(Math.min(zoomX, zoomY));
        byte bzoom = (byte) Math.min(Math.max(zoom, 6), 18);
        setCenter(bBox.getCenter());
        this.mapView.getModel().mapViewPosition.setZoomLevel(bzoom);
    }

    public double getCloseThresholdForZoomLevel(double limit) {
        return Math.min(limit, getCloseThresholdForZoomLevel());
    }

    public double getCloseThresholdForZoomLevel() {
        int currentZoomLevel = this.mapView.getModel().mapViewPosition.getZoomLevel();
        double closeThresholdForZoomLevel = PointModelUtil.getCloseThreshold();
        closeThresholdForZoomLevel = closeThresholdForZoomLevel / (1 << 9);
        closeThresholdForZoomLevel = closeThresholdForZoomLevel * (1 << (25 - currentZoomLevel));
        MGLog.sd("currentZoomLevel="+currentZoomLevel+" closeThreshold="+closeThresholdForZoomLevel * 1.5);
        return closeThresholdForZoomLevel * 1.5;
    }

    public boolean isClose(double distance) {
        return distance < getCloseThresholdForZoomLevel();
    }


    public Dimension getMapViewDimension() {
        return mapView.getDimension();
    }

    /** @noinspection unused*/
    public BBox getMapViewBBox() {
        return BBox.fromBoundingBox(mapView.getBoundingBox());
    }

    public int getZoomLevel() {
        return this.mapView.getModel().mapViewPosition.getZoomLevel();
    }
    public void setZoomLevel(byte zoom) {
        this.mapView.getModel().mapViewPosition.setZoomLevel(zoom);
    }

    public PointModel getCenter() {
        return new PointModelImpl(this.mapView.getModel().mapViewPosition.getCenter());
    }
    public void setCenter(PointModel pm){
        if (pm.getLaLo() != PointModelUtil.NO_POS){
            this.mapView.getModel().mapViewPosition.setCenter(new LatLong(pm.getLat(),pm.getLon()));
        }
    }

    public float getTrackWidth() {
        return DEFAULT_TRACK_WIDTH * mapView.getModel().displayModel.getScaleFactor();
    }

    public Point getPoint4PointModel(PointModel pm) {
        int[] loc = new int[2];
        this.mapView.getLocationOnScreen(loc);
        int tileSize = this.mapView.getModel().displayModel.getTileSize();
        Dimension dimension = this.mapView.getModel().mapViewDimension.getDimension();
        if (dimension == null) {
            DisplayMetrics dm = context.getResources().getDisplayMetrics();
            dimension = new Dimension(dm.widthPixels, dm.heightPixels);
        }

        PointModel pmCenter = getCenter();
        long mapSize = MercatorProjection.getMapSize(mapView.getModel().mapViewPosition.getZoomLevel(), tileSize);
        double dx = MercatorProjection.longitudeToPixelX(pm.getLon(), mapSize) - MercatorProjection.longitudeToPixelX(pmCenter.getLon(), mapSize) + dimension.width / 2.0;
        double dy = MercatorProjection.latitudeToPixelY(pm.getLat(), mapSize) - MercatorProjection.latitudeToPixelY(pmCenter.getLat(), mapSize) + dimension.height / 2.0;
        return new Point((int) dx + loc[0], (int) dy + loc[1]);
    }

    public PointModel getPointModel4Point(Point p) {
        int[] loc = new int[2];
        this.mapView.getLocationOnScreen(loc);
        int tileSize = this.mapView.getModel().displayModel.getTileSize();
        Dimension dimension = this.mapView.getModel().mapViewDimension.getDimension();
        if (dimension == null) {
            DisplayMetrics dm = context.getResources().getDisplayMetrics();
            dimension = new Dimension(dm.widthPixels, dm.heightPixels);
        }

        PointModel pmCenter = getCenter();
        long mapSize = MercatorProjection.getMapSize(mapView.getModel().mapViewPosition.getZoomLevel(), tileSize);
        double lon = MercatorProjection.pixelXToLongitude(MercatorProjection.longitudeToPixelX(pmCenter.getLon(), mapSize) - dimension.width / 2.0 + p.x - loc[0], mapSize);
        double lat = MercatorProjection.pixelYToLatitude(MercatorProjection.latitudeToPixelY(pmCenter.getLat(), mapSize) - dimension.height / 2.0 + p.y - loc[1], mapSize);
        return new PointModelImpl(lat, lon);
    }

    public void setScaleBarVMargin(int vMargin){
        MapScaleBar mapScaleBar = mapView.getMapScaleBar();
        if (mapScaleBar != null){
            mapScaleBar.setMarginVertical(vMargin);
            mapScaleBar.redrawScaleBar();
        }
    }
    public void setScaleBarColor(int color){
        if (mapView.getMapScaleBar() instanceof DefaultMapScaleBar mapScaleBar) {
            mapScaleBar.setColor(color);
            mapView.getMapScaleBar().redrawScaleBar();
        }
    }
    public void setScaleBarVisibility(boolean visibility){
        MapScaleBar mapScaleBar = mapView.getMapScaleBar();
        if (mapScaleBar != null) {
            mapScaleBar.setVisible(visibility);
            mapView.invalidate();
        }
    }

    public boolean getTrackVisibility(){
        return ((MGMapActivity)context).getTrackVisibility();
    }
}