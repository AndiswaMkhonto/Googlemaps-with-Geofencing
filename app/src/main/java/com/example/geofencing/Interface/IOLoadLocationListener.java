package com.example.geofencing.Interface;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public interface IOLoadLocationListener {
    void onLoadLocationSuccess(List <MyLatLng> latLngs);

    void onLoadLocationFailed(String message);


}
