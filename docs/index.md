The **MGMapViewer** app is first of all a viewer on maps. But it provides also several options
to deal with tracks. 
This app is based on the [mapsforge](https://github.com/mapsforge/mapsforge) library available via github. 
This library provides an excellent base for vector map visualisation. There were a few contributions to the mapsforge 
project to enable straight forward implementation in this app.

This is the [Getting Started](./GettingStarted/GettingStarted.md).

# Feature overview

Each feature contains a link to the corresponding detailed feature description.
 
### Main map features
- [mapsforge](./Features/MainMapFeatures/Mapsforge/mapsforge.md): show vector maps e.g. from [openandromaps](https://www.openandromaps.org/). 
  This is the main map type for this app.
- [mapsforge themes](./Features/MainMapFeatures/MapsforgeThemes/mapsforgethemes.md): Themes provide a customisation of the layout of mapsforge maps e.g. for hiking or MTB usage. 
- [mapstores](./Features/MainMapFeatures/MapStore/mapstore.md): show maps, which are provided via an offline tile store
- [maponline](./Features/MainMapFeatures/MapOnline/maponline.md): show maps, which are provided via an online tile store (e.g. openstreetmap via Mapnik renderer)
- [mapgrid](./Features/MainMapFeatures/MapGrid/mapgrid.md): show a customizable grid from degree of latitude and longitude
- [Overlay multiple map layers](./Features/MainMapFeatures/MapMulti/multimap.md) and control transparency per layer (except grid)

### Main track features:
- [Track visualization](./Features/MainTrackFeatures/TrackVisualization/trackvisu.md): load/show multiple tracks 
- [Record a track](./Features/MainTrackFeatures/TrackRecord/trackrecord.md): recording of a track allows multiple segments
- [Track storage](./Features/MainTrackFeatures/TrackStorage/trackstorage.md):  store tracks as gpx files, but additionally store some meta data on the tracks (for faster search)
- [Bounding Box](./Features/MainTrackFeatures/BoundingBox/boundingbox.md) search tracks by marking an area (bounding box) and load all tracks through this area 
- [Marker Track](./Features/MainTrackFeatures/MarkerTrack/markertrack.md) create a track (marker track) by tapping some points, including manipulations like moving points, 
  insert and delete points. Such a marker track can also be imported/exported. 
- [Basic Routing](./Features/MainTrackFeatures/Routing/routing.md) basic route calculation (shortest path) based on a marker track.
- [Dashboard](./Features/MainTrackFeatures/Dashboard/dashboard.md) visualization of the most important information of the recording, the selected and the route track log.

### Further features
- [enlarge](./Features/FurtherFeatures/Enlarge/enlarge.md) temporary a view entry (dashboard, status line) on a tap event (becomes readable without glasses)
- [toggle GPS](./Features/FurtherFeatures/GPS/gps.md) (without recording)
- [center](./Features/FurtherFeatures/Center/center.md) automatically current GPS position
- [Statistic view](./Features/FurtherFeatures/Statistic/statistic.md): show a table of all stored tracks with basic statistic information
- [Height profile](./Features/FurtherFeatures/HeightProfile/hprof.md): Show the height profile form the recording track, the selected track or the current route 
- [Remaining distance](./Features/FurtherFeatures/Remaining/remaining.md): show distance along the selected track 
- [Air distance](./Features/FurtherFeatures/AirDistance/airdistance.md): show the air distance between current position and the center position of the map
- [Geocode](./Features/FurtherFeatures/Geocode/geocode.md): Search location by name and search entity by location
- [Status line](./Features/FurtherFeatures/Status/status.md): show some state information in the status line
- [Quick controls](./Features/FurtherFeatures/QuickControl/quickcontrols.md): show some state information in the status line
- [GDrive](./Features/FurtherFeatures/GDrive/gdrive.md): synchronize your gpx files with a folder on your GDrive account 

### Developer features
- [Way details](./Features/DeveloperFeatures/WayDetails/waydetails.md) shows the ways of a tile 
  - based on a mapsforge map 
  - includes the tile border
  - highlights a taped graph segment
- [approaches](./Features/DeveloperFeatures/Approach/approach.md) of marker points to ways as the basis of routing
- [developer documentation](./Features/DeveloperFeatures/Developer/developer.md) provides more information for developers.

# About  
[Here](./History.md) you can find some information, how this app was originated.


Remark: Since the geocoding feature was added later, most feature
documentation doesn't contain the quick control to start geocoding
search.

