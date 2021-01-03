<small><small>[Back to Index](../../../index.md)</small></small>

## Further Features: quick controls (new version)

### MGMapActivity (main activity)

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

<table style="font-size: small">
<tr>
	<th></th>
    <th style="text-align:center">Menu Task</th>
    <th style="text-align:center">Menu Search</th>
    <th style="text-align:center">Menu Marker</th>
    <th style="text-align:center">Menu bounding box</th>
    <th style="text-align:center">Menu record</th>
    <th style="text-align:center">Menu Hide</th>
    <th style="text-align:center">Menu multi</th>
</tr>
<tr>
<td width="5%" style="min-width:50px;max-width:60px">Menu<br/>icon</td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/group_task.svg" width="75px" height="50px"></td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/group_search1.svg" width="75px" height="50px"></td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/group_marker1.svg" width="75px" height="50px"></td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/group_bbox1.svg" width="75px" height="50px"></td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/group_record1.svg" width="75px" height="50px"></td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/show_hide.svg" width="75px" height="50px"></td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/multi.svg" width="75px" height="50px"></td>
</tr>
<tr>
	<td style="min-width:50px;max-width:60px">Menu<br/>functions</td>
    <td><ul><li>settings</li><li>further settings</li><li>statistics</li><li>height profile</li><li>download</li><li>themes</li></ul></td>
    <td><ul><li>toggle search mode</li><li>toggle show search result mode</li></ul></td>
    <td><ul><li>toggle marker edit mode</li><li>toggle routing hint mode</li><li>map matching action</li></ul></td>
    <td><ul><li>load from bbox</li><li>toggle bbox mode</li><li>load all tiles for tilestore</li><li>load missing tiles for tilestore</li><li>delete all tiles from tilestore</li></ul></td>
    <td><ul><li>toggle center mode</li><li>toggle GPS mode</li><li>toggle record track mode</li><li>toggle record track segment mode</li></ul></td>
    <td>Menu Hide</td>
    <td>Menu multi (exit, zomm in, zoom out, lunch homescreen)</td>
</tr>
<tr>
<td style="min-width:50px;max-width:60px">Menu<br/>states</td>

<td></td>
<td><ul>
	<li>search mode<br/><img src="../../../icons/group_search1.svg" width="48px" height="32px">&nbsp;<img src="../../../icons/group_search2.svg" width="48px" height="32px"></li>
	<li>show search result mode<br/><img src="../../../icons/group_search1.svg" width="48px" height="32px">&nbsp;<img src="../../../icons/group_search3.svg" width="48px" height="32px"></li>
</ul></td>
<td><ul>
	<li>marker edit mode<br/><img src="../../../icons/group_marker1.svg" width="48px" height="32px">&nbsp;<img src="../../../icons/group_marker2.svg" width="48px" height="32px"></li>
	<li>routing hint mode<img src="../../../icons/group_marker1.svg" width="48px" height="32px">&nbsp;<img src="../../../icons/group_marker3.svg" width="48px" height="32px"></li>
</ul></td>
<td><ul>
	<li>bbox mode<br/><img src="../../../icons/group_bbox1.svg" width="48px" height="32px">&nbsp;<img src="../../../icons/group_bbox2.svg" width="48px" height="32px"></li>
</ul></td>
<td><ul>
	<li>GPS mode<br/><img src="../../../icons/group_record1.svg" width="48px" height="32px">&nbsp;<img src="../../../icons/group_record2.svg" width="48px" height="32px"></li>
</ul></td>
<td></td>
<td></td>
</tr>
</table>

#### Menu task quick controls

<table style="font-size: small">
<tr>
	<th></th>
    <th style="text-align:center">Action Help</th>
    <th style="text-align:center">Action Settings</th>
    <th style="text-align:center">Action Further settings</th>
    <th style="text-align:center">Action Statistic</th>
    <th style="text-align:center">Action Height profile</th>
    <th style="text-align:center">Action Download</th>
    <th style="text-align:center">Action Themes</th>
</tr>
<tr>
<td width="5%" style="min-width:50px;max-width:60px">Menu<br/>icon</td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/help.svg" width="75px" height="50px"></td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/settings.svg" width="75px" height="50px"></td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/settings_fu.svg" width="75px" height="50px"></td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/statistik.svg" width="75px" height="50px"></td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/height_profile.svg" width="75px" height="50px"></td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/download.svg" width="75px" height="50px"></td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/themes.svg" width="75px" height="50px"></td>
</tr>
<tr>
	<td style="min-width:50px;max-width:60px">Menu<br/>functions</td>
    <td>Provide help</td>
    <td>Start settings activity with main preference screen</td>
    <td>Start settings activity with further preference screen</td>
    <td>Start statistic activity</td>
    <td>Start height profile activity</td>
    <td>Start settings activity with download preference screen</td>
    <td>Start theme settings activity</td>
</tr>
</table>


#### Menu search quick controls

<table style="font-size: small">
<tr>
	<th></th>
    <th style="text-align:center">Action Help</th>
    <th style="text-align:center">Action Toggle search mode</th>
    <th style="text-align:center">Action Toggle show search result mode</th>
    <th style="text-align:center"></th>
    <th style="text-align:center"></th>
    <th style="text-align:center"></th>
    <th style="text-align:center"></th>
</tr>
<tr>
<td width="5%" style="min-width:50px;max-width:60px">Menu<br/>icon</td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/help.svg" width="75px" height="50px"></td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/search.svg" width="75px" height="50px"></td>
<td width="12%" style="text-align:center; min-width:100px"><img src="../../../icons/search_res1.svg" width="75px" height="50px"></td>
<td width="12%" style="text-align:center; min-width:100px"> </td>
<td width="12%" style="text-align:center; min-width:100px"> </td>
<td width="12%" style="text-align:center; min-width:100px"> </td>
<td width="12%" style="text-align:center; min-width:100px"> </td>
</tr>
<tr>
	<td style="min-width:50px;max-width:60px">Menu<br/>functions</td>
    <td>Provide help</td>
    <td>
		<table style="padding:0px;border:none">
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:48px" src="../../../icons/search1b.svg" width="48px" height="32px"></td><td width="60%" style="padding-top:5px;border:none"><small>Switch search mode on</small></td></tr>
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:48px" src="../../../icons/search.svg" width="48px" height="32px"></td><td width="60%" style="padding-top:5px;border:none"><small>Switch search mode off</small></td></tr>
		</table>
	</td>
    <td>
		<table style="padding:0px;border:none">
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:48px" src="../../../icons/search_res2.svg" width="48px" height="32px"></td><td width="60%" style="padding-top:5px;border:none"><small>Switch show search result mode on</small></td></tr>
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:48px" src="../../../icons/search_res1.svg" width="48px" height="32px"></td><td width="60%" style="padding-top:5px;border:none"><small>Switch show search result mode off</small></td></tr>
			<tr><td width="40%" style="padding-top:10px;border:none"><img style="text-align:center;min-width:48px" src="../../../icons/search_res3.svg" width="48px" height="32px"></td><td width="60%" style="padding-top:5px;border:none"><small>Disabled</small></td></tr>
		</table>
	</td>
    <td></td>
    <td></td>
    <td></td>
</tr>
</table>

















####  Track controls
<table style="font-size: small">
<th width="12%" style="text-align:center; min-width:60px; max-width:60px"> </th>
<th width="17%" style="text-align:center; min-width:100px"><img src="./RTL.png" width="75" height="50"></th>
<th width="17%" style="text-align:center; min-width:100px"><img src="./RoTL.png" width="75" height="50"><br/>and<br/><img src="./ct3.png" width="75" height="50"></th>
<th width="17%" style="text-align:center; min-width:100px"><img src="./RoTL.png" width="75" height="50"><br/>and<br/><img src="./ct3a.png" width="75" height="50"></th>
<th width="17%" style="text-align:center; min-width:100px"><img src="./stl.png" width="75" height="50"></th>
<th width="17%" style="text-align:center; min-width:100px"><img src="./atl.png" width="75" height="50"></th>

<tr>
    <td>short tap</td>
    <td> </td>
    <td> </td>
    <td>on point: delete point<br/>on line: insert new point </td>
    <td> </td>
    <td>make this track to the selected</td>
</tr>
<tr>
    <td>long tap</td>
    <td>toggle gain/loss mode</td>
    <td>toggle gain/loss mode </td>
    <td>on point: toggle gain/loss mode<br/>on line: toggle direct route </td>
    <td>toggle gain/loss mode</td>
    <td> </td>
</tr>
</table>

### Track statistic activity

#### Standard quick controls 
<table style="font-size: small">
<th width="12%" style="text-align:center; min-width:60px; max-width:60px"> </th>
<th width="12%" style="text-align:center; min-width:75px"><img src="./st1.png" width="60px" height="50px"></th>
<th width="12%" style="text-align:center; min-width:75px"><img src="./st2.png" width="60px" height="50px"></th>
<th width="12%" style="text-align:center; min-width:75px"><img src="./st3.png" width="60px" height="50px"></th>
<th width="12%" style="text-align:center; min-width:75px"><img src="./st4.png" width="60px" height="50px"></th>
<th width="12%" style="text-align:center; min-width:75px"><img src="./st5.png" width="60px" height="50px"></th>
<th width="12%" style="text-align:center; min-width:75px"><img src="./st6.png" width="60px" height="50px"></th>
<th width="12%" style="text-align:center; min-width:75px"><img src="./st7.png" width="60px" height="50px"></th>
<th width="12%" style="text-align:center; min-width:75px"><img src="./st8.png" width="60px" height="50px"></th>
<th width="12%" style="text-align:center; min-width:75px"><img src="./st9.png" width="60px" height="50px"></th>


<tr>
    <td>short tap</td>
    <td>toggle fullscreen mode</td>
    <td>select all entries</td>
    <td>deselect all entries</td>
    <td>view selected tracks</td>
    <td>open as marker track</td>
    <td>share tracks</td>
    <td>save tracks</td>
    <td>delete tracks</td>
    <td>back to manin activity</td>
</tr>
</table>




 <small><small>[Back to Index](../../../index.md)</small></small>

