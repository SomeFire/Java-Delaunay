package com.hoten.delaunay.voronoi.nodename.as3delaunay;

import com.hoten.delaunay.geom.Point;
import java.util.List;

public final class Polygon {

    /** Polygon vertices. */
    private List<Point> vertices;

    /**
     * @param vertices Vertices.
     */
    public Polygon(List<Point> vertices) {
        this.vertices = vertices;
    }

    /**
     * @return Polygon area.
     */
    public double area() {
        return Math.abs(signedDoubleArea() * 0.5);
    }

    /**
     * @return Polygon winding.
     */
    public Winding winding() {
        double signedDoubleArea = signedDoubleArea();

        if (signedDoubleArea < 0)
            return Winding.CLOCKWISE;

        if (signedDoubleArea > 0)
            return Winding.COUNTERCLOCKWISE;

        return Winding.NONE;
    }

    /**
     * @return Signed Double area.
     */
    private double signedDoubleArea() {
        double signedDoubleArea = 0;

        for (int index = 0; index < vertices.size(); ++index) {
            int nextIndex = (index + 1) % vertices.size();

            Point point = vertices.get(index);
            Point next = vertices.get(nextIndex);

            signedDoubleArea += point.x * next.y - next.x * point.y;
        }

        return signedDoubleArea;
    }
}