## Main map Feature: mapsforge

Typically you will download mapsforge compliant maps from [openandromaps](https://www.openandromaps.org/). 
But you can create these maps also with the software from the [mapsforge](https://github.com/mapsforge/mapsforge) library.

A mapsforge map is a ".map" file. This file has to be placed in the directory "./MGMapViewer/maps/mapsforge/"

Here is an example of such a map: 

<img src="./mapsforge_map.png" width="400" />

Since map files are large you can share mapfiles for multiple apps. 
For this reason you can also use a reference to such a map file.

Such a reference is a file e.g. "test.ref" with the content

<img src="./map_ref.png" width="400" />

It has also to be placed in the directory "./MGMapViewer/maps/mapsforge/". The referenced location must 
be readable for the app and this might require permission for external storage (if not yet granted anyhow)." 

Remark: Mapsforge maps require installation of corresponding [themes](../MapsforgeThemes/mapsforgethemes.md).

Remark: Unfortunately the external storage is in Android devices formatted in a way that it does NOT support symlinks , which would be easier.
