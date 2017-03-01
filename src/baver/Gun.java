package baver;

import pontus.Vector2D;
import robocode.AdvancedRobot;
import robocode.Bullet;
import robocode.ScannedRobotEvent;

import java.awt.*;

/**
 * Created by pontu on 2017-03-01.
 */
public class Gun {
    private AdvancedRobot robot;
    private static final double AIM_LIMIT = 0.5f;
    private Vector2D toEnemy;
    private Vector2D enemyPath;
    private Vector2D toHitPoint;
    private double deltaAngle = 180;
    private double firePower = 1;
    private double additional = 0;

    public Gun(AdvancedRobot robot){
        this.robot = robot;
    }

    public void lockToEnemy(ScannedRobotEvent e){
        //double deltaAngle = robot.getRadarHeading() - robot.getGunHeading();
        double distance = e.getDistance();
        double enemyVel = e.getVelocity();
        //firePower = 3 - 3 * distance / robot.getMaxDistance();
        double bulletSpeed = 20 - 3 * firePower;

        toEnemy = Vector2D.getHeadingVector(robot.getRadarHeadingRadians(), e.getDistance(), 1);
        enemyPath = Vector2D.getHeadingVector(e.getHeadingRadians(), (e.getDistance()*enemyVel/bulletSpeed), 1);
        toHitPoint = Vector2D.add(toEnemy, enemyPath);

        deltaAngle = Util.get180(toHitPoint.getHeading() - robot.getGunHeading());

        //additional = e.getDistance()*0.024*Math.signum(deltaAngle);

        if(deltaAngle < 180 && deltaAngle > -180){
            robot.setTurnGunRight(deltaAngle);
         //   System.out.println("DeltaAngle: " + deltaAngle);
        }else if(deltaAngle > 180){
            robot.setTurnGunRight(360 - deltaAngle);
        }else{
            robot.setTurnGunRight(360 + deltaAngle);
        }
    }

    public void fire(ScannedRobotEvent e) {
        if(robot.getGunTurnRemaining() < 3 && robot.getGunHeat() == 0){
            robot.setFireBullet(firePower);
        }
    }

    public void paintGun(Graphics2D g){
        if(enemyPath != null && toEnemy != null){
            enemyPath.paintVector(g, robot.getX() + toEnemy.getX(), robot.getY() + toEnemy.getY(), Color.RED);
            toEnemy.paintVector(g, robot.getX(), robot.getY(), Color.RED);
            toHitPoint.paintVector(g, robot.getX(), robot.getY(), Color.CYAN);
        }
    }
}
