/*
 * Created on Jun 20, 2003
 */
package streamit.eclipse.grapheditor.graph;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

import org.jgraph.JGraph;
import org.jgraph.graph.DefaultPort;
import org.jgraph.graph.GraphConstants;

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
	 * Get the first work function of the filter.
	 * @return GEWorkFunction.
	 */
	public GEWorkFunction getWorkFunction() throws ArrayIndexOutOfBoundsException
	{
		if (getNumberOfWFs() > 0)
		{
			return (GEWorkFunction) workFunctions.get(0);
		}
		else 
		{
			throw new ArrayIndexOutOfBoundsException();
		}
		
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
	public GEStreamNode construct(GraphStructure graphStruct, int lvl)
	{
		System.out.println("Constructing the filter " +this.getName());
		
		this.level = lvl;
		
		if (this.getNumberOfWFs() > 0) 
		{
			this.setInfo(this.getWFAsString(0));
			this.setUserObject(this.getInfoLabel());
		}
		else
		{
			this.setUserObject(this.getNameLabel());
		}
		
		this.initDrawAttributes(graphStruct, new Rectangle(new Point(100,100)));
		graphStruct.getCells().add(this);

		return this;
	}
	
	public void setDisplay(JGraph jgraph)
	{
		this.setInfo(this.getWFAsString(0));
		Map change = GraphConstants.createMap();
		GraphConstants.setValue(change, this.getInfoLabel());
		Map nest = new Hashtable ();
		nest.put(this, change);
		jgraph.getModel().edit(nest, null, null, null);
	}
	
	/**
	 * Initialize the default attributes that will be used to draw the GEPhasedFilter.
	 * @param graphStruct The GraphStructure that will have its attributes set.
	 */	
	public void initDrawAttributes(GraphStructure graphStruct, Rectangle bounds)
	{
		(graphStruct.getAttributes()).put(this, this.attributes);
				
		GraphConstants.setAutoSize(this.attributes, true);
		GraphConstants.setVerticalTextPosition(this.attributes, JLabel.CENTER);
		GraphConstants.setBounds(this.attributes, bounds);
	
		//demoadd
		GraphConstants.setBorder(this.attributes , BorderFactory.createLineBorder(Color.red));
		/* demoremove
		try 
		{
			ImageIcon icon = ImageLoader.getImageIcon("filter.GIF");
			GraphConstants.setIcon(this.attributes, icon);
		} catch (Exception ex) 
		{
			ex.printStackTrace();
		}*/
		
		this.port = new DefaultPort();
		this.add(this.port);
				
//		graphStruct.getCells().add(this);
	}
	
	
	public void setPushPopPeekRates(int push, int pop, int peek)
	{
		if (this.workFunctions.size() != 0)
		{
			GEWorkFunction wf = (GEWorkFunction) this.workFunctions.get(0);
			wf.setPushValue(push);
			wf.setPopValue(pop);
			wf.setPeekValue(peek);
		}
		else
		{
			this.workFunctions.add(new GEWorkFunction("work function", push, pop, peek));
									
		}
		this.setInfo(this.getWFAsString(0));
		this.setUserObject(this.getInfoLabel());


		
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
	
	/**
	 * Hide the GEStreamNode in the display. Note that some nodes cannot be hidden or 
	 * they cannot be made visible.
	 * @return true if it was possible to hide the node; otherwise, return false.
	 */
	public boolean hide()
	{
		return false;
	}

	/**
	 * Make the GEStreamNode visible in the display. Note that some nodes cannot be hidden or 
	 * they cannot be made visible. 
	 * @return true if it was possible to make the node visible; otherwise, return false.
	 */	
	public boolean unhide()
	{
		return false;
	};

	/** Returns a list of nodes that are contained by this GEStreamNode. If this GEStreamNode is
	 * not a container node, then a list with no elements is returned.
	 * @return ArrayList of contained elements. If <this> is not a container, return empty list.
	 */
	public ArrayList getContainedElements(){return new ArrayList();};
	
	/**
	 * Writes the textual representation of the GEStreamNode using the PrintWriter specified by out. 
	 * In this case, the textual representation corresponds to the the StreamIt source code 
	 * equivalent of the GEStreamNode. 
	 * @param out PrintWriter that is used to output the textual representation of the graph.  
	 */
	public void outputCode(PrintWriter out)
	{
		String tab = "     ";
		out.println();
		out.print(this.inputTape + "->" + this.outputTape + " filter " + this.name);
	
		if (this.args.size() > 0)
		{
			this.outputArgs(out);
		}
		out.println(" { ");		
		
		
		//TODO: Handle the case when there are more than one work functions
		if (this.getNumberOfWFs() > 0 )
		{
			GEWorkFunction wf = this.getWorkFunction(0);
			out.print(tab + "work ");
			out.print(wf.getPopValue() > 0 ? "pop " + wf.getPopValue() + " " : "");
			out.print(wf.getPushValue() > 0 ? "push " + wf.getPushValue() + " " : "");
			out.print(wf.getPeekValue() > 0 ? "peek " + wf.getPeekValue() + " " : "");
			out.println("{");
			//TODO: Initialization code specific to the filter
			out.println(tab+ "}");
		}
		
		out.println("} ");
		out.println();		
	}
}
