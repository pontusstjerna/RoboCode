package baver;

import robocode.AdvancedRobot;
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
    private double turretBearing = -1;
    private double deltaHeading;

    //Primary constructor
    public Shot(ScannedRobotEvent e, AdvancedRobot robot){
        state = states.IN_AIR;
        bearing = e.getBearing();
        velocity = e.getVelocity();
        distance = e.getDistance();
        firedTime = e.getTime();
        robotPointOfFire = new Point2D.Double(robot.getX(), robot.getY());
        robotVelocity = robot.getVelocity();
        turretBearing = Util.get180(Util.get180(e.getBearing()) - Util.get180(robot.getGunHeading() - robot.getHeading()));
        deltaHeading = Util.get180(robot.getHeading() - e.getHeading());
    }

    //Mainly for registering enemy shots
    public Shot(ScannedRobotEvent e, Point2D.Double enemyPos, AdvancedRobot robot){
        this(e, robot);
        enemyPointOfFire = enemyPos;
    }

    //Getting hit by enemy shots
    public Shot(HitByBulletEvent e){
        state = states.HIT;
        bearing = e.getBearing();
        velocity = e.getVelocity();
        distance = -1;
        bullet = e.getBullet();
    }

    //Registering fired shots
    public Shot(ScannedRobotEvent e, AdvancedRobot robot, Bullet b){
        this(e, robot);
        bullet = b;
    }

    public void setHit(Bullet b){
        setHit();
        bullet = b;
    }

    public void setHit(){
        state = states.HIT;
    }

    public void setMiss() {state = states.MISS; }

    public double getBearing(){
        return bearing;
    }

    public double getVelocity(){
        return velocity;
    }

    public double getDeltaHeading() {return deltaHeading;}

    public double getDistance(){
        return distance;
    }

    public Bullet getBullet() {return bullet; }

    public double getTurretBearing(){
        return turretBearing;
    }

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
        double dTurretBearing = 0;
        double dDeltaHeading = getDeltaHeading() - shot.getDeltaHeading();

        if(distance != -1 && shot.getDistance() != -1)
            dDistance = distance - shot.getDistance();
        if(turretBearing != -1 && shot.getTurretBearing() != -1)
            dTurretBearing = turretBearing - shot.getTurretBearing();
        double dRobVel = robotVelocity - shot.robotVelocity;

        return Math.sqrt(dBearing*dBearing + dVelocity*dVelocity + dDistance*dDistance + dRobVel*dRobVel +
        dTurretBearing*dTurretBearing + dDeltaHeading*dDeltaHeading);
    }

    states getState(){
        return state;
    }

    void update(){
        if(state != states.IN_AIR)
            return;

        alive++;

        if(alive > IN_AIR_TIMEOUT){
            if(bullet != null){
                if(bullet.getVictim() == null)
                    state = states.MISS;
                else
                    state = states.HIT;
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
