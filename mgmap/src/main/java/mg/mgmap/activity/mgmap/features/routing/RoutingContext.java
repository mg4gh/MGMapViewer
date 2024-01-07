package mg.mgmap.activity.mgmap.features.routing;

public class RoutingContext {

    public int maxRoutingDistance;
    boolean snap2Way;
    public int maxRouteLengthFactor;
    int approachLimit;

    public RoutingContext(int maxRoutingDistance, boolean snap2Way, int maxRouteLengthFactor, int approachLimit) {
        this.maxRoutingDistance = maxRoutingDistance;
        this.snap2Way = snap2Way;
        this.maxRouteLengthFactor = maxRouteLengthFactor;
        this.approachLimit = approachLimit;
    }
}
