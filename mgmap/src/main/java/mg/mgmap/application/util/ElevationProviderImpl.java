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
package mg.mgmap.application.util;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.model.TrackLogPoint;
import mg.mgmap.generic.model.WriteablePointModel;
import mg.mgmap.generic.model.WriteablePointModelImpl;
import mg.mgmap.generic.util.Pref;

/** Provide an elevation value for a given position on the .hgt file basis. */
public class ElevationProviderImpl implements ElevationProvider{

    final HgtProvider hgtProvider;
    final Pref<Boolean> prefBicubicInterpolation;
    final Pref<Boolean> prefBicubicSplineInterpolation;

    public ElevationProviderImpl(MGMapApplication mgMapApplication, HgtProvider hgtProvider){
        this.hgtProvider = hgtProvider;
        prefBicubicInterpolation = mgMapApplication.getPrefCache().get("prefUseBicubicInterpolation", false);
        prefBicubicSplineInterpolation = mgMapApplication.getPrefCache().get("prefUseBicubicSplineInterpolation", false);
    }
    public ElevationProviderImpl(HgtProvider hgtProvider, Pref<Boolean> prefBicubicInterpolation, Pref<Boolean> prefBicubicSplineInterpolation){
        this.hgtProvider = hgtProvider;
        this.prefBicubicInterpolation = prefBicubicInterpolation;
        this.prefBicubicSplineInterpolation = prefBicubicSplineInterpolation;
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
        byte[] hgtBuf = hgtProvider.getHgtBuf(hgtName);
        if ((hgtBuf != null) && (hgtBuf.length > 0)){ // hgt files exists (real one or dummy)
            if (hgtBuf.length > 1){ // ok, exists with content (length 1 indicates dummy file for sea level)
                double dlat = 1 - (latitude - iLat);
                double dlat3600 = dlat * 3600;
                int oLat = (int) (dlat3600);
                double dlon = longitude - iLon;
                double dlon3600 = dlon * 3600;
                int oLon = (int) (dlon3600);
                int off = oLat * 7202 + oLon * 2;

                double nwEle = getEle(hgtBuf, off);
                double neEle = getEle(hgtBuf, off+2);
                double seEle = getEle(hgtBuf, off+7204);
                double swEle = getEle(hgtBuf, off+7202);

                double hi, maxEle, minEle;
                if (prefBicubicInterpolation.getValue() && (1 <= oLon) && (oLon <= 3598) && (1 <= oLat) && (oLat <= 3598)){
                    double h0 = cubicInterpolate(getEle(hgtBuf,off-7204),getEle(hgtBuf,off-7202),getEle(hgtBuf,off-7200),getEle(hgtBuf,off-7198), dlon3600 - oLon);
                    double h1 = cubicInterpolate(getEle(hgtBuf,off-2),nwEle,neEle,getEle(hgtBuf,off+4), dlon3600 - oLon);
                    double h2 = cubicInterpolate(getEle(hgtBuf,off+7200),swEle,seEle,getEle(hgtBuf,off+7206), dlon3600 - oLon);
                    double h3 = cubicInterpolate(getEle(hgtBuf,off+14402),getEle(hgtBuf,off+14404),getEle(hgtBuf,off+14406),getEle(hgtBuf,off+14408), dlon3600 - oLon);
                    hi = cubicInterpolate(h0, h1, h2, h3, dlat3600 - oLat);
                } else {
                    double nhi = PointModelUtil.interpolate(0, 1, nwEle, neEle, dlon3600 - oLon);
                    double shi = PointModelUtil.interpolate(0, 1, swEle, seEle, dlon3600 - oLon);
                    hi = PointModelUtil.interpolate(0, 1, nhi, shi, dlat3600 - oLat);
                }
                maxEle = Math.max( Math.max(nwEle,neEle), Math.max(seEle,swEle));
                minEle = Math.min( Math.min(nwEle,neEle), Math.min(seEle,swEle));
                hi = Math.round(hi*PointModelUtil.ELE_FACTOR)/PointModelUtil.ELE_FACTOR;
                wpm.setEle((float) hi);
                wpm.setEleAcc((float) (maxEle-minEle));

            } else { // dummy file for sea level
                wpm.setEle(0);
                wpm.setEleAcc(0);
            }
        } else {
            wpm.setEle(PointModel.NO_ELE);
            wpm.setEleAcc(PointModel.NO_ACC);
        }
    }

    private float getEle(byte[] hgtBuf, int off) {
        float res = PointModel.NO_ELE;
        if (hgtBuf.length >= off+2){
            byte b1 = hgtBuf[off];
            byte b2 = hgtBuf[off+1];
            res = (short)( ((b1 & 0xff)<<8) + (b2 & 0xff) );
        }
        return res;
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
