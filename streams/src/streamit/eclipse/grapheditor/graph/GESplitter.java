/*
 * Created on Jun 23, 2003
 */
package streamit.eclipse.grapheditor.graph;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;

import javax.swing.JLabel;

import org.jgraph.JGraph;
import org.jgraph.graph.DefaultPort;
import org.jgraph.graph.GraphConstants;

/**
 * GESplitter is the graph editor's internal representation of a splitter.
 * @author jcarlos
 */
public class GESplitter extends GEStreamNode implements Serializable{
	
	/**
	 * The weights corresponding to the splitter.
	 */
	private int[] weights;

	/**
	 * GESplitter constructor.
	 * @param name The name of the GESplitter.
	 * @param weights The weights of the GESplitter.
	 */
	public GESplitter(String name,int[] weights)
	{
		super(GEType.SPLITTER , name);
		this.name = name;
		this.weights = weights;
		
	}
	
	/**
	 * GESplitter constructor.
	 * @param name The name of the GESplitter.
	 */
	public GESplitter(String name)
	{
		super(GEType.SPLITTER , name);
		this.name = name;
		this.weights = null;
	}
	
	/**
	 * Get the weights of this 
	 * @return The weights corresponding to the GESplitter
	 */
	public int[] getWeights()
	{
		return this.weights;
	}
	
	/**
	 * Set the weights of the GESplitter
	 * @param weigths 
	 */
	public void setWeights(int[] weights)
	{
		this.weights = weights;
	}
	
	/**
	 * Get the weight as a string of the form: "(weight1, weight2, weight3,... , weightN)".
	 * @return String representation of the weights of the GESplitter.
	 */
	public String getWeightsAsString()
	{
		String strWeight = "(";
		for(int i = 0; i < this.weights.length; i++)
		{
			if (i != 0)
			{
				strWeight += ", ";
			}
			strWeight += this.weights[i];
		
		}
		
		strWeight += ")";
		return strWeight;
	}

	/**
	 * Construct the GESplitter and return <this>. 
	 * @return <this>.
	 */
	public GEStreamNode construct(GraphStructure graphStruct, int lvl)
	{
		System.out.println("Constructing the Splitter " +this.getName());
		
		this.level = lvl;
		if (weights != null)
		{
			this.setInfo(this.getWeightsAsString());
			this.setUserObject(this.getInfoLabel());
		}
		else 
		{
			this.setUserObject(this.getNameLabel());
		}
		
		this.initDrawAttributes(graphStruct, new Rectangle(new Point(100,100)));	
		return this;
	}

	/**
	 * Initialize the default attributes that will be used to draw the GESplitter.
	 * @param graphStruct The GraphStructure that will have its attributes set.
	 */	
	public void initDrawAttributes(GraphStructure graphStruct, Rectangle bounds)
	{
		(graphStruct.getAttributes()).put(this, this.attributes);
		GraphConstants.setAutoSize(this.attributes, true);
		GraphConstants.setBounds(this.attributes, bounds);
		GraphConstants.setVerticalTextPosition(this.attributes, JLabel.CENTER);
		
		/* demoremove
		try 
		{
			ImageIcon icon = ImageLoader.getImageIcon("splitter.GIF");
			GraphConstants.setIcon(this.attributes, icon);
		} 
		catch (Exception ex) 
		{
			ex.printStackTrace();
		}*/
		//demoadd
		GraphConstants.setBorderColor(this.attributes, Color.green.darker());
		
		this.port = new DefaultPort();
		this.add(this.port);
		graphStruct.getCells().add(this);
	}

	/**
	 * Draw this Splitter
	 */
	public void draw()
	{
		System.out.println("Drawing the Splitter " +this.getName());
	}
		
	public void collapseExpand(JGraph jgraph){};
	public void collapse(JGraph jgraph){};
	public void expand(JGraph jgraph){};
	
	/**
	 * Hide the GEStreamNode in the display. Note that some nodes cannot be hidden or 
	 * they cannot be made visible.
	 * @return true if it was possible to hide the node; otherwise, return false.
	 */
	public boolean hide()
	{
		return false;
	}

	/**
	 * Make the GEStreamNode visible in the display. Note that some nodes cannot be hidden or 
	 * they cannot be made visible. 
	 * @return true if it was possible to make the node visible; otherwise, return false.
	 */	
	public boolean unhide()
	{
		return false;
	};

	/** Returns a list of nodes that are contained by this GEStreamNode. If this GEStreamNode is
	 * not a container node, then a list with no elements is returned.
	 * @return ArrayList of contained elements. If <this> is not a container, return empty list.
	 */
	public ArrayList getContainedElements(){return new ArrayList();};


	/**
	 * Writes the textual representation of the GEStreamNode using the PrintWriter specified by out. 
	 * In this case, the textual representation corresponds to the the StreamIt source code 
	 * equivalent of the GEStreamNode. 
	 * @param out PrintWriter that is used to output the textual representation of the graph.  
	 */
	public void outputCode(PrintWriter out){};
}
