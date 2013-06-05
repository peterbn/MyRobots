package pbn.omgnator;

import robocode.*;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.*;
import static java.lang.Math.PI;
import static robocode.util.Utils.normalAbsoluteAngle;
import static robocode.util.Utils.normalRelativeAngle;

/**
 * An anti-gravity bot
 */
public class OMGNator extends AdvancedRobot {

    private Map<String, Recording> tracks;
    private int bfX, bfY, bfX2, bfY2;
    private int direction = 1;
    private String targetBot;

    @Override
    public void run() {
        setColors(Color.PINK, new Color(0, 0xFF, 0xFF, 0x40), new Color(0,0,0,0));
        tracks = new HashMap<String, Recording>(getOthers());
        bfX = (int) getBattleFieldWidth();
        bfY = (int) getBattleFieldHeight();
        bfX2 = bfX / 2;
        bfY2 = bfY / 2;
        setAdjustRadarForGunTurn(true);
        setAdjustRadarForRobotTurn(true);
        setAdjustGunForRobotTurn(true);

        //noinspection InfiniteLoopStatement
        do {
            if (targetBot == null) {
                setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
            } else {
                try {
                    Recording recording = tracks.get(targetBot);
                    if (recording.time < getTime() - 3) {
                        targetBot = null;
                    }
                } catch (NullPointerException e) {
                    setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
                    targetBot = null;
                }
            }
            navigate();
            execute();
        } while (true);
    }

    private void navigate() {
        Map<Point2D, Integer> forcePoints = getForcePoints();
        double[] totalVector = new double[]{0, 0};
        for (Map.Entry<Point2D, Integer> forcePoint : forcePoints.entrySet()) {
            double[] vector = forceVector(forcePoint.getKey(), forcePoint.getValue());
            totalVector[0] += vector[0];
            totalVector[1] += vector[1];
        }
        Point2D destination = new Point2D.Double(getX() + totalVector[0], getY() + totalVector[1]);
        double bearing = getAbsoluteBearing(currentPosition(), destination);
        double turn = normalRelativeAngle(bearing - getHeadingRadians());
        double distance = currentPosition().distance(destination);
        setDebugProperty("Distance to destination", String.valueOf(distance));
        setDebugProperty("total vector", "(" + totalVector[0] + ", " + totalVector[1] +")");
        setAhead(direction * 20);
        setTurnRightRadians(direction* turn);
    }

    private Map<Point2D, Integer> getForcePoints() {
        Point2D pos = currentPosition();
        Map<Point2D, Integer> points = new HashMap<Point2D, Integer>();
        int fixedPointValue = max(2, getOthers() / 2);
        points.put(new Point2D.Double(bfX2, bfY2), fixedPointValue); //midpoint
        points.put(new Point2D.Double(pos.getX(), pos.getY() < bfY2 ? 0: bfY), fixedPointValue); //walls
        points.put(new Point2D.Double(pos.getX() < bfX2 ? 0 : bfX, pos.getY()), fixedPointValue);
        for (Recording bot : tracks.values()) {
            points.put(bot.advance(getTime()+1), 2); //bots
        }
        return points;
    }


    private double[] forceVector(Point2D point, int weight) {
        double F = (1.4 * (weight)) / point.distance(currentPosition());
        double bearing = getAbsoluteBearing(point, currentPosition());
        return new double[]{dx(bearing) * F, dy(bearing) * F};
    }

    private Point2D currentPosition() {
        return new Point2D.Double(getX(), getY());
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent event) {
        Recording record = new Recording(this, event, tracks.get(event.getName()));
        tracks.put(record.name, record);
        if ( getOthers() == 1  ) {
            targetBot = event.getName();
            setInterruptible(true);
            Point2D advance = record.advance(getTime() + 1);
            double d = normalRelativeAngle(
                    getAbsoluteBearing(currentPosition(), advance) - getRadarHeadingRadians());
            if (d != 0) {
                setTurnRadarRightRadians(d);
            }
        }
    }

    @Override
    public void onRobotDeath(RobotDeathEvent event) {
        tracks.remove(event.getName());
    }

    @Override
    public void onHitByBullet(HitByBulletEvent event) {
        direction*=-1;
    }

    @Override
    public void onHitWall(HitWallEvent event) {
        direction*=-1;
    }

    @Override
    public void onHitRobot(HitRobotEvent event) {
        direction*=-1;
    }

    public static double getAbsoluteBearing(Point2D from, Point2D to) {
        double dY = to.getY() - from.getY();
        double dX = to.getX() - from.getX();
        double angle;
        if (dX != 0) {
            angle = PI / 2 - Math.atan2(dY, dX);
        } else {
            angle = dY > 0 ? 0 : PI;
        }
        return normalAbsoluteAngle(angle);
    }

    public static double dx(double angle) {
        return sin(angle);
    }

    public static double dy(double angle) {
        return cos(angle);
    }

    @Override
    public void onPaint(Graphics2D g) {
        Map<Point2D, Integer> forcePoints = getForcePoints();
        for (Map.Entry<Point2D, Integer> fp : forcePoints.entrySet()) {
            Point2D point = fp.getKey();
            double[] vector = forceVector(point, fp.getValue());
            drawPointerLine(g, new Point2D.Double(point.getX() + vector[0] * 50, point.getY() + vector[1] * 50), point, Color.BLUE);
        }
    }

    public static void drawPointerLine(Graphics2D g, Point2D firingPoint, Point2D targetPos, Color color) {
        g.setColor(color);
        int fromX = (int) firingPoint.getX();
        int fromY = (int) firingPoint.getY();
        int toX = (int) targetPos.getX();
        int toY = (int) targetPos.getY();
        g.drawLine(fromX, fromY, toX, toY);
        g.fillOval(toX - 5, toY - 5, 10, 10);
    }

}
