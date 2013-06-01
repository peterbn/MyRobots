package pbn.internals;

import robocode.AdvancedRobot;

import java.awt.geom.Point2D;

import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.signum;
import static robocode.util.Utils.isNear;
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
     * @param power       Power of bullet
     * @return Absolute bearing of shot (radians)
     * @throws NoSolutionException if not solution was found
     */
    public double getAbsoluteShotBearing(
            Track track,
            final Point2D firingPoint,
            final long firingTime,
            final double power) throws NoSolutionException {
        final Recording top = track.top();
        Point2D targetPos = top.advance(firingTime);
        double remainingDistance = targetPos.distance(firingPoint);
        long dt = 0;
        while (remainingDistance > (robot.getWidth() / 2)) {
            dt++; //one step closer
            targetPos = top.advance(firingTime + dt);
            double bulletDistance = getBulletDistance(dt, power);
            remainingDistance = targetPos.distance(firingPoint) - bulletDistance;
            if (bulletDistance > robot.getBattleFieldHeight() + robot.getBattleFieldWidth()) {
                throw new NoSolutionException();
            }
        }
        DebugGraphics.drawAimLine(robot.getGraphics(), firingPoint, targetPos);
        return getAbsoluteBearing(firingPoint, targetPos);
    }


    public static double getBulletTravelTime(double distanceToEnemy, double bulletPower) {
        return Math.ceil(distanceToEnemy / getBulletDistance(1, bulletPower));
    }

    public static double getBulletDistance(long ticks, double bulletPower) {
        return (20 - (3 * bulletPower)) * ticks;
    }

    public static double getAbsoluteBearing(Point2D from, Point2D to) {
        double dY = from.getY() - to.getY();
        double dX = from.getX() - to.getX();
        if (abs(dY) > 0) {
            return signum(dY) * normalAbsoluteAngle(Math.atan(dX / dY));
        } else {
            return dX > 0 ? PI / 2 : 3 * PI / 2;
        }
    }
}
