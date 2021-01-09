package mg.mgmap.features.routing;

import mg.mgmap.model.PointModel;

public class RoutingHint {

    public static final double MIN_DISTANCE = 3.0;

    PointModel pmCurrent; // point, on which is Routing hint is relevant
    PointModel pmNext; // next point of Route
    PointModel pmPrev; // prev point of Route

    double directionDegree; // angle from 0 (go direct back), via 90 (turn left), 180 go straight ahead, 270 (turn right)

    int numberOfPathes; // 2 means no options, 1 means dead end, 4 means normal crossing
    double nextLeftDegree;  // beside the path to continue, which is the direction degree of the closest path, if you turn left from the target path
    double nextRightDegree;  // beside the path to continue, which is the direction degree of the closest path, if you turn right from the target path
}
