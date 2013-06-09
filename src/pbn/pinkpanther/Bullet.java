package pbn.pinkpanther;

import java.awt.geom.Point2D;

/**
 * Created with IntelliJ IDEA.
 * User: Peter
 * Date: 05-06-13
 * Time: 15:51
 * To change this template use File | Settings | File Templates.
 */
public class Bullet {
    public long fireTime;
    public double speed;
    public double heading;
    public Point2D position;
    public int offset;

    public Bullet(long fireTime, double speed, double heading,int offset, Point2D position) {
        this.fireTime = fireTime;
        this.speed = speed;
        this.heading = heading;
        this.offset = offset;
        this.position = position;
    }
}
