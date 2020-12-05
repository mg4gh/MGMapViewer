<small><small>[Back to Index](../../../index.md)</small></small>

## Further Features: statistic view

There is a statistic view to keep an overview on all existing tracks.

To enter the statistic view use <span style="color:gray">*Menu | Statistic*</span>.

<img src="./stat1.png" width="200" />&nbsp;

#### Statistic information

The following figure shows an example of the statistic view - for the coloring see [here](../../track.md).
Inside of a color group the entries are <a href="#sort">sorted (descending)</a> .

<img src="./stat2.png" width="400" />&nbsp;


<img src="./stat3.png" width="800" />&nbsp;

Each statistic entry consists of three lines - details are explained above.


#### Single entry context menu

With a long press on an statistic entry you get a popup menu (you may notice the color change for the selected entry):

<img src="./popup.png" width="400" />&nbsp;

This menu offers functionality:
- Show Track: open this track as the selected track
- Marker Track: open this track as the marker track
- Height Profile: open the height profile for this track
- Share Track: open a share intent - allows to send the track as gpx file via mail, Whatsapp, etc
- Delete track: delete the track (there is a confirm required - but once you confirm, then it will be deleted, you don't get it back!)

#### Multi entry context menu

You can mark a statistic entry with a simple (short) tap on it. As a visual feedback the color of the entry becomes slightly stronger.
Tapping on a marked entry does unmark it, so the color changes again back.
With a long tap you get again a context menu with those entries, which make sense for multiple entries:

<img src="./popup2.png" width="400" />&nbsp;

This context menu offers functionality:
- Show Track: open all marked tracks, the entry with the context menu as the selected one
- Share Track: open a share intent - allows to send the marked tracks as gpx files via mail, Whatsapp, etc
- Delete track: delete the tracks (there is a confirm required - but once you confirm, then they will be deleted, you don't get them back!)

#### <a id="sort">Sorting of entries</a>

Basically the entries are sorted descending by date.

But which date? ... and what about entries without timestamp?  
Each TrackLog has a "sort-name" which consists of the date prefix followed by the name.
The date will be taken from the main track statistic, which contains the gpx header timestamp (if available) or otherwise from the first point (if available).
If no date at all is available, the "0" timestamp is taken, which corresponds to the 1.1.1970.

With a double-click on an entry you can switch the name to the sort-name, and with a second double click back to the original name.



<small><small>[Back to Index](../../../index.md)</small></small>
