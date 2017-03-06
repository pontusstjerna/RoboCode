package baver;

import robocode.Bullet;
import robocode.HitByBulletEvent;
import robocode.ScannedRobotEvent;

import java.awt.geom.Point2D;
import java.io.Serializable;

/**
 * Created by pontu on 2017-02-27.
 */
public class Shot implements Serializable{

    public enum states {IN_AIR, HIT, MISS}

    private final int IN_AIR_TIMEOUT = 60; //2 sec @ 30 tps
    private states state;

    private double bearing, velocity;
    private double distance = -1;
    private Bullet bullet = null;
    private int alive = 0;
    private long firedTime = 0;
    private Point2D.Double enemyPointOfFire;
    private Point2D.Double robotPointOfFire;
    private double robotVelocity;

    public Shot(ScannedRobotEvent e, Point2D.Double enemyPos, BaverMain robot){
        state = states.IN_AIR;
        bearing = e.getBearing();
        velocity = e.getVelocity();
        distance = e.getDistance();
        firedTime = e.getTime();
        enemyPointOfFire = enemyPos;
        robotPointOfFire = new Point2D.Double(robot.getX(), robot.getY());
        robotVelocity = robot.getVelocity();
    }

    public Shot(HitByBulletEvent e){
        state = states.HIT;
        bearing = e.getBearing();
        velocity = e.getVelocity();
        distance = -1;
        bullet = e.getBullet();
    }

    public Shot(ScannedRobotEvent e, Point2D.Double enemyPos, BaverMain robot, Bullet b){
        this(e, enemyPos, robot);
        bullet = b;
    }

    public void setHit(Bullet b){
        setHit();
        bullet = b;
    }

    public void setHit(){
        state = states.HIT;
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

    public Bullet getBullet() {return bullet; }

    public Point2D.Double getEnemyPointOfFire(){
        return enemyPointOfFire;
    }

    public Point2D.Double getRobotPointOfFire(){
        return robotPointOfFire;
    }

    public double getDistance(Shot shot){
        double dBearing = getBearing() - shot.getBearing();
        double dVelocity = getVelocity() - shot.getVelocity();
        double dDistance = 0;
        if(distance != -1 && shot.getDistance() != -1)
            dDistance = distance - shot.getDistance();
        double dRobVel = robotVelocity - shot.robotVelocity;

        return Math.sqrt(dBearing*dBearing + dVelocity*dVelocity + dDistance*dDistance + dRobVel*dRobVel);
    }

    states getState(){
        return state;
    }

    void update(){
        alive++;

        if(alive > IN_AIR_TIMEOUT){
            if(bullet != null){
                if(bullet.getVictim().isEmpty())
                    state = states.MISS;
            }else
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
