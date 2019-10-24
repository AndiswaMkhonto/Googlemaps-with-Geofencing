package com.example.geofencing;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.widget.Toast;

import com.example.geofencing.Interface.IOLoadLocationListener;
import com.example.geofencing.Interface.MyLatLng;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GeoQueryEventListener, IOLoadLocationListener {

    private GoogleMap mMap;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Marker currentUser;
    private DatabaseReference myLocationRef;
    private GeoFire geofire;
    private List<LatLng> wifiAreas;
    private IOLoadLocationListener listener;

    private DatabaseReference myCity;
    private Location lastLocation;
    private GeoQuery geoQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);


        Dexter.withActivity(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        buildLocationRequest();
                        buildLocationCallBack();
                        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MapsActivity.this);

                        // Obtain the SupportMapFragment and get notified when the map is ready to be used.

                        initArea();
                        settingGeoFire();

                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(MapsActivity.this, "You need to enable permission", Toast.LENGTH_LONG).show();

                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                    }
                })
                .check();



    }

    private void initArea() {
        myCity = FirebaseDatabase.getInstance()
                .getReference("FreeWifiHotspots")
                .child("MyCity");

        listener=this;




//                myCity.addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                     List <MyLatLng> latLngList = new ArrayList<>();
//                     for(DataSnapshot locationSnapShot: dataSnapshot.getChildren()){
//                         MyLatLng latLng = locationSnapShot.getValue(MyLatLng.class);
//                         latLngList.add(latLng);
//                     }
//                     listener.onLoadLocationSuccess(latLngList);
//                    }

//                    @Override
//                    public void onCancelled(@NonNull DatabaseError databaseError) {
//                     listener.onLoadLocationFailed(databaseError.getMessage());
//                    }
//                });
                myCity.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {


                        List <MyLatLng> latLngList = new ArrayList<>();
                        for(DataSnapshot locationSnapShot: dataSnapshot.getChildren()){
                            MyLatLng latLng = locationSnapShot.getValue(MyLatLng.class);
                            latLngList.add(latLng);
                        }

                        listener.onLoadLocationSuccess(latLngList);


//                        wifiAreas = new ArrayList<>();
//                        for(MyLatLng myLatLng: latLngs){
//                            LatLng convert = new LatLng(myLatLng.getLatitude(), myLatLng.getLongitude());
//                            wifiAreas.add(convert);
//                        }




                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });







//        wifiAreas = new ArrayList<>();
//        wifiAreas.add(new LatLng(25.7487, 28.2380));//hatfield
//        wifiAreas.add(new LatLng(25.7453, 28.2030));//arcadia
//        wifiAreas.add(new LatLng(25.7535, 28.2079));//sunny side


//        FirebaseDatabase.getInstance()
//                .getReference("FreeWifiHotspots")
//                .child("MyCity")
//                .setValue(wifiAreas)
//        .addOnCompleteListener(new OnCompleteListener<Void>() {
//            @Override
//            public void onComplete(@NonNull Task<Void> task) {
//                Toast.makeText(MapsActivity.this, "Updated!",Toast.LENGTH_LONG).show();
//            }
//        }).addOnFailureListener(new OnFailureListener() {
//            @Override
//            public void onFailure(@NonNull Exception e) {
//                Toast.makeText(MapsActivity.this, ""+e.getMessage(), Toast.LENGTH_LONG).show();
//            }
//        });

    }

    private void addUserMarker() {

        geofire.setLocation("You", new GeoLocation(lastLocation.getLatitude(),
                                    lastLocation.getLongitude()), new GeoFire.CompletionListener() {
                                @Override
                                public void onComplete(String key, DatabaseError error) {
                                    if(currentUser != null) currentUser.remove();
                                    currentUser = mMap.addMarker(new MarkerOptions()
                                            .position(new LatLng(lastLocation.getLatitude(),
                                                   lastLocation.getLongitude()))
                                           .title("You"));
                                   mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentUser.getPosition(), 12.0f));


                                }
                            });


    }

    private void settingGeoFire() {
        myLocationRef = FirebaseDatabase.getInstance().getReference("My_Location");
        geofire = new GeoFire(myLocationRef);


    }

    private void buildLocationCallBack() {
        locationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(final LocationResult locationResult) {
                if(mMap != null){
                    lastLocation = locationResult.getLastLocation();


//                    geofire.setLocation("You", new GeoLocation(lastLocation.getLatitude(),
//                            lastLocation.getLongitude()), new GeoFire.CompletionListener() {
//                        @Override
//                        public void onComplete(String key, DatabaseError error) {
//                            if(currentUser != null) currentUser.remove();
//                            currentUser = mMap.addMarker(new MarkerOptions()
//                                    .position(new LatLng(lastLocation.getLatitude(),
//                                            lastLocation.getLongitude()))
//                                    .title("You"));
//                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentUser.getPosition(), 12.0f));
//
//
//                        }
//                    });

                    addUserMarker();
                }


            }
        };

    }

    private void buildLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(30000);
        locationRequest.setSmallestDisplacement(10f);

    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

//        // Add a marker in Sydney and move the camera
//        LatLng sydney = new LatLng(-34, 151);
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));



        mMap.getUiSettings().setZoomControlsEnabled(true);

        if(fusedLocationProviderClient != null)
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                   return;
                }



            }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());

            //Add circle for wifi hot spots
//        for(LatLng latLng: wifiAreas){
//            mMap.addCircle(new CircleOptions().center(latLng)
//            .radius(500) //500m
//            .strokeColor(Color.GREEN)
//            .fillColor(0x220000FF) //22 is transparent code
//            .strokeWidth(5.0f));
//
//            //Create GeoQuery when user is in range of wifi hotspot
//
//            GeoQuery geoQuery = geofire.queryAtLocation(new GeoLocation(latLng.latitude, latLng.longitude) ,0.5f);
//            geoQuery.addGeoQueryEventListener(MapsActivity.this);
//        }
addCircleArea();
    }

    private void addCircleArea() {

        if(geoQuery != null){
            geoQuery.removeGeoQueryEventListener(this);
            geoQuery.removeAllListeners();


        }


        for(LatLng latLng: wifiAreas){
            mMap.addCircle(new CircleOptions().center(latLng)
            .radius(500) //500m
            .strokeColor(Color.GREEN)
            .fillColor(0x220000FF) //22 is transparent code
            .strokeWidth(5.0f));

            //Create GeoQuery when user is in range of wifi hotspot

        geoQuery = geofire.queryAtLocation(new GeoLocation(latLng.latitude, latLng.longitude) ,0.5f);
        geoQuery.addGeoQueryEventListener(MapsActivity.this);
        }

    }

    @Override
    protected void onStop() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        super.onStop();
    }

    public void onKeyEntered(String key, GeoLocation location) {
        sendNotification("EDMTDev", String.format("You have entered wifi hotspot area", key));
    }



    @Override
    public void onKeyExited(String key) {
        sendNotification("EDMTDev", String.format("You have exited wifi hotspot area", key));

    }

    @Override
    public void onKeyMoved(String key, GeoLocation location) {
        sendNotification("EDMTDev", String.format("You are moving within  wifi hotspot area", key));

    }

    @Override
    public void onGeoQueryReady() {

    }

    @Override
    public void onGeoQueryError(DatabaseError error) {
        Toast.makeText(this,error.getMessage(), Toast.LENGTH_LONG).show();

    }
    private void sendNotification(String title, String content) {

       Toast.makeText(this, ""+content, Toast.LENGTH_LONG).show();

        String NOTIFICATION_CHANNEL_ID = "edmt_multiple_location";
        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,"My notifications",
                    NotificationManager.IMPORTANCE_DEFAULT);

            //Config
            notificationChannel.setDescription("Channel Description");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setVibrationPattern(new long[] {0, 1000, 500, 1000});
            notificationChannel.enableVibration(true);
            notificationManager.createNotificationChannel(notificationChannel);

        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        builder.setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(false)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher));

        Notification notification = builder.build();
        notificationManager.notify(new Random().  nextInt(),notification );

    }

    @Override
    public void onLoadLocationSuccess(List<MyLatLng> latLngs) {
        wifiAreas = new ArrayList<>();
        for(MyLatLng myLatLng: latLngs){
            LatLng convert = new LatLng(myLatLng.getLatitude(), myLatLng.getLongitude());
            wifiAreas.add(convert);
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(MapsActivity.this);

        if(mMap != null){
            mMap.clear();

            addUserMarker();
////
            addCircleArea();
        }


    }

    @Override
    public void onLoadLocationFailed(String message) {
        Toast.makeText(this, ""+message, Toast.LENGTH_LONG).show();

    }
}
