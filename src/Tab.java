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
import java.util.ArrayList;
public class Tab {
    private BufferedImage image;
    private Deque<BufferedImage> undoStack = new ArrayDeque<>();
    private Deque<BufferedImage> redoStack = new ArrayDeque<>();
    private Rectangle selectionBounds;
    private BufferedImage selectionImage;

    public Tab(int width, int height) {
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.dispose();
    }

    public BufferedImage getImage() { return image; }
    public void setImage(BufferedImage img) { image = img; }
    public Deque<BufferedImage> getUndoStack() { return undoStack; }
    public Deque<BufferedImage> getRedoStack() { return redoStack; }
    public Rectangle getSelectionBounds() { return selectionBounds; }
    public void setSelectionBounds(Rectangle r) { selectionBounds = r; }
    public BufferedImage getSelectionImage() { return selectionImage; }
    public void setSelectionImage(BufferedImage img) { selectionImage = img; }
}