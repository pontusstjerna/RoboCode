package pontus;

import robocode.ScannedRobotEvent;

/**
 * Created by Pontus on 2016-02-08.
 */
public class Radar {
    private Korven korven;

    public Radar(Korven robot){
        korven = robot;
    }

    public void lock(ScannedRobotEvent e){
        double radBear = getRadarBearing(e);
        korven.setTurnRadarRight(radBear);
    }

    public double getRadarBearing(ScannedRobotEvent e) {
        return e.getBearing() - (korven.get180(korven.getRadarHeading() - korven.get180(korven.getHeading())));
    }
}
