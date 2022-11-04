<small><small>[Back to Index](../../../index.md)</small></small>

## Software Update

This page describes the options for software updates.

Depending on the installation you have to update this app yourself. This description is only valid as long as the installation is **not** done via an App Store.
The following options exist:
- Download and install latest apk (from github)<br>
The download of the apk is based on the [apk directory](https://github.com/mg4gh/MGMapViewer/tree/master/apk) of the github project page. If you use this option for the first time, you 
will be asked to give this app the permission to install apps. This is necessary to trigger the installation after download automatically. 
If you don't want to give this permission to the app, then use the option "Download other apk".
- Download apk from local FTP server<br>
This option is for developer to speed up the test - it allows to generate an install a new app version in a very few seconds - but it requires some additional effort 
on the developer machine. To get this option visible you need to provide a config file named *ftp_config.properties* in the *./MGMapViewer/config* path. It is expected that the client finds there an .apk file and a corresponding .sha256 file. The apk will be downloaded and the download will be verified with the .sha256 hash code.
After successful download the apk will be installed. Again the permission for this has to be granted.
  ```
  FTP_SERVER=<ip address>
  PORT=<port>
  USERNAME=<username>
  PASSWORD=<password>
  ```
  

- Download other apk
This option opens just a browser window with the path *https://github.com/mg4gh/MGMapViewer/tree/master/apk*. Then you can manually select the desired apk, download and 
install it via any browser and file manager of your choice.








<small><small>[Back to Index](../../../index.md)</small></small>