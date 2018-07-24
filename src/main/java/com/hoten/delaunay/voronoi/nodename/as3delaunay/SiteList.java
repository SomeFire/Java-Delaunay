package com.hoten.delaunay.voronoi.nodename.as3delaunay;

import com.hoten.delaunay.geom.Point;
import com.hoten.delaunay.geom.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Internal list for sites.
 */
final class SiteList {

    /** Site list. */
    private final List<Site> sites = new ArrayList<>();

    /** Index to iterate over collection. */
    private int currentIndex;

    /** */
    private boolean sorted;

    /**
     * @param site Site.
     * @return New site list size.
     */
    public int add(Site site) {
        sorted = false;
        sites.add(site);
        return sites.size();
    }

    /**
     * @return Site count.
     */
    public int size() {
        return sites.size();
    }

    /**
     * @return Iterate to the next site.
     * @throws IllegalStateException If sites are not sorted.
     */
    public Site next() {
        if (!sorted)
            throw new IllegalStateException("SiteList::next():  sites have not been sorted");

        if (currentIndex < sites.size())
            return sites.get(currentIndex++);

        return null;
    }

    /**
     * If not sorted - sort sites and reset iterator.
     */
    public void sort() {
        if (!sorted) {
            Site.sortSites(sites);
            currentIndex = 0;
            sorted = true;
        }
    }

    /**
     * @return Bounds where all sites fitted.
     */
    public Rectangle getSitesBounds() {
        double xmin, xmax, ymin, ymax;

        if (sites.isEmpty())
            return new Rectangle(0, 0, 0, 0);

        xmin = Double.MAX_VALUE;
        xmax = Double.MIN_VALUE;

        for (Site site : sites) {
            if (site.getX() < xmin)
                xmin = site.getX();

            if (site.getX() > xmax)
                xmax = site.getX();
        }

        // here's where we assume that the sites have been sorted on y:
        ymin = sites.get(0).getY();
        ymax = sites.get(sites.size() - 1).getY();

        return new Rectangle(xmin, ymin, xmax - xmin, ymax - ymin);
    }

    /*public ArrayList<Color> siteColors(referenceImage:BitmapData = null)
     {
     var colors:Vector.<uint> = new Vector.<uint>();
     for each (var site:Site in sites)
     {
     colors.add(referenceImage ? referenceImage.getPixel(site.x, site.y) : site.color);
     }
     return colors;
     }*/

    /**
     * @return Coordinates of all sites in this site list.
     */
    public List<Point> siteCoords() {
        List<Point> coords = new ArrayList<>();

        for (Site site : sites)
            coords.add(site.getPosition());

        return coords;
    }

    /**
     * @return the largest circle centered at each site that fits in its region;
     * if the region is infinite, return a circle of radius 0.
     */
    public List<Circle> circles() {
        List<Circle> circles = new ArrayList<>();

        for (Site site : sites) {
            double radius = 0;

            Edge nearestEdge = site.nearestEdge();

            //!nearestEdge.isPartOfConvexHull() && (radius = nearestEdge.sitesDistance() * 0.5);
            if (!nearestEdge.isPartOfConvexHull())
                radius = nearestEdge.sitesDistance() * 0.5;

            circles.add(new Circle(site.getX(), site.getY(), radius));
        }

        return circles;
    }

    /**
     * Region is continuous line represented as a sequence of points clipped in graph bounds.
     *
     * @param plotBounds Graph bounds.
     * @return Regions for sites in this site list.
     */
    public List<List<Point>> regions(Rectangle plotBounds) {
        List<List<Point>> regions = new ArrayList<>();

        for (Site site : sites)
            regions.add(site.region(plotBounds));

        return regions;
    }

    /**
     *
     * @param proximityMap a BitmapData whose regions are filled with the site
     * index values; see PlanePointsCanvas::fillRegions()
     * @param x
     * @param y
     * @return coordinates of nearest Site to (x, y)
     *
     */
    /*public Point nearestSitePoint(proximityMap:BitmapData, double x, double y)
     {
     var index:uint = proximityMap.getPixel(x, y);
     if (index > sites.length - 1)
     {
     return null;
     }
     return sites[index].coord;
     }*/
}