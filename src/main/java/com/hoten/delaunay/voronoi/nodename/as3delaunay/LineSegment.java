package com.hoten.delaunay.voronoi.nodename.as3delaunay;

import com.hoten.delaunay.geom.GenUtils;
import com.hoten.delaunay.geom.Point;

public final class LineSegment {

    /** Segment ends. */
    public final Point p0, p1;

    /**
     * @param p0 Point.
     * @param p1 Point.
     */
    LineSegment(Point p0, Point p1) {
        this.p0 = p0;
        this.p1 = p1;
    }
}