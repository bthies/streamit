package at.dms.kjc;

import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.*;
import at.dms.util.*;
import java.util.*;
import java.io.*;

/**
 * This class does the front-end processing to turn a Kopi compilation
 * unit into StreamIt classes, and then prints the class graph as a
 * dot file.
 */
public class StreamItDot implements AttributeStreamVisitor
{
    private PrintStream outputStream;

    /**
     * Inner class to represent dot graph components.
     */
    class NamePair
    {
        /** Name of the first node in an object. */
        String first;
        /** Name of the last node in an object. */
        String last;
        /** Create a new NamePair with both fields null. */
        NamePair() { first = null; last = null; }
        /** Create a new NamePair with both fields the same. */
        NamePair(String s) { first = s; last = s; }
        /** Create a new NamePair with two different names. */
        NamePair(String f, String l) { first = f; last = l; }
    }
    
    private int lastNode;

    public StreamItDot(PrintStream outputStream) {
	lastNode = 0;
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
        print("digraph streamit {\n");
        stream.accept(this);
        print("}\n");
    }

    /**
     * Prints dot graph of <str> to System.out
     */
    public static void printGraph(SIRStream str) {
	str.accept(new StreamItDot(System.out));
    }

    /**
     * Prints dot graph of <str> to <filename>
     */
    public static void printGraph(SIRStream str, String filename) {
	try {
	    FileOutputStream out = new FileOutputStream(filename);
	    StreamItDot dot = new StreamItDot(new PrintStream(out));
	    dot.print("digraph streamit {\n");
	    str.accept(dot);
	    dot.print("}\n");
	    out.flush();
	    out.close();
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    void print(String f) 
    {
	outputStream.print(f);
    }

    void printEdge(String from, String to)
    {
        if (from == null || to == null)
            return;
        print(from + " -> " + to + "\n");
    }

    String makeEmptyNode()
    {
        String name = getName();
        print(name + " [ style=invis ]\n");
        return name;
    }

    String makeLabelledNode(String label)
    {
        String name = getName();
        if (label == null) label = name;
        print(name + " [ label=\"" + label + "\" ]\n");
        return name;
    }

    String makeLabelledInvisNode(String label)
    {
        String name = getName();
        if (label == null) label = name;
        print(name + " [ label=\"" + label + "\" ]\n");
        return name;
    }

    String getName()
    {
        lastNode++;
        return "node" + lastNode;
    }

    /* visit a structure */
    public Object visitStructure(SIRStructure self,
                                 JFieldDeclaration[] fields) 
    {
        return new NamePair(makeLabelledNode(self.getIdent()));
    }

    /* visit a filter */
    public Object visitFilter(SIRFilter self,
                              JFieldDeclaration[] fields,
                              JMethodDeclaration[] methods,
                              JMethodDeclaration init,
                              JMethodDeclaration work,
                              CType inputType, CType outputType)
    {
        // Return a name pair with both ends pointing to this.
	//        return new NamePair(makeLabelledNode(self.getRelativeName()));
	String label = self.getIdent();
	try {
	    label += "\\npush=" + self.getPushInt();
	    label += "\\npop=" + self.getPopInt();
	    label += "\\npeek=" + self.getPeekInt();
	    if (self instanceof SIRTwoStageFilter) {
		SIRTwoStageFilter two = (SIRTwoStageFilter)self;
		label += "\\ninitPush=" + two.getInitPush();
		label += "\\ninitPop=" + two.getInitPop();
		label += "\\ninitPeek=" + two.getInitPeek();
	    }
	} catch (Exception e) {
	    // if constants not resolved for the ints, will get an exception
	}
	
	return new NamePair(makeLabelledNode(label));
    }
    
    /* visit a splitter */
    public Object visitSplitter(SIRSplitter self,
                                SIRSplitType type,
                                JExpression[] expWeights)
    {
	String label = type.toString();
	// try to add weights to label
	try {
	    int[] weights = self.getWeights();
	    label += "(";
	    for (int i=0; i<weights.length; i++) {
		label += weights[i];
		if (i!=weights.length-1) {
		    label+=",";
		}
	    }
	    label += ")";
	} catch (Exception e) {}
        // Create an empty node and return it.
        return new NamePair(makeLabelledInvisNode(label));
    }
    
    /* visit a joiner */
    public Object visitJoiner(SIRJoiner self,
                              SIRJoinType type,
                              JExpression[] expWeights)
    {
	String label = type.toString();
	// try to add weights to label
	try {
	    int[] weights = self.getWeights();
	    label += "(";
	    for (int i=0; i<weights.length; i++) {
		label += weights[i];
		if (i!=weights.length-1) {
		    label+=",";
		}
	    }
	    label += ")";
	} catch (Exception e) {}
        return new NamePair(makeLabelledInvisNode(label));
    }
    
    /* pre-visit a pipeline */
    public Object visitPipeline(SIRPipeline self,
                                JFieldDeclaration[] fields,
                                JMethodDeclaration[] methods,
                                JMethodDeclaration init)
    {
        NamePair pair = new NamePair();
        
        // Print this within a subgraph.
        print("subgraph cluster_" + getName() + " {\n");
        
        // Walk through each of the elements in the pipeline.
        Iterator iter = self.getChildren().iterator();
        while (iter.hasNext())
        {
            SIROperator oper = (SIROperator)iter.next();
            NamePair p2 = (NamePair)oper.accept(this);
            printEdge(pair.last, p2.first);
            // Update the known edges.
            if (pair.first == null)
                pair.first = p2.first;
            pair.last = p2.last;
        }

        print("}\n");
        return pair;
    }

    /* pre-visit a splitjoin */
    public Object visitSplitJoin(SIRSplitJoin self,
                                 JFieldDeclaration[] fields,
                                 JMethodDeclaration[] methods,
                                 JMethodDeclaration init,
                                 SIRSplitter splitter,
                                 SIRJoiner joiner)
    {
        NamePair pair = new NamePair();
        
        // Create a subgraph again...
        print("subgraph cluster_" + getName() + " {\n");

        // Visit the splitter and joiner to get their node names...
        NamePair np;
        np = (NamePair)splitter.accept(this);
        pair.first = np.first;
        np = (NamePair)joiner.accept(this);
        pair.last = np.last;

        // ...and walk through the body.
        Iterator iter = self.getParallelStreams().iterator();
        while (iter.hasNext())
        {
            SIROperator oper = (SIROperator)iter.next();
            np = (NamePair)oper.accept(this);
            printEdge(pair.first, np.first);
            printEdge(np.last, pair.last);
        }

        print("}\n");
        return pair;
    }

    /* pre-visit a feedbackloop */
    public Object visitFeedbackLoop(SIRFeedbackLoop self,
                                    JFieldDeclaration[] fields,
                                    JMethodDeclaration[] methods,
                                    JMethodDeclaration init,
                                    JMethodDeclaration initPath)
    {
        NamePair np;
        
        // Create a subgraph again...
        print("subgraph cluster_" + getName() + " {\n");

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
