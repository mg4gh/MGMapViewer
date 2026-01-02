package mg.mgmap.generic.util;

import java.beans.PropertyChangeListener;

public interface Observable {


    void addObserver(Observer propertyChangeListener);

    void deleteObserver(Observer propertyChangeListener);

    void deleteObservers();

    void notifyObservers();

    void notifyObservers(Object newValue);

    void setChanged();

    default void changed(){
        setChanged();
        notifyObservers();
    }

}
