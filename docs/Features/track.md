<small><small>[Back to Index](../index.md)</small></small>

## Definitions: Track

A track is represented by a TrackLog object, which consists of one or multiple TrackLogSegment 
objects.

The following terminology is used for TrackLog objects in this description (and in the app):

<table style="font-size: small">
<tr>
	<th style="text-align:center">TrackLog Type</th>
    <th style="text-align:center">Map visualisation</th>
    <th style="text-align:center">Gain/Loss mode</th>
    <th style="text-align:center">Dashboard & Height profile</th>
    <th style="text-align:center">Statistic visualisation</th>
    <th style="text-align:center">Description</th>
</tr>
<tr>
    <td width="12%" style="padding:5px;vertical-align:top;min-width=100px">RecordingTrackLog</td>
	<td width="12%" style="padding:5px;vertical-align:top;min-width=100px">red line</td>
	<td width="12%" style="padding:5px;vertical-align:top;min-width=100px"> X </td>
	<td width="12%" style="padding:5px;vertical-align:top;min-width=100px">red entry</td>
	<td width="12%" style="padding:5px;vertical-align:top;min-width=100px">red entry</td>
	<td width="12%" style="padding:5px;vertical-align:top;min-width=100px">track with currently ongoing recording action</td>
</tr>
<tr>
    <td width="12%" style="padding:5px;vertical-align:top;min-width=100px">MarkerTrackLog</td>
    <td style="padding:5px;vertical-align:top">thin pink line</td>
    <td style="padding:5px;vertical-align:top"> X </td>
    <td style="padding:5px;vertical-align:top"> - </td>
    <td style="padding:5px;vertical-align:top">pink entry</td>
    <td style="padding:5px;vertical-align:top">Track from marked points for planning</td>
</tr>
<tr>
    <td width="12%" style="padding:5px;vertical-align:top;min-width=100px">RouteTrackLog</td>
    <td style="padding:5px;vertical-align:top">purple line</td>
    <td style="padding:5px;vertical-align:top"> - </td>
    <td style="padding:5px;vertical-align:top">purple entry</td>
    <td style="padding:5px;vertical-align:top">purple entry</td>
    <td style="padding:5px;vertical-align:top">calculated route based on MarkerTrack</td>
</tr>
<tr>
    <td width="12%" style="padding:5px;vertical-align:top;min-width=100px">SelectedTrackLog</td>
    <td style="padding:5px;vertical-align:top">blue line</td>
    <td style="padding:5px;vertical-align:top"> X (purple circle for marker points) </td>
    <td style="padding:5px;vertical-align:top">blue entry</td>
    <td style="padding:5px;vertical-align:top">blue entry</td>
    <td style="padding:5px;vertical-align:top">one specific TrackLog from the amount of AvailableTrackLogs</td>
</tr>
<tr>
    <td width="12%" style="padding:5px;vertical-align:top;min-width=100px">AvailableTrackLog</td>
    <td style="padding:5px;vertical-align:top">green line</td>
    <td style="padding:5px;vertical-align:top"> - </td>
    <td style="padding:5px;vertical-align:top"> - </td>
    <td style="padding:5px;vertical-align:top">green entry</td>
    <td style="padding:5px;vertical-align:top">currently visible track logs</td>
</tr>
<tr>
    <td width="12%" style="padding:5px;vertical-align:top;min-width=100px">MetaTrackLog</td>
    <td style="padding:5px;vertical-align:top"> - </td>
    <td style="padding:5px;vertical-align:top"> - </td>
    <td style="padding:5px;vertical-align:top"> - </td>
    <td style="padding:5px;vertical-align:top">gray entry</td>
    <td style="padding:5px;vertical-align:top">all known track logs</td>
</tr>
</table>


<small><small>[Back to Index](../index.md)</small></small>