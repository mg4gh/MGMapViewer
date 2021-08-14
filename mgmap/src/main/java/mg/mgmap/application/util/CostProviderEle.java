package mg.mgmap.application.util;

import android.util.Log;

import org.mapsforge.map.datastore.Way;

import mg.mgmap.generic.graph.GNeighbour;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelUtil;

import static java.lang.Math.abs;
import static java.lang.Math.max;

public abstract class CostProviderEle extends CostProvider {

    protected boolean log = false;

    protected double cost12;
    protected double cost21;

    private Path path = null;
    private Path root = null;

    private static final double avgDist = 200.0;
    private static final double highCostUpHm = 35; // each meter uphill equivalence high value
    private static final double highCostDownHm = 35; // each meter downhill equivalence high value

    private double baseCostHm = 6; // each meter uphill equivalence base
    private double optUpSlope = 0.2; // min slope for uphill equivalence high value
    private double optDownSlope = 0.35; // min slope for downhill equivalence high value

//    private double fixUpDistFactor   = 1; // fix cost factor, e.g. used for stairs
//    private double fixDownDistFactor = 1; // fix cost factor, e.g. used for stairs
    private double genUpCostFactor = 1;  // general uplift of costs
    private double genDownCostFactor = 1;

    private double costDistLargerOptUpEle   ;
    private double costDistLargerOptDownEle ;

    protected void setUpSlopeParameters( double optUpSlope ){
       this.optUpSlope = optUpSlope;
//       offsetHighUp   = 1 + (baseCostHm - highCostUpHm)  * optUpSlope;
    }

    protected void setDownSlopeParameters( double optDownSlope ){
        this.optDownSlope = optDownSlope;
//        offsetHighDown = 1               - highCostDownHm * highCostDownSlope;
    }

    public void setBaseCostHm(double baseCostHm) {
        this.baseCostHm = baseCostHm;
    }

    protected void setGenCostFactor(double genCostFactor) {
        this.genUpCostFactor =  genCostFactor;
        this.genDownCostFactor =  genCostFactor;
    }

    protected void setCostFactorMulti(double costFactorRed) {
        this.genUpCostFactor  = Math.max(1,this.genUpCostFactor*costFactorRed);
        this.genDownCostFactor  = Math.max(1,this.genDownCostFactor*costFactorRed);
    }

    protected void setUpGenCostFactor(double genCostFactor) {
        this.genUpCostFactor =  genCostFactor;
    }
//    protected void setUpCostFactorMulti(double costFactorRed) {
//        this.genUpCostFactor  = Math.max(1,this.genUpCostFactor*costFactorRed);
//    }
    protected void setDownGenCostFactor(double genCostFactor) {
        this.genDownCostFactor =  genCostFactor;
    }
//    protected void setDownCostFactorMulti(double costFactorRed) {
//        this.genDownCostFactor  = Math.max(1,this.genDownCostFactor*costFactorRed);
//    }

//    protected void setFixUpDistParameter(double distFactor ){
//        this.fixUpDistFactor = distFactor;
//    }
//    protected void setFixDownDistParameter(double distFactor ){ this.fixDownDistFactor = distFactor;};


//    @Override
//    public double getHeuristicCosts(PointModel node1, PointModel node2) {
//        double d = PointModelUtil.distance(node1, node2);
//        float e = PointModelUtil.eleDiff(node1, node2);
//        double slope = e/d;
//        double costHm = baseCostHm;
//        double costd;
//        if (e > 0 ){
////            if (true) return d;
//            if (d < 3 * avgDist) return d;
//            else if (true) return d + baseCostHm * ( d - 3 * avgDist )  * slope * 0.5 ;
//            if( slope <= optUpSlope ) return  d + baseCostHm * e; // cost depend on distance + elevation difference
//            else {
//                costd = costDistLargerOptUpEle;
//                if ( d < avgDist){
//                    costHm = baseCostHm + ( highCostUpHm - baseCostHm ) *  d / avgDist; // see also calcCostFactor!!
//                    costd  = 1 + (baseCostHm - costHm) * optUpSlope; // if costd = 0 costHm = baseCostHm + 1/optUpSlope
//                }
//                if (costd <= 0)
//                    return (baseCostHm + 1 / optUpSlope) * e; // as log costd negative (since dist cannot be negative) minimal costs just depend on elevation diff
//                else return costd * d + costHm * e; // no other cheaper path exists
//            }
//        }
//        else {
//            if (true) return d;
//            if( -slope <= optDownSlope ) return  d;
//            else {
//                costd = costDistLargerOptDownEle;
//                if ( d < avgDist){
//                    costHm =  highCostDownHm  *  d / avgDist; // see also calcCostFactor!!
//                    costd  =  1 - costHm * optDownSlope;
//                }
//                if (costd <= 0) return -1 / optDownSlope * e;
//                else return costd * d - costHm * e;
//            }
//        }
//    }

    @Override
    public void initializeSegment(Way way) {
        initClearCnt=initClearCnt+1;
        initCnt = initCnt + 1;
//        if (initCnt == 28 )
//            log = false;
//        if (initClearCnt != 1)
//            log = false;
        log = false;
        cost12 = 0;
        cost21 = 0;
        this.root = new Path();
        this.path = root;
    }

    protected void initOffsets(){
        costDistLargerOptUpEle   = 1 + (baseCostHm - highCostUpHm)  * optUpSlope; //cost per distance if slope larger than optimal slopw
        costDistLargerOptDownEle = 1               - highCostDownHm * optDownSlope;
    }


    private class CostFactors{

        protected   double upCostFactor;
        protected   double downCostFactor;
        protected   double slope;
    }

    private class Path {
        public PointModel node1 = null;
        public PointModel node2 = null;
        public GNeighbour neighbour12 = null;
        public GNeighbour neighbour21 = null;
        public double d;
        public float e;
        //       public double slope;
        public Path next;
//        public CostFactors costFactors = null;

        public void setCost(CostFactors costFactors) {
//           neighbour12.setDebug(d,e, way);
//           neighbour21.setDebug(d,-e, way);
            if (log)
                Log.d("cost",String.valueOf(d)+" "+String.valueOf(costFactors.upCostFactor)+" "+String.valueOf(costFactors.slope)
                        +" "+ String.valueOf(node1.getLat())+" "+String.valueOf(node1.getLon()+" "+String.valueOf(node1.getEleD()))
                        +" "+ String.valueOf(node2.getLat())+" "+String.valueOf(node2.getLon()+" "+String.valueOf(node2.getEleD()))
                );
            if (costFactors.slope > 0) {//ascent
                neighbour12.setCost(d * costFactors.upCostFactor);
                neighbour21.setCost(d * costFactors.downCostFactor);
                if (log) {
                    cost12 = cost12 + d * costFactors.upCostFactor;
                    cost21 = cost21 + d * costFactors.downCostFactor;
                }
            } else {// descent
                neighbour21.setCost(d * costFactors.upCostFactor);
                neighbour12.setCost(d * costFactors.downCostFactor);
                if (log) {
                    cost12 = cost12 + d * costFactors.downCostFactor;
                    cost21 = cost21 + d * costFactors.upCostFactor;
                }
            }
//            this.costFactors = costFactors;
        }
    }

    @Override
    public void setNodes(PointModel node1, GNeighbour neighbour12, PointModel node2, GNeighbour neighbour21) {
        if (log){
//            Log.d("set",String.valueOf(node1.getLat())+" "+String.valueOf(node1.getLon()));
            path.node1 = node1;
            path.node2 = node2;
        };
        double d = PointModelUtil.distance(node1, node2);
        float e = PointModelUtil.eleDiff(node1, node2);

        if (root == null) {
            path = new Path();
            path.d = d;
            path.neighbour12 = neighbour12;
            path.neighbour21 = neighbour21;
            path.setCost(calcCostFactor(d, e));
        } else {
            path.d = d; //PointModelUtil.distance(node1, node2);
            path.e = e;
            path.neighbour12 = neighbour12;
            path.neighbour21 = neighbour21;
            path.next = new Path();
            path = path.next;
        }
    }

    @Override
    public void finalizeSegment() {
        if ( root.next != null ) {
            Path minpath = root;
            Path midpath = root;
            Path maxpath;
            Path iterpath;

            double d1 = 0;
            double d2 = 0;
            float e1 = 0;
            float e2 = 0;
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

            CostFactors costFactors = calcCostFactor(d1 + d2, e1 + e2);
            iterpath = root;
            while (iterpath != midpath) {
                iterpath.setCost(costFactors);
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
                costFactors = calcCostFactor(d1 + d2, e1 + e2);
                midpath.setCost(costFactors);
                midpath = midpath.next;
            }
// process last section
            iterpath = midpath;
            while (iterpath.next != null) {
                iterpath.setCost(costFactors);
                iterpath = iterpath.next;
            }

//            iterpath = root;
//            while (iterpath.next != null) {
//                if (iterpath.costFactors == null || iterpath.costFactors.upCostFactor < 1 || iterpath.costFactors.downCostFactor < 1) {
//                    iterpath.setCost(calcCostFactor(300, 0));
//                }
//                iterpath = iterpath.next;
//            }
        }
        if(log)
            Log.d("SegmentCost","cost12:"+String.valueOf(cost12)+",cost21:"+String.valueOf(cost21));
        clearSegment();

    }

    public void clearSegment(){
        root = null;
        path = null;
        initClearCnt=initClearCnt-1;
    }

    private CostFactors calcCostFactor( double dist, float elediff){
        CostFactors costFactors = new CostFactors();
        costFactors.slope = elediff / dist;
// uphill factors
//        if ( fixUpDistFactor != 1 ) costFactors.upCostFactor=fixUpDistFactor;
//        else
         if ( abs(costFactors.slope) > optUpSlope) {
            double highCostHm = highCostUpHm;
            double offsetHigh = this.costDistLargerOptUpEle;
            if ( dist < avgDist){
                highCostHm = baseCostHm + ( highCostHm - baseCostHm ) *  dist / avgDist; // damping high costs for short distances to reduce artifacts ...
                offsetHigh = 1 + (baseCostHm - highCostHm) * optUpSlope; // see also heuristic Caluculation!!!
            }
            costFactors.upCostFactor = (offsetHigh + highCostHm * abs(costFactors.slope))*genUpCostFactor;
        }
        else
            costFactors.upCostFactor= (1 + baseCostHm * abs(costFactors.slope))*genUpCostFactor;
// downhill factors
//        if ( fixDownDistFactor != 1 ) costFactors.upCostFactor=fixDownDistFactor;
//        else
        if ( abs(costFactors.slope) > optDownSlope) {
            double highCostHm = CostProviderEle.highCostDownHm;
            double offsetHigh = this.costDistLargerOptDownEle;
            if (  dist < avgDist){
                highCostHm =  highCostHm * dist / avgDist; // damping high costs for short distances to reduce artifacts ...
                offsetHigh = 1 - highCostHm * optDownSlope;
            }
            costFactors.downCostFactor = (offsetHigh + highCostHm * abs(costFactors.slope))*genDownCostFactor;
        }
        else
            costFactors.downCostFactor = genDownCostFactor;
        return costFactors;
    }

}

