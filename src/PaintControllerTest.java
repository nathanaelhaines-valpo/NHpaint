

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;  // ✅ For assertEquals, assertTrue, etc.
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.awt.image.BufferedImage;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.util.Deque;
import java.util.ArrayDeque;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;



import java.io.File;


class PaintControllerTest {

    @Test
    void testGetFileExtension() {
        PaintController controller = new PaintController();

        assertEquals("png", controller.getFileExtension(new File("pic.png")));
        assertEquals("jpg", controller.getFileExtension(new File("photo.jpg")));
        assertEquals("", controller.getFileExtension(new File("noextension")));
    }


    private ImagePanel panel;

    @BeforeEach
    void setUp() {
        // Create a small 10x10 image for testing
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        // Fill it with red
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 10; y++) {
                img.setRGB(x, y, Color.RED.getRGB());
            }
        }

        PaintModel model = new PaintModel();
        panel = new ImagePanel(model);
        panel.setImage(img);
    }

    @Test
    void testGetSubimage() {
        PaintController controller = new PaintController();
        // Get a 5x5 subimage from top-left corner
        BufferedImage sub = panel.getSubimage(0, 0, 5, 5);

        assertNotNull(sub, "Subimage should not be null");
        assertEquals(5, sub.getWidth(), "Subimage width should be 5");
        assertEquals(5, sub.getHeight(), "Subimage height should be 5");

        // Check pixel color
        for (int x = 0; x < sub.getWidth(); x++) {
            for (int y = 0; y < sub.getHeight(); y++) {
                assertEquals(Color.RED.getRGB(), sub.getRGB(x, y), "Pixel color should be red");
            }
        }
    }


    @Test
    void testZoomIn() {
        PaintController controller = new PaintController();
        double oldZoom = panel.getZoom();

        controller.getView().setImagePanel(panel);
        controller.zoomIn();

        double newZoom = panel.getZoom();
        assertTrue(newZoom > oldZoom, "Zoom should increase after zoomIn()");
        assertEquals(oldZoom * 1.25, newZoom, 0.0001, "Zoom should increase by factor of 1.25");
    }
}

