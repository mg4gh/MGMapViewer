<small><small>[Back to Index](../../../index.md)</small></small>

## Main map Feature: mapstore
The term tile store or offline tile store will be also used for this kind of store.

A mapstore layer consists of a directory with the name of the layer.
This is a direct subdirectory of ./MGMapViewer/maps/mapstores. 

In a mapstore all tiles are stored in a single sqlite database file, which has the file extension ".mbtiles".
The format definition is available via [github](https://github.com/mapbox/mbtiles-spec). If you have a prefilled database file, place it in the store directory.
You can find examples of such tile stores on openandromaps: The [overview maps](https://www.openandromaps.org/downloads/ubersichts-karten) are in this format.
They are well suited as background maps.

Alternatively you can provide a configuration file with the fix name config.xml, 
which allows to fill the store for a selected area. The difference is that you need to trigger the download manually for a selected area using the
[bounding box](../../MainTrackFeatures/BoundingBox/boundingbox.md#tileloading)
feature. Such a mapstore could contain a "normal" map (e.g. downloaded from mapnik), but it could be also something like an overlay heatmap, 
generated from whatever data.

<img src="./mapstore_map.png" width="400" />

Here you can find more [samples](../SampleConfig/sampleconfigs.md) for configuration files.

<small><small>[Back to Index](../../../index.md)</small></small>