package mg.mgmap.activity.mgmap.features.routing;

import java.util.ArrayList;

import mg.mgmap.activity.mgmap.features.routing.profile.MTB;
import mg.mgmap.activity.mgmap.features.routing.profile.ShortestDistance;
import mg.mgmap.activity.mgmap.features.routing.profile.TrekkingBike;
import mg.mgmap.application.MGMapApplication;

public class RoutingProfileManager {

    MGMapApplication mgMapApplication;
    ArrayList<RoutingProfile> routingProfiles = new ArrayList<>();
    RoutingProfile currentRoutingProfile;

    public RoutingProfileManager(MGMapApplication mgMapApplication){
        this.mgMapApplication = mgMapApplication;
        routingProfiles.add(new ShortestDistance(mgMapApplication));
        routingProfiles.add(new MTB(mgMapApplication));
        routingProfiles.add(new TrekkingBike(mgMapApplication));
    }

    public RoutingProfile getCurrentRoutingProfile(){
        return currentRoutingProfile;
    }

    public void setCurrentRoutingProfile(String routingProfileId){
        if ((currentRoutingProfile==null) || (!routingProfileId.equals(this.currentRoutingProfile.getId())) ){
            this.currentRoutingProfile = getRoutingProfileById(routingProfileId);
        }
    }

    public RoutingProfile getRoutingProfileById(String id){
        if ((currentRoutingProfile!=null) && currentRoutingProfile.getId().equals(id)) {
            return currentRoutingProfile;
        }
        for (RoutingProfile routingProfile : routingProfiles){
            if (routingProfile.getId().equals(id)){
                return routingProfile;
            }
        }
        return null;
    }

    public ArrayList<RoutingProfile> getRoutingProfiles() {
        return routingProfiles;
    }
}
