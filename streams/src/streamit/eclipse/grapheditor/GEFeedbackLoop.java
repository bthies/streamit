/*
 * Created on Jun 24, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package grapheditor;
import java.io.*;

/**
 * @author jcarlos
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class GEFeedbackLoop extends GEStreamNode implements Serializable{
	private GESplitter splitter;
	private GEJoiner joiner;
	private GEStreamNode body;
	private GEStreamNode loop;
	
	
	public GEFeedbackLoop(GESplitter split, GEJoiner join, GEStreamNode body, GEStreamNode loop)
	{
		super(GEType.FEEDBACK_LOOP, "");
		this.splitter = split;
		this.joiner = join;
		this.body = body;
		this.loop = loop;
	}
	
	public void draw(){};
	public void construct(){};
	
}
