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
	
	
	public GEFeedbackLoop(String name, GESplitter split, GEJoiner join, GEStreamNode body, GEStreamNode loop)
	{
		super(GEType.FEEDBACK_LOOP, name);
		this.splitter = split;
		this.joiner = join;
		this.body = body;
		this.loop = loop;
	}
	
	/**
	 * Get the splitter part of this
	 * @return GESplitter corresponding to this GEFeedbackLoop.
	 */
	public GESplitter getSplitter()
	{
		return this.splitter;
	}
	
	/**
	 * Get the joiner part of this
	 * @return GESJoiner corresponding to this GEFeedbackLoop.
	 */
	public GEJoiner getJoiner()
	{
		return this.joiner;
	}	
	
	/**
	 * Get the body of this 
	 * @return GEStreamNode that is the body of GEFeedbackLoop.
	 */
	public GEStreamNode getBody()
	{
		return this.body;
	}
	
	/**
	 * Get the loop of this
	 * @return GEStreamNode that is the loop of GEFeedbackLoop.
	 */
	public GEStreamNode getLoop()
	{
		return this.loop;
	}
	
	
	/**
	 * Draw this Pipeline
  	 */	
	public void draw()
	{
		System.out.println("Drawing the pipeline " +this.getName());
		// TO BE ADDED
	}
	
	
	public GEStreamNode construct(GraphStructure graphStruct)
	{
		System.out.println("Constructing the feedback loop " +this.getName());
		this.draw();
		joiner.construct(graphStruct);
		GEStreamNode lastBody = body.construct(graphStruct);
				
		System.out.println("Connecting " + joiner.getName()+  " to "+ body.getName());		
		graphStruct.connectDraw(joiner, lastBody );
	
		System.out.println("Connecting " + body.getName()+  " to "+ splitter.getName());
		graphStruct.connectDraw(body, splitter);
		
		splitter.construct(graphStruct);
		GEStreamNode lastLoop = loop.construct(graphStruct); 
		
		System.out.println("Connecting " + splitter.getName()+  " to "+ loop.getName());
		graphStruct.connectDraw(splitter, loop);
		
		System.out.println("Connecting " + loop.getName()+  " to "+ joiner.getName());
		graphStruct.connectDraw(loop, joiner);
		
		return splitter;
	}
	public void collapse(){};
	
}
