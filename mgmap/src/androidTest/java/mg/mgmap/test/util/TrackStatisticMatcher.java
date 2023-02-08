package mg.mgmap.test.util;

import android.view.View;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import mg.mgmap.activity.statistic.TrackStatisticView;

public class TrackStatisticMatcher extends TypeSafeDiagnosingMatcher<View>{


    private final String nameMatch;

    private TrackStatisticMatcher(String nameMatch) {
        this.nameMatch = nameMatch;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("matches any staistic entry with name is matching regular expression=`" + nameMatch + "`");
    }

    @Override
    protected boolean matchesSafely(View view, Description mismatchDescription) {
        if (view instanceof TrackStatisticView) {
            TrackStatisticView trackStatisticView = (TrackStatisticView) view;
            return trackStatisticView.trackLog.getName().matches(nameMatch);
        }
        return false;
    }

    public static TrackStatisticMatcher matchTrack(String nameMatch){
        return new TrackStatisticMatcher(nameMatch);
    }
}
