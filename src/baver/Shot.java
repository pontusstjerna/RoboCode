package baver;

import robocode.Bullet;
import robocode.ScannedRobotEvent;

/**
 * Created by pontu on 2017-02-27.
 */
public class Shot {

    private final int IN_AIR_TIMEOUT = 150; //5 sec @ 30 tps
    private enum states {IN_AIR, HIT, MISS}
    private states state;

    private double bearing, velocity, distance;
    private Bullet bullet = null;
    private int alive = 0;

    public Shot(double bearing, double velocity, double distance){
        state = states.IN_AIR;
        this.bearing = bearing;
        this.velocity = velocity;
        this.distance = distance;
    }

    public void setHit(boolean bulletHit, Bullet b){
        if(bulletHit)
            state = states.HIT;

        bullet = b;
    }

    public double getBearing(){
        return bearing;
    }

    public double getVelocity(){
        return velocity;
    }

    public double getDistance(){
        return distance;
    }

    public boolean isInAir(){
        return state == states.IN_AIR;
    }

    public void addTick(){
        alive++;
    }
}
