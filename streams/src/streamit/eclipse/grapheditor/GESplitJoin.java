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
	

	public GESplitJoin(GESplitter split, GEJoiner join)
	{
		super(GEType.SPLIT_JOIN , "");
		this.splitter = split;
		this.joiner = join;
		this.setChildren(split.getChildren());
		
	}

	private void setChildren(ArrayList children)
	{
		this.children = children;
	}
	
	public void draw(){};
	public void construct(){};
}
