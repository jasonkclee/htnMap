package com.example.android.camera2basic;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.graphics.Camera;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;
import android.hardware.SensorManager;
import android.widget.ImageView;

public class LocationActivity extends AppCompatActivity implements SensorEventListener {
    private LocationCallback mLocationCallback;
    private SensorManager mSensorManager;
    Sensor accelerometer;
    Sensor magnetometer;

    Camera mCamera;
    ImageView arrow;

    ArrayList<Float> readings = new ArrayList<>();

    double currentLat = 43.4725764;
    double currentLong = -80.5397156;
    double targetLat = 43.4728327;
    double targetLong = -80.5399427;

    private float average(ArrayList<Float> arr) {
        float sum = 0;
        float count = 0;
        for(int i = 0; i < arr.size(); i++) {
            count += 1;
            sum += arr.get(i);
        }
        return sum/count;
    }

    private void rotate(ImageView image, int angle) {
//        Matrix matrix = new Matrix();
//        image.setScaleType(ImageView.ScaleType.MATRIX);
//        matrix.postRotate((float) angle, image.getWidth()/2, image.getHeight()/2);
//        image.setImageMatrix(matrix);
//
//        return matrix;
        image.setRotation(angle);
    }

    private double computeAngle(double lat1, double long1, double lat2, double long2) {
        double c = Math.sqrt(Math.pow(lat2 - lat1, 2) + Math.pow(long2 - long1, 2));
        double  deltaLat = lat2 - lat1;
        return Math.acos(deltaLat / c);
    }

    private float[] applyLowPassFilter(float[] input, float[] output) {
        if(output == null) {
            return input;
        }
        for (int i = 0; i < input.length; i++) {
            output[i] = output[i] + 0.5f * (input[i] - output[i]);
        }
        return output;
    }

    protected LocationRequest createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return mLocationRequest;
    }

    private void startLocationUpdates() {
        Log.wtf("TAG", "starting to get the location updates");
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationAvailability(LocationAvailability locationAvailability) {
                Log.wtf("TAG",""+ locationAvailability.isLocationAvailable());
            }

            @Override
            public void onLocationResult(LocationResult locationResult) {
                Log.wtf("TAG", "got location request");
                List<Location> locations = locationResult.getLocations();
                for(Location l : locations ) {
                    Log.wtf("TAG", "" + l.getLatitude() + ", " + l.getLongitude());
                }
            }
        };

        if(this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.wtf("TAG", "requesting permission");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 111);
        } else {
            Log.wtf("TAG", "requesting location updatess");
            FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            mFusedLocationClient.requestLocationUpdates(createLocationRequest(), mLocationCallback, null);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.wtf("TAG", "computing the angle" + computeAngle(currentLat, currentLong, targetLat, targetLong));

        //setup the location service
        setContentView(R.layout.activity_location);
        Button locationButton = (Button) findViewById(R.id.btnShowLocation);
        locationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Log.wtf("TAG", "location updates button clicked");
                startLocationUpdates();
            }
        });

        // setup the sensors
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        arrow = findViewById(R.id.arrow);
    }


    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    float[] mGravity;
    float[] mGeomagnetic;
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            mGravity = applyLowPassFilter(event.values, mGravity);
        }
        if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            mGeomagnetic = applyLowPassFilter(event.values, mGeomagnetic);
        }
        if(mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
            if(success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);
                double rotationAngle = orientation[0]/3.14 * 360;
                readings.add((float) rotationAngle);
                if(readings.size() > 100) {
                    rotate(arrow, (int) (average(readings)));
                    readings = new ArrayList<>();
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startLocationUpdates();
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_FASTEST);

    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

}
