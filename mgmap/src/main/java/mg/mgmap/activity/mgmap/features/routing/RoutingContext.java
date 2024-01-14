package mg.mgmap.activity.mgmap.features.routing;

public class RoutingContext {

    public int maxBeelineDistance;
    boolean snap2Way;
    public int maxRouteLengthFactor;
    int approachLimit;

    public RoutingContext(int maxBeelineDistance, boolean snap2Way, int maxRouteLengthFactor, int approachLimit) {
        this.maxBeelineDistance = maxBeelineDistance;
        this.snap2Way = snap2Way;
        this.maxRouteLengthFactor = maxRouteLengthFactor;
        this.approachLimit = approachLimit;
    }
}
