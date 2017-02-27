package baver;

import pontus.Vector2D;
import robocode.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static robocode.Rules.RADAR_SCAN_RADIUS;


/**
 * Created by pontu on 2017-02-26.
 */
public class BaverMain extends AdvancedRobot {

    private Radar radar;

    private final int LOCK_TIMEOUT = 60;
    private static final int MAX_DISTANCE = 300;
    private static final int MIN_DISTANCE = 200;
    private static final double WALL_LIMIT = 100;

    private int lockTicks = LOCK_TIMEOUT;
    private int dir = 1;
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
        if(detectShot(e))
            dodgeBullet();

        //keepDistance(e);
        setTurnRight((e.getBearing() - 90));

        shots.stream().filter(s -> s.getState() == Shot.states.IN_AIR).forEach(s -> s.addTick());


        scan();
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
        dir = -dir;
    }

    @Override
    public void onDeath(DeathEvent e){
        System.out.println("Shots hit: " + shots.stream().filter(s -> s.getState() == Shot.states.HIT).count());
        System.out.println("Shots missed: " + shots.stream().filter(s -> s.getState() == Shot.states.MISS).count());
    }

    @Override
    public void onPaint(Graphics2D g) {
        g.setColor(new Color(255, 0, 0));

        /*shots.stream().filter(sh -> sh.getState() == Shot.states.IN_AIR).forEach(s -> {
            g.fillRoundRect((int)getX(), (int)getY(), 10, 10, 10, 10);

        });*/

        shots.stream().filter(s -> s.getState() == Shot.states.HIT).forEach(s -> {
            g.fillRoundRect((int)s.getBullet().getX(), (int)s.getBullet().getY(), 10, 10, 10, 10);
        });
    }

    private boolean detectShot(ScannedRobotEvent e){
        boolean fired = e.getEnergy() < oldEnemyEnergy;

        if(fired){
            System.out.println("Shot registered. Shots: " + shots.size());
            Shot shot = new Shot(e);
            shots.add(shot);
        }

        oldEnemyEnergy = e.getEnergy();

        return fired;
    }

    private void dodgeBullet(){
        if(shots.size() == 0)
            return;

        Shot mostRecent = shots.get(shots.size() - 1);
        Shot similarShot = getBestMatchedShot(mostRecent);

        if(similarShot == null)
            return;

        if(similarShot.getState() == Shot.states.HIT){
            setAhead(getHeight()*2*dir);
        }
    }

    private void keepDistance(ScannedRobotEvent e) {
        if(getDistanceToWall() > WALL_LIMIT || e.getEnergy() == 0){ //If not close to any wall
            setMaxVelocity(8);
            if(e.getDistance() > MAX_DISTANCE || e.getEnergy() == 0){ //If too far away or disabled
                setTurnRight(e.getBearing() - 20);
            }else if(e.getDistance() < MIN_DISTANCE){ //If too close to the enemy
                setTurnRight(e.getBearing() - 160);
            }else{ //If in the perfect distance interval
                setTurnRight((e.getBearing() - 90));
            }
        }else{ //Turn to middle!
            if(getAngleToMiddle() > 90 || getAngleToMiddle() < -90){ //If too close, break
                setMaxVelocity(8 - 7*(1/getDistanceToWall()));
            }
            setTurnRight(getAngleToMiddle());
        }

       // setAhead(100*dir);
    }

    private Shot getBestMatchedShot(Shot shot){
        Shot closest;

        if(shots.stream().filter(s -> s.getState() != Shot.states.IN_AIR).findAny().isPresent())
             closest = shots.stream().filter(s -> s.getState() != Shot.states.IN_AIR).findAny().get();
        else
            return null;

        for(Shot s : shots.stream().filter(sh -> sh.getState() != Shot.states.IN_AIR).collect(Collectors.toList())){
            if(shot.getDistance(s) < closest.getDistance(s))
                closest = s;
        }

        return closest;
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

    private double getDistanceToWall() {
        double[] distances = {getX(), getY(), getBattleFieldWidth() - getX(), getBattleFieldHeight() - getY()};
        double distance = distances[0];

        for (int i = 0; i < distances.length; i++) {
            if (distances[i] < distance) {
                distance = distances[i];
            }
        }

        return distance;
    }

    private double getAngleToMiddle() {
        double dx = (getBattleFieldWidth() / 2) - getX();
        double dy = (getBattleFieldHeight() / 2) - getY();

        Vector2D toMiddle = new Vector2D(dx, dy);

        double angle = get180(toMiddle.getHeading() - get180(getHeading()));
        return angle;
    }

    void refreshLock(){
        lockTicks = 0;
    }

}
