package pbn.internals;

import robocode.AdvancedRobot;
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

    private final AdvancedRobot robot;
    private final long triggerTime;
    private final ShootingSolution shootingSolution;

    public FireGunCondition(AdvancedRobot robot, long triggerTime, ShootingSolution shootingSolution) {
        super("FireGun", 99);
        this.robot = robot;
        this.triggerTime = triggerTime;
        this.shootingSolution = shootingSolution;
    }

    @Override
    public boolean test() {
        robot.out.println("Testing for fire gun condition (" + triggerTime + ") at time: " + robot.getTime());
        return triggerTime <= robot.getTime() && robot.getGunTurnRemaining() <= 0;
    }

    public double getPower() {
        return shootingSolution.getBulletPower();
    }

    public ShootingSolution getShootingSolution() {
        return shootingSolution;
    }
}
