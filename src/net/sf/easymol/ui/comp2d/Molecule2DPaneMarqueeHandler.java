/*
 * Created on 31 oct. 2004
 *
 */
package net.sf.easymol.ui.comp2d;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import net.sf.easymol.core.Atom;
import net.sf.easymol.core.EasyMolException;
import net.sf.easymol.core.ValencyBond;
import net.sf.easymol.ui.actions.InsertAtomAction;
import net.sf.easymol.ui.actions.InsertValencyBondAction;
import net.sf.easymol.ui.actions.RemoveAction;

import org.jgraph.JGraph;
import org.jgraph.graph.BasicMarqueeHandler;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.PortView;


/**
 * @author avaughan
 */
public class Molecule2DPaneMarqueeHandler extends BasicMarqueeHandler {
    // Holds the Start and the Current Point
    protected Point2D start, current = null;

    // Holds the First and the Current Port
    protected PortView port, firstPort = null;

    protected Molecule2DPane moleculePane = null;

    protected JGraph graph = null;

    public Molecule2DPaneMarqueeHandler(Molecule2DPane pane) {
        moleculePane = pane;
        graph = moleculePane.getGraph();
    }

    // Override to Gain Control (for PopupMenu and ConnectMode)
    public boolean isForceMarqueeEvent(MouseEvent e) {
        if (e.isShiftDown())
            return false;
        // If Right Mouse Button we want to Display the PopupMenu
        if (SwingUtilities.isRightMouseButton(e))
            // Return Immediately
            return true;
        // Find and Remember Port
        port = getSourcePortAt(e.getPoint());
        // If Port Found and in ConnectMode (=Ports Visible)
        if (port != null && graph.isPortsVisible())
            return true;
        // Else Call Superclass
        return super.isForceMarqueeEvent(e);
    }

    // Display PopupMenu or Remember Start Location and First Port
    public void mousePressed(final MouseEvent e) {
        // If Right Mouse Button
        if (SwingUtilities.isRightMouseButton(e)) {
            // Find Cell in Model Coordinates
            Object cell = graph.getFirstCellForLocation(e.getX(), e.getY());
            //Create PopupMenu for the Cell
            JPopupMenu menu = createPopupMenu(e.getPoint(), cell);
            // Display PopupMenu
            menu.show(graph, e.getX(), e.getY());
            // Else if in ConnectMode and Remembered Port is Valid
        } else if (port != null /* && graph.isPortsVisible() */) {
            // Remember Start Location
            start = graph.toScreen(port.getLocation(null));
            // Remember First Port
            firstPort = port;
        } else {
            // Call Superclass
            super.mousePressed(e);
        }
    }

    // Find Port under Mouse and Repaint Connector
    public void mouseDragged(MouseEvent e) {
        // If remembered Start Point is Valid
        if (start != null) {
            // Fetch Graphics from Graph
            Graphics g = graph.getGraphics();
            // Reset Remembered Port
            PortView newPort = getTargetPortAt(e.getPoint());
            // Do not flicker (repaint only on real changes)
            if (newPort == null || newPort != port) {
                // Xor-Paint the old Connector (Hide old Connector)
                paintConnector(Color.black, graph.getBackground(), g);
                // If Port was found then Point to Port Location
                port = newPort;
                if (port != null)
                    current = graph.toScreen(port.getLocation(null));
                // Else If no Port was found then Point to Mouse Location
                else
                    current = graph.snap(e.getPoint());
                // Xor-Paint the new Connector
                paintConnector(graph.getBackground(), Color.black, g);
            }
        }
        // Call Superclass
        super.mouseDragged(e);
    }

    public PortView getSourcePortAt(Point2D point) {
        // Disable jumping
        graph.setJumpToDefaultPort(false);
        PortView result;
        try {
            // Find a Port View in Model Coordinates and Remember
            result = graph.getPortViewAt(point.getX(), point.getY());
        } finally {
            graph.setJumpToDefaultPort(true);
        }
        return result;
    }

    // Find a Cell at point and Return its first Port as a PortView
    protected PortView getTargetPortAt(Point2D point) {
        // Find a Port View in Model Coordinates and Remember
        return graph.getPortViewAt(point.getX(), point.getY());
    }

    // Connect the First Port and the Current Port in the Graph or Repaint
    public void mouseReleased(MouseEvent e) {
        // If Valid Event, Current and First Port
        if (e != null && port != null && firstPort != null && firstPort != port) {
            // Then Establish Connection
            try {
                moleculePane.insertValencyBond(
                        (ChemicalCompoundGraphCell) firstPort.getParentView()
                                .getCell(), (ChemicalCompoundGraphCell) port
                                .getParentView().getCell(), moleculePane
                                .getValencyBondMode());
                moleculePane.updateErrorIndicator(null);
            } catch (EasyMolException ex) {
                //ex.printStackTrace();
                moleculePane.updateErrorIndicator(ex);
                graph.repaint();
            }
            // Else Repaint the Graph
        } else
            graph.repaint();
        // Reset Global Vars
        firstPort = port = null;
        start = current = null;
        // Call Superclass
        super.mouseReleased(e);
    }

    // Show Special Cursor if Over Port
    public void mouseMoved(MouseEvent e) {
        // Check Mode and Find Port
        if (e != null && getSourcePortAt(e.getPoint()) != null
                && graph.isPortsVisible()) {
            // Set Cusor on Graph (Automatically Reset)
            graph.setCursor(new Cursor(Cursor.HAND_CURSOR));
            // Consume Event
            // Note: This is to signal the BasicGraphUI's
            // MouseHandle to stop further event processing.
            e.consume();
        } else
            // Call Superclass
            super.mouseMoved(e);
    }

    // Use Xor-Mode on Graphics to Paint Connector
    protected void paintConnector(Color fg, Color bg, Graphics g) {
        // Set Foreground
        g.setColor(fg);
        // Set Xor-Mode Color
        g.setXORMode(bg);
        // Highlight the Current Port
        paintPort(graph.getGraphics());
        // If Valid First Port, Start and Current Point
        if (firstPort != null && start != null && current != null)
            // Then Draw A Line From Start to Current Point
            g.drawLine((int) start.getX(), (int) start.getY(), (int) current
                    .getX(), (int) current.getY());

    }

    // Use the Preview Flag to Draw a Highlighted Port
    protected void paintPort(Graphics g) {
        // If Current Port is Valid
        if (port != null) {
            // If Not Floating Port...
            boolean o = (GraphConstants.getOffset(port.getAttributes()) != null);
            // ...Then use Parent's Bounds
            Rectangle2D r = (o) ? port.getBounds() : port.getParentView()
                    .getBounds();
            // Scale from Model to Screen
            r = graph.toScreen((Rectangle2D) r.clone());
            // Add Space For the Highlight Border
            r.setFrame(r.getX() - 3, r.getY() - 3, r.getWidth() + 6, r
                    .getHeight() + 6);
            // Paint Port in Preview (=Highlight) Mode
            graph.getUI().paintCell(g, port, r, true);
        }
    }

    public JPopupMenu createPopupMenu(final Point pt, final Object cell) {
        JPopupMenu menu = new JPopupMenu();
        // Remove
        if (!graph.isSelectionEmpty()) {
            menu.addSeparator();
            menu.add(new RemoveAction(moleculePane));
            menu.addSeparator();
        }
        // Insert
        menu.add(new InsertAtomAction(moleculePane, Atom.H));
        menu.add(new InsertAtomAction(moleculePane, Atom.C));
        menu.add(new InsertAtomAction(moleculePane, Atom.N));
        menu.add(new InsertAtomAction(moleculePane, Atom.O));
        menu.add(new InsertAtomAction(moleculePane, Atom.S));
        menu.add(new InsertAtomAction(moleculePane, Atom.P));
        menu.addSeparator();
        menu.add(new InsertValencyBondAction(moleculePane,
                ValencyBond.SINGLE_BOND));
        menu.add(new InsertValencyBondAction(moleculePane,
                ValencyBond.DOUBLE_BOND));
        menu.add(new InsertValencyBondAction(moleculePane,
                ValencyBond.TRIPLE_BOND));
        return menu;
    }

}