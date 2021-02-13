package mg.mgmap.test.tc.features.control;

import android.graphics.PointF;
import android.util.Log;

import mg.mgmap.R;
import mg.mgmap.test.TestControl;
import mg.mgmap.test.Testcase;

public class TestSettingsSelectSearchProvider extends Testcase {

    public TestSettingsSelectSearchProvider(TestControl tc) {
        super(tc, Log.DEBUG);
    }

    @Override
    protected void setup() {
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(7,97),1000), 1100);
        tc.timer.postDelayed(() -> tc.doClick(this), 3000);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(21,97),200), 4000);
        tc.timer.postDelayed(() -> tc.doClick(this), 5000);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(20,28),600), 6500);
        tc.timer.postDelayed(() -> tc.doClick(this), 7600);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(20,58),1000), 9000);
        tc.timer.postDelayed(() -> tc.doClick(this), 10500);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(25,97),1000), 11500);
        tc.timer.postDelayed(() -> tc.doClick(this), 13000);
        tc.timer.postDelayed(rFinal, 14000);
        tc.timer.postDelayed(() -> tc.getPref(R.string.preference_choose_search_key, "").setValue("Graphhopper"), 13500);
    }

    @Override
    protected void addRegexs() {
        addRegex("Testcase.start.*  TestSettingsSelectSearchProvider");
        addRegex("key=FSControl.qc_selector value=1");
        addRegex("SettingsActivity.onCreate.* open PreferenceFragment mg.mgmap.settings.MainPreferenceScreen");
        addRegex("SearchProviderListPreference.onClick.*  key=SelectSearchProvider value=Graphhopper");
        addRegex("SearchProviderListPreference.*  key=SelectSearchProvider value=Pelias");
        addRegex("mg.mgmap.test.TestControl.onResume.* set TestView MGMapActivity");
    }
}
