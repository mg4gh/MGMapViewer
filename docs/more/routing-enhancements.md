# Routing enhancements

This page tries to describe the ideas for optimization of the routing feature

### Problem

Current algorithm is limited by the amount of memory given by Android to the app.
This amount is increased by the `android:largeHeap="true"` option in the manifest.xml.
Still memory is the limiting factor for long routes.

Each Node of the graph is a separate Object (GNode), each neighbour too (GNeighbour). Furthermore each GNodeRef is also a separate object.
Persistent caching of these objects seems to be difficult, since all object relationships need to be cached too.

One more problem of current implementation is that approaches might be outdated due to caching of GTile (GGrapthTile might be thrown away and setup newly - then 
the approach point equals to a new one, but is not in the node list of GTile - see `RoutingEngine.getVerifyRoutePointModel`).

### Ideas

Replace the fine granular object structure in GGraphTile by some ByteBuffers that contain main part of the data, in particular:
- node BB (identified by idx)
  - lat, lon: int (in microdegree)
  - ele: float
  - flags (4 - border node, 3 - height smoothing) - 2 byte
  - first neighbour (2 byte)  index to the Neighbour BB of this tile (first neighbour points always to itself - as of today, but is this really necessary?)  
- Neighbour BB (identified by idx)
  - neighbour node (2 bytes)
    - 3 bits neighbour tile selector
    - 13 bit index of node in referenced tile (assumption: not more than 8192 point i one tile)
  - next neighbour (2 byte) index to the Neighbour BB of this tile
  - distance ? mybe keep as float
  - cost ?
    - today cost is attribute of GNeighbour, but reset an profile/map changes
    - Would it be better to calc costs always as part of routing process (similar to what is done today with the lazy calculation/filling of the cost - just do it always - but on moving/recalculating a route it could slow down)? 
      If so, the we do not need the cost attribute, since value is calculated just in time
    - if we use cost, probably a float value would be sufficient
  - wayAttributes index (two bytes) 
  - primaryDirection (derived from: (idx %2 == 0) || (neighbourTileSelector!=0) || (wayAttributes==-1))
  - reverseNeighbour: derived from (idx ^ 1)
- NodeRef Array (array size = number of nodes), may contain a GNodeRef reference (for bidirectional AStar use two such arrays)
- GNodeRef
  - node  (not needed - given via index to node BB)
  - predecessor (4 bytes tile idx + two byte node reference - as neighbour node in neighbour BB)
  - flags (2 byte)  - setteled, reverse
  - neighbour (not needed - not used today)
  - cost (use float instead double) (calculated during settlement of predecessor, used during own settlement action)
  - heuristicCost (use float instead double) ((calculated during settlement of predecessor, used for sorting the prio queue)
- Prio queue (TreeSet of GNode ref, sorted by heuristicCost)
  - does it make sense to remove entries after settlement? (frees some memory, but requires much rebalancing of prio queue)
- Array of WayAttributes

Setup of data structures:
- keep neighbour and reverse neighbour always as two consecutive entries in the neighbourBB
- place tile connecting neighbours (both directions) always in both neighbourBB - use for settlement only neighbours from neighbourBB of own tile.

Further aspects:
- Don't need to keep track usage of GGraphTile, since the is no object reference between tiles - it contains only node index values that are the same 
  after recreation of the tile  
  This can simplify the GTileCache significant
- Approaches should contain
  - tileX, tileY: (int) were used for validateApproachModel -> now use (short) for node1,node2 tile identification
  - pmPos (PointModel) - unchanged
  - node1, node2: (short) use index to nodeBB 
  - approachNode: should be PointModel (no longer GNode)
- In fact nowhere is a set of ApproachModel used (only best one)
  - could be placed directly in RoutePointModel ?? oder ist das eine blöde Abhängigkeit? von routing->graph
  - move calcApproaches to graph package (similar to verify)
  
- RoutingProfile needs to be changed
  - change internal methods to float values
  - replace external called Methods to use no PointModel anymore

### Preparation

- separation graph package and routing package
- reduce to one (best) approach per RPM
- ApproachModel as Interface of ApproachModelImpl (as part of model package, while approachModelImpl is part of graph package)
  - just getter for pmPos, pmApproach, distance
  - access to pmNode1, pmNode2 only via approachModelImpl
  - access to approachModelImpl via GMultiGraph (inside graph package) 
- move calcApproaches (similar to validateApproachModel) to GGraphTileFactory
- move calcRouting method from RoutingEngine to GGraphMulti, e.g. calcRoute
  - parameter should be ApproachModel (may have null as pmApproach) for source and target
