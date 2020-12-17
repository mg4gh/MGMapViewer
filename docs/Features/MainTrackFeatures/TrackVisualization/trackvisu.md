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

Out of these 5 types of tracks there are three of them, which allow an alternative gain/loss representation of this track:

| track type | allow gain/loss representation | Controls to toggle representation |
|---|---|---|
| recording track | X | long press on track |
| marker track | - | |
| route track | X | long press on track (only if marker track edit is switched off)  or long press on one of the marker track points (always) |
| selected track | X | via settings menu or via long press on track |
| available track | - | |

The following picture shows an selected track with gain/loss representation switched on.

<img src="./gainLoss1.png" width="400" />&nbsp;

Colored depending on gain/loss means
- drak blue: steep descending
- light blue: moderate descending
- green: not ascending/descending
- yellow: moderate ascending
- red: steep ascending

**Track transparency**

It is possible to control the transparency for all track types. To do this use a long tap on the "transparency control" [quick control](../../FurtherFeatures/QuickControl/quickcontrols.md).
The next figures show an example of the usage: First the marker track is invisible, while in the second figure the transparency of the marker track is reduced, so it becomes well visible.

<img src="./alpha1.png" width="400" />&nbsp;
<img src="./alpha2.png" width="400" />&nbsp;

As a second example we increase transparency of the selected track. So the other available (green) track becomes much better visible and also the map properties are better visible.

<img src="./alpha3.png" width="400" />&nbsp;

There is no control for the recording track log visible, since there is currently no recording track. Switch off the track transparency controls with another long press of the same
quick control.

**Track selection**

There are multiple options to make a track the selected track:
- If there is more than one available track, then just tap on a track and this track becomes the new selected track.
- Use the <span style="color:gray">*Menu | Load Track | Prev*</span> &nbsp;&nbsp;and&nbsp;
  <span style="color:gray">*Menu | Load Track | Next*</span> &nbsp;to get the
  previous/next track (from the set of MetaTrackLogs). This corresponds to the set of known tracks sorted by name.
- Use the track statistics activity and choose a
  new selected track via the context menu entry "Select Track" of the track.

<small><small>[Back to Index](../../../index.md)</small></small>

