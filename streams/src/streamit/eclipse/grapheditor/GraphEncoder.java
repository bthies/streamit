package grapheditor;

import at.dms.kjc.sir.*;
import at.dms.kjc.*;
import java.io.*;
import java.util.*;

//import java.util.*;

public class GraphEncoder implements AttributeStreamVisitor {
    //May Want outputStream or stdout. Not sure
    private PrintStream outputStream;
    private ArrayList nodesList;
    

    //ADDED TO COMPILE

    private int lastNode;

    public void print(String f) 
    {
	outputStream.print(f);
    }

    void printEdge(String from, String to)
    {
        if (from == null || to == null)
            return;
        print(from + " -> " + to + "\n");
    }

    /**
     * Prints out the subgraph cluser line that is needed in to make clusters. This method is overridden to make colored
     * pipelines and splitjoins in LinearDot.
     **/
    public String getClusterString(SIRStream self) {
	return "subgraph cluster_" + getName() + " {\n label=\"" + self.getName() + "\";\n";
    }
    
    public String getName()
    {
        lastNode++;
        return "node" + lastNode;
    }

    //END ADDED

    public GraphEncoder() 
    {
	this.nodesList = new ArrayList();
	
	//Feel free to add arguments and call it correctly
	//Setup an output stream to output to here
	//For now we are using System.out
	//Later we can change to outputting to file perhaps
	
	this.outputStream = System.out;
    }
    
    public GraphEncoder(PrintStream outputStream) 
    {
	this.nodesList = new ArrayList();
	
	//Feel free to add arguments and call it correctly
	//Setup an output stream to output to here
	//For now we are using System.out
	//Later we can change to outputting to file perhaps
	
	this.outputStream = outputStream;
    }
    
	/**
	 * Prints out a dot graph for the program being compiled.
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
	 * Prints dot graph of <str> to System.out
	 */
	public static void printGraph(SIRStream str) 
	{
		str.accept(new GraphEncoder(System.out));
	}

	/**
	 * Prints dot graph of <str> to <filename>
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
                              	
		GEPhasedFilter phFilter = new GEPhasedFilter();					
		try 
		{
			SIRWorkFunction[] phases = self.getPhases();
			for (int i = 0; i < phases.length; i++)
			{
				
	// REMOVED TO ALLOW COMPILE			
	//			phFilter.addWorkFunction(new GEWorkFunction(phases[i].getName(), 
	//														phases[i].getPushInt(), 
	//														phases[i].getPopInt(), 
	//														phases[i].getPeekInt()));
			}
			phases = self.getInitPhases();
			for (int i = 0; i < phases.length; i++)
			{
				
	//REMOVED TO ALLOW COMPILE	
	//			phFilter.addInitWorkFunction (new GEWorkFunction(phases[i].getName(), 
	//															 phases[i].getPushInt(), 
	//															 phases[i].getPopInt(), 
	//															 phases[i].getPeekInt()));
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
		
        
		//###
	   	// Establish this is a subgraph cluster
		//###
		print(getClusterString(self));
     
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
                                JExpression[] expWeights) {
	return null;
	
    }
    
    /* visit a joiner */
    public Object visitJoiner(SIRJoiner self,
                              SIRJoinType type,
                              JExpression[] expWeights) {
	return null;
	
    }
    
    /* visit a work function */
    public Object visitWorkFunction(SIRWorkFunction self,
                                    JMethodDeclaration work) 
    {
		try 
		{
			GEWorkFunction wf = new GEWorkFunction(work.getName(), self.getPushInt(), self.getPopInt(),self.getPeekInt());
			return wf;
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
        //###
		// Establish this is a subgraph cluster
		//###
		print(getClusterString(self));

        
		// Walk through each of the elements in the pipeline.
		Iterator iter = self.getChildren().iterator();
		while (iter.hasNext())
		{
			SIROperator oper = (SIROperator)iter.next();
//			REMOVED TO ALLOW COMPILE	
	//		GEStreamNode currNode = oper.accept(this);
			
			//###
			// Instead of printEdge, add the edge relationship representation to the graph
			//###
//			REMOVED TO ALLOW COMPILE	
//			graphStruct.createEdge(lastNode, currNode);	
//		 	pipe.addPipeStage(currNode); 	//
			// Update the known edges.
//			lastNode = currNode;
			
		}
		return null;
    }
    
    /* Pre-visit a splitjoin 
     */
    public Object visitSplitJoin(SIRSplitJoin self,
                                 JFieldDeclaration[] fields,
                                 JMethodDeclaration[] methods,
                                 JMethodDeclaration init,
                                 SIRSplitter splitter,
                                 SIRJoiner joiner) {
									NamePair pair = new NamePair();
        
	//###
	// Establish this is a subgraph cluster
	//###
	print(getClusterString(self));
	

	// Visit the splitter and joiner to get their node names...
	NamePair np;
	np = (NamePair)splitter.accept(this);
	pair.first = np.first;
	np = (NamePair)joiner.accept(this);
	pair.last = np.last;

	// ...and walk through the body.
	Iterator iter = self.getParallelStreams().iterator();
	while (iter.hasNext()) {
		SIROperator oper = (SIROperator)iter.next();
		np = (NamePair)oper.accept(this);
		
		//###
		// Instead of printEdge, add the edge relationship representation to the graph
		//###	
		printEdge(pair.first, np.first);
		printEdge(np.last, pair.last);
	}
	
	return pair;                              	
                    	                               	
                               
    }

    /* pre-visit a feedbackloop */
    public Object visitFeedbackLoop(SIRFeedbackLoop self,
                                    JFieldDeclaration[] fields,
                                    JMethodDeclaration[] methods,
                                    JMethodDeclaration init,
                                    JMethodDeclaration initPath) {
										NamePair np;
        
	//###
	// Establish this is a subgraph cluster
	//###
	print(getClusterString(self));
	

	// Visit the splitter and joiner.
	np = (NamePair)self.getJoiner().accept(this);
	String joinName = np.first;
	np = (NamePair)self.getSplitter().accept(this);
	String splitName = np.first;

	// Visit the body and the loop part.
	np = (NamePair)self.getBody().accept(this);
	printEdge(joinName, np.first);
	printEdge(np.last, splitName);
	np = (NamePair)self.getLoop().accept(this);
	
	printEdge(splitName, np.first);
	printEdge(np.last, joinName);

											   print("}\n");
											   return new NamePair(joinName, splitName);
	
	
    }
}
