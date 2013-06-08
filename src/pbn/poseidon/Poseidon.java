package pbn.poseidon;

import robocode.*;
import robocode.annotation.SafeStatic;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;

import static java.lang.Math.*;
import static robocode.util.Utils.normalAbsoluteAngle;
import static robocode.util.Utils.normalRelativeAngle;

/**
 * An anti-gravity bot with wave-based (hence, Poseidon) guess-factor targeting
 */
public class Poseidon extends AdvancedRobot {

    private static final int BOT_WEIGHT = 4;
    private static final int BULLET_WEIGHT = 2;

    public static final double PI2 = PI / 2;

    static final double[] powers = {.5, 1, 1.5, 3};

    @SafeStatic
    static Map<String, int[][][]> guessFactors = new HashMap<String, int[][][]>();
    static int opponents;

    static Map<String, Recording> tracks;
    static Set<Bullet> bullets;
    static List<Wave> waves;
    static int bfX, bfY, bfX2, bfY2;
    private String lookingFor;
    private String target;

    @Override
    public void run() {
        setColors(Color.PINK, Color.PINK, Color.PINK, Color.PINK, Color.PINK);
        tracks = new HashMap<String, Recording>(getOthers());
        bullets = new HashSet<Bullet>();
        waves = new LinkedList<Wave>();
        opponents = max(0, getOthers());
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

    private int power = 2;
    private void gun() {
        //get optimal solution
        double bearing = getGunHeadingRadians();
        double bestDistSq = Double.POSITIVE_INFINITY;
        try {
            Recording target = null;
            if (getGunHeat() > 0.7 || this.target == null) {
                target = new TreeSet<Recording>(tracks.values()).iterator().next();
                this.target = target.name;
            }
        /*for (Recording enemy : tracks.values()) {
            for (int shotPower = 0; shotPower < powers.length; shotPower++) {
                double firingAngle = firingAngle(enemy.name, shotPower);
                double curDistSq = 0;
                for (String other : tracks.keySet()) {
                    curDistSq += pow(firingAngle - firingAngle(other, shotPower), 2);
                }
                if (curDistSq < bestDistSq) {
                    power = shotPower;
                    bearing = firingAngle;
                }
            }
        }*/
            bearing = firingAngle(this.target, power);
            if (abs(bearing - getGunHeadingRadians())  > toRadians(2) ) {
                setTurnGunRightRadians(normalRelativeAngle(bearing - getGunHeadingRadians()));
            } else if (abs(getGunTurnRemaining()) < 2 && getGunHeat() == 0) {
                setFire(powers[power]);
            }
        } catch (Exception ignored) {
        }
    }

    private double firingAngle(String target, int powerIndex) {
        Recording recording = tracks.get(target);
        Point2D pos = recording.advance(getTime());
        int distance = (int) (pos.distance(currentPosition()) / 100);
        double offset = 0;
        if (guessFactors.containsKey(target)) {
            int[] factors = guessFactors.get(target)[((int) recording.velocity)][powerIndex];
            int bestAngle = factors.length / 2 + 1;
            for (int i = 0; i < factors.length; i++) {
                if (factors[bestAngle] < factors[i]) {
                    bestAngle = i;
                }
            }
            double guessFactor = (double)(bestAngle - (factors.length - 1) / 2) / ((factors.length - 1) / 2);
            offset = recording.direction * guessFactor * asin(8 / Rules.getBulletSpeed(powers[powerIndex]));
        }
        double bearing = getAbsoluteBearing(currentPosition(), pos);
        return normalAbsoluteAngle(bearing + offset);
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
            double d = normalRelativeAngle(getAbsoluteBearing(currentPosition(), oldest.position) - getRadarHeadingRadians());
            setTurnRadarRightRadians(d * Double.POSITIVE_INFINITY);
        }
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

    @Override
    public void onStatus(StatusEvent e) {
        try {
            for (Recording record : tracks.values()) {
                int[][][] factors = guessFactors.get(record.name);
                if (factors == null) {
                    guessFactors.put(record.name, (factors = new int[9][powers.length][31]));
                }
                for (int power = 0; power < powers.length; power++) {
                    int[] data = factors[((int) abs(record.velocity))][power];
                    waves.add(new Wave(currentPosition(), getAbsoluteBearing(currentPosition(), record.position), Rules.getBulletSpeed(powers[power]), getTime(), record.name, record.direction, data));
                }
            }
            for (Iterator<Wave> iterator = waves.iterator(); iterator.hasNext(); ) {
                Wave wave = iterator.next();
                boolean b = wave.checkBreak(tracks, getTime());
                if (b) {
                    iterator.remove();
                }
            }
        } catch (NullPointerException ignored) {
        }
    }

    public static double getAbsoluteBearing(Point2D from, Point2D to) {
        double dY = to.getY() - from.getY();
        double dX = to.getX() - from.getX();
        double angle;
        if (dY != 0) {
            //noinspection SuspiciousNameCombination
            angle = atan2(dX, dY);
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
            renderBullets = false,
            renderForcePoints = false,
            renderNavVector = false,
            renderRadarInfo = false,
            renderSelfOverlay = false,
            renderCurrentTarget = false,
            renderWaves = false;

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
            case 'v':
                renderWaves = !renderWaves;
                break;
        }
    }

    private void printCharMap(Graphics2D g) {
        FontMetrics fontMetrics = g.getFontMetrics();
        int height = fontMetrics.getHeight();
        int x = 25;
        int y = 25;
        Color color;
        color = renderWaves ? Color.GREEN : Color.RED;
        g.setColor(color);
        g.drawString("v: Render Waves", x, y);

        y += 2 * height;
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
            int opportunityRadius = 500;
            g.setColor(new Color(0x2000ff00, true));
            int x = (int) getX();
            int y = (int) getY();
            g.fillOval(x-opportunityRadius, y - opportunityRadius, opportunityRadius*2, opportunityRadius* 2);
            int gunTurn = (int) (20 * getGunHeat() / getGunCoolingRate());
            g.fillArc(x - opportunityRadius, y - opportunityRadius, opportunityRadius * 2, opportunityRadius * 2, (int)getGunHeading() - gunTurn, 2 * gunTurn);
            renderBotOverlay(g, x, y, Color.BLUE);
        }
        if (renderRadarInfo) {
            for (Recording recording : tracks.values()) {
                Color color = new Color(0x8000ff00 - (int) (getTime() - recording.time) * 0x10000000, true);
                int x = (int) recording.position.getX();
                int y = (int) recording.position.getY();
                g.setColor(color);
                g.fillRect(x - 20, y - 20, 40, 40);
                Point2D advance = recording.advance(getTime());
                g.setColor(Color.GREEN);
                g.drawLine(x,y, (int) advance.getX(),(int)advance.getY());
            }
        }
        Map<Point2D, Integer> forcePoints = getForcePoints();
        for (Map.Entry<Point2D, Integer> fp : forcePoints.entrySet()) {
            Point2D point = fp.getKey();
            double[] vector = forceVector(point, fp.getValue());
            if (renderForcePoints) {
                renderForcePoints(g, point, vector);
            }
        }
        double[] totalVector = getTotalVector(forcePoints);
        Point2D destination = new Point2D.Double(getX() + totalVector[0], getY() + totalVector[1]);
        if (renderNavVector) {
            Point2D targetPos = currentPosition();
            g.setColor(Color.PINK);
            int fromX = (int) destination.getX();
            int fromY = (int) destination.getY();
            int toX = (int) targetPos.getX();
            int toY = (int) targetPos.getY();
            g.drawLine(fromX, fromY, toX, toY);
        }
        if (renderBullets) {
            renderBullets(g);
        }
        if (renderCurrentTarget) {
            renderCurrentTarget(g);
        }
        if (renderWaves) {
            renderWaves(g);
        }
    }

    private void renderWaves(Graphics2D g) {
        g.setStroke(new BasicStroke(2));
        g.setColor(new Color(0x80FF8000, true));
        for (Wave wave : waves) {
            int y = (int) wave.origin.getY();
            int x = (int) wave.origin.getX();
            int escapeAngle = (int) toDegrees(asin(8 / wave.velocity));
            int heading = (int) toDegrees(wave.heading);
            int radius = (int) (wave.velocity * (getTime() - wave.fireTime));
            g.drawArc(x - radius, y - radius, radius * 2, radius * 2, heading - escapeAngle, escapeAngle * 2);
            g.fillOval(x - 2, y - 2, 4, 4);
        }
    }

    private void renderForcePoints(Graphics2D g, Point2D point, double[] vector) {
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
            boolean locked = !(getGunHeat() > 0.7);
            Color color = locked ? Color.RED : Color.YELLOW;

            int targetRadius = renderBotOverlay(g, x, y, color);

            g.setStroke(new BasicStroke(1));
            int botX = (int) getX();
            int botY = (int) getY();
            double absoluteBearing = getAbsoluteBearing(position, currentPosition());
            int rectX = (int) (x + sin(absoluteBearing) * targetRadius);
            int rectY = (int) (y + cos(absoluteBearing) * targetRadius);
            g.drawLine(botX, botY, rectX, rectY);
            double firingAngle = firingAngle(target, power);
            g.setColor(Color.RED);

            g.drawLine(botX,botY, (int) (botX + 200 * sin(firingAngle)), (int) (botY + 200*cos(firingAngle)));
        } catch (Exception ignored) {
        }
    }

    private int renderBotOverlay(Graphics2D g, int x, int y, Color color) {
        g.setColor(new Color(0x40000000 + color.getRGB(), true));
        int targetRadius = 20;
        g.fillOval(x - targetRadius, y - targetRadius, 40, 40);
        g.setColor(color);
        g.fillOval(x - 2, y - 2, 4, 4);
        g.setStroke(new BasicStroke(3));
        g.drawOval(x - targetRadius, y - targetRadius, 40, 40);
        return targetRadius;
    }

}
