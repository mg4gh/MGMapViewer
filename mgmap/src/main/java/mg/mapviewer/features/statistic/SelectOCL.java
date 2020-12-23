package mg.mapviewer.features.statistic;

import android.view.View;
import android.view.ViewGroup;

import mg.mapviewer.view.TrackStatisticEntry;

public class SelectOCL implements View.OnClickListener{
    ViewGroup parent;
    boolean select;
    public SelectOCL(ViewGroup parent, boolean select){
        this.parent = parent;
        this.select = select;
    }

    @Override
    public void onClick(View v) {
        setSelectedAll(select);
    }

    private void setSelectedAll(boolean selected){
        for (int idx=0; idx < parent.getChildCount(); idx++){
            if (parent.getChildAt(idx) instanceof TrackStatisticEntry) {
                TrackStatisticEntry entry = (TrackStatisticEntry) parent.getChildAt(idx);
                entry.setPrefSelected(selected);
            }
        }
    }

}
