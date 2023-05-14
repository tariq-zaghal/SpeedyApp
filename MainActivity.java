package com.example.speedyjava;


import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.Manifest;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Granularity;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

   double[] latLng = new double[4];
    boolean flag = true;
    private TextView tvSpeed;
    private Button btnStart;

    private TextView tvLocation;
    private FusedLocationProviderClient fusedLocationClient;


    FirebaseDatabase database;
    DatabaseReference myRef;

    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    ArrayList<Double> myData = new ArrayList<>();

    String myDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Write a message to the database
         database = FirebaseDatabase.getInstance();

         myRef = database.getReference("Speeds");

        //myRef.setValue("Hello, World!");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        tvSpeed = findViewById(R.id.tvSpeed);
        btnStart = findViewById(R.id.btnStart);
        tvLocation = findViewById(R.id.tvLocation);


        btnStart.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        if(btnStart.getText().equals("Start")){
                        if (hasLocationPermission()) {
                             LocalDateTime now = LocalDateTime.now();
                              myDate = dtf.format(now).toString();
                            btnStart.setText("Stop");
                            setupLocationUpdates();
                        } else {
                            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                                showCustomDialog("Location Permission", "This app needs location Permission", "OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        multiplePermissionLauncher.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
                                    }
                                }, "Cancel", null);
                            } else {
                                multiplePermissionLauncher.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
                            }
                        }
                    }else{
                            fusedLocationClient.removeLocationUpdates(locationCallback);
                            tvSpeed.setText(0+"");
                            btnStart.setText("Start");
                            myRef.child(myDate).setValue(myData);
                            myData.clear();
                        }
                    }
                }
        );
    }

    private final int REQUEST_CHECK_CODE = 1001;


    private void setupLocationUpdates() {

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        LocationRequest locationRequest = new LocationRequest.Builder(1000)
                .setGranularity(Granularity.GRANULARITY_FINE)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMinUpdateDistanceMeters(0)
                .build();

        LocationSettingsRequest locationSettingsRequest = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
                .build();
        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        settingsClient.checkLocationSettings(locationSettingsRequest).addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
            @SuppressLint("MissingPermission")
            @Override
            public void onComplete(@NonNull Task<LocationSettingsResponse> task) {
                if (task.isSuccessful()) {
                    fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

                }else{
                    if(task.getException() instanceof ResolvableApiException){
                        ResolvableApiException resolvableApiException = (ResolvableApiException) task.getException();
                        try {
                            resolvableApiException.startResolutionForResult(MainActivity.this,REQUEST_CHECK_CODE);
                        } catch (IntentSender.SendIntentException e) {
                            throw new RuntimeException(e);
                        }
                    }else{

                    }
                }
            }
        });
    }


    @SuppressLint("MissingPermission")
    private void getLastLocation(){
        CurrentLocationRequest currentLocationRequest = new CurrentLocationRequest.Builder()
                .setGranularity(Granularity.GRANULARITY_FINE)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setDurationMillis(1000)
                .setMaxUpdateAgeMillis(1000)
                .build();

        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        fusedLocationClient.getCurrentLocation(currentLocationRequest,cancellationTokenSource.getToken()).addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                if(task.isSuccessful()){
                    Location location  = task.getResult();
                    Log.d("demo","onComplete: "+ location);
                    latLng[0] = location.getLatitude();
                    latLng[1] = location.getLongitude();
                    tvLocation.setText("location: "+latLng[0]+" "+latLng[1]);

                }else{
                    task.getException().printStackTrace();
                }
            }
        });

    }


    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            super.onLocationResult(locationResult);
            Log.d("demo","locationResult "+ locationResult);
            for (Location location : locationResult.getLocations()) {
                // Update UI with location d
                //ata
                // ..
                double lat = location.getLatitude();
                double lng = location.getLongitude();
                tvLocation.setText("location: "+lat+" "+lng);

                if(flag){
                    latLng[0] = lat;
                    latLng[1] = lng;
                    flag = false;
                }

                latLng[2] = lat;
                latLng[3] = lng;

                double spd  = distance(latLng);
               // LocalDateTime now = LocalDateTime.now();
              //  String s = dtf.format(now).toString();
               // myRef.child(s).setValue(spd);
               // myRef.push().setValue(spd);
                myData.add(spd);

                tvSpeed.setText(String.format("%.2f",spd));


            }
        }

    };


    private boolean hasLocationPermission(){
        return checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)== PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED;

    }

    void showCustomDialog(String title, String message,
                          String positiveBtnTitle, DialogInterface.OnClickListener positiveListener,
                          String negativeBtnTitle, DialogInterface.OnClickListener negativeListener){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveBtnTitle,positiveListener)
                .setNegativeButton(negativeBtnTitle,negativeListener);
        builder.create().show();
    }

    private ActivityResultLauncher<String[]> multiplePermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), new ActivityResultCallback<Map<String, Boolean>>() {
        @Override
        public void onActivityResult(Map<String, Boolean> result) {
            boolean findPermissionAllowed = false;
            if(result.get(Manifest.permission.ACCESS_FINE_LOCATION)!=null){
                findPermissionAllowed = result.get(Manifest.permission.ACCESS_FINE_LOCATION);
                if(findPermissionAllowed){
                   // getLastLocation();
                    setupLocationUpdates();
                }else{
                    if(!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)){
                        showCustomDialog("Location Permission", "the app needs fine location permission", "OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.parse("package:"));
                                startActivity(intent);
                            }
                        },"Cancel",null);
                    }
                }
            }
        }
    });



//    double lat1, double lat2, double lon1,
//    double lon2, double el1, double el2

    public static double distance(double[] arr) {

        final int R = 6371; // Radius of the earth

        double latDistance = Math.toRadians(arr[2] - arr[0]);
        double lonDistance = Math.toRadians(arr[3] - arr[1]);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(arr[0])) * Math.cos(Math.toRadians(arr[2]))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters


        distance = Math.pow(distance, 2);

        arr[0] = arr[2];
        arr[1] = arr[3];

        return Math.sqrt(distance);
    }


}