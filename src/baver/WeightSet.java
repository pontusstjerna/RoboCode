package baver;

/**
 * Created by Pontus on 2017-03-26.
 */
public class WeightSet {
    public enum Weights {ENEMY_VELOCITY, DISTANCE_BETWEEN_ROBOTS, FRIENDLY_VELOCITY, BEARING_FROM_TURRET,
        HEADING_DIFFERENCE, FRIENDLY_DIRECTION, ENEMY_DIRECTION, BEARING_DIFFERENCE}

    public static double[] NEUTRAL_WEIGHTS = new double[] {1,1,1,1,1,1,1,1};

    private double[] weights;

    public WeightSet(double enemyVelocity, double distanceBetweenRobots, double friendlyVelocity, double bearingFromTurret,
                     double headingDifference, double friendlyDirection, double enemyDirection, double bearingDifference){

        weights = new double[Weights.values().length];
        weights[0] = enemyVelocity;
        weights[1] = distanceBetweenRobots;
        weights[2] = friendlyVelocity;
        weights[3] = bearingFromTurret;
        weights[4] = headingDifference;
        weights[5] = friendlyDirection;
        weights[6] = enemyDirection;
        weights[7] = bearingDifference;
    }

    public WeightSet(double[] weights){
        this.weights = weights;
    }

    public WeightSet(){
        this(1,1,1,1,1,1,1,1);
    }

    public double getWeight(Weights weight){
        return weights[weight.ordinal()];
    }

    public void setWeight(Weights weight, double value){
        weights[weight.ordinal()] = value;
    }

    public double getMaxWeight(){
        double max = 0;
        for(double w : weights){
            if(w > max)
                max = w;
        }

        return max;
    }

    public double getMinWeight(){
        double min = 0;
        for(double w : weights){
            if(w < min)
                min = w;
        }

        return min;
    }

    public double getSummedWeights(){
        double sum = 0;
        for(double w : weights)
            sum += w;

        return sum;
    }
}
