<small><small>[Back to Index](../../../index.md)</small></small>

## Further Features: statistic activity

Beside the main activity this statistic activity is the most important one, since it allows you to manage
existing track objects. You can open it with
<img src="../../../icons/group_task.svg" width="24"/> + <img src="../../../icons/statistik.svg" width="24"/>.

#### Statistic information (Overview)

The following figure shows an example of the statistic view - for the coloring see [here](../../track.md).
Inside of a color group the entries are <a href="#sort">sorted (descending)</a> .

<img src="./stat.png" width="400" />&nbsp;

You can select/deselect an entry with a short tap on it. Selected entries have a stronger color and top-left square is checked.

#### Statistic information (Entry)

Each statistic entry consists of three lines:

<img src="./stat3.png" width="600" />&nbsp;



#### Quick Controls of Statistic Activity

<table style="font-size: x-small">
<th width="6%" style="text-align:center; min-width:50px; max-width:50px"> </th>
<th width="10%" style="text-align:center; min-width:64px; padding:2px"><img src="../../../icons/filter.svg" width="60px" height="60px"><br><img src="../../../icons/filter2.svg" width="60px" height="60px"></th>
<th width="10%" style="text-align:center; min-width:64px; padding:2px"><img src="../../../icons/select_all.svg" width="60px" height="60px"><br><img src="../../../icons/deselect_all.svg" width="60px" height="60px"></th>
<th width="10%" style="text-align:center; min-width:64px; padding:2px"><img src="../../../icons/edit2.svg" width="60px" height="60px"></th>
<th width="10%" style="text-align:center; min-width:64px; padding:2px"><img src="../../../icons/show.svg" width="60px" height="60px"></th>
<th width="10%" style="text-align:center; min-width:64px; padding:2px"><img src="../../../icons/mtlr.svg" width="60px" height="60px"></th>
<th width="10%" style="text-align:center; min-width:64px; padding:2px"><img src="../../../icons/share.svg" width="60px" height="60px"></th>
<th width="10%" style="text-align:center; min-width:64px; padding:2px"><img src="../../../icons/save.svg" width="60px" height="60px"></th>
<th width="10%" style="text-align:center; min-width:64px; padding:2px"><img src="../../../icons/delete.svg" width="60px" height="60px"></th>
<th width="10%" style="text-align:center; min-width:64px; padding:2px"><img src="../../../icons/back.svg" width="60px" height="60px"></th>


<tr>
    <td style="text-align:center; min-width:50px; padding:2px">enabled condition</td>
    <td style="text-align:center; min-width:64px; padding:2px">always<br> 2nd icon,  if filter is on</td>
    <td style="text-align:center; min-width:64px; padding:2px">always<br> 2nd icon, if all entries selected</td>
    <td style="text-align:center; min-width:64px; padding:2px">exactly one entry is selected</td>
    <td style="text-align:center; min-width:64px; padding:2px">at least one entry is selected</td>
    <td style="text-align:center; min-width:64px; padding:2px">there is exactly one track selected and this is neither the MarkerTrackLog nor the RouteTrackLog</td>
    <td style="text-align:center; min-width:64px; padding:2px">there is at least one track selected and all selected tracks are stored persistent</td>
    <td style="text-align:center; min-width:64px; padding:2px">there is at least one modified (not yet saved) track in the selected set</td>
    <td style="text-align:center; min-width:64px; padding:2px">there is at least one track selected and none of the RecordingTrackLog, MarkerTrackLog and RouteTrackLog is in the set of selected tracks</td>
    <td style="text-align:center; min-width:64px; padding:2px">always</td>
</tr>
<tr>
    <td style="text-align:center; min-width:64px; padding:2px">action on tap</td>
    <td style="text-align:center; min-width:64px; padding:2px">toggle filter on/off</td>
    <td style="text-align:center; min-width:64px; padding:2px">select/deselect all visible entries</td>
    <td style="text-align:center; min-width:64px; padding:2px">edit track name</td>
    <td style="text-align:center; min-width:64px; padding:2px">view selected tracks</td>
    <td style="text-align:center; min-width:64px; padding:2px">open as marker track</td>
    <td style="text-align:center; min-width:64px; padding:2px">share tracks (Always the persistent gpx will be shared. Not yet saved changes are not included!)</td>
    <td style="text-align:center; min-width:64px; padding:2px">save tracks (Modified tracks are marked with a "*" at the end of the name. Unmodified tracks are untouched by this operation.)</td>
    <td style="text-align:center; min-width:64px; padding:2px">delete tracks</td>
    <td style="text-align:center; min-width:64px; padding:2px">back to main activity</td>
</tr>
</table>


#### <a id="sort">Sorting of entries</a>

Basically the entries are sorted descending by date.

But which date? ... and what about entries without timestamp?  
Each TrackLog has a "nameKey" which consists of the date prefix followed by the name.
The date will be taken from the main track statistic, which contains the gpx header timestamp (if available) or otherwise from the first point (if available).
If no date at all is available, the track date will be set to 02.01.2000.

<small><small>[Back to Index](../../../index.md)</small></small>
