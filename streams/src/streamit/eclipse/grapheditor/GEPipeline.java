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
			
	public GEPipeline(String name)
	{
		super(GEType.PIPELINE, name);
	}

	public void draw(){};
	public void construct()
	{
		ArrayList nodeList = (ArrayList) this.getChildren();
		Iterator listIter =  nodeList.listIterator();
		while(listIter.hasNext())
		{
			GEStreamNode strNode = (GEStreamNode) listIter.next();
			strNode.construct();
		}
		this.draw();
		
	}
}
