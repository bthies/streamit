/*
 * Created on Jun 23, 2003
 *
 */
package streamit.eclipse.grapheditor.graph;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.PrintWriter;
import java.io.Serializable;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import org.jgraph.graph.DefaultPort;
import org.jgraph.graph.GraphConstants;

import streamit.eclipse.grapheditor.graph.resources.ImageLoader;

/**
 * GEJoiner is the graph editor's internal representation of a joiner.
 * @author jcarlos
 */
public class GEJoiner extends GEStreamNode implements Serializable{
	
	/**
	 * The weights corresponding to the splitter.
	 */	
	private int[] weights;

	/**
	 * GEJoiner constructor.
	 * @param name The name of the GEJoiner.
	 * @param weights The weights of the GEJoiner.
	 */
	public GEJoiner (String name, int[] weights)
	{
		super(GEType.JOINER, name);
		this.name = name;
		this.weights = weights;
	}
	/**
	 * GEJoiner constructor (used when the weights information is not available).
	 * @param name The name of the GEJoiner.
	 */
	public GEJoiner(String name)
	{
		super(GEType.JOINER, name);
		this.name = name;
		weights = null;
	}
	
	/**
	 * Get the weights of this 
	 * @return The weights corresponding to the GEJoiner
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
	 * @return String representation of the weights of the GEJoiner.
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
	 * Contructs the joiner and returns <this>.
	 * @return <this>
	 */
	public GEStreamNode construct(GraphStructure graphStruct, int lvl)
	{
		System.out.println("Constructing the Joiner " +this.getName());
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
		
		this.initDrawAttributes(graphStruct, new Rectangle(new Point(100, 100)));
		return this;
	}

	/**
	 * Initialize the default attributes that will be used to draw the GEJoiner.
	 * @param graphStruct The GraphStructure that will have its attributes set.
	 */	
	public void initDrawAttributes(GraphStructure graphStruct, Rectangle bounds)
	{
		(graphStruct.getAttributes()).put(this, this.attributes);
		GraphConstants.setAutoSize(this.attributes, true);
		GraphConstants.setBounds(this.attributes, bounds);
		GraphConstants.setVerticalTextPosition(this.attributes, JLabel.CENTER);
				
		try 
		{
			ImageIcon icon = ImageLoader.getImageIcon("joiner.GIF");
			GraphConstants.setIcon(this.attributes, icon);
		} 
		catch (Exception ex) 
		{
			ex.printStackTrace();
		}
		
		this.port = new DefaultPort();
		this.add(this.port);
		graphStruct.getGraphModel().insert(new Object[] {this}, null, null, null, null);
		//graphStruct.getJGraph().getGraphLayoutCache().setVisible(new Object[] {this}, true);
	}
	
	
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
	
	/**
	 * Writes the textual representation of the GEStreamNode using the PrintWriter specified by out. 
	 * In this case, the textual representation corresponds to the the StreamIt source code 
	 * equivalent of the GEStreamNode. 
	 * @param out PrintWriter that is used to output the textual representation of the graph.  
	 */
	public void outputCode(PrintWriter out){};
	
}
