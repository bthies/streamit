package at.dms.kjc.sir.lowering;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import java.util.List;
import java.util.ListIterator;
import java.util.LinkedList;
import java.util.HashMap;

/**
 * This creates structures (inner classes) to encapsulate the state of
 * each hierarchical unit of a stream graph.
 */
public class Structurer extends at.dms.util.Utils implements SIRVisitor {
    /**
     * List of the class declarations defined during traversal
     */
    private LinkedList structs;

    /**
     * Creates a new structurer.
     */
    private Structurer() {
	this.structs = new LinkedList();
    }

    /**
     * Returns an array of class declaration's corresponding to the
     * state structures used within <toplevel>.  Also mutates the
     * stream structure within <toplevel> so that each function within
     * a stream takes its state as the first parameter, and references
     * fields via the state.
     */
    public static JClassDeclaration[] structure(SIROperator toplevel) {
	Structurer f = new Structurer();
	toplevel.accept(f);
	return (JClassDeclaration[])f.structs.toArray();
    }

    /**
     * Creates a structure with name <name> and adds it to <structs>
     * given a set of <fields> and <children> of a stream structure.  
     */
    private void createStruct(String name,
			      JFieldDeclaration[] fields, 
			      List children) {

	// there is one field for each child, plus one for the context
	JFieldDeclaration[] classFields = new JFieldDeclaration[children.size()
							       + fields.length
							       + 1];

	// pos tracks our filling up of <classFields>
	int pos = 0;

	// fill in the context
	classFields[pos++] = LoweringConstants.getContextField();

	// fill in the fields of the class
	for (int i=0; i<fields.length; i++) {
	    classFields[pos++] = fields[i];
	}

	// fill in the children
	for (int i=0; i<children.size(); i++) {
	    // the name of the type in the structure
	    String typeName = ((SIROperator)children.get(i)).getName();
	    // the name for the variable in the structure
	    String varName = "child" + i;
	    // define a variable of the structure
	    JVariableDefinition var = 
		new JVariableDefinition(/* tokenref */ null, 
					/* modifiers */ at.dms.kjc.
					Constants.ACC_PUBLIC,
					/* type */ CClassType.lookup(typeName),
					/* identifier  */ varName,
					/* initializer */ null);
	    // define the field
	    classFields[pos++] = 
		new JFieldDeclaration(/* tokenref */ null, 
				      /* variable */ var, 
				      /* javadoc  */ null, 
				      /* comments */ null);
	}

	// make the inner class

	JClassDeclaration classDecl = 
	    new JClassDeclaration(/* TokenReference where */
				  null,
				  /* int modifiers, */
				  at.dms.kjc.Constants.ACC_PUBLIC,
				  /* String ident,  */
				  name,
				  /* CClassType superClass, */
				  CStdType.Object,
				  /* CClassType[] interfaces, */
				  CClassType.EMPTY,
				  /* JFieldDeclaration[] fields, */
				  fields,
				  /* JMethodDeclaration[] methods, */
				  null,
				  /* JTypeDeclaration[] inners, */
				  null,
				  /* JPhylum[] initializers, */
				  null,
				  /* JavadocComment javadoc, */
				  null,
				  /* JavaStyleComment[] comment */
				  null
				  );

	// add class to list
	structs.add(classDecl);
    }

    /**
     * For each method in <methods>, belonging to stream named
     * <streamName>, add a parameter representing the structure of
     * state, and change all references to state to be references to
     * the structure.  
     */
    private void addStructReferences(String streamName,
				     JMethodDeclaration[] methods) {
	// for each method
	for (int i=0; i<methods.length; i++) {
	    // add the parameter
	    addParameter(streamName, methods[i]);
	    // for each statement in the method, change references
	    JStatement[] body = methods[i].getStatements();
	}
    }

    /**
     * Adds a parameter to the beginning of the parameter list of
     * <meth> of type <type>.
     */
    private void addParameter(String type, JMethodDeclaration meth) {
	meth.addParameter(new JFormalParameter(/* tokref */ null,
					       /* desc */ 
					       JLocalVariable.DES_PARAMETER,
					       /* type */
					       CClassType.lookup(type),
					       /* name */
					       LoweringConstants.PARAM_NAME,
					       /* isFinal */
					       false));
    }

    /**
     * PLAIN-VISITS 
     */
     
    /* visit a filter */
    public void visitFilter(SIRFilter self,
			    SIRStream parent,
			    JFieldDeclaration[] fields,
			    JMethodDeclaration[] methods,
			    JMethodDeclaration init,
			    int peek, int pop, int push,
			    JMethodDeclaration work,
			    CType inputType, CType outputType) {
	// create struct type
	createStruct(self.getName(), fields, EMPTY_LIST);
	// add closure-referencing to methods
	addStructReferences(self.getName(), methods);
    }
  
    /* visit a splitter */
    public void visitSplitter(SIRSplitter self,
			      SIRStream parent,
			      SIRSplitType type,
			      int[] weights) {
	fail("Not implemented yet");
    }
  
    /* visit a joiner */
    public void visitJoiner(SIRJoiner self,
			    SIRStream parent,
			    SIRJoinType type,
			    int[] weights) {
	fail("Not implemented yet");
    }

    /**
     * PRE-VISITS 
     */
	    
    /* pre-visit a pipeline */
    public void preVisitPipeline(SIRPipeline self,
				 SIRStream parent,
				 JFieldDeclaration[] fields,
				 JMethodDeclaration[] methods,
				 JMethodDeclaration init,
				 List elements) {
	// don't do anything--visit on the way up
    }
  
    /* pre-visit a splitjoin */
    public void preVisitSplitJoin(SIRSplitJoin self,
				  SIRStream parent,
				  JFieldDeclaration[] fields,
				  JMethodDeclaration[] methods,
				  JMethodDeclaration init) {
	fail("Not implemented yet");
    }
  
    /* pre-visit a feedbackloop */
    public void preVisitFeedbackLoop(SIRFeedbackLoop self,
				     SIRStream parent,
				     JFieldDeclaration[] fields,
				     JMethodDeclaration[] methods,
				     JMethodDeclaration init,
				     int delay,
				     JMethodDeclaration initPath) {
	fail("Not implemented yet");
    }
  
    /**
     * POST-VISITS 
     */
	    
    /* post-visit a pipeline */
    public void postVisitPipeline(SIRPipeline self,
				  SIRStream parent,
				  JFieldDeclaration[] fields,
				  JMethodDeclaration[] methods,
				  JMethodDeclaration init,
				  List elements) {
	// create structure
	createStruct(self.getName(), fields, elements);
	// add closure-referencing to methods
	addStructReferences(self.getName(), methods);
    }
  
    /* post-visit a splitjoin */
    public void postVisitSplitJoin(SIRSplitJoin self,
				   SIRStream parent,
				   JFieldDeclaration[] fields,
				   JMethodDeclaration[] methods,
				   JMethodDeclaration init) {
	fail("Not implemented yet");
    }
  
    /* post-visit a feedbackloop */
    public void postVisitFeedbackLoop(SIRFeedbackLoop self,
				      SIRStream parent,
				      JFieldDeclaration[] fields,
				      JMethodDeclaration[] methods,
				      JMethodDeclaration init,
				      int delay,
				      JMethodDeclaration initPath) {
	fail("Not implemented yet");
    }
}
