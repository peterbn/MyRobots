package pbn.poseidon;

import java.awt.geom.Point2D;
import java.util.Map;

import static java.lang.Math.*;
import static pbn.poseidon.Poseidon.getAbsoluteBearing;
import static robocode.util.Utils.normalRelativeAngle;

/**
 */
public class Wave {
    Point2D origin;
    double heading;
    double velocity;
    long fireTime;
    String target;
    int direction;
    int[] data;

    public Wave(Point2D origin, double heading, double velocity, long fireTime, String target, int direction, int[] data) {
        this.origin = origin;
        this.heading = heading;
        this.velocity = velocity;
        this.fireTime = fireTime;
        this.target = target;
        this.direction = direction;
        this.data = data;
    }

    public boolean checkBreak(Map<String, Recording> tracks, long time) {
        Recording target = tracks.get(this.target);
        if (target != null) {
            Point2D currentPoint = target.advance(time);
            double dp = currentPoint.distance(origin) - (time - fireTime) * velocity;
            if ( abs(dp) < 10) { // Wave intersects bot
                double guessFactor = max(-1, min(1, normalRelativeAngle(getAbsoluteBearing(origin, currentPoint) - heading)  / asin(8 / velocity))) * direction;
                int index = (int) round((data.length - 1) / 2 * (guessFactor + 1));
                data[index]++;
                return true;
            } else if (dp < -10) { // Wave passed target
                return true;
            }
        }
        return false;
    }
}
