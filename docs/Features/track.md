<small><small>[Back to Index](../index.md)</small></small>

## Definitions: Track

A track is represented by a TrackLog object, which consists of one or multiple TrackLogSegment 
objects.

The following terminology is used for TrackLog objects in this description (and in the app):

|  TrackLog Type |  Map visualisation | Dashboard & height profile visualisation | Statistic visualisation | Description |
|---|---|---|---|---|
| RecordingTrackLog | red line | red entry| red entry | track with currently ongoing recording action |
| SelectedTrackLog | either blue line or colored depending on height gain/loss | blue entry | blue entry | one specific TrackLog from the amount of AvailableTrackLogs |
| AvailableTrackLog | green line | - | green entry | currently visible track log |
| MarkerTrack | thin red line, thin red circles on points | - | - | Track from marked points for planning |
| Route | purple line | purple entry | - | calculated route based on MarkerTrack |
| MetaTrackLog | - | - | gray entry | track log from meta files (exists for each known track) |

<small><small>[Back to Index](../index.md)</small></small>