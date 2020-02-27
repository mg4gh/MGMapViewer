# Getting Started: Installation and first Usage of MGMapViewer

1. Copy apk to the device.
2. Select the file in the file Exlorer and just tap on it.
Make sure that the permission for local app installation is given to the file explorer.
3. Just start the app.
Don't be disappointed, since you'll see only a [screen](./screenshot/installed_01.png) 
the quick controls and the status line with very few information. 
4. Tap on the background to get the menu. The menu disappears automatically after a few seconds.
Alternatively it disappears after a second tap on the background.
4. Decide, which storage location you want to use (Menu/[Settings](./screenshot/installed_03.png)/[Storage Settings](./screenshot/installed_02.png)). Currently there are two options:
    - /\<sdcard>/Andorid/data/mg.mgmap/files/MGMapViewer
    - /\<sdcard>/MGMapViewer  
  where \<sdcard> is not necessarily a real sdcard. It's rather the default external storage location. Often the path is "/storage/emulated/0".
  The preconfiguered option is the first one. If you put your data to this location then keep in mind that the uninstall of the app deletes all these data!
  If you use the second option, then data wil be available after deinstallation. If you don't need the data anymore, you have to cleanup the 
  directory manually.
5. The app creates a below the MGMapViewer directory new subdirectories:
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
6. In the typical usage scenario you provide a map from [openandromaps](https://www.openandromaps.org/). 
   Put the map (unzipped) in the ./MGMapViewer/maps/mapsforge directory and select it via <br/> Settings/Select map layers/Select map layer \<n>.
7. Additionally you have to provide a theme, which you also get via [openandromaps](https://www.openandromaps.org/). 
   Download e.g. [elevate theme](https://www.openandromaps.org/wp-content/users/tobias/Elevate.zip), unzip it <br/> and 
   put it into the ./MGMapViewer/themes/ directory.  Select it via Menu/[Settings](./screenshot/settings_02.png)/[Select theme](./screenshot/settings_theme.png) elevate.xml.
8. When you go back to the main activity view, then this map is visible and you can navigate in this map.
7. Finally go to menu/themes to select the most suitable theme for you. Click on the [current theme](./screenshot/themes.png) to get a 
   [selection](./screenshot/themes2.png) of the main themes.   


  