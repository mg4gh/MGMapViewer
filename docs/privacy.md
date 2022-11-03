# Privacy

This app tries to respect your privacy as much as possible. During "normal" operation no data is transferred to any server.
But there are a few features that need or offer internet access to different servers to match their functionality. So please check these feature with internet connectivity:

- [Maponline](./Features/MainMapFeatures/MapOnline/maponline.md)<br>As the name of this feature suggests this is to visualize online maps that consists of a set of tiles. 
For each such map you need to provide an config file with the list used serves (es the example in the feature description). The internet access for those
online maps is part of the [mapsforge](https://github.com/mapsforge/mapsforge) library. It is just executing a tile download for the requested tiles.
- [Mapstore](./Features/MainMapFeatures/MapStore/mapstore.md)<br>
A mapstore can be downloaded completely offline and copied just to the path for mapstore. But there is an additional feature that allows to download tiles
similar to the online maps, but to store these tiles additionally in a local mapstore, similar to a cache. So they are available for later usage independent 
on the internet access.
- 
- [Geocode]