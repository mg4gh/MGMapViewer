package mg.mapviewer.util;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import java.util.Observable;
import java.util.Observer;

import mg.mapviewer.R;
import mg.mapviewer.util.MGPref;

public class HomeObserver implements Observer {

    private final Activity activity;

    public HomeObserver(Activity activity){
        this.activity = activity;
    }

    @Override
    public void update(Observable o, Object arg) {
        launchHomeScreen(activity);
    }

    public static void launchHomeScreen(Activity activity) {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN, null);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        activity.startActivity(homeIntent);
    }

}
