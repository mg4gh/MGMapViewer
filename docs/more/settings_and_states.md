## Preferences and states


| preference key | application property name | type | init | usage | use observer | require recreate |
|---|---|---|---|---|---|---|
| SelectMap\[1..5] | - | String |  | MGMapActivity; ControlView | - | yes |
| "no_recreate_alpha_"+\<layer_key> | - | float | none | ControlView | - | no |
| SelectTheme | - | String | MGMapActivity.getRenderTheme Elevate.xml | MGMapActivity | - | yes |
| SelectSearchProvider | - | String | MSSerach.changeVisibility Nominatim | MSSearch | - | no |
| - | gpsOn | boolean | MGMapApplication.\<create> false | GpsControl; MSPosotion;  MSMotion; ... | - | no |
| - | centerCurrentPosition | boolean | MGMapApplication.\<create> true; <br/> on resume TrackRecording true  | CenterControl; MSPosotion; | + | no |
| way_details | wayDetails | boolean | MGMapApplication.\<create> false; MGMapApplication.onCreate false; MGMapActivity.onCreate from preference  | MSGraphDetails; MSRouting; | - | ? |
| - | showAlphaSliders | boolean | MGMapApplication.\<create> false; | ControlView | + |  no |
| - | editMarkerTrack | boolean | MGMapApplication.\<create> false; | ControlView; MSMarker; MSRouting | + | no |
| - | routingHints | boolean | MGMapApplication.\<create> false; | ControlView; MSRoutingHint; RoutingHintService | + | no |
| - | showRouting | boolean | MGMapApplication.\<create> true; | MSRouting; \<RoutingControls> | - | no |
| stl_gl | stlWithGL | boolean | further_preferences.xml true; MGMapApplication.\<create> true; MGMapApplication.onCreate from preference; MGMapActivity.onCreate from preference | MSAvailableTrackLogs | - |  no |
| - | fullscreen | boolean | MGMapApplication.\<create> true; | ControlView; MGMapActivity; MSSearch | + |  no |
| - | searchOn | boolean | MGMapApplication.\<create> false; | ControlView; MSSearch | + | no |
| - | bboxOn | boolean | MGMapApplication.\<create> false; | ControlView; MSBB | + | no |
| no_recreate_show_marker_track | showMarkerTrack | boolean | further_preferences.xml false; MGMapApplication.\<create> false; MGMapActivity.onResume from preference | MSMarker | - | no |
| no_recreate_snap_to_way | snapMarkerToWay | boolean | further_preferences.xml true; MGMapApplication.\<create> true; MGMapActivity.onResume from preference | MSMarker | - |  no |
| no_recreate_auto_marker_settings | - | boolean | further_preferences.xml true; | MSMarker | - | no |
| hprof_gl | - | boolean | further_preferences.xml false;  | HeightProfileActivity | - | no |
| scale | - | List\<0.7, 1.0, 1.5, 2.0> | further_preferences.xml 1.0;  | mapsforge libs | - | yes |
| scalebar | - | List\<...> | further_preferences.xml metric;  | MapViewerBase | - | yes |
| language_selection | - | List\<...> | further_preferences.xml metric;  | MapViewerBase | - | yes |




