## Definitions: Track 

A track is represented by a TrackLog object, which consists of one or multiple TrackLogSegment 
objects.

The following terminology is used for TrackLog objects in this description (and in the app):

|  TrackLog Type |  Map visualisation | Statistic visualisation | Description |
|---|---|---|---|
| RecordingTrackLog | red line | red entry | track with currently ongoing recording action |
| MetaTrackLog | no line | gray entry | track log from meta files (exists for each known track) |
| AvailableTrackLog | green line | green entry | currently visible track log |
| SelectedTrackLog | either blue line or colored depending on height gain/loss | blue entry | one specific TrackLog from the amount of AvailableTrackLogs |

