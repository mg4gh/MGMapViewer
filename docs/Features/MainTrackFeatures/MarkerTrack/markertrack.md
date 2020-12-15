<small><small>[Back to Index](../../../index.md)</small></small>

## Main Track Feature: marker track

A marker track is a sequence of marker track points. Usually marker tracks are used for a [basic routing](../Routing/routing.md).

For explanation purposes of the marker tracks we first change a few settings:
- switch off <span style="color:gray">*Menu | Settings and more | Further settings | Automatic marker settings*</span>
- switch <span style="color:gray">*Menu | Route | Route off*</span> to set marker route transparency on (and marker track transparency off).

To start the special mode for a marker track press the corresponding quick control.
As a visual feedback the icon on this button get red. This implies that the edit mode for the marker track is active.

<img src="./Quick1.png" width="200" />&nbsp;
<img src="./Quick2.png" width="200" />&nbsp;

<a id="action"> </a>The following actions exists to edit the marker track:
- tap on a free area: add a new marker point at the end of the track
- tap on a marker point: delete this marker point
- tap on the connecting line between two marker points: insert a new point between the existing points
- drag and drop of a marker point: move the position of an existing marker point

You can even combine the last two actions to insert and immediately move the inserted point.
The following example shows a creation of such a track. Marker points are visible with a small pink circle,
consecutive points are connected with a thin pink line:

<img src="./MT1.png" width="400" />&nbsp;
<img src="./MT4.png" width="400" />&nbsp;
<img src="./MT6.png" width="400" />&nbsp;

After finishing don't forget to press again the marker track quick control to switch off the marker track edit mode.

<img src="./Quick3.png" width="200" />&nbsp;

Alternatively the edit mode will be left after 15s of inactivity.

The full power of marker tracks becomes visible in conjunction with the [basic routing](../Routing/routing.md) feature.
Before starting with this feature switch back at least following settings:
- switch on <span style="color:gray">*Menu | Settings and more | Further settings | Automatic marker settings*</span>
- switch on route visibility with <span style="color:gray">*Menu | Route | Route on*</span>.

and delete the current marker track with  <span style="color:gray">*Menu | Marker Track | Delete All*</span>.


**Export Marker Track / Save Marker Track**

A marker track can be exported (saved) via
<span style="color:gray">*Menu | Marker Track | Export*</span>.

<img src="./MarkerExport1.png" width="200" />&nbsp;
<img src="./MarkerExport2.png" width="200" />&nbsp;

The export of a marker track generates a .gpx file in the directory ./MGMapViewer/track/gpx.
The filename has the structure \<date>_\<time>_MarkerTrack.gpx.

For the example above the result looks like:

```
<?xml version="1.0" encoding="UTF-8"?>
<gpx version="1.1" creator="MGMap">
	<metadata>
		<name>20201211_150111_MarkerTrack</name>
		<time>2020-12-11T15:01:11</time>
	</metadata>
	<trk>
		<name>20201211_150111_MarkerTrack</name>
		<desc>start=11.12.2020_15:01:11 duration=0:00:00 totalLength=981.31 gain=54.5 loss=0.0 minEle=340.0 maxEle=402.4 numPoints=7</desc>
		<trkseg>
			<desc>start=11.12.2020_15:01:11 duration=0:00:00 totalLength=981.31 gain=54.5 loss=0.0 minEle=340.0 maxEle=402.4 numPoints=7</desc>
			<trkpt  lat="49.428436" lon="8.776411">
				<ele>340.0</ele>
			</trkpt>
			<trkpt  lat="49.429625" lon="8.776102">
				<ele>354.8</ele>
			</trkpt>
			<trkpt  lat="49.431154" lon="8.776506">
				<ele>362.7</ele>
			</trkpt>
			<trkpt  lat="49.431980" lon="8.778239">
				<ele>382.1</ele>
			</trkpt>
			<trkpt  lat="49.432884" lon="8.779183">
				<ele>385.9</ele>
			</trkpt>
			<trkpt  lat="49.433421" lon="8.776926">
				<ele>394.5</ele>
			</trkpt>
			<trkpt  lat="49.435290" lon="8.775810">
				<ele>402.4</ele>
			</trkpt>
		</trkseg>
	</trk>
</gpx>

```

There is an alternative option to save the marker track: Open the [statistic activity](../../FurtherFeatures/Statistic/statistic.md) with <span style="color:gray">*Menu | Statistic*</span>,
select the marker track with a short tap and then use the "Save" quick control in this activity.

**Import Marker Track / Load Marker Track**

There are two options to get a gpx back as a marker track:

First option is to make the marker track the selected track and then to use
<span style="color:gray">*Menu | Marker Track | Import*</span>  as a second step.
As a side effect of the import the selected track will be hidden - so then you see only the original marker track.

As a second option you can do this similar to the export also via the [statistic activity](../../FurtherFeatures/Statistic/statistic.md).  Select the track you want to open as marker track and
then tap on the marker track quick control. This will reopen the selected gpx as marker track.

**Marker Track Options**

As we have seen already in the beginning of this chapter, there are a few settings concerning the marker track:

- Show marker track: This switch toggles the visibility of the marker track.
- Snap to way: If this option is switched on, then newly set marker points are automatically moved to the next way (as long as there is a close way).

In the context of routing we will see two main use cases:
1. setup a basic route (or modify a previously setup route)
2. load a recorded track as marker track and automatically try to correct recording inaccuracies.

So now there is a problem with the two marker track settings above: For the first routing use case the setting "show marker track" should be switched off and the setting "snap to way" should be on,
but for the second routing use case it's vice versa. To prevent the need for a manually change of these options each time, there is another setting "automatic marker settings" - which tries to distinguish between both
routing use cases and to set the marker track settings automatically to the correct value for the detected use case. The criteria for this automatic mode is related to the number of marker track points during
the load of the marker track. If this number is less then 200, then it is assumed that we are in the first use case, while a higher number of marker track points refers to the second use case.



<small><small>[Back to Index](../../../index.md)</small></small>