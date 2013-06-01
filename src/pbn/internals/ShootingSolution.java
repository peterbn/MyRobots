package pbn.internals;

import robocode.util.Utils;

import java.awt.geom.Point2D;

/**
 * A shooting solution computed by the TargetingComputer
 */
public class ShootingSolution {

    private final Point2D shootingPosition;
    private final Point2D targetPosition;
    private final double distance;
    private final double absoluteShotHeading;
    private final double bulletPower;
    private final String robotName;

    public ShootingSolution(Point2D shootingPosition,
                            Point2D targetPosition,
                            double distance,
                            double absoluteShotHeading,
                            double bulletPower,
                            String robotName) {
        this.shootingPosition = shootingPosition;
        this.targetPosition = targetPosition;
        this.distance = distance;
        this.absoluteShotHeading = absoluteShotHeading;
        this.bulletPower = bulletPower;
        this.robotName = robotName;
    }

    public Point2D getShootingPosition() {
        return shootingPosition;
    }

    public Point2D getTargetPosition() {
        return targetPosition;
    }

    public double getDistance() {
        return distance;
    }

    public double getAbsoluteShotHeading() {
        return absoluteShotHeading;
    }

    public double getBulletPower() {
        return bulletPower;
    }

    public String getRobotName() {
        return robotName;
    }

    @Override
    public String toString() {
        return "ShootingSolution{" +
                "shootingPosition=" + shootingPosition +
                ", targetPosition=" + targetPosition +
                ", distance=" + distance +
                ", absoluteShotHeading=" + (int)(absoluteShotHeading * 180) / Math.PI +
                ", bulletPower=" + bulletPower +
                '}';
    }
}
