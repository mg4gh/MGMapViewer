/*
 * Copyright 2017 - 2022 mg4gh
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
package mg.mgmap.activity.statistic;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import mg.mgmap.generic.model.TrackLog;

public class TrackStatisticAdapter extends RecyclerView.Adapter<TrackStatisticAdapter.ViewHolder> {

    private final ArrayList<TrackLog> trackLogs;

    /** Provide a reference to the type of views that you are using (custom ViewHolder). */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TrackStatisticView trackStatisticView;

        public ViewHolder(View view) {
            super(view);
            trackStatisticView = (TrackStatisticView) view;
        }

        public TrackStatisticView getTrackStatisticView() {
            return trackStatisticView;
        }
    }

    public TrackStatisticAdapter(ArrayList<TrackLog> trackLogs) {
        this.trackLogs = trackLogs;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view, which defines the UI of the list item
        View view = new TrackStatisticView(viewGroup.getContext());
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, final int position) {
        // Get element from trackLogs at this position and replace the
        // contents of the view with that element
        viewHolder.getTrackStatisticView().bind(trackLogs.get(position));
    }

    @Override
    public int getItemCount() {
        return trackLogs.size();
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        holder.getTrackStatisticView().unbind();
        super.onViewRecycled(holder);
    }

}
