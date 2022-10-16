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

import android.os.Handler;
import android.util.Log;
import android.view.View;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.util.basic.NameUtil;

public class ExtendedClickListener implements View.OnClickListener {

    private static Handler timer = new Handler();
    protected long doubleClickTimeout = 10;
    private class TTSingle implements Runnable{
        @Override
        public void run() {
            ttSingle = null;
            onSingleClick(view);
        }
    };
    private TTSingle ttSingle = null;
    private View view = null;


    @Override
    public void onClick(View v) {
        view = v;
        if (ttSingle == null){
            ttSingle = new TTSingle();
            timer.postDelayed(ttSingle,doubleClickTimeout);
        } else {
            timer.removeCallbacks(ttSingle);
            ttSingle = null;
            onDoubleClick(view);
        }
    }

    public void onSingleClick(View view){}

    public void onDoubleClick(View view){
        Log.v(MGMapApplication.LABEL, NameUtil.context()+" double");
        onSingleClick(view);
        onSingleClick(view);
    }
}
