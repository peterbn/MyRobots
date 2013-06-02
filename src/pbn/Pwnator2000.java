package pbn;
import pbn.internals.*;
import robocode.*;
import robocode.util.Utils;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.*;

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
    private DrivingComputer drivingComputer;

    private volatile boolean aiming = false;

    private double gunCoolingRate;
    private Map<Bullet, ShootingSolution> trackedBullets = Collections.synchronizedMap(new HashMap<Bullet,ShootingSolution>());
    private Set<ShootingSolution> pendingSolutions = Collections.synchronizedSet(new HashSet<ShootingSolution>());



	/**
	 * run: Pwnator2000's default behavior
	 */
    @Override
	public void run() {
        // Initialization of the robot should be put here
        out.println("Pimpin' bot!");
        double width = getWidth();
        double height = getHeight();
        setColors(new Color(10, 10, 10, 20), Color.RED, new Color(200, 200, 200, 0), Color.ORANGE, new Color(255, 0, 255, 128));

        out.println("Started robot");
        this.tracker = new Tracker(this);
        out.println("Tracking subsystem online!");
        this.targetingComputer = new TargetingComputer(this);
        out.println("Targeting subsystem online!");
        this.drivingComputer = new DrivingComputer(this);
        out.println("Driving subsystem online!");

        setAdjustRadarForGunTurn(true);
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForRobotTurn(true);
        gunCoolingRate = getGunCoolingRate();

        //set up infinite radar turn
        out.println("Bringing Radar online");
        addCustomEvent(new RadarTurnCompleteCondition(this));
        setTurnRadarRightRadians(RADAR_TURN_RATE);
        out.println("Rardar online");
        addCustomEvent(new AimReadyCondition());
        out.println("Aim subsystem started");

        out.println("Initialization done");

        // Robot main loop
        //noinspection InfiniteLoopStatement
        while (true) {
            drivingComputer.iterate();
        }
    }

    @Override
    public void onCustomEvent(CustomEvent event) {
        super.onCustomEvent(event);
        //switch on event
        Condition condition = event.getCondition();
        //PERSISTENT EVENTS - remember to return after dispatch
        if (condition instanceof RadarTurnCompleteCondition) {
            setTurnRadarRightRadians(RADAR_TURN_RATE);
            return;
        }
        if (condition instanceof AimReadyCondition) {
            aim();
            return;
        }

        //TEMPORARY EVENTS
        if (condition instanceof FireGunCondition) {
            FireGunCondition fireCondition = (FireGunCondition) condition;
            onFireGunEvent(fireCondition);
        }

        //remove temporary event
        removeCustomEvent(condition);
    }

    private void onFireGunEvent(FireGunCondition fireCondition) {
        out.println("Firing gun at time " + getTime());
        ShootingSolution solution = fireCondition.getShootingSolution();
        if ( solution.getShootingPosition().distance(getX(), getY()) <= 10
                && solution.getFiringTime() >= getTime() - 1) {
            Bullet bullet = setFireBullet(solution.getBulletPower());
            trackedBullets.put(bullet, solution);
        }
        pendingSolutions.remove(solution);
        aiming = false;
    }

    private void aim() {
        out.println("setting target");
        Track currentTarget = tracker.getClosestRobotTrack();
        if (currentTarget != null ) {
            Recording target = currentTarget.top();
            out.println("aiming for " + target);
            long currentTime = getTime();
            double gunCoolingTime = ceil(getGunHeat() / gunCoolingRate);
            long shotTime = (long) (GUN_AIM_TIME + currentTime);
            try {
                Point2D firingPoint = drivingComputer.getFiringPosition((long) GUN_AIM_TIME);
                ShootingSolution solution = targetingComputer.getShootingSolution(
                        currentTarget,
                        firingPoint,
                        shotTime);
                double turnRateRadians = Rules.getTurnRateRadians(getVelocity());
                out.println("Tank turn rate is: " + toDegrees(turnRateRadians));
                double turn = normalRelativeAngle(solution.getAbsoluteShotHeading() - getGunHeadingRadians());

                double turnTime = ceil(abs(turn) / (MAX_GUN_TURN_RATE_RAD - turnRateRadians));
                out.println("Turn time is: " + turnTime);
                out.println("Gun cooing time is: " + gunCoolingTime);
                long readyTime = (long) max(1, max(turnTime, ceil(gunCoolingTime)));
                if (readyTime < GUN_AIM_TIME) {
                    out.println("Turn will only take " + readyTime + ", recomputing shot");
                    //aim a little closer to the mark
                    shotTime = currentTime + readyTime;
                    firingPoint = drivingComputer.getFiringPosition(readyTime);
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
        drivingComputer.onHitWall(e);
	}

    @Override
    public void onHitRobot(HitRobotEvent event) {
        drivingComputer.onHitRobot(event);
    }

    @Override
    public void onRobotDeath(RobotDeathEvent event) {
        tracker.update(event);
    }

    public void onPaint(Graphics2D g) {
        drivingComputer.paint(g);
        tracker.paint(g);
        for (ShootingSolution solution : pendingSolutions) {
            DebugGraphics.drawAimLine(g, solution.getShootingPosition(), solution.getTargetPosition());
        }
        for (ShootingSolution solution : trackedBullets.values()) {
            DebugGraphics.drawBulletLine(g, solution.getShootingPosition(), solution.getTargetPosition());
        }
    }

    /**
     * Condition that checks if we're ready to aim for a new bot
     */
    public class AimReadyCondition extends Condition {

        public AimReadyCondition() {
            super("AimReady", 90);
        }

        @Override
        public boolean test() {
            boolean hasEnemies = tracker.hasEnemies();
            double gunCoolingTime = ceil(getGunHeat() / gunCoolingRate);
            boolean gunIsCoolEnough = gunCoolingTime < GUN_AIM_TIME / 2;
            boolean hasNavData = drivingComputer.hasNavData();

            boolean b = hasEnemies && !aiming && gunIsCoolEnough &&  hasNavData;
            if (b) {
                Recording top = tracker.getClosestRobotTrack().top();
                return top.time > getTime() - 2 && top.distance(Pwnator2000.this) < 600;
            }
            return false;
        }
    }
}


