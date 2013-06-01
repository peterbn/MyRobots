package pbn.internals;

import java.awt.geom.Point2D;

/**
 * Combined position and heading information
 */
public class PositionInformation {
    public final Point2D position;
    public final double heading;

    public PositionInformation(Point2D position, double heading) {
        this.position = position;
        this.heading = heading;
    }
}
