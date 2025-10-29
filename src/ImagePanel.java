import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.Composite;

public class ImagePanel extends JPanel {
    private BufferedImage image;
    private double zoom = 1.0;

    // For live preview
    private int previewStartX = -1;
    private int previewStartY = -1;
    private int previewEndX = -1;
    private int previewEndY = -1;
    private Tool previewTool = Tool.LINE;
    private float previewBrushWidth = 1.0f;
    private Color previewBrushColor = Color.GRAY;
    private int previewSides;
    private PaintModel model = new PaintModel();

    public ImagePanel(PaintModel m){
        this.model = m;
    }

    public void setPreview(int startX, int startY, int endX, int endY, Tool tool, float brushWidth, Color color) {
        this.previewStartX = startX;
        this.previewStartY = startY;
        this.previewEndX = endX;
        this.previewEndY = endY;
        this.previewTool = tool;
        this.previewBrushWidth = brushWidth;
        this.previewBrushColor = color;
        repaint();
    }
    public void setPreview(int startX, int startY, int endX, int endY, Tool tool, float brushWidth, Color color, int sides) {
        this.previewStartX = startX;
        this.previewStartY = startY;
        this.previewEndX = endX;
        this.previewEndY = endY;
        this.previewTool = tool;
        this.previewBrushWidth = brushWidth;
        this.previewBrushColor = color;
        this.previewSides = sides;
        repaint();
    }

    public void clearPreview() {
        previewStartX = previewStartY = previewEndX = previewEndY = -1;
        previewTool = null;
        repaint();
    }

    /**
     * puts the image on the panal
     * @param img the image
     */
    public void setImage(BufferedImage img) {
        this.image = img;
        revalidate(); // tell scroll pane size might have changed
        repaint();
    }

    public BufferedImage getImage() {
        return image;
    }
    public Tool getPreviewTool(){
        return previewTool;
    }

    /**
     * Sets the zoom level for the image panel.
     * <p>
     * This method updates the zoom factor, which affects how the image
     * is scaled when displayed. After setting the zoom, the panel is
     * revalidated and repainted to reflect the change.
     * </p>
     *
     * @param zoom the new zoom factor (e.g., 1.0 for 100%, 2.0 for 200%)
     */
    public void setZoom(double zoom) {
        this.zoom = zoom;
        revalidate();
        repaint();
    }
    /**
     * Returns the current zoom level of the image panel.
     * <p>
     * The zoom factor determines the scale at which the image is displayed.
     * A value of 1.0 represents 100% (original size), 2.0 represents 200%, and so on.
     * </p>
     *
     * @return the current zoom factor
     */
    public double getZoom() {
        return zoom;
    }

    private int toImageX(int panelX) {
        if (image == null) return panelX;
        int imgWidth = (int)(image.getWidth() * zoom);
        int offsetX = (getWidth() - imgWidth) / 2;
        return (int)((panelX - offsetX) / zoom);
    }

    private int toImageY(int panelY) {
        if (image == null) return panelY;
        int imgHeight = (int)(image.getHeight() * zoom);
        int offsetY = (getHeight() - imgHeight) / 2;
        return (int)((panelY - offsetY) / zoom);
    }

    private int toPanelX(int imageX) {
        if (image == null) return imageX;
        int imgWidth = (int)(image.getWidth() * zoom);
        int offsetX = (getWidth() - imgWidth) / 2;
        return (int)(imageX * zoom + offsetX);
    }

    private int toPanelY(int imageY) {
        if (image == null) return imageY;
        int imgHeight = (int)(image.getHeight() * zoom);
        int offsetY = (getHeight() - imgHeight) / 2;
        return (int)(imageY * zoom + offsetY);
    }

    /**
     * Paints the image panel, including the main image, current selection, paste previews,
     * and live tool previews.
     * <p>
     * This method overrides {@link javax.swing.JComponent#paintComponent(Graphics)}
     * to render the image at the current zoom level, centered within the panel. It also:
     * </p>
     * <ul>
     *     <li>Draws the main image with scaling based on the zoom factor.</li>
     *     <li>Renders a semi-transparent preview of any paste operation.</li>
     *     <li>Displays live previews for drawing tools (line, rectangle, ellipse, triangle, polygon, star).</li>
     *     <li>Renders the selection bounds with a dashed outline and draws the selection image.</li>
     * </ul>
     * <p>
     * The method preserves graphics state (color, stroke, composite) before drawing previews
     * and restores it afterward.
     * </p>
     *
     * @param g the {@link Graphics} object to use for painting
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        // compute offsets and display size
        int offsetX = 0, offsetY = 0;
        if (image != null) {
            int imgW = image.getWidth();
            int imgH = image.getHeight();
            int displayW = (int) Math.round(imgW * zoom);
            int displayH = (int) Math.round(imgH * zoom);
            offsetX = (getWidth() - displayW) / 2;
            offsetY = (getHeight() - displayH) / 2;

            // draw image in transformed (image-space) coordinates:
            g2d.translate(offsetX, offsetY);
            g2d.scale(zoom, zoom);
            g2d.drawImage(image, 0, 0, null);

            if (model.isPastePreviewActive() && model.getSelectionImage() != null) {
                BufferedImage img = model.getSelectionImage();
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
                g2d.drawImage(img, model.getPasteX(), model.getPasteY(), null);
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));


                // optional: dashed outline
                g2d.setColor(Color.BLACK);
                g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_BEVEL, 0, new float[]{4}, 0));
                g2d.drawRect(model.getPasteX(), model.getPasteY(), img.getWidth(), img.getHeight());
            }

        } else {
            g2d.dispose();
            return;
        }

        // ---------- Draw live preview (image-space coordinates) ----------
        if (previewStartX != -1 && previewEndX != -1 && previewTool != null) {
            Stroke oldStroke = g2d.getStroke();
            Color oldColor = g2d.getColor();

            g2d.setColor(previewBrushColor);
            g2d.setStroke(new BasicStroke(previewBrushWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            int sx = previewStartX;
            int sy = previewStartY;
            int ex = previewEndX;
            int ey = previewEndY;

            switch (previewTool) {
                case LINE:
                    g2d.drawLine(sx, sy, ex, ey);
                    break;
                case RECTANGLE: {
                    int x = Math.min(sx, ex);
                    int y = Math.min(sy, ey);
                    int w = Math.abs(ex - sx);
                    int h = Math.abs(ey - sy);
                    g2d.drawRect(x, y, w, h);
                    break;
                }
                case ELLIPSE: {
                    int x = Math.min(sx, ex);
                    int y = Math.min(sy, ey);
                    int w = Math.abs(ex - sx);
                    int h = Math.abs(ey - sy);
                    g2d.drawOval(x, y, w, h);
                    break;
                }
                case TRIANGLE: {
                    int topX = sx, topY = sy;
                    int baseLeft = Math.min(sx, ex);
                    int baseRight = Math.max(sx, ex);
                    int baseY = ey;
                    int[] xp = {topX, baseLeft, baseRight};
                    int[] yp = {topY, baseY, baseY};
                    g2d.drawPolygon(xp, yp, 3);
                    break;
                }
                case POLYGON: {
                    int sides = previewSides;
                    if (sides >= 3) {
                        double radius = Math.hypot(previewEndX - previewStartX, previewEndY - previewStartY);
                        double angleStep = 2 * Math.PI / sides;
                        double startAngle = Math.atan2(previewEndY - previewStartY, previewEndX - previewStartX);
                        int[] xp = new int[sides];
                        int[] yp = new int[sides];
                        for (int i = 0; i < sides; i++) {
                            double angle = startAngle + i * angleStep;
                            xp[i] = (int) Math.round(previewStartX + radius * Math.cos(angle));
                            yp[i] = (int) Math.round(previewStartY + radius * Math.sin(angle));
                        }
                        g2d.drawPolygon(xp, yp, sides);
                    }
                    break;
                }
                case STAR: {
                    int points = previewSides;
                    if (points < 2) points = 5; // sensible default (5-point star) if caller forgot to set sides

                    // outer radius is distance from center (previewStart) to mouse (previewEnd).
                    double outerRadius = Math.hypot(previewEndX - previewStartX, previewEndY - previewStartY);
                    if (outerRadius <= 0.5) break; // nothing to draw

                    // inner radius chosen relative to outer; tune the denominator for different "sharpness"
                    double innerRadius = outerRadius / 2.5;
                    if (innerRadius <= 0) innerRadius = Math.max(1.0, outerRadius * 0.4);

                    // Start angle must point at the mouse end location so one outer corner aligns with the mouse.
                    double startAngle = Math.atan2(previewEndY - previewStartY, previewEndX - previewStartX);

                    int totalPoints = points * 2;               // outer + inner alternating
                    int[] xp = new int[totalPoints];
                    int[] yp = new int[totalPoints];

                    // angle step: alternate between outer and inner points, so step = PI / points
                    double angleStep = Math.PI / points;

                    for (int i = 0; i < totalPoints; i++) {
                        double angle = startAngle + i * angleStep;
                        double r = (i % 2 == 0) ? outerRadius : innerRadius; // even indices -> outer
                        xp[i] = (int) Math.round(previewStartX + r * Math.cos(angle));
                        yp[i] = (int) Math.round(previewStartY + r * Math.sin(angle));
                    }



                    g2d.setColor(previewBrushColor);
                    g2d.setStroke(new BasicStroke(previewBrushWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2d.drawPolygon(xp, yp, totalPoints);

                    g2d.setColor(oldColor);
                    g2d.setStroke(oldStroke);
                    break;
                }


                case SELECTION: {
                    int x = Math.min(sx, ex);
                    int y = Math.min(sy, ey);
                    int w = Math.abs(ex - sx);
                    int h = Math.abs(ey - sy);
                    if (w > 0 && h > 0) {
                        Stroke dashed = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{4f}, 0f);
                        g2d.setColor(Color.BLACK);
                        g2d.setStroke(dashed);
                        g2d.drawRect(x, y, w, h);
                        // don't draw the selection image here (we create selection on release)
                    }
                    break;
                }
                default:
                    break;
            }

            g2d.setColor(oldColor);
            g2d.setStroke(oldStroke);
        }

        // ---------- Draw selection (if exists) ----------
        Rectangle sel = model.getSelectionBounds();
        BufferedImage selImg = model.getSelectionImage();
        if (sel != null) {
            // dashed rectangle
            Stroke oldStroke = g2d.getStroke();
            Color oldColor = g2d.getColor();
            Stroke dashed = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{4f}, 0f);
            g2d.setColor(Color.BLACK);
            g2d.setStroke(dashed);
            g2d.drawRect(sel.x, sel.y, sel.width, sel.height);

            if (selImg != null) {
                Composite oldComp = g2d.getComposite();
                // If selection has been cut, draw opaque; otherwise draw slightly translucent so user can see original under it
                if (model.isSelectionCut()) {
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
                } else {
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f));
                }
                g2d.drawImage(selImg, sel.x, sel.y, null);
                g2d.setComposite(oldComp);
            }

            g2d.setColor(oldColor);
            g2d.setStroke(oldStroke);
        }
    }
    /**
     * Returns the preferred size of the image panel based on the current image and zoom level.
     * <p>
     * If an image is loaded, the preferred size is calculated by scaling the image dimensions
     * by the current zoom factor. If no image is present, the method falls back to the default
     * component preferred size.
     * </p>
     *
     * @return a {@link Dimension} representing the preferred size of the panel
     */
    @Override
    public Dimension getPreferredSize() {
        if (image != null) {
            return new Dimension(
                    (int) (image.getWidth() * zoom),
                    (int) (image.getHeight() * zoom)
            );
        }
        return super.getPreferredSize();
    }
    /**
     * Returns a copy of a rectangular subregion of the current image.
     * <p>
     * The subimage is clamped to the bounds of the image, ensuring that the
     * requested region does not exceed the image dimensions. The returned
     * subimage is a new {@link BufferedImage} and does not share data with
     * the original image.
     * </p>
     *
     * @param x the x-coordinate of the top-left corner of the subregion
     * @param y the y-coordinate of the top-left corner of the subregion
     * @param w the width of the subregion
     * @param h the height of the subregion
     * @return a new {@link BufferedImage} containing the requested subregion,
     *         or {@code null} if the image is {@code null} or the region is invalid
     */
    public BufferedImage getSubimage(int x, int y, int w, int h) {
        if (image == null) return null;

        // clamp
        if (x < 0) { w += x; x = 0; }
        if (y < 0) { h += y; y = 0; }
        if (x + w > image.getWidth()) w = image.getWidth() - x;
        if (y + h > image.getHeight()) h = image.getHeight() - y;
        if (w <= 0 || h <= 0) return null;

        BufferedImage copy = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = copy.createGraphics();
        // draw the requested region into the copy
        g.drawImage(image, 0, 0, w, h, x, y, x + w, y + h, null);
        g.dispose();
        return copy;
    }
}