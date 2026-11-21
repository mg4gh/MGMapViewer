package mg.mgmap.generic.graph.impl4;

import androidx.annotation.NonNull;

import mg.mgmap.generic.model.PointModel;

public class ExDNode extends DNode {

    short dla;
    short dlo;
    short neighbourCnt = 0;
    byte border;
    byte flags;
    ExDNeighbour neighbour;


    public ExDNode(short nodeIdx){
        this.nodeIdx = nodeIdx;
    }

    void init(int la, int lo, int minLa, int minLo){
        this.setLa(la);
        this.setLo(lo);
        this.dla = (short)(la - minLa);
        this.dlo = (short)(lo - minLo);
        neighbour = null;
        neighbourCnt = 0;
        border = 0;
        flags = DGraphTile.ND_FLAG_VALID;
    }

    public void setNeighbour(ExDNeighbour neighbour){
        this.neighbour = neighbour;
    }

    public ExDNeighbour getNeighbour(){
        return neighbour;
    }

    @Override
    public int compareTo(@NonNull PointModel o) {
        if (o instanceof ExDNode oNode){
            if (this.dla < oNode.dla) return -1;
            if (this.dla > oNode.dla) return 1;
            return Short.compare(this.dlo, oNode.dlo);
        }
        return super.compareTo(o);
    }

    public void setFlags(byte... flags){
        for (byte flag:flags){
            this.flags |= flag;
        }
    }
    public void unsetFlags(byte... flags){
        for (byte flag:flags){
            this.flags &= (byte) ~flag;
        }
    }
    public boolean hasFlags(byte... flags){
        for (byte flag:flags){
            if ((this.flags & flag) == 0) return false;
        }
        return true;
    }
    @SuppressWarnings("unused")
    public boolean hasNotFlags(byte... flags){
        for (byte flag:flags){
            if ((this.flags & flag) != 0) return false;
        }
        return true;
    }


}
