package pontus;

import robocode.Bullet;
import robocode.ScannedRobotEvent;

import java.awt.*;

/**
 * Created by Pontus on 2016-02-08.
 */
public class Gun {
    private Korven korven;
    private static final double AIM_LIMIT = 2;
    private Vector2D toEnemy;
    private Vector2D enemyPath;
    private Vector2D toHitPoint;
    private double deltaAngle = 180;

    public Gun(Korven korven){
        this.korven = korven;
    }

    public void lockToRadar(){
        deltaAngle = korven.getRadarHeading() - korven.getGunHeading();
        if(deltaAngle < 180 && deltaAngle > -180){
            korven.setTurnGunRight(deltaAngle);
        }else if(deltaAngle > 180){
            korven.setTurnGunRight(360 - deltaAngle);
        }else{
            korven.setTurnGunRight(360 + deltaAngle);
        }
    }

    public void lockToEnemy(ScannedRobotEvent e){
        //double deltaAngle = korven.getRadarHeading() - korven.getGunHeading();
        double distance = e.getDistance();
        double enemyVel = e.getVelocity();
        double firePower = 3 - 3 * distance / korven.getMaxDistance();
        double bulletSpeed = 20 - 3 * firePower;

        toEnemy = Vector2D.getHeadingVector(korven.getRadarHeadingRadians(), e.getDistance(), 1);
        enemyPath = Vector2D.getHeadingVector(e.getHeadingRadians(), (e.getDistance()*enemyVel)/bulletSpeed, 1);
        toHitPoint = Vector2D.add(toEnemy, enemyPath);

        double additional = e.getDistance()*0.0025*e.getVelocity();

        deltaAngle = toHitPoint.getHeading() - (korven.getGunHeading() - additional);

        if(deltaAngle < 180 && deltaAngle > -180){
            korven.setTurnGunRight(deltaAngle);
        }else if(deltaAngle > 180){
            korven.setTurnGunRight(360 - deltaAngle);
        }else{
            korven.setTurnGunRight(360 + deltaAngle);
        }
    }

    public void fire(ScannedRobotEvent e) {
        double distance = e.getDistance();
        //double deltaAngle = korven.getRadarHeading() - korven.getGunHeading();
        double firePower = 3 - 3 * distance / korven.getMaxDistance();

        if ((korven.closeEnough(distance*1.5) || e.getVelocity() < 2) && deltaAngle < AIM_LIMIT && deltaAngle > -AIM_LIMIT) {
            Bullet bullet = (korven.setFireBullet(firePower));
            korven.addBullet(bullet, e);
        }else{
            lockToEnemy(e);
        }
    }

    public void paintGun(Graphics2D g){
        if(enemyPath != null && toEnemy != null){
            enemyPath.paintVector(g, korven.getX() + toEnemy.getX(), korven.getY() + toEnemy.getY(), Color.RED);
            toEnemy.paintVector(g, korven.getX(), korven.getY(), Color.RED);
            toHitPoint.paintVector(g, korven.getX(), korven.getY(), Color.CYAN);
        }
    }
}
