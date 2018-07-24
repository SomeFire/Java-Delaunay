package com.hoten.delaunay.voronoi;

import com.hoten.delaunay.geom.GenUtils;
import com.hoten.delaunay.geom.Point;
import com.hoten.delaunay.geom.Rectangle;
import com.hoten.delaunay.voronoi.groundshapes.HeightAlgorithm;
import com.hoten.delaunay.voronoi.nodename.as3delaunay.LineSegment;
import com.hoten.delaunay.voronoi.nodename.as3delaunay.Voronoi;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * VoronoiGraph.java
 *
 * @author Connor
 */
public abstract class VoronoiGraph {

    private final List<Edge> edges = new ArrayList<>();
    private final List<Corner> corners = new ArrayList<>();
    private final List<Center> centers = new ArrayList<>();
    private final Rectangle bounds;
    private final Random r;
    private final BufferedImage pixelCenterMap;
    protected Color OCEAN, RIVER, LAKE, BEACH;

    /**
     * @param v Voronoi structure.
     * @param numLloydRelaxations Amount of Lloyd relaxations.
     * @param r Randomizer.
     * @param algorithm Ground shape algorithm.
     */
    public VoronoiGraph(Voronoi v, int numLloydRelaxations, Random r, HeightAlgorithm algorithm) {
        this.r = r;
        bounds = v.getPlotBounds();

        v = relaxGraph(v, numLloydRelaxations);

        buildGraph(v);
        improveCorners();

        assignCornerElevations(algorithm);
        assignOceanCoastAndLand();
        redistributeElevations(landCorners());
        assignPolygonElevations();

        calculateDownslopes();
        //calculateWatersheds();
        createRivers();
        assignCornerMoisture();
        redistributeMoisture(landCorners());
        assignPolygonMoisture();
        assignBiomes();

        pixelCenterMap = new BufferedImage((int) bounds.width, (int) bounds.width, BufferedImage.TYPE_4BYTE_ABGR);
    }

    /**
     * Isn't Lloyd relaxation, but it's easy workaround.
     *
     * @param v Voronoi structure.
     * @param numLloydRelaxations Amount of relaxation steps.
     * @return Voronoi structure with more evenly distributed points.
     */
    private Voronoi relaxGraph(Voronoi v, int numLloydRelaxations) {
        for (int i = 0; i < numLloydRelaxations; i++) {
            List<Point> points = v.siteCoords();

            for (Point p : points) {
                List<Point> region = v.region(p);

                double x = 0;
                double y = 0;

                for (Point r : region) {
                    x += r.x;
                    y += r.y;
                }

                x /= region.size();
                y /= region.size();

                p.x = x;
                p.y = y;
            }

            v = new Voronoi(points, v.getPlotBounds());
        }

        return v;
    }

    abstract protected Enum getBiome(Center p);

    abstract protected Color getColor(Enum biome);

    private void improveCorners() {
        Point[] newP = new Point[corners.size()];

        for (Corner c : corners) {
            if (c.border) {
                newP[c.index] = c.loc;
            } else {
                double x = 0;
                double y = 0;

                for (Center center : c.touches) {
                    x += center.loc.x;
                    y += center.loc.y;
                }

                newP[c.index] = new Point(x / c.touches.size(), y / c.touches.size());
            }
        }

        corners.stream().forEach((c) -> {
            c.loc = newP[c.index];
        });

        edges.stream().filter((e) -> (e.v0 != null && e.v1 != null)).forEach((e) -> {
            e.setVornoi(e.v0, e.v1);
        });
    }

    private Edge edgeWithCenters(Center c1, Center c2) {
        for (Edge e : c1.borders) {
            if (e.d0 == c2 || e.d1 == c2)
                return e;
        }

        return null;
    }

    private void drawTriangle(Graphics2D g, Corner c1, Corner c2, Center center) {
        int[] x = new int[3];
        int[] y = new int[3];
        x[0] = (int) center.loc.x;
        y[0] = (int) center.loc.y;
        x[1] = (int) c1.loc.x;
        y[1] = (int) c1.loc.y;
        x[2] = (int) c2.loc.x;
        y[2] = (int) c2.loc.y;

        g.fillPolygon(x, y, 3);
    }

    public BufferedImage createMap() {
        int width = (int) bounds.width;
        int height = (int) bounds.height;

        final BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);

        Graphics2D g = img.createGraphics();

        paint(g);

        return img;
    }

    private void paint(Graphics2D g) {
        paint(g, true, true, true, true, true, true);
    }

    private void drawPolygon(Graphics2D g, Center c, Color color) {
        g.setColor(color);

        //only used if Center c is on the edge of the graph. allows for completely filling in the outer polygons
        Corner edgeCorner1 = null;
        Corner edgeCorner2 = null;
        c.area = 0;

        for (Center n : c.neighbors) {
            Edge e = edgeWithCenters(c, n);

            if (e.v0 == null) {
                //outermost voronoi edges aren't stored in the graph
                continue;
            }

            //find a corner on the exterior of the graph
            //if this Edge e has one, then it must have two,
            //finding these two corners will give us the missing
            //triangle to render. this special triangle is handled
            //outside this for loop
            Corner cornerWithOneAdjacent = e.v0.border ? e.v0 : e.v1;
            if (cornerWithOneAdjacent.border) {
                if (edgeCorner1 == null) {
                    edgeCorner1 = cornerWithOneAdjacent;
                } else {
                    edgeCorner2 = cornerWithOneAdjacent;
                }
            }

            drawTriangle(g, e.v0, e.v1, c);

            c.area += Math.abs(c.loc.x * (e.v0.loc.y - e.v1.loc.y)
                    + e.v0.loc.x * (e.v1.loc.y - c.loc.y)
                    + e.v1.loc.x * (c.loc.y - e.v0.loc.y)) / 2;
        }

        //handle the missing triangle
        if (edgeCorner2 != null) {
            //if these two outer corners are NOT on the same exterior edge of the graph,
            //then we actually must render a polygon (w/ 4 points) and take into consideration
            //one of the four corners (either 0,0 or 0,height or width,0 or width,height)
            //note: the 'missing polygon' may have more than just 4 points. this
            //is common when the number of sites are quite low (less than 5), but not a problem
            //with a more useful number of sites. 
            //TODO: find a way to fix this

            if (GenUtils.closeEnough(edgeCorner1.loc.x, edgeCorner2.loc.x, 1)) {
                drawTriangle(g, edgeCorner1, edgeCorner2, c);
            } else {
                int[] x = new int[4];
                int[] y = new int[4];
                x[0] = (int) c.loc.x;
                y[0] = (int) c.loc.y;
                x[1] = (int) edgeCorner1.loc.x;
                y[1] = (int) edgeCorner1.loc.y;

                //determine which corner this is
                x[2] = (int) ((GenUtils.closeEnough(edgeCorner1.loc.x, bounds.x, 1) ||
                    GenUtils.closeEnough(edgeCorner2.loc.x, bounds.x, .5)) ? bounds.x : bounds.right);
                y[2] = (int) ((GenUtils.closeEnough(edgeCorner1.loc.y, bounds.y, 1) ||
                    GenUtils.closeEnough(edgeCorner2.loc.y, bounds.y, .5)) ? bounds.y : bounds.bottom);

                x[3] = (int) edgeCorner2.loc.x;
                y[3] = (int) edgeCorner2.loc.y;

                g.fillPolygon(x, y, 4);
                c.area += 0; //TODO: area of polygon given vertices
            }
        }
    }

    //also records the area of each voronoi cell
    private void paint(Graphics2D g, boolean drawBiomes, boolean drawRivers, boolean drawSites, boolean drawCorners,
        boolean drawDelaunay, boolean drawVoronoi) {
        final int numSites = centers.size();

        Color[] defaultColors = null;

        if (!drawBiomes) {
            defaultColors = new Color[numSites];

            for (int i = 0; i < defaultColors.length; i++)
                defaultColors[i] = new Color(r.nextInt(255), r.nextInt(255), r.nextInt(255));
        }

        Graphics2D pixelCenterGraphics = pixelCenterMap.createGraphics();

        //draw via triangles
        for (Center c : centers) {
            drawPolygon(g, c, drawBiomes ? getColor(c.biome) : defaultColors[c.index]);
            drawPolygon(pixelCenterGraphics, c, new Color(c.index));
            /*Stroke s = g.getStroke();
            g.setStroke(new BasicStroke(5));
            g.setColor(Color.WHITE);
            g.drawString(c.loc.toString(), (int)c.loc.x, (int)c.loc.y);
            g.setStroke(s);*/
        }

        for (Edge e : edges) {
            if (drawDelaunay) {
                g.setStroke(new BasicStroke(1));
                g.setColor(Color.YELLOW);
                g.drawLine((int) e.d0.loc.x, (int) e.d0.loc.y, (int) e.d1.loc.x, (int) e.d1.loc.y);
            }

            if (drawRivers && e.river > 0) {
                g.setStroke(new BasicStroke(1 + (int) Math.sqrt(e.river * 2)));
                g.setColor(RIVER);
                g.drawLine((int) e.v0.loc.x, (int) e.v0.loc.y, (int) e.v1.loc.x, (int) e.v1.loc.y);
            }
        }

        if (drawSites) {
            g.setColor(Color.BLACK);
            centers.stream().forEach((s) -> {
                g.fillOval((int) (s.loc.x - 2), (int) (s.loc.y - 2), 4, 4);
            });
        }

        if (drawCorners) {
            g.setColor(Color.WHITE);
            corners.stream().forEach((c) -> {
                g.fillOval((int) (c.loc.x - 2), (int) (c.loc.y - 2), 4, 4);
                /*Stroke s = g.getStroke();
                g.setStroke(new BasicStroke(5));
                g.setColor(Color.WHITE);
                g.drawString(c.loc.toString(), (int)c.loc.x, (int)c.loc.y);
                g.setStroke(s);*/
            });
        }

        g.setColor(Color.WHITE);
        g.drawRect((int) bounds.x, (int) bounds.y, (int) bounds.width, (int) bounds.height);

        //TODO remove test paint
        /*for (Center center : centers) {
            ArrayList<Point> l = vor.region(center.loc);
            for (Point c : l) {
                Stroke s = g.getStroke();
                g.setStroke(new BasicStroke(5));
                g.setColor(Color.GREEN);
                g.fillOval((int) (c.x - 2), (int) (c.y - 2), 4, 4);
                g.drawString(c.toString(), (int)c.x, (int)c.y);
                g.setStroke(s);
            }
        }*/
    }

    private void buildGraph(Voronoi v) {
        Map<Point, Center> pointCenterMap = generateCenters(v);

        generateEdges(v, pointCenterMap);
    }

    private Map<Point, Center> generateCenters(Voronoi v) {
        Map<Point, Center> pointCenterMap = new HashMap<>();
        List<Point> points = v.siteCoords();

        points.stream().forEach((p) -> {
            Center c = new Center(centers.size(), p);
            centers.add(c);
            pointCenterMap.put(p, c);
        });

        //bug fix
        centers.stream().forEach((c) -> {
            v.region(c.loc);
        });

        return pointCenterMap;
    }

    private void generateEdges(Voronoi v, Map<Point, Center> pointCenterMap) {
        final List<com.hoten.delaunay.voronoi.nodename.as3delaunay.Edge> libEdges = v.edges();
        final Map<Integer, Corner> pointCornerMap = new HashMap<>();

        for (com.hoten.delaunay.voronoi.nodename.as3delaunay.Edge libEdge : libEdges) {
            final LineSegment vEdge = libEdge.voronoiEdge();
            final LineSegment dEdge = libEdge.delaunayLine();

            final Edge edge = new Edge();
            edge.index = edges.size();
            edges.add(edge);

            edge.v0 = makeCorner(pointCornerMap, vEdge.p0);
            edge.v1 = makeCorner(pointCornerMap, vEdge.p1);
            edge.d0 = pointCenterMap.get(dEdge.p0);
            edge.d1 = pointCenterMap.get(dEdge.p1);

            // Centers point to edges. Corners point to edges.
            if (edge.d0 != null) {
                edge.d0.borders.add(edge);
            }
            if (edge.d1 != null) {
                edge.d1.borders.add(edge);
            }
            if (edge.v0 != null) {
                edge.v0.protrudes.add(edge);
            }
            if (edge.v1 != null) {
                edge.v1.protrudes.add(edge);
            }

            // Centers point to centers.
            if (edge.d0 != null && edge.d1 != null) {
                addToCenterList(edge.d0.neighbors, edge.d1);
                addToCenterList(edge.d1.neighbors, edge.d0);
            }

            // Corners point to corners
            if (edge.v0 != null && edge.v1 != null) {
                addToCornerList(edge.v0.adjacent, edge.v1);
                addToCornerList(edge.v1.adjacent, edge.v0);
            }

            // Centers point to corners
            if (edge.d0 != null) {
                addToCornerList(edge.d0.corners, edge.v0);
                addToCornerList(edge.d0.corners, edge.v1);
            }
            if (edge.d1 != null) {
                addToCornerList(edge.d1.corners, edge.v0);
                addToCornerList(edge.d1.corners, edge.v1);
            }

            // Corners point to centers
            if (edge.v0 != null) {
                addToCenterList(edge.v0.touches, edge.d0);
                addToCenterList(edge.v0.touches, edge.d1);
            }
            if (edge.v1 != null) {
                addToCenterList(edge.v1.touches, edge.d0);
                addToCenterList(edge.v1.touches, edge.d1);
            }
        }
    }

    // Helper functions for the following for loop; ideally these
    // would be inlined
    private void addToCornerList(List<Corner> list, Corner c) {
        if (c != null && !list.contains(c))
            list.add(c);
    }

    private void addToCenterList(List<Center> list, Center c) {
        if (c != null && !list.contains(c))
            list.add(c);
    }

    //ensures that each corner is represented by only one corner object
    private Corner makeCorner(Map<Integer, Corner> pointCornerMap, Point p) {
        if (p == null)
            return null;

        int index = (int) ((int) p.x + (int) (p.y) * bounds.width * 2);

        Corner c = pointCornerMap.get(index);

        if (c == null) {
            c = new Corner();
            c.loc = p;
            c.border = bounds.liesOnAxes(p);
            c.index = corners.size();
            corners.add(c);

            pointCornerMap.put(index, c);
        }

        return c;
    }

    private void assignCornerElevations(HeightAlgorithm algorithm) {
        Deque<Corner> queue = new LinkedList<>();

        for (Corner c : corners) {
            c.water = algorithm.isWater(c.loc, bounds, r);

            if (c.border) {
                c.elevation = 0;
                queue.add(c);
            } else {
                c.elevation = Double.MAX_VALUE;
            }
        }

        while (!queue.isEmpty()) {
            Corner c = queue.pop();

            for (Corner a : c.adjacent) {
                double newElevation = 0.01 + c.elevation;

                if (!c.water && !a.water) {
                    newElevation += 1;
                }

                if (newElevation < a.elevation) {
                    a.elevation = newElevation;
                    queue.add(a);
                }
            }
        }
    }

    private void assignOceanCoastAndLand() {
        Deque<Center> queue = new LinkedList<>();
        final double waterThreshold = .3;

        for (final Center center : centers) {
            int numWater = 0;

            for (final Corner c : center.corners) {
                if (c.border) {
                    center.border = center.water = center.ocean = true;
                    queue.add(center);
                }

                if (c.water) {
                    numWater++;
                }
            }

            center.water = center.ocean || ((double) numWater / center.corners.size() >= waterThreshold);
        }

        while (!queue.isEmpty()) {
            final Center center = queue.pop();

            for (final Center n : center.neighbors) {
                if (n.water && !n.ocean) {
                    n.ocean = true;
                    queue.add(n);
                }
            }
        }

        for (Center center : centers) {
            boolean oceanNeighbor = false;
            boolean landNeighbor = false;

            for (Center n : center.neighbors) {
                oceanNeighbor |= n.ocean;
                landNeighbor |= !n.water;
            }

            center.coast = oceanNeighbor && landNeighbor;
        }

        for (Corner c : corners) {
            int numOcean = 0;
            int numLand = 0;

            for (Center center : c.touches) {
                numOcean += center.ocean ? 1 : 0;
                numLand += !center.water ? 1 : 0;
            }

            c.ocean = numOcean == c.touches.size();
            c.coast = numOcean > 0 && numLand > 0;
            c.water = c.border || ((numLand != c.touches.size()) && !c.coast);
        }
    }

    private List<Corner> landCorners() {
        List<Corner> list = new ArrayList<>();

        for (Corner c : corners) {
            if (!c.ocean && !c.coast)
                list.add(c);
        }

        return list;
    }

    private void redistributeElevations(List<Corner> landCorners) {
        landCorners.sort(Comparator.comparingDouble(c -> c.elevation));

        final double SCALE_FACTOR = 1.1;

        for (int i = 0; i < landCorners.size(); i++) {
            double y = (double) i / landCorners.size();
            double x = Math.sqrt(SCALE_FACTOR) - Math.sqrt(SCALE_FACTOR * (1 - y));
            x = Math.min(x, 1);
            landCorners.get(i).elevation = x;
        }

        for (Corner c : corners) {
            if (c.ocean || c.coast)
                c.elevation = 0.0;
        }
    }

    private void assignPolygonElevations() {
        for (Center center : centers) {
            double total = 0;

            for (Corner c : center.corners)
                total += c.elevation;

            center.elevation = total / center.corners.size();
        }
    }

    private void calculateDownslopes() {
        for (Corner c : corners) {
            Corner down = c;
            //System.out.println("ME: " + c.elevation);
            for (Corner a : c.adjacent) {
                //System.out.println(a.elevation);
                if (a.elevation <= down.elevation)
                    down = a;
            }

            c.downslope = down;
        }
    }

    private void createRivers() {
        for (int i = 0; i < bounds.width / 2; i++) {
            Corner c = corners.get(r.nextInt(corners.size()));

            if (c.ocean || c.elevation < 0.3 || c.elevation > 0.9) {
                continue;
            }

            // Bias rivers to go west: if (q.downslope.x > q.x) continue;
            while (!c.coast) {
                if (c == c.downslope) {
                    break;
                }

                Edge edge = lookupEdgeFromCorner(c, c.downslope);

                if (!edge.v0.water || !edge.v1.water) {
                    edge.river++;
                    c.river++;
                    c.downslope.river++;  // TODO: fix double count
                }

                c = c.downslope;
            }
        }
    }

    private Edge lookupEdgeFromCorner(Corner c, Corner downslope) {
        for (Edge e : c.protrudes) {
            if (e.v0 == downslope || e.v1 == downslope)
                return e;
        }

        return null;
    }

    private void assignCornerMoisture() {
        Deque<Corner> queue = new LinkedList<>();

        for (Corner c : corners) {
            if ((c.water || c.river > 0) && !c.ocean) {
                c.moisture = c.river > 0 ? Math.min(3.0, (0.2 * c.river)) : 1.0;
                queue.push(c);
            } else {
                c.moisture = 0.0;
            }
        }

        while (!queue.isEmpty()) {
            Corner c = queue.pop();

            for (Corner a : c.adjacent) {
                double newM = .9 * c.moisture;

                if (newM > a.moisture) {
                    a.moisture = newM;
                    queue.add(a);
                }
            }
        }

        // Salt water
        for (Corner c : corners) {
            if (c.ocean || c.coast)
                c.moisture = 1.0;
        }
    }

    private void redistributeMoisture(List<Corner> landCorners) {
        landCorners.sort(Comparator.comparingDouble(c -> c.moisture));

        for (int i = 0; i < landCorners.size(); i++)
            landCorners.get(i).moisture = (double) i / landCorners.size();
    }

    private void assignPolygonMoisture() {
        for (Center center : centers) {
            double total = 0;

            for (Corner c : center.corners)
                total += c.moisture;

            center.moisture = total / center.corners.size();
        }
    }

    private void assignBiomes() {
        for (Center center : centers)
            center.biome = getBiome(center);
    }
}
