package mg.mgmap.generic.graph;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Locale;

import mg.mgmap.generic.model.MultiPointModel;

public class RoutingSummary {

    public static ArrayList<RoutingSummary> routingSummaries = new ArrayList<>();


    ApproachModel amSource;
    ApproachModel amTarget;
    MultiPointModel mpm;
    double cost;
    long duration;
    int rc;

    public RoutingSummary(ApproachModel amSource, ApproachModel amTarget, MultiPointModel mpm, double cost, long duration, int rc) {
        this.amSource = amSource;
        this.amTarget = amTarget;
        this.mpm = mpm;
        this.cost = cost;
        this.duration = duration;
        this.rc = rc;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(Locale.ENGLISH,"RoutingSummary{source=%s, target=%s, cnt=%d, cost=%.3f, duration=%d rc=%d", amSource.getApproachNode(),amTarget.getApproachNode(),mpm.size(), cost, duration, rc);
    }
}
