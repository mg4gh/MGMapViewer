/*
 * Copyright 2017 - 2020 mg4gh
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package mg.mapviewer.graph;

import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import mg.mapviewer.model.PointModel;

/**
 * Implementation of the Dijkstra algorithm.
 */

public class Dijkstra {


    private static final int CHECK_CONNECTED_ATTEMPTS = 20;

    private List<GNode> nodes;
    GGraph graph;

    private TreeSet<GNodeRef> prioQueue = null;
    GNode target = null;


    private long duration = 0;
    private int cntTotal = 0;
    private int cntRelaxed = 0;
    private int cntSettled = 0;
    private int resultPathLength = 0;
//    private ArrayList<GNode> relaxedList = new ArrayList<>();


    public Dijkstra(GGraph graph) {
        nodes = graph.getNodes();
        this.graph = graph;
    }


    public List<GNodeRef> perform(GNode source, GNode target, double costLimit, ArrayList<PointModel> relaxedList){
        List<GNode> sources = new ArrayList<>();
        sources.add(source);
        return perform(sources,target,costLimit,relaxedList);
    }

    public List<GNodeRef> perform(List<GNode> sources, GNode target, double costLimit, ArrayList<PointModel> relaxedList){
        prioQueue = new TreeSet<>();
        resetNodeRefs();
        if (relaxedList != null) relaxedList.clear();

        this.target = target;
        long tStart = System.currentTimeMillis();
        for (GNode source : sources){
            GNodeRef ref = new GNodeRef(source,0,null,null, heuristic(source));
            source.setNodeRef(ref);
            prioQueue.add(ref);
        }

        GNodeRef ref = prioQueue.first();
        while ((ref != null) && (ref.getNode() != target) && (ref.getCost() <= costLimit)){ // abort  if target reached of if there are no more nodes to settle
            if (ref.getNode().getNodeRef() == ref){ // if there was already a better path to node found, then node.getNodeRef points to this -> then we ca skip this entry of the prioQueue

                GNode node = ref.getNode();
                GNeighbour neighbour = ref.getNode().getNeighbour(); // start relax all neighbours
                while ((neighbour = graph.getNextNeighbour(node, neighbour)) != null){
                    GNode neighbourNode = neighbour.getNeighbourNode();
                    double currentCost = ref.getCost() + neighbour.getCost(); // calc cost on current relaxed path
                    // create new prioQueue entry, if there is currently none or if the current relaxted path has better cost
                    GNodeRef neighbourRef = neighbourNode.getNodeRef();
                    if ((neighbourRef == null) || (currentCost < neighbourRef.getCost() )){
                        neighbourRef = new GNodeRef(neighbourNode,currentCost,ref.getNode(),neighbour, heuristic(neighbourNode));
                        neighbourNode.setNodeRef(neighbourRef);
                        prioQueue.add(neighbourRef);
                    }
                }
                ref.setSettled(true);
            }
            ref = prioQueue.higher(ref);
        }
        duration = System.currentTimeMillis() - tStart;

        // generate statistics
        cntTotal = 0;
        cntRelaxed = 0;
        cntSettled = 0;
        resultPathLength = 0;
        for (GNode node : nodes){
            if (node.getNeighbour() != null){
                cntTotal++;
                if (node.getNodeRef() != null){
                    cntRelaxed++;
                    if (relaxedList != null) relaxedList.add(node);
                    if (node.getNodeRef().isSetteled()){
                        cntSettled++;
                    }
                }
            }
        }

        ArrayList<GNodeRef> resultPath = new ArrayList<>();
        if ((target != null) && (target.getNodeRef() != null)){
            ref = target.getNodeRef();
            while (ref.getPredecessor() != null){
                resultPath.add(0, ref);
                ref = ref.getPredecessor().getNodeRef();
            }
            resultPath.add(0,ref);
        }

        resultPathLength = resultPath.size();
        return resultPath;
    }


    double heuristic(GNode node){
        return 0;
    }

    public boolean markConnected(){
        for (int i=0; i< CHECK_CONNECTED_ATTEMPTS; i++) {
            int sIdx = (int) (Math.random() * (nodes.size() - 0.1));
            GNode source = nodes.get(sIdx); // check random index
            perform(source, null, Double.MAX_VALUE, null);
            if ((double) (cntRelaxed) / cntTotal > 0.6) {
                for (GNode node : nodes) {
                    if (node.getNodeRef() != null) node.setConnected(true);
                }
                return true;
            }
        }
        return false;
    }

    private void resetNodeRefs(){
        for (GNode node : nodes){
            node.setNodeRef(null);
        }
    }

    public String getResult(){
        String res = "\n";
        res += String.format(Locale.GERMAN, "%s  nodes: %d graphNodes: %d, relaxed: %d, settled: %d, duration %dms\n",this.getClass().getSimpleName(),nodes.size(),cntTotal,cntRelaxed, cntSettled, duration);
        if (target == null){
            res += "No search for target path.";
        } else if (target.getNodeRef() == null) {
            res += "no path found";
        } else {
            res += "traget path found - hop count "+resultPathLength;
        }
        return res;
    }

}
