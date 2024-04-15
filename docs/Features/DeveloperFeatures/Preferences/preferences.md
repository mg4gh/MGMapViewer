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


```
FSSearch.reverseSearchOn=Boolean:true
FSSearch.locationBasedSearchOn=Boolean:true
```
From the device you can use this link create this configuration: 
[geocode.zip](mgmap-install://mg4gh.github.io/MGMapViewer/Features/FurtherFeatures/Geocode/geocode.zip)

<small><small>[Back to Index](../../../index.md)</small></small>