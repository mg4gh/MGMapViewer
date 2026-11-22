package mg.mgmap.application.util;

import java.io.IOException;
import java.io.InputStream;

import mg.mgmap.generic.model.PointModel;

public class HgtBufferImplBA implements HgtBuffer{

    byte[] buf;

    public HgtBufferImplBA(InputStream is) throws IOException {
        int todo = is.available();
        buf = new byte[todo];
        int done = 0;
        while (todo > 0) {
            int step = is.read(buf, done, todo);
            todo -= step;
            done += step;
        }
    }

    @Override
    public boolean isSeaLevel() {
        return false;
    }

    @Override
    public float get(int idx) {
        float res = PointModel.NO_ELE;
        int off = idx*2;
        if (buf.length >= off+2){
            byte b1 = buf[off];
            byte b2 = buf[off+1];
            res = (short)( ((b1 & 0xff)<<8) + (b2 & 0xff) );
        }
        return res;
    }

}
