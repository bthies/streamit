/*
 * Created on Jun 23, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package grapheditor;

import java.io.*;

import com.jgraph.graph.*;
import java.awt.Color;
import javax.swing.BorderFactory; 


/**
 * GESplitter is the graph editor's internal representation of a splitter.
 * @author jcarlos
 */
public class GESplitter extends GEStreamNode implements Serializable{
	
	private String label;
	private int[] weights;

	public GESplitter(String label,int[] weights)
	{
		super(GEType.SPLITTER , label);
		this.label = label;
		this.weights = weights;
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
	 * Contructs the Splitter 
	 */
	public GEStreamNode construct(GraphStructure graphStruct)
	{
		System.out.println("Constructing the Splitter " +this.getName());
		
		(graphStruct.getAttributes()).put(this, this.attributes);
		GraphConstants.setAutoSize(this.attributes, true);
		GraphConstants.setBounds(this.attributes, graphStruct.setRectCoords(this));
		GraphConstants.setBorder(this.attributes , BorderFactory.createRaisedBevelBorder());
		GraphConstants.setBackground(this.attributes, Color.orange);
		
		this.port = new DefaultPort();
		this.add(this.port);
		graphStruct.getCells().add(this);
		
		this.draw();
		return this;
	}
	
	/**
	 * Draw this Splitter
	 */
	public void draw()
	{
		System.out.println("Drawing the Splitter " +this.getName());
	}
		
	public void collapse(){};
}
