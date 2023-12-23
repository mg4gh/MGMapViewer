package mg.mgmap.activity.mgmap.features.routing;

import android.content.Context;

import org.mapsforge.map.datastore.Way;

import java.util.HashMap;
import java.util.Map;

import mg.mgmap.generic.graph.GNode;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.view.ExtendedTextView;

public abstract class RoutingProfile {

    Context context;
    protected String ID;
    protected Map<String, String> parameters = new HashMap<>();

    public RoutingProfile(Context context){
        this.context = context;
        ID = constructId(this.getClass());
    }

    public static String constructId(Class<?> clazz){
        return "RP_"+clazz.getSimpleName();
    }

    public String getId(){
        return ID;
    }

    public Map<String, String> getParameters(){
        return parameters;
    }

    public void setParameter(String name, String value){
        parameters.put(name,value);
    }

    public abstract double getCost(Way way, GNode node1, GNode node2);

    abstract protected int getIconIdActive();
    abstract protected int getIconIdInactive();


    void initETV(ExtendedTextView etv, Pref<String> prefCurrentRoutingProfileId){
        Pref<Boolean> rpState = new Pref<>(ID.equals(prefCurrentRoutingProfileId.getValue()));
        prefCurrentRoutingProfileId.addObserver(evt -> rpState.setValue( ID.equals(prefCurrentRoutingProfileId.getValue()) ));
        etv.setData(rpState, getIconIdInactive(), getIconIdActive());
        etv.setOnClickListener(v -> prefCurrentRoutingProfileId.setValue(ID));
    }
}
