package pbn;
import pbn.internals.*;
import robocode.*;

import java.awt.*;
import java.awt.geom.Point2D;

import static java.lang.Math.*;
import static robocode.util.Utils.*;

// API help : http://robocode.sourceforge.net/docs/robocode/robocode/Robot.html

/**
 * Pwnator2000 - a robot by pbn
 */
public class Pwnator2000 extends AdvancedRobot
{

    private static final double MAX_GUN_TURN_RATE_DEG = 20;

    private static final double BULLET_POWER = 2;

    private static final int RADAR_TURN_RATE = 1000;// MAX_RADAR_TURN_RATE;// - MAX_GUN_TURN_RATE; //max turn rate without interruption
    private static final double GUN_AIM_TIME = ceil(180 / MAX_GUN_TURN_RATE_DEG);

    private Tracker tracker;
    private TargetingComputer targetingComputer;

    private boolean aiming = false;

    private double gunCoolingRate;



	/**
	 * run: Pwnator2000's default behavior
	 */
    @Override
	public void run() {
        // Initialization of the robot should be put here
        out.println("Started robot");
        out.println("Estimated time to aim gun: " + GUN_AIM_TIME);
        this.tracker = new Tracker(this);
        out.println("Tracker online!");
        this.targetingComputer = new TargetingComputer(this);
        out.println("Targeting computer online!");

        setAdjustRadarForGunTurn(true);
        gunCoolingRate = getGunCoolingRate();

        //set up infinite radar turn
        addCustomEvent(new RadarTurnCompleteCondition(this));
        setTurnRadarRightRadians(RADAR_TURN_RATE);
        out.println("Rardar online");
        out.println("Initialization done");

        // Robot main loop
        //noinspection InfiniteLoopStatement
        while (true) {
            ahead(10);
            aim();
            back(10);

            //huntTank();
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
        if (condition instanceof FireGunCondition) {
            FireGunCondition fireCondition = (FireGunCondition) condition;
            out.println("Firing gun at time " + getTime());
            fire(fireCondition.getPower());
            removeCustomEvent(condition);
            aiming = false;
        }
    }

    private void aim() {
        if (!tracker.hasEnemies()) return;
        out.println("setting target");
        Track currentTarget = tracker.getClosestRobotTrack();

        if (currentTarget != null && !aiming) {
            Recording target = currentTarget.top();
            if (target != null) {
                out.println("aiming for " + target);
                double gunCoolingTime = getGunHeat() / gunCoolingRate;
                long currentTime = getTime();
                out.println("Gun cooled at " + (currentTime + gunCoolingTime));
                if (gunCoolingTime < GUN_AIM_TIME) {
                    out.print("Gun is cold before turret has turned - aiming for robot");
                    long shotTime = (long) (GUN_AIM_TIME + currentTime);
                    out.println("Aiming to shoot at time " + shotTime + ", current time is " + currentTime);
                    try {
                        double absoluteShotBearing = targetingComputer.getAbsoluteShotBearing(
                                currentTarget,
                                new Point2D.Double(getX(), getY()),
                                shotTime,
                                BULLET_POWER);
                        out.println("Absolute shot bearing is: " + toDegrees(absoluteShotBearing));
                        out.println("Current gun heading is: " + getGunHeading());
                        out.println("Current tank heading is: " + getHeading());
                        double turn = normalRelativeAngle(absoluteShotBearing - getGunHeadingRadians());
                        out.println("Turning gun: " + toDegrees(turn));
                        setTurnGunRightRadians(turn);
                        addCustomEvent(new FireGunCondition(this, shotTime, BULLET_POWER));
                        aiming = true;
                    } catch (NoSolutionException e) {
                        out.println("Unable to compute solution");
                    }
                }
            }
        }
    }


    /**
	 * onScannedRobot: What to do when you see another robot
	 */
    @Override
	public void onScannedRobot(ScannedRobotEvent e) {
        setDebugProperty("lastScannedRobot", e.getName() + " at " + e.getBearing() + " degrees at time " + getTime());
        tracker.update(e);
        setDebugProperty("Track", tracker.toString());

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
        tracker.update(event);
    }

    public void onPaint(Graphics2D g) {
        tracker.paint(g);
    }

}


