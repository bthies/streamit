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
		this.graph = new GraphStructure();
	
		//Feel free to add arguments and call it correctly
		//Setup an output stream to output to here
		//For now we are using System.out
		//Later we can change to outputting to file perhaps
	
		this.outputStream = System.out;
    }
    
    public GraphEncoder(PrintStream outputStream) 
    {
		this.graph = new GraphStructure();
		this.outputStream = outputStream;
    }
    
	/**
	 * Creates graph structure for the program being compiled.
	 */
	public void compile(JCompilationUnit[] app) 
	{
		Kopi2SIR k2s = new Kopi2SIR(app);
		SIRStream stream = null;
		for (int i = 0; i < app.length; i++)
		{
			SIRStream top = (SIRStream)app[i].accept(k2s);
			if (top != null)
				stream = top;
		}
        
		if (stream == null)
		{
			System.err.println("No top-level stream defined!");
			System.exit(-1);
		}

		// Use the visitor.
		stream.accept(this);
	}
    
	/**
	 * creates graph structure of <str> to System.out
	 */
	public static void printGraph(SIRStream str) 
	{
		str.accept(new GraphEncoder(System.out));
	}

	/**
	 * Creates grapg structure of <str> in <filename>
	 */
	public static void printGraph(SIRStream str, String filename) 
	{
		try 
		{
			FileOutputStream out = new FileOutputStream(filename);
			GraphEncoder graphEnc = new GraphEncoder(new PrintStream(out));
			str.accept(graphEnc);
			out.flush();
			out.close();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
        
   /*public void print(String f) {
      outputStream.print(f);
   }*/
    
    /* visit a structure */
    public Object visitStructure(SIRStructure self,
                                 JFieldDeclaration[] fields) {
        return new GEStreamNode(self.getIdent(), "");
    }
    
    /* visit a filter */
    public Object visitFilter(SIRFilter self,
                              JFieldDeclaration[] fields,
                              JMethodDeclaration[] methods,
                              JMethodDeclaration init,
                              JMethodDeclaration work,
                              CType inputType, CType outputType) 
   {
                              	
		GEPhasedFilter phFilter = new GEPhasedFilter();					
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
		}
				
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
     
    	GEPhasedFilter phFilter = new GEPhasedFilter();
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
						
		return phFilter;
	
    }
    
    /* visit a splitter */
    public Object visitSplitter(SIRSplitter self,
                                SIRSplitType type,
                                JExpression[] expWeights) 
	{
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
                                	
		GEPipeline pipeline = new GEPipeline();        
        
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
