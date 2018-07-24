package com.hoten.delaunay.voronoi.nodename.as3delaunay;

import com.hoten.delaunay.geom.GenUtils;
import com.hoten.delaunay.geom.Point;

final public class Vertex implements ICoord {

    /** Vertex at affinity. Used in edge reordering to find out wrong edge list for reordering. */
    final public static Vertex VERTEX_AT_INFINITY = new Vertex(Double.NaN, Double.NaN);

    /** Vertex index. */
    private int index;

    /** Vertex position. */
    private final Point position;

    /**
     * @param x Coordinate.
     * @param y Coordinate.
     */
    private Vertex(double x, double y) {
        position = new Point(x, y);
    }

    /** {@inheritDoc} */
    @Override public Point getPosition() {
        return position;
    }

    /**
     * @return index.
     */
    public int getIndex() {
        return index;
    }

    /**
     * @param idx New index.
     */
    public void setIndex(int idx) {
        index = idx;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return "Vertex (" + index + ") " + position.x + ", " + position.y;
    }

    /**
     * This is the only way to make a Vertex.
     *
     * @param halfedge0 Halfedge 0.
     * @param halfedge1 Halfedge 1.
     * @return Intersection vertex or {@code null} if there is no intersection between given halfedges.
     */
    public static Vertex intersect(Halfedge halfedge0, Halfedge halfedge1) {
        Edge edge0 = halfedge0.edge;
        Edge edge1 = halfedge1.edge;

        if (edge0 == null || edge1 == null)
            return null;

        if (edge0.getRightSite() == edge1.getRightSite())
            return null;

        double determinant = edge0.a * edge1.b - edge0.b * edge1.a;

        if (-1.0e-10 < determinant && determinant < 1.0e-10) {
            // the edges are parallel
            return null;
        }

        double intersectionX = (edge0.c * edge1.b - edge1.c * edge0.b) / determinant;
        double intersectionY = (edge1.c * edge0.a - edge0.c * edge1.a) / determinant;

        if (Double.isNaN(intersectionX) || Double.isNaN(intersectionY))
            return VERTEX_AT_INFINITY;

        Edge edge;
        Halfedge halfedge;

        if (GenUtils.compareByYThenX(edge0.getRightSite(), edge1.getRightSite()) < 0) {
            halfedge = halfedge0;
            edge = edge0;
        } else {
            halfedge = halfedge1;
            edge = edge1;
        }

        boolean rightOfSite = intersectionX >= edge.getRightSite().getX();

        if ((rightOfSite && halfedge.leftRight == LR.LEFT)
                || (!rightOfSite && halfedge.leftRight == LR.RIGHT)) {
            return null;
        }

        return new Vertex(intersectionX, intersectionY);
    }

    /**
     * @return X coordinate.
     */
    public double getX() {
        return position.x;
    }

    /**
     * @return Y coordinate.
     */
    public double getY() {
        return position.y;
    }
}
