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
package mg.mgmap.features.search;

import mg.mgmap.model.PointModel;

public class SearchRequest {

    public String text;
    public int actionId;
    public long timestamp;
    public PointModel pos;
    public int zoom;

    public SearchRequest(String text, int actionId, long timestamp, PointModel pos, int zoom) {
        this.text = text;
        this.actionId = actionId;
        this.timestamp = timestamp;
        this.pos = pos;
        this.zoom = zoom;
    }
}
