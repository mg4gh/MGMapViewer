package mg.mgmap.features.tilestore;

import android.util.Log;

import org.mapsforge.core.model.Tile;

import java.net.URLConnection;

import mg.mgmap.MGMapActivity;
import mg.mgmap.MGMapApplication;
import mg.mgmap.util.BgJob;
import mg.mgmap.util.NameUtil;

public abstract class MGTileStoreLoaderJob extends BgJob {

    TileStoreLoader tileStoreLoader;
    Tile tile;
    URLConnection conn = null;
    String debug = null;

    public MGTileStoreLoaderJob(TileStoreLoader tileStoreLoader, Tile tile){
        this.tileStoreLoader = tileStoreLoader;
        this.tile = tile;
    }

    @Override
    protected void doJob() throws Exception {
        try {
            boolean success = false;
            if ( (tileStoreLoader.errorCounter - tileStoreLoader.successCounter*3) < 8){
                doJobNow();
                success = true;
            }
            tileStoreLoader.jobFinished(success, null);
        } catch (Exception e) {
            tileStoreLoader.jobFinished(false, e);
            throw e;
        }
    }

    protected void doJobNow() throws Exception {}
 }
