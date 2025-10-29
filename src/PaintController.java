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
import javax.swing.Timer;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.geom.AffineTransform;


public class PaintController {
    //logger class for threading
    ActionLogger logger = new ActionLogger(new File("actions_log.txt"));

    //fundementals
    private PaintModel model;
    private PaintView view;
    //tools
    private Tool currentTool = Tool.LINE;
    private float brushWidth = 3.0f;
    private Color brushColor = Color.RED;
    private JLabel statusLabel = new JLabel();
    private int penLastX=-1,penLastY=-1;
    //undo redo
    private final Deque<BufferedImage> undoStack = new ArrayDeque<>();
    private final Deque<BufferedImage> redoStack = new ArrayDeque<>();
    private final int UNDO_LIMIT = 50; // keep memory bounded
    //preview coords
    private int previewStartX = -1, previewStartY = -1;
    private int previewEndX = -1, previewEndY = -1;
    //polygon sides
    private int polygonSides;
    //clipboard saves
    private BufferedImage clipboardImage;
    private boolean isPasting = false;
    private int pasteX, pasteY;
    // tabs
    private int currentTabIndex = -1; // no tab selected initially
    private Timer autoSaveTimer;

    /**
     * Initializes and starts an automatic save timer that periodically saves the current image
     * to the associated file every 5 minutes.
     * <p>
     * The auto-save operation runs only if both an image and a valid file reference exist
     * in the {@code model}. If no image or file is present, the save attempt is skipped.
     * </p>
     * <p>
     * When an auto-save occurs, the image is written to disk using its current file extension.
     * If the extension cannot be determined, the image defaults to being saved as a PNG file.
     * The method also logs each auto-save attempt and updates the model's dirty state accordingly.
     * </p>
     *
     * @see javax.swing.Timer
     * @see javax.imageio.ImageIO
     */
    private void setupAutoSaveTimer() {
        int interval = 5 * 60 * 1000; // 5 minutes in milliseconds

        autoSaveTimer = new Timer(interval, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Only save if there’s an image AND an existing file
                if (model.getImage() != null && model.getCurrentFile() != null) {
                    try {
                        String ext = getFileExtension(model.getCurrentFile());
                        if (ext.isEmpty()) ext = "png"; // default to PNG
                        javax.imageio.ImageIO.write(model.getImage(), ext, model.getCurrentFile());
                        model.setDirty(false);
                        System.out.println("Auto-saved at: " + java.time.LocalTime.now());logger.log(model.getCurrentFileName(), "auto saved");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        System.out.println("Auto-save failed.");
                        logger.log(model.getCurrentFileName(), "auto save failed");
                    }
                } else {
                    // Optional: log that auto-save skipped
                    System.out.println("Auto-save skipped: no file to save to.");
                    logger.log(model.getCurrentFileName(), "auto save skipped");
                }
            }
        });

        autoSaveTimer.start();
    }





    /**
     * Constructs a new {@code PaintController} instance and initializes the core components
     * of the paint application, including the model, view, and various event handlers.
     * <p>
     * This constructor sets up the user interface, event listeners, and drawing functionality,
     * creates a blank canvas with a default size of 800x600 pixels, and starts the auto-save timer.
     * </p>
     *
     * @see #setupMenu()
     * @see #setupWindowListener()
     * @see #setupMouseDrawing()
     * @see #createBlankImage(int, int)
     * @see #setupAutoSaveTimer()
     */
    public PaintController() {
        model = new PaintModel();
        view = new PaintView(model);

        setupMenu();
        setupWindowListener();
        setupMouseDrawing();

        createBlankImage(800,600);
        setupAutoSaveTimer();

    }
    public PaintView getView(){
        return view;
    }

    /**
     * Sets up the application's main menu bar, including all menu categories, items, and shortcuts.
     * <p>
     * This method constructs and configures the following menus:
     * </p>
     * <ul>
     *   <li><b>File</b> — Handles file operations such as open, save, resize, and exit.</li>
     *   <li><b>View</b> — Provides zoom in and zoom out controls.</li>
     *   <li><b>Edit</b> — Contains undo/redo actions and brush settings.</li>
     *   <li><b>Tools</b> — Offers various drawing tools such as pen, line, shape, color picker, and text tools.</li>
     *   <li><b>Help</b> — Displays application information.</li>
     * </ul>
     * <p>
     * In addition to creating the menus, this method assigns icons and tooltips, connects event listeners to
     * corresponding actions, and registers global keyboard shortcuts (e.g., Ctrl+S for save, Ctrl+C/V for copy/paste,
     * and rotation/mirroring hotkeys).
     * </p>
     * <p>
     * The menu bar also includes a dynamic status label that updates based on the current tool or action.
     * </p>
     *
     * @see #openFile()
     * @see #saveFile()
     * @see #saveFileAs()
     * @see #resizeImageDialog()
     * @see #exitApp()
     * @see #zoomIn()
     * @see #zoomOut()
     * @see #undo()
     * @see #redo()
     * @see #setBrushWidth()
     * @see #setBrushColor()
     * @see #mirrorCanvasOrSelection()
     * @see javax.swing.JMenuBar
     * @see javax.swing.JMenu
     * @see javax.swing.JMenuItem
     */

    private void setupMenu() {
        JMenuBar menuBar = new JMenuBar();
        //============File Menu============ I like these equal signs they make things fancey
        JMenu fileMenu = new JMenu("File \uD83D\uDDC4\uFE0F");
        JMenuItem openItem = new JMenuItem("Open \uD83D\uDCC2");
        JMenuItem saveItem = new JMenuItem("Save \uD83D\uDCBE");
        JMenuItem saveItemAs = new JMenuItem("Save as \uD83D\uDCBD");
        JMenuItem resizeItem = new JMenuItem(("Resize \uD83D\uDCCF"));
        JMenuItem exitItem = new JMenuItem("Exit ⏏\uFE0F");

        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.add(saveItemAs);
        fileMenu.addSeparator();
        fileMenu.add(resizeItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        menuBar.add(fileMenu);
        //=========View Menu=========

        JMenu viewMenu = new JMenu("View \uD83D\uDC41\uFE0F");
        JMenuItem zoomInItem = new JMenuItem("Zoom In\uD83D\uDD0D");
        JMenuItem zoomOutItem = new JMenuItem("Zoom Out \uD83D\uDD0E");

        viewMenu.add(zoomInItem);
        viewMenu.add(zoomOutItem);
        menuBar.add(viewMenu);

        //=========Edit Menu==========
        JMenu editMenu = new JMenu("Edit ");
        JMenuItem undoItem = new JMenuItem("Undo");
        JMenuItem redoItem = new JMenuItem("Redo");
        JMenuItem brushWidthItem = new JMenuItem("Set Brush Width...");
        JMenuItem colorItem = new JMenuItem("Set Brush Color...");

        editMenu.add(undoItem);
        editMenu.add(redoItem);
        editMenu.addSeparator();
        editMenu.add(brushWidthItem);
        editMenu.add(colorItem);
        menuBar.add(editMenu);

        //============Help Menu=============
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("about");

        helpMenu.add(aboutItem);
        menuBar.add(helpMenu);

        view.setJMenuBar(menuBar);
        //==========Tools Menu==========
        JMenu toolMenu = new JMenu("Tools \uD83E\uDDF0");

        JMenuItem lineTool = new JMenuItem("Line \uDB40\uDC7C");
        lineTool.setToolTipText("Draw straight lines on the canvas");
        JMenuItem penTool = new JMenuItem("Pen \uD83D\uDD8A\uFE0F");
        penTool.setToolTipText("Drag to free draw on canvas");
        JMenuItem dottedTool = new JMenuItem("Dotted");
        dottedTool.setToolTipText("Lke the Line tool but with dashes");
        JMenuItem rectangleTool = new JMenuItem("Rectangle ☐");
        rectangleTool.setToolTipText("Makes a 4 sided polygon with 2a 2b side propotions");
        JMenuItem ellipseTool = new JMenuItem("Ellipse ⭕");
        ellipseTool.setToolTipText("bean shaped shape");
        JMenuItem triangleTool = new JMenuItem("Triangle \uD83D\uDD3A");
        triangleTool.setToolTipText("Isn't everything a triangle if you think about it?");
        JMenuItem polygonTool = new JMenuItem("Polygon \uD83D\uDCA0");
        polygonTool.setToolTipText("Allows you to select how many sides your uniform polygon will have. Click to decide cencer");
        JMenuItem starTool = new JMenuItem("Star");
        starTool.setToolTipText("Lets you pick how many points you want your star to have");
        JMenuItem colorPickerTool = new JMenuItem("Color Dropper \uD83E\uDE78");
        colorPickerTool.setToolTipText("Click on part of the picture and the rest of your tools will use that color");
        JMenuItem selectionTool = new JMenuItem("Selection ☝\uFE0F");
        selectionTool.setToolTipText("Drag out a rectangle that highlights the image. Dragging this rectangle will move that image. You can also ctrl c ctrl v the selection and M will flip it");
        JMenuItem textTool = new JMenuItem("Text \uD83D\uDDDB");
        textTool.setToolTipText("allows you to print text to the canves where you click. Brush width is font size");


        toolMenu.add(lineTool);
        toolMenu.add(penTool);
        toolMenu.add(dottedTool);
        toolMenu.add(rectangleTool);
        toolMenu.add(ellipseTool);
        toolMenu.add(triangleTool);
        toolMenu.add(polygonTool);
        toolMenu.add(starTool);
        toolMenu.add(colorPickerTool);
        toolMenu.add(selectionTool);
        toolMenu.add(textTool);

        menuBar.add(toolMenu);
        // ========tabs menu=========

        // ===== Status/Info =====
        updateStatusLabel(); // show initial state
        menuBar.add(Box.createHorizontalGlue()); // pushes the label to the right
        menuBar.add(statusLabel);

        view.setJMenuBar(menuBar);
        // Hook up actions


        openItem.addActionListener(e -> openFile());
        saveItem.addActionListener(e -> saveFile());
        saveItemAs.addActionListener(e -> saveFileAs());
        resizeItem.addActionListener(e -> resizeImageDialog());
        exitItem.addActionListener(e -> exitApp());

        zoomInItem.addActionListener(e -> zoomIn());
        zoomOutItem.addActionListener(e -> zoomOut());

        undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
        redoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK));
        undoItem.addActionListener(e -> undo());
        redoItem.addActionListener(e -> redo());
        brushWidthItem.addActionListener(e -> setBrushWidth());
        colorItem.addActionListener(e -> setBrushColor());

        aboutItem.addActionListener(e -> tellAbout());

        lineTool.addActionListener(e -> {
            logger.log(model.getCurrentFileName(), "Line tool selected");
            currentTool = Tool.LINE;
            updateStatusLabel();
        });
        penTool.addActionListener(e -> {
            logger.log(model.getCurrentFileName(), "Pen tool selected");
            currentTool = Tool.PEN;
            updateStatusLabel();
        });
        dottedTool.addActionListener(e -> {
            logger.log(model.getCurrentFileName(), "Dotted Line tool selected");
            currentTool = Tool.DOTTED;
            updateStatusLabel();
        });
        rectangleTool.addActionListener(e -> {
            logger.log(model.getCurrentFileName(), "Rectangle tool selected");
            currentTool = Tool.RECTANGLE;
            updateStatusLabel();
        });
        ellipseTool.addActionListener(e -> {
            logger.log(model.getCurrentFileName(), "Ellipse tool selected");
            currentTool = Tool.ELLIPSE;
            updateStatusLabel();
        });
        triangleTool.addActionListener(e -> {
            logger.log(model.getCurrentFileName(), "triangle tool selected");
            currentTool = Tool.TRIANGLE;
            updateStatusLabel();
        });
        polygonTool.addActionListener(e -> {
            logger.log(model.getCurrentFileName(), "Polygon tool selected");
            currentTool = Tool.POLYGON;
            updateStatusLabel();
            String input = JOptionPane.showInputDialog(view, "Enter number of sides:", polygonSides);
            if (input != null) {
                try {
                    int sides = Integer.parseInt(input);
                    if (sides >= 3) { // minimum polygon is a triangle
                        polygonSides = sides;
                    } else {
                        JOptionPane.showMessageDialog(view, "Polygon must have at least 3 sides.");
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(view, "Invalid number.");
                }
            }

        });


        starTool.addActionListener(e -> {
            logger.log(model.getCurrentFileName(), "Star tool selected");
            currentTool = Tool.STAR;
            updateStatusLabel();
            String input = JOptionPane.showInputDialog(view, "Enter number of sides:", polygonSides);
            if (input != null) {
                try {
                    int sides = Integer.parseInt(input);
                    if (sides >= 3) { // minimum polygon is a triangle
                        polygonSides = sides;
                    } else {
                        JOptionPane.showMessageDialog(view, "Star must have at least 3 sides.");
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(view, "Invalid number.");
                }
            }

        });

        colorPickerTool.addActionListener(e -> {
            logger.log(model.getCurrentFileName(), "Color picked tool selected");
            currentTool = Tool.COLOR_PICKER;
            updateStatusLabel();
        });
        selectionTool.addActionListener(e-> {
            logger.log(model.getCurrentFileName(), "selection tool selected");
            currentTool = Tool.SELECTION;
            updateStatusLabel();
        });
        textTool.addActionListener(e -> {
            logger.log(model.getCurrentFileName(), "text tool selected");
            currentTool = Tool.TEXT;
            updateStatusLabel();
        });

        view.setJMenuBar(menuBar);
        //===========Short cuts===========
        InputMap im = view.getImagePanel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = view.getImagePanel().getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK), "copy");
        am.put("copy", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { copySelection(); }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK), "Paste");
        am.put("Paste", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { clickedCtrlV(); }
        });

        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        saveItem.addActionListener(e -> saveFile());
        // ==== Rotation Shortcuts ====
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_K, 0), "rotateLeft");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, 0), "rotateRight");

        am.put("rotateLeft", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {

                rotateCurrent(-90); // rotate 10 degrees left
                logger.log(model.getCurrentFileName(), "rotated left");
            }
        });
        am.put("rotateRight", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                rotateCurrent(90); // rotate 10 degrees right
                logger.log(model.getCurrentFileName(), "rotated right");
            }
        });

        ImagePanel panel = view.getImagePanel();
        panel.setFocusable(true);
        panel.requestFocusInWindow();
        panel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_M) {
                    mirrorCanvasOrSelection();  // your mirror method
                }
            }
        });



    }



    //==========File Methods==========

    /**
     * Opens an image file selected by the user through a file chooser dialog and loads it
     * into the application.
     * <p>
     * This method allows the user to browse for image files with supported extensions
     * (PNG, JPG, JPEG, BMP). When a valid file is chosen, it reads the image and updates
     * both the model and view accordingly. If the image cannot be opened or is unsupported,
     * an error dialog is shown.
     * </p>
     *
     * <p><b>Behavior:</b></p>
     * <ul>
     *   <li>Prompts the user with a file chooser dialog.</li>
     *   <li>Validates and loads the selected image into the canvas.</li>
     *   <li>Updates the current file reference in the model.</li>
     *   <li>Logs the action and marks the model as clean (not dirty).</li>
     *   <li>Displays error messages for unsupported or failed image loads.</li>
     * </ul>
     *
     * @see javax.swing.JFileChooser
     * @see javax.swing.filechooser.FileNameExtensionFilter
     * @see javax.imageio.ImageIO
     * @see PaintModel#setImage(BufferedImage)
     * @see PaintView#getImagePanel()
     */
    private void openFile() {
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "Image files (PNG, JPG, BMP)", "png", "jpg", "jpeg", "bmp");
        chooser.setFileFilter(filter);

        int option = chooser.showOpenDialog(view); // use the view as parent
        if (option == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                BufferedImage newImage = javax.imageio.ImageIO.read(file);
                if (newImage != null) {
                    // Update the model
                    model.setImage(newImage);
                    model.setCurrentFile(file);
                    model.setDirty(false);

                    // Update the view
                    view.getImagePanel().setImage(newImage);
                    logger.log(model.getCurrentFileName(), "Opened a file");
                } else {
                    JOptionPane.showMessageDialog(view,
                            "That file isn’t a supported image.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(view,
                        "Failed to open image.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Prompts the user to choose a location and file name to save the current image under
     * a new file.
     * <p>
     * This method is used when the user selects <i>"Save As"</i> from the menu. It opens a
     * file chooser dialog allowing the user to specify a destination and file format.
     * If there is no image currently loaded in the model, a warning dialog is shown and
     * the operation is aborted.
     * </p>
     *
     * <p><b>Behavior:</b></p>
     * <ul>
     *   <li>Checks whether an image exists in the model before proceeding.</li>
     *   <li>Displays a warning message if no image is available to save.</li>
     *   <li>Otherwise, opens a file chooser dialog for selecting the new save location.</li>
     * </ul>
     *
     * @see javax.swing.JFileChooser
     * @see javax.imageio.ImageIO
     * @see JOptionPane
     */

    private void saveFileAs() {
        if (model.getImage() == null) {
            JOptionPane.showMessageDialog(view,
                    "No image to save.",
                    "Warning",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "Image files (PNG, JPG, BMP)", "png", "jpg", "jpeg", "bmp");
        chooser.setFileFilter(filter);

        int option = chooser.showSaveDialog(view); // view is parent
        if (option == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            String ext = getFileExtension(file);

            // If extension is missing or unsupported, default to png
            if (!ext.equals("png") && !ext.equals("jpg") && !ext.equals("jpeg") && !ext.equals("bmp")) {
                file = new File(file.getAbsolutePath() + ".png");
                ext = "png";
            }

            try {
                javax.imageio.ImageIO.write(model.getImage(), ext, file);
                model.setCurrentFile(file);
                model.setDirty(false);
                JOptionPane.showMessageDialog(view, "Image saved successfully!");
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(view,
                        "Failed to save image.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
            logger.log(model.getCurrentFileName(), "Saved file as");
        }
    }

    /**
     * Saves the current image from the model to a file.
     * <p>
     * This method performs the following steps:
     * <ul>
     *   <li>Checks if there is an image in the model; if not, displays a warning and returns.</li>
     *   <li>If there is no existing file associated with the image, delegates to {@link #saveFileAs()} to prompt the user for a file location.</li>
     *   <li>If a file exists, determines the file extension (defaulting to PNG if missing) and attempts to write the image to the file.</li>
     *   <li>Updates the model's dirty flag and logs the save action.</li>
     *   <li>Displays a success or error message depending on the outcome of the save operation.</li>
     * </ul>
     *
     * @see #saveFileAs()
     */

    private void saveFile() {
        if (model.getImage() == null) {
            JOptionPane.showMessageDialog(view,
                    "No image to save.",
                    "Warning",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (model.getCurrentFile() == null) {
            // No existing file, fallback to Save As
            saveFileAs();
            return;
        }

        // Save to existing file
        String ext = getFileExtension(model.getCurrentFile());
        if (ext.isEmpty()) ext = "png"; // default to PNG

        try {
            javax.imageio.ImageIO.write(model.getImage(), ext, model.getCurrentFile());
            model.setDirty(false);
            JOptionPane.showMessageDialog(view, "Image saved successfully!");
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(view,
                    "Failed to save image.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
        logger.log(model.getCurrentFileName(), "saved a file");
    }

    /**
     * Exits the application, handling unsaved changes appropriately.
     * <p>
     * If the current model has unsaved changes, the user is prompted with a dialog
     * asking whether to save before exiting. The behavior is as follows:
     * <ul>
     *   <li>{@link JOptionPane#YES_OPTION}: Attempts to save the current file by calling {@link #saveFile()}.
     *       Exits only if the save succeeds (i.e., the model is no longer dirty).</li>
     *   <li>{@link JOptionPane#NO_OPTION}: Discards unsaved changes and exits immediately.</li>
     *   <li>{@link JOptionPane#CANCEL_OPTION}: Cancels the exit; the application remains open.</li>
     * </ul>
     * If there are no unsaved changes, the application exits immediately.
     * <p>
     * In all exit scenarios, the action is logged via the logger and the logger is stopped before terminating the program.
     *
     * @see #saveFile()
     */
    private void exitApp() {
        if (model.isDirty()) {
            int option = JOptionPane.showConfirmDialog(view,
                    "You have unsaved changes. Save before exiting?",
                    "Unsaved Changes",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (option == JOptionPane.YES_OPTION) {
                saveFile(); // tries to save (calls saveAsFile if needed)
                // Only exit if save succeeded
                if (!model.isDirty()) {
                    logger.log(model.getCurrentFileName(), "closed app");
                    logger.stop();
                    System.exit(0);
                }
            } else if (option == JOptionPane.NO_OPTION) {
                logger.log(model.getCurrentFileName(), "Closed app without saving");
                logger.stop();
                System.exit(0); // discard changes
            }
            // CANCEL_OPTION → do nothing
        } else {
            logger.log(model.getCurrentFileName(), "Closed app");
            logger.stop();
            System.exit(0);
        }
    }

    /**
     * Returns the file extension of the given file.
     * <p>
     * The extension is defined as the substring after the last dot ('.') in the file name.
     * The returned extension is converted to lowercase. If the file name has no extension,
     * an empty string is returned.
     *
     * @param file the file whose extension is to be retrieved
     * @return the lowercase file extension, or an empty string if none exists
     */
    public String getFileExtension(File file) {
        String name = file.getName();
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < name.length() - 1) {
            return name.substring(dotIndex + 1).toLowerCase();
        }
        return "";
    }

    /**
     * Links the closing on the windowed X to the exitApp method
     */
    private void setupWindowListener() {
        view.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitApp();
            }
        });
    }

    /**
     * Prompts the user to resize the current image via input dialogs.
     * <p>
     * This method performs the following steps:
     * <ul>
     *   <li>Logs the resize action using the current file name.</li>
     *   <li>If there is no image in the model, the method returns immediately.</li>
     *   <li>Prompts the user to enter new width and height values.</li>
     *   <li>If valid numeric values are entered, resizes the image using {@link #resizeImage(BufferedImage, int, int)}, updates the model, and refreshes the view.</li>
     *   <li>If the user enters invalid numbers, displays an error message.</li>
     * </ul>
     */
    private void resizeImageDialog() {
        logger.log(model.getCurrentFileName(), "resized image");
        BufferedImage img = model.getImage();
        if (img == null) return; // nothing to resize

        String widthStr = JOptionPane.showInputDialog(view,
                "Enter new width:", img.getWidth());
        String heightStr = JOptionPane.showInputDialog(view,
                "Enter new height:", img.getHeight());

        if (widthStr != null && heightStr != null) {
            try {
                int newWidth = Integer.parseInt(widthStr);
                int newHeight = Integer.parseInt(heightStr);

                BufferedImage resized = resizeImage(img, newWidth, newHeight);
                model.setImage(resized); // update model
                view.getImagePanel().setImage(resized); // update view
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(view,
                        "Invalid size entered.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * resizes an image
     * @param original the image that will be resized
     * @param newWidth the desired width
     * @param newHeight the desired hight
     * @return returns a new resized bufferedimage
     */
    private BufferedImage resizeImage(BufferedImage original, int newWidth, int newHeight) {
        BufferedImage resized = new BufferedImage(newWidth, newHeight, original.getType());
        Graphics2D g2d = resized.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.drawImage(original, 0, 0, newWidth, newHeight, null);
        g2d.dispose();
        return resized;
    }


    //=============View Methods==============

    /**
     * zooms into to panel
     */
    public void zoomIn() {
        ImagePanel panel = view.getImagePanel();
        panel.setZoom(panel.getZoom() * 1.25);
        logger.log(model.getCurrentFileName(), "zoomed in");
    }

    /**
     * zooms out of panel
     */
    private void zoomOut() {
        ImagePanel panel = view.getImagePanel();
        panel.setZoom(panel.getZoom() / 1.25);
        logger.log(model.getCurrentFileName(), "zoomed out");
    }
//===========Edit Methods================

    /**
     * opens diologe with user and changes brush width
     */
    private void setBrushWidth() {
        logger.log(model.getCurrentFileName(), "set brush width");
        String input = JOptionPane.showInputDialog(view,
                "Enter brush width (pixels):",
                3);
        if (input != null) {
            try {
                float width = Float.parseFloat(input);
                if (width > 0){
                    brushWidth = width;
                    updateStatusLabel();
                }
                else JOptionPane.showMessageDialog(view, "Width must be positive.");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(view, "Invalid number entered.");
            }
        }
    }

    /**
     * opens diologe with user and chnages brush color
     */
    private void setBrushColor() {
        logger.log(model.getCurrentFileName(), "set brush color");
        Color chosen = JColorChooser.showDialog(view, "Choose Brush Color", brushColor);
        if (chosen != null) brushColor = chosen;
        updateStatusLabel();
    }

    /**
     * Sets up mouse interactions for drawing and editing on the image panel.
     * <p>
     * This method attaches {@link java.awt.event.MouseListener} and
     * {@link java.awt.event.MouseMotionListener} to the {@link ImagePanel} from the view,
     * enabling support for various drawing tools and image editing actions.
     *
     * <p>
     * Supported interactions include:
     * <ul>
     *   <li>Mouse press: starts drawing operations, places pasted content, begins selection or text input, or picks color depending on the current tool.</li>
     *   <li>Mouse release: finalizes drawing shapes (line, rectangle, ellipse, polygon, star, etc.), completes selections, or commits dragged selections.</li>
     *   <li>Mouse drag: updates previews for shapes, pen strokes, polygons, stars, and selection marquee, and performs freehand drawing with the pen tool.</li>
     * </ul>
     *
     * <p>
     * Specific behaviors per tool:
     * <ul>
     *   <li>{@link Tool#PEN}: freehand drawing with continuous stroke updates.</li>
     *   <li>{@link Tool#DOTTED}: draws individual points or dotted lines.</li>
     *   <li>{@link Tool#LINE, RECTANGLE, ELLIPSE, TRIANGLE, POLYGON, STAR}: shows live preview while dragging and draws final shape on release.</li>
     *   <li>{@link Tool#SELECTION}: supports marquee selection, moving selections with optional cut/copy, and updates selection bounds.</li>
     *   <li>{@link Tool#TEXT}: prompts the user for text input and renders it on the image.</li>
     *   <li>{@link Tool#COLOR_PICKER}: selects a color from the clicked pixel.</li>
     * </ul>
     *
     * <p>
     * The method also handles undo state saving, updates the model’s dirty flag, and repaints the view as needed.
     */
    private void setupMouseDrawing() {
        ImagePanel panel = view.getImagePanel();

        panel.addMouseListener(new MouseAdapter() {
            private int startX, startY;

            @Override
            public void mousePressed(MouseEvent e) {
                saveStateForUndo();
                startX = toImageX(e.getX());
                startY = toImageY(e.getY());
                previewStartX = startX;
                previewStartY = startY;
                model.setPasteX(startX);
                model.setPasteY(startY);
                panel.repaint();

                if (model.isPastePreviewActive()) {
                    pasteSelectionAt(startX, startY);
                    model.setPastePreviewActive(false);
                    view.getImagePanel().repaint();
                }

                switch (currentTool) {
                    case DOTTED:
                        drawPoint(startX, startY);
                        break;
                    case PEN:
                        penLastX = startX;
                        penLastY = startY;
                        break;
                    case COLOR_PICKER:
                        if (startX >= 0 && startX < panel.getImage().getWidth() &&
                                startY >= 0 && startY < panel.getImage().getHeight()) {
                            brushColor = new Color(panel.getImage().getRGB(startX, startY));
                            updateStatusLabel();
                        }
                        break;
                    case SELECTION:
                        Rectangle bounds = model.getSelectionBounds();
                        if (bounds != null && bounds.contains(startX, startY)) {
                            // start dragging existing selection
                            model.setSelectionCut(true);
                            model.setDragOffset(new Point(startX - bounds.x, startY - bounds.y));

                            // if CTRL is NOT down => cut original (so the selection appears to move)
                            if (!e.isControlDown()) {
                                // we already have selectionImage in model; clear original pixels
                                cutSelectionFromImage(bounds);
                                model.setSelectionCut(true);
                            } else {
                                // control+drag => copy (don't clear original)
                                model.setSelectionCut(false);
                            }
                        } else {
                            // start a new marquee selection
                            model.setSelectionBounds(null);
                            model.setSelectionImage(null);
                            model.setSelectionCut(false);

                            // we use preview values for the marquee until mouseReleased
                            panel.setPreview(startX, startY, startX, startY, Tool.SELECTION, 1f, Color.GRAY);
                        }
                        break;
                    case TEXT:

                        String input = JOptionPane.showInputDialog(panel, "Enter text:");
                        if (input != null && !input.trim().isEmpty()) {
                            logger.log(model.getCurrentFileName(), "Text Placed");
                            Graphics2D g2d = model.getImage().createGraphics();
                            g2d.setColor(brushColor);

                            // use brushWidth as font size (your choice of mapping)
                            int fontSize = Math.max(1, (int) brushWidth * 10);
                            g2d.setFont(new Font("Arial", Font.PLAIN, fontSize));

                            // baseline alignment: shift y down by font size
                            g2d.drawString(input, startX, startY + fontSize);

                            g2d.dispose();
                            panel.repaint();
                            model.setDirty(true);
                        }
                        break;
                    default:
                        // for line, rectangle, ellipse, triangle, polygon, star: do nothing here
                        break;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                int endX = toImageX(e.getX());
                int endY = toImageY(e.getY());
                penLastX = endX;
                penLastY = endY;
                panel.clearPreview();

                switch (currentTool) {
                    case LINE:
                        drawLine(startX, startY, endX, endY);
                        break;
                    case DOTTED:
                        drawDottedLine(startX, startY, endX, endY);
                        break;
                    case RECTANGLE:
                        drawRectangle(startX, startY, endX, endY);
                        break;
                    case ELLIPSE:
                        drawEllipse(startX, startY, endX, endY);
                        break;
                    case TRIANGLE:
                        drawTriangle(startX, startY, endX, endY);
                        break;
                    case POLYGON:
                        drawPolygon(startX, startY, endX, endY, polygonSides);
                        break;

                    // ---------- NEW: finalize STAR on mouse release ----------
                    case STAR:
                        // call the concrete drawStar implementation (added below)
                        drawStar(startX, startY, endX, endY, polygonSides);
                        break;
                    // --------------------------------------------------------

                    case SELECTION:
                        if (model.isSelectionCut()) {
                            // Finish dragging: commit selection to the image at current bounds
                            commitSelectionAtCurrentBounds();
                            model.setSelectionCut(false);
                            // selection remains active at the new location
                        } else {
                            // Finalize new marquee selection
                            int x = Math.min(startX, endX);
                            int y = Math.min(startY, endY);
                            int w = Math.abs(endX - startX);
                            int h = Math.abs(endY - startY);
                            if (w > 0 && h > 0 && panel.getImage() != null) {
                                Rectangle rect = new Rectangle(x, y, w, h);
                                model.setSelectionBounds(rect);
                                BufferedImage sel = panel.getSubimage(x, y, w, h);
                                model.setSelectionImage(sel);
                                model.setSelectionCut(false);
                            } else {
                                model.clearSelection();
                            }
                        }
                        panel.clearPreview();
                        panel.repaint();
                        break;
                    default:
                        break;
                }
            }
        });

        // note: you already have a MouseMotionListener below; we add STAR handling there too
        panel.addMouseMotionListener(new MouseMotionAdapter() {

            @Override
            public void mouseDragged(MouseEvent e) {
                int x = toImageX(e.getX());
                int y = toImageY(e.getY());

                switch (currentTool) {
                    case LINE:
                    case RECTANGLE:
                    case ELLIPSE:
                    case TRIANGLE:
                    case DOTTED:
                        panel.setPreview(previewStartX, previewStartY, x, y, currentTool, brushWidth, brushColor);
                        break;
                    case PEN:
                        if (penLastX != -1 && penLastY != -1) {
                            int[] adj = expandCanvasIfNeeded(penLastX, penLastY, x, y);
                            Graphics2D g2d = model.getImage().createGraphics();
                            g2d.setColor(brushColor);
                            g2d.setStroke(new BasicStroke(brushWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                            g2d.drawLine(adj[0], adj[1], adj[2], adj[3]);
                            g2d.dispose();
                            panel.repaint();
                            model.setDirty(true);
                            penLastX = adj[2];
                            penLastY = adj[3];
                        } else {
                            penLastX = x;
                            penLastY = y;
                        }
                        break;
                    case POLYGON:
                        // existing polygon preview
                        panel.setPreview(previewStartX, previewStartY, x, y, Tool.POLYGON, brushWidth, brushColor, polygonSides);
                        break;

                    // ---------- NEW: STAR preview while dragging ----------
                    case STAR:
                        // use same params as polygon but tell panel this is a STAR and pass sides
                        panel.setPreview(previewStartX, previewStartY, x, y, Tool.STAR, brushWidth, brushColor, polygonSides);
                        break;
                    // -------------------------------------------------------

                    case SELECTION:
                        if (model.isSelectionCut()) {
                            // moving selection: update bounds so paintComponent draws selectionImage at new position
                            Rectangle b = model.getSelectionBounds();
                            if (b != null) {
                                Point off = model.getDragOffset();
                                b.x = x - off.x;
                                b.y = y - off.y;
                                // optional: keep selection inside allowed range — or allow free dragging
                                panel.repaint();
                            }
                        } else {
                            // marquee preview
                            panel.setPreview(previewStartX, previewStartY, x, y, Tool.SELECTION, 1f, Color.GRAY);
                        }
                        break;
                    default:
                        break;
                }
            }
        });
    }



    // helper methods

    /**
     * draws a single dot on the panel
     * @param x the x location on the panel of the dot
     * @param y the y location on the panel of the dot
     */
    private void drawPoint(int x, int y) {
        logger.log(model.getCurrentFileName(), "drew point");
        int[] coords = expandCanvasIfNeeded(x,y,x,y);
        x = coords[0];
        y = coords[1];
        Graphics2D g = model.getImage().createGraphics();
        g.setColor(brushColor);
        g.setStroke(new BasicStroke(brushWidth));
        g.drawLine(x, y, x, y);
        g.dispose();
        view.getImagePanel().repaint();
        model.setDirty(true);
    }

    /**
     * draws a full stright line across the panel
     * @param x1 x location of the first end of the line
     * @param y1 etc
     * @param x2 etc
     * @param y2 etc
     */
    private void drawLine(int x1, int y1, int x2, int y2) {
        logger.log(model.getCurrentFileName(), "drew line");
        int[] coords = expandCanvasIfNeeded(x1,y1,x2,y2);
        x1 = coords[0];
        y1 = coords[1];
        x2 = coords[2];
        y2 = coords[3];
        view.getImagePanel().repaint();
        Graphics2D g = model.getImage().createGraphics();
        g.setColor(brushColor);
        g.setStroke(new BasicStroke(brushWidth));
        g.drawLine(x1, y1, x2, y2);
        g.dispose();
        view.getImagePanel().repaint();
        model.setDirty(true);
    }

    /**
     * draws a dotted line but acts just like drawLine otherwise
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     */
    private void drawDottedLine(int x1, int y1, int x2, int y2) {
        logger.log(model.getCurrentFileName(), "Drew Dotted line");
        int[] coords = expandCanvasIfNeeded(x1,y1,x2,y2);
        x1 = coords[0];
        y1 = coords[1];
        x2 = coords[2];
        y2 = coords[3];
        if (model.getImage() != null) {
            Graphics2D g2d = model.getImage().createGraphics();
            g2d.setColor(brushColor);

            // dash pattern: 10px on, 10px off (you can tweak these numbers)
            float[] dashPattern = { 10f, 10f };
            g2d.setStroke(new BasicStroke(
                    brushWidth,                  // line width
                    BasicStroke.CAP_BUTT,        // flat line caps
                    BasicStroke.JOIN_MITER,      // joins
                    10f,                         // miter limit
                    dashPattern,                 // dash pattern
                    0f                           // dash phase
            ));

            g2d.drawLine(x1, y1, x2, y2);
            g2d.dispose();
            view.getImagePanel().repaint();
            model.setDirty(true);
        }
    }

    /**
     * converts coordinates from the panel (where the mouse click happens) to coordinates on the actual image.
     * @param panelX
     * @return
     */
    private int toImageX(int panelX) {
        ImagePanel imgPanel = view.getImagePanel();
        if (imgPanel.getImage() == null) return panelX; // fallback

        int imgWidth = imgPanel.getImage().getWidth();
        int displayWidth = (int)(imgWidth * imgPanel.getZoom());
        int offsetX = (imgPanel.getWidth() - displayWidth) / 2;

        return (int)((panelX - offsetX) / imgPanel.getZoom());
    }

    private int toImageY(int panelY) {
        ImagePanel imgPanel = view.getImagePanel();
        if (imgPanel.getImage() == null) return panelY;

        int imgHeight = imgPanel.getImage().getHeight();
        int displayHeight = (int)(imgHeight * imgPanel.getZoom());
        int offsetY = (imgPanel.getHeight() - displayHeight) / 2;

        return (int)((panelY - offsetY) / imgPanel.getZoom());
    }

    /**
     * Draws a rectangle on the current image using the specified corner coordinates.
     * <p>
     * The rectangle is drawn by connecting four lines between the calculated top-left,
     * top-right, bottom-right, and bottom-left corners. If necessary, the canvas is
     * expanded to accommodate the rectangle.
     * <p>
     * The action is logged using the current file name.
     *
     * @param x1 the x-coordinate of the first corner
     * @param y1 the y-coordinate of the first corner
     * @param x2 the x-coordinate of the opposite corner
     * @param y2 the y-coordinate of the opposite corner
     */
    private void drawRectangle(int x1,int y1,int x2,int y2){
        logger.log(model.getCurrentFileName(), "Drew Rectangle");
        int[] coords = expandCanvasIfNeeded(x1,y1,x2,y2);
        x1 = coords[0];
        y1 = coords[1];
        x2 = coords[2];
        y2 = coords[3];
        int topLeftX = Math.min(x1, x2);
        int topLeftY = Math.min(y1, y2);

        int bottomRightX = Math.max(x1, x2);
        int bottomRightY = Math.max(y1, y2);
// Other two corners
        int topRightX = bottomRightX;
        int topRightY = topLeftY;

        int bottomLeftX = topLeftX;
        int bottomLeftY = bottomRightY;

        drawLine(topLeftX, topLeftY, topRightX, topRightY);     // top edge
        drawLine(topRightX, topRightY, bottomRightX, bottomRightY); // right edge
        drawLine(bottomRightX, bottomRightY, bottomLeftX, bottomLeftY); // bottom edge
        drawLine(bottomLeftX, bottomLeftY, topLeftX, topLeftY);   // left edge

    }
    /**
     * Draws an ellipse on the current image defined by the specified start and end coordinates.
     * <p>
     * The ellipse is drawn within the bounding rectangle determined by the two corner points.
     * If necessary, the canvas is expanded to fit the ellipse. The ellipse is drawn using the
     * current brush color and brush width.
     * <p>
     * After drawing, the image panel is repainted and the model's dirty flag is set to true.
     * The action is also logged using the current file name.
     *
     * @param startX the x-coordinate of one corner of the bounding rectangle
     * @param startY the y-coordinate of one corner of the bounding rectangle
     * @param lastX the x-coordinate of the opposite corner of the bounding rectangle
     * @param lastY the y-coordinate of the opposite corner of the bounding rectangle
     */
    private void drawEllipse(int startX,int startY,int lastX,int lastY){
        logger.log(model.getCurrentFileName(), "drew Ellipse");
        int[] coords = expandCanvasIfNeeded(startX,startY,lastX,lastY);
        startX = coords[0];
        startY = coords[1];
        lastX = coords[2];
        lastY = coords[3];
        int x = Math.min(startX, lastX);   // top-left corner X
        int y = Math.min(startY, lastY);   // top-left corner Y
        int width = Math.abs(lastX - startX);  // width of the bounding box
        int height = Math.abs(lastY - startY); // height of the bounding box

        Graphics2D g2d = model.getImage().createGraphics();
        g2d.setColor(brushColor);
        g2d.setStroke(new BasicStroke(brushWidth));
        g2d.drawOval(x, y, width, height);
        g2d.dispose();

        view.getImagePanel().repaint();
        model.setDirty(true);
    }
    /**
     * Draws an isosceles triangle on the current image defined by the specified start and end coordinates.
     * <p>
     * The triangle's apex is positioned at the starting coordinates, and its base spans horizontally
     * between the start and end x-coordinates at the ending y-coordinate. If necessary, the canvas
     * is expanded to accommodate the triangle. The triangle is drawn using the current brush color
     * and brush width.
     * <p>
     * After drawing, the image panel is repainted and the model's dirty flag is set to true.
     * The action is also logged using the current file name.
     *
     * @param startX the x-coordinate of the triangle's apex
     * @param startY the y-coordinate of the triangle's apex
     * @param lastX the x-coordinate defining the base of the triangle
     * @param lastY the y-coordinate defining the base of the triangle
     */
    private void drawTriangle(int startX,int startY,int lastX,int lastY){
        logger.log(model.getCurrentFileName(), "drew triangle");
        int[] coords = expandCanvasIfNeeded(startX,startY,lastX,lastY);
        startX = coords[0];
        startY = coords[1];
        lastX = coords[2];
        lastY = coords[3];
        int topX = startX;
        int topY = startY;

        int baseLeftX = Math.min(startX, lastX);
        int baseRightX = Math.max(startX, lastX);
        int baseY = lastY;

        int[] xPoints = { topX, baseLeftX, baseRightX };
        int[] yPoints = { topY, baseY, baseY };

        Graphics2D g2d = model.getImage().createGraphics();
        g2d.setColor(brushColor);
        g2d.setStroke(new BasicStroke(brushWidth));
        g2d.drawPolygon(xPoints, yPoints, 3);
        g2d.dispose();

        view.getImagePanel().repaint();
        model.setDirty(true);
    }

    /**
     * Draws a regular polygon on the current image with a specified number of sides.
     * <p>
     * The polygon is centered at (centerX, centerY) and one vertex is placed at (vertexX, vertexY),
     * defining the radius and initial angle of the shape. The polygon is drawn using the current
     * brush color and brush width. The canvas is expanded if necessary to fit the polygon.
     * <p>
     * After drawing, the image panel is repainted and the model's dirty flag is set to true.
     * The action is also logged using the current file name.
     *
     * @param centerX the x-coordinate of the polygon's center
     * @param centerY the y-coordinate of the polygon's center
     * @param vertexX the x-coordinate of one polygon vertex to determine radius
     * @param vertexY the y-coordinate of one polygon vertex to determine radius
     * @param sides the number of sides of the polygon (must be 3 or more)
     */
    private void drawPolygon(int centerX, int centerY, int vertexX, int vertexY, int sides) {
        logger.log(model.getCurrentFileName(), "Drew Polygon");
        if (sides < 3) return; // polygons need at least 3 sides

        // Compute radius and initial angle
        double radius = Math.hypot(vertexX - centerX, vertexY - centerY);
        double startAngle = Math.atan2(vertexY - centerY, vertexX - centerX);

        int[] xPoints = new int[sides];
        int[] yPoints = new int[sides];

        for (int i = 0; i < sides; i++) {
            double angle = startAngle + i * 2 * Math.PI / sides;
            xPoints[i] = centerX + (int) Math.round(radius * Math.cos(angle));
            yPoints[i] = centerY + (int) Math.round(radius * Math.sin(angle));
        }

        // Expand canvas if needed
        int[] adjusted = expandCanvasIfNeededArrays(xPoints, yPoints);
        xPoints = Arrays.copyOfRange(adjusted, 0, sides);
        yPoints = Arrays.copyOfRange(adjusted, sides, sides * 2);

        // Draw polygon
        Graphics2D g2d = model.getImage().createGraphics();
        g2d.setColor(brushColor);
        g2d.setStroke(new BasicStroke(brushWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawPolygon(xPoints, yPoints, sides);
        g2d.dispose();

        view.getImagePanel().repaint();
        model.setDirty(true);
    }
    /**
     * Expands the canvas if any points lie outside the current image bounds and adjusts the point coordinates.
     * <p>
     * This method checks the provided arrays of x and y coordinates. If any point falls outside
     * the current image dimensions, the canvas is expanded to include all points, filling new areas
     * with a white background. The original image is preserved and repositioned correctly on the
     * expanded canvas.
     * <p>
     * After expanding, the method returns a single array containing the adjusted coordinates:
     * the first half contains the x-coordinates, and the second half contains the corresponding y-coordinates.
     *
     * @param xPoints the array of x-coordinates to check and adjust
     * @param yPoints the array of y-coordinates to check and adjust
     * @return an array of length 2 * number of points; first half is adjusted x-coordinates, second half is adjusted y-coordinates
     */
    private int[] expandCanvasIfNeededArrays(int[] xPoints, int[] yPoints) {
        int minX = Arrays.stream(xPoints).min().orElse(0);
        int minY = Arrays.stream(yPoints).min().orElse(0);
        int maxX = Arrays.stream(xPoints).max().orElse(0);
        int maxY = Arrays.stream(yPoints).max().orElse(0);

        BufferedImage img = model.getImage();
        int newLeft = Math.min(0, minX);
        int newTop = Math.min(0, minY);
        int newRight = Math.max(img.getWidth(), maxX + 1);
        int newBottom = Math.max(img.getHeight(), maxY + 1);

        if (newRight - newLeft != img.getWidth() || newBottom - newTop != img.getHeight()) {
            BufferedImage expanded = new BufferedImage(newRight - newLeft, newBottom - newTop, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = expanded.createGraphics();
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, expanded.getWidth(), expanded.getHeight());
            g2d.drawImage(img, -newLeft, -newTop, null);
            g2d.dispose();

            model.setImage(expanded);
            view.getImagePanel().setImage(expanded);
        }

        // Adjust points to new canvas
        int sides = xPoints.length;
        int[] adjusted = new int[sides * 2];
        for (int i = 0; i < sides; i++) {
            adjusted[i] = xPoints[i] - newLeft;
            adjusted[i + sides] = yPoints[i] - newTop;
        }
        return adjusted;
    }
    /**
     * Draws a star shape on the current image with a specified number of points.
     * <p>
     * The star is centered at (centerX, centerY) with an outer vertex pointing towards
     * (vertexX, vertexY). The number of points defines how many tips the star has.
     * An inner radius is automatically calculated relative to the outer radius to create the star shape.
     * <p>
     * The canvas is expanded if needed to accommodate the star. The star is drawn using
     * the current brush color and brush width. After drawing, the image panel is repainted
     * and the model's dirty flag is set to true. The action is logged using the current file name.
     *
     * @param centerX the x-coordinate of the star's center
     * @param centerY the y-coordinate of the star's center
     * @param vertexX the x-coordinate of one outer vertex to determine size and orientation
     * @param vertexY the y-coordinate of one outer vertex to determine size and orientation
     * @param points the number of points (tips) of the star; must be at least 2
     */
    private void drawStar(int centerX, int centerY, int vertexX, int vertexY, int points) {
        logger.log(model.getCurrentFileName(), "DrewStar");
        if (points < 2) points = 5; // sensible default

        // outer radius is distance from center to the vertex (mouse)
        double outerRadius = Math.hypot(vertexX - centerX, vertexY - centerY);
        if (outerRadius <= 0.5) return; // nothing to draw

        // inner radius relative to outer (tweak denominator for the look you want)
        double innerRadius = outerRadius / 2.5;
        if (innerRadius <= 0) innerRadius = Math.max(1.0, outerRadius * 0.4);

        int totalPoints = points * 2; // outer + inner alternating
        int[] xp = new int[totalPoints];
        int[] yp = new int[totalPoints];

        // start angle points at mouse so an outer corner points exactly at the mouse
        double startAngle = Math.atan2(vertexY - centerY, vertexX - centerX);
        double angleStep = Math.PI / points; // alternate every PI/points

        for (int i = 0; i < totalPoints; i++) {
            double angle = startAngle + i * angleStep;
            double r = (i % 2 == 0) ? outerRadius : innerRadius;
            xp[i] = (int) Math.round(centerX + r * Math.cos(angle));
            yp[i] = (int) Math.round(centerY + r * Math.sin(angle));
        }

        // Expand canvas if needed; this adjusts points to new coordinates if expansion happened
        int[] adjusted = expandCanvasIfNeededArrays(xp, yp);
        int[] ax = Arrays.copyOfRange(adjusted, 0, totalPoints);
        int[] ay = Arrays.copyOfRange(adjusted, totalPoints, totalPoints * 2); // note expandCanvas returns 2*sides length

        // Draw the star
        Graphics2D g2d = model.getImage().createGraphics();
        g2d.setColor(brushColor);
        g2d.setStroke(new BasicStroke(brushWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawPolygon(ax, ay, totalPoints);
        g2d.dispose();

        view.getImagePanel().repaint();
        model.setDirty(true);
    }



    //========Help Methods=============
    /**
     * Displays an "About" dialog with information about the application.
     * <p>
     * This method shows a message dialog containing the application name, version,
     * and a brief description. The action is also logged using the current file name.
     */

    private void tellAbout(){
        logger.log(model.getCurrentFileName(), "Opend Help diolog");
        JOptionPane.showMessageDialog(view,
                "NHpaint v0.0.6\nSimple paint program.\nMore help coming soon!",
                "About NHpaint",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * updates the label for the tool type, width, and color of the brush being used
     */
    private void updateStatusLabel() {
        String text = "Tool: " + currentTool + " | Width: " + brushWidth;
        statusLabel.setText(text);
        statusLabel.setForeground(brushColor);
    }
    /**
     * Creates a new blank image with the specified dimensions and sets it in the model and view.
     * <p>
     * The new image is initialized with a white background. After creation, the image panel
     * is updated to display the new image, and the model's dirty flag is set to true.
     *
     * @param width the width of the new blank image in pixels
     * @param height the height of the new blank image in pixels
     */
    private void createBlankImage(int width, int height) {
        BufferedImage blankImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = blankImage.createGraphics();
        g2d.setColor(Color.WHITE); // Fill the background with black
        g2d.fillRect(0, 0, width, height);
        g2d.dispose();

        model.setImage(blankImage);
        view.getImagePanel().setImage(blankImage);
        model.setDirty(true);
    }
    private void clearCanvas() {
        if (model.getImage() == null) return;

        int option = JOptionPane.showConfirmDialog(view,
                "Are you sure you want to clear the canvas?",
                "Confirm Clear",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (option == JOptionPane.YES_OPTION) {
            createBlankImage(model.getImage().getWidth(), model.getImage().getHeight());
        }
    }
    /**
     * Expands the canvas if the specified coordinates extend beyond the current image bounds.
     * <p>
     * This method checks the coordinates (x1, y1) and (x2, y2). If any coordinate falls outside
     * the current image dimensions, the canvas is expanded to include them, filling new areas
     * with a white background. The original image is preserved and repositioned on the expanded canvas.
     * <p>
     * Returns the adjusted coordinates relative to the possibly expanded canvas.
     *
     * @param x1 the x-coordinate of the first point
     * @param y1 the y-coordinate of the first point
     * @param x2 the x-coordinate of the second point
     * @param y2 the y-coordinate of the second point
     * @return an array of adjusted coordinates: {adjustedX1, adjustedY1, adjustedX2, adjustedY2}
     */
    private int[] expandCanvasIfNeeded(int x1, int y1, int x2, int y2) {
        BufferedImage current = model.getImage();
        if (current == null) return new int[]{x1, y1, x2, y2};

        int newLeft = Math.min(0, Math.min(x1, x2));
        int newTop = Math.min(0, Math.min(y1, y2));
        int newRight = Math.max(current.getWidth(), Math.max(x1, x2) + 1);
        int newBottom = Math.max(current.getHeight(), Math.max(y1, y2) + 1);

        int newWidth = newRight - newLeft;
        int newHeight = newBottom - newTop;

        if (newWidth != current.getWidth() || newHeight != current.getHeight()) {
            BufferedImage expanded = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = expanded.createGraphics();

            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, newWidth, newHeight);

            g2d.drawImage(current, -newLeft, -newTop, null);
            g2d.dispose();

            model.setImage(expanded);
            view.getImagePanel().setImage(expanded);

            // Return adjusted coordinates
            return new int[]{x1 - newLeft, y1 - newTop, x2 - newLeft, y2 - newTop};
        }

        return new int[]{x1, y1, x2, y2};
    }

    //==========undo redo methods=============
    /**
     * Creates a deep copy of the given {@link BufferedImage}.
     * <p>
     * The returned image is a new {@link BufferedImage} with the same width, height, and type as the source,
     * containing an exact copy of the pixel data. If the source image has an undefined type, it defaults
     * to {@link BufferedImage#TYPE_INT_ARGB}.
     *
     * @param src the source image to copy; may be {@code null}
     * @return a new {@link BufferedImage} that is a deep copy of {@code src}, or {@code null} if {@code src} is {@code null}
     */
    private BufferedImage deepCopy(BufferedImage src) {
        if (src == null) return null;
        int type = src.getType();
        if (type == 0) type = BufferedImage.TYPE_INT_ARGB; // fallback
        BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(), type);
        Graphics2D g = copy.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return copy;
    }

    // call before starting an action that will modify the image
    /**
     * Saves the current image state to the undo stack for future undo operations.
     * <p>
     * This method creates a deep copy of the current image and pushes it onto the undo stack.
     * If the undo stack exceeds the {@code UNDO_LIMIT}, the oldest states are removed.
     * After saving the state, the redo stack is cleared, since new actions invalidate redo history.
     * If there is no current image, the method does nothing.
     */
    private void saveStateForUndo() {
        BufferedImage img = model.getImage();
        if (img == null) return;
        undoStack.push(deepCopy(img));
        // limit size
        while (undoStack.size() > UNDO_LIMIT) undoStack.removeLast();
        // new user action clears redo
        redoStack.clear();
    }

    // Undo: restore previous snapshot
    /**
     * Restores the previous image state from the undo stack.
     * <p>
     * If the undo stack is not empty, the current image is first saved onto the redo stack,
     * then the most recent state from the undo stack is popped and set as the current image
     * in both the model and the view. The model's dirty flag is set to true. If the undo stack
     * is empty, this method does nothing.
     * <p>
     * The action is logged using the current file name.
     */
    private void undo() {
        logger.log(model.getCurrentFileName(), "Used Undo");
        if (undoStack.isEmpty()) return;
        BufferedImage current = model.getImage();
        if (current != null) redoStack.push(deepCopy(current));
        BufferedImage prev = undoStack.pop();
        model.setImage(prev);              // model now contains the restored image
        view.getImagePanel().setImage(prev);
        model.setDirty(true);
    }

    // Redo: re-apply undone snapshot
    /**
     * Restores the next image state from the redo stack.
     * <p>
     * If the redo stack is not empty, the current image is first saved onto the undo stack,
     * then the most recent state from the redo stack is popped and set as the current image
     * in both the model and the view. The model's dirty flag is set to true. If the redo stack
     * is empty, this method does nothing.
     * <p>
     * The action is logged using the current file name.
     */
    private void redo() {
        logger.log(model.getCurrentFileName(), "used Redo");
        if (redoStack.isEmpty()) return;
        BufferedImage current = model.getImage();
        if (current != null) undoStack.push(deepCopy(current));
        BufferedImage next = redoStack.pop();
        model.setImage(next);
        view.getImagePanel().setImage(next);
        model.setDirty(true);
    }

    // convenient: clear undo/redo (call after open/new)
    private void clearHistory() {
        undoStack.clear();
        redoStack.clear();
    }
    /**
     * Cuts (removes) the selected region from the current image.
     * <p>
     * If the image supports transparency, the specified rectangular area is cleared to become transparent.
     * Otherwise, the area is filled with white. After modifying the image, the model's dirty flag is set
     * to true, and the image panel is repainted. If the provided bounds are {@code null} or the image
     * is {@code null}, the method does nothing.
     *
     * @param bounds the rectangular region to cut from the image
     */
    private void cutSelectionFromImage(Rectangle bounds) {
        if (bounds == null) return;
        BufferedImage img = model.getImage();
        if (img == null) return;

        Graphics2D g = img.createGraphics();
        try {
            if (img.getColorModel().hasAlpha()) {
                // clear (make transparent)
                g.setComposite(AlphaComposite.Clear);
                g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
                g.setComposite(AlphaComposite.SrcOver);
            } else {
                // no alpha support: paint over with white (or background color)
                g.setComposite(AlphaComposite.SrcOver);
                g.setColor(Color.WHITE);
                g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }
        } finally {
            g.dispose();
        }
        model.setDirty(true);
        view.getImagePanel().repaint();
    }

    /**
     * Commits the currently selected region to the main image at its current bounds.
     * <p>
     * This method draws the selection image onto the main image at the location defined by
     * the selection bounds. If necessary, the canvas is expanded to accommodate the selection.
     * After committing, the selection bounds are updated to reflect any adjustments, the model's
     * dirty flag is set to true, and the view is repainted. If there is no selection image or bounds,
     * the method does nothing.
     * <p>
     * The action is logged using the current file name.
     */
    private void commitSelectionAtCurrentBounds() {
        logger.log(model.getCurrentFileName(), "comit Selection");
        Rectangle b = model.getSelectionBounds();
        BufferedImage sel = model.getSelectionImage();
        if (b == null || sel == null) return;

        // ensure the canvas is large enough for the selection at this location
        // expandCanvasIfNeeded expects two points; it will replace model.image and view accordingly
        int[] adjusted = expandCanvasIfNeeded(b.x, b.y, b.x + b.width - 1, b.y + b.height - 1);
        // adjusted returns coords (may be same as input if no expansion)
        int drawX = adjusted[0];
        int drawY = adjusted[1];

        // draw selection image into model image
        Graphics2D g = model.getImage().createGraphics();
        try {
            g.setComposite(AlphaComposite.SrcOver);
            g.drawImage(sel, drawX, drawY, null);
        } finally {
            g.dispose();
        }
        // update selection bounds to the (possibly adjusted) location:
        model.setSelectionBounds(new Rectangle(drawX, drawY, sel.getWidth(), sel.getHeight()));
        model.setDirty(true);
        // Update the view image reference (expandCanvasIfNeeded already set it) and repaint
        view.getImagePanel().setImage(model.getImage());
        view.getImagePanel().repaint();
    }

    private void cutSelection() {
        Rectangle b = model.getSelectionBounds();
        if (b == null) return;
        // selectionImage already present (if user selected), otherwise create it
        if (model.getSelectionImage() == null) {
            model.setSelectionImage(view.getImagePanel().getSubimage(b.x, b.y, b.width, b.height));
        }
        cutSelectionFromImage(b);
        model.setSelectionCut(true);
    }

    // Paste selection at image coordinates (x,y)
    /**
     * Pastes the current selection image at the specified coordinates on the main image.
     * <p>
     * The selection image from the model is placed with its top-left corner at (x, y),
     * updating the selection bounds accordingly. After setting the selection, it is
     * immediately committed to the main image. If there is no selection image, this method does nothing.
     *
     * @param x the x-coordinate where the top-left corner of the selection should be placed
     * @param y the y-coordinate where the top-left corner of the selection should be placed
     */
    private void pasteSelectionAt(int x, int y) {
        BufferedImage clipboard = model.getSelectionImage();
        if (clipboard == null) return;
        Rectangle newBounds = new Rectangle(x, y, clipboard.getWidth(), clipboard.getHeight());
        model.setSelectionBounds(newBounds);
        model.setSelectionImage(clipboard);
        // commit if you want it permanently on canvas:
        commitSelectionAtCurrentBounds();
    }
    /**
     * Copies the currently selected region from the image to the model's clipboard.
     * <p>
     * The method retrieves the selection bounds and creates a new {@link BufferedImage}
     * containing the selected area. This copied image is then stored in the model’s
     * clipboard for later use (e.g., pasting). If no valid selection exists, the method
     * does nothing and prints a message to the console.
     * <p>
     * The action is also logged using the current file name.
     */

    private void copySelection() {
        logger.log(model.getCurrentFileName(), "copied Selection");
        Rectangle bounds = model.getSelectionBounds();
        if (bounds == null || bounds.width == 0 || bounds.height == 0) {
            System.out.println("No selection to copy!");
            return;
        }
        BufferedImage canvas = view.getImagePanel().getImage();
        BufferedImage selectionCopy = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = selectionCopy.createGraphics();
        g.drawImage(canvas.getSubimage(bounds.x, bounds.y, bounds.width, bounds.height), 0, 0, null);
        g.dispose();

        // store it in the model's clipboard
        model.setClipboardImage(selectionCopy);
    }

    /**
     * Handles the paste action (Ctrl+V) by inserting the image stored in the model's clipboard.
     * <p>
     * If a clipboard image exists, it is set as the current selection image and a paste preview
     * is activated. The image is initially positioned at the center of the image panel, and the
     * view is repainted to reflect the pending paste operation.
     * <p>
     * If the clipboard is empty or does not contain an image, a message is printed to the console.
     */
    public void clickedCtrlV() {
        BufferedImage clipboardImage = model.getClipboardImage(); // get image from clipboard
        if (clipboardImage != null) {
            // Set the selection image in the model
            model.setSelectionImage(clipboardImage);
            model.setPastePreviewActive(true);

            // Initialize paste position (e.g., center of panel)
            int startX = (view.getImagePanel().getWidth() - clipboardImage.getWidth()) / 2;
            int startY = (view.getImagePanel().getHeight() - clipboardImage.getHeight()) / 2;
            model.setPasteX(startX);
            model.setPasteY(startY);

            view.getImagePanel().repaint();
        } else {
            // Optional: feedback if nothing in clipboard
            System.out.println("Clipboard is empty or not an image");
        }
    }
    /**
     * Rotates the current image or the active selection by the specified number of degrees.
     * <p>
     * If a selection is active, only that selection is rotated around its center; otherwise,
     * the entire canvas image is rotated. The rotation is applied non-destructively and recorded
     * in the undo history before modification. The method also attempts to preserve the user's
     * viewport position and re-center the view after the rotation is applied.
     * </p>
     *
     * <p><b>Behavior summary:</b></p>
     * <ul>
     *   <li>If a selection exists — rotates only the selected area and updates its bounding box.</li>
     *   <li>If no selection — rotates the entire canvas image.</li>
     *   <li>Automatically saves a snapshot for undo and clears redo history.</li>
     *   <li>Re-centers the viewport to keep the image roughly in view after rotation.</li>
     * </ul>
     *
     * @param degrees the angle in degrees to rotate the image or selection.
     *                Positive values rotate counterclockwise.
     */
    private void rotateCurrent(double degrees) {
        // snapshot for undo
        saveStateForUndo();

        ImagePanel panel = view.getImagePanel();
        JViewport vp = getViewport();

        // capture center in image coords so we can re-center after rotation
        int centerImageX = -1, centerImageY = -1;
        Dimension extent = null;
        if (vp != null && panel.getImage() != null) {
            Point vpPos = vp.getViewPosition();
            extent = vp.getExtentSize();
            int viewCenterPanelX = vpPos.x + extent.width / 2;
            int viewCenterPanelY = vpPos.y + extent.height / 2;
            centerImageX = toImageX(viewCenterPanelX);
            centerImageY = toImageY(viewCenterPanelY);
        }

        Rectangle bounds = model.getSelectionBounds();
        BufferedImage sel = model.getSelectionImage();

        if (bounds != null && sel != null) {
            // rotate just the selection image
            BufferedImage rotatedSel = rotateImage(sel, degrees);
            model.setSelectionImage(rotatedSel);

            // Update selection bounds to keep rotation centered around the selection center
            int newX = bounds.x + bounds.width/2 - rotatedSel.getWidth()/2;
            int newY = bounds.y + bounds.height/2 - rotatedSel.getHeight()/2;
            model.setSelectionBounds(new Rectangle(newX, newY, rotatedSel.getWidth(), rotatedSel.getHeight()));
        } else if (model.getImage() != null) {
            // rotate whole canvas
            BufferedImage rotated = rotateImage(model.getImage(), degrees);
            model.setImage(rotated);
            view.getImagePanel().setImage(rotated);
        }

        // refresh layout/painting
        view.getImagePanel().revalidate();
        view.getImagePanel().repaint();
        model.setDirty(true);

        // --- Center the viewport on the image after rotation ---
        if (vp != null && panel.getImage() != null) {
            extent = vp.getExtentSize();
            int imageW = panel.getImage().getWidth();
            int imageH = panel.getImage().getHeight();

            // Compute panel coordinates of image center (accounting for zoom and centering)
            double zoom = panel.getZoom();
            int displayW = (int) Math.round(imageW * zoom);
            int displayH = (int) Math.round(imageH * zoom);
            int offsetX = (panel.getWidth() - displayW) / 2;
            int offsetY = (panel.getHeight() - displayH) / 2;

            int imageCenterPanelX = offsetX + displayW / 2;
            int imageCenterPanelY = offsetY + displayH / 2;

            // Compute the top-left position so that image center aligns with viewport center
            int newViewX = imageCenterPanelX - extent.width / 2;
            int newViewY = imageCenterPanelY - extent.height / 2;

            // Clamp within panel bounds
            newViewX = Math.max(0, Math.min(newViewX, Math.max(0, panel.getWidth() - extent.width)));
            newViewY = Math.max(0, Math.min(newViewY, Math.max(0, panel.getHeight() - extent.height)));

            vp.setViewPosition(new Point(newViewX, newViewY));
        }

    }



    /**
     * Rotates the given image by a specified number of degrees around its center.
     * <p>
     * The method creates a new {@link BufferedImage} large enough to fully contain
     * the rotated image without clipping, preserving transparency and image quality.
     * It uses bicubic interpolation for smoother rotation results.
     * </p>
     *
     * @param src     the source {@link BufferedImage} to rotate; may be {@code null}.
     * @param degrees the rotation angle in degrees. Positive values rotate counterclockwise.
     * @return a new {@link BufferedImage} containing the rotated image, or {@code null} if the source is {@code null}.
     */
    private BufferedImage rotateImage(BufferedImage src, double degrees) {
        if (src == null) return null;
        double radians = Math.toRadians(degrees);
        double sin = Math.abs(Math.sin(radians));
        double cos = Math.abs(Math.cos(radians));
        int w = src.getWidth();
        int h = src.getHeight();
        int newW = (int) Math.floor(w * cos + h * sin);
        int newH = (int) Math.floor(h * cos + w * sin);

        BufferedImage rotated = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = rotated.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        AffineTransform at = new AffineTransform();
        // translate so original image is centered in destination
        at.translate((newW - w) / 2.0, (newH - h) / 2.0);
        // rotate about original image center
        at.rotate(radians, w / 2.0, h / 2.0);

        g2d.drawImage(src, at, null);
        g2d.dispose();
        return rotated;
    }


    // helper: find the viewport (null if not inside a scroll pane)
    private JViewport getViewport() {
        return (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, view.getImagePanel());
    }

    // convert image coords -> panel coords (matching ImagePanel.toPanelX / toPanelY logic)
    private int toPanelX(int imageX) {
        ImagePanel imgPanel = view.getImagePanel();
        if (imgPanel.getImage() == null) return imageX;
        double zoom = imgPanel.getZoom();
        int imgWidth = imgPanel.getImage().getWidth();
        int displayWidth = (int) Math.round(imgWidth * zoom);
        int offsetX = (imgPanel.getWidth() - displayWidth) / 2;
        return (int) Math.round(imageX * zoom + offsetX);
    }
    private int toPanelY(int imageY) {
        ImagePanel imgPanel = view.getImagePanel();
        if (imgPanel.getImage() == null) return imageY;
        double zoom = imgPanel.getZoom();
        int imgHeight = imgPanel.getImage().getHeight();
        int displayHeight = (int) Math.round(imgHeight * zoom);
        int offsetY = (imgPanel.getHeight() - displayHeight) / 2;
        return (int) Math.round(imageY * zoom + offsetY);
    }
    /**
     * Mirrors the current selection or the entire canvas horizontally.
     * <p>
     * If a selection is active, only the selected region is mirrored and then committed
     * back to the canvas. Otherwise, the entire image canvas is mirrored from left to right.
     * The operation preserves transparency and updates the view and model accordingly.
     * </p>
     *
     * <p>After mirroring, the method marks the model as dirty to indicate unsaved changes.</p>
     */
    private void mirrorCanvasOrSelection() {
        logger.log(model.getCurrentFileName(), "mirroed image");
        BufferedImage img = model.getImage();
        if (img == null) return;

        Rectangle selBounds = model.getSelectionBounds();
        if (selBounds != null && selBounds.width > 0 && selBounds.height > 0) {
            // Mirror selection only
            BufferedImage selection = model.getSelectionImage();
            if (selection == null) return;

            BufferedImage mirrored = new BufferedImage(selection.getWidth(), selection.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = mirrored.createGraphics();
            g2d.drawImage(selection, 0, 0, selection.getWidth(), selection.getHeight(),
                    selection.getWidth(), 0, 0, selection.getHeight(), null);
            g2d.dispose();

            model.setSelectionImage(mirrored);
            commitSelectionAtCurrentBounds(); // update canvas
        } else {
            // Mirror entire canvas
            BufferedImage mirrored = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = mirrored.createGraphics();
            g2d.drawImage(img, 0, 0, img.getWidth(), img.getHeight(),
                    img.getWidth(), 0, 0, img.getHeight(), null);
            g2d.dispose();

            model.setImage(mirrored);
            view.getImagePanel().setImage(mirrored);
            view.getImagePanel().repaint();
            model.setDirty(true);
        }
    }



}
