/*
 * Created on Jun 23, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package grapheditor;

import java.io.*;
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
	
	public void draw(){};
	public void construct(){};
}
