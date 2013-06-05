package pbn.internals;

import robocode.AdvancedRobot;
import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.signum;
import static pbn.internals.TargetingComputer.getAbsoluteBearing;
import static robocode.util.Utils.normalRelativeAngle;

/**
 * Utility subsystem to keep track of enemy robots
 */
public class Tracker {
    private final Map<String, Track> buffer;
    private final AdvancedRobot robot;

    public Tracker(AdvancedRobot robot) {
        this.robot = robot;
        buffer = new HashMap<String, Track>();
    }

    public synchronized boolean hasEnemies() {
        return !buffer.isEmpty();
    }

    public synchronized void update(ScannedRobotEvent e) {
        Track track = buffer.get(e.getName());
        robot.setInterruptible(true);
        if (track == null) {
            track = new Track();
            buffer.put(e.getName(), track);
        }

        Recording target = Recording.record(robot, e, track.top());
        track.add(target);
        Point2D robotPos = new Point2D.Double(robot.getX(), robot.getY());

        if (robot.getOthers() == 1) {
            Point2D advance = target.advance(robot.getTime() + 1);
            double d = normalRelativeAngle(
                    getAbsoluteBearing(robotPos, advance) - robot.getRadarHeadingRadians());
            if (d != 0) {
                robot.setTurnRadarRightRadians(d);
            }

        }
    }

    public synchronized void update(RobotDeathEvent event) {
        buffer.remove(event.getName());
    }

    public synchronized Track getClosestRobotTrack() {
        Track closest = null;
        for (Track track : buffer.values()) {
            Recording top = track.top();
            if (closest == null || closest.top().distance(robot) > top.distance(robot)) {
                closest = track;
            }
        }
        return closest;
    }

    public void paint(Graphics2D g) {
        for (Track track : buffer.values()) {
            DebugGraphics.drawTrackOverlay(g, track, robot.getTime());
        }
    }
}
