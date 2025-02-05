<small><small>[Back to Index](../../../index.md)</small></small>

## Further Features: File manager

Starting with Android 10 google restricts the access to the main app directory [see File system information](../../../GettingStarted/FileSystem.md) more and more.
Now, with Android 14, even the relevant development tools (e.g. the device explorer of AndroidStudio) cannot access these folders any longer. But how to place a
configuration file, a map file or simply a config file into their expected location? 
The answer is: Use the new internal <span style="color:black"><b>File Manager</b></span>.

To open the file manager use <img src="../../../icons/group_task.svg" width="24"/> + <img src="../../../icons/file_mgr.svg" width="24"/> and you'll get the app 
main directory.

<img src="./01_Start.png" width="270" />&nbsp;

Each entry consists of a <span style="color:gray">*Select check box*</span> and the name of the diectory entry. The entries are either diectories 
<img src="../../../icons/file_mgr_dir.svg" width="24"/> of files <img src="../../../icons/file_mgr_file.svg" width="24"/>. 
As long as there is no other entry seleceted, a short tap on a directory name opens this directory while a short tap on a file tries to open this.
All other tap actions on an entry toggle its selection state.

The file manager headline allows to go one level up <img src="../../../icons/file_mgr_up.svg" width="24"/> or to jump to an explicit parent directory.

The quick controls provide follwoing functions:
<table>
  <tr>
    <th>Quick control icon</th>
    <th>Quick control enabled condition</th>
    <th>Quick control Funtion</th>
  </tr>
  <tr>
    <td><img src="../../../icons/file_mgr_dir.svg" width="24"/></td> 
    <td>always</td>
    <td>create a new subdirectory</td>	
  </tr>
  <tr>
    <td><img src="../../../icons/file_mgr_file.svg" width="24"/></td> 
    <td>always</td>
    <td>create a new file in the current directory</td>	
  </tr>
  <tr>
    <td><img src="../../../icons/edit2.svg" width="24"/></td> 
    <td>one entry is seleceted</td>
    <td>edit the file or directory name</td>	
  </tr>
  <tr>
    <td><img src="../../../icons/show.svg" width="24"/></td> 
    <td>one file is seleceted</td>
    <td>open the file (same as single tap)</td>	
  </tr>
  
  
  <tr>
    <td><img src="../../../icons/file_mgr_move.svg" width="24"/></td> 
    <td>one or more entries are selected</td>
    <td>move file(s) and/or directory/directories</td>	
  </tr>
  <tr>
    <td><img src="../../../icons/share.svg" width="24"/></td> 
    <td>one or more files are selected</td>
    <td>share the selected files/directories</td>	
  </tr>
  <tr>
    <td><img src="../../../icons/save.svg" width="24"/></td> 
    <td>uncompleted move or share operation</td>
    <td>save the (moved or via share received) content to the current directory <br/> <b>Hint</b>: first navigate to the desired target directory, then use the save button</td>	
  </tr>
  <tr>
    <td><img src="../../../icons/delete.svg" width="24"/></td> 
    <td>one or more entries are selected</td>
    <td>delete the selected files and directories</td>	
  </tr>
</table>


<small><small>[Back to Index](../../../index.md)</small></small>