package com.hoten.delaunay.geom;

/**
 * Point.java
 *
 * @author Connor
 */
public class Point {

    /** Coordinate. */
    public double x, y;

    /**
     * @param x Coordinate.
     * @param y Coordinate.
     */
    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return (int)x + ", " + (int)y;
    }

    /**
     * @return Distance from (0, 0) to the point without square root.
     */
    public double l2() {
        return x * x + y * y;
    }

    /**
     * @return Distance from (0, 0) to the point.
     */
    public double length() {
        return Math.sqrt(x * x + y * y);
    }
}
