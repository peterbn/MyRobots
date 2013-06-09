package pbn.pinkpanther;

import robocode.*;

import java.awt.*;
import java.awt.event.KeyEvent;
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
    private static final int BULLET_WEIGHT = 2;

    public static final double PI2 = PI / 2;

    static Map<String, Recording> tracks;
    static Set<Bullet> bullets;
    static int bfX, bfY, bfX2, bfY2;
    private String lookingFor;
    volatile String target;

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
        Recording.robot = this;

        setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
        //noinspection InfiniteLoopStatement
        do {
            navigate();
            gun();
            execute();
        } while (true);
    }

    private void gun() {
        //take aim
        Recording newTarget;
        Set<Recording> list = new TreeSet<Recording>(tracks.values());
        int gunCoolTime = (int) (getGunHeat() / getGunCoolingRate());

        long firingTime = getTime() + gunCoolTime + 1;
        try {
            newTarget = list.iterator().next();
            if (newTarget != null) {
                if (this.target == null || getGunHeat() > 0.7) {
                    target = newTarget.name;
                }
                if (!newTarget.equals(target)) {
                    double power;
                    Point2D targetPos;
                    Recording targetRecord = tracks.get(newTarget);

                    power = computePower(targetRecord);
                    targetPos = getTargetPos(firingTime, power, targetRecord);
                    double gunTurn = normalRelativeAngle(getAbsoluteBearing(currentPosition(), targetPos) - getGunHeadingRadians());
                    if (abs(gunTurn) / toRadians(20) < gunCoolTime -1) {
                        target = newTarget.name;
                    }
                }
            }
        } catch (Exception ignored1) {
        }
        if (target != null) {
            setDebugProperty("Current target", target);
            Recording targetRecord = tracks.get(target);
            if (targetRecord == null) {
                target = null;
                return;
            }
            try {
                double power;
                Point2D targetPos;

                //power to kill + a bit
                power = computePower(targetRecord);
                targetPos = getTargetPos(firingTime, power, targetRecord);
                double gunTurn = normalRelativeAngle(getAbsoluteBearing(currentPosition(), targetPos) - getGunHeadingRadians());

                if (gunCoolTime == 0 && getGunTurnRemaining() < 2) {
                    setFire(power);
                } else if (gunCoolTime > 1) { //lock target when we have a single turn left.
                    setTurnGunRightRadians(gunTurn);
                }
            } catch (IndexOutOfBoundsException ignored) {
            }
        }
    }

    private double computePower(Recording targetRecord) {
        return min((3. *  targetRecord.score()) / 100, min(min(3, getEnergy()), 0.1 + (targetRecord.energy > 4 ? (targetRecord.energy + 2) / 6 : targetRecord.energy / 4)));
    }

    private void navigate() {
        Map<Point2D, Integer> forcePoints = getForcePoints();
        double[] totalVector = getTotalVector(forcePoints);
        Point2D destination = new Point2D.Double(getX() + totalVector[0], getY() + totalVector[1]);

        double targetBearing = getAbsoluteBearing(currentPosition(), destination);
        double turn = normalRelativeAngle(targetBearing - getHeadingRadians());
        double ahead = 16;
        if (abs(turn) > PI2) {
            ahead *= -1;
            turn -= PI2;
        }
        setAhead(ahead);
        if (abs(ahead) > getWidth() / 4) {
            setTurnRightRadians(turn);
        }
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
        Map<Point2D, Integer> points = new HashMap<Point2D, Integer>();
        int fixedPointValue = max(2, getOthers() / 2 + bullets.size() / 8);

        points.put(new Point2D.Double(bfX2, bfY2), BOT_WEIGHT); //center
        points.put(new Point2D.Double(getX(), getY() < bfY2 ? 0 : bfY), fixedPointValue); //walls
        points.put(new Point2D.Double(getX() < bfX2 ? 0 : bfX, getY()), fixedPointValue);
        for (Recording bot : tracks.values()) {
            points.put(bot.advance(getTime() + 1), 2 + (int) bot.energy / 10); //bots
        }
        for (Iterator<Bullet> iterator = bullets.iterator(); iterator.hasNext();) {
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
        double F = (weight) / pow(point.distance(currentPosition()), 2);
        double bearing = getAbsoluteBearing(point, currentPosition());
        return new double[]{sin(bearing) * F, cos(bearing) * F};
    }

    public Point2D currentPosition() {
        return new Point2D.Double(getX(), getY());
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent event) {
        Recording previous = tracks.get(event.getName());
        Recording record = new Recording(event, previous);
        tracks.put(record.name, record);
        try {
            double energyDrop = previous.energy - record.energy;
            if (energyDrop > 0 && energyDrop <= 3) { //assume that everyone shoots at me! A Lot!
                bullets.add(new Bullet(previous.time, Rules.getBulletSpeed(energyDrop), getAbsoluteBearing(record.position, currentPosition()), record.position));
            }
        } catch (NullPointerException ignored) {
        }
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

    private Point2D getTargetPos(long firingTime, double power, Recording target) throws IndexOutOfBoundsException {
        Point2D targetPos = target.advance(firingTime);
        double remainingDistance = targetPos.distance(currentPosition());
        int dt = 0;
        while (remainingDistance > (getWidth() / 2)) {
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
        if (dY != 0) {
            //noinspection SuspiciousNameCombination
            angle = Math.atan2(dX, dY);
        } else {
            angle = dX > 0 ? PI2 : 3 * PI2;
        }
        return normalAbsoluteAngle(angle);
    }

    private Point2D advanceBullet(Bullet bullet) {
        double x = bullet.position.getX() + (getTime() - bullet.fireTime) * sin(bullet.heading) * bullet.speed;
        double y = bullet.position.getY() + (getTime() - bullet.fireTime) * cos(bullet.heading) * bullet.speed;
        return new Point2D.Double(x, y);
    }

    static boolean
            renderBullets       = false,
            renderForcePoints   = false,
            renderNavVector     = false,
            renderRadarInfo     = false,
            renderSelfOverlay   = false,
            renderCurrentTarget = false;

    @Override
    public void onKeyTyped(KeyEvent e) {
        switch (e.getKeyChar()) {
            case 'b':
                renderBullets = !renderBullets;
                break;
            case 'f':
                renderForcePoints = !renderForcePoints;
                break;
            case 'n':
                renderNavVector = !renderNavVector;
                break;
            case 'r':
                renderRadarInfo = !renderRadarInfo;
                break;
            case 's':
                renderSelfOverlay = !renderSelfOverlay;
                break;
            case 't':
                renderCurrentTarget = !renderCurrentTarget;
                break;
        }
    }

    private void printCharMap(Graphics2D g) {
        FontMetrics fontMetrics = g.getFontMetrics();
        int height = fontMetrics.getHeight();
        int x = 25;
        int y = 25;
        Color color;
        color = renderCurrentTarget ? Color.GREEN : Color.RED;
        g.setColor(color);
        g.drawString("t: Render Current target", x, y);

        y += 2 * height;
        color = renderSelfOverlay ? Color.GREEN : Color.RED;
        g.setColor(color);
        g.drawString("s: Render Self overlay", x, y);

        y += 2 * height;
        color = renderRadarInfo ? Color.GREEN : Color.RED;
        g.setColor(color);
        g.drawString("r: Render Radar information", x, y);

        y += 2 * height;
        color = renderNavVector ? Color.GREEN : Color.RED;
        g.setColor(color);
        g.drawString("n: Render Navigation vector", x, y);

        y += 2 * height;
        color = renderForcePoints ? Color.GREEN : Color.RED;
        g.setColor(color);
        g.drawString("f: Render Force points", x, y);

        y += 2 * height;
        color = renderBullets ? Color.GREEN : Color.RED;
        g.setColor(color);
        g.drawString("b: Render Bullets", x, y);
    }

    @Override
    public void onPaint(Graphics2D g) {
        printCharMap(g);
        if (renderSelfOverlay) {
            renderSelfOverlay(g);
        }
        if (renderRadarInfo) {
            renderRadarInfo(g);
        }
        if (renderForcePoints) {
            renderForcePoints(g);
        }
        if (renderNavVector) {
            renderNavVector(g);
        }
        if (renderBullets) {
            renderBullets(g);
        }
        if (renderCurrentTarget) {
            renderCurrentTarget(g);
        }
    }

    private void renderSelfOverlay(Graphics2D g) {
        int opportunityRadius = 200;
        g.setColor(new Color(0x1000ff00, true));
        int x = (int) getX();
        int y = (int) getY();
        g.fillOval(x-opportunityRadius, y - opportunityRadius, opportunityRadius*2, opportunityRadius* 2);
        int gunTurn = (int) (20 * getGunHeat() / getGunCoolingRate());
        g.fillArc(x - opportunityRadius, y - opportunityRadius, opportunityRadius * 2, opportunityRadius * 2, (int)getGunHeading() - gunTurn, 2 * gunTurn);
        renderBotOverlay(g, x, y, Color.BLUE);
    }

    private void renderNavVector(Graphics2D g) {
        double[] totalVector = getTotalVector(getForcePoints());
        Point2D destination = new Point2D.Double(getX() + totalVector[0], getY() + totalVector[1]);
        Point2D targetPos = currentPosition();
        g.setColor(Color.PINK);
        int fromX = (int) destination.getX();
        int fromY = (int) destination.getY();
        int toX = (int) targetPos.getX();
        int toY = (int) targetPos.getY();
        g.drawLine(fromX, fromY, toX, toY);
    }

    private void renderForcePoints(Graphics2D g) {
        Map<Point2D, Integer> forcePoints = getForcePoints();
        for (Map.Entry<Point2D, Integer> fp : forcePoints.entrySet()) {
            Point2D point = fp.getKey();
            double[] vector = forceVector(point, fp.getValue());
            Point2D firingPoint = new Point2D.Double(point.getX() + vector[0], point.getY() + vector[1]);
            int fromX = (int) firingPoint.getX();
            int fromY = (int) firingPoint.getY();
            int toX = (int) point.getX();
            int toY = (int) point.getY();
            g.setColor(new Color(0x80ff00ff, true));
            g.drawLine(fromX, fromY, toX, toY);
            g.setColor(new Color(0x800000ff, true));
            g.fillOval(toX - 5, toY - 5, 10, 10);
        }
    }

    private void renderRadarInfo(Graphics2D g) {
        for (Recording recording : tracks.values()) {
            Point2D advance = recording.advance(getTime());
            int x = (int) advance.getX();
            int y = (int) advance.getY();
            g.setColor(new Color(0x10ffffff, true));
            int proximity = 200;
            g.fillOval(x - proximity, y - proximity, proximity*2, proximity*2);
            Color color = getRadarColor(recording.time, recording.score());
            g.setColor(color);
            g.fillRect(x - 20, y - 20, 40, 40);
            g.setColor(Color.BLUE);
            g.drawString(String.valueOf(recording.score()), x - 20, y - 20);
        }
    }

    private Color getRadarColor(long time, int score) {
        int red = score >= 50 ? 0xff : (int)(0xff*(score*2.)/100);
        int green = score <= 50 ? 0xff : (int)(0xff * (100. - score) / 100);
        return new Color(red, green, 0x00, 0x70 );
    }

    private void renderBullets(Graphics2D g) {
        g.setColor(new Color(0x80FF0000, true));
        for (Bullet bullet : bullets) {
            Point2D bulletPos = advanceBullet(bullet);
            g.fillOval((int) bulletPos.getX() - 5, (int) bulletPos.getY() - 5, 10, 10);
        }
    }

    private void renderCurrentTarget(Graphics2D g) {
        try {
            Recording currentTarget = tracks.get(target);
            Point2D position = currentTarget.advance(getTime());
            int x = (int) position.getX();
            int y = (int) position.getY();
            boolean locked = !(getGunHeat() > 0.1);
            Color color = locked ? Color.RED : Color.YELLOW;

            int targetRadius = renderBotOverlay(g, x, y, color);

            g.setStroke(new BasicStroke(1));
            int botX = (int) getX();
            int botY = (int) getY();
            double absoluteBearing = getAbsoluteBearing(position, currentPosition());
            int rectX = (int) (x + sin(absoluteBearing) * targetRadius);
            int rectY = (int) (y + cos(absoluteBearing) * targetRadius);
            g.drawLine(botX, botY, rectX, rectY);
        } catch (Exception ignored) {
        }
    }

    private int renderBotOverlay(Graphics2D g, int x, int y, Color color) {
        g.setColor(new Color(0x40000000 + color.getRGB(), true));
        int targetRadius = 30;
        g.fillOval(x - targetRadius, y - targetRadius, targetRadius*2, targetRadius*2);
        g.setColor(color);
        g.fillOval(x - 2, y - 2, 4, 4);
        g.setStroke(new BasicStroke(3));
        g.drawOval(x - targetRadius, y - targetRadius, targetRadius*2, targetRadius*2);
        return targetRadius;
    }

}
