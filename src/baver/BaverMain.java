package baver;

import robocode.AdvancedRobot;
import robocode.HitByBulletEvent;
import robocode.HitWallEvent;
import robocode.ScannedRobotEvent;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by pontu on 2017-02-26.
 */
public class BaverMain extends AdvancedRobot {

    private Radar radar;

    private final int LOCK_TIMEOUT = 30;
    private int lockTicks = LOCK_TIMEOUT;
    private double oldEnemyEnergy;

    private List<Shot> shots;

    @Override
    public void run(){
        setColors(new Color(255, 196, 13),
                new Color(29, 29, 29),
                new Color(0, 52, 158)); // body,gun,radar

        setAdjustRadarForGunTurn(true);
        setAdjustRadarForRobotTurn(true);
        setAdjustGunForRobotTurn(true);

        radar = new Radar(this);
        shots = new ArrayList<>();

        while (true) {
            if(lockTicks >= LOCK_TIMEOUT) {
                setTurnRadarRight(360);
            }else{
                lockTicks++;
            }

            shots.stream().filter(s -> s.isInAir()).forEach(s -> s.addTick());

            execute();
        }
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        radar.lockOnTarget(e);
        detectShot(e);
    }

    @Override
    public void onHitByBullet(HitByBulletEvent e) {

    }

    @Override
    public void onHitWall(HitWallEvent e) {

    }

    @Override
    public void onPaint(Graphics2D g) {

    }

    private void detectShot(ScannedRobotEvent e){
        if(e.getEnergy() < oldEnemyEnergy){
            registerShot(e);
        }

        oldEnemyEnergy = e.getEnergy();
    }

    private void registerShot(ScannedRobotEvent e){
        System.out.println("Shot registered. Shots: " + shots.size());
        shots.add(new Shot(e.getBearing(), e.getVelocity(), e.getDistance()));
    }

    double get180(double angle) {
        angle = angle % 360;
        if (angle > 180 && angle > 0) {
            return angle - 360;
        }else if(angle < -180){
            return angle + 360;
        }
        return angle;
    }

    void refreshLock(){
        lockTicks = 0;
    }

}
