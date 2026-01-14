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
import mg.mgmap.generic.util.Observer;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.view.ExtendedTextView;

public class FSShareLoc extends FeatureService {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    static final String DATE_FORMAT = "dd.MM.yy HH:mm";

    private final Pref<Boolean> prefShareLoc = getPref(R.string.FSShareLoc_shareLocOn_key, false);
    private final Pref<Boolean> prefGps = getPref(R.string.FSPosition_pref_GpsOn, false);
    private final Pref<Boolean> prefCenter = getPref(R.string.FSPosition_pref_Center, true);
    private final Pref<Boolean> prefEditMarkerTrack =  getPref(R.string.FSMarker_qc_EditMarkerTrack, false);
    Pref<Boolean> toggleShareLocation = new Pref<>(true);
    Pref<Boolean> shareWithActive = getPref(R.string.FSShareLoc_shareWithActive,false);
    Pref<Boolean> shareFromActive = new Pref<>(false);
    Pref<Boolean> showLocationText = new Pref<>(false);


    SharePerson me = null;
    ShareLocConfig shareLocConfig;
    LocationSender locationSender = null;
    LocationReceiver locationReceiver = null;
    final Map<String, MultiPointModelImpl> shareLocMap = new HashMap<>();
    Observer gpsObserver = pce->updateShareWithActive();
    boolean active = false;


    public FSShareLoc(MGMapActivity mmActivity) {
        super(mmActivity);
        shareWithActive.setValue(false);

        prefShareLoc.addObserver(pce->{
            if (prefShareLoc.getValue()){
                activate();
            } else {
                deactivate();
            }
        });
    }

    void activate(){
        if (active) return;

        File fMyCrt = new File(application.getFilesDir(), "certs/my.crt");
        if (fMyCrt.exists()){
            try (InputStream clientCrt = new FileInputStream(fMyCrt) ) {
                me = CryptoUtils.getPersonData(clientCrt);
            } catch (Exception e) { mgLog.e(e); }
        }
        if (me == null){
            me = new SharePerson();
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

        toggleShareLocation.addObserver(plc -> new ShareLocationSettings(getActivity(), shareLocConfig, me).show());

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


        prefGps.addObserver(gpsObserver);

        active = true;
        new Thread(() -> {
            while (active){
                try {
                    synchronized (shareLocMap){
                        shareLocMap.wait(30000);
                    }
                    if (active){
                        refreshObserver.onChange();
                    }
                } catch (Exception e) {
                    mgLog.e(e.getMessage());
                }
            }
        }).start();
    }

    void deactivate(){
        if (!active) return;

        try {
            unregisterAll();
            if (locationSender != null){
                locationSender.stop();
            }
            if (locationReceiver != null){
                locationReceiver.stop();
            }
            prefGps.deleteObserver(gpsObserver);
            toggleShareLocation.deleteObservers();

            showLocationText.setValue(false);
            showLocationText.deleteObservers();

            shareWithActive.setValue(false);
            shareWithActive.deleteObservers();
            shareFromActive.setValue(false);
            shareFromActive.deleteObservers();
            shareLocConfig.deleteObservers();
            shareLocConfig = null;
            me = null;
        } catch (Exception e) {
            mgLog.e(e);
        } finally {
            active = false;
            synchronized (shareLocMap){
                shareLocMap.notifyAll();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (! prefShareLoc.getValue()) return; // if feature is switched off, then do nothing
        updateShareWithActive();
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
            prefShareLoc.addObserver(pce->{
                if (prefShareLoc.getValue()){
                    etv.setData(R.drawable.shareloc);
                    etv.setPrAction(toggleShareLocation);
                    etv.setHelp(r(R.string.FSShareLoc_qcLsd_help));
                } else {
                    etv.setPrAction(new Pref<>(false));
                    etv.setData(R.drawable.empty);
                }
            });
            prefShareLoc.changed();
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
            boolean closeSegment = (best.getEndPointIndex() > 0);
            boolean closePoint = PointModelUtil.distance(point, mpm.get(mpm.size()-1))<getMapViewUtility().getCloseThresholdForZoomLevel();
            if (closeSegment || closePoint){
                showLocationText.toggle();
                return true;
            }
        }
        return false;
    }
}
