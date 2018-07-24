package com.hoten.delaunay.voronoi.nodename.as3delaunay;

import com.hoten.delaunay.geom.GenUtils;
import com.hoten.delaunay.geom.Point;
import com.hoten.delaunay.geom.Rectangle;
import java.util.HashMap;
import java.util.Map;

/**
 * The line segment connecting the two Sites is part of the Delaunay
 * triangulation; the line segment connecting the two Vertices is part of the
 * Voronoi diagram
 *
 * @author ashaw
 *
 */
public final class Edge {

    /** Represents deleted edge in halfedges. Used in edge reordering to find out wrong edge list for reordering. */
    public final static Edge DELETED = new Edge(-1);

    /** Edge index. */
    private int index;

    /** The equation of the edge: ax + by = c. */
    public double a, b, c;

    /**
     *  Once {@link #clipVertices(Rectangle)} is called, this Map will hold two Points
     *  representing the clipped coordinates of the left and right ends...
     */
    private Map<LR, Point> clippedVertices;

    /**
     * One of two Voronoi vertices that the edge connects
     * (if one of them is null, the edge extends to infinity).
     */
    private Vertex leftVertex, rightVertex;

    /**
     * The two input Sites for which this Edge is a bisector. So called joins of this edge.
     * It can be {@link LR#LEFT} or {@link LR#RIGHT}.
     */
    private HashMap<LR, Site> sites;

    /**
     * This is the only way to create a new Edge.
     *
     * @param site0 Site 0.
     * @param site1 Site 1.
     * @param idx Edge index.
     * @return Border edge between given sites.
     *
     */
    public static Edge createBisectingEdge(Site site0, Site site1, int idx) {
        double a, b, c;

        double dx = site1.getX() - site0.getX();
        double dy = site1.getY() - site0.getY();

        c = site0.getX() * dx + site0.getY() * dy + (dx * dx + dy * dy) * 0.5;

        if (Math.abs(dx) > Math.abs(dy)) {
            a = 1.0;
            b = dy / dx;
            c /= dx;
        } else {
            b = 1.0;
            a = dx / dy;
            c /= dy;
        }

        Edge edge = new Edge(idx);

        edge.setLeftSite(site0);
        edge.setRightSite(site1);
        site0.addEdge(edge);
        site1.addEdge(edge);

        edge.leftVertex = null;
        edge.rightVertex = null;

        edge.a = a;
        edge.b = b;
        edge.c = c;
        //trace("createBisectingEdge: a ", edge.a, "b", edge.b, "c", edge.c);

        return edge;
    }

    public LineSegment delaunayLine() {
        // draw a line connecting the input Sites for which the edge is a bisector:
        return new LineSegment(getLeftSite().getPosition(), getRightSite().getPosition());
    }

    public LineSegment voronoiEdge() {
        if (!isVisible())
            return new LineSegment(null, null);

        return new LineSegment(clippedVertices.get(LR.LEFT), clippedVertices.get(LR.RIGHT));
    }

    /**
     * @return Left end of edge.
     */
    public Vertex getLeftVertex() {
        return leftVertex;
    }

    /**
     * @return Right end of edge.
     */
    public Vertex getRightVertex() {
        return rightVertex;
    }

    /**
     * @param leftRight Vertex orientation relative to themselves.
     * @return One of the end vertices.
     */
    public Vertex vertex(LR leftRight) {
        return (leftRight == LR.LEFT) ? leftVertex : rightVertex;
    }

    /**
     * @param leftRight Vertex position relative to this edge.
     * @param v Vertex.
     */
    public void setVertex(LR leftRight, Vertex v) {
        if (leftRight == LR.LEFT) {
            leftVertex = v;
        } else {
            rightVertex = v;
        }
    }

    /**
     * @return {@code True} if this edge have no at least one of ends.
     */
    public boolean isPartOfConvexHull() {
        return (leftVertex == null || rightVertex == null);
    }

    /**
     * @return Distance between joins of this edge.
     */
    public double sitesDistance() {
        return GenUtils.distance(getLeftSite().getPosition(), getRightSite().getPosition());
    }

    /**
     * @return Vertices clipped in boundaries.
     */
    public Map<LR, Point> getClippedEnds() {
        return clippedVertices;
    }

    /**
     * Unless the entire Edge is outside the graph bounds. In that case visible will be false:
     *
     * @return {@code True} if edge is inside graph bounds. Otherwise - {@code false}.
     */
    public boolean isVisible() {
        return clippedVertices != null;
    }

    /**
     * Set left site for this edge.
     *
     * @param s Site.
     */
    public void setLeftSite(Site s) {
        sites.put(LR.LEFT, s);
    }

    /**
     * @return Left site for this edge.
     */
    public Site getLeftSite() {
        return sites.get(LR.LEFT);
    }

    /**
     * Set right site for this edge.
     *
     * @param s Site.
     */
    public void setRightSite(Site s) {
        sites.put(LR.RIGHT, s);
    }

    /**
     * @return Right site for this edge.
     */
    public Site getRightSite() {
        return sites.get(LR.RIGHT);
    }

    /**
     * @param leftRight Site orientation relative to the edge.
     * @return Site for given orientation.
     */
    public Site site(LR leftRight) {
        return sites.get(leftRight);
    }

    /**
     * @param idx Edge index.
     */
    private Edge(int idx) {
        sites = new HashMap<>();
        index = idx;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return "Edge " + index + "; sites " + sites.get(LR.LEFT) + ", " + sites.get(LR.RIGHT)
            + "; endVertices " + (leftVertex != null ? leftVertex.getIndex() : "null") + ", "
            + (rightVertex != null ? rightVertex.getIndex() : "null") + "::";
    }

    /**
     * Set clippedVertices to contain the two ends of the portion of the
     * Voronoi edge that is visible within the graph bounds. If no part of the Edge
     * falls within the bounds, leave clippedVertices null.
     *
     * @param bounds Graph bounds.
     */
    public void clipVertices(Rectangle bounds) {
        double xmin = bounds.x;
        double ymin = bounds.y;
        double xmax = bounds.right;
        double ymax = bounds.bottom;

        Vertex vertex0, vertex1;
        double x0, x1, y0, y1;

        if (a == 1.0 && b >= 0.0) {
            vertex0 = rightVertex;
            vertex1 = leftVertex;
        } else {
            vertex0 = leftVertex;
            vertex1 = rightVertex;
        }

        if (a == 1.0) {
            y0 = ymin;
            if (vertex0 != null && vertex0.getY() > ymin) {
                y0 = vertex0.getY();
            }
            if (y0 > ymax) {
                return;
            }
            x0 = c - b * y0;

            y1 = ymax;
            if (vertex1 != null && vertex1.getY() < ymax) {
                y1 = vertex1.getY();
            }
            if (y1 < ymin) {
                return;
            }
            x1 = c - b * y1;

            if ((x0 > xmax && x1 > xmax) || (x0 < xmin && x1 < xmin)) {
                return;
            }

            if (x0 > xmax) {
                x0 = xmax;
                y0 = (c - x0) / b;
            } else if (x0 < xmin) {
                x0 = xmin;
                y0 = (c - x0) / b;
            }

            if (x1 > xmax) {
                x1 = xmax;
                y1 = (c - x1) / b;
            } else if (x1 < xmin) {
                x1 = xmin;
                y1 = (c - x1) / b;
            }
        } else {
            x0 = xmin;
            if (vertex0 != null && vertex0.getX() > xmin) {
                x0 = vertex0.getX();
            }
            if (x0 > xmax) {
                return;
            }
            y0 = c - a * x0;

            x1 = xmax;
            if (vertex1 != null && vertex1.getX() < xmax) {
                x1 = vertex1.getX();
            }
            if (x1 < xmin) {
                return;
            }
            y1 = c - a * x1;

            if ((y0 > ymax && y1 > ymax) || (y0 < ymin && y1 < ymin)) {
                return;
            }

            if (y0 > ymax) {
                y0 = ymax;
                x0 = (c - y0) / a;
            } else if (y0 < ymin) {
                y0 = ymin;
                x0 = (c - y0) / a;
            }

            if (y1 > ymax) {
                y1 = ymax;
                x1 = (c - y1) / a;
            } else if (y1 < ymin) {
                y1 = ymin;
                x1 = (c - y1) / a;
            }
        }

        clippedVertices = new HashMap<>();

        if (vertex0 == leftVertex) {
            clippedVertices.put(LR.LEFT, new Point(x0, y0));
            clippedVertices.put(LR.RIGHT, new Point(x1, y1));
        } else {
            clippedVertices.put(LR.RIGHT, new Point(x0, y0));
            clippedVertices.put(LR.LEFT, new Point(x1, y1));
        }
    }
}