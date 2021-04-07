<small><small>[Back to Index](../../../index.md)</small></small>

## Main Track Feature: track visualisation

The visualization of tracks is a one important feature of this app. The
track visualization is done by drawing colored lines on top of the
visible map layers.


A track is represented by a TrackLog object - please check the [track definitions](../../track.md).

The following pictures show:
- a RecordingTrackLog (red line)
- multiple available tracks (green lines) and a selected track (blue line)

<img src="./RecordingTrack1.png" width="400" />&nbsp;
<img src="./AvailableTracks2.png" width="400" />&nbsp;

Beside these three types of visualized tracks there might be also a [marker track](../MarkerTrack/markertrack.md) ( pink line) and
a [route track](../Routing/routing.md) (purple line) visible.
Out of these 5 types of tracks there are three of them (recording track, route track and selected track),
which allow an alternative gain/loss representation of this track.

The following picture shows an selected track with gain/loss representation switched on.

<img src="./gainLoss1.png" width="400" />&nbsp;

Colored depending on gain/loss means
- drak blue: steep descending
- light blue: moderate descending
- green: not ascending/descending
- yellow: moderate ascending
- red: steep ascending

Use the [Statistic activity](../../FurtherFeatures/Statistic/statistic.md) to load tracks as available, selected and marker track.

### Controls for track visualization

#### Track controls

<table style="font-size: small">
<th width="12%" style="text-align:center; min-width:60px; max-width:60px"> </th>
<th width="17%" style="text-align:center; min-width:100px"><img src="./RTL.png" width="75" height="50"></th>
<th width="17%" style="text-align:center; min-width:100px"><img src="./RoTL.png" width="75" height="50"></th>
<th width="17%" style="text-align:center; min-width:100px"><img src="./stl.png" width="75" height="50"></th>
<th width="17%" style="text-align:center; min-width:100px"><img src="./atl.png" width="75" height="50"></th>

<tr>
    <td>short tap</td>
    <td> </td>
    <td>when <img src="../../../icons/group_marker2.svg" width="24"/><br/>on point: delete point<br/>on line: insert new point </td>
    <td> </td>
    <td>make this track to the selected</td>
</tr>
<tr>
    <td>long tap</td>
    <td>toggle gain/loss mode</td>
    <td>when <img src="../../../icons/group_marker1.svg" width="24"/><br/>toggle gain/loss mode<br/><br/>
        when <img src="../../../icons/group_marker2.svg" width="24"/><br/>on point: toggle gain/loss mode<br/>on line: toggle direct route </td>
    <td>toggle gain/loss mode</td>
    <td> </td>
</tr>
</table>

#### Track transparency
It is possible to control the transparency for all track types. To do this use
<img src="../../../icons/group_hide.svg" width="24"/> + <img src="../../../icons/slider_layer2.svg" width="24"/>.
The next figures show an example of the usage: First the marker track is invisible, while in the second figure the transparency of the marker track is reduced, so it becomes well visible.

<img src="./alpha1.png" width="400" />&nbsp;
<img src="./alpha2.png" width="400" />&nbsp;

As a second example we increase transparency of the selected track. So the other available (green) track becomes much better visible and also the map properties are better visible.

<img src="./alpha3.png" width="400" />&nbsp;

There is no control for the recording track log visible, since there is currently no recording track. Switch off the track transparency controls again with
<img src="../../../icons/group_hide.svg" width="24"/> + <img src="../../../icons/slider_layer1.svg" width="24"/>.


<small><small>[Back to Index](../../../index.md)</small></small>

