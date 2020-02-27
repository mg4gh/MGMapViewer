# Overview on MGMapViewer

The MGMapViewer app is first of all a viewer on maps. But it provides also several options
to deal with tracks. 

This app is based on the [mapsforge](https://github.com/mapsforge/mapsforge) library available via github. 
It provides an excellent base for vector map visualisation. There were a few contributions to the mapsforge 
project to enable straight forward implementation in this app.

## Feature overview
 
### Main map features
- mapsforge: show vector maps e.g. from [openandromaps](https://www.openandromaps.org/). 
  This includes theme selection and customization of themes
- mapstores: show maps, which are provided via an offline tile store
- maponline: show maps, which are provided via an online tile store (e.g. openstreetmap via Mapnik renderer)
- mapgrid: show a customizable grid from degree of latitude and longitude
- Overlay multiple map layers and control transparency per layer (except grid)

### Main track features:
- Record a track: recording of a track allows multiple segments
- store tracks as gpx files, but additionally store some meta data on the tracks (for faster search)
- load/show multiple tracks at the same time
- highlight one of the loaded tracks as the "selected" track. Optional color the selected track according to height gain/loss.
- change/select "selected" track with an tap action 
- mark an area (bounding box) and load all tracks through this area (search track by location)
- create a track (marker track) by tapping some points, including manipulations like moving points, 
  insert and delete points. Such a marker track can also be imported/exported. 
- simple route (track) calculation based on a marker track (shortest path).
- dashboard visualization of the most important information of the recording, the selected and the route track log.

### Further features
- select storage location between 
    - /\<sdcard>/Andorid/data/mg.mgmap/files/MGMapViewer
    - /\<sdcard>/MGMapViewer  
  where \<sdcard> is not necessarily a real sdcard. It's rather the default external storage location. Often the path is "/storage/emulated/0".
- enlarge temporary a view entry (dashboard, status line) on a tap event (becomes readable without glasses)
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



## Controls Overview

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
  
## Views Overview
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
 










