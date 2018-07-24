package com.hoten.delaunay.geom;

import com.hoten.delaunay.voronoi.nodename.as3delaunay.Edge;
import com.hoten.delaunay.voronoi.nodename.as3delaunay.LineSegment;
import com.hoten.delaunay.voronoi.nodename.as3delaunay.Site;

/**
 * GenUtil.java
 *
 * @author Connor
 */
public class GenUtils {

    /**
     * Checks that given values are close.
     *
     * @param d1 Value 1.
     * @param d2 Value 2.
     * @param diff Inaccuracy.
     * @return {@code True} if values are close, {@code false} otherwise.
     */
    public static boolean closeEnough(double d1, double d2, double diff) {
        return Math.abs(d1 - d2) <= diff;
    }

    /**
     * Checks that given points are close.
     *
     * @param p1 Point 1.
     * @param p2 Point 2.
     * @param diff Inaccuracy.
     * @return {@code True} if points are close, {@code false} otherwise.
     */
    public static boolean closeEnough(Point p1, Point p2, double diff) {
        return Math.abs(distance(p1, p2)) <= diff;
    }

    /**
     * @param p1 Point 1.
     * @param p2 Point 2.
     * @return Distance between given points.
     */
    public static double distance(Point p1, Point p2) {
        return Math.sqrt((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y));
    }

    /**
     * Compares distance between joins for the given edges.
     *
     * @param edge0 Edge 0.
     * @param edge1 Edge 1.
     * @return 0 if distances are equal.
     * A value less than 0 if distance between joins of edge 0 are shorter than distance between joins of edge 1.
     * And a value greater than 0 if distance between joins of edge 0 are greater.
     */
    public static int compareSitesDistances_MAX(Edge edge0, Edge edge1) {
        double length0 = edge0.sitesDistance();
        double length1 = edge1.sitesDistance();

        return Double.compare(length1, length0);
    }

    /**
     * Compares distance between joins for the given edges.
     *
     * @param edge0 Edge 0.
     * @param edge1 Edge 1.
     * @return 0 if distances are equal.
     * A value greater than 0 if distance between joins of edge 0 are shorter than distance between joins of edge 1.
     * And a value less than 0 if distance between joins of edge 0 are greater.
     */
    public static int compareSitesDistances(Edge edge0, Edge edge1) {
        return -compareSitesDistances_MAX(edge0, edge1);
    }

    public static double compareLengths_MAX(LineSegment segment0, LineSegment segment1) {
        double length0 = GenUtils.distance(segment0.p0, segment0.p1);
        double length1 = GenUtils.distance(segment1.p0, segment1.p1);

        return Double.compare(length1, length0);
    }

    public static double compareLengths(LineSegment edge0, LineSegment edge1) {
        return -compareLengths_MAX(edge0, edge1);
    }

    /**
     * @param s1 Site 1.
     * @param s2 Site 2.
     * @return 0 if sites are equal.
     * A value greater than 0 site 1 if coordinates are greater than second coordinates.
     * And a value less than 0 site 1 if coordinates are less than second coordinates.
     */
    public static int compareByYThenX(Site s1, Site s2) {
        if (s1.getY() < s2.getY())
            return -1;

        if (s1.getY() > s2.getY())
            return 1;

        return Double.compare(s1.getX(), s2.getX());
    }

    /**
     * @param s Site.
     * @param p Point.
     * @return 0 if sites are equal.
     * A value greater than 0 if site coordinates are greater than point coordinates.
     * And a value less than 0 if site coordinates are less than point coordinates.
     */
    public static int compareByYThenX(Site s, Point p) {
        if (s.getY() < p.y)
            return -1;

        if (s.getY() > p.y)
            return 1;

        return Double.compare(s.getX(), p.x);
    }
}
