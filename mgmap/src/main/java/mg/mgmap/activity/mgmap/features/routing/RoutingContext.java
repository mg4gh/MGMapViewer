package mg.mgmap.activity.mgmap.features.routing;

public class RoutingContext {

    public final int maxBeelineDistance;
    final boolean snap2Way;
    public final int maxRouteLengthFactor;
    final int approachLimit;

    public RoutingContext(int maxBeelineDistance, boolean snap2Way, int maxRouteLengthFactor, int approachLimit) {
        this.maxBeelineDistance = maxBeelineDistance;
        this.snap2Way = snap2Way;
        this.maxRouteLengthFactor = maxRouteLengthFactor;
        this.approachLimit = approachLimit;
    }
}
