<small><small>[Back to Index](../../../index.md)</small></small>

## Developer Features: Preferences 

**Caution:** use this feature only, if you exactly know what you do!

There is an option to set manually shared preferences values. Create in the config folder a subfolder load containing files with the extension 
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


### Sample config

The following sample configuration
```
#FSRouting.RoutingAlgorithm=BidirectionalAStarFNO
FSRouting.RoutingAlgorithm=BidirectionalAStar
FSSearch.reverseSearchOn=Boolean:true
FSSearch.locationBasedSearchOn=Boolean:true
DownloadMapsDirect=Boolean:false
```
can be downloaded and installed [here](mgmap-install://mg4gh.github.io/MGMapViewer/Features/DeveloperFeatures/Preferences/config.zip).
After installation you can use the internal [FileManager](../../FurtherFeatures/FileManager/filemanager.md) to modify these preferences.


<small><small>[Back to Index](../../../index.md)</small></small>