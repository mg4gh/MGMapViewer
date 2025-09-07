package mg.mgmap.application.util;

import java.nio.ByteBuffer;

import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.model.TrackLogPoint;
import mg.mgmap.generic.model.WriteablePointModel;
import mg.mgmap.generic.model.WriteablePointModelImpl;
import mg.mgmap.generic.util.basic.MGLog;

public class ElevationProviderImplHelper2 implements ElevationProvider{

    final HgtProvider2 hgtProvider;

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

//    public void setElevationX(WriteablePointModel wpm){
//        double latitude = wpm.getLat();
//        double longitude = wpm.getLon();
//        int iLat = HgtProvider.getLower(latitude);
//        int iLon = HgtProvider.getLower(longitude);
//        if (latitude - iLat == 0){
//            iLat--;
//        }
//        String hgtName = HgtProvider.getHgtName(iLat,iLon);
//        byte[] hgtBuf = hgtProvider.getHgtBuf(hgtName);
//        if ((hgtBuf != null) && (hgtBuf.length > 0)){ // hgt files exists (real one or dummy)
//            if (hgtBuf.length > 1){ // ok, exists with content (length 1 indicates dummy file for sea level)
//                double dlat = 1 - (latitude - iLat);
//                int oLat = (int) (dlat * 3600);
//                double dlon = longitude - iLon;
//                int oLon = (int) (dlon * 3600);
//
//                double nwLat = iLat + (1 - oLat / 3600.0);  // nw - northWest
//                double nwLon = iLon + (oLon / 3600.0);
//                double nwEle = getEle(hgtBuf, oLat, oLon);
//                oLon++;
//                double neLon = iLon + (oLon / 3600.0);     // ne - northEast
//                double neEle = getEle(hgtBuf, oLat, oLon);
//                oLat++;
//                double seLon = iLon + (oLon / 3600.0);     // se - southEast
//                double seEle = getEle(hgtBuf, oLat, oLon);
//                oLon--;
//                double swLat = iLat + (1 - oLat / 3600.0); // sw - southWest
//                double swLon = iLon + (oLon / 3600.0);
//                double swEle = getEle(hgtBuf, oLat, oLon);
//
//                double nhi = interpolate(nwLon, neLon, nwEle, neEle, longitude);
//                double shi = interpolate(swLon, seLon, swEle, seEle, longitude);
//                double hi = interpolate(nwLat, swLat, nhi, shi, latitude);
//
//                double maxEle = Math.max( Math.max(nwEle,neEle), Math.max(seEle,swEle));
//                double minEle = Math.min( Math.min(nwEle,neEle), Math.min(seEle,swEle));
//
//                wpm.setEle((float) hi);
//                wpm.setEleAcc((float) (maxEle-minEle));
//            } else { // dummy file for sea level
//                wpm.setEle(0);
//                wpm.setEleAcc(0);
//            }
//        } else {
//            wpm.setEle(PointModel.NO_ELE);
//            wpm.setEleAcc(PointModel.NO_ACC);
//        }
////        MGLog.sd(wpm.getLat()+" "+wpm.getLon()+" "+wpm.getEle());
//    }


    public void setElevation(WriteablePointModel wpm){
//        setElevationX(wpm);
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

                double nhi = interpolate(0, 1, nwEle, neEle, dlon3600 - oLon);
                double shi = interpolate(0, 1, swEle, seEle, dlon3600 - oLon);
                double hi = interpolate(0, 1, nhi, shi, dlat3600 - oLat);

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


//    private float getEle(byte[] hgtBuf, int oLat, int oLon) {
//        int latOffset = oLat * 3601 * 2;
//        int lonOffset = oLon * 2;
//        int off = (latOffset+lonOffset);
//        float res = PointModel.NO_ELE;
//        if (hgtBuf.length >= off+2){
//            byte b1 = hgtBuf[off];
//            byte b2 = hgtBuf[off+1];
//            res = (short)( ((b1 & 0xff)<<8) + (b2 & 0xff) );
//        }
//        return res;
//    }

    private float getEle(byte[] hgtBuf, int off) {
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
