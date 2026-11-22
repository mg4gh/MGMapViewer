package mg.mgmap.application.util;

import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.model.TrackLogPoint;
import mg.mgmap.generic.model.WriteablePointModel;
import mg.mgmap.generic.model.WriteablePointModelImpl;
import mg.mgmap.generic.util.Pref;

public class ElevationProviderImplHelper2 implements ElevationProvider{

    final HgtProvider2 hgtProvider;

    Pref<Boolean> prefBicubicInterpolation = new Pref<>(false);
    Pref<Boolean> prefBicubicSplineInterpolation = new Pref<>(false);

    public Pref<Boolean> getPrefBicubicInterpolation() {
        return prefBicubicInterpolation;
    }
    public Pref<Boolean> getPrefBicubicSplineInterpolation() {
        return prefBicubicSplineInterpolation;
    }


    public ElevationProviderImplHelper2(HgtProvider2 hgtProvider){
        this.hgtProvider = hgtProvider;
    }

    public void setElevation(TrackLogPoint tlp) {
        WriteablePointModel tlpAdapter = new WriteablePointModelImpl(tlp.getLat(), tlp.getLon()){
            @Override
            public void setEle(float elevation) {
                tlp.setHgtEle(elevation);
            }
            @Override
            public void setEleAcc(float eleAcc) {
                tlp.setHgtEleAcc(eleAcc);
            }
        };
        setElevation(tlpAdapter);
    }


    public void setElevation(WriteablePointModel wpm){
        double latitude = wpm.getLat();
        double longitude = wpm.getLon();
        int iLat = HgtProvider.getLower(latitude);
        int iLon = HgtProvider.getLower(longitude);
        if (latitude - iLat == 0){
            iLat--;
        }
        String hgtName = HgtProvider.getHgtName(iLat,iLon);
        HgtBuffer hb = hgtProvider.getHgtBuf(hgtName);
        if (hb != null){ // hgt files exists (real one or dummy)
            if (hb.isSeaLevel()){ // dummy file for sea level
                wpm.setEle(0);
                wpm.setEleAcc(0);
            } else { // ok, exists with content
                double dlat = 1 - (latitude - iLat);
                double dlat3600 = dlat * 3600;
                int oLat = (int) (dlat3600);
                double dlon = longitude - iLon;
                double dlon3600 = dlon * 3600;
                int oLon = (int) (dlon3600);
                int sIdx = oLat * 3601 + oLon;

                double nwEle = getEle(hb, sIdx);
                double neEle = getEle(hb, sIdx+1);
                double seEle = getEle(hb, sIdx+3602);
                double swEle = getEle(hb, sIdx+3601);

                double hi;
                if (prefBicubicInterpolation.getValue() && (1 <= oLon) && (oLon <= 3598) && (1 <= oLat) && (oLat <= 3598)){
                    double h0 = cubicInterpolate(getEle(hb,sIdx-3602), getEle(hb,sIdx-3601), getEle(hb,sIdx-3600), getEle(hb,sIdx-3599), dlon3600 - oLon);
                    double h1 = cubicInterpolate(getEle(hb,sIdx-1),    nwEle,                     neEle,                     getEle(hb,sIdx+2),    dlon3600 - oLon);
                    double h2 = cubicInterpolate(getEle(hb,sIdx+3600), swEle,                     seEle,                     getEle(hb,sIdx+3603), dlon3600 - oLon);
                    double h3 = cubicInterpolate(getEle(hb,sIdx+7201), getEle(hb,sIdx+7202), getEle(hb,sIdx+7203), getEle(hb,sIdx+7204), dlon3600 - oLon);
                    hi = cubicInterpolate(h0, h1, h2, h3, dlat3600 - oLat);
                } else {
                    double nhi = PointModelUtil.interpolate(0, 1, nwEle, neEle, dlon3600 - oLon);
                    double shi = PointModelUtil.interpolate(0, 1, swEle, seEle, dlon3600 - oLon);
                    hi = PointModelUtil.interpolate(0, 1, nhi, shi, dlat3600 - oLat);
                }
                double maxEle = Math.max( Math.max(nwEle,neEle), Math.max(seEle,swEle));
                double minEle = Math.min( Math.min(nwEle,neEle), Math.min(seEle,swEle));
                hi = Math.round(hi*PointModelUtil.ELE_FACTOR)/PointModelUtil.ELE_FACTOR;
                wpm.setEle((float) hi);
                wpm.setEleAcc((float) (maxEle-minEle));

            }
        } else {
            wpm.setEle(PointModel.NO_ELE);
            wpm.setEleAcc(PointModel.NO_ACC);
        }
    }

    private float getEle(HgtBuffer hgtBuffer, int sIdx){
        return hgtBuffer.get(sIdx);
    }

    public double cubicInterpolate(double y0, double y1, double y2, double y3, double t) {
        if (prefBicubicSplineInterpolation.getValue()){
            return cubicSplineInterpolate(y0, y1, y2, y3, t);
        } else {
            return cubicInterpolate1(y0, y1, y2, y3, t);
        }
    }

    public static double cubicInterpolate1(double y0, double y1, double y2, double y3, double t) {
        // Kubische Interpolation für gleichmäßig verteilte x
        double a = (-y0 + 3*y1  - 3*y2 + y3)/6;
        double b = (y0 - 2*y1 + y2)/2;
        double c = (y2 - y0)/2 - a;
        @SuppressWarnings("unused")
        double d = y1;

        return ((a * t + b) * t + c) * t + d;
    }

    public static double cubicSplineInterpolate(double y0, double y1, double y2, double y3, double t) {
        // Kubische Spline Interpolation für gleichmäßig verteilte x
        @SuppressWarnings("unused")
        double d = y1;
        double b =  (4*y0 -9*y1 +6*y2 -y3)/5;
        double c = y1 -y0 + (2*b)/3;
        double a = y2 -b -c -d;

        return ((a * t + b) * t + c) * t + d;
    }

}
