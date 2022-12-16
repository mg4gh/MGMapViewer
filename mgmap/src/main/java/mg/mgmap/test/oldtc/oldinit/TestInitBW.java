package mg.mgmap.test.oldtc.oldinit;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PointF;
import android.net.Uri;
import android.util.Log;

import mg.mgmap.service.bgjob.BgJobService;
import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.test.OldTestControl;
import mg.mgmap.test.OldTestcase;
import mg.mgmap.generic.util.basic.NameUtil;

public class TestInitBW extends OldTestcase {

    public TestInitBW(OldTestControl tc) {
        super(tc);
    }

    final Runnable rAfterDownload = new Runnable() {
        @Override
        public void run() {
            Log.i(MGMapApplication.LABEL, NameUtil.context()+" active="+BgJobService.isActive());
            if (BgJobService.isActive()) {
                tc.timer.postDelayed(rAfterDownload, 1000);
            } else {
                if (getTestView().getContext() instanceof Activity) {
                    tc.timer.postDelayed(rFinal, 3000);
                    Activity activity = (Activity) getTestView().getContext();
                    activity.recreate();
                }
            }
        }
    };

    @Override
    protected void setup() {
        timeout = 5*60*1000;
        tc.timer.postDelayed(() -> Log.i(MGMapApplication.LABEL, NameUtil.context()+" ("+name+")"),1000);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(7,97),600), 1100);
        tc.timer.postDelayed(() -> tc.doClick(this), 2000);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(80,97),600), 3000);
        tc.timer.postDelayed(() -> tc.doClick(this), 4000);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(30,55),600), 5000);
        tc.timer.postDelayed(() -> tc.doClick(this), 6000);
        tc.timer.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(tc.application.getApplicationContext(), MGMapActivity.class);
                intent.setAction("android.intent.action.VIEW");

                try {
                    intent.setData(Uri.parse("mf-theme://download.openandromaps.org/themes/Elevate.zip"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                tc.application.startActivity(intent);
            }
        }, 8000);

        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(7,97),600), 11000);
        tc.timer.postDelayed(() -> tc.doClick(this), 12000);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(80,97),600), 13000);
        tc.timer.postDelayed(() -> tc.doClick(this), 14000);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(30,35),600), 15000);
        tc.timer.postDelayed(() -> tc.doClick(this), 16000);

        tc.timer.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(tc.application.getApplicationContext(), MGMapActivity.class);
                intent.setAction("android.intent.action.VIEW");

                try {
                    intent.setData(Uri.parse("mf-v4-map://download.openandromaps.org/mapsV4/Germany/baden-wuerttemberg.zip"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                tc.application.startActivity(intent);
            }
        }, 18000);
         tc.timer.postDelayed(rAfterDownload, 25000);


    }

    @Override
    protected void addRegexs() {
        addRegex("Testcase.start.*  TestInitBW");
        addRegex("key=FSControl.qc_selector value=1");
        addRegex("Zipper.unpack.* extract url=https://www.openandromaps.org/wp-content/users/tobias/Elevate.zip");
        addRegex("Zipper.unpack.*/MGMapViewer/themes/Elevate.xml");
        addRegex("Zipper.unpack.*/MGMapViewer/themes/ele_res/s_bakery.svg");
        addRegex("MGPreferenceScreen.* https://www.openandromaps.org/downloads/deutschland");
        addRegex("Zipper.unpack.*/MGMapViewer/maps/mapsforge/baden-wuerttemberg_oam.osm.map");
        addRegex("onActivityResumed.*  MGMapActivity");
    }
}
