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
    
	    

	public void encode(SIRStream str)
	{
		// PENDING 
		// ADD code to set str as the toplevel stream in the graph structure 		
		str.accept(this);
	}

    
    /* visit a structure */
    public Object visitStructure(SIRStructure self,
                                 JFieldDeclaration[] fields) {
   		//REMOVED TO COMPILE
   		//
   		//
   		//
        //return new GEStreamNode(self.getIdent(), "");
        return null;
    }
    
    /* visit a filter */
    public Object visitFilter(SIRFilter self,
                              JFieldDeclaration[] fields,
                              JMethodDeclaration[] methods,
                              JMethodDeclaration init,
                              JMethodDeclaration work,
                              CType inputType, CType outputType) 
   {

		System.out.println("Entering visitFilter for filter " + self.getName());                              	
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
    
    /* visit a phased filter */
    public Object visitPhasedFilter(SIRPhasedFilter self,
                                    JFieldDeclaration[] fields,
                                    JMethodDeclaration[] methods,
                                    JMethodDeclaration init,
                                    JMethodDeclaration work,
                                    SIRWorkFunction[] initPhases,
                                    SIRWorkFunction[] phases,
                                    CType inputType, CType outputType)
	{
		System.out.println("Entering visitPhasedFilter");
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
    
    /* visit a splitter */
    public Object visitSplitter(SIRSplitter self,
                                SIRSplitType type,
                                JExpression[] expWeights) 
	{
		System.out.println("Entering visitSplitter");
		try 
		{
			return new GESplitter(type.toString(), self.getWeights());
		}
		catch (Exception e) 
		{
			return null;
		}
	}
    
    /* visit a joiner */
    public Object visitJoiner(SIRJoiner self,
                              SIRJoinType type,
                              JExpression[] expWeights) 
    {
		System.out.println("Entering visitJoiner");
		try {
			return new GEJoiner(type.toString(), self.getWeights());
		}
		catch (Exception e) {
			return null;
		} 
    }
    
    /* visit a work function */
    public Object visitWorkFunction(SIRWorkFunction self,
                                    JMethodDeclaration work) 
    {
		System.out.println("Entering visitWorkFunction");
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
		
        /* Pre-visit a pipeline
     */
    public Object visitPipeline(SIRPipeline self,
                                JFieldDeclaration[] fields,
                                JMethodDeclaration[] methods,
                                JMethodDeclaration init) {                 	
		
		System.out.println("Entering visitPipeline "+ self.getName());
                                	
		GEPipeline pipeline = new GEPipeline(self.getName());        
        
		// Walk through each of the elements in the pipeline.
		Iterator iter = self.getChildren().iterator();
		while (iter.hasNext())
		{
			SIROperator oper = (SIROperator)iter.next();
	
			GEStreamNode currNode = (GEStreamNode) oper.accept(this);
			pipeline.addChild(currNode);
				
		}
		  
		graph.addHierarchy(pipeline, pipeline.getChildren());
		
		
		
		
		return pipeline;
    }
    
    /* Pre-visit a splitjoin 
     */
    public Object visitSplitJoin(SIRSplitJoin self,
                                 JFieldDeclaration[] fields,
                                 JMethodDeclaration[] methods,
                                 JMethodDeclaration init,
                                 SIRSplitter splitter,
                                 SIRJoiner joiner) {

		System.out.println("Entering visitSplitJoin");
                                 	
		// Visit the splitter and joiner 
		GESplitter split = (GESplitter)splitter.accept(this);
		GEJoiner join = (GEJoiner) joiner.accept(this);
		
		
	
		// ...and walk through the body.
		Iterator iter = self.getParallelStreams().iterator();
		while (iter.hasNext()) {
			
			SIROperator oper = (SIROperator)iter.next();
			GEStreamNode strNode = (GEStreamNode)oper.accept(this);		
			split.addChild(strNode);
			strNode.addChild(join);		
		
		}
		
		GESplitJoin splitjoin =  new GESplitJoin(split, join);
		graph.addHierarchy(splitjoin, splitjoin.getChildren());
			
		return splitjoin;

		
                               
    }

    /* pre-visit a feedbackloop */
    public Object visitFeedbackLoop(SIRFeedbackLoop self,
                                    JFieldDeclaration[] fields,
                                    JMethodDeclaration[] methods,
                                    JMethodDeclaration init,
                                    JMethodDeclaration initPath) {
										NamePair np;
    
    	System.out.println("Entering visitFeedbackLoop");
    	
		// Visit the splitter and joiner.
		GESplitter split = (GESplitter) self.getSplitter().accept(this);
		GEJoiner join = (GEJoiner) self.getJoiner().accept(this);

		GEStreamNode body = (GEStreamNode) self.getBody().accept(this);
		GEStreamNode loop = (GEStreamNode) self.getLoop().accept(this);

		join.addChild(body);
		body.addChild(split);
		split.addChild(loop);
		loop.addChild(join);
		
		return new GEFeedbackLoop(split, join, body, loop);
    }
}
