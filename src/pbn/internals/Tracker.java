package pbn.internals;

import robocode.AdvancedRobot;
import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

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

    public boolean hasEnemies() {
        return !buffer.isEmpty();
    }

    public void update(ScannedRobotEvent e) {
        Track track = buffer.get(e.getName());
        if (track == null) {
            track = new Track();
            buffer.put(e.getName(), track);
        }

        Recording target = Recording.record(robot, e);
        track.add(target);

        DebugGraphics.drawBigCircle(robot.getGraphics(), target.position.getX(), target.position.getY(), Color.green);
    }

    public void update(RobotDeathEvent event) {
        buffer.remove(event.getName());
    }

    public Track getClosestRobotTrack() {
        Track closest = null;
        for (Track track : buffer.values()) {
            if (closest == null || closest.top().distance(robot) > track.top().distance(robot)) {
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
