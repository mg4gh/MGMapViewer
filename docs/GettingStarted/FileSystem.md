<small><small>[Back to Index](../index.md)</small></small>

## Getting Started: File system information


1. Storage location: The main storage location of this app is  
   `/<sdcard>/Andorid/data/mg.mgmap/files/MGMapViewer`  
   where \<sdcard> is not necessarily a real sdcard. It's rather the default external storage location. Often the path is "/storage/emulated/0".
   Sometimes the term "internal storage" is used. Keep in mind
   that the uninstall of the app deletes all data!  
   Remember the storage location, you might need it. Blame Google, if you don't like it :-)

   With each Android version it became more difficult to access this location with a file manager on the device.
   Meanwhile (Android 13) there is no way to do it on the device. So if you want manually read out data or place a configuration file there, then you need a PC or Laptop and 
   a USB cable. Then (and only then) you can access all files and folders.


2. The app creates below the MGMapViewer directory new subdirectories:
```
 ./MGMapViewer/apk/                          // to store downloaded apk
              /config/                       // configuration data
                     /search/                // search configuration data
              /hgt/                          // store hgt height data files
              /log/                          // store log files
              /maps/                         // store map related data
                   /mapsforge/               // store mapsforge maps - unzipped
                   /mapstores/               // store offline tile stores
                   /maponline/               // store descriptions for online tile stores
                   /mapgrid/                 // store description files for grid map layers
              /themes/                       // store themes for mapsforge maps
              /track/                        // store track related data
                    /gpx                     // store tracks in .gpx format
                    /meta                    // store meta data of tracks - statistics and bounding box 
                                             //    information on a set of latitude/longitude values
                    /recording               // store all data of the currently recording track - enables 
                                             //    to continue recording after app or device restart
```

<small><small>[Back to Index](../index.md)</small></small>