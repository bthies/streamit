package grapheditor.jgraphextension;
import java.util.Map;
import java.util.HashMap;
import com.jgraph.graph.GraphConstants;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.table.DefaultTableModel;
import com.jgraph.JGraph;
import com.jgraph.graph.ConnectionSet;
import com.jgraph.graph.DefaultEdge;
import com.jgraph.graph.DefaultPort;
import com.jgraph.graph.DefaultGraphCell;
import com.jgraph.graph.DefaultGraphModel;
import javax.swing.event.InternalFrameEvent;
import java.beans.PropertyVetoException;
import javax.swing.event.InternalFrameAdapter;
import java.util.ArrayList;
import javax.swing.*;
import java.awt.event.*;
import java.awt.BorderLayout;

public class LiveJGraphDemo extends JFrame {
  private JDesktopPane _desktopPane = null;
  private FrameSelectionListener _fsl = null;
  private FrameComponentListener _fcl = null;
  private AddParentInternalFrameAction _apifa = null;
  private AddChildInternalFrameAction _acifa = null;
  private DefaultGraphModel _graph = null;
  private JPanel _canvas = null;
  private ComponentListener _cl = null;

  private DefaultGraphModel model = null;


  public LiveJGraphDemo()
  {
    super("Live JGraph Demo");
    
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    _fsl = new FrameSelectionListener();
    _fcl = new FrameComponentListener();
    _cl = new CompListener();
    _graph = new DefaultGraphModel();
    AddRootInternalFrameAction arifa = new AddRootInternalFrameAction();
    _apifa = new AddParentInternalFrameAction();
    _apifa.setEnabled(false);
    _acifa = new AddChildInternalFrameAction();
    _acifa.setEnabled(false);
  
 	JPanel mainPanel = new JPanel(new BorderLayout());
    _desktopPane = new JDesktopPane();
    _desktopPane.addComponentListener(_cl);
    _canvas = new JPanel(new BorderLayout());

     JGraph graphComp = new JGraph(_graph);


    _canvas.add(graphComp,BorderLayout.CENTER);
    _desktopPane.add(_canvas,JLayeredPane.FRAME_CONTENT_LAYER);
    mainPanel.add(_desktopPane,BorderLayout.CENTER);
    getContentPane().add(mainPanel);
    
    
    
    JMenuBar menuBar = new JMenuBar();
    JMenu menu = new JMenu("File");
    JMenuItem menuItem = new JMenuItem("Exit");
    menuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e)
      {
        System.exit(0);
      }
      });
    menu.add(menuItem);
    menuBar.add(menu);
    menu = new JMenu("Components");
    menu.add(arifa);
    menu.add(_apifa);
    menu.add(_acifa);
    menuBar.add(menu);
    setJMenuBar(menuBar);
    pack();
    setSize(500,700);
    setVisible(true);
  }
 
	public JDesktopPane getDesktopPane()
	{
		return this._desktopPane;
	}
	public void setDesktopPane(JDesktopPane pane)
	{
			this._desktopPane = pane;
	}
	
	
	public void setDefaultGraphModel(DefaultGraphModel model)
	{
		this.model = model;
	}

  private class AddRootInternalFrameAction extends AbstractAction {
    public AddRootInternalFrameAction()
    {
      super("Add Root Component",new ImageIcon("images/plus.gif"));
      putValue(Action.SHORT_DESCRIPTION,"Add Root Component");
    }

    public void actionPerformed(ActionEvent ae)
    {
    	/*
	  	JGraph thegraph = new JGraph(model);
    
      	//Create initial internal frame
		LiveJGraphInternalFrame internalFrame = new LiveJGraphInternalFrame(thegraph);
		System.out.println("ADDED THE MODEL TO THE FRAME");
      	internalFrame.create();
      	
      	//Add to graph model
      	DefaultGraphCell insertCell = new DefaultGraphCell(internalFrame);
      	internalFrame.setGraphCell(insertCell);
      	Object insertCells[] = new Object[] {insertCell};
      	_graph.insert(insertCells,null,null,null,null);
      	internalFrame.addInternalFrameListener(_fsl);
      	internalFrame.addComponentListener(_fcl);
      	_desktopPane.add(internalFrame);
      try 
      {
        internalFrame.setSelected(true);
      } catch(PropertyVetoException pve) {
      }
      */
    }
  }

  private class AddChildInternalFrameAction extends AbstractAction {
    public AddChildInternalFrameAction()
    {
      super("Add Child Component",new ImageIcon("images/add_down.gif"));
      putValue(Action.SHORT_DESCRIPTION,"Add Child Component");
    }

    public void actionPerformed(ActionEvent ae)
    {
    	/*	
    	
      //Get currently selected internal frame
      LiveJGraphInternalFrame currentSelectedFrame = (LiveJGraphInternalFrame)_desktopPane.getSelectedFrame();
      
      //Create initial internal frame
      LiveJGraphInternalFrame internalFrame = new LiveJGraphInternalFrame();
      internalFrame.create();
      
      //Add to graph model
      DefaultGraphCell insertCell = new DefaultGraphCell(internalFrame);
      internalFrame.setGraphCell(insertCell);
      Object insertCells[] = new Object[] {insertCell};
      _graph.insert(insertCells,null,null,null,null);
      DefaultGraphCell parentCell = currentSelectedFrame.getGraphCell();
      DefaultPort parentPort = new DefaultPort();
      parentCell.add(parentPort);
      DefaultPort childPort = new DefaultPort();
      insertCell.add(childPort);
      DefaultEdge edge = new DefaultEdge();
      HashMap map = new HashMap();
      Map atts = GraphConstants.createMap();
      GraphConstants.setLineEnd(atts,GraphConstants.ARROW_CLASSIC);
      GraphConstants.setEndFill(atts,true);
      map.put(edge,atts);
      ConnectionSet cs = new ConnectionSet(edge,parentPort,childPort);
      Object insertEdges[] = new Object[] {edge};
      _graph.insert(insertEdges,map,cs,null,null);
      internalFrame.addInternalFrameListener(_fsl);
      internalFrame.addComponentListener(_fcl);
      _desktopPane.add(internalFrame);
      try 
      {
        internalFrame.setSelected(true);
      } 
      catch(PropertyVetoException pve) 
      {}
      */
    }
  }

  private class AddParentInternalFrameAction extends AbstractAction {
    public AddParentInternalFrameAction()
    {
      super("Add Parent Component",new ImageIcon("images/add_up.gif"));
      putValue(Action.SHORT_DESCRIPTION,"Add Parent Component");
    }

    public void actionPerformed(ActionEvent ae)
    {
    	/*
      //Get currently selected internal frame
      LiveJGraphInternalFrame currentSelectedFrame = (LiveJGraphInternalFrame)_desktopPane.getSelectedFrame();
      
      //Create initial internal frame
      LiveJGraphInternalFrame internalFrame = new LiveJGraphInternalFrame();
      internalFrame.create();
      
      //Add to graph model
      DefaultGraphCell insertCell = new DefaultGraphCell(internalFrame);
      internalFrame.setGraphCell(insertCell);
      Object insertCells[] = new Object[] {insertCell};
      _graph.insert(insertCells,null,null,null,null);
      DefaultGraphCell childCell = currentSelectedFrame.getGraphCell();
      DefaultPort childPort = new DefaultPort();
      childCell.add(childPort);
      DefaultPort parentPort = new DefaultPort();
      insertCell.add(parentPort);
      DefaultEdge edge = new DefaultEdge();
      HashMap map = new HashMap();
      Map atts = GraphConstants.createMap();
      GraphConstants.setLineEnd(atts,GraphConstants.ARROW_CLASSIC);
      GraphConstants.setEndFill(atts,true);
      map.put(edge,atts);
      ConnectionSet cs = new ConnectionSet(edge,parentPort,childPort);
      Object insertEdges[] = new Object[] {edge};
      _graph.insert(insertEdges,map,cs,null,null);
      
      internalFrame.addInternalFrameListener(_fsl);
      internalFrame.addComponentListener(_fcl);
      _desktopPane.add(internalFrame);
      
      try 
      {
        internalFrame.setSelected(true);
      } catch(PropertyVetoException pve) {
      }
      */
    }
  }

  private class FrameSelectionListener extends InternalFrameAdapter {
    public void internalFrameActivated(InternalFrameEvent ife)
    {
      _apifa.setEnabled(true);
      _acifa.setEnabled(true);
    }

    public void internalFrameDeactivated(InternalFrameEvent ife)
    {
      _apifa.setEnabled(false);
      _acifa.setEnabled(false);
    }
  }

  private class CompListener extends ComponentAdapter {
    public void componentResized(ComponentEvent ce)
    {
      _canvas.setSize(_desktopPane.getSize());
      _canvas.updateUI();
    }
  }

  private class FrameComponentListener extends ComponentAdapter {
    public void componentResized(ComponentEvent ce)
    {
      HashMap map = new HashMap();
      Map atts = GraphConstants.createMap();
      LiveJGraphInternalFrame frame = (LiveJGraphInternalFrame)ce.getComponent();
      GraphConstants.setBounds(atts,frame.getBounds());
      map.put(frame.getGraphCell(),atts);
      _graph.edit(map,null,null,null);
    }

    public void componentMoved(ComponentEvent ce)
    {
      HashMap map = new HashMap();
      Map atts = GraphConstants.createMap();
      LiveJGraphInternalFrame frame = (LiveJGraphInternalFrame)ce.getComponent();
      GraphConstants.setBounds(atts,frame.getBounds());
      map.put(frame.getGraphCell(),atts);
      _graph.edit(map,null,null,null);
    }
  }
}
