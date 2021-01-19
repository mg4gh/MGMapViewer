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
package mg.mgmap.util;

import android.util.Log;

import mg.mgmap.BgJobService;
import mg.mgmap.MGMapApplication;

public class BgJob {

    public BgJobService service = null;
    public int notification_id = 0;
    long lastNotification = 0;
    int max = 0;
    int progress = 0;
    String text = null;

    public boolean started = false;
    public boolean finished = false;

    public void start(){
        try {
            notification_id = 100+(int)(Math.random()*1000000000);
            started = true;
            doJob();
        } catch (Exception e){
            Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
        } finally {
            finished = true;
        }
    }

    protected void doJob() throws Exception{

    }

    protected void notifyUser(){
        long now = System.currentTimeMillis();
        if (now - lastNotification > 1000){
            lastNotification = now;
            service.notifyUser(notification_id, text, max, progress, (max==0)||(progress==0));
            Log.i(MGMapApplication.LABEL, NameUtil.context()+" "+text+" "+max+" "+progress);
        }
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
