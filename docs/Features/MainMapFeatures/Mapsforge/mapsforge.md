## Main map Feature: mapsforge

Typically you will download mapsforge compliant maps from [openandromaps](https://www.openandromaps.org/). 
But you can create these maps also with the software from the [mapsforge](https://github.com/mapsforge/mapsforge) library.

A mapsforge map is a ".map" file. This file has to be placed in the directory "./MGMapViewer/maps/mapsforge/"

Here is an example of such a map: 

<img src="./mapsforge_map.png" width="400" />

A shown already in the  [Getting Started](../../../GettingStarted/GettingStarted.md) you can use the mechanisms via
<span style="color:gray">*Menu | Settings and more | Download | Download maps*</span> to download the map you want.

Since map files are large you can share mapfiles for multiple apps. 
For this reason you can also use a reference to such a map file.
Such a reference is a file e.g. "test.ref" with a single line of content, e.g. the following:

```/sdcard/Download/bw.map```

It has to be placed also in the directory "./MGMapViewer/maps/mapsforge/". The referenced location must 
be readable for the app and this might require permission for external storage (if not yet granted anyhow)." 

Remark 1: Mapsforge maps require installation of corresponding [themes](../MapsforgeThemes/mapsforgethemes.md).

Remark 2: Unfortunately the external storage is in Android devices formatted in a way that it does NOT support symlinks , which would be easier.

Remark 3: The references to other storage locations are deprecated in Android 10, and they will not longer work in Android 11 !!
Since storage space is increasing over the last few years, this feature is recommended only for older devices.