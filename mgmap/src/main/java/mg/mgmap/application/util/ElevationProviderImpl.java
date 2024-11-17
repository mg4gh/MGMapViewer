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

import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.model.TrackLogPoint;
import mg.mgmap.generic.model.WriteablePointModel;
import mg.mgmap.generic.model.WriteablePointModelImpl;

/** Provide an elevation value for a given position on the .hgt file basis. */
public class ElevationProviderImpl implements ElevationProvider{

    final HgtProvider hgtProvider;

    public ElevationProviderImpl(HgtProvider hgtProvider){
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
        byte[] hgtBuf = hgtProvider.getHgtBuf(hgtName);
        if ((hgtBuf != null) && (hgtBuf.length > 0)){ // hgt files exists (real one or dummy)
            if (hgtBuf.length > 1){ // ok, exists with content (length 1 indicates dummy file for sea level)
                double dlat = 1 - (latitude - iLat);
                int oLat = (int) (dlat * 3600);
                double dlon = longitude - iLon;
                int oLon = (int) (dlon * 3600);

                double nwLat = iLat + (1 - oLat / 3600.0);  // nw - northWest
                double nwLon = iLon + (oLon / 3600.0);
                double nwEle = getEle(hgtBuf, oLat, oLon);
                oLon++;
                double neLon = iLon + (oLon / 3600.0);     // ne - northEast
                double neEle = getEle(hgtBuf, oLat, oLon);
                oLat++;
                double seLon = iLon + (oLon / 3600.0);     // se - southEast
                double seEle = getEle(hgtBuf, oLat, oLon);
                oLon--;
                double swLat = iLat + (1 - oLat / 3600.0); // sw - southWest
                double swLon = iLon + (oLon / 3600.0);
                double swEle = getEle(hgtBuf, oLat, oLon);

                double nhi = interpolate(nwLon, neLon, nwEle, neEle, longitude);
                double shi = interpolate(swLon, seLon, swEle, seEle, longitude);
                double hi = interpolate(nwLat, swLat, nhi, shi, latitude);

                double maxEle = Math.max( Math.max(nwEle,neEle), Math.max(seEle,swEle));
                double minEle = Math.min( Math.min(nwEle,neEle), Math.min(seEle,swEle));

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

    private float getEle(byte[] hgtBuf, int oLat, int oLon) {
        int latOffset = oLat * 3601 * 2;
        int lonOffset = oLon * 2;
        int off = (latOffset+lonOffset);
        float res = PointModel.NO_ELE;
        if (hgtBuf.length >= off+2){
            byte b1 = hgtBuf[off];
            byte b2 = hgtBuf[off+1];
            res = (short)( ((b1 & 0xff)<<8) + (b2 & 0xff) );
        }
        return res;
    }

    private double interpolate(double refMin, double refMax, double valMin, double valMax, double ref){
        return PointModelUtil.interpolate(refMin,refMax, valMin, valMax, ref);
    }
}
