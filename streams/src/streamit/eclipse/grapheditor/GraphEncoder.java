package grapheditor;

import at.dms.kjc.sir.*;
import at.dms.kjc.*;
import java.io.*;
import java.util.*;


 

public class GraphEncoder implements AttributeStreamVisitor {
 
    //May Want outputStream or stdout. Not sure
    private PrintStream outputStream;
    private GraphStructure graph;

    public GraphEncoder() 
    {
    	System.out.println("Created GraphEncoder that ouputs to System.out");
		this.graph = new GraphStructure();
	
		//Feel free to add arguments and call it correctly
		//Setup an output stream to output to here
		//For now we are using System.out
		//Later we can change to outputting to file perhaps
	
		this.outputStream = System.out;
	
    }
    
    public GraphEncoder(PrintStream outputStream) 
    {
		System.out.println("Created GraphEncoder that ouputs to outputStream");
		this.graph = new GraphStructure();
		this.outputStream = outputStream;
    }
    
	/** 
	 * Set the toplevel and begin visiting the nodes
	*/
	public void encode(SIRStream str)
	{
	 
		graph.setTopLevel((GEStreamNode) str.accept(this));
		
		/* ***********************************************************
		 * DEBUGGING CODE BEGIN
		 */
		System.out.println("The toplevel stream is "+ graph.getTopLevel().getName());
		
		ArrayList topChildren = graph.getTopLevel().getSuccesors();
		for (int i = 0; i< topChildren.size(); i++)
		{
			 System.out.println("The children of the toplevel are " + ((GEStreamNode) topChildren.get(i)).getName());
			 	  
		}
		System.out.println("#########################################################");
		graph.constructGraph();
	
		System.out.println("End of Test");
		/* 
		 * DEBUGGING CODE END
		 * *********************************************************** */
		 





	}
			
		 
		 	 
		 
		 
    /**
     * Visit a SIRStructure
     */
    public Object visitStructure(SIRStructure self,
                                 JFieldDeclaration[] fields) {
   		//REMOVED TO COMPILE
        //return new GEStreamNode(self.getIdent(), "");
        return null;
    }
    
    /**
     * Visit a SIRFilter 
     * @return Visited SIRFilter encoded as a GEPhasedFilter.
     * */
    public Object visitFilter(SIRFilter self,
                              JFieldDeclaration[] fields,
                              JMethodDeclaration[] methods,
                              JMethodDeclaration init,
                              JMethodDeclaration work,
                              CType inputType, CType outputType) 
   {

		System.out.println("***** Entering visitFilter for filter " + self.getName());                              	
		GEPhasedFilter phFilter = new GEPhasedFilter(self.getName());					
		try 
		{
			SIRWorkFunction[] phases = self.getPhases();
			for (int i = 0; i < phases.length; i++)
			{	
				phFilter.addWorkFunction(new GEWorkFunction(work.getName(), 
															phases[i].getPushInt(), 
															phases[i].getPopInt(), 
															phases[i].getPeekInt()));
			}
			phases = self.getInitPhases();
			for (int i = 0; i < phases.length; i++)
			{
				phFilter.addInitWorkFunction (new GEWorkFunction(work.getName(), 
																 phases[i].getPushInt(), 
																 phases[i].getPopInt(), 
																 phases[i].getPeekInt()));
			}			
		}
		catch (Exception e) 
		{
			// if constants not resolved for the ints, will get an exception
			System.out.println("Exception thrown " + e.toString());
		}
		
		
		
			
			/* ***********************************************************
			 * DEBUGGING CODE BEGIN
			 */
			for (int i = 0; i < phFilter.getNumberOfInitWFs() ; i++)
			{
				System.out.println("Init Function "+ i +" for Filter "+ phFilter.getName());
				GEWorkFunction wf = (GEWorkFunction) phFilter.getInitWorkFunction(i);
				System.out.println("\t label = " + wf.getName());
				System.out.println("\t Push value = "+ wf.getPushValue());
				System.out.println("\t Pop value = "+ wf.getPopValue());
				System.out.println("\t Peek value = "+ wf.getPeekValue());

			}
		
			for (int i = 0; i < phFilter.getNumberOfWFs() ; i++)
			{
				System.out.println("Work Function "+ i +" for Filter "+phFilter.getName());
				GEWorkFunction wf = (GEWorkFunction) phFilter.getWorkFunction(i);
				System.out.println("\t label = " + wf.getName());
				System.out.println("\t Push value = "+ wf.getPushValue());
				System.out.println("\t Pop value = "+ wf.getPopValue());
				System.out.println("\t Peek value = "+ wf.getPeekValue());
			}
			/* 
			 * DEBUGGING CODE END
			 *********************************************************** */				
					
		return phFilter;             	
                              	    	
		
    }
    
    /**
     * Visit a SIRPhasedFilter 
     * @return Visited SIRPhasedFilter encoded as a GEPhasedFilter.
     */
    public Object visitPhasedFilter(SIRPhasedFilter self,
                                    JFieldDeclaration[] fields,
                                    JMethodDeclaration[] methods,
                                    JMethodDeclaration init,
                                    JMethodDeclaration work,
                                    SIRWorkFunction[] initPhases,
                                    SIRWorkFunction[] phases,
                                    CType inputType, CType outputType)
	{
		System.out.println("***** Entering visitPhasedFilter");
    	GEPhasedFilter phFilter = new GEPhasedFilter(self.getName());
		// Walk through each of the phases.
		if (initPhases != null)
		{
			for (int i = 0; i < initPhases.length; i++)
			{   
				GEWorkFunction wf = (GEWorkFunction) initPhases[i].accept(this);
				phFilter.addInitWorkFunction(wf);
			}
		}
		if (phases != null)
		{
	   		for (int i = 0; i < phases.length; i++)
	   		{
				GEWorkFunction wf = (GEWorkFunction) initPhases[i].accept(this);
				phFilter.addWorkFunction(wf);
	   		}
		}        
						
		/* ***********************************************************
		 * DEBUGGING CODE BEGIN
		 */
				for (int i = 0; i < phFilter.getNumberOfInitWFs() ; i++)
				{
					System.out.println("Init Function "+ i +" for Filter "+ phFilter.getName());
					GEWorkFunction wf = (GEWorkFunction) phFilter.getInitWorkFunction(i);
					System.out.println("\t label = " + wf.getName());
					System.out.println("\t Push value = "+ wf.getPushValue());
					System.out.println("\t Pop value = "+ wf.getPopValue());
					System.out.println("\t Peek value = "+ wf.getPeekValue());
			
				}
		
				for (int i = 0; i < phFilter.getNumberOfWFs() ; i++)
				{
					System.out.println("Work Function "+ i +" for Filter "+phFilter.getName());
					GEWorkFunction wf = (GEWorkFunction) phFilter.getWorkFunction(i);
					System.out.println("\t label = " + wf.getName());
					System.out.println("\t Push value = "+ wf.getPushValue());
					System.out.println("\t Pop value = "+ wf.getPopValue());
					System.out.println("\t Peek value = "+ wf.getPeekValue());
				}
		/* 
		 * DEBUGGING CODE END
		 *********************************************************** */				
						
		return phFilter;
	
    }
    
    /** 
     * Visit a SIRSplitter
     * @return Visited SIRSplitter encoded as a GESplitter. 
     */
    public Object visitSplitter(SIRSplitter self,
                                SIRSplitType type,
                                JExpression[] expWeights) 
	{
		System.out.println("***** Entering visitSplitter");
		try 
		{
			GESplitter splitter = new GESplitter(self.getName(), self.getWeights());
			
			/* ***********************************************************
			 * DEBUGGING CODE BEGIN
			 */	
			System.out.println("Reached the start of the debugging code");
			int[] weights = splitter.getWeights();
			System.out.println("The number of weights for Splitter " + splitter.getName() + " = " + weights.length);
			System.out.println("The weights for Splitter " + splitter.getName()); 		
			for (int i = 0; i < weights.length; i++)
			{
				System.out.println("\tWeight " +i + " = " + weights[i]);
			}
			/* 
			 * DEBUGGING CODE END
			 *********************************************************** */
			
			return splitter;			
			
			
			
			
		}
		catch (Exception e) 
		{
			System.out.println("Exception thrown " + e.toString() + " in visitSplitter");
			return null;
		}
		
	}
    
    /** 
     * Visit a SIRJoiner
     * @return Visited SIRJoiner encoded as a GEJoiner. 
     */
    public Object visitJoiner(SIRJoiner self,
                              SIRJoinType type,
                              JExpression[] expWeights) 
    {
		System.out.println("***** Entering visitJoiner");
		try 
		{
			GEJoiner joiner = new GEJoiner(self.getName(), self.getWeights());
			
			/* ***********************************************************
			 * DEBUGGING CODE BEGIN
			 */	
			int[] weights = joiner.getWeights();
			System.out.println("The number of weights for Joiner " + joiner.getName() + " = " + weights.length);
			System.out.println("The weights for Joiner " + joiner.getName()); 		
			
			for (int i = 0; i < weights.length; i++)
			{
				System.out.println("\tWeight " +i + " = " + weights[i]);
			}
			/* 
			 * DEBUGGING CODE END
			 *********************************************************** */
			
			return joiner;
		}
		catch (Exception e) 
		{
			System.out.println("Exception thrown " + e.toString() + " in visitJoiner");	
			return null;
		} 
		
		
    }
    
    /** 
     * Visit a SIRWorkFunction
     * @return Visited SIRWorkFunction encoded as a GEWorkFunction.
     */
    public Object visitWorkFunction(SIRWorkFunction self,
                                    JMethodDeclaration work) 
    {
		System.out.println("***** Entering visitWorkFunction");
		try 
		{
			return new GEWorkFunction(work.getName(), self.getPushInt(), self.getPopInt(),self.getPeekInt());
		} 
		catch (Exception e) 
		{
			// if constants not resolved for the ints, will get an exception
			return null;
		}
	}
		
    /**
     * Visit a SIRPipeline
     * @return Visited SIRPipeline encoded as a GEPipeline.
     */
    public Object visitPipeline(SIRPipeline self,
                                JFieldDeclaration[] fields,
                                JMethodDeclaration[] methods,
                                JMethodDeclaration init) {                 	
		
		System.out.println("***** Entering visitPipeline "+ self.getName());
                                	
		GEPipeline pipeline = new GEPipeline(self.getName());        
         
	
		// Walk through each of the elements in the pipeline.
		Iterator iter = self.getChildren().iterator();
	
		while (iter.hasNext())
		{
			SIROperator oper = (SIROperator)iter.next();
			
			
			GEStreamNode currNode = (GEStreamNode) oper.accept(this);
			pipeline.addChild(currNode);
			currNode.setEncapsulatingNode(pipeline);	
		
		}
		  
		graph.addHierarchy(pipeline, pipeline.getSuccesors());
		
		
		/* ***********************************************************
		 * DEBUGGING CODE BEGIN
		*/
		System.out.println("The pipeline "+ pipeline.getName() + " has the following children");
		ArrayList pipeChildren = pipeline.getSuccesors();
		for (int i = 0; i < pipeChildren.size(); i++)
		{
			System.out.println("Child  "+ i + " = " + ((GEStreamNode) pipeChildren.get(i)).getName());
		}
		/* 
		 * DEBUGGING CODE END
		*********************************************************** */		
		
		return pipeline;
    }
    
    /**
     * Visit a SIRSplitJoin
     * @return Visited SIRSplitJoin encoded as a GESplitJoin.
     */
    public Object visitSplitJoin(SIRSplitJoin self,
                                 JFieldDeclaration[] fields,
                                 JMethodDeclaration[] methods,
                                 JMethodDeclaration init,
                                 SIRSplitter splitter,
                                 SIRJoiner joiner) {

		System.out.println("***** Entering visitSplitJoin");
                                 	
		// Visit the splitter and joiner 
		GESplitter split = (GESplitter)splitter.accept(this);
		GEJoiner join = (GEJoiner) joiner.accept(this);

		
		
		GESplitJoin splitjoin =  new GESplitJoin(self.getName(), split, join);
		split.setEncapsulatingNode(splitjoin);
		join.setEncapsulatingNode(splitjoin);
	
		// ...and walk through the body.
		Iterator iter = self.getParallelStreams().iterator();
		while (iter.hasNext()) {
			
			SIROperator oper = (SIROperator)iter.next();
			GEStreamNode strNode = (GEStreamNode)oper.accept(this);		
	 		split.addChild(strNode);
			strNode.addChild(join);		
			strNode.setEncapsulatingNode(splitjoin);
		}
		
		
		graph.addHierarchy(splitjoin, split.getSuccesors());
		
		/* ***********************************************************
	     * DEBUGGING CODE BEGIN
		*/
		System.out.println("SplitJoin "+ splitjoin.getName() + " has the following children");
		System.out.println("\t Split = "+ splitjoin.getSplitter().getName());
		System.out.println("\t Join = "+ splitjoin.getJoiner().getName());
		
		ArrayList sjChildren = splitjoin.getSuccesors();
		
		for (int i = 0; i < sjChildren.size(); i++)
		{
			System.out.println("\tInner Child "+ i+ " = " +((GEStreamNode) sjChildren.get(i)).getName());
		}
		
		/* 
		 * DEBUGGING CODE END
	 	*********************************************************** */		
			
		return splitjoin;

		
                               
    }

    /**
     * Visit a SIRFeedbackLoop
     * @return Visited SIRFeedbackLoop encoded as a GEFeedbackLoop.
     */ 
    public Object visitFeedbackLoop(SIRFeedbackLoop self,
                                    JFieldDeclaration[] fields,
                                    JMethodDeclaration[] methods,
                                    JMethodDeclaration init,
                                    JMethodDeclaration initPath) {
										NamePair np;
    
    	System.out.println("***** Entering visitFeedbackLoop");
    	
		// Visit the splitter and joiner.
		GESplitter split = (GESplitter) self.getSplitter().accept(this);
		GEJoiner join = (GEJoiner) self.getJoiner().accept(this);

		GEStreamNode body = (GEStreamNode) self.getBody().accept(this);
		GEStreamNode loop = (GEStreamNode) self.getLoop().accept(this);

		join.addChild(body);
		body.addChild(split);
		split.addChild(loop);
		loop.addChild(join);
	
		GEFeedbackLoop floop = new GEFeedbackLoop(self.getName(), split, join, body, loop);
	
		/* ***********************************************************
		 * DEBUGGING CODE BEGIN
	   	 */
		System.out.println("FeedbackLoop "+ floop.getName() + " has the following children");
		System.out.println("\t Split = "+ floop.getSplitter().getName());
		System.out.println("\t Join = "+ floop.getJoiner().getName());
		System.out.println("\t Body = "+floop.getBody().getName());			
		System.out.println("\t Loop = " +floop.getLoop().getName()); 
		 
		/* 
		 * DEBUGGING CODE END
		*********************************************************** */		
					
		return floop;
    }
}
