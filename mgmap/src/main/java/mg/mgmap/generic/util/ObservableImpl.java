package mg.mgmap.generic.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.UUID;

public class ObservableImpl implements Observable{

    final ArrayList<PropertyChangeListener> propertyChangeListeners = new ArrayList<>();

    protected final String propertyName;
    private Object value;
    private boolean changed = false;

    public ObservableImpl(){
        propertyName = UUID.randomUUID().toString();
    }

    public ObservableImpl(String propertyName){
        this.propertyName = propertyName;
    }

    @Override
    public void addObserver(PropertyChangeListener propertyChangeListener) {
        propertyChangeListeners.add(propertyChangeListener);
    }

    @Override
    public void deleteObserver(PropertyChangeListener propertyChangeListener) {
        propertyChangeListeners.remove(propertyChangeListener);
    }

    @Override
    public void deleteObservers() {
        propertyChangeListeners.clear();
    }

    @Override
    public void notifyObservers() {
        notifyObservers(value);
    }

    @Override
    public void notifyObservers(Object newValue) {
        if (changed) {
            PropertyChangeEvent propertyChangeEvent = new PropertyChangeEvent(this, propertyName, value, newValue);
            for (PropertyChangeListener propertyChangeListener : propertyChangeListeners) {
                propertyChangeListener.propertyChange(propertyChangeEvent);
            }
            value = newValue;
            changed = false;
        }
    }

    @Override
    public void setChanged() {
        this.changed = true;
    }


}
