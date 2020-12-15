<small><small>[Back to Index](../../../index.md)</small></small>

## Further Features: statistic view

#### Open the statistic activity

There is an activity to keep an overview on all existing tracks.
To enter this statistic view use <span style="color:gray">*Menu | Statistic*</span>.

<img src="./stat1.png" width="200" />&nbsp;

#### Statistic information (Overview)

The following figure shows an example of the statistic view - for the coloring see [here](../../track.md).
Inside of a color group the entries are <a href="#sort">sorted (descending)</a> .

<img src="./stat2.png" width="400" />&nbsp;

You can select/deselect an entry with a short tap on it.

#### Statistic information (Entry)

Each statistic entry consists of three lines:

<img src="./stat3.png" width="600" />&nbsp;



#### Quick Controls of Statistic Activity

Similar to the main activity the statistic activity provides also a set of quick controls to operate on the statistic entries:

<img src="./stat2a.png" width="400" />&nbsp;

These controls are (from left to right)
- toggle fullscreen  
  enabled always
- select all entries  
  enabled, if at least one entry is not selected
- deselect all entries  
  enabled, if at least one entry is selected
- show selected entries (first selected entry in statistic becomes the "selected track")  
  enabled, if at least one entry is selected
- open selected track as marker track  
  enabled, if there is exactly one track selected and this is neither the MarkerTrackLog nor the RouteTrackLog
- share selected tracks  
  enabled, if  there is at least one track selected and all selected tracks are stored persistent. Note: Always
the persistent gpx will be shared. Not yet saved changes are not included!
- save selected tracks  
  enabled, if  there is at least one modified (not yet saved) track in the selected set. Modified tracks
are marked with a "*" at the end of the name. Unmodified tracks are untouched by this operation.
- delete selected tracks  
  enabled, if there is at least one track selected and none of the RecordingTrackLog, MarkerTrackLog and
RouteTrackLog is in the set of selected tracks.
- back action - jump back to the main (calling) activity.  
  enabled always

#### <a id="sort">Sorting of entries</a>

Basically the entries are sorted descending by date.

But which date? ... and what about entries without timestamp?  
Each TrackLog has a "nameKey" which consists of the date prefix followed by the name.
The date will be taken from the main track statistic, which contains the gpx header timestamp (if available) or otherwise from the first point (if available).
If no date at all is available, the "0" timestamp is taken, which corresponds to the 1.1.1970.

With a double-click on an entry you can switch the name to the sort-name, and with a second double click back to the original name.



<small><small>[Back to Index](../../../index.md)</small></small>
