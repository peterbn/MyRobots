package pbn;
import pbn.internals.*;
import robocode.*;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.lang.Math.*;
import static robocode.util.Utils.*;

// API help : http://robocode.sourceforge.net/docs/robocode/robocode/Robot.html

/**
 * Pwnator2000 - a robot by pbn
 */
public class Pwnator2000 extends AdvancedRobot
{

    private static final double MAX_GUN_TURN_RATE_RAD = (PI * 20) / 180;

    private static final int RADAR_TURN_RATE = 1000;
    private static final double GUN_AIM_TIME = ceil(PI / MAX_GUN_TURN_RATE_RAD);

    private Tracker tracker;
    private TargetingComputer targetingComputer;

    private volatile boolean aiming = false;

    private double gunCoolingRate;
    private Map<Bullet, ShootingSolution> trackedBullets = new HashMap<Bullet,ShootingSolution>();
    private Set<ShootingSolution> pendingSolutions = new HashSet<ShootingSolution>();



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
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForRobotTurn(true);
        gunCoolingRate = getGunCoolingRate();

        //set up infinite radar turn
        addCustomEvent(new RadarTurnCompleteCondition(this));
        setTurnRadarRightRadians(RADAR_TURN_RATE);
        out.println("Rardar online");
        out.println("Initialization done");

        // Robot main loop
        //noinspection InfiniteLoopStatement
        while (true) {
            aim();
            turnRightRadians(PI/8);

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
            removeCustomEvent(condition);
            FireGunCondition fireCondition = (FireGunCondition) condition;
            out.println("Firing gun at time " + getTime());
            Bullet bullet = setFireBullet(fireCondition.getPower());
            ShootingSolution solution = fireCondition.getShootingSolution();
            pendingSolutions.remove(solution);
            trackedBullets.put(bullet, solution);
            aiming = false;
        }
    }

    private void aim() {
        if (!tracker.hasEnemies()) return;
        out.println("setting target");
        Track currentTarget = tracker.getClosestRobotTrack();
        if (currentTarget != null && !aiming) {
            Recording target = currentTarget.top();
            out.println("aiming for " + target);
            double gunCoolingTime = ceil(getGunHeat() / gunCoolingRate);
            long currentTime = getTime();
            out.println("Gun cooled at " + (currentTime + gunCoolingTime));
            if (gunCoolingTime < GUN_AIM_TIME/2) {
                out.print("Gun is cold before turret has turned - aiming for robot");
                long shotTime = (long) (GUN_AIM_TIME + currentTime);
                out.println("Aiming to shoot at time " + shotTime + ", current time is " + currentTime);
                try {
                    Point2D.Double firingPoint = new Point2D.Double(getX(), getY());
                    ShootingSolution solution = targetingComputer.getShootingSolution(
                            currentTarget,
                            firingPoint,
                            shotTime);
                    double turn = normalRelativeAngle(solution.getAbsoluteShotHeading() - getGunHeadingRadians());
                    long readyTime = (long) max(ceil(turn / MAX_GUN_TURN_RATE_RAD) , ceil(gunCoolingTime)) + 1;
                    if (readyTime < GUN_AIM_TIME) {
                        out.println("Turn will only take " + readyTime + ", recomputing shot");
                        //aim a little closer to the mark
                        shotTime = currentTime + readyTime;
                        solution = targetingComputer.getShootingSolution(
                                currentTarget,
                                firingPoint,
                                shotTime
                        );
                        turn = normalRelativeAngle(solution.getAbsoluteShotHeading() - getGunHeadingRadians());
                    }
                    out.println("Shooting at: " + solution);
                    out.println("Current gun heading is: " + getGunHeading());
                    out.println("Current tank heading is: " + getHeading());

                    out.println("Turning gun: " + toDegrees(turn));
                    aiming = true;
                    pendingSolutions.add(solution);
                    synchronized (this) {
                        setTurnGunRightRadians(turn);
                        addCustomEvent(new FireGunCondition(this, shotTime, solution));
                    }
                } catch (NoSolutionException e) {
                    out.println("Unable to compute solution: " + e.getMessage());
                }
            }
        }
    }


    /**
	 * onScannedRobot: What to do when you see another robot
	 */
    @Override
	public void onScannedRobot(ScannedRobotEvent e) {
        tracker.update(e);
	}

    /**
	 * onHitByBullet: What to do when you're hit by a bullet
	 */
    @Override
	public void onHitByBullet(HitByBulletEvent e) {
		// Replace the next line with any behavior you would like
		// back(10);
	}

    @Override
    public void onBulletHit(BulletHitEvent event) {
        trackedBullets.remove(event.getBullet());
    }

    @Override
    public void onBulletHitBullet(BulletHitBulletEvent event) {
        trackedBullets.remove(event.getBullet());
    }

    @Override
    public void onBulletMissed(BulletMissedEvent event) {
        trackedBullets.remove(event.getBullet());
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
        for (ShootingSolution solution : pendingSolutions) {
            DebugGraphics.drawAimLine(g, solution.getShootingPosition(), solution.getTargetPosition());
        }
        for (ShootingSolution solution : trackedBullets.values()) {
            DebugGraphics.drawBulletLine(g, solution.getShootingPosition(), solution.getTargetPosition());
        }
    }

}


