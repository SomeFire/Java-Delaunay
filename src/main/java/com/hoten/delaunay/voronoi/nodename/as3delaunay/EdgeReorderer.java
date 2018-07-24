package com.hoten.delaunay.voronoi.nodename.as3delaunay;

import java.util.ArrayList;
import java.util.List;

final class EdgeReorderer {

    /** Sorted edges. Will be empty in case of failed reordering. */
    private final List<Edge> edges;

    /** Edge start orientations in traversal order. Will be empty in case of failed reordering. */
    private final List<LR> edgeOrientations;

    /**
     * @return Sorted edges. Will be empty in case of failed reordering.
     */
    List<Edge> getEdges() {
        return edges;
    }

    /**
     * @return Edge start orientations in traversal order. Will be empty in case of failed reordering.
     */
    List<LR> getEdgeOrientations() {
        return edgeOrientations;
    }

    /**
     * We're going to reorder the edges in order of traversal.
     * <p>
     * In case of {@link Vertex} criterion
     * {@link #edges} will contain sorted border (continuous closed line) for a site.
     * <br>
     * In case of {@link Site} criterion - sorted edges between convex hull sites.
     * <br>
     * Or empty list in case of failure.
     *
     * @param origEdges Original edge list. It should be site border or edges between convex hull sites.
     * @param criterion Determines what we need to reorder.
     */
    EdgeReorderer(List<Edge> origEdges, Class<? extends ICoord> criterion) {
        assert criterion == Vertex.class || criterion == Site.class : "Edges: criterion must be Vertex or Site";

        edges = new ArrayList<>();
        edgeOrientations = new ArrayList<>();

        if (origEdges.size() > 0) {
            boolean reordered = reorderEdges(origEdges, criterion);

            if (!reordered) {
                edges.clear();
                edgeOrientations.clear();
            }
        }
    }

    /**
     * @param origEdges Original edge list. It should be site border or edges between convex hull sites.
     * @param criterion Determines what we need to reorder.
     * @return {@code True} if reordering was successfully finished.
     */
    private boolean reorderEdges(List<Edge> origEdges, Class<? extends ICoord> criterion) {
        int n = origEdges.size();
        boolean done[] = new boolean[n];
        int i = 0;

        Edge edge = origEdges.get(i);

        edges.add(edge);
        edgeOrientations.add(LR.LEFT);

        ICoord firstPoint = (criterion == Vertex.class) ? edge.getLeftVertex() : edge.getLeftSite();
        ICoord lastPoint = (criterion == Vertex.class) ? edge.getRightVertex() : edge.getRightSite();

        if (firstPoint == Vertex.VERTEX_AT_INFINITY || lastPoint == Vertex.VERTEX_AT_INFINITY)
            return false;

        done[i] = true;

        int nDone = 1;

        while (nDone < n) {
            int nBefore = nDone;

            for (i = 1; i < n; ++i) {
                if (done[i])
                    continue;

                edge = origEdges.get(i);
                ICoord leftPoint = (criterion == Vertex.class) ? edge.getLeftVertex() : edge.getLeftSite();
                ICoord rightPoint = (criterion == Vertex.class) ? edge.getRightVertex() : edge.getRightSite();

                if (leftPoint == Vertex.VERTEX_AT_INFINITY || rightPoint == Vertex.VERTEX_AT_INFINITY)
                    return false;

                if (leftPoint == lastPoint) {
                    lastPoint = rightPoint;
                    edgeOrientations.add(LR.LEFT);
                    edges.add(edge);
                    done[i] = true;
                } else if (rightPoint == firstPoint) {
                    firstPoint = leftPoint;
                    edgeOrientations.add(0, LR.LEFT);
                    edges.add(0, edge);
                    done[i] = true;
                } else if (leftPoint == firstPoint) {
                    firstPoint = rightPoint;
                    edgeOrientations.add(0, LR.RIGHT);
                    edges.add(0, edge);
                    done[i] = true;
                } else if (rightPoint == lastPoint) {
                    lastPoint = leftPoint;
                    edgeOrientations.add(LR.RIGHT);
                    edges.add(edge);
                    done[i] = true;
                }

                if (done[i])
                    ++nDone;
            }

            // Infinite loop
            if (nBefore == nDone)
                return false;
        }

        return true;
    }
}