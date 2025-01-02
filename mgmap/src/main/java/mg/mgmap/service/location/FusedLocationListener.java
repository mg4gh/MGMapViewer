package mg.mgmap.service.location;

import android.location.Location;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import mg.mgmap.application.MGMapApplication;

public class FusedLocationListener extends AbstractLocationListener {

    private final LocationCallback locationCallback;
    private final FusedLocationProviderClient fusedLocationClient;

    public FusedLocationListener(MGMapApplication application, TrackLoggerService trackLoggerService){
        super(application, trackLoggerService);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(application.getApplicationContext() );
        locationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()){
                    locationChanged(location);
                }
            }
        };
    }

    protected void activate(int minMillis,int minDistance) throws SecurityException{
        super.activate(minMillis, minDistance);
        LocationRequest locationRequest;
        LocationRequest.Builder builder = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY);
        builder.setIntervalMillis(minMillis);
        builder.setPriority(Priority.PRIORITY_HIGH_ACCURACY); // although Priority.PRIORITY_HIGH_ACCURACY = 100 is given in the constructor, the priority value is PRIORITY_BALANCED_POWER_ACCURACY = 102
        builder.setMaxUpdateDelayMillis(minMillis);
        builder.setMinUpdateIntervalMillis(minMillis);
        builder.setMinUpdateDistanceMeters(minDistance);
        locationRequest = builder.build();
        // Register the listener
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
    }


    protected void deactivate(){
        super.deactivate();
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

}
