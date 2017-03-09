package baver;

import robocode.AdvancedRobot;
import robocode.Bullet;
import robocode.RobocodeFileOutputStream;
import robocode.ScannedRobotEvent;

import java.awt.*;
import java.io.*;
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
    private boolean active = false;

    public LearningGun(AdvancedRobot robot){
        this.robot = robot;
        shots = loadPreviousShots();
    }

    //Returns between 0 and 1 on how sure it is
    //No it doesn't
    public double aimAndFire(ScannedRobotEvent e){
        if(robot.getGunTurnRemaining() < AIM_LIMIT && robot.getGunHeat() == 0)
            registerBullet(e, robot.setFireBullet(3));

        turnToTarget(e);
        return 1;
    }

    public void setActive(boolean isActive){
        if(!active && isActive)
            System.out.println("Learning gun activated. Hit rate: " + getHitRate()*100 + "%.");

        active = isActive;
    }

    public boolean isActive(){
        return active;
    }

    public void registerBullet(ScannedRobotEvent e, Bullet b){
        Shot newShot = new Shot(e, robot, b);
        shots.add(newShot);

      //  System.out.println("Turret bearing: " + newShot.getTurretBearing());
    }

    public double getHitRate(){
        long totalNotInAir = shots.stream().filter(x -> x.getState() != Shot.states.IN_AIR).count();
        if(totalNotInAir < 2)
            return 0;

        return ((double)(shots.stream().filter(x -> x.getState() == Shot.states.HIT).count()))/
                totalNotInAir;
    }

    public int getShotsSize(){
        return shots.size();
    }
    public long getHitShots() { return shots.stream().filter(x -> x.getState() == Shot.states.HIT).count();}

    public void paintExpectations(Graphics g){
        g.setColor(new Color(255,0,255));
        if(currentBestMatched != null)
            g.fillRoundRect((int)currentBestMatched.getBullet().getX(), (int)currentBestMatched.getBullet().getY(), 10, 10, 10, 10);
    }

    public void saveShots(){
        System.out.println("Shots hit: " + shots.stream().filter(s -> s.getState() == Shot.states.HIT).count());
        System.out.println("Shots missed: " + shots.stream().filter(s -> s.getState() == Shot.states.MISS).count());

        //Remove in air
        List toRemove = new ArrayList<>();
        shots.stream().filter(s -> s.getState() == Shot.states.IN_AIR).forEach(s -> toRemove.add(s));
        shots.removeAll(toRemove);

        try {
            RobocodeFileOutputStream fout = new RobocodeFileOutputStream(robot.getDataFile("friendlyShots"));
            ObjectOutputStream oos = new ObjectOutputStream(fout);
            oos.writeObject(shots);
            fout.close();
            oos.close();

        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void registerBulletHit(Bullet b){
        //System.out.println("My bullet hit!");
        shots.stream().filter(x -> x.getState() == Shot.states.IN_AIR && x.getBullet().equals(b)).findFirst().get().setHit();
    }

    public void registerBulletMiss(Bullet b){
        //System.out.println("My bullet missed!");
        shots.stream().filter(x -> x.getState() == Shot.states.IN_AIR && x.getBullet().equals(b)).findFirst().get().setMiss();
    }

    private void turnToTarget(ScannedRobotEvent e){
        Shot pretendShot = new Shot(e, robot);
        Shot shot = getBestMatched(e, pretendShot);
        if(shot == null)
            return;

        currentBestMatched = shot;

        double deltaAngle = pretendShot.getTurretBearing() - shot.getTurretBearing();

        if(deltaAngle < 180 && deltaAngle > -180){
            robot.setTurnGunRight(deltaAngle);
         //   System.out.println("DeltaAngle: " + deltaAngle);
        }else if(deltaAngle > 180){
            robot.setTurnGunRight(360 - deltaAngle);
        }else{
            robot.setTurnGunRight(360 + deltaAngle);
        }

        /*System.out.println("Old turretbearing: " + shot.getTurretBearing());
        System.out.println("Cur turretbearing: " + pretendShot.getTurretBearing());
        System.out.println("D   turretbearing: " + deltaAngle);
        System.out.println("--");*/
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

        private List<Shot> loadPreviousShots() {
        List<Shot> shots = null;


        if (robot.getRoundNum() > 0) {
            try {
                FileInputStream fout = new FileInputStream(robot.getDataFile("friendlyShots"));
                ObjectInputStream ois = new ObjectInputStream(fout);
                shots = (List<Shot>) ois.readObject();
                fout.close();
                ois.close();
                System.out.println(shots.size() + " friendly shots successfully loaded.");
            } catch (Exception e) {
                System.out.println("Unable to load any friendly shots!");
            }
        }

        if (shots == null)
            shots = new ArrayList<>();

        return shots;
    }
}
