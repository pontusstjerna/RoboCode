package baver;

import Util.Vector2D;
import robocode.AdvancedRobot;
import robocode.HitByBulletEvent;
import robocode.ScannedRobotEvent;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import Util.Util;

/**
 * Created by pontu on 2017-03-27.
 */
public class AvoidanceSystem {

    private AdvancedRobot robot;
    private double oldEnemyEnergy;
    private long lastBulletHitTime = 0;
    private Point2D.Double enemyPos = new Point2D.Double();

    private List<Shot> enemyShots;
    private LinkedList<Shot> inAir;
    private WeightSet avoidWeights;

    private Vector2D avoidanceVector;
    private Point2D.Double avoidanceVectorOrigin;

    public AvoidanceSystem(AdvancedRobot robot) {
        this.robot = robot;
        enemyShots = Util.loadPreviousShots(robot.getDataFile("enemyShots"), robot.getRoundNum());
        avoidWeights = new WeightSet(Reference.AVOIDING_WEIGHTS);
        avoidanceVector = new Vector2D(robot.getX(), robot.getY());
        avoidanceVectorOrigin = new Point2D.Double(robot.getX(), robot.getY());
        inAir = new LinkedList<>();
    }

    public int getNewDirection(ScannedRobotEvent e, int currentDirection) {
        if (detectShot(e)) return dodgeBullet();
        return currentDirection;
    }

    public void update() {
        enemyShots.stream().filter(s -> s.getState() == Shot.states.IN_AIR).forEach(s -> s.update());
        updateAvoidanceVectors();

        Shot s = inAir.peekFirst();
        if (s != null && s.getDistanceFromShooter() > (s.getEnemyPointAtFire().distance(robot.getX(), robot.getY())))
            inAir.remove();
    }

    public void updateEnemyPos(double x, double y) {
        enemyPos.x = x;
        enemyPos.y = y;
    }

    public void registerBulletHit(HitByBulletEvent e) {
        lastBulletHitTime = e.getTime();
        Optional<Shot> registeredShot = enemyShots.stream().filter(s -> e.getTime() == s.getTime()).findFirst();
        if (registeredShot.isPresent()) {
            registeredShot.get().setRobotHit(e.getBullet(), (Point2D.Double) enemyPos.clone());
            if(!inAir.isEmpty()) inAir.remove();
        }
        else {
            System.out.println("Unregistered shot hit!");
            //   enemyShots.add(new Shot(e));
        }
    }

    public void saveShots() {
        System.out.println("Enemy shots hit: " + enemyShots.stream().filter(s -> s.getState() == Shot.states.HIT).count());
        System.out.println("Enemy shots missed: " + enemyShots.stream().filter(s -> s.getState() == Shot.states.MISS).count());

        //Remove missed
        List<Shot> toRemove = new ArrayList<>();
        enemyShots.stream().filter(s -> s.getState() == Shot.states.MISS).forEach(s -> toRemove.add(s));
        enemyShots.removeAll(toRemove);

        Util.saveShots(robot.getDataFile("enemyShots"), enemyShots);
    }

    public void paintAvoidanceSystem(Graphics2D g) {

        if (!inAir.isEmpty()) {

            Vector2D[] predictedRoutes = getShotVectors(10);
            Shot closest = inAir.element();

            for (int i = 0; i < predictedRoutes.length; i++) {
                Color c = new Color(255 - i * 25, i * 25, 0);
                g.setColor(c);

                Point2D.Double is = getIntersection(
                        avoidanceVector, predictedRoutes[i], avoidanceVectorOrigin, closest.getEnemyPointAtFire());

                if (is != null)
                    g.drawRect((int) is.getX(), (int) is.getY(), 15, 15);

                //predictedRoutes[i].paintVector(g, closest.getEnemyPointAtFire().getX(), closest.getEnemyPointAtFire().getY(), c);
            }
        }
        avoidanceVector.paintVector(g, avoidanceVectorOrigin.getX(), avoidanceVectorOrigin.getY(), Color.black);
        Point2D.Double safeSpot = findSafeSpot();
        if(safeSpot != null)
            g.fillRoundRect((int)safeSpot.getX(), (int)safeSpot.getY(), 15,15,15,15);
    }

    private boolean detectShot(ScannedRobotEvent e) {
        double enemyDeltaEnergy = e.getEnergy() - oldEnemyEnergy;
        boolean fired = enemyDeltaEnergy < 0 && e.getTime() != lastBulletHitTime;

        if (fired) {
            //System.out.println("Shot registered with power " + enemyDeltaEnergy + " . Shots: " + enemyShots.size());
            Shot shot = new Shot(e, (Point2D.Double) enemyPos.clone(), enemyDeltaEnergy, robot);
            enemyShots.add(shot);
            inAir.add(shot);
        }

        oldEnemyEnergy = e.getEnergy();

        return fired;
    }

    private int dodgeBullet() {
        findSafeSpot();
        if (inAir.isEmpty()) return 1;

        double nFwd = 0;
        double nRev = 0;

        List<Point2D.Double> is = getIntersections(100);
        for (int i = 0; i < is.size(); i++) {
            Point2D.Double p = is.get(i);
            if(p != null) {
                if (Util.isInFront(robot.getX(), robot.getY(), p.getX(), p.getY(), robot.getHeading())) {
                    nFwd += (1 / ((double) i + 1));
                    //   System.out.println("In front!");
                } else {
                    nRev += (1 / ((double) i + 1));
                    // System.out.println("In back!");
                }

            }
                //(1/((double)i + 1));
        }

        if (nFwd > nRev) {
            return -1;
        } else
            return 1;
    }

    private Point2D.Double findSafeSpot(){
        if (inAir.isEmpty()) return null;

        List<Point2D.Double> is = getIntersections(100);

        double x1 = robot.getX() + Reference.STICK_LENGTH*Math.sin(robot.getHeadingRadians());
        double y1 = robot.getY() + Reference.STICK_LENGTH*Math.cos(robot.getHeadingRadians());
        double x2 = avoidanceVectorOrigin.getX();
        double y2 = avoidanceVectorOrigin.getY();

        for(int i = 1; i < 256; i*=2){
            int upper = 0;
            int lower = 0;
            for(int j = 0; j < is.size(); j++){
                Point2D.Double p = is.get(j);
                if(p != null) {
                    /*if (Util.isBetween(x1, y1, x2 + (Reference.STICK_LENGTH / i) * Math.sin(robot.getHeadingRadians()),
                            y2 + (Reference.STICK_LENGTH / i) * Math.cos(robot.getHeadingRadians()),
                            p.getX(), p.getY()))
                        upper++;
                    else
                        lower++;*/

                    if (Util.isInFront(x2 + (Reference.STICK_LENGTH / i) * Math.sin(robot.getHeadingRadians()),
                            y2 + (Reference.STICK_LENGTH / i) * Math.cos(robot.getHeadingRadians()), p.getX(), p.getY(), robot.getHeading())) {
                        lower++;
                    } else {
                        upper++;
                    }
                }
            }
         //   System.out.println("x1: " + x1 + " y1: " + y1 + " x2: " + x2 + " y2: " + y2);

            if(upper > lower) {
                x2 += (Reference.STICK_LENGTH/i)*Math.sin(robot.getHeadingRadians());
                y2 += (Reference.STICK_LENGTH/i)*Math.cos(robot.getHeadingRadians());
            }
            else{
                x1 -= (Reference.STICK_LENGTH/i)*Math.sin(robot.getHeadingRadians());
                y1 -= (Reference.STICK_LENGTH/i)*Math.cos(robot.getHeadingRadians()) ;
            }
        }

        return new Point2D.Double(x1, y1);
    }

    private Vector2D[] getShotVectors(int limit) {
        Shot mostRecent = enemyShots.get(enemyShots.size() - 1);
        List<Shot> matched = getBestMatchedShots(mostRecent, limit);
        Vector2D[] vectors = new Vector2D[matched.size()];

        for (int i = 0; i < matched.size(); i++) {
            Vector2D er = new Vector2D(mostRecent.getRobotPointAtFire().getX() - mostRecent.getEnemyPointAtFire().getX(), mostRecent.getRobotPointAtFire().getY() - mostRecent.getEnemyPointAtFire().getY());
            double ER_bearingRad = er.getHeadingRadians();

            double angle = ER_bearingRad - Math.toRadians(matched.get(i).getEnemyDeltaAngle());
            double length = Point.distance(robot.getX(), robot.getY(), mostRecent.getEnemyPointAtFire().getX(), mostRecent.getEnemyPointAtFire().getY());

            vectors[i] = new Vector2D(
                    length * 1.5 * Math.sin(angle),
                    length * 1.5 * Math.cos(angle));
        }

        return vectors;
    }

    private double getCurrentDistance(Shot shot) {
        return shot.getTimeAlive() * (20 - 3 * shot.getPower());
    }

    private Shot getBestMatchedShot(Shot shot) {
        List<Shot> bestMatched = getBestMatchedShots(shot, 1);

        if (bestMatched.size() > 0)
            return getBestMatchedShots(shot, 1).get(0);
        else
            return null;
    }

    private List<Shot> getBestMatchedShots(Shot shot, int maxSize) {
        return enemyShots.stream().filter(s -> s.getState() == Shot.states.HIT)
                .sorted((x, y) -> x.getDistance(shot, avoidWeights) < y.getDistance(shot, avoidWeights) ? -1 : 1)
                .limit(maxSize).collect(Collectors.toList());
    }


    private void updateAvoidanceVectors() {
        avoidanceVector.setX(Reference.STICK_LENGTH * 2 * Math.sin(robot.getHeadingRadians()));
        avoidanceVector.setY(Reference.STICK_LENGTH * 2 * Math.cos(robot.getHeadingRadians()));
        avoidanceVectorOrigin.setLocation(
                robot.getX() - Reference.STICK_LENGTH * Math.sin(robot.getHeadingRadians()),
                robot.getY() - Reference.STICK_LENGTH * Math.cos(robot.getHeadingRadians())
        );
    }

    private List<Point2D.Double> getIntersections(int limit){
        List<Point2D.Double> intersections = new ArrayList<>();

        if (inAir.isEmpty()) return intersections;
        Shot closest = inAir.element();

        Vector2D[] predictedRoutes = getShotVectors(limit);
        for (int i = 0; i < predictedRoutes.length; i++) {
            intersections.add(getIntersection(
                    avoidanceVector, predictedRoutes[i], avoidanceVectorOrigin, closest.getEnemyPointAtFire()));
        }

        return intersections;
    }

    private Point2D.Double getIntersection(Vector2D a, Vector2D b, Point2D.Double aStart, Point2D.Double bStart) {
        //Algorithm from http://stackoverflow.com/questions/563198/how-do-you-detect-where-two-line-segments-intersect

        double p0_x = aStart.getX();
        double p0_y = aStart.getY();
        double p1_x = aStart.getX() + a.getX();
        double p1_y = aStart.getY() + a.getY();
        double p2_x = bStart.getX();
        double p2_y = bStart.getY();
        double p3_x = bStart.getX() + b.getX();
        double p3_y = bStart.getY() + b.getY();

        double s1_x = p1_x - p0_x;
        double s2_x = p3_x - p2_x;
        double s1_y = p1_y - p0_y;
        double s2_y = p3_y - p2_y;

        double s = (-s1_y * (p0_x - p2_x) + s1_x * (p0_y - p2_y)) / (-s2_x * s1_y + s1_x * s2_y);
        double t = (s2_x * (p0_y - p2_y) - s2_y * (p0_x - p2_x)) / (-s2_x * s1_y + s1_x * s2_y);

        if (s >= 0 && s <= 1 && t >= 0 && t <= 1) {
            return new Point2D.Double(p0_x + (t * s1_x), p0_y + (t * s1_y));
        } else return null;
    }


    //NOT USED
    private Vector2D getExpectedImpact(Shot justFired, Shot oldShot) {
        double eDist = enemyPos.distance(robot.getX(), robot.getY()) * 1.5;
        Vector2D er = new Vector2D(justFired.getRobotPointAtFire().getX() - justFired.getEnemyPointAtFire().getX(),
                justFired.getRobotPointAtFire().getY() - justFired.getEnemyPointAtFire().getY());
        double ER_bearingRad = er.getHeadingRadians();

        double angle = ER_bearingRad - Math.toRadians(oldShot.getEnemyDeltaAngle());

        return new Vector2D(eDist * Math.sin(angle), eDist * Math.cos(angle));
    }
}


/*

char get_line_intersection(float p0_x, float p0_y, float p1_x, float p1_y,
    float p2_x, float p2_y, float p3_x, float p3_y, float *i_x, float *i_y)
{
    float s1_x, s1_y, s2_x, s2_y;
    s1_x = p1_x - p0_x;     s1_y = p1_y - p0_y;
    s2_x = p3_x - p2_x;     s2_y = p3_y - p2_y;

    float s, t;
    s = (-s1_y * (p0_x - p2_x) + s1_x * (p0_y - p2_y)) / (-s2_x * s1_y + s1_x * s2_y);
    t = ( s2_x * (p0_y - p2_y) - s2_y * (p0_x - p2_x)) / (-s2_x * s1_y + s1_x * s2_y);

    if (s >= 0 && s <= 1 && t >= 0 && t <= 1)
    {
        // Collision detected
        if (i_x != NULL)
            *i_x = p0_x + (t * s1_x);
        if (i_y != NULL)
            *i_y = p0_y + (t * s1_y);
        return 1;
    }

    return 0; // No collision
}
         */

/*
        Shot mostRecent = enemyShots.get(enemyShots.size() - 1);
        List<Shot> matched = getBestMatchedShots(mostRecent, 10);

        //System.out.println("Matched size " + matched.size());
        for (int i = 0; i < matched.size(); i++) {
            Vector2D er = new Vector2D(mostRecent.getRobotPointAtFire().getX() - mostRecent.getEnemyPointAtFire().getX(), mostRecent.getRobotPointAtFire().getY() - mostRecent.getEnemyPointAtFire().getY());
            double ER_bearingRad = er.getHeadingRadians();

            double angle = ER_bearingRad - Math.toRadians(matched.get(i).getEnemyDeltaAngle());
            double currDistance = mostRecent.getTimeAlive() * (20 - 3 * mostRecent.getPower());
            Point2D.Double hitPoint = new Point2D.Double(
                    mostRecent.getEnemyPointAtFire().getX() + currDistance * Math.sin(angle),
                    mostRecent.getEnemyPointAtFire().getY() + currDistance * Math.cos(angle));

            g.setColor(new Color(255 - i * 20, i * 20, 0));
            g.fillRoundRect((int) hitPoint.getX(), (int) hitPoint.getY(), 10, 10, 10, 10);
        }*/