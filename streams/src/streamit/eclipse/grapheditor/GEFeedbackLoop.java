/*
 * Created on Jun 24, 2003
 */
package grapheditor;
import java.io.*;
import com.jgraph.JGraph;


/**
 * GEFeedbackLoop is the graph internal representation of  a feedback loop.
 * @author jcarlos
 */
public class GEFeedbackLoop extends GEStreamNode implements Serializable{
	
	/**
	 * The splitter belonging to this feedback loop.
	 */
	private GESplitter splitter;
	
	/**
	 * The joiner belonging to this feedback loop.
	 */
	private GEJoiner joiner;
	
	/**
	 * The body of the feedback loop.
	 */
	private GEStreamNode body;
	
	/**
	 * The loop part of the feedback loop.
	 */
	private GEStreamNode loop;
	
	/**
	 * GEFeedbackLoop constructor.
	 * @param name The name of the GEFeedbackLoop.
	 * @param split The GESplitter that corresponds to this GEFeedbackLoop.
	 * @param join The GEJoiner that corresponds to this GEFeedbackLoop.
	 * @param body The GEStreamNode that represents the body of theGEFeedbackLoop.
	 * @param loop The GEStreamNode that represents the body of the GEFeedbackLoop.
	 */
	public GEFeedbackLoop(String name, GESplitter split, GEJoiner join, GEStreamNode body, GEStreamNode loop)
	{
		super(GEType.FEEDBACK_LOOP, name);
		this.splitter = split;
		this.joiner = join;
		this.body = body;
		this.loop = loop;
	}
	
	
	
	/**
	 * Get the splitter part of this.
	 * @return GESplitter corresponding to this GEFeedbackLoop.
	 */
	public GESplitter getSplitter()
	{
		return this.splitter;
	}
	
	/**
	 * Get the joiner part of this.
	 * @return GESJoiner corresponding to this GEFeedbackLoop.
	 */
	public GEJoiner getJoiner()
	{
		return this.joiner;
	}	
	
	/**
	 * Get the body of this.
	 * @return GEStreamNode that is the body of GEFeedbackLoop.
	 */
	public GEStreamNode getBody()
	{
		return this.body;
	}
	
	/**
	 * Get the loop of this.
	 * @return GEStreamNode that is the loop of GEFeedbackLoop.
	 */
	public GEStreamNode getLoop()
	{
		return this.loop;
	}
	
	
	/**
	 * Draw this GEFeedbackLoop.
  	 */	
	public void draw()
	{
		System.out.println("Drawing the pipeline " +this.getName());
		// TO BE ADDED
	}
	
	
	public GEStreamNode construct(GraphStructure graphStruct, int level)
	{
		System.out.println("Constructing the feedback loop " +this.getName());
		this.draw();
		joiner.construct(graphStruct, level);
		GEStreamNode lastBody = body.construct(graphStruct, level);
				
		System.out.println("Connecting " + joiner.getName()+  " to "+ body.getName());		
		graphStruct.connectDraw(joiner, lastBody );
	
		System.out.println("Connecting " + body.getName()+  " to "+ splitter.getName());
		graphStruct.connectDraw(body, splitter);
		
		splitter.construct(graphStruct, level);
		GEStreamNode lastLoop = loop.construct(graphStruct, level); 
		
		System.out.println("Connecting " + splitter.getName()+  " to "+ loop.getName());
		graphStruct.connectDraw(splitter, loop);
		
		System.out.println("Connecting " + loop.getName()+  " to "+ joiner.getName());
		graphStruct.connectDraw(loop, joiner);
		
		return splitter;
	}
	
	public void collapseExpand(JGraph jgraph){};
	public void collapse(JGraph jgraph){};
	public void expand(JGraph jgraph){};

}
