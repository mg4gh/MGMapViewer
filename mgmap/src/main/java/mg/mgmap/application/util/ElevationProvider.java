package mg.mgmap.application.util;

import mg.mgmap.generic.model.TrackLogPoint;
import mg.mgmap.generic.model.WriteablePointModel;

public interface ElevationProvider {

    void setElevation(TrackLogPoint tlp);

    void setElevation(WriteablePointModel wpm);
}
