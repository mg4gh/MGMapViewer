<small><small>[Back to Index](../../../index.md)</small></small>

## Synchronisation Features: SSH Synchronisation

This synchronization is an automatic feature, there is even no option to do any manual action.

### SSH Synchronization

This feature allows an automatic backup of the gpx files. These might be created due to
- the recording of a new track,
- a save action of an imported track or
- a save action of a route track.

To use this feature you need a computer with a running SSH server. A good option is a raspberry pi that
is permanently running. There should be an extra user established on this server and SSH with automatic login based
on a keyfile. This user should have limited access rights, just enough to save the files to a local directory.

The synchronization is triggered, if
- recording of an track is finished,
- a track is saved from the statistic view and
- about one minute after starting the main view.

The synchronization takes only place, if the smartphone is connected to the configured WLAN.
So as long as there is no config, this condition will never be matched.

If the device is connected to the configured WLAN, then it will verify which gpx files are already on the ssh server available
and which gpx files exists locally. Beside the filename also the last modified timestamp will be compared to make sure that also
updated files will be saved.
The target location is the path \<users home\>/\<targetPrefix\> which might also point as a symbolic link to any place on that machine.

### Configuration

This section shows the sample content from the configuration file of this feature.
```
pkFile=id_rsa
hostname=192.168.178.181
port=22
username=pi
targetPrefix=mgmap_test/
wlanSSID="AndroidWifi"
```
This configuration file has to be named "sync.cfg" in the MGMapViewer/config folder of the device. The keyfile with the
private key will be placed in the MGMapViewer/config/token subdirectory, where the name has to match the configured value (e.g. id_rsa).



<small><small>[Back to Index](../../../index.md)</small></small>