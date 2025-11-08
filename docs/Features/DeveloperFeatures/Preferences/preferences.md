<small><small>[Back to Index](../../../index.md)</small></small>

## Developer Features: Preferences 

**Caution:** use this feature only, if you exactly know what you do!

There is an option to set manually shared preferences values. Create in the config folder a subfolder `load` containing any file with the extension 
".properties" which contain key value pairs. By default these preferences are considered as String preferences. Using the prefix "Boolean:" in the 
value is changing this behaviour to a Boolean preference.


### Location based search

If you use the playstore version and you want to use full search functionality, you need to set

```
FSSearch.reverseSearchOn=Boolean:true
FSSearch.locationBasedSearchOn=Boolean:true
```
From the device you can use this link create this configuration: 
[geocode.zip](mgmap-install://mg4gh.github.io/MGMapViewer/Features/FurtherFeatures/Geocode/geocode.zip)

### Routing algorithm

You can configure three different routing algorithms:
- [AStar](mgmap-install://mg4gh.github.io/MGMapViewer/Features/MainTrackFeatures/Routing/routing_astar.zip)
- [BidirectionalAStar](mgmap-install://mg4gh.github.io/MGMapViewer/Features/MainTrackFeatures/Routing/routing_bidirectionalastar.zip) (AStar with search in both directions, delivers optimal result)
- [BidirectionalAStarFNO](mgmap-install://mg4gh.github.io/MGMapViewer/Features/MainTrackFeatures/Routing/routing_bidirectionalastarfno.zip) (AStar with search in both directions, faster, but may deliver none optimal result)

For BidirectionalAStar it looks like this:
```
#FSRouting.RoutingAlgorithm=AStar
FSRouting.RoutingAlgorithm=BidirectionalAStar
#FSRouting.RoutingAlgorithm=BidirectionalAStarFNO
```

### Direct Map Download

The download of [mapsforge vector maps](../../MainMapFeatures/Mapsforge/mapsforge.md) is done via the [openandromaps](https://www.openandromaps.org/) site. 
Since this page was recently down and because the location of the maps is in fact an ftp server:
```
https://ftp.gwdg.de/pub/misc/openstreetmap
```
there is an option download directly from the ftp server.
For this purpose you can use the preference:
```
DownloadMapsDirect=Boolean:true
```


### Load tiles for transparent layers

When you use tile stores and also the hgt layer, then you can use the bounding box to load further tiles/hgt files. 
If there is more than one layer that mght be the target of such an operation, you will get a dialog to select the 
layer on which the operation (load remaining, load all or delete all) shall be executed. 
The standard behaviour is to offer all Tilestore and HgtGridLayers, except these layers are fully transparent at that point of time.
With the property
```
FSBB.loadTransparent=Boolean:true
```
you will get also transparent layers offered for the operation.

### Mapsforge Number of Render Threads

This property allows to manipulate the number of render threads in mapsforge.
```
prefMapsforgeNumRenderThreads=4
```

### Hi-Res hill shading

This property allows to enable high resolution hill shading. This functionality is fully part of mapsforge.
It promise better quality of hillshading results. Be aware that this might slow down map visualisation, especially if 
the area of multiple hgt files becomes visible.
```
preferences_hill_shading_hiRes_key=Boolean:false
```

### Bicubic height interpolation

This property enables bicubic height interpolation in the ElevationProvider (as long as this is possible within one hgt buffer - not close to the borders). 
```
prefUseBicubicInterpolation=Boolean:true
```

### Smoothing configuration

These properties allow to set the smoothing distance threshold. 
if the second config item (prefSmoothingUsePrimary) is switched on, then smoothing happens only for the primary direction (instead of random order with use of nodes visited flag)
```
prefSmoothingDistance=32
prefSmoothingUsePrimary=Boolean:true
```


### Sample config

The following sample configuration
```
#FSRouting.RoutingAlgorithm=BidirectionalAStarFNO
FSRouting.RoutingAlgorithm=BidirectionalAStar
FSSearch.reverseSearchOn=Boolean:true
FSSearch.locationBasedSearchOn=Boolean:true
DownloadMapsDirect=Boolean:false
#FSBB.loadTransparent=Boolean:false
#prefMapsforgeNumRenderThreads=2
preferences_hill_shading_hiRes_key=Boolean:false
prefUseBicubicInterpolation=Boolean:true
prefSmoothingDistance=16
prefSmoothingUsePrimary=Boolean:true
```
can be downloaded and installed [here](mgmap-install://mg4gh.github.io/MGMapViewer/Features/DeveloperFeatures/Preferences/config.zip).
After installation you can use the internal [FileManager](../../FurtherFeatures/FileManager/filemanager.md) to modify these preferences.


<small><small>[Back to Index](../../../index.md)</small></small>