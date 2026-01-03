package mg.mgmap.activity.mgmap.features.shareloc;

import androidx.lifecycle.Lifecycle;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.FeatureService;
import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.generic.model.MultiPointModel;
import mg.mgmap.generic.model.MultiPointModelImpl;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.model.TrackLogRefApproach;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.view.ExtendedTextView;

public class FSShareLoc extends FeatureService {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    static final String DATE_FORMAT = "dd.MM.yy HH:mm";

    private final Pref<Boolean> prefGps = getPref(R.string.FSPosition_pref_GpsOn, false);
    private final Pref<Boolean> prefCenter = getPref(R.string.FSPosition_pref_Center, true);
    private final Pref<Boolean> prefEditMarkerTrack =  getPref(R.string.FSMarker_qc_EditMarkerTrack, false);
    Pref<Boolean> toggleShareLocation = new Pref<>(true);
    Pref<Boolean> shareWithActive = new Pref<>(false);
    Pref<Boolean> shareFromActive = new Pref<>(false);
    Pref<Boolean> showLocationText = new Pref<>(false);


    SharePerson me = null;
    ShareLocConfig shareLocConfig;
    LocationSender locationSender = null;
    LocationReceiver locationReceiver = null;
    final Map<String, MultiPointModelImpl> shareLocMap = new HashMap<>();

    public FSShareLoc(MGMapActivity mmActivity) {
        super(mmActivity);
        File fMyCrt = new File(application.getFilesDir(), "certs/my.crt");
        if (fMyCrt.exists()){
            try (InputStream clientCrt = new FileInputStream(fMyCrt) ) {
                me = CryptoUtils.getPersonData(clientCrt);
            } catch (Exception e) { mgLog.e(e); }
        }
        shareLocConfig = new ShareLocConfig(getApplication());
        shareLocConfig.loadConfig(me);
        shareLocConfig.addObserver(propertyChangeEvent -> {
            if (locationSender != null){
                locationSender.setConfig(shareLocConfig);
            }
            updateShareWithActive();
            updateShareFromActive();
        });
        showLocationText.addObserver(refreshObserver);

        Runnable ttHideText = ()->showLocationText.setValue(false);
        showLocationText.addObserver(pce->{
            if (showLocationText.getValue()){
                getTimer().postDelayed(ttHideText, 10000);
            } else {
                getTimer().removeCallbacks(ttHideText);
            }
        });

        toggleShareLocation.addObserver(plc -> new ShareLocationSettings(mmActivity, shareLocConfig, me).show());

        shareWithActive.addObserver(pce->{
            if (shareWithActive.getValue()){
                if (locationSender == null){
                    locationSender =  (LocationSender) getApplication().lastPositionsObservable.findObserver(LocationSender.class);
                    if (locationSender == null){
                        mgLog.d("create new LocationSender");
                        locationSender = new LocationSender(getApplication(), me, shareLocConfig);
                    } else {
                        shareLocConfig.changed();
                    }
                }
            } else {
                if (locationSender != null){
                    locationSender.stop();
                    locationSender = null;
                }
            }
        });

        shareFromActive.addObserver(propertyChangeEvent -> {
            if (shareFromActive.getValue()){ // should be active
                if (locationReceiver == null){
                    try {
                        locationReceiver = new LocationReceiver(getApplication(), this, me, shareLocConfig, shareLocMap);
                    } catch (Exception e) {
                        mgLog.e(e);
                    }
                }
            } else {
                if (locationReceiver != null){
                    locationReceiver.stop();
                    locationReceiver = null;
                }
            }
        });

        prefGps.addObserver(pce->updateShareWithActive());

        new Thread(() -> {
            while (true){
                try {
                    synchronized (shareLocMap){
                        shareLocMap.wait(30000);
                    }
                    refreshObserver.onChange();
                } catch (Exception e) {
                    mgLog.e(e.getMessage());
                }
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshObserver.onChange();
    }

    @Override
    protected void doRefreshResumedUI() {
        updateShareFromActive();

        unregisterAll();
        for (SharePerson person : shareLocConfig.persons){
            MultiPointModel mpm = shareLocMap.get(person.email);
            if (mpm != null){
                register(new MultiPointViewSL(mpm, person.color));
                register(new PointViewShareLoc(mpm.get(mpm.size()-1), person, showLocationText));
            }
        }

    }

    void updateShareWithActive(){
        shareWithActive.setValue(prefGps.getValue() && shareLocConfig.isShareWithActive());
    }
    void updateShareFromActive(){
        boolean resumed = (getActivity().getLifecycle().getCurrentState() == Lifecycle.State.RESUMED);
        shareFromActive.setValue(resumed && shareLocConfig.isShareFromActive());
    }





    public ExtendedTextView initQuickControl(ExtendedTextView etv, String info) {
        super.initQuickControl(etv,info);
        if ("shareloc".equals(info)){
            etv.setData(R.drawable.shareloc);
            etv.setPrAction(toggleShareLocation);
            etv.setHelp(r(R.string.FSShareLoc_qcLsd_help));
        }
        return etv;
    }

    void onLocationReceived(PointModel pm){
        if ((!prefEditMarkerTrack.getValue()) && (!prefGps.getValue() || !prefCenter.getValue())){
            getMapViewUtility().setCenter(pm);
        }
    }

    public boolean checkClose(PointModel point) {
        TrackLogRefApproach best = new TrackLogRefApproach(null, -1, getMapViewUtility().getCloseThresholdForZoomLevel());
        for (MultiPointModel mpm : shareLocMap.values()){
            PointModelUtil.getBestDistance(mpm, point, best);
            if (best.getEndPointIndex() > 0){
                showLocationText.toggle();
                return true;
            }
        }
        return false;
    }
}
