package pbn.pinkpanther;

import robocode.ScannedRobotEvent;
import robocode.annotation.SafeStatic;

import java.awt.geom.Point2D;
import java.util.ArrayList;

import static java.lang.Math.*;
import static robocode.util.Utils.normalAbsoluteAngle;

/**
 */
public class Recording implements Comparable<Recording>{
    public final long time;
    public final Point2D position;
    public final double
            velocity,
            turnRate,
            heading;
    public final String name;
    public final  double energy;
    @SafeStatic
    public static PinkPanther robot;

    Recording(ScannedRobotEvent event, Recording previous) {
        this.name = event.getName();
        double distance = event.getDistance();
        double bearingRadians = normalAbsoluteAngle(robot.getHeadingRadians() + event.getBearingRadians());
        heading = event.getHeadingRadians();
        double x = sin(bearingRadians) * distance + robot.getX();
        double y = cos(bearingRadians) * distance + robot.getY();

        position = new Point2D.Double(x, y);
        time = event.getTime();
        velocity = event.getVelocity();
        energy = event.getEnergy();
        if (previous != null && previous.time < time) {
            turnRate = (heading - previous.heading) / (time - previous.time);
        } else {
            turnRate = 0;
        }
    }

    //A score function - for target selection
    public int score() {
        int closeBots = 0;
        for (Recording recording : robot.tracks.values()) {
            closeBots += recording.advance(robot.getTime()).distance(advance(robot.getTime())) < 200 ? 1 : 0;
        }
        return (int) max(0, (73. * (1200 - robot.currentPosition().distance(advance(robot.getTime())))) / 1200
                + (15. * (100 - energy)) / 100
                + (20. * closeBots) / robot.getOthers()
                + 2 * (name.equals(robot.target) ? 1 : 0)
        );
    }

    public Point2D advance(long time) {
        long dt = time - this.time;
        //iterative, because i'm stupid
        double dx = 0;
        double dy = 0;
        for (long t = 0; t < dt; t++) {
            dx += sin(heading + turnRate * t) * velocity;
            dy += cos(heading + turnRate * t) * velocity;
        }
        return new Point2D.Double(position.getX() + dx, position.getY() + dy);
    }


    @Override
    public int compareTo(Recording o) {
        return o.score() - score();
    }
}
