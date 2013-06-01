package pbn.internals;

import robocode.Robot;
import robocode.ScannedRobotEvent;

import java.awt.geom.Point2D;

import static java.lang.Math.*;
import static robocode.util.Utils.normalAbsoluteAngle;

/**
 * Created by IntelliJ IDEA.
 * User: pbn
 * Date: 01-06-13
 * Time: 11:03
 * To change this template use File | Settings | File Templates.
 */
public class Recording {
    public final long time;
    public final Point2D position;
    public final double
            velocity,
            headingRadians;
    public final String name;

    public static Recording record(Robot robot, ScannedRobotEvent event) {
        return new Recording(robot, event);
    }



    private Recording(Robot me, ScannedRobotEvent event) {
        this.name = event.getName();
        double distance = event.getDistance();
        double bearingRadians = normalAbsoluteAngle(toRadians(me.getHeading()) + event.getBearingRadians());
        headingRadians = event.getHeadingRadians();
        double x = (sin(bearingRadians)) * distance + me.getX() ;
        double y = cos(bearingRadians) * distance + me.getY() ;

        position = new Point2D.Double(x, y);
        time = event.getTime();
        velocity = event.getVelocity();
    }

    public double distance(Robot me) {
        return position.distance(me.getX(), me.getY());
    }

    public double currentBearing(Robot me, long dt) {
        Point2D currentPosition = advance(me.getTime() + dt);
        double myX = me.getX();
        double myY = me.getY();

        double targetY = currentPosition.getY();
        double targetX = currentPosition.getX();
        if (targetX != myX) {
            return normalAbsoluteAngle(PI/2 - Math.atan2(targetY - myY, targetX - myX));
        } else {
            return targetX > myX ? 90 : 270;
        }
    }

    public Point2D advance(long time) {
        long dt = time - this.time;
        double dx = getDX() * dt;
        double dy = getDY() * dt;
        return new Point2D.Double(position.getX() + dx, position.getY() + dy);
    }

    @Override
    public String toString() {
        return "Recording{" +
                "time=" + time +
                ", position=" + position +
                ", velocity=" + velocity +
                ", headingDegrees=" + toDegrees(headingRadians) +
                ", name='" + name + '\'' +
                '}';
    }

    public double getDX() {
        return -cos(headingRadians + PI/2) * velocity;
    }

    public double getDY() {
        return sin(headingRadians + PI/2) * velocity;
    }
}
