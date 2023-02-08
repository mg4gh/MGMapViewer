package mg.mgmap.test.util;

import android.view.View;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import java.lang.invoke.MethodHandles;

import mg.mgmap.activity.statistic.TrackStatisticView;
import mg.mgmap.generic.util.basic.MGLog;

public class ViewCallbackAction implements ViewAction {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    public interface ActionCallback {
        void callback(View view);
    }
    ActionCallback onclickActionCallback;
    public ViewCallbackAction(ActionCallback onclickActionCallback){
        this.onclickActionCallback = onclickActionCallback;
    }

    @Override
    public Matcher<View> getConstraints(){
        return new BaseMatcher<>() {
            @Override
            public boolean matches(Object actual) {
                return true;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("ANY matcher");
            }
        };
    }


    @Override
    public String getDescription(){
        return "callback action";
    }

    @Override
    public void perform(UiController uiController, View view){
        if (view instanceof TrackStatisticView) {
            TrackStatisticView trackStatisticView = (TrackStatisticView) view;
            mgLog.i("callback on "+trackStatisticView.trackLog.getName());
        }
        onclickActionCallback.callback(view);
    }

}