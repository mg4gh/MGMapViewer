The MGMapViewer app is first of all a viewer on maps. But it provides also several options
to deal with tracks. 

This app is based on the [mapsforge](https://github.com/mapsforge/mapsforge) library available via github. 
It provides an excellent base for vector map visualisation. There were a few contributions to the mapsforge 
project to enable straight forward implementation in this app.

# Feature overview

Each feature contains a link to the corresponding detailed feature description.
 
### Main map features
- [mapsforge](./Features/MainMapFeatures/mapsforge.md): show vector maps e.g. from [openandromaps](https://www.openandromaps.org/). 
  This includes theme selection and customization of themes
- [mapstores](./Features/MainMapFeatures/mapstore.md): show maps, which are provided via an offline tile store
- [maponline](./Features/MainMapFeatures/maponline.md): show maps, which are provided via an online tile store (e.g. openstreetmap via Mapnik renderer)
- [mapgrid](./Features/MainMapFeatures/mapgrid.md): show a customizable grid from degree of latitude and longitude
- [Overlay multiple map layers](./Features/MainMapFeatures/multimap.md) and control transparency per layer (except grid)

### Main track features:
- [Track visualization](./Features/MainTrackFeatures/TrackVisualization/trackvisu.md): load/show multiple tracks 
  - recording track
  - available tracks
  - selected track
- [Record a track](./Features/MainTrackFeatures/TrackRecord/trackrecord.md): recording of a track allows multiple segments
- [Track storage](./Features/MainTrackFeatures/TrackStorage/trackstorage.md):  store tracks as gpx files, but additionally store some meta data on the tracks (for faster search)
- [Bounding Box](./Features/MainTrackFeatures/BoundingBox/boundingbox.md) search tracks by marking an area (bounding box) and load all tracks through this area 
- [Marker Track](./Features/MainTrackFeatures/MarkerTrack/markertrack.md) create a track (marker track) by tapping some points, including manipulations like moving points, 
  insert and delete points. Such a marker track can also be imported/exported. 
- [Basic Routing](./Features/MainTrackFeatures/Routing/routing.md) basic route calculation (shortest path) based on a marker track.
- [Dashboard](./Features/MainTrackFeatures/Dashboard/dashboard.md) visualization of the most important information of the recording, the selected and the route track log.

### Further features
- [storage location](./Features/FurtherFeatures/StorageLocation/storagelocation.md) selection  
- [enlarge](./Features/FurtherFeatures/Enlarge/enlarge.md) temporary a view entry (dashboard, status line) on a tap event (becomes readable without glasses)
- toggle GPS (without recording)
- automatic center current GPS position
- Statistic view: show a table of all stored tracks with basic statistic information
- Height profile: Show the height profile form the recording track, the selected track or the current route 
- show distance along the selected track 
  - remaining distance based on current position
  - remaining distance based on a given point
  - remaining distance to the reverse end of the selected track
  - distance along the track based on two given points
- show distance between current position and the center position of the map
- show current time
- show current height (or atmosphere pressure)
- show current zoom level (some information are only available at certain zoom levels)
- show battery percentage

### Developer features
- show the graph of a tile (base on a vector map) including way-points, including the tile border
- show a single way in a tile (base on a vector map) including way-points
- show approaches of marker points to ways as the basis of routing



# Controls Overview

#### Menus
There are menus at the left and at the right side. Menus become visible on a tap. 
They disappear after usage and automatically after a few seconds. Some menus use submenus, other don't.
Menu buttons are usually only enabled, if the action is allowed/make sense.

#### Quick Controls
The are quick controls at the bottom for 
- zoom in
- zoom out
- toggle full screen
- toggle recording of a marker track
- toggle controls for layer transparency
  
# Views Overview
There is a dashboard on the top of the view and additionally there is a status line with some 
information visible. 

#### Dashboard
Dashboard entries are visible for 
- the recording track (red)
- the recording track segment (if there is more than one) (red)
- the selected track (blue), 
- a selected segment of the selected track, if there is more than one (blue),
- the route track (purple)

A dashboard gives information about
- segment indicator
- the length
- the height gain
- the height loss
- the duration


#### Status line
The status line provides following information:
- zoom level
- direct distance between current position and the center of the map
- remaining distance (depends on the context)
- current height/pressure
- current time
 










