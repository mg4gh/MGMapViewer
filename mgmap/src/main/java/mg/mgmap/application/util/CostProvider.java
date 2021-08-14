package mg.mgmap.application.util;

import org.mapsforge.map.datastore.Way;

import mg.mgmap.generic.graph.GNeighbour;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.util.Pref;


public abstract class CostProvider {
//     private Way way = null;
     protected boolean accessable;
     public int initClearCnt = 0;
     public int initCnt = 0;

     static private CostProvider  costProvider;

//    static public RoutingProfileChangedObserver routingProfileChangedObserver = new RoutingProfileChangedObserver();
//
//    static private class RoutingProfileChangedObserver implements Observer{
//        @Override
//        public void update(Observable observable, Object o) {
//            Pref<?> pref = (Pref<?>)observable;
//            newCostProvider((String)pref.getValue());
//        }
//    }

    static public void initCostProvider(Pref<?> pref){
//        pref.addObserver(routingProfileChangedObserver);
        newCostProvider((String)pref.getValue());
    }


    static private void newCostProvider(String routingProfile){
        if ( routingProfile.equals("ShortDist"))
            costProvider = new CostProviderShortDist( );
        else if ( routingProfile.equals("CycTour"))
            costProvider = new CostProviderCycTour( );
      else
            costProvider = new CostProviderMTBfun( );
    }

    static public CostProvider getInst(){
        return costProvider;
    }


//    public CostProvider(MGMapApplication application ) {this.application = application; }


    public final boolean isWay(){
        return accessable;
    }

    public abstract void setNodes(PointModel node1,GNeighbour neighbour12, PointModel node2,GNeighbour neighbour21) ;

//    public abstract double getHeuristicCosts(PointModel node1,PointModel node2);
    public abstract void initializeSegment(Way way) ;
    public abstract void finalizeSegment() ;
    public abstract void clearSegment();

}
