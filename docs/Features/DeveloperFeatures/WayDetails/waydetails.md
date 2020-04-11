## Developer Features: way data

This feature supports development of the
[routing](../../MainTrackFeatures/Routing/routing.md) feature. The basis of the routing algorithm is a graph. 
But if routing fails, is this related to a bug in the data or does the 
routing process cause the problem? To answer this question it is necessary to investigate the relevant
graph data. So how to investigate a special situation, where routing doesn't provide the desired result.
For this purpose it is possible to visualize the way raw data from the tiles.

This is done with <span style="color:gray">*Menu | Setting*</span>, check the box "way details" and
the go back to the map view as shown here:

<img src="./wd1.png" width="300" />&nbsp;<img src="./wd2.png" width="300" />&nbsp;<img src="./wd3.png" width="300" />&nbsp;

Now the *way details* mode is enabled: If you tap to the marked position, you get following result:

<img src="./wd4.png" width="400" />&nbsp;
<img src="./wd5.png" width="400" />&nbsp;

The rectangle visualize the size of the current tile. All way entries of the tile are shown.
This is done with a small gray circle for each point and a thin gray connecting line.
Out of these data the [graph](../Developer/Graph.png) is setup. A graph segment can be
considered as a set of consecutive nodes each with exactly two neighbours (no junctions).
Due to the tap on the graph the tapped graph segment will be highlighted with red color.
If this segment cross the tile boarder, then the graph of the tile will be limited to this boarder.
This might require an extra node for the graph.

Zooming a few steps in, this becomes better visible:

<img src="./wd6.png" width="400" />&nbsp;

This visualization helps to understand the results of the routing process.