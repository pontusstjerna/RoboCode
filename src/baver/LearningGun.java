package baver;

import robocode.AdvancedRobot;
import robocode.Bullet;
import robocode.ScannedRobotEvent;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by pontu on 2017-03-08.
 */
public class LearningGun {
    //Balancing
    private final double AIM_LIMIT = 1;

    private AdvancedRobot robot;
    private List<Shot> shots;
    private Shot currentBestMatched;

    public LearningGun(AdvancedRobot robot){
        this.robot = robot;
        shots = new ArrayList<>();
    }

    //Returns between 0 and 1 on how sure it is
    //No it doesn't
    public double aimAndFire(ScannedRobotEvent e){
        if(robot.getGunTurnRemaining() < AIM_LIMIT && robot.getGunHeat() == 0)
            robot.fireBullet(3);

        turnToTarget(e);
        return 1;
    }

    public void registerBullet(ScannedRobotEvent e, Bullet b){
        System.out.println(shots.size());
        shots.add(new Shot(e, robot, b));
    }

    public void updateShots(){
        shots.stream().filter(x -> x.getState() == Shot.states.IN_AIR).forEach(x -> x.update());
    }

    public double getHitRate(){
        return shots.stream().filter(x -> x.getState() == Shot.states.HIT).count()/
                ((double)shots.stream().filter(x -> x.getState() != Shot.states.IN_AIR).count());
    }

    public int getShotsSize(){
        return shots.size();
    }

    public void paintExpectations(Graphics g){
        g.setColor(new Color(255,0,255));
        if(currentBestMatched != null)
            g.fillRoundRect((int)currentBestMatched.getBullet().getX(), (int)currentBestMatched.getBullet().getY(), 10, 10, 10, 10);
    }

    private void turnToTarget(ScannedRobotEvent e){
        Shot pretendShot = new Shot(e, robot);
        Shot shot = getBestMatched(e, pretendShot);
        if(shot == null)
            return;

        currentBestMatched = shot;

        robot.setTurnGunRight(shot.getTurretBearing());
    }

    private Shot getBestMatched(ScannedRobotEvent e, Shot shot){
        if(shots.size() == 0)
            return null;

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
}
