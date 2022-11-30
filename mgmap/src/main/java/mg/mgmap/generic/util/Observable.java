package mg.mgmap.generic.util;

import java.beans.PropertyChangeListener;

public interface Observable {


    void addObserver(PropertyChangeListener propertyChangeListener);

    void deleteObserver(PropertyChangeListener propertyChangeListener);

    void deleteObservers();

    void notifyObservers();

    void notifyObservers(Object newValue);

    void setChanged();

    default void changed(){
        setChanged();
        notifyObservers();
    }

}
