import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.imageio.*;
import java.io.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.Currency;
import javax.swing.filechooser.FileNameExtensionFilter;
public class PaintView extends JFrame {
    private ImagePanel imagePanel;
    private PaintModel model;


    /**
     * Constructs the main application window for NHpaint with the specified model.
     * <p>
     * This constructor initializes the JFrame, sets up the image panel within a scroll pane,
     * and configures default window settings such as size, location, and close operation.
     * The window is made visible upon construction.
     * </p>
     *
     * @param m the {@link PaintModel} that the view will observe and interact with
     */
    public PaintView(PaintModel m) {
        super("NHpaint");
        JTabbedPane tabbedPane = new JTabbedPane();
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.model = m;
        imagePanel = new ImagePanel(model);
        JScrollPane scrollPane = new JScrollPane(imagePanel);
        add(scrollPane, BorderLayout.CENTER);

        setSize(800, 600);
        setLocationRelativeTo(null);
        setVisible(true);
    }
    public ImagePanel getImagePanel() {
        return imagePanel;
    }
    public void setImagePanel(ImagePanel p){
        imagePanel=p;
    }


}
