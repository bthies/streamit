/*
 * Created on Jun 20, 2003
 */
package streamit.eclipse.grapheditor.graph;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Properties;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import org.jgraph.JGraph;
import org.jgraph.graph.DefaultPort;
import org.jgraph.graph.GraphConstants;

import streamit.eclipse.grapheditor.graph.resources.ImageLoader;

/**
 *  GEPhasedFilter is the graph editor's internal representation of a phased filter.
 * 	A GEPhasedFilter has a single input and a single output.
 * 	GEPhasedFilters contain work work functions. 
 * 
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
	
	
	private GraphStructure localGraphStruct;
	
	
	/**
	 * GEPhasedFilter constructor.
	 * @param name The name of this GEPhasedFilter.
	 */
	public GEPhasedFilter(String name)
	{
		super(GEType.PHASED_FILTER, name);
		initWorkFunctions = new ArrayList();
		workFunctions = new ArrayList();
		this.localGraphStruct = new GraphStructure();
	}

		
	public GEPhasedFilter(String name, GraphStructure gs)
	{
		super(GEType.PHASED_FILTER, name);
		initWorkFunctions = new ArrayList();
		workFunctions = new ArrayList();
		this.localGraphStruct = gs;

	}
	
	/**
	 *	Returns true if the work function was added to collection of init work functions
	 *	@param GEWorkFunction 
	 *	@return true if the work function was added succesfully; otherwise, return false.
	 */
	public boolean addInitWorkFunction(GEWorkFunction wf)
	{
		return initWorkFunctions.add(wf);
	}

	/**
	 * Returns true if the work function was added to collection of work functions
	 * @param GEWorkFunction
	 * @return true if the work function was added succesfully; otherwise, return false.
	 */
	public boolean addWorkFunction(GEWorkFunction wf)
	{
		return workFunctions.add(wf);
	}
	
	/**
	 * Returns the work function at the given index.
	 * @param int 
	 * @return GEWorkFunction at the given index.
	 */
	public GEWorkFunction getWorkFunction(int index)
	{
		return (GEWorkFunction) workFunctions.get(index);
	}
		
	/**
	 * Return the init work function at the given index
	 * @param int
	 * @return GEWorkFunction 
	 */
	public GEWorkFunction getInitWorkFunction(int index)
	{
		return (GEWorkFunction) initWorkFunctions.get(index);
	}
	
	/** 
	 * Returns the number of init work functions.
	 * @return int that represents the number of init work functions.
	 */
	public int getNumberOfInitWFs()
	{
		return this.initWorkFunctions.size();
	}
	
	/**
	 * Returns the number of work functions
	 * @return int that represents the number of init work functions.
	 */
	public int getNumberOfWFs()
	{
		return this.workFunctions.size();
	}
	
	/**
	 * Gets the first work function of the filter.
	 * @throws ArrayIndexOutOfBoundsException
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
		String strWF = Constants.HTML_LINE_BREAK + "Push = " + wf.getPushValue() + 
					   Constants.HTML_LINE_BREAK + "Pop = " + wf.getPopValue() + 
					   Constants.HTML_LINE_BREAK + "Peek = " + wf.getPeekValue();
		return strWF;
	}
	
	
	/**
	 * Contructs the filter by setting its draw attributes.
	 * @return GEStreamNode 
	 */
	public GEStreamNode construct(GraphStructure graphStruct, int lvl)
	{
		//TODO: Do we need to set a localGraphStruct ?? (1/28/04)
		// (3/7/04) It seems we do in order to deal with cloning.
		this.localGraphStruct = graphStruct;
	
		
		if (this.getNumberOfWFs() > 0) 
		{
			this.setInfo(this.getWFAsString(0));
			this.setUserObject(this.getInfoLabel());
		}
		else
		{
			this.setUserObject(this.getNameLabel());
		}
		
		this.initDrawAttributes(graphStruct, new Rectangle(new Point(10,10)));
	
		return this;
	}
	/**
	 * Change the display information of the GEPhasedFilter.
	 * @param jgraph JGraph
	 */
	public void setDisplay(JGraph jgraph)
	{
		this.setInfo(this.getWFAsString(0));
		super.setDisplay(jgraph);
	}
	
	/**
	 * Initialize the default attributes that will be used to draw the GEPhasedFilter.
	 * @param graphStruct The GraphStructure that will have its attributes set.
	 */	
	public void initDrawAttributes(GraphStructure graphStruct, Rectangle bounds)
	{
		/** Add the port to the GEPhasedFilter */
		this.port = new DefaultPort();
		this.add(this.port);
		
		/** Set the attributes corresponding to the GEPhasedFilter */
		(graphStruct.getAttributes()).put(this, this.attributes);
		GraphConstants.setAutoSize(this.attributes, true);
		GraphConstants.setVerticalTextPosition(this.attributes, JLabel.CENTER);
		GraphConstants.setBounds(this.attributes, bounds);
	
		/** Set the image representation the GEPhasedFilter */
		try 
		{
			ImageIcon icon = ImageLoader.getImageIcon("filter.GIF");
			GraphConstants.setIcon(this.attributes, icon);
		} catch (Exception ex) 
		{
			ex.printStackTrace();
		}

		/** Insert the GEPhasedFilter into the graph model */		
		graphStruct.getGraphModel().insert(new Object[] {this}, null, null, null, null);
		graphStruct.getGraphModel().edit(graphStruct.getAttributes(), graphStruct.getConnectionSet(), null, null);

	}
	
	/** Set the push, pop and seek rates for the GEPhasedFilter's work function.
	 *  If the GEPhasedFilter contains no work function, one will be added with the
	 * given rates. The object is updated to reflect the changes in the rates.
	 * @param push int representation of push rate.
	 * @param pop int representation of pop rate.
	 * @param peek intrepresentation of peek rate.
	 */
	public void setPushPopPeekRates(int push, int pop, int peek)
	{
		/** Set the rates for an existing work function (the first one) */
		if (this.workFunctions.size() != 0)
		{
			GEWorkFunction wf = (GEWorkFunction) this.workFunctions.get(0);
			wf.setPushValue(push);
			wf.setPopValue(pop);
			wf.setPeekValue(peek);
		}
		/** If there is no existing work function, add one with the given rates */
		else
		{
			this.workFunctions.add(new GEWorkFunction("work function", push, pop, peek));
									
		}
		/** Set the object to reflect these changes. */
		this.setInfo(this.getWFAsString(0));
		this.setUserObject(this.getInfoLabel());
	}

	/**
	 * Get a clone that of this instance of the GEPhasedFilter.
	 * @return Object that is a clone of this instance of the GEPhasedFilter
	 */
	public Object clone() 
	{
		/** Call the clone method for the supertypes of the GEPhasedFilter and
		 * create new instances of the mutable fields belonging to this object */
		GEPhasedFilter clonedFilter = (GEPhasedFilter) super.clone();
		clonedFilter.initWorkFunctions = new ArrayList (this.initWorkFunctions);
		clonedFilter.workFunctions = new ArrayList (this.workFunctions);
		
		//TODO: remove
		clonedFilter.removeAllChildren();
		DefaultPort newPort = new DefaultPort();
		clonedFilter.setPort(newPort);
		clonedFilter.add(newPort);
				
	//	clonedFilter.setUserObject("<HTML>" + Constants.HTML_TSIZE_BEGIN + clonedFilter.name + Constants.HTML_TSIZE_END+ "</html>");
		/*
		if ( ! (clonedFilter.getEncapsulatingNode() instanceof GEPipeline))
		{
			NodeCreator.construct(clonedFilter, clonedFilter.encapsulatingNode, 
								  clonedFilter.localGraphStruct, new Rectangle(new Point(10,10)));
		}*/
		return clonedFilter;
	}
	
	/**
	 * Get the properties of the GEPhasedFilter.
	 * @return Properties of this GEPhasedFilter.
	 */
	public Properties getNodeProperties()
	{
		Properties properties =  super.getNodeProperties();
		try
		{
			GEWorkFunction wf = this.getWorkFunction();
			properties.put(GEProperties.KEY_PUSH_RATE, Integer.toString(wf.getPushValue())  );
			properties.put(GEProperties.KEY_POP_RATE, Integer.toString(wf.getPopValue()));
			properties.put(GEProperties.KEY_PEEK_RATE, Integer.toString(wf.getPeekValue()));
		}
		catch(ArrayIndexOutOfBoundsException e)
		{
			properties.put(GEProperties.KEY_PUSH_RATE, "0");
			properties.put(GEProperties.KEY_POP_RATE, "0");
			properties.put(GEProperties.KEY_PEEK_RATE, "0");
		}
		return properties;
	}
	
	/**
	 * Set the properties of the GEStreamNode.  
	 * @param properties Properties
	 * @param jgraph JGraph 
	 * @param containerNodes ContainerNodes
	 */
	public void setNodeProperties(Properties properties, JGraph jgraph, ContainerNodes containerNodes)
	{
		super.setNodeProperties(properties, jgraph, containerNodes);
		GEWorkFunction wf = this.getWorkFunction();
		wf.setPushValue(Integer.parseInt(properties.getProperty(GEProperties.KEY_PUSH_RATE)));
		wf.setPopValue(Integer.parseInt(properties.getProperty(GEProperties.KEY_POP_RATE)));
		wf.setPeekValue(Integer.parseInt(properties.getProperty(GEProperties.KEY_PEEK_RATE)));
			
		this.setDisplay(jgraph);	
	}
	
	
	
	
	
	
	/**
	 * Writes the textual representation of the GEStreamNode to the StringBuffer. 
	 * In this case, the textual representation corresponds to the the StreamIt source code 
	 * equivalent of the GEStreamNode. 
	 * @param strBuff StringBuffer that is used to output the textual representation of the graph.
	 * @param nameList List of the names of the nodes that have already been added to the template code.  
	 */
	public void outputCode(StringBuffer strBuff, ArrayList nameList)
	{
		if ( ! (nameList.contains(this.getName())))
		{
			nameList.add(this.getName());
			String tab = "     ";
			String newLine = "\n";
			
			/** Create the basic definition for the GEStreamNode */
			strBuff.append(newLine + this.inputTape)
						.append("->")
						.append(this.outputTape + " ")
						.append(GEType.GETypeToString(this.type)+" ")
						.append(this.getName() + this.outputArgs() + " {" + newLine + newLine);
			
			/** add the "init" declaration */
			strBuff.append(tab + "init {" + newLine + newLine);
			strBuff.append(tab + "}" + newLine + newLine);
		
			/** add the "work" declaration */
			int pop = 0;
			int push = 0;
			int peek = 0;
			try
			{
				GEWorkFunction wf = this.getWorkFunction();
				pop = wf.getPopValue();
				push = wf.getPushValue();
				peek = wf.getPeekValue();
			}
			catch (Exception e)
			{
				 
			}
			strBuff.append(tab + "work ")
					.append(pop > 0 ? "pop " + pop + " " : "")
					.append(push > 0 ? "push " + push + " " : "")
					.append(peek > 0 ? "peek " + peek + " " : "")
					.append("{" + newLine + newLine + tab +"}" + newLine);
	
			strBuff.append("}" + newLine);	
				
		}				
	}
}
