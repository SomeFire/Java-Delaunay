package com.hoten.delaunay.voronoi;

import com.hoten.delaunay.geom.Point;

/**
 * Represents dual edge, where centers are centers of voronoi site and corners in delaunay triangulation.
 *
 * @author Connor
 */
public class Edge {

    /** Edge index in graph. */
    public int index;

    /** Delanay edge. */
    public Center d0, d1;

    /** Voronoi edge. */
    public Corner v0, v1;

    /** Halfway between v0, v1. */
    public Point midpoint;

    /** Is edge represents a river? */
    public int river;

    /**
     * @param v0 Corner 0.
     * @param v1 Corner 1.
     */
    public void setVornoi(Corner v0, Corner v1) {
        this.v0 = v0;
        this.v1 = v1;
        midpoint = new Point((v0.loc.x + v1.loc.x) / 2, (v0.loc.y + v1.loc.y) / 2);
    }
}
