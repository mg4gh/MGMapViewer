<small><small>[Back to Index](./index.md)</small></small>

# Privacy

This app tries to respect your privacy as much as possible. During "normal" operation no data is transferred to any server.
But there are a few features that need or offer internet access to different servers to match their functionality. So please check these feature with internet connectivity:

- [mapsforge](./Features/MainMapFeatures/Mapsforge/mapsforge.md)<br>
Once a mapsforge map is available in your device this main feature for map visualisation doesn't need or execute any internet access. But how to get a map to the correct directory? 
For this purpose the map download feature can be used as described in step 4 of [Getting Started](./GettingStarted/GettingStarted.md). This download procedure opens a browser window to the domain
**www.openandromaps.org**. By pressing the "Install others" button, a url with the protocol schema *mf-v4-map* is opened and since this app has registered for this protocol schema, this 
app will be called with the url for the map download. The map download is based on the domain **ftp.gwdg.de**, which is an ftp server provided by the 
"Gesellschaft für wissenschaftliche Datenverarbeitung mbH Göttingen" (a part of the university of Göttingen).
- [Maponline](./Features/MainMapFeatures/MapOnline/maponline.md)<br>As the name of this feature suggests this feature is to visualize online maps that consists of a set of tiles. 
For each such map you need to provide an config file with the list used serves (es the example in the feature description). The internet access for those
online maps is part of the [mapsforge](https://github.com/mapsforge/mapsforge) library. It is just executing a tile download for the requested tiles.
- [Mapstore](./Features/MainMapFeatures/MapStore/mapstore.md)<br>
A mapstore can be downloaded completely offline and copied just to the path for the mapstore. But there is an additional feature that allows to download tiles
similar to the online maps, except that the downloaded tiles are stored additionally in a local mapstore, similar to a cache. So they are available for later usage independent 
on the internet access. Again the server have to be listed in a configuration file that is provided by the user. 
- [hgt grid layer](./Features/MainMapFeatures/MapGrid/hgt.md)<br>
Similar to the mapstore download feature the hgt grid layer can be used to download hgt height files. In this case there is no configuration file required. The data are downloaded
from the domain **step.esa.int** that is provided by the European Space Agency (ESA).
- [mapsforge themes](./Features/MainMapFeatures/MapsforgeThemes/mapsforgethemes.md)<br>
Similar to the mapsforge maps there is a download option for the mapsforge map themes. As in the feature description visible, this download procedure opens a browser window again to the domain
**www.openandromaps.org**. By pressing the "Standard Karten App" button, this app is called again based on the protocol schema *mf-theme*. The theme download is based also on the domain
  **www.openandromaps.org**.
- [Geocode](./Features/FurtherFeatures/Geocode/geocode.md)<br>
This feature obviously needs some data to find location based on a name or to find an entity by a location. Internet access depends on the selected search provider:
  - **POI** - This search provider is based on the poi-file that is downloaded together with a mapsforge map file. Therefore it doesn't need internet access. 
  - **Nominatim** - This search provider is based on the domain **nominatim.openstreetmap.org**. In case of a search request the search strings and positions for reverse geocoding are transferred to this server.
  - **GeoLatLong** - As this is only some visualisation of geo coordinates this provider has no internet access.
  - **Graphhopper** - This search provider is based on the domain **graphhopper.com**. In case of a search request the search strings and positions for reverse geocoding are transferred to this server. 
Be aware that an incremental search starts as soon as your search string has a at least 5 character.
  - **Pelias** - This search provider is based on the domain **api.openrouteservice.org**. In case of a search request the search strings and positions for reverse geocoding are transferred to this server.
    Be aware that an incremental search starts as soon as your search string has a at least 5 character.
- [SshSync](Features/FurtherFeatures/SshSync/sshsync.md)<br>
This feature allows an automatic backup of your gpx files. Once the corresponding configuration file will be found, then this feature starts to do its work and to sync to the specified server 
(typically in your local network) - in other words: as long as you do not provide a config for this, there will be no data transfer.
- [Software update](Features/FurtherFeatures/SoftwareUpdate/softwareUpdate.md)<br>
Depending on the selected option for software update the download is based on the project page on the **github.com** domain or based on a local ftp server.


<small><small>[Back to Index](./index.md)</small></small>

