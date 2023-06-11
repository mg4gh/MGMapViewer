<small><small>[Back to Index](../../../index.md)</small></small>

## Main map Feature: mapstore
The term tile store or offline tile store will be also used for this kind of store.

In this case all tiles are stored in a single sqlite database file, which has the file extension ".mbtiles".
The format definition is available via [github](https://github.com/mapbox/mbtiles-spec). If you have a prefilled database file, place it in the store directory.
You can find examples of such tile stores on openandromaps: The [overview maps](https://www.openandromaps.org/downloads/ubersichts-karten) are in this format.
They are well suited as background maps.

Alternatively you can provide a [config.xml](../MapOnline/config.xml)
configuration file, which allows to fill the store similar to an online
mapstore. The difference is that you need to trigger the download manually for a selected area using the
[bounding box](../../MainTrackFeatures/BoundingBox/boundingbox.md)
feature.

A mapstore could contain a "normal" map (e.g. downloaded from mapnik), but it could be also something like a heatmap, 
generated from whatever data.

<img src="./mapstore_map.png" width="400" />

<small><small>[Back to Index](../../../index.md)</small></small>