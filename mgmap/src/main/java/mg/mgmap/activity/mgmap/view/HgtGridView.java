package mg.mgmap.activity.mgmap.view;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Path;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.model.DisplayModel;

import java.util.HashMap;
import java.util.Map;

import mg.mgmap.R;
import mg.mgmap.generic.util.CC;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.application.util.HgtProvider;

public class HgtGridView extends Grid {

    private static final Map<Byte, Double> spacingConfig = new HashMap<>() {
        {
            put((byte) 5, 1.0);
            put((byte) 6, 1.0);
            put((byte) 7, 1.0);
            put((byte) 8, 1.0);
            put((byte) 9, 1.0);
            put((byte) 10, 1.0);
            put((byte) 11, 1.0);
            put((byte) 12, 1.0);
            put((byte) 13, 1.0);
            put((byte) 14, 1.0);
            put((byte) 15, 1.0);
            put((byte) 16, 1.0);
        }
    };

    final GraphicFactory graphicFactory;
    final MGMapApplication application;
    final Paint hgtAvail;
    final Paint hgtNotAvail;

    public HgtGridView(MGMapApplication application, GraphicFactory graphicFactory, DisplayModel displayModel) {
        super(graphicFactory, displayModel, spacingConfig);
        this.graphicFactory = graphicFactory;
        this.application = application;

        hgtAvail = graphicFactory.createPaint();
        hgtAvail.setStyle(Style.FILL);

        hgtNotAvail = graphicFactory.createPaint();
        hgtNotAvail.setColor(CC.getColor(R.color.CC_RED));
        hgtNotAvail.setStyle(Style.FILL);

    }

    public void setAlpha(float alpha){
        alpha = Math.max(0, Math.min(1, alpha));
        super.setAlpha(alpha);
        hgtAvail.setColor(CC.addAlpha(CC.getColor(R.color.CC_GREEN), alpha));
        hgtNotAvail.setColor(CC.addAlpha(CC.getColor(R.color.CC_RED), alpha));
    }


    @Override
    public void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) {

        Double spacing = spacingConfig.get(zoomLevel);
        if (spacing != null){

            double minLongitude = spacing * (Math.floor(boundingBox.minLongitude / spacing));
            double maxLongitude = spacing * (Math.ceil(boundingBox.maxLongitude / spacing));
            double minLatitude = spacing * (Math.floor(boundingBox.minLatitude / spacing));
            double maxLatitude = spacing * (Math.ceil(boundingBox.maxLatitude / spacing));

            long mapSize = MercatorProjection.getMapSize(zoomLevel, this.displayModel.getTileSize());


            int margin = (int)(7 * displayModel.getScaleFactor());
            for (double latitude = minLatitude; latitude <= maxLatitude; latitude += spacing) {
                int pixelY1 = (int) (MercatorProjection.latitudeToPixelY(latitude, mapSize) - topLeftPoint.y) - margin;
                int pixelY2 = (int) (MercatorProjection.latitudeToPixelY(latitude+spacing, mapSize) - topLeftPoint.y) + margin;

                for (double longitude = minLongitude; longitude <= maxLongitude; longitude += spacing) {
                    int pixelX1 = (int) (MercatorProjection.longitudeToPixelX(longitude, mapSize) - topLeftPoint.x) + margin;
                    int pixelX2 = (int) (MercatorProjection.longitudeToPixelX(longitude+spacing, mapSize) - topLeftPoint.x) - margin;

                    Path path = graphicFactory.createPath();
                    path.moveTo(pixelX1, pixelY1);
                    path.lineTo(pixelX2, pixelY1);
                    path.lineTo(pixelX2, pixelY2);
                    path.lineTo(pixelX1, pixelY2);
                    path.lineTo(pixelX1, pixelY1);
                    String hgtName = HgtProvider.getHgtName(HgtProvider.getLower(latitude),HgtProvider.getLower(longitude));
                    canvas.drawPath(path, application.getHgtProvider().hgtIsAvailable(hgtName)?hgtAvail:hgtNotAvail);
                }
            }
        } // if (spacing != null)
        super.draw(boundingBox, zoomLevel, canvas, topLeftPoint);

    }

}