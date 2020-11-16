<small><small><a href="../../../index.md">Back to Index</a></small></small>

## Main Track Feature: routing

This feature provides a basic route calculation (shortest path) based on a [marker track](../MarkerTrack/markertrack.md).
But for a basic usage of this feature this isn't important.
The route calculation is using an simple [A\*](https://de.wikipedia.org/wiki/A*-Algorithmus) algorithm.
There is a further feature [routing hints](../../FurtherFeatures/RoutingHints/hints.md), which is based on this routing feature.


As already said the routing is an addon feature for the marker track - so to start route calculation
we use the quick control to start edit the marker track and verify the red color of the icon.

<img src="./Quick1.png" width="200" />&nbsp;
<img src="./Quick2.png" width="200" />&nbsp;
 
Now we start to set points:

<img src="./RT1.png" width="400" />&nbsp;
<img src="./RT2.png" width="400" />&nbsp;
<img src="./RT3.png" width="400" />&nbsp;


After finishing don't forget to press again the marker track quick control to switch off the marker track edit mode.

<img src="./Quick3.png" width="200" />&nbsp;

As it is visible in this example we need only a very few points to mark a route exactly. If the route (representing the shortest path) 
doesn't match our expectation, we need to move or insert another point to get the route as desired.
So basically this means to move/insert/remove a marker point (although we don't recognize this marker track by default).
Therefore there are (almost) the same actions valid:
- tap on a free area: add a new point at the end
- tap on a point: delete this point
- tap on the connecting route between two points: insert a new point between the existing points
- long press on the connecting route between two points: this will switch this route segment to the direct air-line instead of the route along existing ways.
Long press on the air-line will switch back to the normal calculated route.
- drag and drop of a marker point: move the position of an existing marker point

You can even combine insert and drag - tap on a route segment (to insert a point) and move it immediately to its new position.

Remarks:
- If an action above fails, check whether the "edit"-Mode is still switched on (there is a timer to deactivate it).
- The route points (in fact marker points) snap to the next way, if they are close enough. This behaviour can be toggled on/off via  
<span style="color:gray">*Menu | Settings and more | Further settings | Snap to way*</span>.
- If there isn't found a route (because there is no close way or there is no connection found, which is short enough), then the connection will also be shown as
air-line.
- The visible route has a marker track as its basis, which is not visualized by default. This behaviour can be toggled on/off via  
<span style="color:gray">*Menu | Settings and more | Further settings | Show marker track*</span>.
- It you want to save the route and you want to be able to modify the route later after reloading of it, then do not save (export) the route, rather save (export) the marker track.
If you reload it, the same route will be recalculated from this basis and you are still be able to change it again.
- In contrast, if you want to pass this route as a .gpx to someone else, then use the route export.

**Route export**

The route menu provides also an option <span style="color:gray">*Route | Route export*</span> to save the calculated route to a gpx file in the directory ./MGMapViewer/track/gpx.
The filename has the structure \<date>_\<time>_MarkerRoute.gpx. Compared to the marker track this gpx consists of much more track points to describe the
exact path as it is visible in the map. So use this export, if you want to give it to other people and/or tools.

**Route optimize**

This feature is **not** related to calculated routes as seen above. Instead, if you have a real world track then this track doesn't exactly 
match the ways you have used. And especially on zigzag ways the calculated length may differ from real world. If you wan't to
get precise data, then you need to match the recorded track to the map. Therefore the [graphhopper](https://www.graphhopper.com) project 
calls this function map matching. But we want to do the same without the internet and a server infrastructure.

Attention: Before you try a load a GPS recorded track as marker track, switch off the "snap to way" option with  
<span style="color:gray">*Menu | Settings and more | Further settings | Snap to way*</span>. Optionally you can also visualize the marker track  
<span style="color:gray">*Menu | Settings and more | Further settings | Show marker track*</span>.

Now try to load the relevant track via
- statistic view and the context menu marker track or
- load it as selected track and then use <span style="color:gray">*Menu | Marker track | Import*</span>.

After importing you can use the <span style="color:gray">*Menu | Route | Route optimize*</span> to correct the track.
This cannot be succeed 100%, but it can correct most of the issues. If desired, you can correct remaining issues manually.
In the result the optimized route reflects the real length of the track quite well.

The route optimisation is triggered via <span style="color:gray">*Menu | Route | Route on*</span>.
The next figures show two examples of typical route optimizations (each example with one picture before and one after optimization):

<img src="./OPT1.png" width="400" />&nbsp;
<img src="./OPT2.png" width="400" />&nbsp;

<img src="./OPT3.png" width="400" />&nbsp;
<img src="./OPT4.png" width="400" />&nbsp;

<small><small><a href="../../../index.md">Back to Index</a></small></small>