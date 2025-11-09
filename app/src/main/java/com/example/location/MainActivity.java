package com.example.location;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.CircularBounds;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.net.SearchNearbyRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener {

    private final String TAG = MainActivity.class.toString();

    private MapView mMap;
    private RecyclerView mView;

    private String[] mPermissionsLocation = {Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION};
    private ArrayList<String> mListPermission = new ArrayList<>();
    private LinkedHashMap<String,Boolean> mMapPermission = new LinkedHashMap<>();
    private ActivityResultLauncher<String[]> mRequestPermissionLauncher;

    private GoogleMap mGoogleMap;
    private LocationManager locationManager;
    private double mLatitude,mLongitude;
    private Bundle bundle;

    @Override
    protected void onCreate(Bundle savedInstanceState){

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if(!Places.isInitialized()){
            Places.initializeWithNewPlacesApiEnabled(getApplicationContext(),"AIzaSyBFa5-sQqGZMNO0FjXgDfaQBZaH_PUrXws");
        }

        bundle = savedInstanceState;

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        mMap = findViewById(R.id.vwMap);
        mView = findViewById(R.id.vWListNearbyPlaces);

        MapsInitializer.initialize(this);

        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){
            mMap.getMapAsync(this);
            mMap.onCreate(bundle);
            if(locationManager!=null){
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,60000,100,MainActivity.this);
            }
        }else{
            Log.i(TAG+"_1st_time_permission","not_available");
            manageLocationPermissions();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap){
        Log.i(TAG+"_called","onMapReady");
        mGoogleMap = googleMap;
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        LocationListener.super.onProviderEnabled(provider);
        Log.i(TAG+"_status","Enabled");
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        LocationListener.super.onProviderDisabled(provider);
        Log.i(TAG+"_status","Disabled");
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        Log.i(TAG+"_called","onLocationChanged");

        int tempLatitude = (int) location.getLatitude();
        int tempLongitude = (int) location.getLongitude();

        int previousLatitude = (int) mLatitude;
        int previousLongitude = (int) mLongitude;

        Log.i(TAG+"_current_data",tempLatitude+", "+tempLongitude+" ..kk");

        if(tempLatitude != previousLatitude && tempLongitude != previousLongitude){

            Log.i(TAG+"_status","True");

            mLatitude = location.getLatitude();
            mLongitude = location.getLongitude();

            try{
                mGoogleMap.setMyLocationEnabled(true);
                LatLng currentLocation = new LatLng(mLatitude,mLongitude);
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(currentLocation);
                markerOptions.title("Current Location");
                mGoogleMap.addMarker(markerOptions);
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation,15));
            }catch(SecurityException e1){
                Log.i(TAG+"_onMapReady_1",e1.getMessage()+" ..kk");
            }catch(Exception e2){
                Log.i(TAG+"_onMapReady_1",e2.getMessage()+" ..kk");
            }

            saveDataForReference();
            findNearbyPlaces();
        }else{
            Log.i(TAG+"_status","False");
        }
    }

    private void manageLocationPermissions(){

        mRequestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), new ActivityResultCallback<Map<String, Boolean>>(){
            @Override
            public void onActivityResult(Map<String, Boolean> map){

                Log.i(TAG+"_permission_status","provided");

                for(Map.Entry<String,Boolean> entry : map.entrySet()){
                    Log.i(TAG+"_permissionInfo",entry.getKey()+"-->"+entry.getValue()+" ..kk");
                }

                if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){
                    Log.i(TAG+"_2nd_time_permission","available");
                    mMap.getMapAsync(MainActivity.this);
                    mMap.onCreate(bundle);
                    if(locationManager!=null){
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,60000,100,MainActivity.this);
                    }
                }
            }
        });

        for(String permission: mPermissionsLocation){
            if(ContextCompat.checkSelfPermission(this,permission) != PackageManager.PERMISSION_GRANTED){
                mMapPermission.put(permission,false);
            }
        }

        for(Map.Entry<String,Boolean> entry : mMapPermission.entrySet()){
            String key = entry.getKey();
            boolean status = entry.getValue();
            if(!status){
                mListPermission.add(key);
            }
        }
        String[] mArr = mListPermission.toArray(new String[0]);
        mRequestPermissionLauncher.launch(mArr);
    }

    @Override
    protected void onResume(){
        mMap.onResume();
        super.onResume();
    }

    @Override
    protected void onPause(){
        mMap.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy(){
        mMap.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory(){
        mMap.onLowMemory();
        super.onLowMemory();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState, @NonNull PersistableBundle outPersistentState) {
        mMap.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState, outPersistentState);
    }

    private void findNearbyPlaces(){
        Log.i(TAG+"_called","findNearbyPlaces()");
        List<Place.Field> placeFields = Arrays.asList(Place.Field.ID, Place.Field.DISPLAY_NAME);
        LatLng center = new LatLng(mLatitude,mLongitude);
        CircularBounds circularBounds = CircularBounds.newInstance(center,1000);

        final List<String> includedTypes = Arrays.asList("restaurant","cafe");

        SearchNearbyRequest searchNearbyRequest = SearchNearbyRequest.builder(circularBounds,placeFields)
                .setMaxResultCount(10)
                .setIncludedTypes(includedTypes)
                .build();

        PlacesClient placesClient = Places.createClient(this);
        placesClient.searchNearby(searchNearbyRequest)
                .addOnSuccessListener(response -> {
                    List<Place> places = response.getPlaces();
                    if(places!=null){
                        if(places.size()>0){
                            for(Place place : places){
                                Log.i(TAG+"_placeName",place.getDisplayName()+" ..kk");
                            }
                        }
                    }
                })
                .addOnFailureListener(response -> {
                    Log.e(TAG+"_nearFailed",response.toString()+" ..kk");
                });
    }

    private void saveDataForReference(){
        SharedPreferences sharedPreferences = getSharedPreferences("LocationDetails",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(String.valueOf(System.currentTimeMillis()),mLatitude+","+mLongitude);
        editor.apply();
    }
}