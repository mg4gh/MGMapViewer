package mg.mgmap.generic.graph.impl2;

import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.model.PointNeighbour;

public class GNodeUtil {

    public static GNode validateGNode(GGraphTile gGraphTile, GNode gNode){
        return gGraphTile.getNode(gNode.getLat(), gNode.getLon());
    }

    public static GNeighbour validateGNodeNeighbour(GGraphTile gGraphTile, GNode gNode, GNeighbour gNeighbour){
        GNeighbour neighbour = null;
        while ((neighbour = gGraphTile.getNextNeighbour(gNode, neighbour)) != null){
            if (neighbour.cntIntermediates() == gNeighbour.cntIntermediates()){
                if (neighbour.cntIntermediates() == 0){
                    if (PointModelUtil.compareTo(neighbour.getNeighbourNode(), gNeighbour.getNeighbourNode()) == 0){
                        return neighbour;
                    }
                } else {
                    if ((neighbour.getIntermediatesPoints()[0] == gNeighbour.getIntermediatesPoints()[0]) &&
                            (neighbour.getIntermediatesPoints()[1] == gNeighbour.getIntermediatesPoints()[1])){
                        return neighbour;
                    }
                }
            }
        }
        return null;
    }

    public static GIntermediateNode validateGIntermediateNode(GGraphTile gGraphTile, GIntermediateNode giNode){
        GNode node = validateGNode(gGraphTile, giNode.node);
        if (node != null){
            GNeighbour neighbour = validateGNodeNeighbour(gGraphTile, node, giNode.neighbour);
            if (neighbour != null){
                return new GIntermediateNode(node, neighbour, giNode.pIdx);
            }
        }
        return null;
    }

    public static PointModel validateNode(GGraphTile gGraphTile, PointModel pm){
        if (pm instanceof GNode node){
            return validateGNode(gGraphTile, node);
        } else if (pm instanceof  GIntermediateNode giNode) {
            return validateGIntermediateNode(gGraphTile, giNode);
        }
        return null;
    }


    public static GNeighbour validateGNeighbour(GGraphTile gGraphTile, GNeighbour gNeighbour){
        GNode node = validateGNode(gGraphTile, gNeighbour.getReverse().getNeighbourNode());
        if (node != null){
            return validateGNodeNeighbour(gGraphTile, node, gNeighbour);
        }
        return null;
    }

    public static PointNeighbour validateNeighbour(GGraphTile gGraphTile, PointNeighbour neighbour){
        if (neighbour instanceof GNeighbour gNeighbour){
            return validateGNeighbour(gGraphTile, gNeighbour);
        }
        return null;
    }
}
