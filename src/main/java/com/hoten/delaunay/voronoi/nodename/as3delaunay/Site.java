package com.hoten.delaunay.voronoi.nodename.as3delaunay;

import com.hoten.delaunay.geom.GenUtils;
import com.hoten.delaunay.geom.Point;
import com.hoten.delaunay.geom.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Site implements ICoord {

    /** Inaccuracy of measurements. */
    private static final double EPSILON = .005;

    /** Site location. **/
    private final Point position;

    /** Currently have no use and created by "Math.random". Possible way for height algorithms. **/
    private double weight;

    /** Site index. */
    private int index;

    /** The edges that define this Site's Voronoi region. */
    private List<Edge> edges;

    /** Which end of each edge hooks up with the previous edge in edges. */
    private List<LR> edgeOrientations;

    /** Ordered list of points that define site border clipped to bounds. */
    private List<Point> region;

    /**
     * Sort sites on y, then x coord, also change each site's index to
     * match its new position in the list so the index can be used to
     * identify the site for nearest-neighbor queries.
     *
     * haha "also" - means more than one responsibility...
     *
     */
    static void sortSites(List<Site> sites) {
        sites.sort((s1, s2) -> {
            int returnValue = GenUtils.compareByYThenX(s1, s2);

            // swap index values if necessary to match new ordering:
            int tempIndex;

            if (returnValue == -1) {
                if (s1.index > s2.index) {
                    tempIndex = s1.index;
                    s1.index = s2.index;
                    s2.index = tempIndex;
                }
            }
            else if (returnValue == 1) {
                if (s2.index > s1.index) {
                    tempIndex = s2.index;
                    s2.index = s1.index;
                    s1.index = tempIndex;
                }

            }

            return returnValue;
        });
    }

    /** {@inheritDoc} */
    @Override public Point getPosition() {
        return position;
    }

    /**
     * @param p Point.
     * @param index Site index.
     * @param weight Site weight.
     */
    Site(Point p, int index, double weight) {
        position = p;
        this.index = index;
        this.weight = weight;
        edges = new ArrayList<>();
        region = null;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return "Site " + index + ": " + getPosition();
    }

    /**
     * @param edge Additional border segment.
     */
    void addEdge(Edge edge) {
        edges.add(edge);
    }

    /**
     * @return Nearest border edge.
     */
    public Edge nearestEdge() {
        edges.sort(GenUtils::compareSitesDistances);

        return edges.get(0);
    }

    /**
     * @return Neighbors.
     */
    List<Site> neighborSites() {
        if (edges == null || edges.isEmpty())
            return new ArrayList<>();

        if (edgeOrientations == null)
            reorderEdges();

        ArrayList<Site> list = new ArrayList<>();

        for (Edge edge : edges)
            list.add(neighborSite(edge));

        return list;
    }

    /**
     * @param edge Border edge.
     * @return Neighbor site by given edge. Return {@code null} if no neighbor exist or edge isn't border for this site.
     */
    private Site neighborSite(Edge edge) {
        if (this == edge.getLeftSite())
            return edge.getRightSite();

        if (this == edge.getRightSite())
            return edge.getLeftSite();

        return null;
    }

    /**
     * @param clippingBounds Graph bounds.
     * @return Continuous line represented as a sequence of points clipped in graph bounds.
     */
    List<Point> region(Rectangle clippingBounds) {
        if (edges == null || edges.isEmpty())
            return new ArrayList<>();

        if (edgeOrientations == null || edgeOrientations.isEmpty()) {
            reorderEdges();

            region = clipToBounds(clippingBounds);

            if ((new Polygon(region)).winding() == Winding.CLOCKWISE)
                Collections.reverse(region);
        }

        return region;
    }

    /**
     * Sort border edges. After sorting they will represent continuous closed line.
     */
    private void reorderEdges() {
        EdgeReorderer reorderer = new EdgeReorderer(edges, Vertex.class);

        edges = reorderer.getEdges();
        edgeOrientations = reorderer.getEdgeOrientations();
    }

    /**
     * Converts site edge border to sequence points (edge ends positions), clipped to given boundaries.
     *
     * @param bounds Graph bounds.
     * @return Clipped site border represented as a sequence of points.
     */
    private ArrayList<Point> clipToBounds(Rectangle bounds) {
        int visibleEdgeIdx = 0;

        while (visibleEdgeIdx < edges.size() && (!edges.get(visibleEdgeIdx).isVisible()))
            ++visibleEdgeIdx;

        if (visibleEdgeIdx == edges.size()) {
            // no edges visible
            return new ArrayList<>();
        }

        Edge edge = edges.get(visibleEdgeIdx);
        LR orientation = edgeOrientations.get(visibleEdgeIdx);

        ArrayList<Point> points = new ArrayList<>();

        points.add(edge.getClippedEnds().get(orientation));
        points.add(edge.getClippedEnds().get((LR.other(orientation))));

        for (int j = visibleEdgeIdx + 1; j < edges.size(); ++j) {
            edge = edges.get(j);

            if (!edge.isVisible())
                continue;

            connect(points, j, bounds, false);
        }

        // Close up the polygon by adding another corner point of the bounds if needed.
        connect(points, visibleEdgeIdx, bounds, true);

        return points;
    }

    /**
     * Insert ends of "j" edge into sequence of points representing clipped site border.
     *
     * @param points List containing unfinished sequence.
     * @param j Start index.
     * @param bounds Graph bounds.
     * @param closingUp {@code True} if we should finish connecting.
     */
    private void connect(ArrayList<Point> points, int j, Rectangle bounds, boolean closingUp) {
        Point lastPoint = points.get(points.size() - 1);
        Edge newEdge = edges.get(j);
        LR newOrientation = edgeOrientations.get(j);
        // the point that  must be connected to rightPoint:
        Point newPoint = newEdge.getClippedEnds().get(newOrientation);

        if (!GenUtils.closeEnough(lastPoint, newPoint, EPSILON)) {
            // The points do not coincide, so they must have been clipped at the bounds;
            // see if they are on the same border of the bounds:
            if (lastPoint.x != newPoint.x
                    && lastPoint.y != newPoint.y) {
                // They are on different borders of the bounds;
                // insert one or two corners of bounds as needed to hook them up:
                // (NOTE this will not be correct if the region should take up more than
                // half of the bounds rect, for then we will have gone the wrong way
                // around the bounds and included the smaller part rather than the larger)
                int rightCheck = BoundsCheck.check(lastPoint, bounds);
                int newCheck = BoundsCheck.check(newPoint, bounds);

                double px, py;

                if ((rightCheck & BoundsCheck.RIGHT) != 0) {
                    px = bounds.right;

                    if ((newCheck & BoundsCheck.BOTTOM) != 0) {
                        py = bounds.bottom;
                        points.add(new Point(px, py));
                    } else if ((newCheck & BoundsCheck.TOP) != 0) {
                        py = bounds.top;
                        points.add(new Point(px, py));
                    } else if ((newCheck & BoundsCheck.LEFT) != 0) {
                        if (lastPoint.y - bounds.y + newPoint.y - bounds.y < bounds.height)
                            py = bounds.top;
                        else
                            py = bounds.bottom;

                        points.add(new Point(px, py));
                        points.add(new Point(bounds.left, py));
                    }
                } else if ((rightCheck & BoundsCheck.LEFT) != 0) {
                    px = bounds.left;
                    if ((newCheck & BoundsCheck.BOTTOM) != 0) {
                        py = bounds.bottom;
                        points.add(new Point(px, py));
                    } else if ((newCheck & BoundsCheck.TOP) != 0) {
                        py = bounds.top;
                        points.add(new Point(px, py));
                    } else if ((newCheck & BoundsCheck.RIGHT) != 0) {
                        if (lastPoint.y - bounds.y + newPoint.y - bounds.y < bounds.height) {
                            py = bounds.top;
                        } else {
                            py = bounds.bottom;
                        }
                        points.add(new Point(px, py));
                        points.add(new Point(bounds.right, py));
                    }
                } else if ((rightCheck & BoundsCheck.TOP) != 0) {
                    py = bounds.top;
                    if ((newCheck & BoundsCheck.RIGHT) != 0) {
                        px = bounds.right;
                        points.add(new Point(px, py));
                    } else if ((newCheck & BoundsCheck.LEFT) != 0) {
                        px = bounds.left;
                        points.add(new Point(px, py));
                    } else if ((newCheck & BoundsCheck.BOTTOM) != 0) {
                        if (lastPoint.x - bounds.x + newPoint.x - bounds.x < bounds.width) {
                            px = bounds.left;
                        } else {
                            px = bounds.right;
                        }
                        points.add(new Point(px, py));
                        points.add(new Point(px, bounds.bottom));
                    }
                } else if ((rightCheck & BoundsCheck.BOTTOM) != 0) {
                    py = bounds.bottom;
                    if ((newCheck & BoundsCheck.RIGHT) != 0) {
                        px = bounds.right;
                        points.add(new Point(px, py));
                    } else if ((newCheck & BoundsCheck.LEFT) != 0) {
                        px = bounds.left;
                        points.add(new Point(px, py));
                    } else if ((newCheck & BoundsCheck.TOP) != 0) {
                        if (lastPoint.x - bounds.x + newPoint.x - bounds.x < bounds.width) {
                            px = bounds.left;
                        } else {
                            px = bounds.right;
                        }
                        points.add(new Point(px, py));
                        points.add(new Point(px, bounds.top));
                    }
                }
            }

            if (closingUp) {
                // newEdge's ends have already been added
                return;
            }

            points.add(newPoint);
        }

        Point newRightPoint = newEdge.getClippedEnds().get(LR.other(newOrientation));

        if (!GenUtils.closeEnough(points.get(0), newRightPoint, EPSILON))
            points.add(newRightPoint);
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

    /**
     * @param p Target.
     * @return Distance between site position and given object.
     */
    public double dist(ICoord p) {
        return GenUtils.distance(p.getPosition(), this.position);
    }
}

final class BoundsCheck {

    final public static int TOP = 1;
    final public static int BOTTOM = 2;
    final public static int LEFT = 4;
    final public static int RIGHT = 8;

    /**
     * @param point
     * @param bounds
     * @return an int with the appropriate bits set if the Point lies on the
     * corresponding bounds lines
     */
    public static int check(Point point, Rectangle bounds) {
        int value = 0;

        if (point.x == bounds.left)
            value |= LEFT;
        else if (point.x == bounds.right)
            value |= RIGHT;

        if (point.y == bounds.top)
            value |= TOP;
        else if (point.y == bounds.bottom)
            value |= BOTTOM;

        return value;
    }

    public BoundsCheck() {
        throw new Error("BoundsCheck constructor unused");
    }
}
