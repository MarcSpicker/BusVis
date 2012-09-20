package infovis.overview;

import infovis.ctrl.Controller;
import infovis.data.BusStation;
import infovis.gui.MouseInteraction;
import infovis.gui.Refreshable;
import infovis.gui.ZoomableUI;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * Interactor for the SVG canvas.
 * 
 * @author Marc Spicker
 */
public final class OverviewMouse extends MouseInteraction implements Refreshable {

  /** The overview visualization. */
  private final Overview over;

  /** Controller. */
  private final Controller ctrl;

  /** The zoomable user interface. */
  private final ZoomableUI zui;

  /**
   * Constructor.
   * 
   * @param over The overview visualization.
   * @param ctrl Controller.
   */
  public OverviewMouse(final Overview over, final Controller ctrl) {
    this.over = over;
    this.ctrl = ctrl;
    zui = new ZoomableUI(this);
    zui.setMaxZoom(2.5);
    focus = over;
  }

  /** The focused component. */
  private JComponent focus;

  @Override
  public void mousePressed(final MouseEvent e) {
    focus.grabFocus();
    final Point2D p = e.getPoint();
    final Point2D c = zui.getForScreen(p);
    final boolean leftButton = SwingUtilities.isLeftMouseButton(e);
    if(click(c, leftButton)) return;

    if(leftButton) {
      startDragging(e, zui.getOffsetX(), zui.getOffsetY());
    }
  }

  /**
   * Setter.
   * 
   * @param focus The component to focus when clicked.
   */
  public void setFocusComponent(final JComponent focus) {
    if(focus == null) {
      new NullPointerException("focus");
    }
    this.focus = focus;
  }

  /**
   * Getter.
   * 
   * @return The component to focus when clicked.
   */
  public JComponent getFocusComponent() {
    return focus;
  }

  /** The radius in which, if clicked, the station is selected. */
  public static final int STATION_RADIUS = 10;

  /**
   * Returns whether or not the user has clicked on a bus station.
   * 
   * @param c Click Point.
   * @param leftButton Whether the click is a left button click.
   * @return Whether a station has been clicked on.
   */
  private boolean click(final Point2D c, final boolean leftButton) {
    double minDist = Double.POSITIVE_INFINITY;
    BusStation closestStation = null;
    for(final BusStation station : ctrl.getStations()) {
      final double curDist = distanceToStationSq(c, station);
      if(curDist < minDist) {
        minDist = curDist;
        closestStation = station;
      }
    }
    if(minDist >= STATION_RADIUS * STATION_RADIUS) {
      if(!leftButton) {
        ctrl.clearSecondarySelection();
        return true;
      }
      return false;
    }
    if(leftButton) {
      ctrl.selectStation(closestStation);
    } else {
      ctrl.toggleSecondarySelected(closestStation);
    }
    return true;
  }

  /**
   * Returns the distance of a point to an abstract station position.
   * 
   * @param p The point.
   * @param station The station.
   * @return Distance between the point and the station.
   */
  private static double distanceToStationSq(final Point2D p, final BusStation station) {
    final double dx = p.getX() - station.getAbstractX();
    final double dy = p.getY() - station.getAbstractY();
    return dx * dx + dy * dy;
  }

  @Override
  public void mouseDragged(final MouseEvent e) {
    if(isDragging()) {
      move(e.getX(), e.getY());
    }
  }

  @Override
  public void mouseReleased(final MouseEvent e) {
    mouseDragged(e);
    stopDragging();
  }

  /**
   * Sets the offset according to the mouse position.
   * 
   * @param x the mouse x position
   * @param y the mouse y position
   */
  private void move(final int x, final int y) {
    setOffset(getMoveX(x), getMoveY(y));
  }

  @Override
  public void mouseWheelMoved(final MouseWheelEvent e) {
    if(!isDragging()) {
      zui.zoomTo(e.getX(), e.getY(), e.getWheelRotation());
    }
  }

  /**
   * Setter.
   * 
   * @param x the x offset.
   * @param y the y offset.
   */
  public void setOffset(final double x, final double y) {

    // FIXME remove this method

    final Rectangle2D svgBB = over.getSVGBoundingRect();
    if(svgBB == null) return;

    double offX = x;
    double offY = y;
    final Rectangle2D visBB = getVisibleCanvas();

    // snap back
    if(!svgBB.contains(visBB)) {
      double transX = 0;
      double transY = 0;

      if(visBB.getMaxX() > svgBB.getMaxX()) {
        // too far right
        transX -= visBB.getMaxX() - svgBB.getMaxX();
      } else if(visBB.getMinX() < svgBB.getMinX()) {
        // too far left
        transX += svgBB.getMinX() - visBB.getMinX();
      }
      if(visBB.getMaxY() > svgBB.getMaxY()) {
        // too far down
        transY -= visBB.getMaxY() - svgBB.getMaxY();
      } else if(visBB.getMinY() < svgBB.getMinY()) {
        // too far up
        transY += svgBB.getMinY() - visBB.getMinY();
      }

      offX -= zui.fromReal(transX);
      offY -= zui.fromReal(transY);
    }

    zui.setOffset(offX, offY);
  }

  /**
   * Returns the visible rectangle in canvas coordinates.
   * 
   * @return The visible rectangle in canvas coordinates.
   */
  public Rectangle2D getVisibleCanvas() {
    final Rectangle2D rect = over.getVisibleRect();
    final Point2D topLeft = zui.getForScreen(new Point2D.Double(rect.getMinX(),
        rect.getMinY()));
    return new Rectangle2D.Double(topLeft.getX(), topLeft.getY(),
        zui.inReal(rect.getWidth()), zui.inReal(rect.getHeight()));
  }

  @Override
  public void refresh() {
    updateTransformation();
  }

  /** Updates the SVG rendering transformation. */
  private void updateTransformation() {
    final AffineTransform at = new AffineTransform();
    zui.transform(at);
    over.setRenderingTransform(at, true);
  }

  /**
   * Transforms an input graphics context with the current translation and
   * scaling.
   * 
   * @param g The input graphics context.
   */
  public void transformGraphics(final Graphics2D g) {
    zui.transform(g);
  }

  /**
   * Zooms towards the center of the display area.
   * 
   * @param factor The zoom factor.
   */
  public void zoom(final double factor) {
    final Rectangle box = over.getVisibleRect();
    zui.zoom(factor, box);
  }

  /**
   * Resets the viewport to a scaling of <code>1.0</code> and
   * <code>(0, 0)</code> being in the center of the component.
   */
  public void reset() {
    final Rectangle2D rect = over.getVisibleRect();
    zui.setTransformation(rect.getCenterX(), rect.getCenterY(), 1);
  }

  /**
   * Resets the viewport to show exactly the given rectangle.
   * 
   * @param bbox The rectangle that is visible.
   */
  public void reset(final Rectangle2D bbox) {
    if(bbox == null) {
      reset();
    } else {
      final Rectangle2D rect = over.getVisibleRect();
      final int nw = (int) (rect.getWidth());
      final int nh = (int) (rect.getHeight());
      zui.setTransformation((nw - bbox.getWidth()) / 2 - bbox.getMinX(),
          (nh - bbox.getHeight()) / 2 - bbox.getMinY(), 1);
      final double rw = nw / bbox.getWidth();
      final double rh = nh / bbox.getHeight();
      final double factor = rw > rh ? rw : rh;
      if(!zui.hasMinZoom()) {
        zui.setMinZoom(factor);
      }
      zoom(factor);
    }
  }

  /**
   * Resets the visible rect and re-determindes the minimal zoom value.
   * 
   * @param newBB The new visible rect.
   */
  public void visibleRectChanged(final Rectangle2D newBB) {
    zui.setMinZoom(-1);
    reset(newBB);
  }

  /**
   * Calculates the component coordinate from the real coordinate.
   * 
   * @param x The real x coordinate.
   * @return The component coordinate.
   */
  public double getXFromCanvas(final double x) {
    return zui.getXFromCanvas(x);
  }

  /**
   * Calculates the component coordinate from the real coordinate.
   * 
   * @param y The real y coordinate.
   * @return The component coordinate.
   */
  public double getYFromCanvas(final double y) {
    return zui.getYFromCanvas(y);
  }

}
