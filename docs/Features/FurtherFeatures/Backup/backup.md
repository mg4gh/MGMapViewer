<small><small>[Back to Index](../../../index.md)</small></small>

## Further Features: backup

The largest amount of data that is used inside the app balongs to the maps and their height data. 
If, e.g. due to a crash you loose these data - no problem, just download latest version again.

There is only one category of data that is different. This category contains your track records and your planned routes, gennerally spoken these are your gpx files.
Unfortunately its not so easy to backup all these gpx data. Therefore a two step approach with **backup latest** and **backup full** is implemented. This approach is explaind in the next two subchapters.

### Backup Latest for gpx files

#### Backup

Google provides a mechnism called "auto backup", sometime also "Google One-Backup". You should switch this on. Go to the Settings of your device and search for **backup**.
So you will find this setting. Android is taking such a backup roughly once the day.

But ... the is a big **but**. It's only possible to backup at most 25 MB of data per app. Even with compression the gpx files can easily become more than this amount.
Therefore the backup is split. One part, the **backup latest**, is explained here.

The MGMapViewer provides data to this process - based on the open map activity and some modification events of tracks, but not more often than every hour. 
First a zip file is created containing gpx files newer than the latest full backup, but at most the latest 365 gpx files. In a second step this zip file is put into another zip file with encryption. 
You can see this with the internal FileManager in the path `MGMapViewer/backup/backup/`.
This encrypted zip file is placed additionally to the folder where the Google backup mechanism takes data from.

#### Restore

If the app is uninstalled and again installed Google is restoring the backup latest zip file. From the place where Google is restoring the backup file it is copied to the folder `MGMapViewer/backup/restore/`.
Then it is automatically unpacked, so that the gpx files are available at the normal place `MGMapViewer/track/gpx`.
Theoretically this shall also work in case of a crash of your device. 
If you install the app on a new (or factory resetted) device this mechnism shoul also restore the data from the backup latest process.
Unfortunately it's not possible to test this procedure well, since virtual devices behave different and I'm not owning enough real devices.

### Backup Full for gpx files

#### Backup

The second part of the backup mechanism runs less frequently, but tries to cover all your gpx. At least every 90 days, but also with more than 300 new (after latest full backup) gpx files the backup full process is triggered.
The gpx files are put again into a zip, which again is encrypted in another zip. Then you'll get a dialog that it's time for a full backup. After pressing ok, the produced zip archive is offered for the Android share process.
It's recommended to use your Google drive as the share target. The advantage of this target is, that the data are automatically synchronized to the cloud  - so there is a backup in case of a device crash. And, since data are encryted,
there is no fear that anybody might be able to access these data.

#### Restore

To restore such a backup use again the Android share mechanism. Share the backup file with the target MGMapViewer application and place it in the folder `MGMapViewer/backup/restore/`. Restart manually the app - this will trigger the 
process to restore the gps files from this archive. If files are on the device and on the the backup, then the newer one is kept. So do no fear, that olfd files overwrite changes in a restore process.


<small><small>[Back to Index](../../../index.md)</small></small>