package mg.mgmap.activity.mgmap.features.shareloc;

import mg.mgmap.activity.mgmap.view.MultiPointView;
import mg.mgmap.generic.model.MultiPointModel;
import mg.mgmap.generic.util.CC;
import mg.mgmap.generic.view.VUtil;

public class MultiPointViewSL extends MultiPointView {

    public MultiPointViewSL(MultiPointModel model, int color) {
        super(model, CC.getStrokePaint4Color(color, VUtil.dp(4) ));
        setPointRadius(VUtil.dp(0));
    }
}
