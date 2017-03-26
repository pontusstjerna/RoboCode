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

    public static final int MAX_DELTA_HEADING = 180;

    public static final double[] AIMING_WEIGHTS = {
            1, //ENEMY_VELOCITY
            1, //DISTANCE_BETWEEN_ROBOTS
            1, //FRIENDLY_VELOCITY
            1, //BEARING_FROM_TURRET
            1, //HEADING_DIFFERENCE
            1, //FRIENDLY_DIRECTION
            1, //ENEMY_DIRECTION
            1  //BEARING_DIFFERENCE
    };

    public static final double[] AVOIDING_WEIGHTS = {
            1, //ENEMY_VELOCITY
            1, //DISTANCE_BETWEEN_ROBOTS
            1, //FRIENDLY_VELOCITY
            1, //BEARING_FROM_TURRET
            1, //HEADING_DIFFERENCE
            1, //FRIENDLY_DIRECTION
            1, //ENEMY_DIRECTION
            1  //BEARING_DIFFERENCE
    };
}
