package infovis.embed;

import infovis.draw.BackgroundRealizer;
import infovis.gui.Context;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.BitSet;

/**
 * Draws spring nodes and interacts with them.
 * 
 * @author Joschi <josua.krause@googlemail.com>
 */
public interface NodeDrawer {

  /**
   * Draws the given node. The graphics device must be translated manually.
   * 
   * @param g The device.
   * @param ctx The canvas context.
   * @param n The node.
   * @param secondarySelected If the node is secondary selected.
   */
  void drawNode(Graphics2D g, Context ctx, SpringNode n, boolean secondarySelected);

  /**
   * Draws the given node. The graphics device must be translated manually.
   * 
   * @param g The device.
   * @param ctx The canvas context.
   * @param n The secondary selected node.
   */
  void drawSecondarySelected(Graphics2D g, Context ctx, SpringNode n);

  /**
   * Draws the edges of the given node. The graphics device must be translated
   * manually.
   * 
   * @param g The device.
   * @param ctx The canvas context.
   * @param n The node.
   * @param secondarySelection If a secondary selection is made, this value is
   *          <code>true</code>.
   */
  void drawEdges(Graphics2D g, Context ctx, SpringNode n, boolean secondarySelection);

  /**
   * Draws the label of a given node. Note that the coordinates are in component
   * coordinates.
   * 
   * @param g The device.
   * @param ctx The canvas context.
   * @param n The node.
   * @param hovered If the node is hovered.
   * @param addText Text to add to the label. If this value is not
   *          <code>null</code> the label will be drawn.
   */
  void drawLabel(Graphics2D g, Context ctx, SpringNode n, boolean hovered, String addText);

  /**
   * Draws the background.
   * 
   * @param g The graphics context.
   * @param ctx The canvas context.
   * @param background How to draw the background.
   */
  void drawBackground(Graphics2D g, Context ctx, BackgroundRealizer background);

  /**
   * Draws the labels of a route.
   * 
   * @param g The graphics context.
   * @param ctx The canvas context.
   * @param n The destination.
   * @param visited The method sets all nodes that are visited.
   */
  void drawRouteLabels(Graphics2D g, Context ctx, SpringNode n, BitSet visited);

  /**
   * A shape defining the area, where a click is associated with the given node.
   * The position of the node must be added manually.
   * 
   * @param n The node.
   * @param real Whether the real position or the position after the complete
   *          animation should be returned.
   * @return The clickable shape of the node.
   */
  Shape nodeClickArea(SpringNode n, boolean real);

  /**
   * The radius of the node.
   * 
   * @param n The node.
   * @return The radius.
   */
  double nodeRadius(SpringNode n);

  /**
   * Defines the tool-tip text for the given node.
   * 
   * @param n The node.
   * @return The tool-tip text or <code>null</code> if none is needed.
   */
  String getTooltipText(SpringNode n);

  /**
   * Drags a single node.
   * 
   * @param n The node.
   * @param startX The original starting x position.
   * @param startY The original starting y position.
   * @param dx The x difference to the original starting point.
   * @param dy The y difference to the original starting point.
   */
  void dragNode(SpringNode n, double startX, double startY, double dx, double dy);

  /**
   * A click on the node occured.
   * 
   * @param n The node.
   */
  void selectNode(SpringNode n);

  /**
   * An iteration over all nodes in the spring embedder system.
   * 
   * @return The iterable.
   */
  Iterable<SpringNode> nodes();

  /**
   * Getter.
   * 
   * @param i The id of the node.
   * @return The node.
   */
  SpringNode getNode(int i);

  /**
   * Is called when the user moves the mouse.
   * 
   * @param cur The current mouse position.
   */
  void moveMouse(Point2D cur);

  /**
   * Calculates the bounding box of the paint area. This method may return
   * <code>null</code> to indicate that the bounding box is not important.
   * 
   * @param background The background.
   * @return The bounding box or <code>null</code>.
   */
  Rectangle2D getBoundingBox(BackgroundRealizer background);

  /**
   * Sets the animator associated with this drawer.
   * 
   * @param animator The animator.
   */
  void setAnimator(Animator animator);

}
