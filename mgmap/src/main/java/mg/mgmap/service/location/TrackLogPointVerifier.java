package mg.mgmap.service.location;

import java.lang.invoke.MethodHandles;
import java.util.Locale;

import mg.mgmap.R;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.model.TrackLogPoint;
import mg.mgmap.generic.util.PrefCache;
import mg.mgmap.generic.util.basic.MGLog;

public class TrackLogPointVerifier {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    protected static final float ACCURACY_LIMIT = 30.0f; // accuracy limit in meter
    protected static final float DEFAULT_HEIGHT_CONSISTENCY_THRESHOLD = 100.0f; // default for height consistency threshold in meter (diff between hgt and gps height)
    protected static final  float SPEED_INCREASE_FACTOR_LIMIT = 4.0f;
    protected static final  float HGT_PRESSURE_DIFF_LIMIT = 4.0f;

    private final PrefCache prefCache;

    private double heightConsistencyThreshold;

    private TrackLogPoint lastTrackLogPoint;
    private TrackLogPoint secondLastTrackLogPoint;

    TrackLogPointVerifier(MGMapApplication application){
        prefCache = application.getPrefCache();
        try {
            heightConsistencyThreshold = Double.parseDouble( prefCache.get(R.string.preferences_height_consistency_check_key,  Double.toString(DEFAULT_HEIGHT_CONSISTENCY_THRESHOLD)).getValue() );
        } catch (NumberFormatException e) {
            mgLog.e(e);
            heightConsistencyThreshold = DEFAULT_HEIGHT_CONSISTENCY_THRESHOLD;
        }
    }

    boolean verify(TrackLogPoint lp){
        if (staticLocationVerification(lp) && dynamicLocationVerification(lp,lastTrackLogPoint,secondLastTrackLogPoint)){
            secondLastTrackLogPoint = lastTrackLogPoint;
            lastTrackLogPoint = lp;
            return true;
        }
        return false;
    }

    protected boolean staticLocationVerification(TrackLogPoint lp){
        if ((lp.getNmeaAcc() != PointModel.NO_ACC) && (lp.getNmeaAcc() >= ACCURACY_LIMIT)){
            mgLog.w("location dropped lp.getNmeaAcc()="+lp.getNmeaAcc());
            return false;
        }
        if ((lp.getNmeaEle() != PointModel.NO_ELE) && (lp.getHgtEle() != PointModel.NO_ELE) && (Math.abs(lp.getNmeaEle() - lp.getHgtEle()) >= heightConsistencyThreshold )){
            mgLog.w("location dropped nmeaEle="+lp.getNmeaEle()+ " hgtEle()="+lp.getHgtEle()+" heightConsistencyThreshold="+heightConsistencyThreshold);
            return false;
        }
        return true;
    }

    protected boolean dynamicLocationVerification(TrackLogPoint lp, TrackLogPoint lastLp, TrackLogPoint secondLP){
        if ((lastLp == null) || (secondLP == null)) return true; // checks make only sense, if there is a history to compare with
        mgLog.d(lp.toLongString());
        mgLog.d(lastLp.toLongString());
        mgLog.d(secondLP.toLongString());

        double distance = PointModelUtil.distance(lp, lastLp);
        double dTime = lp.getTimestamp() - lastLp.getTimestamp();
        double speed = Math.max(3, distance/(dTime / 1000)); // sped in m/s

        double distance2 = PointModelUtil.distance(lastLp, secondLP);
        double dTime2 = lastLp.getTimestamp() - secondLP.getTimestamp();
        double speed2 = Math.max(3, distance2/(dTime2 / 1000)); // sped in m/s

        mgLog.d(String.format(Locale.ENGLISH, "distance=%.2f,dTime=%.2f,speed=%.2f",distance,dTime,speed));
        mgLog.d(String.format(Locale.ENGLISH,"distance2=%.2f,dTime2=%.2f,speed2=%.2f",distance2,dTime2,speed2));
        if (speed / speed2 > SPEED_INCREASE_FACTOR_LIMIT){
            mgLog.w("location dropped - speed increase factor="+(speed/speed2));
            return false;
        }

        if ((lp.getPressureEle() != PointModel.NO_ELE) && (lastLp.getPressureEle() != PointModel.NO_ELE) && (lp.getHgtEle() != PointModel.NO_ELE) && (lastLp.getHgtEle() != PointModel.NO_ELE) ){
            double pressureEleDiff = Math.max(5, Math.abs(lp.getPressureEle() - lastLp.getPressureEle()));
            double hgtEleDiff = Math.max(5, Math.abs(lp.getHgtEle() - lastLp.getHgtEle()));
            mgLog.d(String.format(Locale.ENGLISH,"pressureEleDiff=%.2f,hgtEleDiff=%.2f",pressureEleDiff,hgtEleDiff));
            if (hgtEleDiff / pressureEleDiff > HGT_PRESSURE_DIFF_LIMIT){
                mgLog.w("location dropped - hgt to pressure diff increase factor="+(hgtEleDiff / pressureEleDiff));
                return false;
            }
        }
        return true;
    }


}
