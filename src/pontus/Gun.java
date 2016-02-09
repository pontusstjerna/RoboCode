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

    public Gun(Korven korven){
        this.korven = korven;
    }

    public void lockToRadar(){
        double deltaAngle = korven.getRadarHeading() - korven.getGunHeading();
        if(deltaAngle < 180 && deltaAngle > -180){
            korven.setTurnGunRight(deltaAngle);
        }else if(deltaAngle > 180){
            korven.setTurnGunRight(360 - deltaAngle);
        }else{
            korven.setTurnGunRight(360 + deltaAngle);
        }
    }

    public void lockAndFire(ScannedRobotEvent e) {
        double distance = e.getDistance();
        double deltaAngle = korven.getRadarHeading() - korven.getGunHeading();

        toEnemy = Vector2D.getHeadingVector(korven.getRadarHeadingRadians(), e.getDistance(), 1);

        if (deltaAngle < AIM_LIMIT && deltaAngle > -AIM_LIMIT && korven.closeEnough(distance)) {
            double eSpeed = e.getVelocity();
            double firePower = 3 - 3 * distance / korven.getMaxDistance();
            double bulletSpeed = 20 - 3 * firePower;
            double eRevHead = e.getHeadingRadians() - korven.getGunHeadingRadians();
            double bulletDistance = Math.sqrt(Math.pow(distance, 2) +
                    Math.pow(((distance * eSpeed) / bulletSpeed), 2) -
                    2 * distance * ((distance * eSpeed) / bulletSpeed) * Math.cos(eRevHead));
            double angle = distance / bulletDistance;
            //System.out.println("Degrees: " + Math.acos(angle));

            //Bullet velocity:	20 - 3 * firepower.
            if (bulletDistance != 0 && bulletDistance < korven.getMaxDistance()) {
                korven.setTurnGunRightRadians(Math.acos(angle) + angle * 0.1f);
            }
            Bullet bullet = (korven.setFireBullet(firePower));
            korven.addBullet(bullet, e);
        }else{
            lockToRadar();
        }
    }

    public void paintGun(Graphics2D g){
        if(toEnemy != null){
            toEnemy.paintVector(g, korven.getX(), korven.getY(), Color.RED);
        }
    }
}
