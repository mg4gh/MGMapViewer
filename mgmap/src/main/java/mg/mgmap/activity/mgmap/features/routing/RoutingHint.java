/*
 * Copyright 2017 - 2021 mg4gh
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package mg.mgmap.activity.mgmap.features.routing;

import mg.mgmap.generic.model.PointModel;

public class RoutingHint {
    
    PointModel pmCurrent; // point, on which is Routing hint is relevant
    PointModel pmNext; // next point of Route
    PointModel pmPrev; // prev point of Route

    double directionDegree; // angle from 0 (go direct back), via 90 (turn left), 180 go straight ahead, 270 (turn right)

    int numberOfPathes; // 2 means no options, 1 means dead end, 4 means normal crossing
    double nextLeftDegree;  // beside the path to continue, which is the direction degree of the closest path, if you turn left from the target path
    double nextRightDegree;  // beside the path to continue, which is the direction degree of the closest path, if you turn right from the target path
}
