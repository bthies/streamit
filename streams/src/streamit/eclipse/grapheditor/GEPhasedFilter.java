/*
 * Created on Jun 20, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package grapheditor;

import java.util.ArrayList;

/**
 *  GEPhasedFilter is the graph editor's internal representation of a phased filter.
 *  @author jcarlos
 */
public class GEPhasedFilter extends GEStreamNode{
	
	private ArrayList initWorkFunctions;
	private ArrayList workFunctions;
	
	
	
	public GEPhasedFilter()
	{
		super("PHASED_FILTER", "");
		initWorkFunctions = new ArrayList();
		workFunctions = new ArrayList();
	}
	
	//	Returns true if wf was added to collection of init work functions
	public boolean addInitWorkFunction(GEWorkFunction wf)
	{
		return initWorkFunctions.add(wf);
	}

	// Returns true if wf was added to collection of work functions
	public boolean addWorkFunction(GEWorkFunction wf)
	{
		return workFunctions.add(wf);
	}
	
}
