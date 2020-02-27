/*
 * Copyright 2017 - 2020 mg4gh
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
package mg.mapviewer.model;

/** A specific kind of {@link mg.mapviewer.model.TrackLogRef} to reference a TrackLog.
 * Additionally it stores the information, whether a zoom shall take place to see the full track. */
public class TrackLogRefZoom extends TrackLogRef{

    boolean zoomForBB = true;

    public TrackLogRefZoom(TrackLog trackLog, int segmentIdx, boolean zoomForBB) {
        super(trackLog, segmentIdx);
        this.zoomForBB = zoomForBB;
    }

    public boolean isZoomForBB() {
        return zoomForBB;
    }

    public void setZoomForBB(boolean zoomForBB) {
        this.zoomForBB = zoomForBB;
    }
}
