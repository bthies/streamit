/*
 * Created on Jun 20, 2003
 */
package streamit.eclipse.grapheditor;

import java.io.*;
import java.util.*;
import com.jgraph.graph.*;
import java.awt.Color;
import javax.swing.BorderFactory; 
import com.jgraph.JGraph;

/**
 *  GEPhasedFilter is the graph editor's internal representation of a phased filter.
 *  @author jcarlos
 */
public class GEPhasedFilter extends GEStreamNode implements Serializable{
	
	/**
	 * The initial work functions of the GEPhasedFilter.
	 */
	private ArrayList initWorkFunctions;
	
	/**
	 * The work functions of the GEPhasedFilter.
	 */
	private ArrayList workFunctions;

	/**
	 * GEPhasedFilter constructor.
	 * @param name The name of this GEPhasedFilter.
	 */
	public GEPhasedFilter(String name)
	{
		super(GEType.PHASED_FILTER, name);
		initWorkFunctions = new ArrayList();
		workFunctions = new ArrayList();
	}
	
	/**
	 *	Returns true if wf was added to collection of init work functions
	 */
	public boolean addInitWorkFunction(GEWorkFunction wf)
	{
		return initWorkFunctions.add(wf);
	}

	/**
	 *Returns true if wf was added to collection of work functions
	 */
	public boolean addWorkFunction(GEWorkFunction wf)
	{
		return workFunctions.add(wf);
	}
	
	/**
	 * Returns the work function at the given index
	 */
	public GEWorkFunction getWorkFunction(int index)
	{
		return (GEWorkFunction) workFunctions.get(index);
	}
		
	/**
	 * Return the init work function at the given index
	 */
	public GEWorkFunction getInitWorkFunction(int index)
	{
		return (GEWorkFunction) initWorkFunctions.get(index);
	}
	
	/** 
	 * Returns the number of init work functions
	 */
	public int getNumberOfInitWFs()
	{
		return this.initWorkFunctions.size();
	}
	
	/**
	 * Returns the number of work functions
	 */
	public int getNumberOfWFs()
	{
		return this.workFunctions.size();
	}
	
	/**
	 * Get the string representation of the GEWorkFunction at index.
	 * @return The string representation of the GEWorkFunction
	 * @param index The index of the GEWorkFunction that will be returned as a String.
	 */
	public String getWFAsString(int index)
	{
		GEWorkFunction wf = this.getWorkFunction(index);
		String strWF = "<BR>Push = " + wf.getPushValue() + 
					   "<BR>Pop = " + wf.getPopValue() + 
					   "<BR>Peek = " + wf.getPeekValue();
		return strWF;
	}
	
	
	/**
	 * Contructs the filter and returns <this>.
	 * @return <this>
	 */
	public GEStreamNode construct(GraphStructure graphStruct, int level)
	{
		System.out.println("Constructing the filter " +this.getName());
		
		if (this.getNumberOfWFs() > 0) 
		{
			this.setInfo(this.getWFAsString(0));
			this.setUserObject(this.getInfoLabel());
		}
		else
		{
			this.setUserObject(this.getNameLabel());
		}
		
		this.initDrawAttributes(graphStruct);

		return this;
	}
	
	/**
	 * Initialize the default attributes that will be used to draw the GEPhasedFilter.
	 * @param graphStruct The GraphStructure that will have its attributes set.
	 */	
	public void initDrawAttributes(GraphStructure graphStruct)
	{
		(graphStruct.getAttributes()).put(this, this.attributes);
				
		GraphConstants.setAutoSize(this.attributes, true);
		GraphConstants.setBounds(this.attributes, graphStruct.setRectCoords(this));
		GraphConstants.setBorder(this.attributes , BorderFactory.createLineBorder(Color.red));
		GraphConstants.setBackground(this.attributes, Color.blue);
		
		this.port = new DefaultPort();
		this.add(this.port);
		
		graphStruct.getCells().add(this);
	}
	/**
	 * Draw this GEPhasedFilter.
	 */
	public void draw()
	{
		System.out.println("Drawing the filter " +this.getName());
		// TO BE ADDED
	}

	/**
	 * Expand or collapse the GEPhasedFilter depending on wheter it was already 
	 * collapsed or expanded. 
	 * @param jgraph The JGraph that will be modified to allow the expanding/collapsing.
	 */
	public void collapseExpand(JGraph jgraph)
	{
		if (this.isInfoDisplayed)
		{
			Map change = GraphConstants.createMap();
			GraphConstants.setValue(change, this.getNameLabel());
			Map nest = new Hashtable ();
			nest.put(this, change);
			jgraph.getModel().edit(nest, null, null, null);
						
			this.isInfoDisplayed = false;
		}
		else
		{
			Map change = GraphConstants.createMap();
			GraphConstants.setValue(change, this.getInfoLabel());
			Map nest = new Hashtable ();
			nest.put(this, change);
			jgraph.getModel().edit(nest, null, null, null);
																	
			this.isInfoDisplayed = true;
		}
		System.out.println("The user object is " +this.getUserObject().toString());
		System.out.println(jgraph.convertValueToString(this)); 
	}
	public void collapse(JGraph jgraph){};
	public void expand(JGraph jgraph){};

}
