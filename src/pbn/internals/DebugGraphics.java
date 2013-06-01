package pbn.internals;

import java.awt.*;
import java.awt.geom.Point2D;

/**
 * Static methods to draw debugging (and targeting) graphics
 */
public abstract class DebugGraphics {


    private static final Color RED_TRANSPARENT = new Color(0xff, 0x00, 0x00, 0x80);
    private static final Color YELLOW_TRANSPARENT = new Color(0xFF, 0xff, 0x00, 0x80);
    private static final Color WHITE_TRANSPARENT = new Color(0xFF, 0xFF, 0xFF, 0x80);

    public static void drawBigCircle(Graphics2D g, double x, double y, Color color) {
        g.setColor(color);
        g.drawOval((int) (x - 55), (int) (y - 55), 110, 110);
        g.drawOval((int) (x - 56), (int) (y - 56), 112, 112);
        g.drawOval((int) (x - 59), (int) (y - 59), 118, 118);
        g.drawOval((int) (x - 60), (int) (y - 60), 120, 120);
    }

    public static void drawTrackOverlay(Graphics2D g, Track track, long time) {
        Recording top = track.top();
        Point2D position = top.advance(time);
        g.setColor(RED_TRANSPARENT);
        int xOrig = (int) top.position.getX();
        int yOrig = (int) top.position.getY();
        int x = (int) position.getX();
        int y = (int) position.getY();
        int dx = (int) top.getDX();
        int dy = (int) top.getDY();
        drawRobotBox(g,xOrig, yOrig, YELLOW_TRANSPARENT);
        drawRobotBox(g, x, y, RED_TRANSPARENT);
        g.setColor(Color.yellow);
        g.drawLine(xOrig, yOrig, x + dx*8, y + dy*8);
        g.setColor(WHITE_TRANSPARENT);
        g.drawString(top.name, x - 20, y - 20);

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

    private static void drawPointerLine(Graphics2D g, Point2D firingPoint, Point2D targetPos, Color color) {
        g.setColor(color);
        int fromX = (int) firingPoint.getX();
        int fromY = (int) firingPoint.getY();
        int toX = (int) targetPos.getX();
        int toY = (int) targetPos.getY();
        g.drawLine(fromX, fromY, toX, toY);
        g.fillOval(toX - 5, toY - 5, 10, 10);
    }
}
