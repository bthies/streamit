/*
 * Created on Jun 20, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package grapheditor;
import java.io.*;
import java.util.*;

/**
 * GEPipeline is the graph internal representation of a node. .
 * @author jcarlos
 *
 */
public class GEPipeline extends GEStreamNode implements Serializable{
			
	private GEStreamNode lastNode;		
	
	public GEPipeline(String name)
	{
		super(GEType.PIPELINE, name);
	}


/**
 * Constructs the pipeline and returns the last node in the pipeline that wil be connecting
 * to the next graph structure.
 */
	public GEStreamNode construct()
	{
		System.out.println("Constructing the pipeline" +this.getName());
		boolean first = true;
	
		this.draw();
		ArrayList nodeList = (ArrayList) this.getChildren();
		Iterator listIter =  nodeList.listIterator();
		while(listIter.hasNext())
		{
			GEStreamNode strNode = (GEStreamNode) listIter.next();
			if(!first)
			{
				// TO BE ADDED
				//connectDraw(lastNode, strNode);
				System.out.println("Connecting " + lastNode.getName()+  " to "+ strNode.getName());
			}
			
			lastNode = strNode.construct();
			first = false;
		}
		return this.lastNode;
	}	
	
	
	public void draw()
	{
		System.out.println("Drawing the pipeline " +this.getName());
	}
	public void collapse()
	{
		// draw shrunk version
	}

	
	
	
	
}
