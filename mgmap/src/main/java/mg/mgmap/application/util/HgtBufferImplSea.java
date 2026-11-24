package mg.mgmap.application.util;

public class HgtBufferImplSea implements HgtBuffer{

    String name;
    long accessNanos = System.nanoTime();

    public HgtBufferImplSea(String name){
        this.name = name;
    }

    @Override
    public boolean isSeaLevel() {
        accessNanos = System.nanoTime();
        return true;
    }

    @Override
    public float get(int idx) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getAccessNanos() {
        return accessNanos;
    }

    @Override
    public String getName() {
        return name;
    }
}
