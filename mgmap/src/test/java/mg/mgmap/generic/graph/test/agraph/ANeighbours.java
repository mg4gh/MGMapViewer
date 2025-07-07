package mg.mgmap.generic.graph.test.agraph;

import java.lang.invoke.MethodHandles;

import mg.mgmap.generic.util.basic.MGLog;

public class ANeighbours {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    final static byte PRIMARY_NA = 0; // not applicable
    final static byte PRIMARY_YES = 1;
    final static byte PRIMARY_NO = -1;


    short neighboursUsed = 1;
    short[] neighbourNodes = null;
    byte[] neighbourTileSelectors = null;
    byte[] primaryFlags = null;
    short[] nextNeighbours = null;
    short[] wayAttributes = null;
    float[] distances = null;
    float[] costs = null;


    public void init(short[] neighbourNodes, byte[] neighbourTileSelectors, byte[] primaryFlags, short[] nextNeighbours, short[] wayAttributes, float[] distances, float[] costs) {
        this.neighbourNodes = neighbourNodes;
        this.neighbourTileSelectors = neighbourTileSelectors;
        this.primaryFlags = primaryFlags;
        this.nextNeighbours = nextNeighbours;
        this.wayAttributes = wayAttributes;
        this.distances = distances;
        this.costs = costs;
    }

    public short createNeighbour(short wayAttributesIdx, short neighbourNode, float distance, byte neighbourTileSelector, byte primaryFlag){
        short neighbour = neighboursUsed++;
        neighbourNodes[neighbour] = neighbourNode;
        neighbourTileSelectors[neighbour] = neighbourTileSelector;
        primaryFlags[neighbour] = primaryFlag;
        wayAttributes[neighbour] = wayAttributesIdx;
        distances[neighbour] = distance;
        costs[neighbour] = -1;
        return neighbour;
    }


    public short getNeighbourNode(short neighbour){
        return neighbourNodes[neighbour];
    }

    public byte getTileSelector(short neighbour){
        return neighbourTileSelectors[neighbour];
    }

    public boolean isPrimaryFlag(short neighbour){
        return (primaryFlags[neighbour] == PRIMARY_YES);
    }
    public short getReverse(short neighbour){
        return (short)(neighbour + primaryFlags[neighbour]);
    }


    public short getNextNeighbour(short neighbour){
        assert((neighbour > 0) && (neighbour < nextNeighbours.length)):"unexpected neighbour="+neighbour;
        return nextNeighbours[neighbour];
    }
    public void setNextNeighbour(short neighbour, short nextNeighbour){
        nextNeighbours[neighbour] = nextNeighbour;
    }

    public short getWayAttributes(short neighbour){
        return wayAttributes[neighbour];
    }
    public float getDistance(short neighbour){
        return distances[neighbour];
    }
    public float getCost(short neighbour){
        return costs[neighbour];
    }
    public void setCost(short neighbour, float cost){
        costs[neighbour] = cost;
    }


}
