/*
 * Created on Jun 23, 2003
 *
 */
package grapheditor;
import java.io.*;

import com.jgraph.graph.*;
import java.awt.Color;
import javax.swing.BorderFactory; 

import java.awt.Point;
import com.jgraph.JGraph;

/**
 * GEJoiner is the graph editor's internal representation of a joiner.
 * @author jcarlos
 */
public class GEJoiner extends GEStreamNode implements Serializable{
	
	private String label;
	private int[] weights;


	public GEJoiner (String label, int[] weights)
	{
		super(GEType.JOINER, label);
		this.label = label;
		this.weights = weights;
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
	 * Contructs the joiner and returns itself since Joiners have no children.
	 */
	public GEStreamNode construct(GraphStructure graphStruct)
	{
		System.out.println("Constructing the Joiner " +this.getName());
		
		
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
	 * Draw this Joiner
	 */
	public void draw()
	{
		System.out.println("Drawing the Joiner " +this.getName());
		// TO BE ADDED
	}
	
	public void collapseExpand(JGraph jgraph){};
}
