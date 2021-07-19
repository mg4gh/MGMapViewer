package mg.mgmap.application.util;

import org.mapsforge.map.datastore.Way;

import java.util.Objects;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.graph.GNeighbour;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelUtil;

import static java.lang.Math.abs;

public class CostProvider {
    private MGMapApplication application = null;
    private Way way = null;
    private Path path = null;
    private Path root = null;

    private static final double avgDist = 100.0;
    private static final double baseCostHm = 5; // each meter uphill equivalence base
    private static final double highCostHm = 35; // each meter uphill equivalence high value
    private static final double highCostEle = 0.2; // min elevation for uphill equivalence high value
    private static final double offsetHigh = 1 + (baseCostHm - highCostHm) * highCostEle;

    private class Path{
        public GNeighbour neighbour12 = null;
        public GNeighbour neighbour21 = null;
        public double d;
        public float e;
 //       public double slope;
        public Path next;
        public double costFactor = 0;
        public void setCost(double costFactor){
//           neighbour12.setDebug(d,e, way);
//           neighbour21.setDebug(d,-e, way);

            if ( costFactor > 0) {//ascent
              neighbour12.setCost(d * costFactor);
              neighbour21.setCost(d);
            }
            else {// descent
              neighbour12.setCost(d);
              neighbour21.setCost(d * costFactor * -1);
            }
            this.costFactor = costFactor;
        }
    }

    public CostProvider(MGMapApplication application ) {this.application = application; }
    public void setWay(Way way) {
        this.way = way;
        this.root = new Path();
        this.path = root;
        if ( this.way.tags.size()>2 && Objects.equals(this.way.tags.get(2).value, new String("Schurmanstr."))) {
            this.path = root;
        }

    }
    public void setNodes(PointModel node1,GNeighbour neighbour12, PointModel node2,GNeighbour neighbour21) {
        double d  = PointModelUtil.distance(node1, node2);
        float  e  = PointModelUtil.eleDiff(node1, node2);
        if (root == null) {
            path = new Path();
            path.neighbour12 = neighbour12;
            path.neighbour21 = neighbour21;
            path.setCost(calcCostFactor(d,e));
        }
        else {
            path.d = d; //PointModelUtil.distance(node1, node2);
            path.e = e;
//        path.slope = path.e/path.d;
            path.neighbour12 = neighbour12;
            path.neighbour21 = neighbour21;
            path.next = new Path();
            path = path.next;
        }
    }

    public void calcCosts() {
       if ( root.next == null )
           return;

       Path minpath = root;
       Path midpath = root;
       Path maxpath;
       Path iterpath;

       double d1 = 0;
       double d2 = 0;
       float  e1 = 0;
       float  e2 = 0;
// Initialize and average the first section with section average
           while (midpath.next != null && d1 < avgDist) {
               d1 = d1 + midpath.d;
               e1 = e1 + midpath.e;
               midpath = midpath.next;
           }
           maxpath = midpath;
           while (maxpath.next != null && d2 < avgDist) {
               d2 = d2 + maxpath.d;
               e2 = e2 + maxpath.e;
               maxpath = maxpath.next;
           }

           double costFactor = calcCostFactor(d1 + d2, e1 + e2);
           iterpath = root;
           while (iterpath != midpath) {
               iterpath.setCost(costFactor);
               iterpath = iterpath.next;
           }
//     iterpath.setCost( costFactor );// include midpath!
//       iterpath.setCost( 1 );

// iterate with gliding average
           while (maxpath.next != null) {
               d1 = d1 + midpath.d;
               e1 = e1 + midpath.e;
               d2 = d2 - midpath.d;
               e2 = e2 - midpath.e;
               while (d1 - minpath.d >= avgDist) {
                   d1 = d1 - minpath.d;
                   e1 = e1 - minpath.e;
                   minpath = minpath.next;
               }
               while (d2 < avgDist && maxpath.next != null) {
                   d2 = d2 + maxpath.d;
                   e2 = e2 + maxpath.e;
                   maxpath = maxpath.next;
               }
               costFactor = calcCostFactor(d1 + d2, e1 + e2);
               midpath.setCost(costFactor);
               midpath = midpath.next;
           }
// process last section
           iterpath = midpath;
           while (iterpath.next != null) {
               iterpath.setCost(costFactor);
               iterpath = iterpath.next;
           }

        iterpath = root;
        while ( iterpath.next != null){
            if ( abs(iterpath.costFactor) < 1) {
                iterpath.setCost(1);
            }
            iterpath = iterpath.next;
        }

        root = null;
        path = null;
    }
    private double calcCostFactor( double dist, float elediff){
        double slope = elediff / dist;
        if ( abs(slope) > highCostEle ) {
            double highCostHm = this.highCostHm;
            double offsetHigh = this.offsetHigh;
            if ( dist < avgDist){
                highCostHm = baseCostHm + ( highCostHm - baseCostHm ) * dist / avgDist; // damping high costs for short distances to reduce artifacts ...
                offsetHigh = 1 + (baseCostHm - highCostHm) * highCostEle;
            }
            if ( slope >= 0 ) return (  offsetHigh + highCostHm * slope) ;
            else              return ( -offsetHigh + highCostHm * slope) ; // costFactor negative to indicate descent!
        }
        else
            if ( slope >= 0 ) return   1 + this.baseCostHm * slope;
            else              return  -1 + this.baseCostHm * slope;

    }

/*
        slope =  elediff  / dist ;
        elediff = abs( elediff );


/* cost model:
    continuous function of distance and elevation based on standard model to calculate touring costs

//         if      ( abs(slope) > 0.25 ) cost = -11.5 * dist + 50 * elediff;
//        else if ( abs(slope) > 0.10 ) cost =  0.25 * dist + 10 * elediff;
//        else if ( abs(slope) > 0.05 ) cost =  0.75 * dist +  5 * elediff;
//         else
//           cost =         dist +  0 * elediff;

    }
    public double getCost12() {
        if ( slope > 0 ) return cost ;
    //    else if ( slope < -0.05 ) return dist / 2;
        else return dist;
    }

    public double getCost21() {
        if ( slope < 0 ) return cost ;
    //    else if ( slope > 0.05 ) return dist / 2;
        else return dist;
    }

    public float getElediff(){
        return elediff;
    }
    public double getDist(){
        return dist;
    }
*/
}
