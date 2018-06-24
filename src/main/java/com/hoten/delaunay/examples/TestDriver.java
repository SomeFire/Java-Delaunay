package com.hoten.delaunay.examples;

import com.hoten.delaunay.voronoi.VoronoiGraph;
import com.hoten.delaunay.voronoi.groundshapes.*;
import com.hoten.delaunay.voronoi.nodename.as3delaunay.Voronoi;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.hoten.delaunay.examples.Variables.*;

/**
 * <h3>How to use:</h3>
 * Just change {@link Variables variables} to customize graph, it's shape and image.
 */
public class TestDriver {

    /** */
    public static void main(String[] args) throws IOException {
        printInfo();

        BufferedImage img = createVoronoiGraph(
            GRAPH_BOUNDS,
            SITES_AMOUNT,
            LLOYD_RELAXATIONS,
            SEED,
            ALGORITHM
        ).createMap();

        saveFile(img);

        showGraph(img);
    }

    /** */
    private static void printInfo() {
        System.out.println("Seed: " + SEED);
        System.out.println("Bounds: " + GRAPH_BOUNDS);
        System.out.println("Sites: " + SITES_AMOUNT);
        System.out.println("Shape: " + ALGORITHM);
        System.out.println("Relaxs: " + LLOYD_RELAXATIONS);
        System.out.println("=============================");
    }

    /**
     * @param bounds Graph size in pixels. Currently it is square.
     * @param numSites Number of tiles in graph.
     * @param numLloydRelaxations Relaxations. See {@link Variables#LLOYD_RELAXATIONS}.
     * @param seed Random seed.
     * @param algorithmName Algorithm name. See {@link Variables#getAlgorithmImplementation(Random, String)}
     * @return Voronoi graph.
     */
    private static VoronoiGraph createVoronoiGraph(
        int bounds,
        int numSites,
        int numLloydRelaxations,
        long seed,
        String algorithmName
    ) {
        final Random r = new Random(seed);
        HeightAlgorithm algorithm = getAlgorithmImplementation(r, algorithmName);

        //make the intial underlying voronoi structure
        final Voronoi v = new Voronoi(numSites, bounds, bounds, r, null);

        //assemble the voronoi strucutre into a usable graph object representing a map
        final TestGraphImpl graph = new TestGraphImpl(v, numLloydRelaxations, r, algorithm);

        return graph;
    }

    /**
     * Save PNG image to the \"output\" directory.
     *
     * @param img Image to save.
     * @throws IOException If failed to write file.
     */
    private static void saveFile(BufferedImage img) throws IOException {
        if (SAVE_FILE) {
            File file = new File("output/");
            file.mkdirs();
            file = new File(String.format("output/seed-%s-sites-%d-lloyds-%d.png",
                SEED, SITES_AMOUNT, LLOYD_RELAXATIONS));
            while (file.exists()) file = new File(incrementFileName(file.getPath()));
            ImageIO.write(img, "PNG", file);
        }
    }

    /**
     * If you have equal filenames - use this method to change filename before creating it.
     *
     * @param oldName fileName_index1.format(fileName.format)
     * @return fileName_index2.format(fileName_1.format)
     */
    private static String incrementFileName(String oldName) {
        String newName;
        int i = oldName.lastIndexOf('.');
        Matcher m = Pattern.compile("\\((\\d+)\\).").matcher(oldName);
        if (m.find()) {
            String n = String.valueOf(Integer.valueOf(m.group(1)) + 1);
            newName = oldName.substring(0, m.start()) + "(" + n + ")" + oldName.substring(i);
        } else {
            newName = oldName.substring(0, i) + "(1)" + oldName.substring(i);
        }
        return newName;
    }

    /**
     * Creates frame with drawn graph.
     *
     * @param img Voronoi graph image.
     */
    private static void showGraph(BufferedImage img) {
        ExampleFrame frame = new ExampleFrame(img);
        frame.setVisible(true);
    }
}
