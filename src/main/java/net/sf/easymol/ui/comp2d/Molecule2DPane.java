/*
 * Created on 21 oct. 2004
 *
 */
package net.sf.easymol.ui.comp2d;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ToolTipManager;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import net.sf.easymol.core.AbstractChemicalBond;
import net.sf.easymol.core.AbstractChemicalCompound;
import net.sf.easymol.core.Atom;
import net.sf.easymol.core.EasyMolException;
import net.sf.easymol.core.IChemicalCompoundObserver;
import net.sf.easymol.core.Molecule;
import net.sf.easymol.core.ValencyBond;
import net.sf.easymol.ui.actions.InsertAtomAction;
import net.sf.easymol.ui.actions.InsertValencyBondAction;
import net.sf.easymol.ui.actions.RemoveAction;
import net.sf.easymol.ui.general.IEasyMolPaneHolder;
import net.sf.easymol.ui.general.IMoleculePane;
import net.sf.easymol.ui.general.IconProvider;

import org.jgraph.JGraph;
import org.jgraph.graph.ConnectionSet;
import org.jgraph.graph.DefaultGraphModel;
import org.jgraph.graph.DefaultPort;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphModel;
import org.jgraph.graph.Port;

import com.jgoodies.looks.HeaderStyle;
import com.jgoodies.looks.Options;


/**
 * @author avaughan
 */
public class Molecule2DPane extends JPanel implements IMoleculePane,
        IChemicalCompoundObserver, ActionListener {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private Molecule molecule = null;

    private String moleculeFileName = new String();

    private GraphModel model = null;

    private JGraph graph = null;

    private JLabel valencyBondModeIndicator = new JLabel();

    private JLabel errorIndicator = new JLabel();

    private JTextField moleculeNamer = new JTextField(20);

    private IEasyMolPaneHolder holder = null;

    private boolean modified = false;

    private int valencyBondMode = ValencyBond.SINGLE_BOND;

    public static final int COMPOUND_DEFAULT_WIDTH = 20;

    public static final int COMPOUND_DEFAULT_HEIGHT = 20;

    public Molecule2DPane(Molecule m) {
        model = new DefaultGraphModel();
        graph = new MoleculeGraph(model);
        ToolTipManager.sharedInstance().registerComponent(graph);
        graph.setMarqueeHandler(new Molecule2DPaneMarqueeHandler(this));
        //graph.setPortsVisible(true);
        graph.setGridEnabled(true);
        graph.setGridSize(6);
        graph.setTolerance(2);
        graph.setInvokesStopCellEditing(true);
        graph.setCloneable(true);
        graph.setJumpToDefaultPort(true);
        graph.setGridVisible(true);
        this.setLayout(new BorderLayout());
        this.setMolecule(m);
        this.add("North", createToolBar());
        this.add("Center", new JScrollPane(graph));
        this.add("South", createStatusBar());
        this.createMoleculeGraph();
        moleculeNamer.addActionListener(this);
        moleculeNamer.setText(m.getName());
        m.addObserver(this);
    }

    private JPanel createStatusBar() {
        Border b = null;
        b = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
        JPanel statusBar = new JPanel();
        JPanel modePanel = new JPanel();
        JPanel errorPanel = new JPanel();
        statusBar.setBorder(b);
        this.updateValencyBondModeIndicator();
        this.updateErrorIndicator(null);
        statusBar.setLayout(new FlowLayout(FlowLayout.LEFT, 1, 1));
        modePanel.add(valencyBondModeIndicator);
        errorPanel.add(errorIndicator);
        statusBar.add(modePanel);
        statusBar.add(errorPanel);
        return statusBar;

    }

    private JToolBar createToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(new InsertAtomAction(this, Atom.H));
        toolBar.add(new InsertAtomAction(this, Atom.C));
        toolBar.add(new InsertAtomAction(this, Atom.N));
        toolBar.add(new InsertAtomAction(this, Atom.O));
        toolBar.add(new InsertAtomAction(this, Atom.S));
        toolBar.add(new InsertAtomAction(this, Atom.P));
        toolBar.addSeparator();
        toolBar.add(new InsertValencyBondAction(this, ValencyBond.SINGLE_BOND));
        toolBar.add(new InsertValencyBondAction(this, ValencyBond.DOUBLE_BOND));
        toolBar.add(new InsertValencyBondAction(this, ValencyBond.TRIPLE_BOND));
        toolBar.addSeparator();
        toolBar.add(new RemoveAction(this));
        toolBar.addSeparator();
        toolBar.addSeparator();
        toolBar.add(moleculeNamer);
        toolBar.putClientProperty("JToolBar.isRollover",
                Boolean.TRUE);
        toolBar.putClientProperty(Options.HEADER_STYLE_KEY,
                HeaderStyle.BOTH);
        toolBar.setBorderPainted(false);
        return toolBar;

    }


    public void insertCompound(AbstractChemicalCompound acc, double x, double y) {
        ChemicalCompoundGraphCell oldCell = null;
        Atom a = null;

        // TODO : place this logic outside insertCompound and make it generic so
        // that it can apply to AbstractChemicalCompounds
        //find a default cell to create a valency bond to
        Object cells[] = graph.getRoots();
        for (int i = 0; i < cells.length; i++) {
            if (cells[i] instanceof ChemicalCompoundGraphCell) {
                a = (Atom) ((ChemicalCompoundGraphCell) cells[i]).getCompound();

                //make sure this atom's valency isn't satisfied
                if (a.getSatisfiedValency() < a.getValency()) 
                    oldCell = (ChemicalCompoundGraphCell) cells[i];
            }
        }

        molecule.addCompound(acc);
        Map attributeMap = new Hashtable();
        ChemicalCompoundGraphCell cell = new ChemicalCompoundGraphCell(acc, acc
                .getSymbol());
        Map map = new Hashtable();
        GraphConstants.setBounds(map, new Rectangle2D.Double(x, y,
                COMPOUND_DEFAULT_WIDTH, COMPOUND_DEFAULT_HEIGHT));
        GraphConstants.setSizeable(map, false);
        GraphConstants.setEditable(map, false);
        attributeMap.put(cell, map);
        cell.add(new DefaultPort());
        Object[] insert = new Object[] { cell };
        model.insert(insert, attributeMap, null, null, null);

        //no previous atoms... so no bonds to make
        if (oldCell == null)
            return;

        try {
            insertValencyBond(oldCell, cell, valencyBondMode);
        } catch (EasyMolException e) {
            // e.printStackTrace(); ==> better to do nothing, avoids pollutiong
            // output. maybe send an informative message to the user on why the
            // auto link failed
        }
    }

    public void remove() {
        if (!graph.isSelectionEmpty()) {
            Object[] cells = graph.getSelectionCells();
            for (int i = 0; i < cells.length; i++) {
                if (cells[i] instanceof ChemicalCompoundGraphCell) {
                    molecule
                            .removeCompound(((ChemicalCompoundGraphCell) cells[i])
                                    .getCompound());
                    for (Iterator iter = graph.getModel().edges(cells[i]); iter
                            .hasNext();) {
                        graph.getModel().remove(new Object[] { iter.next() });

                    }
                }
                if (cells[i] instanceof ChemicalBondEdge) {
                    molecule
                            .removeBond(((ChemicalBondEdge) cells[i]).getBond());
                }
            }
            cells = graph.getDescendants(cells);

            graph.getModel().remove(cells);
        }
    }

    public void insertValencyBond(ChemicalCompoundGraphCell cell1,
            ChemicalCompoundGraphCell cell2, int type) throws EasyMolException {
        try {
            Map attributeMap = new Hashtable();
            ValencyBond bond = new ValencyBond((Atom) cell1.getCompound(),
                    (Atom) cell2.getCompound(), type);
            molecule.addBond(bond);
            Map edgeAttrib = new Hashtable();
            GraphConstants.setLineWidth(edgeAttrib, type);
            ChemicalBondEdge edge = new ChemicalBondEdge(bond);
            attributeMap.put(edge, edgeAttrib);
            ConnectionSet cs = new ConnectionSet(edge, getDefaultPort(cell1,
                    model), getDefaultPort(cell2, model));
            Object[] insert = new Object[] { edge };
            model.insert(insert, attributeMap, cs, null, null);
        } catch (ClassCastException e) {
            throw new EasyMolException(201, "Molecule2DPane",
                    "You are trying to create a valency bond between non atom compounds");
        }
    }

    /**
     * @return Returns the molecule.
     */
    public Molecule getMolecule() {
        return molecule;
    }

    /**
     * @param molecule
     *            The molecule to set.
     */
    public void setMolecule(Molecule molecule) {
        this.molecule = molecule;
    }

    public void refresh() {
        if (holder != null) {
            holder.setCurrentPaneName(molecule.getName() + " *");
        }
        modified = true;

    }

    private void createMoleculeGraph() {
        int currentX = 20;

        int currentY = 20;
        Vector rectangles = new Vector(); // of type Rectangle2D.Double;
        //AbstractChemicalCompound acc = null;
        for (var e = molecule.getCompounds(); e.hasNext();) {
            e.next();
            rectangles.add(new Rectangle2D.Double(currentX, currentY,
                    COMPOUND_DEFAULT_WIDTH, COMPOUND_DEFAULT_HEIGHT));
            currentX += 30;
            currentY += 30;
        }

        // Create Nested Map (from Cells to Attributes)
        Map attributes = new Hashtable();

        // create graph vertices (the compounds);
        AbstractChemicalCompound currentCompound = null;
        Hashtable map = new Hashtable();
        int i = 0;
        for (var e = molecule.getCompounds(); e.hasNext();) {
            currentCompound = (AbstractChemicalCompound) e.next();
            ChemicalCompoundGraphCell cell = new ChemicalCompoundGraphCell(
                    currentCompound, currentCompound.getSymbol());
            Map cellAttrib = new Hashtable();
            attributes.put(cell, cellAttrib);
            Rectangle2D cellBounds = (Rectangle2D.Double) rectangles
                    .elementAt(i);
            GraphConstants.setBounds(cellAttrib, cellBounds);
            GraphConstants.setSizeable(cellAttrib, false);
            GraphConstants.setEditable(cellAttrib, false);
            cell.add(new DefaultPort());
            map.put(currentCompound, cell);
            Object[] insert = new Object[] { cell };
            model.insert(insert, attributes, null, null, null);
            i++;
        }
        for (var e = molecule.getBonds(); e.hasNext();) {
            AbstractChemicalBond acb = e.next();
            ChemicalCompoundGraphCell cell1 = (ChemicalCompoundGraphCell) map
                    .get(acb.getFirst());
            ChemicalCompoundGraphCell cell2 = (ChemicalCompoundGraphCell) map
                    .get(acb.getSecond());
            ChemicalBondEdge edge = new ChemicalBondEdge(acb);
            // Create Edge Attributes
            //
            Map edgeAttrib = new Hashtable();
            GraphConstants.setLineWidth(edgeAttrib, acb.getBondStrength());

            attributes.put(edge, edgeAttrib);

            // Connect Edge
            //
            ConnectionSet cs = new ConnectionSet(edge, getDefaultPort(cell1,
                    model), getDefaultPort(cell2, model));
            Object[] cells = new Object[] { edge };

            // Insert into Model
            //
            model.insert(cells, attributes, cs, null, null);

        }
//        if (molecule.getNbBonds() >= 1) {
//            JGraphLayoutAlgorithm layout = new SpringEmbeddedLayoutAlgorithm();
//            layout.createSettings();
//            JGraphLayoutAlgorithm.applyLayout(graph, layout, graph.getRoots());
//        }
    }

    private Port getDefaultPort(Object vertex, GraphModel model) {
        for (int i = 0; i < model.getChildCount(vertex); i++) {
            Object child = model.getChild(vertex, i);
            if (child instanceof Port)
                return (Port) child;
        }
        return null;
    }

    public JGraph getGraph() {
        return graph;
    }

    public int getValencyBondMode() {
        return valencyBondMode;
    }

    public void setValencyBondMode(int valencyBondMode) {
        this.valencyBondMode = valencyBondMode;
        this.updateValencyBondModeIndicator();

    }

    private void updateValencyBondModeIndicator() {
        String iconName = "";
        ImageIcon icon = null;
        String text = " ";
        switch (valencyBondMode) {
        case ValencyBond.SINGLE_BOND:
            iconName = "singleLink";
            text = "Single bond";
            break;
        case ValencyBond.DOUBLE_BOND:
            iconName = "doubleLink";
            text = "Double bond";
            break;
        case ValencyBond.TRIPLE_BOND:
            iconName = "tripleLink";
            text = "Triple bond";
            break;
        }

        icon = IconProvider.getInstance().getIconByName(iconName);
        //valencyBondModeIndicator.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        valencyBondModeIndicator.setToolTipText(text);
        valencyBondModeIndicator.setIcon(icon);
        valencyBondModeIndicator.repaint();

    }

    public void updateErrorIndicator(EasyMolException ex) {
        if (ex == null) {
            errorIndicator.setToolTipText("No error detected");
            errorIndicator.setText(" ");
            errorIndicator.setIcon(IconProvider.getInstance().getIconByName(
                    "ok"));
            //errorIndicator.setBackground(new Color(0,127,0));
        } else {
            errorIndicator.setToolTipText(ex.getMessage());
            errorIndicator.setText(ex.getErrorText());
            errorIndicator.setIcon(IconProvider.getInstance().getIconByName(
                    "error"));
            //errorIndicator.setBackground(new Color(127,0,0));
        }
        errorIndicator.repaint();
    }

    /**
     * @return Returns the moleculeFileName.
     */
    public String getMoleculeFileName() {
        return moleculeFileName;
    }

    /**
     * @param moleculeFileName
     *            The moleculeFileName to set.
     */
    public void setMoleculeFileName(String moleculeFileName) {
        this.moleculeFileName = moleculeFileName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == moleculeNamer) {
            molecule.setName(moleculeNamer.getText());
        }

    }

    /**
     * @return Returns the holder.
     */
    public IEasyMolPaneHolder getHolder() {
        return holder;
    }

    /**
     * @param holder
     *            The holder to set.
     */
    public void setHolder(IEasyMolPaneHolder holder) {
        this.holder = holder;
    }

    /**
     * @return Returns the modified.
     */
    public boolean isModified() {
        return modified;
    }
    /**
     * @param modified The modified to set.
     */
    public void setModified(boolean modified) {
        this.modified = modified;
    }
}
