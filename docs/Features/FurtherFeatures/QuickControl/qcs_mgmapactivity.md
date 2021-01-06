<small><small>[Back to Index](../../../index.md)</small></small>

## Further Features: quick controls of MGMapActivity (main activity)

The MGMapActivity (main activity) provides seven quick control buttons, where each of them represents a menu button.
Whenever you tap on of them, the corresponding submenu will be shown - so you will see a set of action buttons instead.
If you don't use one of them, the buttons switch back after 3 seconds to the normal menu. If you tap on an action button,
then the corresponding action is executed and the buttons switch also back to the menu. These new quick controls
allow the full control with single tap actions. All submenus with action buttons provide on the
left side a help action button. If you tap on this button, you'll get an explanation for all button and what happens, if
you tap on it.

While the menu buttons rather represent the current state, the action button express what happens, if you tap them.
So e.g. the menu for "recording" actions shows a red circle, if GPS is switched on. The action button to toggle this
behaviour shows a red circle, when the GPS is switched off and you can switch it on with a tap on this button.

#### Main menu quick controls

J
<table style="font-size: x-small; padding:2px">
<tr>
	<th></th>
    <th style="text-align:center">Menu Task</th>
    <th style="text-align:center">Menu Search</th>
    <th style="text-align:center">Menu Marker</th>
    <th style="text-align:center">Menu Bounding box</th>
    <th style="text-align:center">Menu Record</th>
    <th style="text-align:center">Menu Show/Hide</th>
    <th style="text-align:center">Menu Multi</th>
</tr>
<tr>
<td width="6%" style="min-width:50px;max-width:60px">Menu<br/>icon</td>
<td width="12%" style="text-align:center; min-width:90px"><img src="../../../icons/group_task.svg" width="60px" height="60px"></td>
<td width="12%" style="text-align:center; min-width:90px"><img src="../../../icons/group_search1.svg" width="60px" height="60px"></td>
<td width="12%" style="text-align:center; min-width:90px"><img src="../../../icons/group_marker1.svg" width="60px" height="60px"></td>
<td width="12%" style="text-align:center; min-width:90px"><img src="../../../icons/group_bbox1.svg" width="60px" height="60px"></td>
<td width="12%" style="text-align:center; min-width:90px"><img src="../../../icons/group_record1.svg" width="60px" height="60px"></td>
<td width="12%" style="text-align:center; min-width:90px"><img src="../../../icons/show_hide.svg" width="60px" height="60px"></td>
<td width="12%" style="text-align:center; min-width:90px"><img src="../../../icons/multi.svg" width="60px" height="60px"></td>
</tr>
<tr>
	<td style="min-width:50px;max-width:60px;padding:5px">Menu<br/>functions</td>
    <td style="padding:5px;vertical-align:top"><ul style="padding:5px;"><li>settings</li><li>further settings</li><li>statistics</li><li>height profile</li><li>download</li><li>themes</li></ul></td>
    <td style="padding:5px;vertical-align:top"><ul style="padding:5px"><li>toggle search mode</li><li>toggle show search result mode</li></ul></td>
    <td style="padding:5px;vertical-align:top"><ul style="padding:5px"><li>toggle marker edit mode</li><li>toggle turning instruction mode</li><li>map matching action</li></ul></td>
    <td style="padding:5px;vertical-align:top"><ul style="padding:5px"><li>load from bbox</li><li>toggle bbox mode</li><li>load all tiles for tilestore</li><li>load missing tiles for tilestore</li><li>delete all tiles from tilestore</li></ul></td>
    <td style="padding:5px;vertical-align:top"><ul style="padding:5px"><li>toggle center mode</li><li>toggle GPS mode</li><li>toggle record track mode</li><li>toggle record track segment mode</li></ul></td>
    <td style="padding:5px;vertical-align:top"><ul style="padding:5px"><li>toggle layer transparency controls</li><li>toggle track transparency controls</li><li>hide selected track</li><li>hide not selected tracks</li><li>hide all tracks</li><li>hide marker/route track</li></ul></td>
    <td style="padding:5px;vertical-align:top"><ul style="padding:5px"><li>leave the app</li><li>toggle fullscreen mode</li><li>zoom in</li><li>zoom out</li><li>lunch Android homescreen</li></ul></td>
</tr>
<tr>
<td  style="padding:5px;vertical-align:top">Menu<br/>states</td>

<td></td>
<td style="padding:5px;vertical-align:top"><ul style="padding:5px">
	<li>search mode<br/><img src="../../../icons/group_search1.svg" width="48px" height="32px">&nbsp;<img src="../../../icons/group_search2.svg" width="48px" height="32px"></li>
	<li>show search result mode<br/><img src="../../../icons/group_search1.svg" width="48px" height="32px">&nbsp;<img src="../../../icons/group_search3.svg" width="48px" height="32px"></li>
</ul></td>
<td style="padding:5px;vertical-align:top"><ul style="padding:5px">
	<li>marker edit mode<br/><img src="../../../icons/group_marker1.svg" width="48px" height="32px">&nbsp;<img src="../../../icons/group_marker2.svg" width="48px" height="32px"></li>
	<li>turning instruction mode<br/><img src="../../../icons/group_marker1.svg" width="48px" height="32px">&nbsp;<img src="../../../icons/group_marker3.svg" width="48px" height="32px"></li>
</ul></td>
<td style="padding:5px;vertical-align:top"><ul style="padding:5px">
	<li>bbox mode<br/><img src="../../../icons/group_bbox1.svg" width="48px" height="32px">&nbsp;<img src="../../../icons/group_bbox2.svg" width="48px" height="32px"></li>
</ul></td>
<td> style="padding:5px;vertical-align:top"<ul style="padding:5px">
	<li>GPS mode<br/>(GPS or track recording or track segment recording)<br/><img src="../../../icons/group_record1.svg" width="48px" height="32px">&nbsp;<img src="../../../icons/group_record2.svg" width="48px" height="32px"></li>
</ul></td>
<td></td>
<td></td>
</tr>
 </table>

#### Menu Task

<table style="font-size: small">
<tr>
	<th></th>
    <th style="text-align:center">Help</th>
    <th style="text-align:center">Settings</th>
    <th style="text-align:center">Further settings</th>
    <th style="text-align:center">Statistic</th>
    <th style="text-align:center">Height profile</th>
    <th style="text-align:center">Download</th>
    <th style="text-align:center">Themes</th>
</tr>
<tr>
<td width="5%" style="min-width:50px;max-width:60px">Action<br/>icon</td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/help.svg" width="60px" height="60px"></td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/settings.svg" width="60px" height="60px"></td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/settings_fu.svg" width="60px" height="60px"></td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/statistik.svg" width="60px" height="60px"></td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/height_profile.svg" width="60px" height="60px"></td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/download.svg" width="60px" height="60px"></td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/themes.svg" width="60px" height="60px"></td>
</tr>
<tr>
	<td style="min-width:50px;max-width:60px">Action<br/>functions</td>
    <td>Provide help</td>
    <td>Start settings activity with main preference screen</td>
    <td>Start settings activity with further preference screen</td>
    <td>Start statistic activity</td>
    <td>Start height profile activity</td>
    <td>Start settings activity with download preference screen</td>
    <td>Start theme settings activity</td>
</tr>
</table>


#### Menu Search

<table style="font-size: small">
<tr>
	<th></th>
    <th style="text-align:center">Help</th>
    <th style="text-align:center">Search mode</th>
    <th style="text-align:center">Show search result mode</th>
    <th style="text-align:center"></th>
    <th style="text-align:center"></th>
    <th style="text-align:center"></th>
    <th style="text-align:center"></th>
</tr>
<tr>
<td width="5%" style="min-width:50px;max-width:60px">Action<br/>icon</td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/help.svg" width="60px" height="60px"></td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/search.svg" width="60px" height="60px"></td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/search_res1.svg" width="60px" height="60px"></td>
<td width="12%" style="text-align:center; min-width:100px"> </td>
<td width="12%" style="text-align:center; min-width:100px"> </td>
<td width="12%" style="text-align:center; min-width:100px"> </td>
<td width="12%" style="text-align:center; min-width:100px"> </td>
</tr>
<tr>
	<td style="min-width:50px;max-width:60px">Action<br/>functions</td>
    <td>Provide help</td>
    <td>
		<table style="padding:0px;border:none">
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:32px" src="../../../icons/search1b.svg" width="32px" height="32px"></td><td width="60%" style="padding-top:5px;border:none"><small>Switch on</small></td></tr>
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:32px" src="../../../icons/search.svg" width="32px" height="32px"></td><td width="60%" style="padding-top:5px;border:none"><small>Switch off</small></td></tr>
		</table>
	</td>
    <td>
		<table style="padding:0px;border:none">
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:32px" src="../../../icons/search_res2.svg" width="32px" height="32px"></td><td width="60%" style="padding-top:5px;border:none"><small>Switch on</small></td></tr>
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:32px" src="../../../icons/search_res1.svg" width="32px" height="32px"></td><td width="60%" style="padding-top:5px;border:none"><small>Switch off</small></td></tr>
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:32px" src="../../../icons/search_res3.svg" width="32px" height="32px"></td><td width="60%" style="padding-top:5px;border:none"><small>Disabled</small></td></tr>
		</table>
	</td>
    <td></td>
    <td></td>
    <td></td>
    <td></td>
</tr>
</table>


#### Menu Marker

<table style="font-size: small">
<tr>
	<th></th>
    <th style="text-align:center">Help</th>
    <th style="text-align:center"> </th>
    <th style="text-align:center">Marker edit mode</th>
    <th style="text-align:center">Routing Hint mode</th>
    <th style="text-align:center"></th>
    <th style="text-align:center">Map matching</th>
    <th style="text-align:center"></th>
</tr>
<tr>
<td width="5%" style="min-width:50px;max-width:60px">Action<br/>icon</td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/help.svg" width="60px" height="60px"></td>
<td width="12%" style="text-align:center; min-width:100px"> </td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/mtlr.svg" width="60px" height="60px"></td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/routing_hints1.svg" width="60px" height="60px"></td>
<td width="12%" style="text-align:center; min-width:100px"> </td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/matching.svg" width="60px" height="60px"></td>
<td width="12%" style="text-align:center; min-width:100px"> </td>
</tr>
<tr>
	<td style="min-width:50px;max-width:60px">Action<br/>functions</td>
    <td>Provide help</td>
    <td></td>
    <td>
		<table style="padding:0px;border:none">
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:32px" src="../../../icons/mtlr2.svg" width="32px" height="32px"></td><td width="60%" style="padding-top:5px;border:none"><small>Switch on</small></td></tr>
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:32px" src="../../../icons/mtlr.svg" width="32px" height="32px"></td><td width="60%" style="padding-top:5px;border:none"><small>Switch off</small></td></tr>
		</table>
	</td>
    <td>
		<table style="padding:0px;border:none">
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:32px" src="../../../icons/routing_hints2.svg" width="32px" height="32px"></td><td width="60%" style="padding-top:5px;border:none"><small>Switch on</small></td></tr>
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:32px" src="../../../icons/routing_hints1.svg" width="32px" height="32px"></td><td width="60%" style="padding-top:5px;border:none"><small>Switch off</small></td></tr>
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:32px" src="../../../icons/routing_hints_dis.svg" width="32px" height="32px"></td><td width="60%" style="padding-top:5px;border:none"><small>Disabled</small></td></tr>
		</table>
	</td>
    <td></td>
    <td>
		<table style="padding:0px;border:none">
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:32px" src="../../../icons/matching.svg" width="32px" height="32px"></td><td width="60%" style="padding-top:5px;border:none"><small>Trigger "map matching" </small></td></tr>
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:32px" src="../../../icons/matching_dis.svg" width="32px" height="32px"></td><td width="60%" style="padding-top:5px;border:none"><small>Disabled</small></td></tr>
		</table>
	</td>
    <td></td>
</tr>
</table>


#### Menu Bounding box (bbox)

<table style="font-size: small">
<tr>
	<th></th>
    <th style="text-align:center">Help</th>
    <th style="text-align:center"> </th>
    <th style="text-align:center">Load from bbox</th>
    <th style="text-align:center">bbox edit mode</th>
    <th style="text-align:center">Load missing tiles to tilestore</th>
    <th style="text-align:center">Load all tiles to tilestore</th>
    <th style="text-align:center">Delete all tiles from tilestore</th>
</tr>
<tr>
<td width="5%" style="min-width:50px;max-width:60px">Action<br/>icon</td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/help.svg" width="60px" height="60px"></td>
<td width="12%" style="text-align:center; min-width:100px"> </td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/load_from_bb.svg" width="60px" height="60px"></td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/bbox.svg" width="60px" height="60px"></td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/bb_ts_load_remain.svg" width="60px" height="60px"></td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/bb_ts_load_all.svg" width="60px" height="60px"></td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/bb_ts_delete_all.svg" width="60px" height="60px"></td>
</tr>
<tr>
	<td style="min-width:50px;max-width:60px">Action<br/>functions</td>
    <td>Provide help</td>
    <td></td>
    <td>
		<table style="padding:0px;border:none">
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:32px" src="../../../icons/load_from_bb.svg" width="32px" height="32px"></td><td width="60%" style="padding-top:5px;border:none"><small>Load tracks by bbox</small></td></tr>
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:32px" src="../../../icons/load_from_bb_dis.svg" width="32px" height="32px"></td><td width="60%" style="padding-top:5px;border:none"><small>Diasabled</small></td></tr>
		</table>
	</td>
    <td>
		<table style="padding:0px;border:none">
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:32px" src="../../../icons/bbox2.svg" width="32px" height="40px"></td><td width="60%" style="padding-top:5px;border:none"><small>Switch on</small></td></tr>
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:32px" src="../../../icons/bbox.svg" width="32px" height="32px"></td><td width="60%" style="padding-top:5px;border:none"><small>Switch off</small></td></tr>
		</table>
	</td>
    <td>
		<table style="padding:0px;border:none">
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:32px" src="../../../icons/bb_ts_load_remain.svg" width="32px" height="32px"></td><td width="60%" style="padding-top:5px;border:none"><small>Load missing tiles by bbox</small></td></tr>
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:32px" src="../../../icons/bb_ts_load_remain_dis.svg" width="32px" height="32px"></td><td width="60%" style="padding-top:5px;border:none"><small>Diasabled</small></td></tr>
		</table>
	</td>
    <td>
		<table style="padding:0px;border:none">
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:32px" src="../../../icons/bb_ts_load_all.svg" width="32px" height="32px"></td><td width="60%" style="padding-top:5px;border:none"><small>Load all tiles by bbox</small></td></tr>
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:32px" src="../../../icons/bb_ts_load_all_dis.svg" width="32px" height="32px"></td><td width="60%" style="padding-top:5px;border:none"><small>Diasabled</small></td></tr>
		</table>
	</td>
    <td>
		<table style="padding:0px;border:none">
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:32px" src="../../../icons/bb_ts_delete_all.svg" width="32px" height="32px"></td><td width="60%" style="padding-top:5px;border:none"><small>Delete all tiles by bbox</small></td></tr>
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:32px" src="../../../icons/bb_ts_delete_all_dis.svg" width="32px" height="32px"></td><td width="60%" style="padding-top:5px;border:none"><small>Diasabled</small></td></tr>
		</table>
	</td>
</tr>
</table>


#### Menu Record

<table style="font-size: small">
<tr>
	<th></th>
    <th style="text-align:center">Help</th>
    <th style="text-align:center"> </th>
    <th style="text-align:center">Center mode</th>
    <th style="text-align:center">GPS mode</th>
    <th style="text-align:center">Record track mode</th>
    <th style="text-align:center">Record track segment mode</th>
    <th style="text-align:center"></th>
</tr>
<tr>
<td width="5%" style="min-width:50px;max-width:60px">Action<br/>icon</td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/help.svg" width="60px" height="60px"></td>
<td width="12%" style="text-align:center; min-width:100px"> </td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/center1.svg" width="60px" height="60px"></td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/gps1.svg" width="60px" height="60px"></td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/record_track1.svg" width="60px" height="60px"></td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/record_segment1.svg" width="60px" height="60px"></td>
<td width="12%" style="text-align:center; min-width:100px"> </td>
</tr>
<tr>
	<td style="min-width:50px;max-width:60px">Action<br/>functions</td>
    <td>Provide help</td>
    <td></td>
    <td>
		<table style="padding:0px;border:none">
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:32px" src="../../../icons/center2.svg" width="32px" height="32px"></td><td width="60%" style="padding-top:5px;border:none"><small>Switch on</small></td></tr>
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:32px" src="../../../icons/center1.svg" width="32px" height="32px"></td><td width="60%" style="padding-top:5px;border:none"><small>Switch off</small></td></tr>
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:32px" src="../../../icons/center_dis.svg" width="32px" height="32px"></td><td width="60%" style="padding-top:5px;border:none"><small>Disabled</small></td></tr>
		</table>
	</td>
    <td>
		<table style="padding:0px;border:none">
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:32px" src="../../../icons/gps1.svg" width="32px" height="32px"></td><td width="60%" style="padding-top:5px;border:none"><small>Switch on</small></td></tr>
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:32px" src="../../../icons/gps2.svg" width="32px" height="32px"></td><td width="60%" style="padding-top:5px;border:none"><small>Switch off</small></td></tr>
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:32px" src="../../../icons/gps_dis.svg" width="32px" height="32px"></td><td width="60%" style="padding-top:5px;border:none"><small>Disabled</small></td></tr>
		</table>
	</td>
    <td>
		<table style="padding:0px;border:none">
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:32px" src="../../../icons/record_track1.svg" width="32px" height="32px"></td><td width="60%" style="padding-top:5px;border:none"><small>Switch on</small></td></tr>
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:32px" src="../../../icons/record_track2.svg" width="32px" height="32px"></td><td width="60%" style="padding-top:5px;border:none"><small>Switch off</small></td></tr>
		</table>
	</td>
    <td>
		<table style="padding:0px;border:none">
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:32px" src="../../../icons/record_segment1.svg" width="32px" height="32px"></td><td width="60%" style="padding-top:5px;border:none"><small>Switch on</small></td></tr>
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:32px" src="../../../icons/record_segment2.svg" width="32px" height="32px"></td><td width="60%" style="padding-top:5px;border:none"><small>Switch off</small></td></tr>
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:32px" src="../../../icons/record_segment_dis.svg" width="32px" height="32px"></td><td width="60%" style="padding-top:5px;border:none"><small>Disabled</small></td></tr>
		</table>
	</td>
    <td></td>
</tr>
</table>

#### Menu Show/Hide

<table style="font-size: small">
<tr>
	<th></th>
    <th style="text-align:center">Help</th>
    <th style="text-align:center">Layer transparency controls</th>
    <th style="text-align:center">Track transparency controls</th>
    <th style="text-align:center">Hide selected track</th>
    <th style="text-align:center">Hide not selected tracks</th>
    <th style="text-align:center">Hide all tracks</th>
    <th style="text-align:center">Hide marker/route track</th>
</tr>

<tr>
<td width="5%" style="min-width:50px;max-width:60px">Action<br/>icon</td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/help.svg" width="60px" height="60px"></td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/slider_layer1.svg" width="60px" height="60px"></td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/slider_track1.svg" width="60px" height="60px"></td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/hide_stl.svg" width="60px" height="60px"></td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/hide_atl.svg" width="60px" height="60px"></td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/hide_all.svg" width="60px" height="60px"></td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/hide_mtl.svg" width="60px" height="60px"></td>
</tr>
<tr>
	<td style="min-width:50px;max-width:60px">Action<br/>functions</td>
    <td>Provide help</td>
	<td>
		<table style="padding:0px;border:none">
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:32px" src="../../../icons/slider_layer2.svg" width="32px" height="32px"></td><td width="60%" style="padding-top:5px;border:none"><small>Switch on</small></td></tr>
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:32px" src="../../../icons/slider_layer1.svg" width="32px" height="32px"></td><td width="60%" style="padding-top:5px;border:none"><small>Switch off</small></td></tr>
		</table>
    <td>
		<table style="padding:0px;border:none">
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:32px" src="../../../icons/slider_track2.svg" width="32px" height="32px"></td><td width="60%" style="padding-top:5px;border:none"><small>Switch on</small></td></tr>
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:32px" src="../../../icons/slider_track1.svg" width="32px" height="32px"></td><td width="60%" style="padding-top:5px;border:none"><small>Switch off</small></td></tr>
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:32px" src="../../../icons/slider_track_dis.svg" width="32px" height="32px"></td><td width="60%" style="padding-top:5px;border:none"><small>Disabled</small></td></tr>
		</table>
	</td>
    <td>
		<table style="padding:0px;border:none">
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:32px" src="../../../icons/hide_stl.svg" width="32px" height="32px"></td><td width="60%" style="padding-top:5px;border:none"><small>Hide selected track</small></td></tr>
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:32px" src="../../../icons/hide_stl_dis.svg" width="32px" height="32px"></td><td width="60%" style="padding-top:5px;border:none"><small>Disabled</small></td></tr>
		</table>
	</td>
    <td>
		<table style="padding:0px;border:none">
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:32px" src="../../../icons/hide_atl.svg" width="32px" height="32px"></td><td width="60%" style="padding-top:5px;border:none"><small>Hide not selected tracks</small></td></tr>
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:32px" src="../../../icons/hide_atl_dis.svg" width="32px" height="32px"></td><td width="60%" style="padding-top:5px;border:none"><small>Disabled</small></td></tr>
		</table>
	</td>
    <td>
		<table style="padding:0px;border:none">
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:32px" src="../../../icons/hide_all.svg" width="32px" height="32px"></td><td width="60%" style="padding-top:5px;border:none"><small>Hide all tracks</small></td></tr>
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:32px" src="../../../icons/hide_all_dis.svg" width="32px" height="32px"></td><td width="60%" style="padding-top:5px;border:none"><small>Disabled</small></td></tr>
		</table>
	</td>
    <td>
		<table style="padding:0px;border:none">
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:32px" src="../../../icons/hide_mtl.svg" width="32px" height="32px"></td><td width="60%" style="padding-top:5px;border:none"><small>Hide marker/route track</small></td></tr>
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:32px" src="../../../icons/hide_mtl_dis.svg" width="32px" height="32px"></td><td width="60%" style="padding-top:5px;border:none"><small>Disabled</small></td></tr>
		</table>
	</td>
</tr>
</table>

#### Menu Multi

<table style="font-size: small">
<tr>
	<th></th>
    <th style="text-align:center">Help</th>
    <th style="text-align:center">Exit</th>
    <th style="text-align:center"> </th>
    <th style="text-align:center">Fullscreen mode</th>
    <th style="text-align:center">Zoom in</th>
    <th style="text-align:center">Zoom out</th>
    <th style="text-align:center">Home</th>
</tr>
<tr>
<td width="5%" style="min-width:50px;max-width:60px">Action<br/>icon</td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/help.svg" width="60px" height="60px"></td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/exit.svg" width="60px" height="60px"></td>
<td width="12%" style="text-align:center; min-width:100px"> </td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/fullscreen.svg" width="60px" height="60px"></td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/zoom_in.svg" width="60px" height="60px"></td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/zoom_out.svg" width="60px" height="60px"></td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/home.svg" width="60px" height="60px"></td>
</tr>
<tr>
	<td style="min-width:50px;max-width:60px">Action<br/>functions</td>
    <td>Provide help</td>
    <td>Exit: Leave the app</td>
    <td> </td>
    <td>Toggle fullscreen mode</td>
    <td>Zoom in</td>
    <td>Zoom out</td>
    <td>Lunch Android homescreen</td>
</tr>
</table>




 <small><small>[Back to Index](../../../index.md)</small></small>

