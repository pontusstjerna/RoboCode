package Util;

import baver.Shot;
import robocode.RobocodeFileOutputStream;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by pontu on 2017-03-01.
 */
public class Util {
    public static double get180(double angle){
        angle = angle % 360;
        if (angle > 180 && angle > 0) {
            return angle - 360;
        }else if(angle < -180){
            return angle + 360;
        }
        return angle;
    }

    public static boolean isInFront(double x1, double y1, double x2, double y2, double heading) {
        Vector2D rb = new Vector2D(x2 - x1, y2 - y1);
        double bearing = get180(get180(heading) - get180(rb.getHeading()));

        return bearing < 90 && bearing >= -90;
    }

    public static List<Shot> loadPreviousShots(File dataFile, int roundNum) {
        List<Shot> shots = null;


        if (roundNum > 0) {
            try {
                FileInputStream fout = new FileInputStream(dataFile);
                ObjectInputStream ois = new ObjectInputStream(fout);
                shots = (List<Shot>) ois.readObject();
                fout.close();
                ois.close();
                System.out.println(shots.size() + " " + dataFile.getName() + " successfully loaded.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (shots == null)
            shots = new ArrayList<>();

        return shots;
    }

    public static void saveShots(File dataFile, List<Shot> shots){
        try {
            RobocodeFileOutputStream fout = new RobocodeFileOutputStream(dataFile);
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

    public static boolean isBetween(double x1, double y1, double x2, double y2, double px, double py){
        return px >= x1 && px <= x2 && py <= y1 && py >= y2;
    }
}
