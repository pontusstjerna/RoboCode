package baver;

/**
 * Created by pontu on 2017-03-10.
 */
public final class Reference {
    public static final double FIREPOWER = 3;
    public static final int LOCK_TIMEOUT = 60;
    public static final int MAX_DISTANCE = 400;
    public static final int MIN_DISTANCE = 200;
    public static final double WALL_LIMIT = 100;
    public static final double AIM_LIMIT = 0.5;

    /*
    return Math.sqrt(dVelocity*dVelocity + dDistance*dDistance + dRobVel*dRobVel +
        dTurretBearing*dTurretBearing + dDeltaHeading*dDeltaHeading + dDir*dDir + dBearing*dBearing);
     */

    public static final int MAX_DELTA_HEADING = 180;
}
