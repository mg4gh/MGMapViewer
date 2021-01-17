/*
 * Copyright 2017 - 2020 mg4gh
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
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.UUID;

import mg.mgmap.ControlView;
import mg.mgmap.MGMapApplication;

/**
 * An OnClickListener with some default functionality.
 */
public class Control implements View.OnClickListener {


    protected ControlView controlView;

    public void setControlView(ControlView controlView){
        this.controlView = controlView;
    }

    protected UUID uuid;
    private boolean hideAfterClick;

    public Control(boolean hideAfterClick){
        uuid = UUID.randomUUID();
        this.hideAfterClick = hideAfterClick;
    }

    @Override
    public void onClick(View v) {
        if (v instanceof Button) {
            Button button = (Button) v;
            Log.i(MGMapApplication.LABEL, NameUtil.context() +" " + button.getText());
        }
        controlView.startTTHideButtons(hideAfterClick?10:7000);
    }

    public void onPrepare(View v){}

    protected void setText(View v, CharSequence text){
        if (v instanceof Button) {
            ((Button) v).setText(text);
        } else if (v instanceof TextView) {
            ((TextView) v).setText(text);
        }
    }

    public UUID getUuid(){
        return uuid;
    }

    protected <T> Pref<T> getPref(int id, T defaultValue){
        return controlView.getActivity().getPrefCache().get(id,defaultValue);
    }
}
