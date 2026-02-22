package com.example.ripple;

import android.content.Context;
import android.location.Location;
import android.os.Looper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class LocationService {

    public interface LocationCallback2 {
        void onLocation(double lat, double lng);
    }

    private final FusedLocationProviderClient client;

    public LocationService(Context context) {
        client = LocationServices.getFusedLocationProviderClient(context);
    }

    @SuppressWarnings("MissingPermission")
    public void getLastLocation(LocationCallback2 callback) {
        client.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                android.util.Log.d("Ripple", "GPS: " + location.getLatitude() + ", " + location.getLongitude());
                callback.onLocation(location.getLatitude(), location.getLongitude());
            } else {
                android.util.Log.d("Ripple", "GPS: no cached location, requesting fresh");
                requestFreshLocation(callback);
            }
        });
    }

    @SuppressWarnings("MissingPermission")
    private void requestFreshLocation(LocationCallback2 callback) {
        LocationRequest request = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMaxUpdates(1)
                .build();

        client.requestLocationUpdates(request, new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                Location loc = result.getLastLocation();
                if (loc != null) {
                    callback.onLocation(loc.getLatitude(), loc.getLongitude());
                }
                client.removeLocationUpdates(this);
            }
        }, Looper.getMainLooper());
    }
}
