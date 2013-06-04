package pbn.internals;

import robocode.*;
import robocode.util.Utils;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.Random;

import static java.lang.Math.*;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static pbn.internals.DebugGraphics.drawPointerLine;
import static pbn.internals.DrivingComputer.Segment.*;
import static pbn.internals.TargetingComputer.dx;
import static pbn.internals.TargetingComputer.dy;
import static pbn.internals.TargetingComputer.getAbsoluteBearing;
import static robocode.util.Utils.normalAbsoluteAngle;
import static robocode.util.Utils.normalRelativeAngle;

/**
 */
public class DrivingComputer {


    private static final int DODGE_DISTANCE = 60;

    enum Segment {
        BOTTOM_LEFT(1, 1),
        BOTTOM_RIGHT(1, 2),
        TOP_RIGHT(2, 2),
        TOP_LEFT(2, 1),;
        public final int xMult;
        public final int yMult;

        private Segment(int xMult, int yMult) {
            this.xMult = xMult;
            this.yMult = yMult;
        }
        public Segment next() {
            Segment[] values = Segment.values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    private NavData navData;

    private final AdvancedRobot robot;
    private double turnRateDeg = Rules.MAX_TURN_RATE;

    private final int bfX, bfY;
    private final int bfX2, bfY2;
    private final int buffer;
    private final Random random = Utils.getRandom();
    private boolean dodging = false;

    public DrivingComputer(AdvancedRobot robot) {
        this.robot = robot;
        bfX = (int) robot.getBattleFieldWidth();
        bfY = (int) robot.getBattleFieldHeight();
        bfX2 = bfX / 2;
        bfY2 = bfY / 2;
        buffer = (int) (Math.max(robot.getHeight(), robot.getWidth()) + 10);
    }


    public synchronized boolean hasNavData() {
        return navData != null;
    }

    public synchronized void iterate() {
        //wait, and get ready for a turn onto the new course
        robot.waitFor(new MoveCompleteCondition(robot));
        dodging = false;
//        robot.waitFor(new TurnCompleteCondition(robot));
        //compute start and end points
        double currX = robot.getX();
        double currY = robot.getY();
        Point2D from = new Point2D.Double(currX, currY);
        Segment currentSegment = getSegment(robot.getX(), robot.getY());
        Segment nextSegment = getNextSegmentRandom();
        while (nextSegment == currentSegment) nextSegment = getNextSegmentRandom() ; // get another segment
        double nextX = min(bfX - buffer, max(buffer, random.nextInt(bfX2) + (nextSegment.xMult - 1) * bfX2));
        double nextY = min(bfY - buffer, max(buffer, random.nextInt(bfY2) + (nextSegment.yMult - 1) * bfY2));
        Point2D to = new Point2D.Double(nextX, nextY);
        navigate(from, to);
    }

    private void setTurnRateDeg(double turnRate) {
        this.turnRateDeg = turnRate;
        robot.setMaxTurnRate(turnRate);
    }

    private void navigate(Point2D from, Point2D to) {
        double currHeading = robot.getHeadingRadians();
        navData = computeNavigationArc(from, to);
        robot.setDebugProperty("NavData", navData.toString());
        setTurnRateDeg(Rules.MAX_TURN_RATE);
        robot.turnRightRadians(normalRelativeAngle(navData.startHeading - currHeading));
        robot.waitFor(new TurnCompleteCondition(robot));

        setTurnRateDeg(navData.getMaxTurnRateDeg());
        robot.setAhead(navData.distance);
        robot.setTurnRightRadians(navData.getTurn());
    }

    private NavData computeNavigationArc(Point2D from, Point2D to) {
        Point2D centerOfArc;

        double heading = getAbsoluteBearing(from, to);
        double directDistance = from.distance(to);
        Point2D midPoint = new Point2D.Double(
                from.getX() + dx(heading) * (directDistance/2),
                from.getY() + dy(heading) * (directDistance/2)
        );
        double centerHeading = normalAbsoluteAngle(heading + PI / 2);
        int xWall = midPoint.getX() > bfX2 ? bfX : 0;
        double dX = xWall - midPoint.getX();

        int yWall = midPoint.getY() > bfY2 ? bfY : 0;
        double dY = yWall - midPoint.getY();
        robot.setDebugProperty("centerHeading", String.valueOf(toDegrees(centerHeading)));
        robot.setDebugProperty("tan(theta)", String.valueOf(tan(centerHeading)));
        robot.setDebugProperty("dx", String.valueOf(dX));
        robot.setDebugProperty("dy", String.valueOf(dY));
        Point2D centerA = new Point2D.Double(xWall,
                midPoint.getY() + dX / tan(centerHeading)
        );
        Point2D.Double centerB = new Point2D.Double(
                midPoint.getX() + dY * tan(centerHeading),
                yWall);
        centerOfArc = midPoint.distance(centerA) < midPoint.distance(centerB) ? centerA : centerB;


        double traversedAngle = abs(getAbsoluteBearing(centerOfArc, to) - getAbsoluteBearing(centerOfArc, from));
        robot.setDebugProperty("traversedAngle" , String.valueOf(toDegrees(traversedAngle)));
        double radius = centerOfArc.distance(to);
        robot.setDebugProperty("radius" , String.valueOf(radius));
        double distance = traversedAngle * radius;

        double startHeading = normalAbsoluteAngle(getAbsoluteBearing(from, centerOfArc))
                + signum(normalRelativeAngle(heading - normalAbsoluteAngle(getAbsoluteBearing(from, centerOfArc)))) * PI/2;
        double endHeading = normalAbsoluteAngle(getAbsoluteBearing(to, centerOfArc))
                + signum(normalRelativeAngle(heading - normalAbsoluteAngle(getAbsoluteBearing(to, centerOfArc)))) * PI/2;
        //reverse the robot if that is closer to the start heading
        if (abs(normalRelativeAngle(startHeading - robot.getHeadingRadians())) > PI / 2) {
            startHeading = normalAbsoluteAngle(PI + startHeading);
            endHeading = normalAbsoluteAngle(PI + endHeading);
            distance *= -1;
        }
        traversedAngle = abs(normalRelativeAngle(startHeading - endHeading));
        robot.setDebugProperty("traversedAngle" , String.valueOf(toDegrees(traversedAngle)));


        return new NavData(from, to, centerOfArc, distance, startHeading, endHeading);
    }

    public Point2D getFiringPosition(long shotTime) {
        double x = robot.getX();
        double y = robot.getY();
        double distanceRemaining = robot.getDistanceRemaining();
        double remainingRadians = robot.getTurnRemainingRadians();
        double turnRate = (PI * this.turnRateDeg) / 180;
        double velocity = robot.getVelocity();
        double heading = robot.getHeadingRadians();
        for (long t = 0; t < shotTime; t++) {
            if (remainingRadians > 0) {
                double dh = signum(remainingRadians) * min(abs(remainingRadians), turnRate);
                heading += dh;
                remainingRadians -= dh;
            }
            double dp = 0;
            if (distanceRemaining > 0) {
                dp = min(distanceRemaining, velocity);
            } else if (distanceRemaining < 0) {
               dp = max(distanceRemaining, velocity);
            }
            distanceRemaining -= dp;
            x += dp * dx(heading);
            y += dp * dy(heading);
        }
        return new Point2D.Double(x, y);
    }


    public void onHitRobot(HitRobotEvent event) {
        if (dodging) return;
        dodging = true;
        double eventBearing = event.getBearingRadians();
        robot.setTurnRightRadians(0);
        if (abs(normalRelativeAngle(eventBearing)) <= PI / 2) {
            robot.setAhead(-150);
        } else {
            robot.setAhead(150);
        }
    }

    public void onHitWall(HitWallEvent event) {
        robot.setAhead(0);
        robot.setTurnRightRadians(0);
    }


    public void onHitByBullet(HitByBulletEvent e) {
        if (dodging) return;
        dodging = true;
        int dodge = 30 + random.nextInt(120);
        setTurnRateDeg(Rules.MAX_TURN_RATE);
        if (robot.getDistanceRemaining() != 0) {
            robot.setAhead(-signum(robot.getDistanceRemaining()) * dodge);
        } else {
            robot.setAhead(dodge);
        }
    }

    public void onStatus(StatusEvent e) {
        //Do wall skimming
    }

    private Segment getSegment(double x, double y) {
        boolean left = x < bfX2;
        boolean bottom = y < bfY2;
        return bottom ? left ? BOTTOM_LEFT : BOTTOM_RIGHT : left ? TOP_LEFT : TOP_RIGHT;
    }

    private Segment getNextSegmentRandom() {
        return Segment.values()[random.nextInt(4)];
    }

    public void paint(Graphics2D g) {
        if (navData != null) {
            drawPointerLine(g, navData.from, navData.to, Color.GREEN);
            drawPointerLine(g, navData.from, navData.centerOfArc, Color.BLUE);
            drawPointerLine(g, navData.to, navData.centerOfArc, Color.BLUE);

            Point2D pos = getFiringPosition(0);
            Point2D firingPosition = getFiringPosition(9);
            drawPointerLine(g, pos, firingPosition, Color.CYAN);
        }
    }

}

class NavData {
    final Point2D from;
    final Point2D to;
    final Point2D centerOfArc;
    final double distance;
    final double startHeading;
    final double endHeading;

    NavData(Point2D from, Point2D to, Point2D centerOfArc, double distance, double startHeading, double endHeading) {
        this.from = from;
        this.to = to;
        this.centerOfArc = centerOfArc;
        this.distance = distance;
        this.startHeading = startHeading;
        this.endHeading = endHeading;
    }

    double getTurn() {
        return normalRelativeAngle(endHeading - startHeading);
    }

    double getMaxTurnRateDeg() {
        long time = timeToTargetFromStop();
        return abs(toDegrees(normalRelativeAngle(endHeading - startHeading))) / time;
    }

    long timeToTargetFromStop() {
        long t = 0;
        double a = Rules.ACCELERATION;
        double d = Rules.DECELERATION;
        long dtA = (long) (Rules.MAX_VELOCITY / a);
        t += dtA;
        long dtD = (long) (Rules.MAX_VELOCITY / d);
        t += dtD;
        double accelerationDistance = (a * pow(dtA, 2))/2;
        double decelerationDistance = (d * pow(dtD, 2))/2;
        double remainingDistance = abs(distance) - (accelerationDistance + decelerationDistance);
        t += max(remainingDistance / Rules.MAX_VELOCITY, 0);
        return t;
    }

    @Override
    public String toString() {
        return "NavData{" +
                "\n\tfrom=" + from +
                "\n\tto=" + to +
                "\n\tcenterOfArc=" + centerOfArc +
                "\n\tdistance=" + distance +
                "\n\tturnRate=" + getMaxTurnRateDeg() +
                "\n\tstartHeading=" + toDegrees(startHeading) +
                "\n\ttimeToTarget=" + timeToTargetFromStop() +
                "\n\tendHeading=" + toDegrees(endHeading) +
                "\n\t}";
    }
}
