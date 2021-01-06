<small><small>[Back to Index](../index.md)</small></small>

## Getting Started: File system information


1. Storage location: The main storage location of this app is  
  `/<sdcard>/Andorid/data/mg.mgmap/files/MGMapViewer`  
  where \<sdcard> is not necessarily a real sdcard. It's rather the default external storage location. Often the path is "/storage/emulated/0".
  Sometimes the term "internal storage" is used. Keep in mind
  that the uninstall of the app deletes all data!  
  Remember the storage location, you might need it. Blame Google, if you don't like it :-)


2. The app creates below the MGMapViewer directory new subdirectories:

    - apk (to store downloaded apk)
    - config (configuration data)
      - search (search configuration data)

    - hgt (store hgt height files)
    - log (store log files)
    - maps (store map related data)
      - mapsforge (store mapsforge maps - unzipped)
      - mapstores (directory to store offline tile stores)
      - maponline (store descriptions for online tile stores)
      - mapgrid (store description files for grid map layers)

    - themes (store themes for mapsforge maps)
    - track (store track related data)
      - gpx (store tracks in .gpx format)
      - meta (store meta data of tracks - statistics and bounding box information on a set of latitude/longitude values)
      - recording (store all data of the currently recording track - enables to continue recording after app or device restart)


<small><small>[Back to Index](../index.md)</small></small>