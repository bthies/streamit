package grapheditor.jgraphextension;

import javax.swing.table.DefaultTableModel;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import com.jgraph.graph.DefaultGraphCell;
import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JInternalFrame;
import java.util.HashMap;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentAdapter;

import com.jgraph.JGraph;
import com.jgraph.graph.*;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.BorderFactory;
import javax.swing.JDesktopPane;
import javax.swing.JLayeredPane;
import java.util.Hashtable;
import java.awt.Rectangle;
import java.awt.Color;
import java.util.Map;
import java.util.ArrayList;
import grapheditor.*;


/**
 * LiveJGraphInternalFrame is a frame that serves as a container
 * for JGraph components.
 * 
 * @author jcarlos
 */
public class LiveJGraphInternalFrame extends JInternalFrame 
{
  
	private LiveJGraphInternalFrame _relatedFrame = null;
	private DefaultGraphCell _graphCell = null;
	private JGraph _jgraph = null;	
	protected DefaultGraphModel model = null;
	
	public GraphStructure graphStruct = null;
	
	//added to test changes
	public JDesktopPane desktopPane;

  	public LiveJGraphInternalFrame(JGraph jgraph)
  	{
  		this._jgraph = jgraph;
    	this.setResizable(true);
    	this.setFrameIcon(null);
    	this.setClosable(false);
    	this.setDefaultCloseOperation(JInternalFrame.DISPOSE_ON_CLOSE);
		
		/*
		JPanel mainPanel = new JPanel(new BorderLayout());
		JScrollPane sp = new JScrollPane(_jgraph);
		mainPanel.add(sp,BorderLayout.CENTER);	
		// this.getContentPane().addComponentListener(new CompListener());
		this.getContentPane().add(mainPanel);
	*/			
  	}
  	
	/**
 	* Create and draw on screen <this> with the given name.
 	* @param name Name of the LiveJInternalFrame component
 	*/
  	public void create(String name)
 	{
  		this.setTitle(name);
  	
    	JPanel mainPanel = new JPanel(new BorderLayout());
	    JScrollPane sp = new JScrollPane(_jgraph);
    	mainPanel.add(sp,BorderLayout.CENTER);
    	this.addComponentListener(new FrameComponentListener());
	    this.getContentPane().add(mainPanel);
	   
    	this.pack();
    	this.setSize(320,320);
    	this.setVisible(true);
  	}

	/**
	 * Get the String representation of <this>
	 * @return String representation of LiveJInternalFrame
	 */
  	public String toString()
  	{
    	return("");
  	}

	/**
 	* Set the DefaultGraphCell of <this> 
 	* @param graphCell DefaultGraphCell corresponding fo LiveJInternalFrame
 	*/
  	public void setGraphCell(DefaultGraphCell graphCell)
  	{
    	_graphCell = graphCell;
  	}

	/**
 	* Get the DefaultGraphCell corresponding to <this>
 	* @return DefaultGraphCell of the LiveJInternalFrame
 	*/
  	public DefaultGraphCell getGraphCell()
  	{
    	return(_graphCell);
  	}
  	
  	/**
  	 * Set the DefaulltGraphModel belonging to <this>
  	 * @param model The DefaultGraphModel of the LiveJGraphInternalFrame.
  	 */
  	public void setGraphModel(DefaultGraphModel model)
  	{
  		this.model = model;
  	}
  	
  	/**
  	 * Get the DefaultGraphModel belonging to <this> 
  	 * @return The DefaultGraphModel of the LiveJGraphInternalFrame.
  	 */
  	public DefaultGraphModel getGraphModel(DefaultGraphModel model)
  	{
  		return this.model;
  	}
  	
  	
  	public JDesktopPane getDesktopPane()
  	{
  		return this.desktopPane;
  	}
  
  	public void setGraphStruct(GraphStructure graphStruct)
  	{
  		this.graphStruct = graphStruct;
  	}
  
	/*
	private class CompListener extends ComponentAdapter {
	  public void componentResized(ComponentEvent ce)
	  {
		_canvas.setSize(_desktopPane.getSize());
		_canvas.updateUI();
		
	  }
	}
*/
	private class FrameComponentListener extends ComponentAdapter {
	  public void componentResized(ComponentEvent ce)
	  {
	  	System.out.println("Entered FrameComponentLinter Resize method");
		HashMap map = new HashMap();
		Map atts = GraphConstants.createMap();
		
		System.out.println("Getting the component");
		LiveJGraphInternalFrame frame = (LiveJGraphInternalFrame)ce.getComponent();
		
		System.out.println("Setting the bounds");
		GraphConstants.setBounds(atts,frame.getBounds());
		
		
		System.out.println("Putting the attributes into the map");
		map.put(frame.getGraphCell(), atts);
		
		
		if  (model !=null)
		{
			System.out.println("Editing the model");	
			model.edit(map,null,null,null);
		}
		else
		{
			System.out.println("Model is null");
		}
		
		
	  }

	  public void componentMoved(ComponentEvent ce)
	  {
		System.out.println("Entered FrameComponentLinter Move method");
		HashMap map = new HashMap();
		Map atts = GraphConstants.createMap();
		LiveJGraphInternalFrame frame = (LiveJGraphInternalFrame)ce.getComponent();
		
		//GraphConstants.setBounds(atts,frame.getBounds());
		GraphConstants.setBounds(graphStruct.getAttributes(),frame.getBounds());
		
		//map.put(frame.getGraphCell(),atts);
		map.put(frame.getGraphCell(),graphStruct.getAttributes());
		
		model.edit(map,null,null,null);
	  }
	}
  	
  	
  	
  	
  	
}
