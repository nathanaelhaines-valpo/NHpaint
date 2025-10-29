import java.awt.*;
import java.io.*;
import java.awt.image.BufferedImage;
import java.util.Stack;

public class PaintModel {
    private BufferedImage image;
    private File currentFile;
    private boolean dirty;

    private Rectangle selectionBounds = null; // area selected
    private BufferedImage selectionImage = null; // copy of selected pixels
    private boolean selectionCut = false;
    private Point dragOffset= new Point(0,0);

    private final Stack<BufferedImage> undoStack = new Stack<>();
    private final Stack<BufferedImage> redoStack = new Stack<>();

    // In PaintModel
    private boolean pastePreviewActive = false;
    private BufferedImage clipboardImage;   // holds copied image
    private int pasteX, pasteY;             // current mouse location during preview

    public boolean isPastePreviewActive() { return pastePreviewActive; }
    public void setPastePreviewActive(boolean b) { pastePreviewActive = b; }

    public BufferedImage getClipboardImage() { return clipboardImage; }
    public void setClipboardImage(BufferedImage img) { clipboardImage = img; }

    public int getPasteX() { return pasteX; }
    public void setPasteX(int x) { pasteX = x; }

    public int getPasteY() { return pasteY; }
    public void setPasteY(int y) { pasteY = y; }



    public BufferedImage getImage() { return image; }

    public void setImage(BufferedImage img) {
        this.image = img;
        this.dirty = true;
    }

    public boolean canUndo() { return !undoStack.isEmpty(); }
    public boolean canRedo() { return !redoStack.isEmpty(); }



    /**
     * Returns the currently associated file with the open image.
     * <p>
     * This file represents the last location from which the image was opened
     * or to which it was saved. If the image has not been saved or loaded yet,
     * this method may return {@code null}.
     * </p>
     *
     * @return the {@link File} object representing the current image file, or {@code null} if none exists
     */
    public File getCurrentFile() { return currentFile; }
    /**
     * Sets the current file associated with the open image.
     * <p>
     * This method updates the reference to the file that represents
     * the image currently being edited. It is typically called after
     * opening an existing image file or saving a new one.
     * </p>
     *
     * @param file the {@link File} object to associate with the current image;
     *              may be {@code null} if no file is associated
     */
    public void setCurrentFile(File file) { this.currentFile = file; }
    /**
     * Checks whether the current image has unsaved changes.
     * <p>
     * The dirty flag indicates if the image has been modified since the last save
     * operation. If {@code true}, the user should be prompted to save changes before
     * closing or opening a new file.
     * </p>
     *
     * @return {@code true} if the image has unsaved modifications, {@code false} otherwise
     */
    public boolean isDirty() { return dirty; }
    /**
     * Sets the dirty state of the current image.
     * <p>
     * This method updates the internal flag that tracks whether the image
     * has unsaved changes. It should be called whenever an edit is made
     * or after the image is successfully saved.
     * </p>
     *
     * @param dirty {@code true} to mark the image as having unsaved changes,
     *              {@code false} to indicate that all changes are saved
     */
    public void setDirty(boolean dirty) { this.dirty = dirty; }
    /**
     * Returns the current selection bounds on the canvas.
     * <p>
     * The selection bounds define the rectangular region of the image
     * that is currently selected for editing operations such as copy,
     * cut, move, or transform. If no selection is active, this method
     * may return {@code null}.
     * </p>
     *
     * @return the {@link Rectangle} representing the selection bounds,
     *         or {@code null} if no selection is active
     */
    public Rectangle getSelectionBounds() {
        return selectionBounds;
    }
    /**
     * Sets the bounds of the current selection on the canvas.
     * <p>
     * This method defines the rectangular region that will be treated
     * as the active selection for operations such as copy, cut, move,
     * or transform. Passing {@code null} clears the current selection.
     * </p>
     *
     * @param selectionBounds the {@link Rectangle} representing the new selection bounds,
     *                        or {@code null} to clear the selection
     */
    public void setSelectionBounds(Rectangle selectionBounds) {
        this.selectionBounds = selectionBounds;
    }
    /**
     * Returns the image of the currently selected region.
     * <p>
     * The selection image contains the pixel data within the selection bounds
     * and can be used for operations such as copy, cut, paste, or transformations.
     * If no selection is active, this method may return {@code null}.
     * </p>
     *
     * @return the {@link BufferedImage} representing the current selection,
     *         or {@code null} if no selection exists
     */
    public BufferedImage getSelectionImage() {
        return selectionImage;
    }
    /**
     * Sets the image for the current selection.
     * <p>
     * This method defines the pixel data for the currently selected region on the canvas.
     * It is typically used when copying, pasting, or modifying a selection. Passing {@code null}
     * will clear the selection image.
     * </p>
     *
     * @param selectionImage the {@link BufferedImage} to use as the selection image,
     *                       or {@code null} to clear the selection
     */
    public void setSelectionImage(BufferedImage selectionImage) {
        this.selectionImage = selectionImage;
    }
    /**
     * Checks whether the current selection is in "cut" mode.
     * <p>
     * When a selection is cut, the original pixels in the selected area
     * are removed (or made transparent) when the selection is moved,
     * rather than just copied. This flag indicates whether the selection
     * will be removed from its original location upon manipulation.
     * </p>
     *
     * @return {@code true} if the selection is marked as cut, {@code false} otherwise
     */
    public boolean isSelectionCut() {
        return selectionCut;
    }
    /**
     * Sets whether the current selection is in "cut" mode.
     * <p>
     * When a selection is marked as cut, the original pixels in the selected area
     * will be removed (or made transparent) when the selection is moved, rather than copied.
     * Setting this flag controls whether the selection behaves as a cut or copy.
     * </p>
     *
     * @param cut {@code true} to mark the selection as cut, {@code false} to mark it as a copy
     */
    public void setSelectionCut(boolean cut)
    { this.selectionCut = cut; }
    /**
     * Sets the drag offset for the current selection.
     * <p>
     * The drag offset represents the distance between the mouse cursor and
     * the top-left corner of the selection when beginning a drag operation.
     * This ensures smooth and accurate movement of the selection.
     * If {@code p} is {@code null}, the offset defaults to (0, 0).
     * </p>
     *
     * @param p the {@link Point} representing the drag offset, or {@code null} to reset to (0, 0)
     */
    public void setDragOffset(Point p)
    { this.dragOffset = (p == null ? new Point(0,0) : p); }
    /**
     * Returns the current drag offset for the active selection.
     * <p>
     * The drag offset indicates the distance between the mouse cursor
     * and the top-left corner of the selection at the start of a drag operation.
     * It is used to ensure accurate positioning while moving the selection.
     * </p>
     *
     * @return a {@link Point} representing the current drag offset
     */
    public Point getDragOffset() { return dragOffset; }
    /**
     * Clears the current selection from the canvas.
     * <p>
     * This method removes the selection bounds and selection image, resets
     * the drag offset to (0, 0), and marks the selection as not cut. After
     * calling this method, no selection will be active.
     */
    public void clearSelection() {
        selectionBounds = null;
        selectionImage = null;
        selectionCut = false;
        dragOffset = new Point(0, 0);
        selectionCut = false;
    }
    /**
     * Returns the name of the currently associated file.
     * <p>
     * If a file is associated with the current image, its name is returned.
     * Otherwise, the method returns a default name "Untitled".
     * </p>
     *
     * @return the name of the current file, or "Untitled" if no file is set
     */
    public String getCurrentFileName() {
        if (currentFile != null) {
            return currentFile.getName();
        } else {
            return "Untitled";
        }
    }


}
