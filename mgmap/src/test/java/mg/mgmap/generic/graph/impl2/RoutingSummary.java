package mg.mgmap.generic.graph.impl2;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Locale;

import mg.mgmap.generic.graph.ApproachModel;
import mg.mgmap.generic.model.MultiPointModel;

public class RoutingSummary {

    public static ArrayList<RoutingSummary> routingSummaries = new ArrayList<>();


    ApproachModel amSource;
    ApproachModel amTarget;
    MultiPointModel mpm;
    double cost;
    int rc;

    public RoutingSummary(ApproachModel amSource, ApproachModel amTarget, MultiPointModel mpm, double cost, int rc) {
        this.amSource = amSource;
        this.amTarget = amTarget;
        this.mpm = mpm;
        this.cost = cost;
        this.rc = rc;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(Locale.ENGLISH,"RoutingSummary{source=%s, target=%s, cnt=%d, cost=%.3f, rc=%d", amSource.getApproachNode(),amTarget.getApproachNode(),mpm.size(), cost, rc);
    }
}
