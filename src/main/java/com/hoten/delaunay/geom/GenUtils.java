package com.hoten.delaunay.geom;

/**
 * GenUtil.java
 *
 * @author Connor
 */
public class GenUtils {

    public static boolean closeEnough(double d1, double d2, double diff) {
        return Math.abs(d1 - d2) <= diff;
    }

    public static boolean closeEnough(Point p1, Point p2, double diff) {
        return Math.abs(distance(p1, p2)) <= diff;
    }

    public static double distance(Point p1, Point p2) {
        return Math.sqrt((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y));
    }
}
