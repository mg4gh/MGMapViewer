package mg.mgmap.test.oldtc.features.oldcontrol;

import android.graphics.PointF;
import android.util.Log;

import mg.mgmap.R;
import mg.mgmap.test.OldTestControl;
import mg.mgmap.test.OldTestcase;

public class TestSettingsSelectTheme extends OldTestcase {

    public TestSettingsSelectTheme(OldTestControl tc) {
        super(tc, Log.DEBUG);
    }

    @Override
    protected void setup() {
        timeout = 20000;
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(7,97),1000), 1100);
        tc.timer.postDelayed(() -> tc.doClick(this), 3000);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(21,97),200), 4000);
        tc.timer.postDelayed(() -> tc.doClick(this), 5000);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(30,20),600), 6500);
        tc.timer.postDelayed(() -> tc.doClick(this), 7600);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(20,45),1000), 9000);
        tc.timer.postDelayed(() -> tc.doClick(this), 10500);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(25,97),1000), 11500);
        tc.timer.postDelayed(() -> tc.doClick(this), 13000);
        tc.timer.postDelayed(rFinal, 19000);
        tc.timer.postDelayed(() -> tc.getPref(R.string.preference_choose_theme_key, "").setValue("Elevate.xml"), 14000);
    }

    @Override
    protected void addRegexs() {
        addRegex("Testcase.start.*  TestSettingsSelectTheme");
        addRegex("key=FSControl.qc_selector value=1");
        addRegex("SettingsActivity.onCreate.* open PreferenceFragment mg.mgmap.activity.settings.MainPreferenceScreen");
        addRegex("ThemeListPreference.onClick.* key=SelectTheme value=Elevate.xml");
        addRegex("MapViewerBase.*  recreate MGMapActivity due to key=SelectTheme value=Elements.xml");
        addRegex("mg.mgmap.test.TestControl.onResume.* set TestView MGMapActivity");
    }
}
