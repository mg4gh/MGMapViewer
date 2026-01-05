package mg.mgmap.generic.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.UUID;

public class ObservableImpl implements Observable{

    final ArrayList<Observer> observers = new ArrayList<>();

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
    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    @Override
    public void deleteObserver(Observer observer) {
        observers.remove(observer);
    }

    @Override
    public void deleteObservers() {
        observers.clear();
    }

    @Override
    public void notifyObservers() {
        notifyObservers(value);
    }

    @Override
    public void notifyObservers(Object newValue) {
        if (changed) {
            PropertyChangeEvent propertyChangeEvent = new PropertyChangeEvent(this, propertyName, value, newValue);
            for (Observer observer : observers) {
                observer.propertyChange(propertyChangeEvent);
            }
            value = newValue;
            changed = false;
        }
    }

    @Override
    public void setChanged() {
        this.changed = true;
    }

    public Observer findObserver(Class<?> clazz){
        for (Observer observer : observers){
            if (clazz.isInstance(observer)){
                return observer;
            }
        }
        return null;
    }

}
