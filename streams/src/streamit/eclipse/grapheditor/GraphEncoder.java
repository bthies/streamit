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
    
    public GraphEncoder(PrintStream outputStream) 
    {
		this.nodesList = new ArrayList();
	
		//Feel free to add arguments and call it correctly
		//Setup an output stream to output to here
		//For now we are using System.out
		//Later we can change to outputting to file perhaps
	
		this.outputStream = System.out;
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
                              CType inputType, CType outputType) {
	
    }
    
    /* visit a phased filter */
    public Object visitPhasedFilter(SIRPhasedFilter self,
                                    JFieldDeclaration[] fields,
                                    JMethodDeclaration[] methods,
                                    JMethodDeclaration init,
                                    JMethodDeclaration work,
                                    SIRWorkFunction[] initPhases,
                                    SIRWorkFunction[] phases,
                                    CType inputType, CType outputType) {
	
    }
    
    /* visit a splitter */
    public Object visitSplitter(SIRSplitter self,
                                SIRSplitType type,
                                JExpression[] expWeights) {
	
    }
    
    /* visit a joiner */
    public Object visitJoiner(SIRJoiner self,
                              SIRJoinType type,
                              JExpression[] expWeights) {
	
    }
    
    /* visit a work function */
    public Object visitWorkFunction(SIRWorkFunction self,
                                    JMethodDeclaration work) {
	return new Node("WORK_FUNCTION");
    }
    
    /* Pre-visit a pipeline
     */
    public Object visitPipeline(SIRPipeline self,
                                JFieldDeclaration[] fields,
                                JMethodDeclaration[] methods,
                                JMethodDeclaration init) {                 	
                                	
		NamePair pair = new NamePair();
        
        //###
		// Establish this is a subgraph cluster
		//###
		print(getClusterString(self));

        
		// Walk through each of the elements in the pipeline.
		Iterator iter = self.getChildren().iterator();
		while (iter.hasNext())
		{
			SIROperator oper = (SIROperator)iter.next();
			NamePair p2 = (NamePair)oper.accept(this);
			
			//###
			// Instead of printEdge, add the edge relationship representation to the graph
			//###
				printEdge(pair.last, p2.first);
		
			// Update the known edges.
			if (pair.first == null)
			   pair.first = p2.first;
			pair.last = p2.last;
		}
		return pair;
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
	
    }
}
