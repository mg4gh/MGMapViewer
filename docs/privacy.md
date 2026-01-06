<small><small>[Back to Index](./index.md)</small></small>

# Privacy

This app can be used in two flavors, one flavor ("soft4mg") is intended to be used via the Google play store.
The second flavor ("mg4gh") reflects the legacy usage with apk installation via github account mg4gh.
Especially for easy update installations this flavor needs additional permissions.

## Permissions

| Permission            | flavor | justification                                                                                                                                                                                                                                                                                                                                                                                                            |
|-----------------------|--------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION | all    | Requested for track recording and for visualization of current position (GPS without recording)                                                                                                                                                                                                                                                                                                                          |
| ACCESS_BACKGROUND_LOCATION | all    | Requested for track recording (recording continues while using other apps or having the device in your pocket)                                                                                                                                                                                                                                                                                                           |
| FOREGROUND_SERVICE, FOREGROUND_SERVICE_LOCATION, FOREGROUND_SERVICE_DATA_SYNC  | all    | The FOREGROUND_SERVICE permission is necessary for each service, that should work continuously, even if the app is not running in foreground. Since Android 14 it is furthermore required to declare the type of the service and to request the corresponding permission. For the TrackLoggerService this is obviously the FOREGROUND_SERVICE_LOCATION, while the BgJobService that enables multiple download and upload scenarios requires the permission FOREGROUND_SERVICE_DATA_SYNC. |
| ACCESS_WIFI_STATE  | all    | This permission is used to allow backup of tracks on a local SSH server. This becomes only active, if you provide a suiteble configuration for this purpose.                                                                                                                                                                                                                                                             |
| POST_NOTIFICATIONS  | all    | While running any backgroud service this is made tranparant by a corresponding notification (This can be the location service or a background service to download any map data).                                                                                                                                                                                                                                         |
| INTERNET  | all    | While all the main functionality of this app is designed to work offline, you need internet access for some features, like initial map download or to use some geocode providers. Details about the data usage via internet can be found in the next section.                                                                                                                                                            |
| REQUEST_INSTALL_PACKAGES  | mg4gh  | This permission is used only in the mg4gh flavor, not in the soft4mg flavor (play store variant). It is needed to enable an easy updata procedure of the app. In the mg4gh flavor there is a menu item allowing to update to the latest version. Once selected, the latest apk will be downloaded and the install of this apk will be triggered. So there is no other utility/app necessary for the update installation. |
| REQUEST_IGNORE_BATTERY_OPTIMIZATIONS | all | Once the TrackLoggerService is started, the app tries to suggest the user to switch off battery optimizations for the app (since battery optimizations means to stop continuously work of the services). This action requires the REQUEST_IGNORE_BATTERY_OPTIMIZATIONS permission. |
 
## Data usage

This app tries to respect your privacy as much as possible. There is **no advertising**. During "normal" operation no data is transferred to any server.
But there are a few features that need or offer internet access to different servers to match their functionality. So please check these feature with internet connectivity:

#### [mapsforge](./Features/MainMapFeatures/Mapsforge/mapsforge.md)
Once a mapsforge map is available in your device this main feature for map visualisation doesn't need or execute any internet access. But how to get a map to the correct directory? 
For this purpose the map download feature can be used as described in the [Getting Started](./GettingStarted/GettingStarted.md). This download procedure opens a browser window to the domain
**www.openandromaps.org**. By pressing the "Install others" button, a url with the protocol schema *mf-v4-map* is opened and since this app has registered for this protocol schema, this 
app will be called with the url for the map download. The map download is based on the domain **ftp.gwdg.de**, which is an ftp server provided by the 
"Gesellschaft für wissenschaftliche Datenverarbeitung mbH Göttingen" (a part of the university of Göttingen).
#### [Maponline](./Features/MainMapFeatures/MapOnline/maponline.md)
As the name of this feature suggests this feature is to visualize online maps that consists of a set of tiles. 
For each such map you need to provide an config file with contains the list of used serves (as the example in the feature description). The internet access for those
online maps is part of the [mapsforge](https://github.com/mapsforge/mapsforge) library. As far as I know this library is just executing a tile download for the requested tiles. 
No further data are transferred.
#### [Mapstore](./Features/MainMapFeatures/MapStore/mapstore.md)
A mapstore can be downloaded completely offline and copied just to the path for the mapstore. But there is an additional feature that allows to download tiles
similar to the online maps, except that the downloaded tiles are stored additionally in a local mapstore, similar to a cache. So they are available for later usage independent 
on the internet access. Again the server have to be listed in a configuration file that is provided by the user. 
##### [height data](./Features/FurtherFeatures/HeightData/heightdata.md)
Similar to the mapstore download feature the [hgt grid layer](./Features/MainMapFeatures/MapGrid/hgt.md) can be used to download hgt height data files. Additionally such a height data download, 
will automatically be offered after a mapsforge map download, if corresponding height data are not yet abailable.
The data are downloaded either form this [github height data project](https://github.com/mg4gh/hgtdata) or (if not found there) 
from the domain **step.esa.int** that is provided by the European Space Agency (ESA).
#### [mapsforge themes](./Features/MainMapFeatures/MapsforgeThemes/mapsforgethemes.md)<br>
Similar to the mapsforge maps there is a download option for the mapsforge map themes. As in the feature description visible, this download procedure opens a browser window again to the domain
**www.openandromaps.org**. By pressing the "Standard Karten App" button, this app is called again based on the protocol schema *mf-theme*. The theme download is based also on the domain
  **www.openandromaps.org**.
#### [Geocode](./Features/FurtherFeatures/Geocode/geocode.md)<br>
Internet access depends on the selected search provider:
  - **POI** - This search provider is based on the poi-file that is downloaded together with a mapsforge map file. Therefore it doesn't need internet access. 
  - **Nominatim** - This search provider is based on the domain **nominatim.openstreetmap.org**. In case of a search request the search strings and positions for reverse geocoding are transferred to this server.
  - **GeoLatLong** - As this is only some visualisation of geo coordinates this provider has no internet access.
  - **Graphhopper** - This search provider is based on the domain **graphhopper.com**. In case of a search request the search strings and positions for reverse geocoding are transferred to this server. 
Be aware that an incremental search starts as soon as your search string has a at least 5 character.
  - **Pelias** - This search provider is based on the domain **api.openrouteservice.org**. In case of a search request the search strings and positions for reverse geocoding are transferred to this server.
    Be aware that an incremental search starts as soon as your search string has a at least 5 character.
#### [SshSync](Features/FurtherFeatures/SshSync/sshsync.md)<br>
This feature allows an automatic backup of your gpx files. If you provide the corresponding configuration file, then this feature starts to do its work and to sync your gpx track files to the specified server 
(typically in your local network) - in other words: as long as you do not provide a config for this, there will be no data transfer.
#### [Software update](Features/FurtherFeatures/SoftwareUpdate/softwareUpdate.md)<br>
  If you have installed the app via Googles Play Store, then the updates will follow the Googles standard procedure for updating the app.
  If you have installed the app manually via apk form the project page on the **github.com**, then there are additional options to update the app via the same project page.
#### [Share Location](Features/FurtherFeatures/ShareLoc/shareloc.md)
  This feature is about to share your location or to received a location shared by other person. To enable this feature, the app is communicating with the MgMap server. 
  User identity is implemented via email address and verified with a confirmation code. All communication with the server is encrypted on transport level with TLS v1.3. 
  Location data which shall be shared with another user is additionally end to end encrypted and will be kept on the server for at most 24 hours.


<small><small>[Back to Index](./index.md)</small></small>

