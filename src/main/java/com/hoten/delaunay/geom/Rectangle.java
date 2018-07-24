package com.hoten.delaunay.geom;

/**
 * Rectangle.java
 *
 * @author Connor
 */
public class Rectangle {

    /** Coordinates. */
    public final double x, y;

    /** Size. */
    public final double width, height;

    /** Bounds. */
    public final double right, bottom, left, top;

    /** */
    public Rectangle(double x, double y, double width, double height) {
        left = this.x = x;
        top = this.y = y;
        this.width = width;
        this.height = height;
        right = x + width;
        bottom = y + height;
    }

    /**
     * @param p Point to check.
     * @return {@code True} if given point lies on the edge of the rectangle.
     */
    public boolean liesOnAxes(Point p) {
        return GenUtils.closeEnough(p.x, x, 1) || GenUtils.closeEnough(p.y, y, 1) ||
            GenUtils.closeEnough(p.x, right, 1) || GenUtils.closeEnough(p.y, bottom, 1);
    }

    /**
     * @param p Point to check.
     * @return {@code True} if given point is lies inside of the rectangle.
     */
    public boolean inBounds(Point p) {
        return inBounds(p.x, p.y);
    }

    /**
     * @param x0 X coordinate of the point to check.
     * @param y0 Y coordinate of the point to check.
     * @return {@code True} if given point is lies inside of the rectangle.
     */
    public boolean inBounds(double x0, double y0) {
        return !(x0 < x || x0 > right || y0 < y || y0 > bottom);
    }

    @Override public String toString() {
        return "[x="+x+", y="+", w="+width+", h="+height+']';
    }
}
