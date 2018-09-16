package com.example.android.camera2basic;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.view.GestureDetector.*;

public class LocationActivity extends AppCompatActivity implements SensorEventListener {
    private MSurfaceView mSurfaceView;
    private SensorManager mSensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSurfaceView = new MSurfaceView(this, this);
        setContentView(mSurfaceView);

        // setup the sensors
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    float[] mGravity;
    float[] mGeomagnetic;
    ArrayList<Float> readings = new ArrayList<>();
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
                    // ROTATE ARROW HERE?
                    //rotate(arrow, (int) (average(readings)));

                    readings = new ArrayList<>();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    protected void onPause() {
        super.onPause();
        mSurfaceView.pause();
        mSensorManager.unregisterListener(this);

    }
    @Override
    protected void onResume() {
        super.onResume();
        mSurfaceView.resume();
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_FASTEST);
    }
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

}
/*
public class LocationActivity extends AppCompatActivity implements OnGestureListener {
    private LocationCallback mLocationCallback;
    private Location lastLocation = null; // store last location
    private int checkIndex = 0;
    private boolean arrived = false;

    private Canvas mCanvas;
    private Paint mPaint = new Paint();
    private Paint mPaintText = new Paint();

    private Bitmap mBitmap;
    private ImageView mImageView;

    private Rect mRect = new Rect();
    private Rect mBounds = new Rect();
    private float tx, ty, sx, sy;

    protected LocationRequest createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(500);
        mLocationRequest.setFastestInterval(400);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return mLocationRequest;
    }

    private void startLocationUpdates() {
        //Log.wtf("TAG", "starting to get the location updates");
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationAvailability(LocationAvailability locationAvailability) {
                //Log.wtf("TAG",""+ locationAvailability.isLocationAvailable());
            }

            @Override
            public void onLocationResult(LocationResult locationResult) {
                List<Location> locations = locationResult.getLocations();
                lastLocation = locations.get(locations.size()-1); // store last location
                if(! arrived){ //check if close enough to move to next point
                    if(CheckPoint.getTestPoints()[checkIndex].atCheckPoint(lastLocation.getLatitude(), lastLocation.getLongitude())){
                        Log.wtf("MYTAG", "at checkpt, moving to next point");
                        final TextView tv1 = (TextView)findViewById(R.id.tvLocations);
                        tv1.setText(tv1.getText() + "\n found cp " + checkIndex);
                        if(checkIndex >= CheckPoint.getTestPoints().length-1){
                            arrived = true;
                            Log.wtf("MYTAG", "Last checkpoint");
                        }
                        else{
                            checkIndex += 1;
                        }
                    }
                }
            }
        };

        if(this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //Log.wtf("TAG", "requesting permission");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 111);
        } else {
            //Log.wtf("TAG", "requesting location updatess");
            FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            mFusedLocationClient.requestLocationUpdates(createLocationRequest(), mLocationCallback, null);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);
        Button locationButton = (Button) findViewById(R.id.btnShowLocation);
        final TextView tv1 = (TextView)findViewById(R.id.tvLocations);
        locationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                //Log.wtf("TAG", "location updates button clicked");
                startLocationUpdates();
                // add last position to tv
                if(lastLocation != null) {
                    Log.wtf("MYTAG", lastLocation.getLatitude() + ", " + lastLocation.getLongitude());
                    tv1.setText(tv1.getText() + "\nlast location: " + lastLocation.getLatitude() + ", " + lastLocation.getLongitude());
                    mDraw(mImageView);
                }
            }
        });
        mPaint.setColor(Color.GREEN);
        mPaintText.setColor(Color.BLACK);
        mImageView = (ImageView) findViewById(R.id.myimageview);
        tx = 0;
        ty = 0;
        sx = 1;
        sy = 1;
    }

    public void mDraw(View view) {
        int vWidth = view.getWidth();
        int vHeight = view.getHeight();
        int halfWidth = vWidth / 2;
        int halfHeight = vHeight / 2;

        mBitmap = Bitmap.createBitmap(vWidth, vHeight, Bitmap.Config.ARGB_8888);
        mImageView.setImageBitmap(mBitmap);
        mCanvas = new Canvas(mBitmap);


        // determine min max of checkpoints
        ArrayList<Double> xs = new ArrayList<Double>();
        ArrayList<Double> ys = new ArrayList<Double>();

        for(CheckPoint p : CheckPoint.getTestPoints()){
            xs.add(p.getLatitude());
            ys.add(p.getLongitude());
        }
        Collections.sort(xs);
        Collections.sort(ys);

        mCanvas.scale(1 , 1);


        double dx = xs.get(xs.size()-1) - xs.get(0);
        double dy = ys.get(ys.size()-1) - ys.get(0);

        mCanvas.drawColor(Color.WHITE);

        for(CheckPoint p : CheckPoint.getTestPoints()){
            Log.wtf("TAG", "Numerator" + (p.getLatitude() - xs.get(0)) + " denom: " + dx * vWidth);
            Log.wtf("TAG", "" + ((p.getLatitude() - xs.get(0)) / dx * vWidth));
            float newX = (float)((p.getLatitude() - xs.get(0)) / dx * vWidth);
            float newY = (float)((p.getLongitude() - ys.get(0)) / dy * vHeight - vHeight/2 );
            mCanvas.drawCircle(newX, newY, vWidth * 0.2f, mPaint);
        }

        //mCanvas.drawText("Some text", 100, 100, mPaintText);
        //mCanvas.drawCircle(halfWidth, halfHeight, halfWidth / 3, mPaint);

        view.invalidate();


    }


    @Override
    protected void onResume() {
        super.onResume();
        startLocationUpdates();
    }

    @Override
    public boolean onDown(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2,
                            float distanceX, float distanceY) {

        return true;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        return false;
    }

}*/
