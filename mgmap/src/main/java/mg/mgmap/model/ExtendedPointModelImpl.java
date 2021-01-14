package mg.mgmap.model;

public class ExtendedPointModelImpl<T> extends PointModelImpl implements ExtendedPointModel<T>{

    T extent;

    public ExtendedPointModelImpl(PointModel pm, T extent){
        this(pm.getLat(), pm.getLon(), pm.getEleA(), extent);
    }

    public ExtendedPointModelImpl(double latitude, double longitude, float ele, T extent){
        super(latitude, longitude,ele);
        this.extent = extent;
    }

    @Override
    public T getExtent() {
        return extent;
    }
}
