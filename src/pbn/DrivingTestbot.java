package pbn;

import pbn.internals.DrivingComputer;
import robocode.AdvancedRobot;

/**
 * Created by IntelliJ IDEA.
 * User: pbn
 * Date: 01-06-13
 * Time: 19:11
 * To change this template use File | Settings | File Templates.
 */
public class DrivingTestbot extends AdvancedRobot {

    private DrivingComputer drivingComputer;

    @Override
    public void run() {
        drivingComputer = new DrivingComputer(this);

        while (true) {
            drivingComputer.iterate();
        }
    }


}
