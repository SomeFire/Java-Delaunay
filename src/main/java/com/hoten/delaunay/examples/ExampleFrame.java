package com.hoten.delaunay.examples;

import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

import static com.hoten.delaunay.examples.Variables.*;

/**
 * Frame with graph image. Image can be scaled and moved.
 */
class ExampleFrame extends JFrame {
    /** Graph image. */
    private final BufferedImage img;

    /** Zoom modifier bounds. */
    private final float zoomMin, zoomMax;

    /** Zoom modifier to scale image. */
    private float zoomModifier = 1;

    /** Zoom level. */
    private int zoom = 1;

    /** Previous cursor coordinate. */
    private int oldX = -1, oldY = -1;

    /** Current image position. */
    private int drawX = 0, drawY = 0;

    /**
     * @param img Graph image.
     */
    ExampleFrame(BufferedImage img) {
        this.img = img;

        setTitle(FRAME_TITLE);
        setSize(FRAME_BOUNDS, FRAME_BOUNDS);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);

        zoomMin = Math.max(0.1f, Math.max((float)FRAME_BOUNDS / img.getWidth(), (float)FRAME_BOUNDS / img.getHeight()));
        zoomMax = 5;

        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                oldX = e.getX();
                oldY = e.getY();
            }
            @Override public void mouseReleased(MouseEvent e) {
                oldX = -1;
                oldY = -1;
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                if (oldX != -1) {
                    int dx = e.getX() - oldX;
                    int dy = e.getY() - oldY;

                    drawX = Math.min(0, Math.max(FRAME_BOUNDS - (int)(GRAPH_BOUNDS * zoomModifier), drawX + dx));
                    drawY = Math.min((int)(GRAPH_BOUNDS * zoomModifier) - FRAME_BOUNDS, Math.max(0, drawY - dy));

                    oldX = e.getX();
                    oldY = e.getY();

                    repaint();
                }
            }
        });

        addMouseWheelListener(new MouseAdapter() {
            @Override public void mouseWheelMoved(MouseWheelEvent e) {
                if (zoomModifier <= zoomMin && e.getWheelRotation() < 0 ||
                    zoomModifier >= zoomMax && e.getWheelRotation() > 0)
                    return;

                zoom = Math.min(10, Math.max(-10, zoom + e.getWheelRotation()));

                if (zoom == 0)
                    zoom += e.getWheelRotation();

                float oldW = (float)FRAME_BOUNDS / zoomModifier;
                float oldH = (float)FRAME_BOUNDS / zoomModifier;
                /*int oldX = drawX;
                int oldY = drawY;
                float oldCX = -drawX+oldW/2;
                float oldCY = drawY+oldH/2;*/

                zoomModifier = zoom > 0 ? zoom : 1 + ((float) zoom / 10);

                if (zoomModifier < zoomMin || zoomModifier > zoomMax) {
                    zoomModifier = zoom > 0 ? zoomMax : zoomMin;
                }

                float newW = (float)FRAME_BOUNDS / zoomModifier;
                float newH = (float)FRAME_BOUNDS / zoomModifier;
                drawX = drawX + (int)((newW - oldW) / 2);
                drawY = drawY - (int)((newH - oldH) / 2);
                /*float newCX = -drawX+newW/2;
                float newCY = drawY+newH/2;*/

                drawX = Math.min(0, Math.max(FRAME_BOUNDS - (int)(GRAPH_BOUNDS * zoomModifier), drawX));
                drawY = Math.min((int)(GRAPH_BOUNDS * zoomModifier) - FRAME_BOUNDS, Math.max(0, drawY));
                //TODO Make center after zoom action same as before zoom.
                //System.out.println("zoom="+zoom+", zMod="+zoomModifier+", oldX="+oldX+", oldY="+oldY+", drawX="+drawX+", drawY="+drawY+", width="+FRAME_BOUNDS / zoomModifier+", height="+FRAME_BOUNDS / zoomModifier+", oldCX="+oldCX+", oldCY="+oldCY+", newCX="+newCX+", newCY="+newCY);

                repaint();
            }
        });
    }

    /** {@inheritDoc} */
    @Override public void paint(Graphics g) {
        g.drawImage(
            img,
            getInsets().left + drawX,
            getInsets().top - drawY,
            (int)(GRAPH_BOUNDS * zoomModifier),
            (int)(GRAPH_BOUNDS * zoomModifier),
            null);
    }
}
