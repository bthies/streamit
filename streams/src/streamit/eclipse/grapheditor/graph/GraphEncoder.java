package streamit.eclipse.grapheditor.graph;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;

import streamit.eclipse.grapheditor.graph.utils.StringTranslator;
import at.dms.kjc.CType;
import at.dms.kjc.JExpression;
import at.dms.kjc.JFieldDeclaration;
import at.dms.kjc.JInterfaceDeclaration;
import at.dms.kjc.JMethodDeclaration;
import at.dms.kjc.sir.AttributeStreamVisitor;
import at.dms.kjc.sir.SIRFeedbackLoop;
import at.dms.kjc.sir.SIRFilter;
import at.dms.kjc.sir.SIRInterfaceTable;
import at.dms.kjc.sir.SIRJoinType;
import at.dms.kjc.sir.SIRJoiner;
import at.dms.kjc.sir.SIROperator;
import at.dms.kjc.sir.SIRPhasedFilter;
import at.dms.kjc.sir.SIRPipeline;
import at.dms.kjc.sir.SIRSplitJoin;
import at.dms.kjc.sir.SIRSplitType;
import at.dms.kjc.sir.SIRSplitter;
import at.dms.kjc.sir.SIRStream;
import at.dms.kjc.sir.SIRStructure;
import at.dms.kjc.sir.SIRWorkFunction;

/**
 * GraphEncoder "visits" the nodes SIRStream structures in order to build a GraphStructure
 * representation of the StreamIt program.
 *  
 * @author jcarlos
 */
public class GraphEncoder implements AttributeStreamVisitor {
 
    //May Want outputStream or stdout. Not sure
    private PrintStream outputStream;
    public static GraphStructure graph;

    public GraphEncoder() 
    {
    	System.out.println("Created GraphEncoder that ouputs to System.out");
		GraphEncoder.graph = new GraphStructure();
	
		//Feel free to add arguments and call it correctly
		//Setup an output stream to output to here
		//For now we are using System.out
		//Later we can change to outputting to file perhaps
	
		this.outputStream = System.out;
	
    }
    
    /**
     * Main entry point from {@link at.dms.kjc.StreaMITMain}.
     * Creates a new <code>GraphEncoder</code> object, and
     * encodes <code>str</code> using it.
     */
    public static void run(SIRStream str,
                           JInterfaceDeclaration[] interfaces,
                           SIRInterfaceTable[] interfaceTables,
                           SIRStructure[] structs)
    {
        System.err.println("Dumping graph..");
        new GraphEncoder().encode(str);
    }

    public GraphEncoder(PrintStream outputStream) 
    {
		System.out.println("Created GraphEncoder that ouputs to outputStream");
		GraphEncoder.graph = new GraphStructure();
		this.outputStream = outputStream;
    }
    
	/** 
	 * Start visiting the nodes in order to encode them in the graph 
	 * structure beginning with the toplevel node. 
	 */
	public void encode(SIRStream str)
	{
	 
		graph.setTopLevel((GEContainer) str.accept(this));
		
		/* ***********************************************************
		 * DEBUGGING CODE BEGIN
		 */
		 /*
		System.out.println("The toplevel stream is "+ graph.getTopLevel().getName());
		
		ArrayList topChildren = ((GEContainer) graph.getTopLevel()).getContainedElements();
		for (int i = 0; i< topChildren.size(); i++)
		{
			 System.out.println("The children of the toplevel are " + ((GEStreamNode) topChildren.get(i)).getName());
			 	  
		}
	
		//graph.constructGraph();
	
		System.out.println("End of Test");
		*/
		/* 
		 * DEBUGGING CODE END
		 * *********************************************************** */

	}
			
		 
    /**
     * Visit a SIRStructure
     */
    public Object visitStructure(SIRStructure self,
                                 JFieldDeclaration[] fields) {
   		//TODO: REMOVED TO COMPILE
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
		
		/** Create a GEPhasedFilter that will represent the current filter.*/                              	
		GEPhasedFilter phFilter = new GEPhasedFilter(StringTranslator.removeUnderscore(self.getName()));
		
		/** Must set the output/intput tape of the filter */
		phFilter.setInputTape(self.getInputType().toString());
		phFilter.setOutputTape(self.getOutputType().toString());
						
		try 
		{
			
			/** Get the work functions and add them to the GEPhasedFilter */
			SIRWorkFunction[] phases = self.getPhases();
			for (int i = 0; i < phases.length; i++)
			{	
				phFilter.addWorkFunction(new GEWorkFunction(work.getName(), 
															phases[i].getPushInt(), 
															phases[i].getPopInt(), 
															phases[i].getPeekInt()));
			}
			/** Get the init work functions and add them to the GEPhasedFilter */
			phases = self.getInitPhases();
			for (int i = 0; i < phases.length; i++)
			{
				phFilter.addInitWorkFunction (new GEWorkFunction(work.getName(), 
																 phases[i].getPushInt(), 
																 phases[i].getPopInt(), 
																 phases[i].getPeekInt()));
			}			
		}
		/** If constants not resolved for the ints, will get an exception */
		catch (Exception e) 
		{ 
			System.out.println("Exception thrown " + e.toString());
		}

			/* ***********************************************************
			 * DEBUGGING CODE BEGIN
			 
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
     * Visit a SIRPhasedFilter in order to create its GEPhasedFilter representation. 
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
		
		/** Create a GEPhasedFilter that will represent the current filter.*/
    	GEPhasedFilter phFilter = new GEPhasedFilter(StringTranslator.removeUnderscore(self.getName()));
		
		/** Must set the output/intput tape of the filter */
		phFilter.setInputTape(self.getInputType().toString());
		phFilter.setOutputTape(self.getOutputType().toString());
    	
		/** Walk through each of the phases. */
		if (initPhases != null)
		{
			/** Get the init work functions and add them to the GEPhasedFilter */
			for (int i = 0; i < initPhases.length; i++)
			{   
				GEWorkFunction wf = (GEWorkFunction) initPhases[i].accept(this);
				phFilter.addInitWorkFunction(wf);
			}
		}
		if (phases != null)
		{
			/** Get the work functions and add them to the GEPhasedFilter */
	   		for (int i = 0; i < phases.length; i++)
	   		{
				GEWorkFunction wf = (GEWorkFunction) initPhases[i].accept(this);
				phFilter.addWorkFunction(wf);
	   		}
		}        
						
		/* ***********************************************************
		 * DEBUGGING CODE BEGIN
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
     * Visit a SIRSplitter in order to create its GESplitter representation.
     * @return Visited SIRSplitter encoded as a GESplitter. 
     */
    public Object visitSplitter(SIRSplitter self,
                                SIRSplitType type,
                                JExpression[] expWeights) 
	{
		try 
		{
			/** Create a GESplitter that will represent the current splitter.*/
			GESplitter splitter = new GESplitter(StringTranslator.removeUnderscore(self.getName()), 
												 self.getWeights());
			
			/* ***********************************************************
			 * DEBUGGING CODE BEGIN
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
     * Visit a SIRJoiner in order to create its GEJoiner representation. 
     * @return Visited SIRJoiner encoded as a GEJoiner. 
     */
    public Object visitJoiner(SIRJoiner self,
                              SIRJoinType type,
                              JExpression[] expWeights) 
    {
		try 
		{
			/** Create a GEJoiner that will represent the current joiner.	*/
			GEJoiner joiner = new GEJoiner(StringTranslator.removeUnderscore(self.getName()), 
										   self.getWeights());
			
			/* ***********************************************************
			 * DEBUGGING CODE BEGIN
			 	
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
		try 
		{
			return new GEWorkFunction(work.getName(), self.getPushInt(), self.getPopInt(),self.getPeekInt());
		} 
		/** If constants not resolved for the ints, will get an exception */
		catch (Exception e) 
		{
			System.out.println("Exception thrown " + e.toString() + " in visitWorkFunction");
			return null;
		}
	}
		
    /**
     * Visit a SIRPipeline in order to create its GEPipeline representation. 
     * @return Visited SIRPipeline encoded as a GEPipeline.
     */
    public Object visitPipeline(SIRPipeline self,
                                JFieldDeclaration[] fields,
                                JMethodDeclaration[] methods,
                                JMethodDeclaration init) {                 	
		/** Create a GEPipeline that will represent the current pipeline.*/
		GEPipeline pipeline = new GEPipeline(StringTranslator.removeUnderscore(self.getName()));
		
		/** Must set the output/intput tape of the pipeline */
		pipeline.setInputTape(self.getInputType().toString());
		pipeline.setOutputTape(self.getOutputType().toString());
		
		/** Add the elements in the pipeline to the GEPipeline*/ 
		Iterator iter = self.getChildren().iterator();
		while (iter.hasNext())
		{
			SIROperator oper = (SIROperator)iter.next();
			GEStreamNode currNode = (GEStreamNode) oper.accept(this);
			pipeline.addNodeToContainer(currNode);		
		}
		  		
		/* ***********************************************************
		 * DEBUGGING CODE BEGIN
		
		System.out.println("The pipeline "+ pipeline.getName() + " has the following children");
		ArrayList pipeChildren = pipeline.getContainedElements();
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
     * Visit a SIRSplitJoin in order to create its GESplitJoin representation. 
     * @return Visited SIRSplitJoin encoded as a GESplitJoin.
     */
    public Object visitSplitJoin(SIRSplitJoin self,
                                 JFieldDeclaration[] fields,
                                 JMethodDeclaration[] methods,
                                 JMethodDeclaration init,
                                 SIRSplitter splitter,
                                 SIRJoiner joiner) 
	{
		System.out.println("***** Entering visitSplitJoin");
                                 	
		/** Visit the splitter and joiner corresponding to the current splitjoin */
		GESplitter split = (GESplitter)splitter.accept(this);
		GEJoiner join = (GEJoiner) joiner.accept(this);
	
		/** Create a GESplitJoin that will represent the current splitjoin.	*/
		GESplitJoin splitjoin =  new GESplitJoin(StringTranslator.removeUnderscore(self.getName()), 
												 split, join);
												 
		/** Must set the output/intput tape of the splitjoin */
		splitjoin.setInputTape(self.getInputType().toString());
		splitjoin.setOutputTape(self.getOutputType().toString());
	
		/** Add the inner nodes (the ones branching out of the splitter) to the splitjoin*/
		Iterator iter = self.getParallelStreams().iterator();
		while (iter.hasNext()) 
		{	
			SIROperator oper = (SIROperator)iter.next();
			GEStreamNode strNode = (GEStreamNode)oper.accept(this);				
			splitjoin.addNodeToContainer(strNode);	
		}

		
		/* ***********************************************************
	     * DEBUGGING CODE BEGIN
		
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
     * Visit a SIRFeedbackLoop in order to create its GEFeedbackLoop representation. 
     * @return Visited SIRFeedbackLoop encoded as a GEFeedbackLoop.
     */ 
    public Object visitFeedbackLoop(SIRFeedbackLoop self,
                                    JFieldDeclaration[] fields,
                                    JMethodDeclaration[] methods,
                                    JMethodDeclaration init,
                                    JMethodDeclaration initPath) 
	{
    	System.out.println("***** Entering visitFeedbackLoop");
    	
		/** Visit the splitter and joiner corresponding to the current feedbackloop */
		GESplitter split = (GESplitter) self.getSplitter().accept(this);
		GEJoiner join = (GEJoiner) self.getJoiner().accept(this);

		/** Visit the body and loop corresponding to the current feedbackloop */
		GEStreamNode body = (GEStreamNode) self.getBody().accept(this);
		GEStreamNode loop = (GEStreamNode) self.getLoop().accept(this);
	
		/** Create a GEFeedbackLoop that will represent the current feedbackloop.*/
		GEFeedbackLoop floop = new GEFeedbackLoop(StringTranslator.removeUnderscore(self.getName()), split, join, body, loop);
		floop.setInputTape(self.getInputType().toString());
		floop.setOutputTape(self.getOutputType().toString());
		

		/* ***********************************************************
		 * DEBUGGING CODE BEGIN
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
