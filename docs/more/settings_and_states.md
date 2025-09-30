## Preferences and states


| preference key string | pref key id name | variable name | type | default | usage | observer | require recreate |
|---|---|---|---|---|---|---|---|
| SelectMap\[1..5] | Layers_pref_chooseMap\[1..5]_key | mapLayerKeys | String | "" | MGMapActivity; ControlComposer | - | yes |
| "alpha_"+\<layer_key> | - | prefAlpha | float | 1.0f | MGMapLayerFactory | - | no |
| PrefThemeChanged | preference_theme_changed | baseLayer | String | MGMapActivity.getRenderTheme Elevate.xml | ThemeSettings; MapViewerBase | - | yes |
| SelectTheme | preference_choose_theme_key | -  | String | "Elevate.xml" | MGMapActivity; MapViewerBase | - | yes |
| SelectSearchProvider | preference_choose_search_key | - | String | Nominatim | FSSearch | - | no |
| FSPosition.GpsOn | FSPosition_prev_GpsOn | prefGps | boolean | false | MGMapActivity; MGMapApplication; TrackLoggerService; FSBeeline; FSPosition; FSRemainings; FSRouting; FS RoutingHints; FSRecordingTrackLog | MGMapActivity; FSBeeline; FSPosition; FSRemainings; FSRouting; FSRoutingHints | no |
| FSPosition.Center | FSPosition_prev_Center | prefCenter | boolean | true | FSPosition | FSPosition | no |
| FSGrad.wayDetails | FSGrad_pref_WayDetails_key | prefWayDetails | boolean | false  | FSGraphDetails; FSRouting | - | no |
| Layers.showAlphaLayers | Layers_qc_showAlphaLayers | prefAlphaLayers | boolean | false | FSAlpha | FSAlpha |  no |
| Layers.showAlphaTracks | Layers_qc_showAlphaTracks | prefAlphaTracks | boolean | false | FSAlpha | FSAlpha |  no |

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




