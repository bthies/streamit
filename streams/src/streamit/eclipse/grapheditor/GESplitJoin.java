/*
 * Created on Jun 24, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package grapheditor;

import java.io.*;
import java.util.*;

/**
 * @author jcarlos
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class GESplitJoin extends GEStreamNode implements Serializable{
	private GESplitter splitter;
	private GEJoiner joiner;
	private ArrayList children;
	

	public GESplitJoin(String name, GESplitter split, GEJoiner join)
	{
		super(GEType.SPLIT_JOIN , name);
		this.splitter = split;
		this.joiner = join;
		this.setChildren(split.getChildren());
		
	}

	private void setChildren(ArrayList children)
	{
		this.children = children;
	}
	
	/**
	 * Get the splitter part of this
	 * @return GESplitter corresponding to this GESplitJoin
	 */
	public GESplitter getSplitter()
	{
		return this.splitter;
	}
	
	/**
 	 * Get the joiner part of this
	 * @return GESJoiner corresponding to this GESplitJoin
	 */
	public GEJoiner getJoiner()
	{
		return this.joiner;
	}
	
	/**
	 * Constructs the splitjoin and returns the last node in the splitjoin that wil be connecting
	 * to the next graph structure.
	 */	
	public GEStreamNode construct()
	{
		System.out.println("Constructing the SplitJoin " +this.getName());
		
		this.draw();
		this.splitter.construct();
		
		ArrayList nodeList = (ArrayList) this.getChildren();
		Iterator listIter =  nodeList.listIterator();
		ArrayList lastNodeList = new ArrayList();
		
		while(listIter.hasNext())
		{
			GEStreamNode strNode = ((GEStreamNode) listIter.next());
			lastNodeList.add(strNode.construct());
			
			System.out.println("Connecting " + splitter.getName()+  " to "+ strNode.getName());
			
			// TO BE ADDED			
			//connectDraw(splitter, strNode);
		}
		
		listIter =  lastNodeList.listIterator();
		this.joiner.construct();
		
		while(listIter.hasNext())
		{
			GEStreamNode strNode = (GEStreamNode) listIter.next();
			System.out.println("Connecting " + strNode.getName()+  " to "+ joiner.getName());
			
			// TO BE ADDED
			//connectDraw(strNode, joiner);
		}	
		
		System.out.println("exiting splitjoin construction");
		return this.joiner ;
	}
	
	
	public ArrayList getChildren()
	{
		return this.getSplitter().getChildren();
	}
	
	
	/**
	 * Draw this SplitJoin
	 */
	public void draw()
	{
		System.out.println("Drawing the SplitJoin " +this.getName());
		// TO BE ADDED
	}	
	
	public void collapse(){};
}
