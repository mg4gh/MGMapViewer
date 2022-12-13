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
- Introduce a "mgmap.properties" with
  - basePath=MGMapViewer (or alternative for tests)
  - logLevel=info (or alternative) -> will be used for logcat command
    -> application may provide LogLevel
  - sharedPreferences=mpmap.mg_preferences (or alternative) // check default
- "mgmap.properties" will be read by application at startup

### Further idea:

#### Data structure on device
- device contains in "application.getExternalFilesDir(null)" which is "/sdcard/Android/data/mg.mgmap/files" a folder "testSetup" with two files:
  - config.properties: contains properties to connect to ssh server (used as sftp server)
    - username: username to be used for ssh 
    - hostname: target hostname
    - port: target port to be used
    - pkFile: private key for login without password, e.g. id_rsa (shuold be in same folder))
    - passphrase: passphrase, if key is secured
    - wifi: ssid of wifi, in which the test shall be done
    - targetPrefix: path prefix on target host that shall be used for all actions (e.g. "data")
  - id_rsa (or similar)
  and a directory with tem data
  - temp
    - testgroup.properties
    - files.properties
  
#### Data structure on testServer:
```
~<username>/<targetPrefix>/apk/<apk for fast installation>
                          /testData/testgroup<nnnn>/testgroup.properties
                                                      appDir (default MGMapViewer)
                                                      preferences (default mg.mgmap_preferences)
                                                      cleanup
                                                   /files.properties (defines the set of required files)
                                                      <localFile>=<remoteFile>  
                                                   /result.properties
```
#### Procedure during startup: 
  
- if testSetup incl testSetup/config.properties exists // only do something, if test setup exists
  - Construct Sftp class based on config.properties
  - if (currentWifi == config.properties.wifi) // only do something, if device is in test WLAN environment
    - Setup sftp connection and channel
    - iterate over ~<username>/<targetPrefix>/testData/testgroup<nnnn>/
      - if result.properties exists continue (if results exists, then do not redo test - delete results if redo is required)
      - use appDir and preferences from testgroup.properties
      - if (cleanup and addDir!=MGMapViewer) delete all in appDir




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