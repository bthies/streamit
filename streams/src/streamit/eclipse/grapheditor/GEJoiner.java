/*
 * Created on Jun 23, 2003
 *
 */
package grapheditor;
import java.io.*;

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
	
	public void draw(){};
	public GEStreamNode construct(){return null;};
	public void collapse(){};
}
