package baver;

import robocode.ScannedRobotEvent;

/**
 * Created by pontu on 2017-02-26.
 */
public class Radar {
    private BaverMain main;


    public Radar(BaverMain baverMain){
        main = baverMain;
    }

    void lockOnTarget(ScannedRobotEvent e){
        double radBear = getRadarBearing(e);
        main.setTurnRadarRight(radBear);
        main.refreshLock();
    }

    double getRadarBearing(ScannedRobotEvent e) {
        return e.getBearing() - (Util.get180(main.getRadarHeading() - Util.get180(main.getHeading())));
    }
}
