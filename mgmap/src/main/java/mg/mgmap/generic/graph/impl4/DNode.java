package mg.mgmap.generic.graph.impl4;

import mg.mgmap.generic.model.WriteablePointModelImpl;

public class DNode extends WriteablePointModelImpl {

    public DNode(){}

    public DNode(int tileIdx, short nodeIdx, int la, int lo, float ele, float eleAcc){
        this.tileIdx = tileIdx;
        this.nodeIdx = nodeIdx;
        setLa(la);
        setLo(lo);
        setEle(ele);
        setEleAcc(eleAcc);
    }

    int tileIdx = 0;
    short nodeIdx = -1;
}
