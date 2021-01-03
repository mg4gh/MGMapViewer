<small><small>[Back to Index](../index.md)</small></small>

# Getting Started: Installation and first Usage of MGMapViewer

1. Copy apk to the device.<br/>
Either take a prebuild apk from the directory [./MGMapViewer/apk](https://github.com/mg4gh/MGMapViewer/tree/master/apk) of this project or download the source code
and use AndroidStudio to build the whole project. Once you decide to take the debug version, you have to update always with the debug version. Vice versa, if you take the
release version, you need always this version for updates. A change between debug and release is only possible after an uninstall of the app!
2. Select the file in the file explorer and just tap on it.
Make sure that the permission for local app installation is given to the file explorer.
3. Just start the app.
Don't be disappointed, since you'll see only an almost empty screen, 
except the menu quick controls and the status line with very few information.

   <img src="./background.png" width="200" />&nbsp;

4. Storage location: The main storage location of this app is  
  `/<sdcard>/Andorid/data/mg.mgmap/files/MGMapViewer`  
  where \<sdcard> is not necessarily a real sdcard. It's rather the default external storage location. Often the path is "/storage/emulated/0".
  Sometimes the term "internal storage" is used. Keep in mind 
  that the uninstall of the app deletes all data!  
  Remember the storage location, you might need it. Blame Google, if you don't like it :-)

5. The app creates below the MGMapViewer directory new subdirectories:
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

5. In the typical usage scenario you provide a map from [openandromaps](https://www.openandromaps.org/).
   Either download the map manually and put it (unzipped) in the ./MGMapViewer/maps/mapsforge directory.
   Alternatively  open via <span style="color:gray">*Menu | Settings and more | Download | Germany*</span>
   the download webpage of openandromaps for germany.

   <img src="./Menu_Settings.png" width="200" />&nbsp;
   <img src="./settings_screen_download.png" width="200" />&nbsp;
   <img src="./download_screen_deutschland.png" width="200" />&nbsp;

   If you want to download e.g. the map of Berlin, then press the "+" button in front
   of "Berlin", now select the  <span style="color:gray">*Install others*</span> entry with the prefix
   <span style="color:gray">*Android mf-V4-map*</span>. This will trigger the download process. If the download
   takes some time, the progress can be observed via the corresponding notifications.
   
   <img src="./download1.png" width="200" />&nbsp;
   <img src="./download2.png" width="200" />&nbsp;

   Then select the downloaded map via  
   <span style="color:gray">*Menu | Settings and more | Select map layers| Select map layer 2*</span>

   <img src="./Menu_Settings.png" width="190" />&nbsp;
   <img src="./settings_screen_selectMap.png" width="190" />&nbsp;
   <img src="./select_map_layers_2.png" width="190" />&nbsp;
   <img src="./selectMap2.png" width="190" />

   Now use twice the <span style="color:gray">*Back*</span> button and you'll see the first map.

   <img src="./select_map_layers_back.png" width="200" />&nbsp;
   <img src="./settings_screen_back.png" width="200" />&nbsp;
   <img src="./berlin_map.png" width="200" />&nbsp;


6. Additionally you have to provide a theme, which you also get via [openandromaps](https://www.openandromaps.org/).
   Download e.g. [elevate theme](https://www.openandromaps.org/wp-content/users/tobias/Elevate.zip), unzip it and
   put it into the ./MGMapViewer/themes/ directory.  
   Alternatively  open via <span style="color:gray">*Menu | Settings and more | Download | Download Elevate Theme*</span>
   the theme download webpage of openandromaps. Scroll to the Elevate 4 section and select the entry
   <span style="color:gray">*Standard Karten App*</span> entry.
   
   <img src="./download3.png" width="200" />&nbsp;

   The "elevate.xml" is already registered as the standard theme file. If you want to select another theme,
    you can do it via  <span style="color:gray">*Menu | Settings and more | Select theme*</span>.


7. Finally go to <span style="color:gray">*Menu | Themes*</span>. Now click on the current theme to get a
  selection of the main themes. Select the most suitable theme for you.  
  Hint: If the menu entries are not visible, restart your app once.  
  &nbsp;  
     <img src="./Menu_Themes.png" width="200" />&nbsp;
     <img src="./themes.png" width="200" />&nbsp;
     <img src="./themes2.png" width="200" />&nbsp;

8. Power Saving: If you are using track recording on Android&nbsp;10, then it is recommended to switch on the option "Ausgenommen vom Energiesparen" for this app.
    On a LG device with Android&nbsp;10 you can reach this Option via
      - "Einstellungen / Akku / Ausgenommen vom Energiesparen" or
      - "Einstellungen / Apps & Benachrichtigungen / Besonderer Zugang / Ausgenommen vom Energiesparen".

    Otherwise it might happen, that Android is killing the
    background service and the track recording will only continue after the next usage of the app.

Congratulations!

<small><small>[Back to Index](../index.md)</small></small>
