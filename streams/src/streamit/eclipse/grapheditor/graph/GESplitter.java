/*
 * Created on Jun 23, 2003
 */
package streamit.eclipse.grapheditor.graph;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Properties;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import org.jgraph.JGraph;
import org.jgraph.graph.DefaultPort;
import org.jgraph.graph.GraphConstants;

import streamit.eclipse.grapheditor.graph.resources.ImageLoader;
import streamit.eclipse.grapheditor.graph.utils.StringTranslator;

/**
 * GESplitter is the graph editor's internal representation of a splitter.
 * A GESplitter can have more than one output coming out from it.
 * 
 * @author jcarlos
 */
public class GESplitter extends GEMultiIONode implements Serializable{
	


	/**
	 * GESplitter constructor.
	 * @param name The name of the GESplitter.
	 * @param weights The weights of the GESplitter.
	 */
	public GESplitter(String name,int[] weights)
	{
		super(GEType.SPLITTER , splitterNameType(name), weights);
	}
	
	/**
	 * GESplitter constructor.
	 * @param name The name of the GESplitter.
	 */
	public GESplitter(String name)
	{
		super(GEType.SPLITTER , splitterNameType(name));
		
	}
	
	/**
	 * Get the name of the type of the splitter. A splitter can either be a duplicate or a roundrobin.
	 * @return String name of the type of the splitter (either a duplicate or a roundrobin)
	 */
	public static String splitterNameType(String name)
	{
		int indexUnderscore = name.indexOf("_");
		
		/** If no underscore was encountered, then the name should remain the same
		 *  (there was no underscore added during the compilation process) */
		if (indexUnderscore == -1)
		{
			return name;
		}
		/** An underscore was encountered, so we have to determine, if the splitter
		 *	is a duplicate or a roundrobin by looking at the substring up to the first
		 *	underscore. */
		else
		{
			if (name.substring(0,indexUnderscore).toUpperCase().equals("DUPLICATE"))
			{
				return "duplicate";
			}
			else 
			{
				return "roundrobin";
			}
		}
	}


	
	/**
	 * Construct the GESplitter. 
	 * @return this GEStreamNode.
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
		/** Initialize the draw attributes of the GESplitter */
		this.initDrawAttributes(graphStruct, new Rectangle(new Point(100,100)));	
		return this;
	}

	/**
	 * Initialize the default attributes that will be used to draw the GESplitter.
	 * @param graphStruct The GraphStructure that will have its attributes set.
	 */	
	public void initDrawAttributes(GraphStructure graphStruct, Rectangle bounds)
	{
		/** Set the attributes corresponding to the GESplitter */
		(graphStruct.getAttributes()).put(this, this.attributes);
		GraphConstants.setAutoSize(this.attributes, true);
		GraphConstants.setBounds(this.attributes, bounds);
		GraphConstants.setVerticalTextPosition(this.attributes, JLabel.CENTER);
		
		/** Set the image representation the GESplitter */
		try 
		{
			ImageIcon icon = ImageLoader.getImageIcon("splitter.GIF");
			GraphConstants.setIcon(this.attributes, icon);
		} 
		catch (Exception ex) 
		{
			ex.printStackTrace();
		}
		
		/** Add the port to the GESplitter */
		this.port = new DefaultPort();
		this.add(this.port);
		
		/** Insert the GESplitter into the graph model */
		graphStruct.getGraphModel().insert(new Object[] {this}, null, null, null, null);
		graphStruct.getGraphModel().edit(graphStruct.getAttributes(), graphStruct.getConnectionSet(), null, null);
		
	}


	/**
	 * Get the properties of the GESplitter.
	 * @return Properties of this GESplitter.
	 */
	public Properties getNodeProperties()
	{
		Properties properties =  super.getNodeProperties();
		properties.put(GEProperties.KEY_SPLITTER_WEIGHTS, this.getWeightsAsString());
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
		String splitWeights = properties.getProperty(GEProperties.KEY_SPLITTER_WEIGHTS);
		this.setWeights(StringTranslator.weightsToInt(splitWeights));
		this.setDisplay(jgraph);
	}

	/**
	 * Get a clone that of this instance of the GESplitter.
	 * @return Object that is a clone of this instance of the GESplitter.
	 */
	public Object clone() 
	{
		GESplitter clonedSplitter = (GESplitter) super.clone();
		return clonedSplitter;
	}

}
