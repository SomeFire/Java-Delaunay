package com.hoten.delaunay.voronoi.groundshapes;

import com.hoten.delaunay.geom.Point;
import com.hoten.delaunay.geom.Rectangle;

import java.util.Random;

/**
 * Use implementation of this interface to find out which points in graph are water and which - ground.
 */
public interface HeightAlgorithm {

    /**
     * Uses specific algorithm to check point.
     *
     * @param p Corner location.
     * @param bounds Graph bounds.
     * @param random Voronoi's randomizer to keep identical results for user's seed.
     * @return {@code True} if point belongs water, {@code false} if point belongs ground.
     */
    boolean isWater(Point p, Rectangle bounds, Random random);

}