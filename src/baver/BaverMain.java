package baver;

import pontus.Vector2D;
import robocode.*;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Created by pontu on 2017-02-26.
 */
public class BaverMain extends AdvancedRobot {

    private Radar radar;
    private AngleGun angleGun;
    private LearningGun learningGun;

    private int lockTicks = Reference.LOCK_TIMEOUT;
    private int dir = 1;
    private double oldEnemyEnergy;
    private double enemyDeltaEnergy = 0;

    private Point2D.Double enemyPos = new Point2D.Double();
    private Random rand;

    private List<Shot> enemyShots;

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
        enemyShots = loadPreviousShots();
        rand = new Random();

        while (true) {
            if (lockTicks >= Reference.LOCK_TIMEOUT) {
                setTurnRadarRight(360);
            } else {
                lockTicks++;
            }

            enemyShots.stream().filter(s -> s.getState() == Shot.states.IN_AIR).forEach(s -> s.update());

            execute();
        }
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        radar.lockOnTarget(e);

        //Avoidance
        avoid(e);

        //Moving
        updateEnemyPos(e);
        keepDistance(e);

        //Shooting
         attack(e);

        enemyShots.stream().filter(s -> s.getState() == Shot.states.IN_AIR).forEach(s -> s.update());
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
        Optional<Shot> registeredShot = enemyShots.stream().filter(s -> e.getTime() == s.getTime()).findFirst();
        if (registeredShot.isPresent())
            registeredShot.get().setRobotHit(e.getBullet(), (Point2D.Double) enemyPos.clone());
        else {
            System.out.println("Unregistered shot hit!");
            //   enemyShots.add(new Shot(e));
        }
    }

    @Override
    public void onHitWall(HitWallEvent e) {
        dir = -dir;
    }

    @Override
    public void onHitRobot(HitRobotEvent e) {
        setAhead(200 * dir);
        setFire(3);
    }

    @Override
    public void onDeath(DeathEvent e) {
        System.out.println("Enemy shots hit: " + enemyShots.stream().filter(s -> s.getState() == Shot.states.HIT).count());
        System.out.println("Enemy shots missed: " + enemyShots.stream().filter(s -> s.getState() == Shot.states.MISS).count());

        //Remove missed
        List toRemove = new ArrayList<>();
        enemyShots.stream().filter(s -> s.getState() == Shot.states.MISS).forEach(s -> toRemove.add(s));
        enemyShots.removeAll(toRemove);

        try {
            RobocodeFileOutputStream fout = new RobocodeFileOutputStream(getDataFile("enemyShots"));
            ObjectOutputStream oos = new ObjectOutputStream(fout);
            oos.writeObject(enemyShots);
            fout.close();
            oos.close();

        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        learningGun.saveShots();
    }

    @Override
    public void onPaint(Graphics2D g) {
        if (enemyShots.size() == 0)
            return;

        double movableDistance = 200;
        double minTolDistance = 3;
        double eDist = enemyPos.distance(getX(), getY())*1.5;
        Vector2D fwd = new Vector2D(movableDistance*Math.sin(getHeadingRadians()), movableDistance*Math.cos(getHeadingRadians()));
        Vector2D rev = new Vector2D(movableDistance*-1*Math.sin(getHeadingRadians()), movableDistance*-1*Math.cos(getHeadingRadians()));
        fwd.paintVector(g, getX(), getY(), Color.GREEN);
        rev.paintVector(g, getX(), getY(), Color.GREEN);

        Shot mostRecent = enemyShots.get(enemyShots.size() - 1);
        for (Shot s : getBestMatchedShots(mostRecent, 10)) {
            Vector2D er = new Vector2D(mostRecent.getRobotPointAtFire().getX() - mostRecent.getEnemyPointAtFire().getX(), mostRecent.getRobotPointAtFire().getY() - mostRecent.getEnemyPointAtFire().getY());
            double ER_bearingRad = er.getHeadingRadians();

            double angle = ER_bearingRad - Math.toRadians(s.getEnemyDeltaAngle());
            double currDistance = mostRecent.getTimeAlive() * (20 - 3 * 3);
            Point2D.Double hitPoint = new Point2D.Double(
                    mostRecent.getEnemyPointAtFire().getX() + currDistance * Math.sin(angle),
                    mostRecent.getEnemyPointAtFire().getY() + currDistance * Math.cos(angle));

            Vector2D eb = new Vector2D(eDist * Math.sin(angle), eDist * Math.cos(angle));

            Point2D.Double intersectionfw = getIntersection(new Point2D.Double(getX(), getY()), mostRecent.getEnemyPointAtFire(), fwd, eb, minTolDistance);
            Point2D.Double intersectionrv = getIntersection(new Point2D.Double(getX(), getY()), mostRecent.getEnemyPointAtFire(), rev, eb, minTolDistance);

            g.setColor(Color.red);
            if(intersectionfw != null){
                g.fillRect((int)intersectionfw.getX(), (int)intersectionfw.getY(), 10, 10);
            }

            if(intersectionrv != null){
                g.fillRect((int)intersectionrv.getX(), (int)intersectionrv.getY(), 10, 10);
            }

            g.setColor(Color.red);
            g.fillRoundRect((int) hitPoint.getX(), (int) hitPoint.getY(), 10, 10, 10, 10);
        }

        g.drawString("Hit rate: " + learningGun.getHitRate() * 100 + "%", 10, 45);

        if (angleGun.isActive())
            angleGun.paintGun(g);
        else
            learningGun.paintExpectations(g);
    }

    private void avoid(ScannedRobotEvent e) {
        if (detectShot(e) && getDistanceToWall() > Reference.WALL_LIMIT)
            dodgeBullet();
    }

    private void attack(ScannedRobotEvent e) {
        angleGun.setActive(learningGun.getHitShots() < 2 || learningGun.getMissCount() > 3);
        learningGun.setActive(!angleGun.isActive());

        if (angleGun.isActive()) {
            Bullet b = angleGun.fire(e);
            if (b != null)
                learningGun.registerBullet(e, b);
            angleGun.lockToEnemy(e);
        } else
            learningGun.aimAndFire(e);
    }

    private Point2D.Double getIntersection(Vector2D a, Vector2D b, Point2D.Double aStart, Point2D.Double bStart){
        //Algorithm from http://stackoverflow.com/questions/563198/how-do-you-detect-where-two-line-segments-intersect

        Vector2D p = new Vector2D(aStart.getX(), aStart.getY());
        Vector2D q = new Vector2D(bStart.getX(), bStart.getY());
        double r = Vector2D.getLength(a);
        double s = Vector2D.getLength(b);

      //  double t = Vector2D.sub(q,p).dot()

        return null;
    }

    private Point2D.Double getIntersection(Point2D.Double startA, Point2D.Double startB, Vector2D a, Vector2D b, double threshold){
        double aHeading = a.getHeadingRadians();
        double bHeading = b.getHeadingRadians();
        for(double i = 0; i < a.getLength(); i += threshold*0.5){
            for(double j = 0; j < b.getLength(); j += threshold*0.5){
                if(Point.distance(
                        startA.getX() + i*Math.sin(aHeading), startA.getY() + i*Math.cos(aHeading),
                        startB.getX() + j*Math.sin(bHeading), startB.getY() + j*Math.cos(bHeading))
                        <= threshold){
                    return new Point2D.Double(startA.getX() + i*Math.sin(aHeading), startA.getY() + i*Math.cos(aHeading));
                }
            }
        }

        return null;
    }

    private boolean detectShot(ScannedRobotEvent e) {
        enemyDeltaEnergy = e.getEnergy() - oldEnemyEnergy;
        boolean fired = enemyDeltaEnergy < 0;

        if (fired) {
            // System.out.println("Shot registered. Shots: " + enemyShots.size());
            Shot shot = new Shot(e, (Point2D.Double) enemyPos.clone(), this);
            enemyShots.add(shot);
        }

        oldEnemyEnergy = e.getEnergy();

        return fired;
    }

    private void dodgeBullet() {
        if (enemyShots.size() == 0)
            return;

        double movableDistance = 200;
        double minTolDistance = 3;
        double eDist = enemyPos.distance(getX(), getY())*1.5;
        Vector2D fwd = new Vector2D(movableDistance*Math.sin(getHeadingRadians()), movableDistance*Math.cos(getHeadingRadians()));
        Vector2D rev = new Vector2D(movableDistance*-1*Math.sin(getHeadingRadians()), movableDistance*-1*Math.cos(getHeadingRadians()));

        Shot mostRecent = enemyShots.get(enemyShots.size() - 1);

        int nFwd = 0;
        int nRev = 0;

        for (Shot s : getBestMatchedShots(mostRecent, 10)) {
            Vector2D eb = getExpectedImpact(mostRecent, s);

            Point2D.Double intersectionfw = getIntersection(new Point2D.Double(getX(), getY()), mostRecent.getEnemyPointAtFire(), fwd, eb, minTolDistance);
            Point2D.Double intersectionrv = getIntersection(new Point2D.Double(getX(), getY()), mostRecent.getEnemyPointAtFire(), rev, eb, minTolDistance);

            if(intersectionfw != null)
                nFwd++;

            if(intersectionrv != null)
                nRev++;
        }

        if(nFwd > nRev){
            dir = -1;
        }else
            dir = 1;
    }

    private void registerShot(Bullet b, ScannedRobotEvent e) {
        if (b == null) return;


    }

    private void keepDistance(ScannedRobotEvent e) {
        double distWall = getDistanceToWall();
        double angToMid = getAngleToMiddle();
        double percentToWall = getDistanceToMiddle() / distWall;
        double additionalTurnToMiddle = angToMid * percentToWall * 0;
        boolean inFront = isInFront(enemyPos.getX(), enemyPos.getY());

        if (distWall > Reference.WALL_LIMIT || e.getEnergy() == 0) { //If not close to any wall
            setMaxVelocity(8);
            if (e.getEnergy() == 0) { //If component disabled
                setTurnRight(e.getBearing() * dir);
                setAhead(500 * dir);
            } else if (e.getDistance() < Reference.MIN_DISTANCE) { //If too close to the enemy
                setTurnRight(e.getBearing() * dir - 160 + additionalTurnToMiddle * dir);
                //if(inFront) dir = -1;
                //else dir = 1;
            } else { //If in the perfect distance interval
                setTurnRight((e.getBearing() - 90 + additionalTurnToMiddle * dir));
            }
        } else { //Turn to middle!
            if (angToMid > 90 && angToMid < -90) { //If too close, break
                //setMaxVelocity(8 - 7 * (1 / getDistanceToWall()));
                dir = -1;
            } else
                dir = 1;
            setTurnRight(angToMid);
            setAhead(Reference.WALL_LIMIT * dir);
        }
        setAhead((rand.nextInt(200) + 70) * dir);
    }

    private void updateEnemyPos(ScannedRobotEvent e) {
        enemyPos.x = getX() + e.getDistance() * Math.sin(getRadarHeadingRadians());
        enemyPos.y = getY() + e.getDistance() * Math.cos(getRadarHeadingRadians());
    }

    private Shot getBestMatchedShot(Shot shot) {
        List<Shot> bestMatched = getBestMatchedShots(shot, 1);

        if (bestMatched.size() > 0)
            return getBestMatchedShots(shot, 1).get(0);
        else
            return null;
    }

    private List<Shot> getBestMatchedShots(Shot shot, int maxSize) {
        Stream<Shot> bestMatched = enemyShots.stream().filter(s -> s.getState() == Shot.states.HIT);

        bestMatched = bestMatched.sorted((x, y) -> x.getDistance(shot) < y.getDistance(shot) ? -1 : 1);
        return bestMatched.limit(maxSize).collect(Collectors.toList());
    }

    private Vector2D getExpectedImpact(Shot justFired, Shot oldShot) {
        double eDist = enemyPos.distance(getX(), getY())*1.5;
        Vector2D er = new Vector2D(justFired.getRobotPointAtFire().getX() - justFired.getEnemyPointAtFire().getX(),
                justFired.getRobotPointAtFire().getY() - justFired.getEnemyPointAtFire().getY());
        double ER_bearingRad = er.getHeadingRadians();

        double angle = ER_bearingRad - Math.toRadians(oldShot.getEnemyDeltaAngle());

        return new Vector2D(eDist * Math.sin(angle), eDist * Math.cos(angle));
    }

    private boolean isInFront(double x, double y) {
        Vector2D rb = new Vector2D(x - getX(), y - getY());
        double bearing = Util.get180(Util.get180(getHeading()) - Util.get180(rb.getHeading()));

        return bearing < 90 && bearing >= -90;
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

    private double getDistanceToMiddle() {
        return Point.distance(getBattleFieldWidth() / 2, getBattleFieldHeight() / 2, getX(), getY());
    }

    private double getAngleToMiddle() {
        double dx = (getBattleFieldWidth() / 2) - getX();
        double dy = (getBattleFieldHeight() / 2) - getY();

        Vector2D toMiddle = new Vector2D(dx, dy);

        double angle = Util.get180(toMiddle.getHeading() - Util.get180(getHeading()));
        return angle;
    }

    void refreshLock() {
        lockTicks = 0;
    }

    private List<Shot> loadPreviousShots() {
        List<Shot> shots = null;


        if (getRoundNum() > 0) {
            try {
                FileInputStream fout = new FileInputStream(getDataFile("enemyShots"));
                ObjectInputStream ois = new ObjectInputStream(fout);
                shots = (List<Shot>) ois.readObject();
                fout.close();
                ois.close();
                System.out.println(shots.size() + " enemyShots successfully loaded.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (shots == null)
            shots = new ArrayList<>();

        return shots;
    }

}
