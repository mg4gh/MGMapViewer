# Getting Started: Installation and first Usage of MGMapViewer

1. Copy apk to the device.<br/>
Either take a prebuild apk from the apk directory of this project or download the source code 
and use AndroidStudio to build the whole project.
2. Select the file in the file explorer and just tap on it.
Make sure that the permission for local app installation is given to the file explorer.
3. Just start the app.
Don't be disappointed, since you'll see only an almost empty screen, 
except the quick controls and the status line with very few information.

<img src="./background.png" width="200" />&nbsp;

 
4. Tap on the background to get the <span style="color:gray">*Menu*</span>. The menu disappears automatically after a few seconds.
Alternatively it disappears after a second tap on the background. Remember this action as it is frequently used in the next sections.

<img src="./Menu.png" width="200" />&nbsp;

5. Decide, which storage location you want to use. Currently there are two options:
  - /\<sdcard>/Andorid/data/mg.mgmap/files/MGMapViewer
  - /\<sdcard>/MGMapViewer  
  where \<sdcard> is not necessarily a real sdcard. It's rather the default external storage location. Often the path is "/storage/emulated/0".
  Sometimes the term "internal storage" is used. The preconfiguered option is the first one, because it doesn't need extra permissions. If you put your data to this location then keep in mind 
  that the uninstall of the app deletes all these data! <br/>
  If you use the second option, then your data are independent on the app Software. They will be available after deinstallation 
  (you still want to have your tracks available even if you don't use this software anymore). This requires extra
  [permissions](./permissions_media.png).  If you don't need the data anymore, you have to cleanup the directory manually.

<img src="./Menu_Settings.png" width="200" />&nbsp;
<img src="./settings_storage_settings.png" width="200" />&nbsp;
<img src="./storage_settings.png" width="200" />&nbsp;

6. The app creates below the MGMapViewer directory new subdirectories:
    - track (store track related data)
      - gpx (store tracks in .gpx format)
      - meta (store meta data of tracks - statistics and bounding box information on a set of latitude/longitude values)
      - recording (store all data of the currently recording track - enables to continue recording after app or device restart)
    - maps (store map related data)
      - mapsforge (store mapsforge maps - unzipped)
      - mapstores (directory to store offline tile stores)
      - maponline (store descriptions for online tile stores)
      - mapgrid (store description files for grid map layers)
    - themes (store themes for mapsforge maps)
    - hgt (store hgt height files)
    - log (store log files)
7. In the typical usage scenario you provide a map from [openandromaps](https://www.openandromaps.org/). 
   Put the map (unzipped) in the ./MGMapViewer/maps/mapsforge directory and select it via
   <span style="color:gray">*Menu | Settings | Select map layers| Select map layer 2*</span>

<img src="./Menu_Settings.png" width="200" />&nbsp;
<img src="./settings_select_map_layers.png" width="200" />&nbsp;
<img src="./select_map_layers_2.png" width="200" />&nbsp;
<img src="./selectMap2.png" width="200" />&nbsp;

8. Additionally you have to provide a theme, which you also get via [openandromaps](https://www.openandromaps.org/). 
   Download e.g. [elevate theme](https://www.openandromaps.org/wp-content/users/tobias/Elevate.zip), unzip it <br/> and 
   put it into the ./MGMapViewer/themes/ directory.  
   Select it via <span style="color:gray">*Menu | Settings | Select theme | Elevate.xml*</span>

<img src="./Menu_Settings.png" width="200" />&nbsp;
<img src="./settings_select_theme.png" width="200" />&nbsp;
<img src="./settings_theme.png" width="200" />&nbsp;

9. When you go back to the main activity view, then this map is visible and you can navigate in this map.
10. Finally go to <span style="color:gray">*Menu | Themes*</span>. Now click on the current theme to get a 
   selection of the main themes. Select the most suitable theme for you. 
   
<img src="./Menu_Themes.png" width="200" />&nbsp;
<img src="./themes.png" width="200" />&nbsp;
<img src="./themes2.png" width="200" />&nbsp;
 


  