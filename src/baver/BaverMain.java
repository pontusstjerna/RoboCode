package baver;

import robocode.AdvancedRobot;
import robocode.HitByBulletEvent;
import robocode.HitWallEvent;
import robocode.ScannedRobotEvent;

import java.awt.*;

/**
 * Created by pontu on 2017-02-26.
 */
public class BaverMain extends AdvancedRobot {

    private Radar radar;

    private final int LOCK_TIMEOUT = 30;
    private int lockTicks = LOCK_TIMEOUT;

    @Override
    public void run(){
        setColors(new Color(255, 196, 13),
                new Color(29, 29, 29),
                new Color(0, 52, 158)); // body,gun,radar

        setAdjustRadarForGunTurn(true);
        setAdjustRadarForRobotTurn(true);
        setAdjustGunForRobotTurn(true);

        radar = new Radar(this);

        while (true) {
            if(lockTicks >= LOCK_TIMEOUT) {
                setTurnRadarRight(360);
            }else{
                lockTicks++;
            }

            execute();
        }
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        radar.lockOnTarget(e);
    }

    @Override
    public void onHitByBullet(HitByBulletEvent e) {
        // Replace the next line with any behavior you would like
    }

    @Override
    public void onHitWall(HitWallEvent e) {

    }

    @Override
    public void onPaint(Graphics2D g) {

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
