/*
 * Created on Jun 23, 2003
 */
package streamit.eclipse.grapheditor.graph;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Hashtable;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import org.jgraph.JGraph;
import org.jgraph.graph.DefaultPort;
import org.jgraph.graph.GraphConstants;

import streamit.eclipse.grapheditor.graph.resources.ImageLoader;

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
			
		try 
		{
			ImageIcon icon = ImageLoader.getImageIcon("splitter.GIF");
			GraphConstants.setIcon(this.attributes, icon);
		} 
		catch (Exception ex) 
		{
			ex.printStackTrace();
		}
		
		
		this.port = new DefaultPort();
		this.add(this.port);
		graphStruct.getGraphModel().insert(new Object[] {this}, null, null, null, null);
		graphStruct.getGraphModel().edit(graphStruct.getAttributes(), graphStruct.getConnectionSet(), null, null);
		//graphStruct.getJGraph().getGraphLayoutCache().setVisible(new Object[] {this}, true);
	}

	public void setDisplay(JGraph jgraph)
	{
		this.setInfo(this.getWeightsAsString());
		
		Map change = GraphConstants.createMap();
		GraphConstants.setValue(change, this.getInfoLabel());
		Map nest = new Hashtable ();
		nest.put(this, change);
		jgraph.getModel().edit(nest, null, null, null);
	}


	/**
	 * Writes the textual representation of the GEStreamNode to the StringBuffer. 
	 * In this case, the textual representation corresponds to the the StreamIt source code 
	 * equivalent of the GEStreamNode. 
	 * @param strBuff StringBuffer that is used to output the textual representation of the graph.  
	 */
	public void outputCode(StringBuffer strBuff){};
}
