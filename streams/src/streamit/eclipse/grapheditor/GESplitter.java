/*
 * Created on Jun 23, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package grapheditor;

/**
 * @author jcarlos
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class GESplitter extends GEStreamNode{
	
	private String label;
	private int[] weights;

	public GESplitter(String label,int[] weights)
	{
		super("SPLITTER", label);
	}
}
