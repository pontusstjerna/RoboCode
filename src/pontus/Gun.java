package pontus;

import robocode.Bullet;
import robocode.ScannedRobotEvent;

import java.awt.*;

/**
 * Created by Pontus on 2016-02-08.
 */
public class Gun {
    private Korven korven;
    private static final double AIM_LIMIT = 0.5f;
    private Vector2D toEnemy;
    private Vector2D enemyPath;
    private Vector2D toHitPoint;
    private double deltaAngle = 180;
    private double firePower = 3;
    private double additional = 0;

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
        //firePower = 3 - 3 * distance / korven.getMaxDistance();
        double bulletSpeed = 20 - 3 * firePower;

        toEnemy = Vector2D.getHeadingVector(korven.getRadarHeadingRadians(), e.getDistance(), 1);
        enemyPath = Vector2D.getHeadingVector(e.getHeadingRadians(), (e.getDistance()*enemyVel/bulletSpeed), 1);
        toHitPoint = Vector2D.add(toEnemy, enemyPath);

        deltaAngle = korven.get180(toHitPoint.getHeading() - korven.getGunHeading());



        //additional = e.getDistance()*0.024*Math.signum(deltaAngle);

        if(deltaAngle < 180 && deltaAngle > -180){
            korven.setTurnGunRight(deltaAngle);
            System.out.println("DeltaAngle: " + deltaAngle);
        }else if(deltaAngle > 180){
            korven.setTurnGunRight(360 - deltaAngle);
        }else{
            korven.setTurnGunRight(360 + deltaAngle);
        }
    }

    long fireTime = 0;
    public void fire(ScannedRobotEvent e) {
        double distance = e.getDistance();
        //double deltaAngle = korven.getRadarHeading() - korven.getGunHeading();

        if(fireTime == korven.getTime() && korven.getGunTurnRemaining() == 0){
            Bullet bullet = (korven.setFireBullet(firePower));
            korven.addBullet(bullet, e);
        }

        fireTime = korven.getTime() + 1;
/*
        if ((korven.closeEnough(distance*1.5) || e.getVelocity() < 2) &&
                deltaAngle < AIM_LIMIT && deltaAngle > -AIM_LIMIT && e.getEnergy() != 0 || korven.inPerfectRange()) {
            Bullet bullet = (korven.setFireBullet(firePower));
            korven.addBullet(bullet, e);
        }else{
            lockToEnemy(e);
        }*/
    }

    public void paintGun(Graphics2D g){
        if(enemyPath != null && toEnemy != null){
            enemyPath.paintVector(g, korven.getX() + toEnemy.getX(), korven.getY() + toEnemy.getY(), Color.RED);
            toEnemy.paintVector(g, korven.getX(), korven.getY(), Color.RED);
            toHitPoint.paintVector(g, korven.getX(), korven.getY(), Color.CYAN);
        }
    }
}
