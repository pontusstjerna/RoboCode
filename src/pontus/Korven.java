package pontus;

import robocode.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// API help : http://robocode.sourceforge.net/docs/robocode/robocode/Robot.html

/**
 * Korven - a robot by Pontus
 */
public class Korven extends AdvancedRobot {
    private int dir = 1; // 1 = right, -1 = left
    private Random rand;
    private static final int MAX_DISTANCE = 300;
    private static final int MIN_DISTANCE = 200;
    private double eEnergy = 0;
    private static final double WALL_LIMIT = 100;
    private double adjust = 1;
    private int shotsFired = 0;
    private List<BulletData> bullets = new ArrayList<>();
    private List<BulletData> eData = new ArrayList<>();
    private boolean locked = false;
    private boolean perfectRange = false;

    private Radar radar;
    private Gun gun;

    @Override
    public void run() {

        setColors(new Color(148, 255, 68),
                new Color(27, 104, 27),
                new Color(65, 154, 42)); // body,gun,radar

        setAdjustRadarForGunTurn(true);
        setAdjustRadarForRobotTurn(true);
        setAdjustGunForRobotTurn(true);
        rand = new Random();

        radar = new Radar(this);
        gun = new Gun(this);

        // Robot main loop
        while (true) {
            if (!locked) {
                setTurnRadarRight(360);
            }else{
                setAhead(WALL_LIMIT*dir);
            }
            execute();
        }
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        radar.lock(e);
        gun.fire(e);
        gun.lockToEnemy(e);
        locked = true;
        keepDistance(e);
        avoid(e);
    }

    @Override
    public void onHitByBullet(HitByBulletEvent e) {
        // Replace the next line with any behavior you would like
    }

    @Override
    public void onBulletHit(BulletHitEvent e) {
        for (BulletData b : bullets) {
            if (b.getBullet().equals(e.getBullet())) {
                b.setHitPoint(e.getBullet().getX(), e.getBullet().getY());
                eData.add(b);
            }
        }
        bullets.clear();
        if (eData.size() > 0) {
            System.out.println("Hits: " + eData.size());
        }
    }

    @Override
    public void onHitWall(HitWallEvent e) {
        dir = -dir;
        setAhead(2*WALL_LIMIT*dir);
    }

    @Override
    public void onPaint(Graphics2D g) {
        /*double dx = (getBattleFieldWidth() / 2) - getX();
        double dy = (getBattleFieldHeight() / 2) - getY();

        Vector2D toMiddle = new Vector2D(dx, dy);
        Vector2D heading = Vector2D.getHeadingVector(getHeadingRadians(), 100, getVelocity());

        toMiddle.paintVector(g, getX(), getY(), Color.YELLOW);
        heading.paintVector(g, getX(), getY(), Color.BLUE);
*/
        gun.paintGun(g);

        //g.setColor(java.awt.Color.RED);
        //g.drawLine((int)getX(), (int)getY(), (int)(getX() + Math.signum(getVelocity())* 100*Math.cos(-getHeadingRadians() + Math.PI/2)), (int)(getY() + Math.signum(getVelocity()) *  100*Math.sin(-getHeadingRadians() + Math.PI/2)));
    }

    public double get180(double angle) {
        angle = angle % 360;
        if (angle > 180 && angle > 0) {
            return angle - 360;
        }else if(angle < -180){
            return angle + 360;
        }
        return angle;
    }

    private void keepDistance(ScannedRobotEvent e) {
        perfectRange = false;
        if(getDistanceToWall() > WALL_LIMIT || e.getEnergy() == 0){ //If not close to any wall
            setMaxVelocity(8);
            if(e.getDistance() > MAX_DISTANCE || e.getEnergy() == 0){ //If too far away or disabled
                setTurnRight(e.getBearing()*dir - 20);
            }else if(e.getDistance() < MIN_DISTANCE){ //If too close to the enemy
                setTurnRight(e.getBearing()*dir - 160);
            }else{ //If in the perfect distance interval
                perfectRange = true;
                setTurnRight((e.getBearing() - 90 * adjust));
            }
        }else{ //Turn to middle!
            if(angleToMiddle() > 90 || angleToMiddle() < -90){ //If too close, break
                setMaxVelocity(8 - 7*(1/getDistanceToWall()));
            }
            setTurnRight(angleToMiddle());
        }
        setAhead((rand.nextInt(200) + 70) * dir); //Always move!
    }

    private void avoid(ScannedRobotEvent e) {
        if (e.getEnergy() < eEnergy && getDistanceToWall() > WALL_LIMIT/2 && e.getDistance() < MAX_DISTANCE) {
            dir = -dir;
        }
        eEnergy = e.getEnergy();
    }

    private double angleToMiddle() {
        double dx = (getBattleFieldWidth() / 2) - getX();
        double dy = (getBattleFieldHeight() / 2) - getY();

        Vector2D toMiddle = new Vector2D(dx, dy);

        double angle = get180(toMiddle.getHeading() - get180(getHeading()))*dir;

        if(angle < 10){
           // angle += 20*dir;
        }
        //System.out.println("Test: "+ angle);
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

    public double getMaxDistance() {
        return Math.sqrt(getBattleFieldWidth() * getBattleFieldWidth() + getBattleFieldHeight() * getBattleFieldHeight());
    }

    public boolean closeEnough(double distance) {
        double hitRatio = (eData.size() != 0 ? (double) eData.size() : 1) / (shotsFired != 0 ? (double) shotsFired : 1);
        return hitRatio > distance / getMaxDistance();
    }

    public void addBullet(Bullet bullet, ScannedRobotEvent e) {
        if (bullet != null) {
            bullets.add(new BulletData(bullet, getRadarHeading(), getX(), getY(), e));
            shotsFired++;
        }
    }

    public boolean inPerfectRange(){
        return perfectRange;
    }
}

/*
MIKAELS ROBOTSKOLA!

kolla KD-Tree !!!!!!!!
Iterera vapen
Bygga om knuth morris pratt algo till vapensystem
Wall-smoothing
 */

/*
double d = 0;
if(distanceToWall < WALL_LIMIT){
    setTurnRight(90);
    if(d < distanceToWall){
    setTurnLeft(90);
    }
   }
 */
/*
    private void avoidWall(){
        double x = getX();
        double y = getY();
        double w = getBattleFieldWidth();
        double h = getBattleFieldHeight();
        double heading = getHeading();
        if(dir == -1){
            heading = (heading + 180) % 180;
        }

        if(heading % 90 == 0){
            heading += 10;
        }

        String location;

        if(heading < 90){
            location = "Up right and ";
            if(w - x < h - y){
                turnLeft(2*heading);
                location += "Right wall";
            }else{
                turnRight(heading);
                location += "Roof";
            }
        }else if(heading < 180){
            location = "Down right and ";
            if(w - x < y){
                turnRight(heading/2);
                location += "Right wall";
            }else{
                turnLeft(2*heading);
                location += "Floor";
            }
        }else if(heading < 270){
            location = "Down left and ";
            if(x < y){
                turnLeft(2*heading);
                location += "Left wall";
            }else{
                turnRight(360 - heading);
                location += "Floor";
            }
        }else{
            location = "Up left and ";
            if(x < h - y){
                turnRight((360 - heading)*2);
                location += "Left wall";
            }else{
                turnLeft(360 - heading);
                location += "Roof";
            }
        }
        System.out.println(location);
    }*/
