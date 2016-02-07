package pontus;

import robocode.Bullet;
import robocode.ScannedRobotEvent;

import java.awt.geom.Point2D;

/**
 * Created by Pontus on 2016-02-03.
 */
public class BulletData {
    private Bullet bullet;
    private double radarHeading;
    private Point2D.Double shotPoint;
    private Point2D.Double hitPoint;
    private ScannedRobotEvent robot;


    public BulletData(Bullet bullet, double radarHeading, double x, double y, ScannedRobotEvent e){
        this.bullet = bullet;
        this.radarHeading = radarHeading;
        shotPoint = new Point2D.Double(x, y);
        robot = e;
    }

    public BulletData(Bullet bullet, double deltaAngle, Point2D.Double shotPoint){
        this.bullet = bullet;
        this.radarHeading = deltaAngle;
        this.shotPoint = shotPoint;
    }

    public void setHitPoint(double x, double y){
        hitPoint = new Point2D.Double(x, y);
    }

    public void setHitPoint(Point2D.Double hitPoint){
        this.hitPoint = hitPoint;
    }

    public Bullet getBullet() {
        return bullet;
    }

    public double getRadarHeading() {
        return radarHeading;
    }

    public Point2D.Double getShotPoint() {
        return shotPoint;
    }

    public Point2D.Double getHitPoint() {
        return hitPoint;
    }

    public ScannedRobotEvent getRobot(){
        return robot;
    }

    @Override
    public String toString(){
        return "Hit with bearing " + robot.getBearing() + " at distance " + robot.getDistance();
    }
}

