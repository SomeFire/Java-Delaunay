package com.hoten.delaunay.voronoi.nodename.as3delaunay;

/*
 * Java implementaition by Connor Clark (www.hotengames.com). Pretty much a 1:1 
 * translation of a wonderful map generating algorthim by Amit Patel of Red Blob Games,
 * which can be found here (http://www-cs-students.stanford.edu/~amitp/game-programming/polygon-map-generation/)
 * Hopefully it's of use to someone out there who needed it in Java like I did!
 * Note, the only island mode implemented is Radial. Implementing more is something for another day.
 * 
 * FORTUNE'S ALGORTIHIM
 * 
 * This is a java implementation of an AS3 (Flash) implementation of an algorthim
 * originally created in C++. Pretty much a 1:1 translation from as3 to java, save
 * for some necessary workarounds. Original as3 implementation by Alan Shaw (of nodename)
 * can be found here (https://github.com/nodename/as3delaunay). Original algorthim
 * by Steven Fortune (see lisence for c++ implementation below)
 * 
 * The author of this software is Steven Fortune.  Copyright (c) 1994 by AT&T
 * Bell Laboratories.
 * Permission to use, copy, modify, and distribute this software for any
 * purpose without fee is hereby granted, provided that this entire notice
 * is included in all copies of any software which is or includes a copy
 * or modification of this software and in all copies of the supporting
 * documentation for such software.
 * THIS SOFTWARE IS BEING PROVIDED "AS IS", WITHOUT ANY EXPRESS OR IMPLIED
 * WARRANTY.  IN PARTICULAR, NEITHER THE AUTHORS NOR AT&T MAKE ANY
 * REPRESENTATION OR WARRANTY OF ANY KIND CONCERNING THE MERCHANTABILITY
 * OF THIS SOFTWARE OR ITS FITNESS FOR ANY PARTICULAR PURPOSE.
 */

import com.hoten.delaunay.geom.GenUtils;
import com.hoten.delaunay.geom.Point;
import com.hoten.delaunay.geom.Rectangle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class Voronoi {

    private SiteList sites;
    private Map<Point, Site> center2siteMap;
    private List<Triangle> _triangles;
    private List<Edge> edges;
    // TODO generalize this so it doesn't have to be a rectangle;
    // then we can make the fractal voronois-within-voronois
    /** Graph bounds. */
    private Rectangle plotBounds;

    public Rectangle getPlotBounds() {
        return plotBounds;
    }

    /**
     * @param points Graph points.
     * @param plotBounds Bounds.
     */
    public Voronoi(List<Point> points, Rectangle plotBounds) {
        init(points, plotBounds);
        fortunesAlgorithm();
    }

    /**
     * @param points Graph points.
     */
    public Voronoi(List<Point> points) {
        double maxWidth = 0, maxHeight = 0;
        double shiftX = 0, shiftY = 0;

        for (Point p : points) {
            maxWidth = Math.max(maxWidth, p.x);
            maxHeight = Math.max(maxHeight, p.y);

            if (p.x < shiftX)
                shiftX = p.x;

            if (p.y < shiftY)
                shiftY = p.y;
        }

        if (shiftX != 0 || shiftY != 0) {
            ArrayList<Point> newPoints = new ArrayList<>();

            for (Point p : points)
                newPoints.add(new Point(p.x - shiftX, p.y - shiftY));

            maxWidth -= shiftX;
            maxHeight -= shiftY;

            points = newPoints;
        }

        System.out.println("Graph bounds: width = " + maxWidth + ", height = " + maxHeight);

        init(points,  new Rectangle(0, 0, maxWidth, maxHeight));

        fortunesAlgorithm();
    }

    /**
     * @param numSites Amount of sites.
     * @param maxWidth Graph width.
     * @param maxHeight Graph height.
     * @param r Randomizer.
     */
    public Voronoi(int numSites, double maxWidth, double maxHeight, Random r) {
        ArrayList<Point> points = new ArrayList<>();

        for (int i = 0; i < numSites; i++)
            points.add(new Point(r.nextDouble() * maxWidth, r.nextDouble() * maxHeight));

        init(points, new Rectangle(0, 0, maxWidth, maxHeight));

        fortunesAlgorithm();
    }

    private void init(List<Point> points, Rectangle plotBounds) {
        sites = new SiteList();
        center2siteMap = new HashMap<>();
        addSites(points);
        this.plotBounds = plotBounds;
        _triangles = new ArrayList<>();
        edges = new ArrayList<>();
    }

    private void addSites(List<Point> points) {
        for (int idx = 0; idx < points.size(); ++idx) {
            Point point = points.get(idx);
            double weight = Math.random() * 100;

            Site site = new Site(point, idx, weight);

            sites.add(site);
            center2siteMap.put(point, site);
        }
    }

    /**
     * @return Graph edges.
     */
    public List<Edge> edges() {
        return edges;
    }

    /**
     * @param p Site center.
     * @return Site border represented as continuous line of edge corners clipped in graph bounds.
     * Empty array if site with center in given point doesn't exist.
     */
    public List<Point> region(Point p) {
        Site site = center2siteMap.get(p);

        if (site == null)
            return new ArrayList<>();

        return site.region(plotBounds);
    }

    // TODO: bug (from AS, no entries found): if you call this before you call region(), something goes wrong :(
    public List<Point> neighborSitesForSite(Point coord) {
        List<Point> points = new ArrayList<>();
        Site site = center2siteMap.get(coord);

        if (site == null)
            return points;

        List<Site> sites = site.neighborSites();

        for (Site neighbor : sites)
            points.add(neighbor.getPosition());

        return points;
    }

    public List<Circle> circles() {
        return sites.circles();
    }

    private List<Edge> selectEdgesForSitePoint(Point coord, List<Edge> edgesToTest) {
        List<Edge> filtered = new ArrayList<>();

        for (Edge e : edgesToTest) {
            //TODO extract condition
            if (((e.getLeftSite() != null && e.getLeftSite().getPosition() == coord)
                    || (e.getRightSite() != null && e.getRightSite().getPosition() == coord))) {
                filtered.add(e);
            }
        }

        return filtered;

        /*function myTest(edge:Edge, index:int, vector:Vector.<Edge>):Boolean
         {
         return ((edge.leftSite && edge.leftSite.coord == coord)
         ||  (edge.rightSite && edge.rightSite.coord == coord));
         }*/
    }

    private List<LineSegment> visibleLineSegments(List<Edge> edges) {
        List<LineSegment> segments = new ArrayList<>();

        for (Edge edge : edges) {
            if (edge.isVisible()) {
                Point p1 = edge.getClippedEnds().get(LR.LEFT);
                Point p2 = edge.getClippedEnds().get(LR.RIGHT);
                segments.add(new LineSegment(p1, p2));
            }
        }

        return segments;
    }

    private List<LineSegment> delaunayLinesForEdges(List<Edge> edges) {
        List<LineSegment> segments = new ArrayList<>();

        for (Edge edge : edges)
            segments.add(edge.delaunayLine());

        return segments;
    }

    public List<LineSegment> voronoiBoundaryForSite(Point coord) {
        return visibleLineSegments(selectEdgesForSitePoint(coord, edges));
    }

    public List<LineSegment> delaunayLinesForSite(Point coord) {
        return delaunayLinesForEdges(selectEdgesForSitePoint(coord, edges));
    }

    public List<LineSegment> voronoiDiagram() {
        return visibleLineSegments(edges);
    }

    /*public ArrayList<LineSegment> delaunayTriangulation(keepOutMask:BitmapData = null)
     {
     return delaunayLinesForEdges(selectNonIntersectingEdges(keepOutMask, edges));
     }*/
    public List<LineSegment> hull() {
        return delaunayLinesForEdges(hullEdges());
    }

    private List<Edge> hullEdges() {
        List<Edge> filtered = new ArrayList<>();

        for (Edge e : edges) {
            if (e.isPartOfConvexHull())
                filtered.add(e);
        }

        return filtered;

        /*function myTest(edge:Edge, index:int, vector:Vector.<Edge>):Boolean
         {
         return (edge.isPartOfConvexHull());
         }*/
    }

    public List<Point> hullPointsInOrder() {
        List<Edge> hullEdges = hullEdges();

        List<Point> points = new ArrayList<>();

        if (hullEdges.isEmpty())
            return points;

        EdgeReorderer reorderer = new EdgeReorderer(hullEdges, Site.class);

        hullEdges = reorderer.getEdges();

        List<LR> orientations = reorderer.getEdgeOrientations();

        LR orientation;

        int n = hullEdges.size();

        for (int i = 0; i < n; ++i) {
            Edge edge = hullEdges.get(i);
            orientation = orientations.get(i);
            points.add(edge.site(orientation).getPosition());
        }

        return points;
    }

    /*public ArrayList<LineSegment> spanningTree(String type, keepOutMask:BitmapData = null)
     {
     ArrayList<Edge>  edges = selectNonIntersectingEdges(keepOutMask, edges);
     ArrayList<LineSegment>  segments = delaunayLinesForEdges(edges);
     return kruskal(segments, type);
     }*/

    /**
     * @return Borders for every site.
     * Each border represented as continuous line of edge corners clipped in graph bounds.
     */
    public List<List<Point>> regions() {
        return sites.regions(plotBounds);
    }

    /*public ArrayList<Integer> siteColors(referenceImage:BitmapData = null)
     {
     return sites.siteColors(referenceImage);
     }*/
    /*
     *
     * @param proximityMap a BitmapData whose regions are filled with the site
     * index values; see PlanePointsCanvas::fillRegions()
     * @param x
     * @param y
     * @return coordinates of nearest Site to (x, y)
     *
     */
    /*public Point nearestSitePoint(proximityMap:BitmapData,double x, double y)
     {
     return sites.nearestSitePoint(proximityMap, x, y);
     }*/
    public List<Point> siteCoords() {
        return sites.siteCoords();
    }

    private void fortunesAlgorithm() {
        Site newSite, bottomSite, topSite, tempSite;
        Vertex v, vertex;
        Point newIntStar = null;
        LR leftRight;
        Halfedge lbnd, rbnd, llbnd, rrbnd, bisector;
        Edge edge;

        sites.sort();

        Rectangle dataBounds = sites.getSitesBounds();

        int sqrt_nsites = (int) Math.sqrt(sites.size() + 4);
        HalfedgePriorityQueue heap = new HalfedgePriorityQueue(dataBounds.y, dataBounds.height, sqrt_nsites);
        HalfedgeList halfedgeList = new HalfedgeList(dataBounds.x, dataBounds.width, sqrt_nsites);

        Site bottomMostSite = sites.next();
        newSite = sites.next();

        int totalVertices = 0;
        int totalEdges = 0;

        for (;;) {
            if (!heap.empty())
                newIntStar = heap.min();

            if (newSite != null && (heap.empty() || GenUtils.compareByYThenX(newSite, newIntStar) < 0)) {
                /* new site is smallest */
                //trace("smallest: new site " + newSite);

                // Step 8:
                lbnd = halfedgeList.edgeListLeftNeighbor(newSite.getPosition());	// the Halfedge just to the left of newSite
                //trace("lbnd: " + lbnd);
                rbnd = lbnd.edgeListRightNeighbor;		// the Halfedge just to the right
                //trace("rbnd: " + rbnd);
                bottomSite = rightRegion(lbnd, bottomMostSite);		// this is the same as leftRegion(rbnd)
                // this Site determines the region containing the new site
                //trace("new Site is in region of existing site: " + bottomSite);

                // Step 9:
                edge = Edge.createBisectingEdge(bottomSite, newSite, totalEdges++);
                //trace("new edge: " + edge);
                edges.add(edge);

                bisector = new Halfedge(edge, LR.LEFT);
                // inserting two Halfedges into edgeList constitutes Step 10:
                // insert bisector to the right of lbnd:
                halfedgeList.insert(lbnd, bisector);

                // first half of Step 11:
                if ((vertex = Vertex.intersect(lbnd, bisector)) != null) {
                    heap.remove(lbnd);
                    lbnd.vertex = vertex;
                    lbnd.ystar = vertex.getY() + newSite.dist(vertex);
                    heap.insert(lbnd);
                }

                lbnd = bisector;
                bisector = new Halfedge(edge, LR.RIGHT);
                // second Halfedge for Step 10:
                // insert bisector to the right of lbnd:
                halfedgeList.insert(lbnd, bisector);

                // second half of Step 11:
                if ((vertex = Vertex.intersect(bisector, rbnd)) != null) {
                    bisector.vertex = vertex;
                    bisector.ystar = vertex.getY() + newSite.dist(vertex);
                    heap.insert(bisector);
                }

                newSite = sites.next();
            } else if (!heap.empty()) {
                /* intersection is smallest */
                lbnd = heap.extractMin();
                llbnd = lbnd.edgeListLeftNeighbor;
                rbnd = lbnd.edgeListRightNeighbor;
                rrbnd = rbnd.edgeListRightNeighbor;
                bottomSite = leftRegion(lbnd, bottomMostSite);
                topSite = rightRegion(rbnd, bottomMostSite);
                // these three sites define a Delaunay triangle
                // (not actually using these for anything...)
                //_triangles.add(new Triangle(bottomSite, topSite, rightRegion(lbnd)));

                v = lbnd.vertex;
                v.setIndex(totalVertices++);
                lbnd.edge.setVertex(lbnd.leftRight, v);
                rbnd.edge.setVertex(rbnd.leftRight, v);
                halfedgeList.remove(lbnd);
                heap.remove(rbnd);
                halfedgeList.remove(rbnd);
                leftRight = LR.LEFT;
                if (bottomSite.getY() > topSite.getY()) {
                    tempSite = bottomSite;
                    bottomSite = topSite;
                    topSite = tempSite;
                    leftRight = LR.RIGHT;
                }
                edge = Edge.createBisectingEdge(bottomSite, topSite, totalEdges++);
                edges.add(edge);
                bisector = new Halfedge(edge, leftRight);
                halfedgeList.insert(llbnd, bisector);
                edge.setVertex(LR.other(leftRight), v);
                if ((vertex = Vertex.intersect(llbnd, bisector)) != null) {
                    heap.remove(llbnd);
                    llbnd.vertex = vertex;
                    llbnd.ystar = vertex.getY() + bottomSite.dist(vertex);
                    heap.insert(llbnd);
                }
                if ((vertex = Vertex.intersect(bisector, rrbnd)) != null) {
                    bisector.vertex = vertex;
                    bisector.ystar = vertex.getY() + bottomSite.dist(vertex);
                    heap.insert(bisector);
                }
            } else {
                break;
            }
        }

        // heap should be empty now
        heap.dispose();

        // we need the vertices to clip the edges
        for (Edge e : edges)
            e.clipVertices(plotBounds);
    }

    Site leftRegion(Halfedge he, Site bottomMostSite) {
        Edge edge = he.edge;

        if (edge == null)
            return bottomMostSite;

        return edge.site(he.leftRight);
    }

    Site rightRegion(Halfedge he, Site bottomMostSite) {
        Edge edge = he.edge;

        if (edge == null)
            return bottomMostSite;

        return edge.site(LR.other(he.leftRight));
    }
}
