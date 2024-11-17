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
package mg.mgmap.generic.util;

import androidx.core.app.NotificationCompat;

import java.io.FileNotFoundException;
import java.lang.invoke.MethodHandles;

import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.service.bgjob.BgJobService;

public class BgJob {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    public BgJobService service = null;
    public BgJobGroup group = null;
    public int notification_id = 0;
    private NotificationCompat.Builder notiBuilder = null;
    long lastNotification = 0;
    int max = 0;
    int progress = 0;
    String text = null;

    public BgJob(){}

    public void start(){
        try {
            notification_id = 100+(int)(Math.random()*1000000000);
            if (group != null){
                boolean success = false;
                if ( (group.errorCounter - group.successCounter*3) < 8){
                    doJob();
                    success = true;
                }
                group.jobFinished(success, null);
            } else {
                doJob();
            }
        } catch (Exception e){
            if (group != null) group.jobFinished(false, e);
            if (!(e instanceof FileNotFoundException)){
                mgLog.e(e);
            }
        } finally {
            service.notifyUserFinish(notification_id);
        }
    }

    protected void doJob() throws Exception{
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
        if (notiBuilder == null){
            notiBuilder = service.createNotificationBuilder(text);
        }
        long now = System.currentTimeMillis();
        if (now - lastNotification > 1000){
            lastNotification = now;
            service.notifyUserProgress(notiBuilder, notification_id, max, progress, (max==0)||(progress==0));
            mgLog.i(text+" "+max+" "+progress);
        }
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
