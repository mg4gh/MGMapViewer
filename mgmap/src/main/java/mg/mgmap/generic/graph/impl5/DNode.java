package mg.mgmap.generic.graph.impl5;

import androidx.annotation.NonNull;

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

    void init(DNode node){
        this.tileIdx = node.tileIdx;
        this.nodeIdx = node.nodeIdx;
        setLa(node.getLa());
        setLo(node.getLo());
        setEle(node.getEle());
        setEleAcc(node.getEleAcc());
    }

    @NonNull
    @Override
    public String toString() {
        return "tileIdx="+tileIdx+" nodeIdx="+nodeIdx+" "+ super.toString();
    }
}
