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
public class Korven extends AdvancedRobot
{
    private int dir = 1; // 1 = right, -1 = left
    private Random rand;
    private final int DISTANCE = 300;
    private double eEnergy = 0;
    private final int SCAN_LIMIT = 2;
    private final double WALL_LIMIT = 50;
    private boolean locked = false;
    private double adjust = 1;
    private boolean vsJaguar = false;
    private int shotsFired = 0;
    private List<BulletData> bullets = new ArrayList<>();
    private List<BulletData> eData = new ArrayList<>();

    @Override
    public void run() {

        setColors(new Color(148, 255, 68),
                new Color(27, 104, 27),
                new Color(65, 154, 42)); // body,gun,radar

        setAdjustRadarForGunTurn(true);
        setAdjustRadarForRobotTurn(true);
        setAdjustGunForRobotTurn(true);
        rand = new Random();

        // Robot main loop
        while(true) {
            if(getDistanceToWall() > WALL_LIMIT){
                /*for(int i = 0; i < 10 && locked; i++){
                    int turnFac = 10;
                    setTurnRadarRight(i*turnFac);
                    setTurnRadarRight(-2*i*turnFac);
                    setTurnRadarRight(i*turnFac);
                }*/
                if(!locked){
                    setTurnRadarRight(360);
                }
            }else{
                setAhead(100*dir);
            }
            execute();
        }
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        lock(e);
        if(!vsJaguar || e.getEnergy() < 10){
          lockAndFire(e);
        }
        keepDistance(e);
       avoid(e);
    }

    @Override
    public void onHitByBullet(HitByBulletEvent e) {
        // Replace the next line with any behavior you would like
    }

    @Override
    public void onBulletHit(BulletHitEvent e){
        for(BulletData b : bullets){
            if(b.getBullet().equals(e.getBullet())){
                b.setHitPoint(e.getBullet().getX(), e.getBullet().getY());
                eData.add(b);
            }
        }
        bullets.clear();
        if(eData.size() > 0){
            System.out.println("Hits: " + eData.size());
        }
    }

    @Override
    public void onHitWall(HitWallEvent e) {
        //turnRight(getRadarHeading() - getHeading());
        //avoidWall();
        //setAhead(WALL_LIMIT*2);
        //dir = -dir;
        //clearAllEvents();
        //turnToMiddle();
        dir = -dir;
        setAhead(WALL_LIMIT*2*dir);
    }

    @Override
    public void onPaint(Graphics2D g) {
        double dx = (getBattleFieldWidth()/2) - getX();
        double dy = (getBattleFieldHeight()/2) - getY();

        Vector2D toMiddle = new Vector2D(dx,dy);
        Vector2D heading = new Vector2D(Math.signum(getVelocity())*100*Math.cos(-(getHeadingRadians()+(Math.PI/2))),
                Math.signum(getVelocity())*100*Math.sin(-(getHeadingRadians()+(Math.PI/2))));

        toMiddle.paintVector(g, getX(), getY());
        heading.paintVector(g, getX(), getY());

        //g.setColor(java.awt.Color.RED);
        //g.drawLine((int)getX(), (int)getY(), (int)(getX() + Math.signum(getVelocity())* 100*Math.cos(-getHeadingRadians() + Math.PI/2)), (int)(getY() + Math.signum(getVelocity()) *  100*Math.sin(-getHeadingRadians() + Math.PI/2)));
    }

    private void lock(ScannedRobotEvent e){
        double radBear = getRadarBearing(e);
        System.out.println(String.valueOf(radBear));
        setTurnRadarRight(radBear);
        double deltaAngle = getRadarHeading() - getGunHeading();
        setTurnGunRight(deltaAngle);

        locked = true;
    }

    private void lockAndFire(ScannedRobotEvent e){
        Bullet bullet;
        double distance = e.getDistance();
        double deltaAngle = getRadarHeading() - getGunHeading();

        if(true) { //No saved data
            if (deltaAngle < 2 && deltaAngle > -2 && closeEnough(distance)) {
                double eSpeed = e.getVelocity();
                double firePower = 3 - 3*distance/getMaxDistance();
                double bulletSpeed = 20 - 3*firePower;
                double eRevHead = e.getHeadingRadians() - getGunHeadingRadians();
                /*double bulletDistance = Math.sqrt(Math.pow(distance, 2) +
                        Math.pow(((distance*Math.sin(eRevHead)*eSpeed)/bulletSpeed),2));*/
                double bulletDistance = Math.sqrt(Math.pow(distance, 2) +
                        Math.pow(((distance*eSpeed)/bulletSpeed),2)-
                2*distance*((distance*eSpeed)/bulletSpeed)*Math.cos(eRevHead));
                double angle = distance/bulletDistance;
                //System.out.println("Degrees: " + Math.acos(angle));

                //Bullet velocity:	20 - 3 * firepower.
                if(bulletDistance != 0 && bulletDistance < getMaxDistance()){
                    setTurnGunRightRadians(Math.acos(angle) + angle*0.1f);
                }
                bullet = (setFireBullet(firePower));
                if (bullet != null) {
                    bullets.add(new BulletData(bullet, getRadarHeading(), getX(), getY(), e));
                    shotsFired++;
                }
            }
        }else{
            for(BulletData b : eData){
                if(b.getRobot().getBearing() == e.getBearing() && getRadarHeading() == b.getRadarHeading() &&
                        e.getDistance() == b.getRobot().getDistance()){
                    bullet = (setFireBullet(3 - (e.getDistance() / getMaxDistance())));
                    if (bullet != null) {
                        bullets.add(new BulletData(bullet, getRadarHeading(), getX(), getY(), e));
                    }
                }
            }
        }
    }

    private double getRadarBearing(ScannedRobotEvent e) {
        return e.getBearing() - (get180(getRadarHeading() - get180(getHeading())));
    }

    private double get180(double angle){
        if (angle > 180) {
            return angle - 360;
        }
        return angle;
    }

    private void keepDistance(ScannedRobotEvent e){
        /*if(e.getDistance() > DISTANCE){
            adjust = 1.1;
        }else if(e.getDistance() < DISTANCE){
            adjust = 0.9;
        }*/
        if(e.getDistance() > DISTANCE){
            setTurnRight(e.getBearing() - 20);
        }else{
            if(getDistanceToWall() > WALL_LIMIT){
                setTurnRight((e.getBearing() - 90*adjust));
            }else{

            }
        }

        //System.out.println("Adjusting: " + adjust + " Distance: " + e.getDistance());



       // System.out.println((e.getBearing() - 90)*adjust*dir);
        setAhead((rand.nextInt(100) + 70)*dir);
    }

    private void avoid(ScannedRobotEvent e){
        if(e.getEnergy() < eEnergy && getDistanceToWall() > WALL_LIMIT && e.getDistance() < DISTANCE){
            dir = -dir;
        }
        eEnergy = e.getEnergy();
    }

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
    }

    private void turnToMiddle(){
        double dx = (getBattleFieldWidth()/2) - getX();
        double dy = (getBattleFieldHeight()/2) - getY();

        Vector2D toMiddle = new Vector2D(dx,dy);
        Vector2D heading = new Vector2D(Math.signum(getVelocity())*100*Math.cos(-(getHeadingRadians()+(Math.PI/2))),
                Math.signum(getVelocity())*100*Math.sin(-(getHeadingRadians()+(Math.PI/2))));

        System.out.println("Degrees: " + toMiddle.minAngle(heading) + " Heading vector: " + heading);

            setTurnRight(toMiddle.minAngle(heading));
    }

    private double getDistanceToWall(){
        double[] distances = {getX(), getY(), getBattleFieldWidth() - getX(), getBattleFieldHeight() - getY()};
        double distance = distances[0];

        for(int i = 0; i < distances.length; i++){
            if(distances[i] < distance){
                distance = distances[i];
            }
        }

        return distance;
    }

    private double getMaxDistance(){
         return Math.sqrt(getBattleFieldWidth()*getBattleFieldWidth() + getBattleFieldHeight()*getBattleFieldHeight());
    }

    private boolean closeEnough(double distance){
        double hitRatio = (eData.size() != 0 ? (double)eData.size() : 1)/(shotsFired != 0 ? (double)shotsFired : 1);
        //System.out.println("Hits: " + eData.size() + " Shotsfired: " + shotsFired);
        //System.out.println("Required percent: " + distance/getMaxDistance() + " Percent: " + hitRatio);
        return hitRatio > distance/getMaxDistance();
        //If there are many hits, you can shoot further
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
