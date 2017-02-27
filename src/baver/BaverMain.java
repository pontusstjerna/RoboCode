package baver;

import robocode.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static robocode.Rules.RADAR_SCAN_RADIUS;


/**
 * Created by pontu on 2017-02-26.
 */
public class BaverMain extends AdvancedRobot {

    private Radar radar;

    private final int LOCK_TIMEOUT = 60;
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

            shots.stream().filter(s -> s.getState() == Shot.states.IN_AIR).forEach(s -> s.addTick());

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
        if(shots.stream().filter(s -> e.getTime() == s.getTime()).findFirst().isPresent())
            shots.stream().filter(s -> e.getTime() == s.getTime()).findFirst().get().setHit(true, e.getBullet());
        else {
            System.out.println("Unregistered shot hit!");
            shots.add(new Shot(e));
        }
    }

    @Override
    public void onHitWall(HitWallEvent e) {

    }

    @Override
    public void onDeath(DeathEvent e){
        System.out.println("Shots hit: " + shots.stream().filter(s -> s.getState() == Shot.states.HIT).count());
        System.out.println("Shots missed: " + shots.stream().filter(s -> s.getState() == Shot.states.MISS).count());
    }

    @Override
    public void onPaint(Graphics2D g) {

    }

    private void detectShot(ScannedRobotEvent e){
        if(e.getEnergy() < oldEnemyEnergy){
            System.out.println("Shot registered. Shots: " + shots.size());
            shots.add(new Shot(e));
        }

        oldEnemyEnergy = e.getEnergy();
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
