/*
 * Created on Jun 23, 2003
 *
 */
package grapheditor;
import java.io.*;

/**
 * @author jcarlos
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
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
	public void draw(){};
	public void construct(){};
}
