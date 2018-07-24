package com.hoten.delaunay.voronoi;

import com.hoten.delaunay.geom.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents center of voronoi graph and corner of the delaunay triangulation in the same time.
 *
 * @author Connor
 */
public class Center {

    /** Center index in graph. */
    public final int index;

    /** Center position. */
    public final Point loc;

    public List<Corner> corners = new ArrayList<>();

    public List<Center> neighbors = new ArrayList<>();

    public List<Edge> borders = new ArrayList<>();

    public boolean border, ocean, water, coast;

    public double elevation;

    public double moisture;

    /** Biome. */
    public Enum biome;

    public double area;

    /**
     * @param index
     * @param loc Center position.
     */
    public Center(int index, Point loc) {
        this.index = index;
        this.loc = loc;
    }
}
