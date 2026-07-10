# MgMain - server for location sharing

## Server environment

The raspberry pi is user as a central server for the location sharing feature.
The server consist out of two parts 
- a mosquitto mqtt server and 
- a logical server written in java. 

This logical server is as well as the MGMapViewer Android App a mqtt client. 
The dynamic DNS name feature from the fritz box is used to access the mosquitto server.
The mosquitto server address is:
```
ssl://kmt6pewiw8ty93su.myfritz.net:9999
```

## Mosquitto server

### file system
The installation is done on the user "pumos" (**p**i **u**ser for **mos**quitto)
The pumos home contains the directories:
- certs 
- mosquitto
- user_certs  

and the files 
- mail.properties
- MgMain-fat.jar
- pumos.sh

Additionally there is the file pumosn.service in the path ```/etc/systemd/system``` to get 
the pumos service automatically started.

The relevant static files can be found on the ```C:\data\dev\pumos``` on the development machine.
The certs directory needs to contain the crt and key files of broker, ca and server. The mosquitto directory needs
mosquitto.acl, mosquitto.conf. The directory user_certs is filled with crt files of registered users. 

Be aware that the ca.crt, client.crt und client.key have to be setup in the ```./MGMapViewer/mgmap/src/main/assets``` path.
The first two are containd in github data, but the key file is only distributed manually.

## MgMain logical server

The Java software for this logical server is placed in the module **mgmain**.
The content is used to setup the file "MgMain-fat.jar". To generate a new version of this fat jar library use the gradle task fatJar.
To do this open a terminal (inside AndroidStudio) and give the command:
```
.\gradlew.bat :mgmain:fatJar
```
Finally copy the result ```.\MGMapViewer\mgmain\build\libs\MgMain-fat.jar``` to  ```/home/pumos/``` directory.
