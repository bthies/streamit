/*
 * Created on Jun 20, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package grapheditor;

import java.io.*;
import java.util.*;


import com.jgraph.graph.*;
import java.awt.Rectangle;
import java.awt.Color;
import javax.swing.BorderFactory; 



/**
 *  GEPhasedFilter is the graph editor's internal representation of a phased filter.
 *  @author jcarlos
 */
public class GEPhasedFilter extends GEStreamNode implements Serializable{
	
	private ArrayList initWorkFunctions;
	private ArrayList workFunctions;

	public GEPhasedFilter(String name)
	{
		super(GEType.PHASED_FILTER, name);
		initWorkFunctions = new ArrayList();
		workFunctions = new ArrayList();
		
		/* *************************************************************************
		 * Creation of the JGraph components necessary to draw the stream structure
		*/
		
		
		/* ***********************************************************
		*/
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
	
	// Returns the work function at the given index
	public GEWorkFunction getWorkFunction(int index)
	{
		return (GEWorkFunction) workFunctions.get(index);
	}
	
	// Return the init work function at the given index
	public GEWorkFunction getInitWorkFunction(int index)
	{
		return (GEWorkFunction) initWorkFunctions.get(index);
	}
	
	// Returns the number of init work functions
	public int getNumberOfInitWFs()
	{
		return this.initWorkFunctions.size();
	}
	
	//Returns the number of work functions
	public int getNumberOfWFs()
		{
			return this.workFunctions.size();
		}
	
	
	
	/**
	 * Contructs the filter and returns itself since Filters have no children.
	 */
	public GEStreamNode construct(GraphStructure graphStruct)
	{
		System.out.println("Constructing the filter " +this.getName());
		
		(graphStruct.getAttributes()).put(this, this.attributes);
		GraphConstants.setBounds(this.attributes, graphStruct.setRectCoords());
		GraphConstants.setBorder(this.attributes , BorderFactory.createRaisedBevelBorder());
		GraphConstants.setBackground(this.attributes, Color.blue);
		
		this.port = new DefaultPort();
		this.add(this.port);
		
		graphStruct.getCells().add(this);
		this.draw();	
		
		return this;
	}
	
	/**
	 * Draw this filter.
	 */
	public void draw()
	{
		System.out.println("Drawing the filter " +this.getName());
		// TO BE ADDED
	}

	public void collapse(){};
}
