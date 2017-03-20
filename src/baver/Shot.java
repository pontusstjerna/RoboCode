package baver;

import pontus.Vector2D;
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
    private int dir = 0;
    private double relativeBearing = -1;

    //Primary constructor
    public Shot(ScannedRobotEvent e, AdvancedRobot robot){
        state = states.IN_AIR;
        //radarBearing = Util.get180(e.getBearing() - Util.get180(robot.getRadarHeading()));
        velocity = e.getVelocity();
        distance = e.getDistance();
        firedTime = e.getTime();
        robotPointAtFire = new Point2D.Double(robot.getX(), robot.getY());
        robotVelocity = robot.getVelocity();
        turretBearing = Util.get180(Util.get180(e.getBearing()) - Util.get180(robot.getGunHeading() - robot.getHeading()));
        deltaHeading = Util.get180(robot.getHeading() - e.getHeading());
    }

    //Mainly for registering enemy shots
    public Shot(ScannedRobotEvent e, Point2D.Double enemyPos, double power, int dir, AdvancedRobot robot){
        this(e, robot);
        enemyPointAtFire = enemyPos;
        this.power = Math.abs(power);
        this.dir = dir;
        relativeBearing = e.getBearing()*dir;
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
    public Shot(ScannedRobotEvent e, AdvancedRobot robot, Bullet b){
        this(e, robot);
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
        System.out.println("Edv: " + enemyDeltaAngle);
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

    public double getDistance(){
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

    public int getDir(){
        return dir;
    }

    public double getDistance(Shot shot){
   //     double dBearing = getRadarBearing() - shot.getRadarBearing();
        double dVelocity = getVelocity() - shot.getVelocity();
        double dDistance = 0;
        double dTurretBearing = 0;
        double dDeltaHeading = getDeltaHeading() - shot.getDeltaHeading();
        int dDir = dir - shot.getDir();
        double dBearing = 0;

        if(distance != -1 && shot.getDistance() != -1)
            dDistance = distance - shot.getDistance();
        if(turretBearing != -1 && shot.getTurretBearing() != -1)
            dTurretBearing = turretBearing - shot.getTurretBearing();
        double dRobVel = robotVelocity - shot.robotVelocity;
        if(relativeBearing != -1 && shot.getRelativeBearing() != -1)
            dBearing = relativeBearing - shot.getRelativeBearing();

        return Math.sqrt(dVelocity*dVelocity + dDistance*dDistance + dRobVel*dRobVel +
        dTurretBearing*dTurretBearing + dDeltaHeading*dDeltaHeading + dDir*dDir + dBearing*dBearing);
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
