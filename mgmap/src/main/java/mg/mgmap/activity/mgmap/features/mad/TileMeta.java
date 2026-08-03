package mg.mgmap.activity.mgmap.features.mad;

import androidx.annotation.NonNull;

import java.util.Locale;

public class TileMeta{
    int tileX,tileY;
    boolean water;
    long offset;
    int length;

    public TileMeta(int tileX, int tileY, boolean water, long offset){
        this.tileX = tileX;
        this.tileY = tileY;
        this.water = water;
        this.offset = offset;
    }

    void setNextOffset(long nextOffset){
        long longLength = nextOffset - offset;
        assert (longLength < Integer.MAX_VALUE);
        this.length = (int)longLength;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(Locale.ENGLISH,"tileX=%05d tileY=%05d water=%s offset=%08x size=%08x",tileX,tileY,water?"Y":"N",offset,length);
    }
}
