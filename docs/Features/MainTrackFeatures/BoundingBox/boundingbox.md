<small><small>[Back to Index](../../../index.md)</small></small>

## Main Track Feature: Use a bounding box to search track(s) ... and more

Initially the bounding box feature was intended to provide a simple search for tracks through a marked area.
Meanwhile a couple of other features for [mapstores](../../MainMapFeatures/MapStore/mapstore.md) are added.

### Switch bounding box feature on and mark a relevant area

Switch on the bounding box mode with <img src="../../../icons/group_bbox1.svg" width="24"/> + <img src="../../../icons/bbox2.svg" width="24"/>.
A central square becomes visible, which is in fact a suggestion for the bounding box.

<img src="./bb_m1.png" width="200" />&nbsp;

Drag and drop the corners with small blue circles to resize the bounding box according to the needs.

### Select and execute the desired action on the bounding box

There are several actions for a bounding box available:

- Load tracks that pass through the marked area
- Load all tiles of a marked area
- Load missing tiles of a marked area
- Remove all tiles inside a marked area

All tile store related actions are only available, if at least one mapstore layer with config.xml is configured 
(see  [mapstore](../../MainMapFeatures/MapStore/mapstore.md)).

Additionally to the config.xml there might be a sample of a tile request as a curl command in the file "sample.curl". HTTPConnection request 
parameter are taken over from this sample. If this HTTPConnection request includes a cookie, then you can provide also an cookies.json file as 
it will be provided by typical cookie manager plugins. Then the cookies in the sample.curl will be refreshed with the values from the cookies.json file.

#### Load tracks that pass through the marked area

Use <img src="../../../icons/group_bbox2.svg" width="24"/> + <img src="../../../icons/load_from_bb.svg" width="24"/> to load tracks passing through the bounding box.

As the result all tracks passing through the marked area will be loaded. If there are multiple tracks, you can select one of them just by tapping on it.
With the use of <img src="../../../icons/group_hide.svg" width="24"/> + <img src="../../../icons/hide_stl.svg" width="24"/>
or alternatively <img src="../../../icons/group_hide.svg" width="24"/> + <img src="../../../icons/hide_atl.svg" width="24"/>
you can reduce the number of visible tracks to the track(s) you are searching for.

<img src="./bb_a1a.png" width="200" />&nbsp;
<img src="./bb_a1b.png" width="200" />&nbsp;

**Shortcut**: A long press action inside the marked area triggers also the load action.

<a name="tileloading"> </a>

## Tile loading

This feature can also be used to load (or drop) tiles for a [mapstores](../../MainMapFeatures/MapStore/mapstore.md).

#### Load all tiles of a marked area

With <img src="../../../icons/group_bbox2.svg" width="24"/> + <img src="../../../icons/bb_ts_load_all.svg" width="24"/>  you can trigger a download of all tiles inside the marked area.
Limitations of the zoom level are given by the config.xml file. This option can be used, if there are no tiles for the specified area available yet.
But if the map data will change over the time, this option can also be used to refresh your tiles in the store after some time.

#### Load missing tiles of a marked area

In a similar way you can download just the not yet available tiles by using
<img src="../../../icons/group_bbox2.svg" width="24"/> + <img src="../../../icons/bb_ts_load_remain.svg" width="24"/>.
Imagine you have a region almost fully available in the app and you want slightly enlarge it without download everything again.

Remark: the tile number for a download request doesn't yet reflect already existing once.

#### Remove all tiles inside a marked area

As your store might grow due to different loading requests, it will require more and more space. Once you realize, that you don't need anymore some data
inside a mapstore, you can drop tiles from it. This can be achieved by
<img src="../../../icons/group_bbox2.svg" width="24"/> + <img src="../../../icons/bb_ts_delete_all.svg" width="24"/>.
This command deletes all tiles that are fully included by this given bounding box.

Remark: There is currently no "VACUUM" command after the deletion process, since this command may run quite long. So the database file will not shrink,
but there is again free space inside to store new tiles.

### Switch off the bounding box feature 

Finally, after some bounding box action, you want to leave this feature. This can be done by
<img src="../../../icons/group_bbox2.svg" width="24"/> + <img src="../../../icons/bbox.svg" width="24"/>.
Alternatively this mode will be left automatically after 30 seconds, if there is no bounding box related action recognized during this time.


<small><small>[Back to Index](../../../index.md)</small></small>