package mg.mgmap.generic.graph.impl5;

public class ExDNeighbour {

    short neighbourIdx; // own index

    ExDNode neighbourNode;
    ExDNeighbour nextNeighbour;
    ExDNeighbour reverseNeighbour;

    short wayAttributesIdx;
    short wayIdx;
    float distance;
    float vertDistance;
    byte border;
    byte flags;

    public ExDNeighbour(short neighbourIdx){
        this.neighbourIdx = neighbourIdx;
    }

    void init(ExDNode neighbourNode, ExDNeighbour nextNeighbour, ExDNeighbour reverseNeighbour, short wayAttributesIdx, short wayIdx, float distance, float vertDistance, byte flags){
        this.neighbourNode = neighbourNode;
        this.nextNeighbour = nextNeighbour;
        this.reverseNeighbour = reverseNeighbour;
        this.wayAttributesIdx = wayAttributesIdx;
        this.wayIdx = wayIdx;
        this.distance = distance;
        this.vertDistance = vertDistance;
        this.border = 0;
        this.flags = flags;
    }

    short getNextNeighbourIdx(){
        if (nextNeighbour == null) return -1;
        return nextNeighbour.neighbourIdx;
    }


}
