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
public class Structurer extends at.dms.util.Utils implements StreamVisitor {
    /**
     * List of the class declarations defined during traversal
     */
    private LinkedList structs;
    /**
     * List of methods that have been flattened.
     */
    private LinkedList flatMethods;

    /**
     * Creates a new structurer.
     */
    private Structurer() {
	this.structs = new LinkedList();
	this.flatMethods = new LinkedList();
    }

    /**
     * Returns an array of class declaration's corresponding to the
     * state structures used within <toplevel>.  Also mutates the
     * stream structure within <toplevel> so that each function within
     * a stream takes its state as the first parameter, and references
     * fields via the state.
     */
    public static JClassDeclaration structure(SIROperator toplevel) {
	Structurer structurer = new Structurer();
	toplevel.accept(structurer);
	return structurer.toFlatClass();
    }

    /**
     * Returns a flattened class representing the stream structure
     * that was traversed.
     */
    private JClassDeclaration toFlatClass() {
	// construct resulting class
	return new JClassDeclaration(/* TokenReference where */
				     null,
				     /* int modifiers */
				     at.dms.kjc.Constants.ACC_PUBLIC,
				     /* java.lang.String ident */
				     "Main",
				     /* CClassType superClass */
				     null,
				    /* CClassType[] interfaces */
				     CClassType.EMPTY,
				     /* JFieldDeclaration[] fields */
				     JFieldDeclaration.EMPTY,
				     /* JMethodDeclaration[] methods */
				     (JMethodDeclaration[])
				     flatMethods.toArray(JMethodDeclaration.
							 EMPTY),
				     /* JTypeDeclaration[] inners */
				     (JTypeDeclaration[])
				     structs.toArray(JClassDeclaration.EMPTY),
				     /* JPhylum[] initializers */
				     null,
				     /* JavadocComment javadoc */
				     null,
				     /* JavaStyleComment[] comment */
				     null);
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
				  classFields,
				  /* JMethodDeclaration[] methods, */
				  JMethodDeclaration.EMPTY,
				  /* JTypeDeclaration[] inners, */
				  JClassDeclaration.EMPTY,
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
     * Requires that <methods> is not null. 
     *
     * For each method in <methods>, belonging to stream named
     * <streamName>, "flatten" the method by:
     *   1. adding a parameter for the state of the stream structure
     *   2. changing all field references within the function to reference the
     *      contents of the stream structure
     *   3. renaming the method to a "unique" name that is formed by 
     *      prepending the stream name to the original method name
     */
    private void flattenMethods(String streamName,
				JMethodDeclaration[] methods) {
	// for each method
	for (int i=0; i<methods.length; i++) {
	    // rename the method
	    methods[i].setName(streamName+"_"+methods[i].getName());
	    // add the parameter
	    addParameter(methods[i], 
			 streamName, 
			 LoweringConstants.STATE_PARAM_NAME);
	    // for each statement in the method, change references
	    JStatement[] statements = methods[i].getStatements();
	    KjcVisitor resolver = new FieldResolver();
	    for (int j=0; j<statements.length; j++) {
		statements[j].accept(resolver);
	    }
	    // add <method> to the list of flattened methods
	    flatMethods.add(methods[i]);
	}
    }

    /**
     * For the work method <work>, add two parameters corresponding to the
     * input and output tapes.
     */
    private void addTapeParameters(JMethodDeclaration work) {
	// add parameter for output tape
	addParameter(work, 
		     LoweringConstants.TAPE_TYPE_NAME,
		     LoweringConstants.OUTPUT_TAPE_NAME);
	// add parameter for input tape
	addParameter(work, 
		     LoweringConstants.TAPE_TYPE_NAME,
		     LoweringConstants.INPUT_TAPE_NAME);
    }

    /**
     * Adds a parameter to the beginning of the parameter list of
     * <meth> of type <typeName> and name <varName>.
     */
    private void addParameter(JMethodDeclaration meth, 
			      String typeName, 
			      String varName) {
	meth.addParameter(new JFormalParameter(/* tokref */ null,
					       /* desc */ 
					       JLocalVariable.DES_PARAMETER,
					       /* type */
					       CClassType.lookup(typeName),
					       /* name */
					       varName, 
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
	// add tape parameters to work function
	addTapeParameters(work);
	// add closure-referencing to methods
	flattenMethods(self.getName(), methods);
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
	// if there are methods, add closure-referencing to methods
	if (methods!=null) {
	    flattenMethods(self.getName(), methods);
	}
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

/**
 * This class replaces all references to local fields with a reference
 * to a state object that is passed as a parameter.
 */
class FieldResolver extends KjcEmptyVisitor {

    /**
     * visits a field expression
     */
    public void visitFieldExpression(JFieldAccessExpression self,
				     JExpression left,
				     String ident) {
	// for <this> expressions, replace the LHS with a refernce to
	// the structure
	if (self.isThisAccess()) {
	    self.setPrefix(new JNameExpression(/* tokref */ 
					       null,
					       /* ident */
					       LoweringConstants.
					       STATE_PARAM_NAME));
	}
    }
    
}
