/*
 * Copyright 2017 - 2021 mg4gh
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package mg.mgmap.activity.mgmap.util;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;

import mg.mgmap.activity.mgmap.MGMapActivity;

public class TermuxUtil {

    MGMapActivity activity;
    @SuppressLint("SdCardPath")
    String CMD_PATH_DEFAULT = "/data/data/com.termux/files/home/mg.mgmap/";
    String WORKDIR_DEFAULT = "/data/data/com.termux/files/home/mg.mgmap/";

    public TermuxUtil(MGMapActivity activity){
        this.activity = activity;
    }

    public void runCommand(String cmd){
        runCommand( cmd, null);
    }

    public void runCommand(String cmd, String[] args){
        runCommand(null, cmd, null, args);
    }

    public void runCommand(String cmdPath, String cmd, String workdir, String[] args){
        Intent intent = new Intent();
        intent.setClassName("com.termux", "com.termux.app.RunCommandService");
        intent.setAction("com.termux.RUN_COMMAND");
//        intent.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/top");
        String fullCmd = ( (cmdPath==null)?CMD_PATH_DEFAULT:cmdPath ) + cmd;
        intent.putExtra("com.termux.RUN_COMMAND_PATH", fullCmd);
        intent.putExtra("com.termux.RUN_COMMAND_WORKDIR", (workdir==null)?WORKDIR_DEFAULT:workdir);
        intent.putExtra("com.termux.RUN_COMMAND_BACKGROUND", true);
        if (args != null){
            intent.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", args);
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                activity.startForegroundService(intent);
            } else {
                activity.startService(intent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
