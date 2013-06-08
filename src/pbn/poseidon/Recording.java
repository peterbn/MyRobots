package pbn.poseidon;

import robocode.ScannedRobotEvent;
import robocode.annotation.SafeStatic;

import java.awt.geom.Point2D;

import static java.lang.Math.*;
import static robocode.util.Utils.*;

/**
 */
public class Recording implements Comparable<Recording>{
    public final long time;
    public final Point2D position;
    public final double
            velocity,
            turnRate,
            headingRadians;
    public final String name;
    public final double energy;
    public final int direction;
    @SafeStatic
    public static Poseidon robot;
    public final double bearing;

    Recording(ScannedRobotEvent event, Recording previous) {
        this.name = event.getName();
        double distance = event.getDistance();
        bearing = normalAbsoluteAngle(robot.getHeadingRadians() + event.getBearingRadians());
        headingRadians = event.getHeadingRadians();
        double x = sin(bearing) * distance + robot.getX();
        double y = cos(bearing) * distance + robot.getY();

        position = new Point2D.Double(x, y);
        time = event.getTime();
        velocity = event.getVelocity();
        energy = event.getEnergy();
        if (previous != null && previous.time < time) {
            turnRate = (headingRadians - previous.headingRadians) / (time - previous.time);
        } else {
            turnRate = 0;
        }
        if (velocity != 0 && previous != null) {
            direction = (int) signum(bearing - previous.bearing);
        } else if (previous != null) {
            direction = previous.direction;
        } else {
            direction = 1; //Chosen arbitrarily
        }
    }

    public Point2D advance(long time) {
        long dt = time - this.time;
        //iterative, because i'm stupid
        double dx = 0;
        double dy = 0;
        for (long t = 0; t < dt; t++) {
            dx += sin(headingRadians + turnRate * t) * velocity;
            dy += cos(headingRadians + turnRate * t) * velocity;
        }
        return new Point2D.Double(position.getX() + dx, position.getY() + dy);
    }


    @Override
    public int compareTo(Recording o) {
        if (energy <= 16 && o.energy > 16) {
            return -1;
        } else if (energy > 16 && o.energy <= 16) {
            return 1;
        } else if (energy <= 16 && o.energy <= 16) {
            return (int) (energy - o.energy);
        } else {
            return (int) (robot.currentPosition().distance(advance(robot.getTime())) - robot.currentPosition().distance(o.advance(robot.getTime())));
        }
    }
}
