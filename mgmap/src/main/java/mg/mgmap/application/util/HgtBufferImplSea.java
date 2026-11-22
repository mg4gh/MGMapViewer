package mg.mgmap.application.util;

public class HgtBufferImplSea implements HgtBuffer{


    @Override
    public boolean isSeaLevel() {
        return true;
    }

    @Override
    public float get(int idx) {
        throw new UnsupportedOperationException();
    }
}
