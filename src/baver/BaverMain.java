package baver;

import Util.Util;
import Util.Vector2D;
import robocode.*;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.Random;


/**
 * Created by pontu on 2017-02-26.
 */
public class BaverMain extends AdvancedRobot {

    private Radar radar;
    private AngleGun angleGun;
    private LearningGun learningGun;
    private AvoidanceSystem avoidanceSystem;

    private int lockTicks = Reference.LOCK_TIMEOUT;
    private int dir = 1;
    private Point2D.Double stick;

    private Random rand;

    @Override
    public void run() {
        setColors(new Color(255, 196, 13),
                new Color(29, 29, 29),
                new Color(0, 52, 158)); // body,angleGun,radar

        setAdjustRadarForGunTurn(true);
        setAdjustRadarForRobotTurn(true);
        setAdjustGunForRobotTurn(true);

        radar = new Radar(this);
        angleGun = new AngleGun(this);
        learningGun = new LearningGun(this);
        avoidanceSystem = new AvoidanceSystem(this);

        rand = new Random();
        stick = new Point2D.Double(getX(), getY());

        Reference.BATTLEFIELD_WIDTH = getBattleFieldWidth();
        Reference.BATTLEFIELD_HEIGHT = getBattleFieldHeight();

        while (true) {
            if (lockTicks >= Reference.LOCK_TIMEOUT) {
                setTurnRadarRight(360);
             //   driveAround();
            } else {
                lockTicks++;
            }

            avoidanceSystem.update();
//            updateStickPos();

            execute();
        }
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        radar.lockOnTarget(e);

        //Avoidance
        dir = avoidanceSystem.getNewDirection(e, dir);
   //     updateStickPos();

        //Moving
        updateEnemyPos(e);
        keepDistance(e);

        //Shooting
        attack(e);

        avoidanceSystem.update();
        scan();
    }

    @Override
    public void onBulletHit(BulletHitEvent e) {
        learningGun.registerBulletHit(e.getBullet());
    }

    @Override
    public void onBulletMissed(BulletMissedEvent e) {
        learningGun.registerBulletMiss(e.getBullet());
    }

    @Override
    public void onHitByBullet(HitByBulletEvent e) {
        avoidanceSystem.registerBulletHit(e);
    }

    @Override
    public void onHitWall(HitWallEvent e) {
        setStop();
        double angToMid = getBearingToMiddle();
        setTurnRight(angToMid);
        setAhead(Reference.WALL_LIMIT);
        setResume();
    }

    @Override
    public void onHitRobot(HitRobotEvent e) {
        setAhead(200 * dir);
        setFire(3);
    }

    @Override
    public void onDeath(DeathEvent e) {
        avoidanceSystem.saveShots();
        learningGun.saveShots();
    }

    @Override
    public void onPaint(Graphics2D g) {
        g.setColor(Color.white);
        g.drawString("Hit shots: " + learningGun.getHitShotsCount(), 10, 90);
        g.drawString("Overall hit rate: " + (int) (learningGun.getHitRate() * 100) + "%", 10, 75);
        g.drawString("AngleGun hit rate: " + (int) (learningGun.getAngleGunHitRate() * 100) + "%", 10, 60);
        g.drawString("LearningGun hit rate: " + (int) (learningGun.getLearningGunHitRate() * 100) + "%", 10, 45);

        //Sticks
      //  g.drawLine((int) getX(), (int) getY(), (int) stick.getX(), (int) stick.getY());

        avoidanceSystem.paintAvoidanceSystem(g);

        if (angleGun.isActive())
            angleGun.paintGun(g);
        else
            learningGun.paintExpectations(g);
    }

    private void attack(ScannedRobotEvent e) {
        angleGun.setActive(learningGun.getHitShotsCount() < Reference.REQUIRED_HIT_LIMIT ||
                learningGun.getMissCount() > 3 || learningGun.getHighestHitDistance() < e.getDistance());
        learningGun.setActive(!angleGun.isActive());

        if (angleGun.isActive()) {
            Bullet b = angleGun.fire(e);
            if (b != null)
                learningGun.registerBullet(e, b, false);
            angleGun.lockToEnemy(e);
        } else
            learningGun.aimAndFire(e);
    }

    private void keepDistance(ScannedRobotEvent e) {
        double distWall = getDistanceToWall(getX(), getY());
        double angToMid = getBearingToMiddle();
        double percentToWall = getDistanceToMiddle() / distWall;
        double additionalTurnToMiddle = angToMid * percentToWall * 0.5;

        //System.out.println("Ang to mid: " + angToMid);

        if (e.getEnergy() == 0) { //If component disabled
            clearAllEvents();
            setTurnRight(e.getBearing()*dir);
            if (e.getBearing() < 3) {
                setAhead(500 * dir);
            }
        } else {
            setTurnRight((e.getBearing() - 90 + additionalTurnToMiddle));
        }
        setAhead((rand.nextInt(200) + 70) * dir);
    }

    private void updateStickPos() {
        stick.setLocation(
                getX() + Reference.STICK_LENGTH * dir * Math.sin(getHeadingRadians()),
                getY() + Reference.STICK_LENGTH * dir * Math.cos(getHeadingRadians()));
    }

    private void driveAround(){
        double distWall = getDistanceToWall(getX(), getY());
        double angToMid = getBearingToMiddle(stick.getX(), stick.getY());
        double percentToWall = getDistanceToMiddle(getX(), getY()) / distWall;
        double additionalTurnToMiddle = angToMid * percentToWall;

        setTurnRight(additionalTurnToMiddle);

        setAhead((rand.nextInt(200) + 70) * dir);
    }

    private void updateEnemyPos(ScannedRobotEvent e) {
        avoidanceSystem.updateEnemyPos(getX() + e.getDistance() * Math.sin(getRadarHeadingRadians()),
                getY() + e.getDistance() * Math.cos(getRadarHeadingRadians()));
    }

    private double getDistanceToWall() {
        return getDistanceToWall(getX(), getY());
    }

    private double getDistanceToWall(double x, double y) {
        double[] distances = {x, y, getBattleFieldWidth() - x, getBattleFieldHeight() - y};
        double distance = distances[0];

        for (int i = 0; i < distances.length; i++) {
            if (distances[i] < distance) {
                distance = distances[i];
            }
        }

        return distance;
    }

    private double getDistanceToMiddle() {
        return getDistanceToMiddle(getX(), getY());
    }

    private double getDistanceToMiddle(double x, double y) {
        return Point.distance(getBattleFieldWidth() / 2, getBattleFieldHeight() / 2, x, y);
    }

    private double getBearingToMiddle() {
       return getBearingToMiddle(getX(), getY());
    }

    private double getBearingToMiddle(double x, double y){
        double dx = (getBattleFieldWidth() / 2) - x;
        double dy = (getBattleFieldHeight() / 2) - y;

        Vector2D toMiddle = new Vector2D(dx, dy);

        double angle = Util.get180(toMiddle.getHeading() - Util.get180(getDirHeading()));
        return angle;
    }

    void refreshLock() {
        lockTicks = 0;
    }

    private double getDirHeading(){
        if(dir == 1){
            return super.getHeading();
        }else{
            return Util.get180(super.getHeading() + 180);
        }
    }
}
