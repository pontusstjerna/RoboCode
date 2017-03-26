package baver;

import Util.Util;
import Util.Vector2D;
import robocode.*;

import baver.WeightSet.Weights;

import java.awt.*;
import java.awt.geom.Point2D;
import java.io.Serializable;
import java.sql.Ref;

/**
 * Created by pontu on 2017-02-27.
 */
public class Shot implements Serializable{

    public enum states {IN_AIR, HIT, MISS}

    private final int IN_AIR_TIMEOUT = 60; //2 sec @ 30 tps
    private states state;

    private double velocity;
    private double distance = -1;
    private Bullet bullet = null;
    private int alive = 0;
    private long firedTime = 0;
    private Point2D.Double robotPointAtFire;
    private Point2D.Double enemyPointAtFire;
    private double robotVelocity;
    private double turretBearing = -1;
    private double deltaHeading;
    private double enemyDeltaAngle = -1;
    private double distanceToImpact = -1;
    private double power = 0;
    private double robDir = 0;
    private double enemyDir = 0;
    private double relativeBearing = -1;
    private double maxDistance = 0; //I want it to crash if division by zero occurs
    private boolean firedByMachineLearning = false;

    //Primary constructor
    public Shot(ScannedRobotEvent e, AdvancedRobot robot){
        state = states.IN_AIR;
        //radarBearing = Util.get180(e.getBearing() - Util.get180(robot.getRadarHeading()));
        velocity = e.getVelocity();
        distance = e.getDistance();
        firedTime = e.getTime();
        robotPointAtFire = new Point2D.Double(robot.getX(), robot.getY());
        robotVelocity = robot.getVelocity();
        deltaHeading = Util.get180(robot.getHeading() - e.getHeading());
        turretBearing = Util.get180(Util.get180(e.getBearing()) - Util.get180(robot.getGunHeading() - robot.getHeading()));
        enemyDir = Math.signum(e.getVelocity());
        robDir = Math.signum(robot.getVelocity());
        maxDistance = Point.distance(0,0, Reference.BATTLEFIELD_WIDTH, Reference.BATTLEFIELD_HEIGHT);
    }

    //Mainly for registering enemy shots
    public Shot(ScannedRobotEvent e, Point2D.Double enemyPos, double power, AdvancedRobot robot){
        this(e, robot);
        enemyPointAtFire = enemyPos;
        this.power = Math.abs(power);
        relativeBearing = e.getBearing()*robDir;
    }

    //Getting hit by enemy shots
    public Shot(HitByBulletEvent e){
        state = states.HIT;
        //radarBearing = e.getBearing();
        velocity = e.getVelocity();
        distance = -1;
        bullet = e.getBullet();
    }

    //Registering fired shots
    public Shot(ScannedRobotEvent e, AdvancedRobot robot, Bullet b, boolean usingMachineLearning){
        this(e, robot);
        firedByMachineLearning = usingMachineLearning;
        bullet = b;
    }

    public void setHit(Bullet b){
        setHit();
        bullet = b;
    }

    public void setRobotHit(Bullet b, Point2D.Double enemyPointAtHit){
        setHit(b);
        double ER_bearing = Vector2D.getHeading(
                robotPointAtFire.getX() - enemyPointAtFire.getX(),
                robotPointAtFire.getY() - enemyPointAtFire.getY());

        Vector2D eb = new Vector2D(b.getX() - enemyPointAtFire.getX(),
                b.getY() - enemyPointAtFire.getY());

        double EB_bearing = eb.getHeading();

        enemyDeltaAngle = Util.get180(ER_bearing - EB_bearing);
       // System.out.println("Edv: " + enemyDeltaAngle);
        distanceToImpact = Vector2D.getLength(eb);
    }

    public void setHit(){
        state = states.HIT;
    }

    public void setMiss() {state = states.MISS; }

    public double getVelocity(){
        return velocity;
    }

    public double getDeltaHeading() {return deltaHeading;}

    public double getDistanceBetweenRobots(){
        return distance;
    }

    public double getRelativeBearing() {return relativeBearing;}

    public Bullet getBullet() {return bullet; }

    public double getTurretBearing(){
        return turretBearing;
    }

    public double getPower(){
        return power == 0 && bullet != null ? bullet.getPower() : power;
    }

    public Point2D.Double getEnemyPointAtFire(){
        return enemyPointAtFire;
    }

    public Point2D.Double getRobotPointAtFire(){
        return robotPointAtFire;
    }

    public double getDistanceToImpact(){
        return distanceToImpact;
    }

    public double getEnemyDeltaAngle(){
        return enemyDeltaAngle;
    }

    public double getRobDir(){
        return robDir;
    }

    public double getEnemyDir() {return enemyDir;}

    public double getFriendlyVelocity(){ return robotVelocity; }

    public boolean isFiredByMachineLearning(){ return firedByMachineLearning;}

    public double getDistance(Shot shot, WeightSet weights){
        double dVelocity = (velocity - shot.velocity)/ (Rules.MAX_VELOCITY*2);
        double dRobVel = (robotVelocity - shot.robotVelocity)/(Rules.MAX_VELOCITY*2);
        double dDistance = 0;
        double dTurretBearing = 0;
        double dDeltaHeading = (deltaHeading - shot.deltaHeading)/Reference.MAX_DELTA_HEADING;
        double dDir = (robDir - shot.robDir)/(double)2;
        double dEnemyDir = (enemyDir - shot.enemyDir)/(double)2;
        double dBearing = 0;

        //Here everything should be normalized


        if(distance != -1 && shot.distance != -1)
            dDistance = (distance - shot.distance)/maxDistance;
        if(turretBearing != -1 && shot.turretBearing != -1)
            dTurretBearing = (turretBearing - shot.turretBearing)/Reference.MAX_DELTA_HEADING;
        if(relativeBearing != -1 && shot.relativeBearing != -1)
            dBearing = (relativeBearing - shot.relativeBearing)/Reference.MAX_DELTA_HEADING*2;


        //Add weights
        dVelocity *= weights.getWeight(Weights.ENEMY_VELOCITY);
        dDistance *= weights.getWeight(Weights.DISTANCE_BETWEEN_ROBOTS);
        dRobVel *= weights.getWeight(Weights.FRIENDLY_VELOCITY);
        dTurretBearing *= weights.getWeight(Weights.BEARING_FROM_TURRET);
        dDeltaHeading *= weights.getWeight(Weights.HEADING_DIFFERENCE);
        dDir *= weights.getWeight(Weights.FRIENDLY_DIRECTION);
        dBearing *= weights.getWeight(Weights.BEARING_DIFFERENCE);
        dEnemyDir *= weights.getWeight(Weights.ENEMY_DIRECTION);

       // System.out.println("dvel: " + dVelocity + " dDist: " + dDistance + " dRobVel: " + dRobVel + " dTurrBear: " + dTurretBearing);
       // System.out.println("dHead: " + dDeltaHeading + " dDir: " + dDir + " dBear: " + dBearing + " dEDir: " + dEnemyDir);

        return Math.sqrt(dVelocity*dVelocity + dDistance*dDistance + dRobVel*dRobVel +
        dTurretBearing*dTurretBearing + dDeltaHeading*dDeltaHeading + dDir*dDir + dBearing*dBearing + dEnemyDir*dEnemyDir);
    }

    public double getDistance(Shot shot){
        return getDistance(shot, new WeightSet());
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
