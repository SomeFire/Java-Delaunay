package com.hoten.delaunay.examples;

import com.hoten.delaunay.voronoi.groundshapes.Blob;
import com.hoten.delaunay.voronoi.groundshapes.HeightAlgorithm;
import com.hoten.delaunay.voronoi.groundshapes.Perlin;
import com.hoten.delaunay.voronoi.groundshapes.Radial;
import java.util.HashMap;
import java.util.Random;

/**
 * Change variables here to customize voronoi graph.
 */
class Variables {

    /** Do you really need to save image? */
    static final boolean SAVE_FILE = false;

    /** The side of the square in which the graph will be fitted. */
    static final int GRAPH_BOUNDS = 2048;

    /** Size of image which will be drawn. */
    static final int FRAME_BOUNDS = 512;

    /** Number of pieces for the graph. */
    static final int SITES_AMOUNT = 8_000;

    /**
     * Each time a relaxation step is performed, the points are left in a slightly more even distribution:
     * closely spaced points move farther apart, and widely spaced points move closer together.
     */
    static final int LLOYD_RELAXATIONS = 2;

    /** Example's frame title. */
    static final String FRAME_TITLE = "Java Fortune";

    /** Same value will create same image every time. */
    static final long SEED = System.nanoTime(); // Use this if you want random images every launch.
    //static final long SEED = 123L;

    /** Random, radial, blob, etc. See {@link #getAlgorithmImplementation(Random, String)} */
    static final String ALGORITHM = "perlin";

    /**
     * Currently there are only 1 algorithm. You can choose one of algorithms exactly or random from this list:
     * <ol start = "0">
     *     <li>random</li>
     *     <li>radial</li>
     *     <li>blob</li>
     *     <li>perlin</li>
     * </ol>
     *
     * @param r Randomizer.
     * @param name Name of the algorithm.
     * @return Selected HeightAlgorithm implementation.
     */
    static HeightAlgorithm getAlgorithmImplementation(Random r, String name) {
        HashMap<String, Integer> implementations = new HashMap<>();

        implementations.put("random", 0);
        implementations.put("radial", 1);
        implementations.put("blob", 2);
        implementations.put("perlin", 3);

        int i = implementations.getOrDefault(name, 0);

        if (i == 0)
            i = 1 + r.nextInt(implementations.size() - 1);

        switch (i) {
            case 1: return new Radial(1.07,
                r.nextInt(5) + 1,
                r.nextDouble() * 2 * Math.PI,
                r.nextDouble() * 2 * Math.PI,
                r.nextDouble() * .5 + .2);

            case 2: return new Blob();

            case 3: return new Perlin(r, 7, 256, 256);

            default: throw new RuntimeException("Method \"getAlgorithmImplementation()\" is broken. " +
                "Check implementations map and switch statement. Their values and cases must match.");
        }
    }
}
