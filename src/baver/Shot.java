package baver;

import robocode.Bullet;
import robocode.HitByBulletEvent;
import robocode.ScannedRobotEvent;

/**
 * Created by pontu on 2017-02-27.
 */
public class Shot {

    public enum states {IN_AIR, HIT, MISS}

    private final int IN_AIR_TIMEOUT = 150; //5 sec @ 30 tps
    private states state;

    private double bearing, velocity;
    private double distance = -1;
    private Bullet bullet = null;
    private int alive = 0;
    private long firedTime = 0;

    public Shot(ScannedRobotEvent e){
        state = states.IN_AIR;
        bearing = e.getBearing();
        velocity = e.getVelocity();
        distance = e.getDistance();
        firedTime = e.getTime();
    }

    public Shot(HitByBulletEvent e){
        state = states.HIT;
        bearing = e.getBearing();
        velocity = e.getVelocity();
        distance = -1;
        bullet = e.getBullet();
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

    states getState(){
        return state;
    }

    void addTick(){
        alive++;

        if(alive > IN_AIR_TIMEOUT){
            state = states.MISS;
        }
    }

    int getTimeAlive(){
        return alive;
    }

    long getTime(){
        return firedTime + alive;
    }
}
