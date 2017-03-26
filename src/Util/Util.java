package Util;

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
}
