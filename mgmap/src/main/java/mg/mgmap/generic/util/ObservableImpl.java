package mg.mgmap.generic.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.UUID;

public class ObservableImpl implements Observable{


    private static final class PropertyChangeSupportHolder {
        static final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(ObservableImpl.class);
    }

    private PropertyChangeSupport getPropertyChangeSupport(){
        if (propertyChangeSupport != null){
            return propertyChangeSupport;
        }
        return PropertyChangeSupportHolder.propertyChangeSupport;
    }


    private final String propertyName;
    private Object value;
    private boolean changed;
    private PropertyChangeSupport propertyChangeSupport = null;

    public ObservableImpl(){
        propertyName = UUID.randomUUID().toString();
    }

    public ObservableImpl(String propertyName){
        this.propertyName = propertyName;
    }

    public void setPropertyChangeSupport(PropertyChangeSupport propertyChangeSupport) {
        this.propertyChangeSupport = propertyChangeSupport;
    }

    public void addObserver(PropertyChangeListener propertyChangeListener){
        getPropertyChangeSupport().addPropertyChangeListener(propertyName, propertyChangeListener);
    }

    public void deleteObserver(PropertyChangeListener propertyChangeListener){
        getPropertyChangeSupport().removePropertyChangeListener(propertyName, propertyChangeListener);
    }

    public void deleteObservers(){
        PropertyChangeListener[] listeners = getPropertyChangeSupport().getPropertyChangeListeners(propertyName);
        for (PropertyChangeListener listener : listeners){
            getPropertyChangeSupport().removePropertyChangeListener(listener);
        }
    }


    public void notifyObservers(){
        if (changed){
            getPropertyChangeSupport().firePropertyChange(new PropertyChangeEvent(this, propertyName, 0, 1));
            changed = false;
        }
    }
    synchronized public void notifyObservers(Object newValue){
        if (changed){
            getPropertyChangeSupport().firePropertyChange(new PropertyChangeEvent(this, propertyName, value, newValue));
            value = newValue;
            changed = false;
        }
    }

    public void setChanged() {
        this.changed = true;
    }

}
