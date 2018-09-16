package com.example.android.camera2basic;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
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

public class MSurfaceView extends SurfaceView implements Runnable, GestureDetector.OnGestureListener {
    private final SurfaceHolder mSurfaceHolder;
    private Paint mPaint;
    private boolean mRunning;
    private Thread mGameThread;
    private float vWidth, vHeight;
    private float tx, ty;
    private boolean first = true;
    private float px, py;
    private ScaleGestureDetector mScaleDetector;
    private float mScaleFactor = 1.f;

    private int checkIndex = 0;
    private boolean arrived = false;
    private Activity mActivity;
    private Location lastLocation = null; // store last location


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.vWidth = w;
        this.vHeight = h;
    }


    public MSurfaceView(Context context, Activity activity) {
        super(context);
        //mContext = context;
        mActivity = activity;
        mSurfaceHolder = getHolder();
        mPaint = new Paint();
        mPaint.setColor(Color.DKGRAY);
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        startLocationUpdates();
    }

    public void pause() {
        mRunning = false;
        try {
            // Stop the thread (rejoin the main thread)
            mGameThread.join();
        } catch (InterruptedException e) {
        }
    }

    public void resume() {
        mRunning = true;
        mGameThread = new Thread(this);
        mGameThread.start();
    }

    @Override
    public void run() {
        Canvas canvas;

        while(mRunning){
            if (mSurfaceHolder.getSurface().isValid()) {
                canvas = mSurfaceHolder.lockCanvas();
                canvas.save();
                canvas.drawColor(Color.GREEN);

                // determine min max of checkpoints
                ArrayList<Double> xs = new ArrayList<Double>();
                ArrayList<Double> ys = new ArrayList<Double>();

                for(CheckPoint p : CheckPoint.getTestPoints()){
                    xs.add(p.getLatitude());
                    ys.add(p.getLongitude());
                }
                Collections.sort(xs);
                Collections.sort(ys);

                canvas.translate(tx, ty);
                canvas.scale(mScaleFactor, mScaleFactor);//sx , sy);


                double dx = xs.get(xs.size()-1) - xs.get(0);
                double dy = ys.get(ys.size()-1) - ys.get(0);

                canvas.drawColor(Color.WHITE);

                for(CheckPoint p : CheckPoint.getTestPoints()){
                    Log.wtf("TAG", "Numerator" + (p.getLatitude() - xs.get(0)) + " denom: " + dx * vWidth);
                    Log.wtf("TAG", "" + ((p.getLatitude() - xs.get(0)) / dx * vWidth));
                    float newX = (float)((p.getLatitude() - xs.get(0)) / dx * vWidth);
                    float newY = (float)((p.getLongitude() - ys.get(0)) / dy * vHeight - vHeight/2 );
                    mPaint.setColor(Color.GRAY);
                    canvas.drawCircle(newX, newY, vWidth * 0.2f, mPaint);
                }

                if(lastLocation != null){
                    float newX = (float)((lastLocation.getLatitude() - xs.get(0)) / dx * vWidth);
                    float newY = (float)((lastLocation.getLongitude() - ys.get(0)) / dy * vHeight - vHeight/2 );
                    mPaint.setColor(Color.GREEN);
                    canvas.drawCircle(newX, newY, vWidth * 0.2f, mPaint);
                }
                else{
                    Log.wtf("MYTAG", "Null last location");
                    //startLocationUpdates();
                }
                canvas.restore();
                mSurfaceHolder.unlockCanvasAndPost(canvas);
            }
            try {
                    Thread.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateT(float x, float y){
        float speed = 0.7f;

        if(first){
            px = x;
            py = y;
            first = false;
        }else {
            tx += (x - px)*speed;
            ty += (y - py) * speed;
            px = x;
            py = y;
            invalidate();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Let the ScaleGestureDetector inspect all events.
        /*if(mScaleDetector.onTouchEvent(event)){
            return true;
        }*/
        mScaleDetector.onTouchEvent(event);

        float x = event.getX();
        float y = event.getY();

        // Invalidate() is inside the case statements because there are
        // many other motion events, and we don't want to invalidate
        // the view for those.
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                updateT(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                updateT(x, y);
                break;
            case MotionEvent.ACTION_UP:
                first = true;
                break;
            default:
                // Do nothing.
        }
        return true;
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
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        tx += v;
        ty += v1;
        return false;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        return false;
    }

    private class ScaleListener
            extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mScaleFactor *= detector.getScaleFactor();

            // Don't let the object get too small or too large.
            mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 5.0f));

            invalidate();
            return true;
        }
    }

    protected LocationRequest createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(50);
        mLocationRequest.setFastestInterval(40);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return mLocationRequest;
    }


    private void startLocationUpdates() {
        //Log.wtf("TAG", "starting to get the location updates");
        LocationCallback mLocationCallback;

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

        if(mActivity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //Log.wtf("TAG", "requesting permission");
            ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 111);
        } else {
            //Log.wtf("TAG", "requesting location updatess");
            FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(mActivity);
            mFusedLocationClient.requestLocationUpdates(createLocationRequest(), mLocationCallback, null);
        }
    }
}
