/*
 * Created on Jul 21, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package grapheditor.jgraphextension;

import java.util.ArrayList;
import java.util.Hashtable;
import com.jgraph.JGraph;

/**
 * @author jcarlos
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class LiveJGraphInternal 
{
	public ArrayList cells = null;
	public Hashtable attributes = null;
	public JGraph jgraph = null;
	
	public LiveJGraphInternal()
	{
		jgraph = new JGraph();
		attributes = new Hashtable();
		cells = new ArrayList();
	}
	
	
	
}
