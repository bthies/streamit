/*
 * Created on Jun 23, 2003
 *
 */
package streamit.eclipse.grapheditor.graph;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.Serializable;
import java.util.Properties;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import org.jgraph.JGraph;
import org.jgraph.graph.DefaultPort;
import org.jgraph.graph.GraphConstants;

import streamit.eclipse.grapheditor.graph.resources.ImageLoader;
import streamit.eclipse.grapheditor.graph.utils.StringTranslator;

/**
 * GEJoiner is the graph editor's internal representation of a joiner.
 * A GEJoiner can have more than one input coming into it. 
 * @author jcarlos
 */
public class GEJoiner extends GEMultiIONode implements Serializable{
	
	
	/**
	 * GEJoiner constructor.
	 * @param name The name of the GEJoiner.
	 * @param weights The weights of the GEJoiner.
	 */
	public GEJoiner (String name, int[] weights)
	{
		super(GEType.JOINER, joinerNameType(name), weights);		
	}
	
	/**
	 * GEJoiner constructor (used when the weights information is not available).
	 * @param name String name of the GEJoiner.
	 */
	public GEJoiner(String name)
	{
		super(GEType.JOINER, joinerNameType(name));
	}
	
	/**
	 * Get the name of the type of the splitter. A splitter can either be a duplicate or a roundrobin.
	 * @return String name of the type of the splitter (either a duplicate or a roundrobin)
	 */
	public static String joinerNameType(String name)
	{
		return "roundrobin";
	}

	/**
	 * Contructs the GEJoiner by setting its label and its draw attributes..
	 * @return GEStreamNode (the GEJoiner).
	 */
	public GEStreamNode construct(GraphStructure graphStruct, int lvl)
	{
		/** Set the label for the GEJoiner */				
		if (weights != null)
		{
			this.setInfo(this.getWeightsAsString());
			this.setUserObject(this.getInfoLabel());
		}
		else 
		{
			this.setUserObject(this.getNameLabel());
		}
		
		/** Initialize the draw attributes of the GEJoiner */
		this.initDrawAttributes(graphStruct, new Rectangle(new Point(100, 100)));
		return this;
	}

	/**
	 * Initialize the default attributes that will be used to draw the GEJoiner.
	 * @param graphStruct The GraphStructure that will have its attributes set.
	 * @param bounds Rectangle that establishes the rectangular bounds where the
	 * GEJoiner will be drawn.
	 */	
	public void initDrawAttributes(GraphStructure graphStruct, Rectangle bounds)
	{
		/** Set the attributes corresponding to the GEJoiner */
		(graphStruct.getAttributes()).put(this, this.attributes);
		GraphConstants.setAutoSize(this.attributes, true);
		GraphConstants.setBounds(this.attributes, bounds);
		GraphConstants.setVerticalTextPosition(this.attributes, JLabel.CENTER);
		
		/** Set the image representation the GEJoiner */
		try 
		{
			ImageIcon icon = ImageLoader.getImageIcon("joiner.GIF");
			GraphConstants.setIcon(this.attributes, icon);
		} 
		catch (Exception ex) 
		{
			ex.printStackTrace();
		}
		
		/** Add the port to the GEJoiner */
		this.port = new DefaultPort();
		this.add(this.port);
		
		/** Insert the GEJoiner into the graph model */
		graphStruct.getGraphModel().insert(new Object[] {this}, null, null, null, null);
		graphStruct.getGraphModel().edit(graphStruct.getAttributes(), graphStruct.getConnectionSet(), null, null);
	}

	
	/**
	 * Get the properties of the GEJoiner.
	 * @return Properties of this GEJoiner.
	 */
	public Properties getNodeProperties()
	{
		Properties properties =  super.getNodeProperties();
		properties.put(GEProperties.KEY_JOINER_WEIGHTS, this.getWeightsAsString());
		return properties;
	}
	
	/**
	 * Set the properties of the GEStreamNode.  
	 * @param properties Properties
	 * @param jgraph JGraph 
	 * @param containerNodes ContainerNodes
	 */
	public void setNodeProperties(Properties properties, JGraph jgraph, ContainerNodes containerNodes)
	{
		super.setNodeProperties(properties, jgraph, containerNodes);
		String joinerWeights = properties.getProperty(GEProperties.KEY_JOINER_WEIGHTS);
		this.setWeights(StringTranslator.weightsToInt(joinerWeights));
		this.setDisplay(jgraph);
	}
	
	/**
	 * Get a clone that of this instance of the GEJoiner.
	 * @return Object that is a clone of this instance of the GEJoiner.
	 */
	public Object clone() 
	{
		GEJoiner clonedJoiner = (GEJoiner) super.clone();
		return clonedJoiner;
	}
	
}
