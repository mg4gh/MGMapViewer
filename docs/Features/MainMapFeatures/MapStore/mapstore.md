## Main map Feature: mapstore
The term tile store or offline tile store will be also used for this kind of store.

There are tow options available:
- Tile store based on file system structure
- Tile store based on SQLite database (*.mbtiles file) 

If you are free, the second option is recommended - since handling of
large amounts of files is slow (e.g. for backup purposes). In both cases
you can also provide a [config.xml](../MapOnline/config.xml)
configuration file, which allows to fill the store similar to an online
mapstore.

### Tile store based on file system structure

A mapstore is a directory with the structure \<store_name>/\<zoom_level>/\<x-tile>/\<y-tile>.png. 
This directory will be placed as a subdirectory to "./MGMapViewer/maps/mapstores/".

The following picture shows an example of such a structure:

<img src="./mapstore.png" width="200" />

A mapstore could contain a "normal" map (e.g. downloaded from mapnik), but it could be also something like a heatmap, 
generated from whatever data.

<img src="./mapstore_map.png" width="400" />

### Tile store based on SQLite database (*.mbtiles file) 

There is a second option to provide a tile store. In this case all tiles are stored in a single sqlite database file, which has the file extension ".mbtiles".
The format definition is available via [github](https://github.com/mapbox/mbtiles-spec). Place the database file with the tiles in the store directory instead the zoom subdirectories.

You can find examples of such tile stores on openandromaps: The [overview maps](https://www.openandromaps.org/downloads/ubersichts-karten) are in this format.
They are well suited as background maps.

Another usecase of these tile stores is to setup your own tile database.
Create a store similar to an [online store](../MapOnline/maponline.md)
with a [config.xml](../MapOnline/config.xml) that specifies a source of
tiles. Put a copy of the tiles. Put a copy of the <a
href="../../../more/store.mbtiles">empty tile store</a>
in the store directory. Now you can fill this store using the
[bounding box](../../MainTrackFeatures/BoundingBox/boundingbox.md)
feature.

