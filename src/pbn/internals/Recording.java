package pbn.internals;

import robocode.Robot;
import robocode.ScannedRobotEvent;

import java.awt.geom.Point2D;

import static java.lang.Math.*;
import static pbn.internals.TargetingComputer.dx;
import static pbn.internals.TargetingComputer.dy;
import static robocode.util.Utils.normalAbsoluteAngle;

/**
 */
public class Recording {
    public final long time;
    public final Point2D position;
    public final double
            velocity,
            turnRate,
            headingRadians;
    public final String name;
    public final  double energy;

    public static Recording record(Robot robot, ScannedRobotEvent event, Recording previous) {
        return new Recording(robot, event, previous);
    }


    private Recording(Robot me, ScannedRobotEvent event, Recording previous) {
        this.name = event.getName();
        double distance = event.getDistance();
        double bearingRadians = normalAbsoluteAngle(toRadians(me.getHeading()) + event.getBearingRadians());
        headingRadians = event.getHeadingRadians();
        double x = (sin(bearingRadians)) * distance + me.getX() ;
        double y = cos(bearingRadians) * distance + me.getY() ;

        position = new Point2D.Double(x, y);
        time = event.getTime();
        velocity = event.getVelocity();
        energy = event.getEnergy();
        if (previous != null && previous.time < time) {
            turnRate = (headingRadians - previous.headingRadians) / (time - previous.time);
        } else {
            turnRate = 0;
        }

    }

    public double distance(Robot me) {
        return position.distance(me.getX(), me.getY());
    }

    public Point2D advance(long time) {
        long dt = time - this.time;
        //iterative, because i'm stupid
        double dx = 0;
        double dy = 0;
        for (long t = 0; t < dt; t++) {
            dx += dx(headingRadians + turnRate * t) * velocity;
            dy += dy(headingRadians + turnRate * t) * velocity;
        }
        return new Point2D.Double(position.getX() + dx, position.getY() + dy);
    }

    @Override
    public String toString() {
        return "Recording{" +
                "time=" + time +
                ", position=" + position +
                ", velocity=" + velocity +
                ", headingDegrees=" + toDegrees(headingRadians) +
                ", turnRateDegrees=" + toDegrees(turnRate) +
                ", name='" + name + '\'' +
                '}';
    }

}
