/*
 * Created on Jun 20, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package grapheditor;
import java.util.ArrayList;

/**
 * GEPipeline is the graph internal representation of a node. .
 * @author jcarlos
 *
 */
public class GEPipeline extends GEStreamNode{
	
	private ArrayList pipeStages;
	
		
	public GEPipeline()
	{
		pipeStages = new ArrayList();
	}
	
	
	// Returns true if next pipe stage was added succesfully.
	public boolean addPipeStage(GEStreamNode sNode)
	{
		return pipeStages.add(sNode);
	}
}
