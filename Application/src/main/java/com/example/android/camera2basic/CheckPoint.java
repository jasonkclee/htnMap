package com.example.android.camera2basic;

import java.util.ArrayList;
import java.util.List;

public class CheckPoint {
    private final double DIST_THRESHOLD = 0.0000405; // from previous testing, variation of points * 2
    private double lat, lon;
    public double getLatitude(){return lat;}
    public double getLongitude(){return lon;}

    public CheckPoint(double lat, double lon){
        this.lat = lat;
        this.lon = lon;
    }

    public double getX(){
        double mapWidth    = 200;
        double x = (lon+180)*(mapWidth/360);
        return x;
    }

    public double getY(){
        double mapWidth    = 200;
        double mapHeight   = 100;
        double latRad = lat*Math.PI/180;
// get y value
        double mercN = Math.log(Math.tan((Math.PI/4)+(latRad/2)));
        double y     = (mapHeight/2)-(mapWidth*mercN/(2*Math.PI));
        return y;
    }
/*
    public double[] getXY(){

        double mapWidth    = 200;
        double mapHeight   = 100;

// get x value
        double x = (lon+180)*(mapWidth/360);

// convert from degrees to radians
        double latRad = lat*Math.PI/180;

// get y value
        double mercN = Math.log(Math.tan((Math.PI/4)+(latRad/2)));
        double y     = (mapHeight/2)-(mapWidth*mercN/(2*Math.PI));
        return new double[] {x, y};
    }*/

    /* Return if the given lat/long is close enough to checkpoint */
    public boolean atCheckPoint(double mLat, double mLon){
        double dx = mLat - lat;
        double dy = mLon - lon;
        return Math.sqrt(dx * dx + dy*dy) < DIST_THRESHOLD;
    }

    private static CheckPoint[] testPoints = null;

    public static CheckPoint[] getTestPoints(){
        if(testPoints == null){
            testPoints = new CheckPoint[]{
                    new CheckPoint(43.4725764,-80.5397156),
                    new CheckPoint(43.4728327,-80.5399427),
                    new CheckPoint(43.4729086,-80.5395343)
            };
        }
        return testPoints;
    }


}
