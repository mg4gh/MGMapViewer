package mg.mgmap.test.tc.features.control;

import android.content.Intent;
import android.graphics.PointF;
import android.net.Uri;
import android.util.Log;

import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.test.Testcase;
import mg.mgmap.test.TestControl;
import mg.mgmap.generic.util.basic.NameUtil;

public class TestTriggerDownload extends Testcase {

    public TestTriggerDownload(TestControl tc) {
        super(tc);
    }

    @Override
    protected void setup() {
        timeout = 30000;
        tc.timer.postDelayed(() -> Log.i(MGMapApplication.LABEL, NameUtil.context()+" ("+name+")"),1000);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(50,50),1), 1000);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(7,97),2000), 1100);
        tc.timer.postDelayed(() -> tc.doClick(this), 3500);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(80,97),1000), 4500);
        tc.timer.postDelayed(() -> tc.doClick(this), 6000);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(50,35),1000), 8000);
        tc.timer.postDelayed(() -> tc.doClick(this), 11000);
        tc.timer.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(tc.application.getApplicationContext(), MGMapActivity.class);
                intent.setAction("android.intent.action.VIEW");

                try {
                    intent.setData(Uri.parse("mf-v4-map://download.openandromaps.org/mapsV4/Germany/Ruegen.zip"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                tc.application.startActivity(intent);
            }
        }, 15000);
        tc.timer.postDelayed(rFinal, 29000);
    }

    @Override
    protected void addRegexs() {
        addRegex("Testcase.start.*  TestTriggerDownload");
        addRegex("key=FSControl.qc_selector value=1");
        addRegex(".MGPreferenceScreen.* https://www.openandromaps.org/downloads/deutschland");
        addRegex("Zipper.unpack.*/maps/mapsforge/Ruegen_oam.osm.poi");
    }
}
