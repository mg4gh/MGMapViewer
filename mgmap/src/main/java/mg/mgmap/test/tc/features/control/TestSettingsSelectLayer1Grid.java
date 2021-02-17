package mg.mgmap.test.tc.features.control;

import android.graphics.PointF;
import android.util.Log;

import mg.mgmap.R;
import mg.mgmap.test.TestControl;
import mg.mgmap.test.Testcase;

public class TestSettingsSelectLayer1Grid extends Testcase {

    public TestSettingsSelectLayer1Grid(TestControl tc) {
        super(tc, Log.DEBUG);
    }

    @Override
    protected void setup() {
        timeout = 23000;
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(7,97),1000), 1100);
        tc.timer.postDelayed(() -> tc.doClick(this), 3000);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(21,97),200), 4000);
        tc.timer.postDelayed(() -> tc.doClick(this), 5000);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(30,10),1000), 6500);
        tc.timer.postDelayed(() -> tc.doClick(this), 8000);
        tc.timer.postDelayed(() -> tc.doClick(this), 10000);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(20,66),1000), 11500);
        tc.timer.postDelayed(() -> tc.doClick(this), 13000);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(25,97),1000), 14500);
        tc.timer.postDelayed(() -> tc.doClick(this), 16000);
        tc.timer.postDelayed(() -> tc.doClick(this), 17000);
        tc.timer.postDelayed(rFinal, 22000);
        tc.timer.postDelayed(() -> tc.getPref(R.string.Layers_pref_chooseMap1_key, "").setValue("none"), 18500);
    }

    @Override
    protected void addRegexs() {
        addRegex("Testcase.start.*  TestSettingsSelectLayer1Grid");
        addRegex("key=FSControl.qc_selector value=1");
        addRegex("SettingsActivity.onCreate.* open PreferenceFragment mg.mgmap.activity.settings.MainPreferenceScreen");
        addRegex("SettingsActivity.* set fragment mg.mgmap.activity.settings.MapLayersPreferenceScreen");
        addRegex("MapLayerListPreference.onClick.* key=SelectMap1 value=none");
        addRegex("MapViewerBase.*  recreate MGMapActivity due to key=SelectMap1 value=MAPGRID: grid.properties");
        addRegex("mg.mgmap.test.TestControl.onResume.* set TestView MGMapActivity");
    }
}
