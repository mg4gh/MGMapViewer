## Main Track Feature: track storage

Tracks will be stored in ".gpx" format in the directory "./MGMapViewer/track/gpx/". 
A real world example is [this](./20200324_102303_GPS.gpx). 

The following picture shows a subset of this:

<img src="./gpx_example.png" width="800" />

The structure with with 
\<trk>, \<trkseg> and \<trkpt> is well visible. Each track point contains information about the
recorded latitude and longitude. It contains also elevation information and a timestamp.
But there are a couple more information available, which is placed in the comment tag to
be complient to the .gpx format.

After recording of a track the corresponding .gpx file will be automatically generated and stored 
in the directory "./MGMapViewer/track/gpx/". Alternatively you can place gpx files manually in this
directory.


Beside the ".gpx" format there is a second file format ".meta", which is stored consequently in the
directory "./MGMapViewer/track/meta/". After track recording such a meta file is also generated 
automatically. But there is also a check at startup of the app, if there are gpx files without 
corresponding meta files. If so, then these meta files will be generated too.
These meta files are binary files with information about the bounding box around the track.
Additionally the track points are split in groups of 4K bytes size. For each group there is also
a bounding box info available. All these meta data provide the ability to search for a track by
marking a certain relevant area.

For detail see Feature "Load from Bounding Box"( TODO ).



