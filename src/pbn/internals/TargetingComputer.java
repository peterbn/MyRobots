package pbn.internals;

import robocode.AdvancedRobot;
import robocode.Rules;

import java.awt.geom.Point2D;

import static java.lang.Math.*;
import static robocode.util.Utils.normalAbsoluteAngle;

/**
 * Created by IntelliJ IDEA.
 * User: pbn
 * Date: 01-06-13
 * Time: 11:53
 * To change this template use File | Settings | File Templates.
 */
public class TargetingComputer {

    private final AdvancedRobot robot;

    public TargetingComputer(AdvancedRobot robot) {
        this.robot = robot;
    }

    /**
     * Get the
     *
     * @param track       Track to aim at
     * @param firingPoint Point from where the shot will be fired
     * @param firingTime  Absolute time of shot
     * @return Absolute bearing of shot (radians)
     * @throws NoSolutionException if not solution was found
     */
    public ShootingSolution getShootingSolution(
            Track track,
            final Point2D firingPoint,
            final long firingTime) throws NoSolutionException {
        final Recording target = track.top();
        double power = min(1.5, robot.getEnergy() / 2);
        Point2D targetPos = getTargetPos(firingPoint, firingTime, power, target);
        double distance = firingPoint.distance(targetPos);
        if (distance > 400 && power > 1) {
            power = 1;
            targetPos = getTargetPos(firingPoint, firingTime, power, target);
            distance = firingPoint.distance(targetPos);
        } else if (distance < 200 && robot.getEnergy() > 20) {
            power = 3;
            targetPos = getTargetPos(firingPoint, firingTime, power, target);
            distance = firingPoint.distance(targetPos);
        }
        DebugGraphics.drawAimLine(robot.getGraphics(), firingPoint, targetPos);
        double absoluteBearing = getAbsoluteBearing(firingPoint, targetPos);
        return new ShootingSolution(firingPoint, firingTime, targetPos, distance, absoluteBearing, power, target.name);
    }

    private Point2D getTargetPos(Point2D firingPoint, long firingTime, double power, Recording top) throws NoSolutionException {
        Point2D targetPos = top.advance(firingTime);
        double remainingDistance = targetPos.distance(firingPoint);
        long dt = 0;
        while (remainingDistance > (robot.getWidth() / 2) ) {
            dt++; //one step closer
            Point2D advance = top.advance(firingTime + dt);
            if (outsideBattleField(advance)) {
                robot.out.println("Lead is outside battlefield, aiming at wall location");
                break;
            }
            targetPos = advance;
            double bulletDistance = getBulletDistance(dt, power);
            remainingDistance = targetPos.distance(firingPoint) - bulletDistance;
            if (bulletDistance > robot.getBattleFieldHeight() + robot.getBattleFieldWidth()) {
                throw new NoSolutionException("Iteration exceeded limits");
            }
        }
        return targetPos;
    }


    private boolean outsideBattleField(Point2D targetPos) {
        int width2 = (int) robot.getWidth() / 4;
        return targetPos.getX() < width2
                || targetPos.getX() > robot.getBattleFieldWidth() - width2
                || targetPos.getY() < width2
                || targetPos.getY() > robot.getBattleFieldHeight() - width2;

    }


    public static double getBulletDistance(long ticks, double bulletPower) {
        return Rules.getBulletSpeed(bulletPower) * ticks;
    }

    public static double getAbsoluteBearing(Point2D from, Point2D to) {
        double dY = to.getY() - from.getY();
        double dX = to.getX() - from.getX();
        double angle;
        if (dX > 0) {
            angle = PI / 2 - Math.atan2(dY, dX);
        } else if (dX < 0) {
            angle = PI / 2 - Math.atan2(dY, dX);
        } else {
            angle = dY > 0 ? 0 : PI;
        }
        return normalAbsoluteAngle(angle);
    }

    public static double dx(double angle) {
        return sin(angle);
    }

    public static double dy(double angle) {
        return cos(angle);
    }
}
