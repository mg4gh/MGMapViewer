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
package mg.mapviewer.features.bb;

import android.content.Context;
import android.util.Log;
import android.view.View;

import java.io.File;

import mg.mapviewer.MGMapActivity;
import mg.mapviewer.MGMapApplication;
import mg.mapviewer.MGMapLayerFactory;
import mg.mapviewer.R;
import mg.mapviewer.util.Control;
import mg.mapviewer.util.NameUtil;
import mg.mapviewer.util.PersistenceManager;
import mg.mapviewer.util.TileStoreLoader;

public class LoadTSFromBBControl extends Control {

    MGMapActivity activity;
    MGMapApplication application;
    MSBB msbb;
    File ts = null;


    public LoadTSFromBBControl(MGMapActivity activity, MGMapApplication application, MSBB msbb){
        super(true);
        this.application = application;
        this.activity = activity;
        this.msbb = msbb;
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);

        try {
            TileStoreLoader tileStoreLoader = new TileStoreLoader(activity, application, ts);
            tileStoreLoader.loadFromBB(msbb.getBBox());
        } catch (Exception e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
        }
    }

    @Override
    public void onPrepare(View v) {
        v.setEnabled(false);
        if (msbb.isLoadAllowed()){
            ts = identifyTS();
            if (ts != null){
                if (new File(ts,"config.xml").exists()){
                    v.setEnabled(true);
                }
            }
        }
        setText(v, controlView.rstring(R.string.btLoadTSFromBB) );
    }

    File identifyTS(){
        for (String key : MGMapLayerFactory.mapLayers.keySet()){
            String[] keypart = key.split(": ");
            if (keypart.length != 2) return null;
            if (keypart[0].equals( MGMapLayerFactory.Types.MAPSTORES.toString() )){
                PersistenceManager pm = PersistenceManager.getInstance();
                File fTS = new File(pm.getMapsDir(), keypart[0]);
                File storePath = new File(fTS, keypart[1].toLowerCase());
                if (storePath.exists() && storePath.isDirectory()){
                    return storePath;
                }
            }
        }
        return null;
    }
}
