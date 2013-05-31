package pbn;
import robocode.*;
import robocode.Robot;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.*;
import static robocode.util.Utils.*;

// API help : http://robocode.sourceforge.net/docs/robocode/robocode/Robot.html

/**
 * Pwnator2000 - a robot by pbn
 */
public class Pwnator2000 extends AdvancedRobot
{

    private static final int MAX_RADAR_TURN_RATE = 45;
    private static final int MAX_GUN_TURN_RATE = 20;

    private static final double BULLET_POWER = 2;

    private static final int RADAR_TURN_RATE = 1000;// MAX_RADAR_TURN_RATE;// - MAX_GUN_TURN_RATE; //max turn rate without interruption

    private final Map<String, Track<Recording>> tracker = new HashMap<String, Track<Recording>>();

    private Track<Recording> currentTarget;

    private boolean hasLock = false;
    private boolean aiming = false;
    private long fireTime;

    private double gunCoolingRate;



	/**
	 * run: Pwnator2000's default behavior
	 */
    @Override
	public void run() {
		// Initialization of the robot should be put here

        setAdjustRadarForGunTurn(true);
        gunCoolingRate = getGunCoolingRate();

        //set up infinite radar turn
        addCustomEvent(new RadarTurnCompleteCondition(this));
        setTurnRadarRightRadians(RADAR_TURN_RATE);
        out.println("Initialization done");

        // Robot main loop
        //noinspection InfiniteLoopStatement
        while(true) {
            aim();
            waitFor(new GunTurnCompleteCondition(this));
            fireGun();
            huntTank();
        }
	}

    @Override
    public void onCustomEvent(CustomEvent event) {
        super.onCustomEvent(event);
        //switch on event
        Condition condition = event.getCondition();
        if (condition instanceof RadarTurnCompleteCondition) {
            setTurnRadarRightRadians(RADAR_TURN_RATE);
        }
    }

    private void aim() {
        if (tracker.isEmpty()) return;
        if (!aiming) {
            setClosestAsTarget();
            boolean hasTarget = currentTarget != null;

            if (hasTarget) {
                Recording target = currentTarget.top();
                if (target != null) {
                    out.println("aiming for " + target);
                    double gunHeading = getGunHeadingRadians();
                    double distance = target.distance(this);
                    if (getGunHeat() > 0) {
                        aiming = false;
                        return;
                    }
                    if (distance > 500) {
                        aiming = false;
                        hasLock = false;
                        return;
                    }
                    aiming = true;
                    hasLock = true;
                    long bulletTravelTime = (long)getBulletTravelTime(distance, BULLET_POWER);
                    double currentBearing = target.currentBearing(this, bulletTravelTime);


                    Point2D advance = target.advance(this.getTime() + bulletTravelTime);
                    drawCircle(advance.getX(), advance.getY(), Color.orange);

                    double approxTurn = normalRelativeAngle(currentBearing - gunHeading);
                    long gunTurnTime = (long) ceil(approxTurn / MAX_GUN_TURN_RATE);
                    double turnBearing = target.currentBearing(this, bulletTravelTime + gunTurnTime);

                    double turn = normalRelativeAngle(turnBearing - gunHeading);

                    setTurnGunRightRadians(turn);
                }
            }
        }
    }

    private void setClosestAsTarget() {
        out.println("setting target");
        Track<Recording> closest = null;
        for (Track<Recording> recording : tracker.values()) {
            if (closest == null || recording.top().distance(this) < closest.top().distance(this)) {
                closest = recording;
            }
        }
        currentTarget = closest;
    }

    private void fireGun() {
        if (aiming) {
            fire(BULLET_POWER);
            aiming = false;
        }
    }

    private void huntTank() {
        waitFor(new MoveCompleteCondition(this));
        waitFor(new TurnCompleteCondition(this));
        if (currentTarget != null) {
            Recording target = currentTarget.top();
            double distance = target.distance(this);
            long travelTime = (long) (distance / 8);
            double targetHeading = target.currentBearing(this, travelTime);
            setTurnRightRadians(normalRelativeAngle(targetHeading - getHeadingRadians()));
            setAhead(distance * 2 / 3);

        }
    }

    private void drawCircle(double x, double y, Color color) {
        Graphics2D g = getGraphics();

        g.setColor(color);
        g.drawOval((int) (x - 55), (int) (y - 55), 110, 110);
        g.drawOval((int) (x - 56), (int) (y - 56), 112, 112);
        g.drawOval((int) (x - 59), (int) (y - 59), 118, 118);
        g.drawOval((int) (x - 60), (int) (y - 60), 120, 120);
    }


    /**
	 * onScannedRobot: What to do when you see another robot
	 */
    @Override
	public void onScannedRobot(ScannedRobotEvent e) {
        setDebugProperty("lastScannedRobot", e.getName() + " at " + e.getBearing() + " degrees at time " + getTime());
        track(e);
        setDebugProperty("Track", tracker.toString());

	}

    private void track(ScannedRobotEvent e) {
        Track<Recording> track = tracker.get(e.getName());
        if (track == null) {
            track = new Track<Recording>();
            tracker.put(e.getName(), track);
        }

        Recording target = new Recording(this, e);
        track.add(target);

        drawCircle(target.position.getX(), target.position.getY(), Color.green);
    }

	/**
	 * onHitByBullet: What to do when you're hit by a bullet
	 */
    @Override
	public void onHitByBullet(HitByBulletEvent e) {
		// Replace the next line with any behavior you would like
		// back(10);
	}
	
	/**
	 * onHitWall: What to do when you hit a wall
	 */
    @Override
	public void onHitWall(HitWallEvent e) {
        double hitLocation = e.getBearing();
        if (hitLocation > 0) {
            turnLeft(180 - hitLocation);
        } else {
            turnRight(180 + hitLocation);
        }
		ahead(120);
	}

    @Override
    public void onRobotDeath(RobotDeathEvent event) {
        super.onRobotDeath(event);
        tracker.remove(event.getName());
    }

    public static double getBulletTravelTime(double distanceToEnemy, double bulletPower) {
         return Math.ceil(distanceToEnemy / (20 - (3 * bulletPower)));
    }

    public void onPaint(Graphics2D g) {
//		g.setColor(Color.red);
//		g.drawOval((int) (getX() - 50), (int) (getY() - 50), 100, 100);
//		g.setColor(new Color(0, 0xFF, 0, 30));
//		g.fillOval((int) (getX() - 60), (int) (getY() - 60), 120, 120);
	}


}

class Track<T> {
    private  Deque<T> elements;
    private static final int limit = 10;

    public Track() {
        elements = new ArrayDeque<T>(limit);
    }

    public synchronized void add(T element) {
        if (elements.size() >= limit) {
            elements.pollLast();
        }
        elements.push(element);
    }

    public T top() {
        return elements.peekFirst();
    }

    @Override
    public String toString() {
        return "Track{" +
                "elements=" + elements +
                '}';
    }
}


class Recording {
    public final long time;
    public final Point2D position;
    public final double
            velocity,
            headingRadians;
    public final String name;

    public Recording(Robot me, ScannedRobotEvent event) {
        this.name = event.getName();
        double distance = event.getDistance();
        double bearingRadians = normalAbsoluteAngle(toRadians(me.getHeading()) + event.getBearingRadians());
        headingRadians = event.getHeadingRadians();
        double x = (sin(bearingRadians)) * distance + me.getX() ;
        double y = cos(bearingRadians) * distance + me.getY() ;

        position = new Point2D.Double(x, y);
        time = event.getTime();
        velocity = event.getVelocity();
    }

    public double distance(Robot me) {
        return position.distance(me.getX(), me.getY());
    }

    public double currentBearing(Robot me, long dt) {
        Point2D currentPosition = advance(me.getTime() + dt);
        double myX = me.getX();
        double myY = me.getY();

        double targetY = currentPosition.getY();
        double targetX = currentPosition.getX();
        if (targetX != myX) {
            return normalAbsoluteAngle(PI/2 - Math.atan2(targetY - myY, targetX - myX));
        } else {
            return targetX > myX ? 90 : 270;
        }
    }

    public Point2D advance(long time) {
        long dt = time - this.time;
        double dx = -cos(headingRadians + PI/2) * velocity * dt;
        double dy = sin(headingRadians + PI/2) * velocity * dt;
        return new Point2D.Double(position.getX() + dx, position.getY() + dy);
    }

    @Override
    public String toString() {
        return "Recording{" +
                "time=" + time +
                ", position=" + position +
                ", velocity=" + velocity +
                ", headingDegrees=" + toDegrees(headingRadians) +
                ", name='" + name + '\'' +
                '}';
    }
}
