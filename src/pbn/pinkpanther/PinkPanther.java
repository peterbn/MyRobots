package pbn.pinkpanther;

import robocode.*;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.*;

import static java.lang.Math.*;
import static java.lang.Math.PI;
import static robocode.util.Utils.normalAbsoluteAngle;
import static robocode.util.Utils.normalRelativeAngle;

/**
 * An anti-gravity bot
 */
public class PinkPanther extends AdvancedRobot {

    private static final int BOT_WEIGHT = 4;
    private static final int BULLET_WEIGHT = 1;

    public static final double PI2 = PI / 2;

    private Map<String, Recording> tracks;
    private Set<Bullet> bullets;
    private int bfX, bfY, bfX2, bfY2;
    private String lookingFor;
    private String target;

    @Override
    public void run() {
        setColors(Color.PINK, Color.PINK, Color.PINK, Color.PINK, Color.PINK);
        tracks = new HashMap<String, Recording>(getOthers());
        bullets = new HashSet<Bullet>();
        bfX = (int) getBattleFieldWidth();
        bfY = (int) getBattleFieldHeight();
        bfX2 = bfX / 2;
        bfY2 = bfY / 2;
        setAdjustRadarForGunTurn(true);
        setAdjustRadarForRobotTurn(true);
        setAdjustGunForRobotTurn(true);

        //noinspection InfiniteLoopStatement
        do {
            if ( lookingFor == null) {
                setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
            } else {
                Recording recording = tracks.get(lookingFor);
                if (recording == null || recording.time < getTime() - 3) {
                    lookingFor = null;
                }
            }
            navigate();
            gun();
            execute();
        } while (true);
    }

    private void gun() {
        //take aim
        setTarget();
        if (target != null) {
            setDebugProperty("Current target", target);
            Recording targetRecord = tracks.get(target);
            if (targetRecord == null) {
                target = null;
                return;
            }
            int gunCoolTime = (int) (getGunHeat() / getGunCoolingRate());
            try {
                double power = min(1.5, getEnergy());
                Point2D targetPos = getTargetPos(getTime() + gunCoolTime + 1, power, targetRecord);
                if (targetPos.distance(currentPosition()) < 300 && getEnergy() > 3) {
                    power = 3;
                    targetPos = getTargetPos(getTime() + gunCoolTime + 1, power, targetRecord);
                }
                double gunTurn = normalRelativeAngle(getAbsoluteBearing(currentPosition(), targetPos) - getGunHeadingRadians());
                if (gunCoolTime < 0.1 && abs(Math.toDegrees(gunTurn)) < 2) {
                    setFire(power);
                    target = null;
                } else {
                    setTurnGunRightRadians(gunTurn);
                }
            } catch (IndexOutOfBoundsException ignored) {
            }
        }
    }

    private void navigate() {
        Map<Point2D, Integer> forcePoints = getForcePoints();
        double[] totalVector = getTotalVector(forcePoints);
        Point2D destination = new Point2D.Double(getX() + totalVector[0], getY() + totalVector[1]);

        double targetBearing = getAbsoluteBearing(currentPosition(), destination);
        double turn = normalRelativeAngle(targetBearing - getHeadingRadians());
        double ahead = currentPosition().distance(destination) / 2;
        if (abs(turn) > PI2) {
            ahead *= -1;
            turn -= PI2;
        }
        setAhead(ahead);
        setTurnRightRadians(turn);
    }

    private double[] getTotalVector(Map<Point2D, Integer> forcePoints) {
        double[] totalVector = new double[]{0, 0};
        for (Map.Entry<Point2D, Integer> forcePoint : forcePoints.entrySet()) {
            double[] vector = forceVector(forcePoint.getKey(), forcePoint.getValue());
            totalVector[0] += vector[0];
            totalVector[1] += vector[1];
        }
        return totalVector;
    }

    private Map<Point2D, Integer> getForcePoints() {
        Point2D pos = currentPosition();
        Map<Point2D, Integer> points = new HashMap<Point2D, Integer>();
        int fixedPointValue = max(BOT_WEIGHT, getOthers() / 2 + bullets.size() / 4);
        points.put(new Point2D.Double(bfX2, bfY2), 2);
        points.put(new Point2D.Double(bfX2 - bfX2 / 2, bfY2), 1);
        points.put(new Point2D.Double(bfX2 + bfX2 / 2, bfY2), 1);
        points.put(new Point2D.Double(bfX2, bfY2 - bfY2 / 2), 1);
        points.put(new Point2D.Double(bfX2, bfY2 + bfY2 / 2), 1);
        points.put(new Point2D.Double(pos.getX(), pos.getY() < bfY2 ? 0: bfY), fixedPointValue); //walls
        points.put(new Point2D.Double(pos.getX() < bfX2 ? 0 : bfX, pos.getY()), fixedPointValue);
        for (Recording bot : tracks.values()) {
            points.put(bot.advance(getTime()+1), BOT_WEIGHT); //bots
        }
        for (Iterator<Bullet> iterator = bullets.iterator(); iterator.hasNext(); ) {
            Bullet bullet = iterator.next();
            Point2D p = advanceBullet(bullet);
            if (outsideBF(p)) {
                iterator.remove();
            } else {
                points.put(p, BULLET_WEIGHT);
            }
        }
        return points;
    }

    private boolean outsideBF(Point2D p) {
        return p.getX() < 0 || p.getX() > bfX || p.getY() < 0 || p.getY() > bfY;
    }


    private double[] forceVector(Point2D point, int weight) {
        double F = (10000 * (weight)) / point.distance(currentPosition());
        double bearing = getAbsoluteBearing(point, currentPosition());
        return new double[]{dx(bearing) * F, dy(bearing) * F};
    }

    private Point2D currentPosition() {
        return new Point2D.Double(getX(), getY());
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent event) {
        Recording previous = tracks.get(event.getName());
        Recording record = new Recording(this, event, previous);
        tracks.put(record.name, record);
        try {
            double energyDrop = previous.energy - record.energy;
            if (energyDrop > 0 && energyDrop <= 3) { //assume that everyone shoots at me!
                bullets.add(new Bullet(previous.time + 1, Rules.getBulletSpeed(energyDrop), getAbsoluteBearing(record.position, currentPosition()), record.position));
            }
        } catch (NullPointerException ignored) {}
        //oldest-seen radar - degenerates to a constant-lock (almost) radar in 1v1
        if (tracks.size() == getOthers() && (lookingFor == null || record.name.equals(lookingFor))) {
            Recording oldest = record;
            for (Recording recording : tracks.values()) {
                if (oldest.time > recording.time) {
                    oldest = recording;
                }
            }
            lookingFor = oldest.name;
            double d = signum(normalRelativeAngle(getAbsoluteBearing(currentPosition(), oldest.position) - getRadarHeadingRadians()));
            setTurnRadarRightRadians(d * Double.POSITIVE_INFINITY);
        }
    }

    private void setTarget() {
        if (this.target == null) {
            Recording newTarget = null;
            double distance = Double.POSITIVE_INFINITY;
            for (Recording recording : tracks.values()) {
                Point2D advance = recording.advance(getTime() + 5);
                double dp = advance.distance(currentPosition());
                if (dp < distance) {
                    distance = dp;
                    newTarget = recording;
                }
            }
            if (newTarget != null) {
                target = newTarget.name;
            }
        }
    }

    private Point2D getTargetPos(long firingTime, double power, Recording target) throws IndexOutOfBoundsException {
        Point2D targetPos = target.advance(firingTime);
        double remainingDistance = targetPos.distance(currentPosition());
        int dt = 0;
        while (remainingDistance > (getWidth() / 2) ) {
            dt++; //one step closer
            Point2D advance = target.advance(firingTime + dt);
            if (outsideBF(advance)) {
                break;
            }
            targetPos = advance;
            double bulletDistance = Rules.getBulletSpeed(power) * dt;
            remainingDistance = targetPos.distance(currentPosition()) - bulletDistance;
            if (bulletDistance > bfX + bfY) {
                throw new IndexOutOfBoundsException("Iteration exceeded limits");
            }
        }
        return targetPos;
    }

    @Override
    public void onRobotDeath(RobotDeathEvent event) {
        tracks.remove(event.getName());
        if (event.getName().equals(lookingFor)) {
            lookingFor = null;
        }
        if (event.getName().equals(target)) {
            target = null;
        }
    }

    public static double getAbsoluteBearing(Point2D from, Point2D to) {
        double dY = to.getY() - from.getY();
        double dX = to.getX() - from.getX();
        double angle;
        if (dX != 0) {
            angle = PI2 - Math.atan2(dY, dX);
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
    private Point2D advanceBullet(Bullet bullet) {
        double x = bullet.position.getX() + (getTime() - bullet.fireTime) * dx(bullet.heading) * bullet.speed;
        double y = bullet.position.getY() + (getTime() - bullet.fireTime) * dy(bullet.heading) * bullet.speed;
        return new Point2D.Double(x, y);
    }

    @Override
    public void onPaint(Graphics2D g) {
        Map<Point2D, Integer> forcePoints = getForcePoints();
        for (Map.Entry<Point2D, Integer> fp : forcePoints.entrySet()) {
            Point2D point = fp.getKey();
            double[] vector = forceVector(point, fp.getValue());
            drawPointerLine(g, new Point2D.Double(point.getX() + vector[0] , point.getY() + vector[1] ), point, Color.BLUE);
        }
        double[] totalVector = getTotalVector(forcePoints);
        Point2D destination = new Point2D.Double(getX() + totalVector[0], getY() + totalVector[1]);
        drawPointerLine(g,destination,currentPosition(),Color.PINK);
        g.setColor(new Color(0x80FF0000, true));
        for (Bullet bullet : bullets) {
            Point2D bulletPos = advanceBullet(bullet);

            g.fillOval((int) bulletPos.getX() - 5, (int) bulletPos.getY() - 5, 10, 10);

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
