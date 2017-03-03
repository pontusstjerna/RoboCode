package baver;

import pontus.Vector2D;
import robocode.*;

import java.awt.*;
import java.awt.geom.Point2D;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;


/**
 * Created by pontu on 2017-02-26.
 */
public class BaverMain extends AdvancedRobot {

    private Radar radar;
    private Gun gun;

    private final int LOCK_TIMEOUT = 60;
    private static final int MAX_DISTANCE = 400;
    private static final int MIN_DISTANCE = 100;
    private static final double WALL_LIMIT = 100;

    private int lockTicks = LOCK_TIMEOUT;
    private int dir = 1;
    private double oldEnemyEnergy;

    private Point2D.Double enemyPos = new Point2D.Double();
    private Random rand;

    private List<Shot> shots;

    @Override
    public void run() {
        setColors(new Color(255, 196, 13),
                new Color(29, 29, 29),
                new Color(0, 52, 158)); // body,gun,radar

        setAdjustRadarForGunTurn(true);
        setAdjustRadarForRobotTurn(true);
        setAdjustGunForRobotTurn(true);

        radar = new Radar(this);
        gun = new Gun(this);
        shots = loadPreviousShots();
        rand = new Random();

        while (true) {
            if (lockTicks >= LOCK_TIMEOUT) {
                setTurnRadarRight(360);
            } else {
                lockTicks++;
            }

            shots.stream().filter(s -> s.getState() == Shot.states.IN_AIR).forEach(s -> s.addTick());

            execute();
        }
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        radar.lockOnTarget(e);
        if (detectShot(e))
            dodgeBullet();

        updateEnemyPos(e);
        //avoidWalls(e);
        //setTurnRight((e.getBearing() - 90));
        keepDistance(e);
        gun.fire(e);
        gun.lockToEnemy(e);

        shots.stream().filter(s -> s.getState() == Shot.states.IN_AIR).forEach(s -> s.addTick());

        scan();
    }

    @Override
    public void onHitByBullet(HitByBulletEvent e) {
        if (shots.stream().filter(s -> e.getTime() == s.getTime()).findFirst().isPresent())
            shots.stream().filter(s -> e.getTime() == s.getTime()).findFirst().get().setHit(true, e.getBullet());
        else {
            System.out.println("Unregistered shot hit!");
            //   shots.add(new Shot(e));
        }
    }

    @Override
    public void onHitWall(HitWallEvent e) {
        dir = -dir;
    }

    @Override
    public void onDeath(DeathEvent e) {
        System.out.println("Shots hit: " + shots.stream().filter(s -> s.getState() == Shot.states.HIT).count());
        System.out.println("Shots missed: " + shots.stream().filter(s -> s.getState() == Shot.states.MISS).count());

        //Remove missed
        List toRemove = new ArrayList<>();
        shots.stream().filter(s -> s.getState() == Shot.states.MISS).forEach(s -> toRemove.add(s));
        shots.removeAll(toRemove);

        try {
            RobocodeFileOutputStream fout = new RobocodeFileOutputStream(getDataFile("shots"));
            ObjectOutputStream oos = new ObjectOutputStream(fout);
            oos.writeObject(shots);
            fout.close();
            oos.close();

        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onPaint(Graphics2D g) {
        g.setColor(new Color(255, 0, 0));

        if (shots.size() == 0)
            return;

        Point2D.Double b = getExpectedImpact(shots.get(shots.size() - 1));
        g.fillRoundRect((int) b.getX(), (int) b.getY(), 10, 10, 10, 10);

        g.setColor(new Color(22, 31, 255));
        g.fillRoundRect((int) enemyPos.getX(), (int) enemyPos.getY(), 5, 5, 5, 5);

        gun.paintGun(g);
    }

    private boolean detectShot(ScannedRobotEvent e) {
        boolean fired = e.getEnergy() < oldEnemyEnergy;

        if (fired) {
            System.out.println("Shot registered. Shots: " + shots.size());
            Shot shot = new Shot(e, (Point2D.Double) enemyPos.clone(), this);
            shots.add(shot);
        }

        oldEnemyEnergy = e.getEnergy();

        return fired;
    }

    private void dodgeBullet() {
        if (shots.size() == 0)
            return;

        Point2D.Double b = getExpectedImpact(shots.get(shots.size() - 1));

        Vector2D rb = new Vector2D(b.getX(), b.getY());
        double bearing = Util.get180(getHeading() - rb.getHeading());

        if (bearing < 90 && bearing >= -90)
            dir = -1;
        else
            dir = 1;
    }

    private void avoidWalls(ScannedRobotEvent e) {
        double distanceToWall = getDistanceToWall();

        if (distanceToWall < WALL_LIMIT || e.getEnergy() == 0) { //Turn to middle!
            /*if(getAngleToMiddle() > 90 || getAngleToMiddle() < -90){ //If too close, break
                setMaxVelocity(8 - 7*(1/getDistanceToWall()));
            }*/

            setMaxVelocity(8 - 7 * (1 / getDistanceToWall()));
            setTurnRight(getAngleToMiddle());
            setAhead(WALL_LIMIT * dir);
        }
    }

    private void keepDistance(ScannedRobotEvent e) {
        if (getDistanceToWall() > WALL_LIMIT || e.getEnergy() == 0) { //If not close to any wall
            setMaxVelocity(8);
            if (e.getDistance() > MAX_DISTANCE || e.getEnergy() == 0) { //If too far away or disabled
                setTurnRight(e.getBearing() * dir - 20);
            } else if (e.getDistance() < MIN_DISTANCE) { //If too close to the enemy
                setTurnRight(e.getBearing() * dir - 160);
            } else { //If in the perfect distance interval
                setTurnRight((e.getBearing() - 90));
            }
        } else { //Turn to middle!
            if (getAngleToMiddle() > 90 || getAngleToMiddle() < -90) { //If too close, break
                setMaxVelocity(8 - 7 * (1 / getDistanceToWall()));
            }
            setTurnRight(getAngleToMiddle());
        }
        setAhead((rand.nextInt(200) + 70) * dir);
    }

    private void updateEnemyPos(ScannedRobotEvent e) {
        enemyPos.x = getX() + e.getDistance() * Math.sin(getRadarHeadingRadians());
        enemyPos.y = getY() + e.getDistance() * Math.cos(getRadarHeadingRadians());
        //enemyPos.x = getX() - e.getDistance()*Math.sin(getHeadingRadians() - e.getBearingRadians());
        //enemyPos.y = getY() - e.getDistance()*Math.cos(getHeadingRadians() - e.getBearingRadians());
    }

    private Shot getBestMatchedShot(Shot shot) {
        Shot closest;

        List<Shot> hitShots = shots.stream().filter(s -> s.getState() == Shot.states.HIT).collect(Collectors.toList());

        if (hitShots.size() > 0)
            closest = hitShots.get(0);
        else
            return null;

        for (Shot s : hitShots) {
            if (shot.getDistance(s) < closest.getDistance(s))
                closest = s;
        }

        return closest;
    }

    private Point2D.Double getExpectedImpact(Shot justFired) {
        Shot similarShot = getBestMatchedShot(justFired);

        if (similarShot == null)
            return justFired.getRobotPointOfFire(); //Basically move the fk out

        return new Point2D.Double(
                justFired.getRobotPointOfFire().getX() + similarShot.getBullet().getX() - similarShot.getRobotPointOfFire().getX(),
                justFired.getRobotPointOfFire().getY() + similarShot.getBullet().getY() - similarShot.getRobotPointOfFire().getY()
        );
    }

    public double get180(double angle) {
        angle = angle % 360;
        if (angle > 180 && angle > 0) {
            return angle - 360;
        } else if (angle < -180) {
            return angle + 360;
        }
        return angle;
    }

    private double getDistanceToWall() {
        double[] distances = {getX(), getY(), getBattleFieldWidth() - getX(), getBattleFieldHeight() - getY()};
        double distance = distances[0];

        for (int i = 0; i < distances.length; i++) {
            if (distances[i] < distance) {
                distance = distances[i];
            }
        }

        return distance;
    }

    private double getAngleToMiddle() {
        double dx = (getBattleFieldWidth() / 2) - getX();
        double dy = (getBattleFieldHeight() / 2) - getY();

        Vector2D toMiddle = new Vector2D(dx, dy);

        double angle = get180(toMiddle.getHeading() - get180(getHeading()));
        return angle;
    }

    void refreshLock() {
        lockTicks = 0;
    }

    private List<Shot> loadPreviousShots() {
        List<Shot> shots = null;


        if (getRoundNum() > 0) {
            try {
                FileInputStream fout = new FileInputStream(getDataFile("shots"));
                ObjectInputStream ois = new ObjectInputStream(fout);
                shots = (List<Shot>) ois.readObject();
                fout.close();
                ois.close();
                System.out.println(shots.size() + " shots successfully loaded.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (shots == null)
            shots = new ArrayList<>();

        return shots;
    }

}
