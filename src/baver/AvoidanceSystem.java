package baver;

import Util.Vector2D;
import robocode.AdvancedRobot;
import robocode.HitByBulletEvent;
import robocode.ScannedRobotEvent;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import Util.Util;
import Util.Line;

/**
 * Created by pontu on 2017-03-27.
 */
public class AvoidanceSystem {

    private AdvancedRobot robot;
    private double oldEnemyEnergy;
    private long lastBulletHitTime = 0;
    private Point2D.Double enemyPos = new Point2D.Double();
    private Line avoidanceLine;

    private List<Shot> enemyShots;
    private WeightSet avoidWeights;

    public AvoidanceSystem(AdvancedRobot robot){
        this.robot = robot;
        enemyShots = Util.loadPreviousShots(robot.getDataFile("enemyShots"), robot.getRoundNum());
        avoidWeights = new WeightSet(Reference.AVOIDING_WEIGHTS);
        avoidanceLine = new Line(1, 0);
    }

    public int getNewDirection(ScannedRobotEvent e, int currentDirection) {
        if (detectShot(e)) return dodgeBullet();
        return currentDirection;
    }

    public void update(){
        enemyShots.stream().filter(s -> s.getState() == Shot.states.IN_AIR).forEach(s -> s.update());
        updateLine();

    }

    public void updateEnemyPos(double x, double y){
        enemyPos.x = x;
        enemyPos.y = y;
    }

    public void registerBulletHit(HitByBulletEvent e){
        lastBulletHitTime = e.getTime();
        Optional<Shot> registeredShot = enemyShots.stream().filter(s -> e.getTime() == s.getTime()).findFirst();
        if (registeredShot.isPresent())
            registeredShot.get().setRobotHit(e.getBullet(), (Point2D.Double) enemyPos.clone());
        else {
            System.out.println("Unregistered shot hit!");
            //   enemyShots.add(new Shot(e));
        }
    }

    public void saveShots(){
        System.out.println("Enemy shots hit: " + enemyShots.stream().filter(s -> s.getState() == Shot.states.HIT).count());
        System.out.println("Enemy shots missed: " + enemyShots.stream().filter(s -> s.getState() == Shot.states.MISS).count());

        //Remove missed
        List<Shot> toRemove = new ArrayList<>();
        enemyShots.stream().filter(s -> s.getState() == Shot.states.MISS).forEach(s -> toRemove.add(s));
        enemyShots.removeAll(toRemove);

        Util.saveShots(robot.getDataFile("enemyShots"), enemyShots);
    }

    public void paintAvoidanceSystem(Graphics2D g){
        if (enemyShots.size() == 0)
            return;

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
        }

        //Line
        avoidanceLine.plot(g, robot.getX(), robot.getY(), 200, Color.BLACK);
    }

    private boolean detectShot(ScannedRobotEvent e) {
        double enemyDeltaEnergy = e.getEnergy() - oldEnemyEnergy;
        boolean fired = enemyDeltaEnergy < 0 && e.getTime() != lastBulletHitTime;

        if (fired) {
            //System.out.println("Shot registered with power " + enemyDeltaEnergy + " . Shots: " + enemyShots.size());
            Shot shot = new Shot(e, (Point2D.Double) enemyPos.clone(), enemyDeltaEnergy, robot);
            enemyShots.add(shot);
        }

        oldEnemyEnergy = e.getEnergy();

        return fired;
    }

    private int dodgeBullet() {
        if (enemyShots.size() == 0)
            return 1;

        Shot mostRecent = enemyShots.get(enemyShots.size() - 1);

        double nFwd = 0;
        double nRev = 0;

        List<Shot> matched = getBestMatchedShots(mostRecent, 10);
        for (int i = 0; i < matched.size(); i++) {
            Vector2D er = new Vector2D(mostRecent.getRobotPointAtFire().getX() - mostRecent.getEnemyPointAtFire().getX(), mostRecent.getRobotPointAtFire().getY() - mostRecent.getEnemyPointAtFire().getY());
            double ER_bearingRad = er.getHeadingRadians();

            double angle = ER_bearingRad - Math.toRadians(matched.get(i).getEnemyDeltaAngle());
            double currDistance = mostRecent.getTimeAlive() * (20 - 3 * mostRecent.getPower());
            Point2D.Double hitPoint = new Point2D.Double(
                    mostRecent.getEnemyPointAtFire().getX() + currDistance * Math.sin(angle),
                    mostRecent.getEnemyPointAtFire().getY() + currDistance * Math.cos(angle));

            if (Util.isInFront(robot.getX(), robot.getY(), hitPoint.getX(), hitPoint.getY(), robot.getHeading())) {
                nFwd += (1 / ((double) i + 1));
                //   System.out.println("In front!");
            } else {
                nRev += (1 / ((double) i + 1));
                // System.out.println("In back!");
            }

            //(1/((double)i + 1));
        }

        if (nFwd > nRev) {
            return -1;
        } else
            return 1;
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

    private void updateLine(){
        avoidanceLine.setAFromHeading(robot.getHeadingRadians());
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

    private Point2D.Double getIntersection(Vector2D a, Vector2D b, Point2D.Double aStart, Point2D.Double bStart) {
        //Algorithm from http://stackoverflow.com/questions/563198/how-do-you-detect-where-two-line-segments-intersect

        Vector2D p = new Vector2D(aStart.getX(), aStart.getY());
        Vector2D q = new Vector2D(bStart.getX(), bStart.getY());
        double r = Vector2D.getLength(a);
        double s = Vector2D.getLength(b);

        //  double t = Vector2D.sub(q,p).dot()

        return null;
    }
}
