# Internal document to manifest some ideas about app test

## Internal state

The state of the app is composed out of
- the persistent data (./MGMapViewer/**/*)
- SharedPreferences (currently default and MapViewerBase with fassade)
- in Memory state (doesn't survive restart of application)

Idea: 
- introduce the option of parallel pathes to ./MGMapViewer for test purposes - as today already done with the external sd card path
- shared preferences should always use a prefix (like MGMapViewer) - so in normal usage every persistent data belongs to this key
- use a dedicated SharedPreferences Instance to control which key will be used at startup time, changing a particular key inside would cause the application to restart with new
value for this key
- testdata shell be downloaded right at the beginning of application startup, maybe even in advance - just throw away old data and unzip expected data package


## External Interface types

### UI Interafces
- click
- long press
- double click
- drag and drop

### HTTP Requests
- new Intent from Testcase
- browseIntent (call external brower)
- HTTP request -> get input stream (tile download, mapsforge zip download, hgt file download, sw-download from pamsforge)
- DynamicHandler 
- geocoder (forward/reverse)

### sensors
- GPS (lat, loh, wgs84, acc, vert. acc)
- Barometer (pressure)

### Android system APIs
- TTS (text to speach)
- WorkManager
- Permissions API
- Install new version API

### Other external Interfaces
- ssh sync 
- ftp software download
- 










# Further Infos

#### Zipper (MapsforgeMap, ThemeDownload, actual Software apk.zip)
- based on: OkHttpClient
- Input: URL
- Output: contentLength, byteStream
#### HgtProvider
- based on: OkHttpClient
- Input: URL
- Output: contentLength, byteStream
#### TileStoreLoaderDB, TileStoreLoaderFile
- based on: java.net.URLConnection
- Input URL, timeout
- Output: inputStream, HTTP Response code
#### Graphhopper, Pelias, Nominatim
- based on: java.net.URLConnection
- Input URL
- Output: inputStream

### DynamicHandler
- based on: OkHttpClient
- ...

### Loacal SW Download
- based on FTP
- ...