package pbn.internals;

import robocode.Condition;
import robocode.Robot;

/**
 * Created by IntelliJ IDEA.
 * User: pbn
 * Date: 01-06-13
 * Time: 14:01
 * To change this template use File | Settings | File Templates.
 */
public class FireGunCondition extends Condition {

    private final Robot robot;
    private final long triggerTime;
    private final double power;

    public FireGunCondition(Robot robot, long triggerTime, double power) {
        super("FireGun", 99);
        this.robot = robot;
        this.triggerTime = triggerTime;
        this.power = power;
    }

    @Override
    public boolean test() {
        robot.out.println("Testing for fire gun condition (" + triggerTime + ") at time: " + robot.getTime());
        return triggerTime <= robot.getTime();
    }

    public double getPower() {
        return power;
    }
}
