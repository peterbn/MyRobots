package pbn.internals;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.Iterator;

import static java.lang.Math.sin;
import static java.lang.StrictMath.cos;

/**
 * Static methods to draw debugging (and targeting) graphics
 */
public abstract class DebugGraphics {


    private static final Color RED_TRANSPARENT = new Color(0xff, 0x00, 0x00, 0x80);
    private static final Color WHITE_TRANSPARENT = new Color(0xFF, 0xFF, 0xFF, 0x80);

    public static void drawTrackOverlay(Graphics2D g, Track track, long time) {
        Iterator<Recording> recordings = track.snapshot().iterator();
        Recording recording = recordings.next();
        Point2D advance = recording.advance(time);
        int x;
        int y;
        int xOrig = (int) recording.position.getX();
        int yOrig = (int) recording.position.getY();
        int step = 0;
        while (recordings.hasNext()) {
            recording = recordings.next();
            int dx = (int) (sin(recording.headingRadians) * recording.velocity);
            int dy = (int) (cos(recording.headingRadians) * recording.velocity);
            x = (int) recording.position.getX();
            y = (int) recording.position.getY();
            g.setColor(Color.yellow);
            g.drawLine(xOrig, yOrig, x + dx, y + dy);
            xOrig = (int) recording.position.getX();
            yOrig = (int) recording.position.getY();
        }
        g.setColor(RED_TRANSPARENT);
        x = (int) advance.getX();
        y = (int) advance.getY();
        drawRobotBox(g, x, y, RED_TRANSPARENT);
        g.setColor(WHITE_TRANSPARENT);
        g.drawString(recording.name, x - 20, y - 20);

    }

    private static void drawRobotBox(Graphics2D g, int x, int y, Color color) {
        g.setColor(color);
        g.fillRect(x - 20, y - 20, 40, 40);
    }

    public static void drawAimLine(Graphics2D graphics, Point2D firingPoint, Point2D targetPos) {
        drawPointerLine(graphics, firingPoint, targetPos, Color.YELLOW);
    }

    public static void drawBulletLine(Graphics2D g, Point2D firingPoint, Point2D targetPoint) {
        drawPointerLine(g, firingPoint, targetPoint, Color.RED);
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
