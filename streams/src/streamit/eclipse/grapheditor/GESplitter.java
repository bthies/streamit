/*
 * Created on Jun 23, 2003
 */
package streamit.eclipse.grapheditor;

import java.awt.Color;
import java.io.Serializable;
import java.util.ArrayList;

import javax.swing.BorderFactory;

import com.jgraph.JGraph;
import com.jgraph.graph.DefaultPort;
import com.jgraph.graph.GraphConstants;

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
	public GEStreamNode construct(GraphStructure graphStruct, int level)
	{
		System.out.println("Constructing the Splitter " +this.getName());
		
		if (weights != null)
		{
			this.setInfo(this.getWeightsAsString());
			this.setUserObject(this.getInfoLabel());
		}
		else 
		{
			this.setUserObject(this.getNameLabel());
		}
		
		this.initDrawAttributes(graphStruct);	
		return this;
	}

	/**
	 * Initialize the default attributes that will be used to draw the GESplitter.
	 * @param graphStruct The GraphStructure that will have its attributes set.
	 */	
	public void initDrawAttributes(GraphStructure graphStruct)
	{
		(graphStruct.getAttributes()).put(this, this.attributes);
		GraphConstants.setAutoSize(this.attributes, true);
		GraphConstants.setBorder(this.attributes , BorderFactory.createRaisedBevelBorder());
		GraphConstants.setBackground(this.attributes, Color.orange);
		
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

}
