package mg.mgmap.generic.graph.impl3;

import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.model.WriteablePointModelImpl;

public class DNode extends WriteablePointModelImpl {

    public DNode(){}

    public DNode(int tileIdx, short nodeIdx, int la, int lo, float ele){
        this.tileIdx = tileIdx;
        this.nodeIdx = nodeIdx;
        setLa(la);
        setLo(lo);
        setEle(ele);
    }

    int tileIdx = 0;
    short nodeIdx = -1;
}
