package at.dms.kjc.sir.lowering;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import java.util.List;
import java.util.ListIterator;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.Arrays;

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
     * fields via the state.  Finally, puts interface declarations
     * <inners> in toplevel structure.
     */
    public static JClassDeclaration structure(SIROperator toplevel,
					      JInterfaceDeclaration[] inners) {
	Structurer structurer = new Structurer();
	toplevel.accept(structurer);
	return structurer.toFlatClass(inners);
    }

    /**
     * Returns a flattened class representing the stream structure
     * that was traversed.
     */
    private JClassDeclaration toFlatClass(JInterfaceDeclaration[] inners) {
	// add <inners> to <structs>
	structs.addAll(Arrays.asList(inners));
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
				     structs.toArray(new JTypeDeclaration[0]),
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

	// there is one field for each child, plus one for the context,
	// plus two for the in and out tapes
	JFieldDeclaration[] classFields = 
	    new JFieldDeclaration[children.size()
				 + fields.length
				 + 1 /* context */
				 + 2 /* in/out tapes */];

	// pos tracks our filling up of <classFields>
	int pos = 0;

	// fill in the context
	classFields[pos++] = LoweringConstants.getContextField();

	// fill in the in/out tapes
	classFields[pos++] = LoweringConstants.getInTapeField();
	classFields[pos++] = LoweringConstants.getOutTapeField();

	// fill in the fields of the class
	for (int i=0; i<fields.length; i++) {
	    classFields[pos++] = fields[i];
	}

	// fill in the children
	for (int i=0; i<children.size(); i++) {
	    // the name of the type in the structure
	    String typeName = ((SIROperator)children.get(i)).getName();
	    // the name for the variable in the structure
	    String varName = ((SIROperator)children.get(i)).getRelativeName();
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
	for (int i=0; i<methods.length; i++)
            flattenMethod(streamName, methods[i]);
    }

    /**
     * Flatten a single method, in the same way flattenMethods()
     * does.
     */
    private void flattenMethod(String streamName,
                               JMethodDeclaration method) {
        // rename the method
        method.setName(LoweringConstants.
                       getMethodName(streamName, 
                                     method.getName()));
        // add the parameter
        addParameter(method, 
                     streamName, 
                     LoweringConstants.STATE_PARAM_NAME);
        // for each statement in the method, change references
        KjcVisitor resolver = new FieldResolver();
        ListIterator statements = method.getStatementIterator();
        while (statements.hasNext()) {
            ((JStatement)statements.next()).accept(resolver);
        }
        // add <method> to the list of flattened methods
        flatMethods.add(method);
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
	//addTapeParameters(work);
	// add closure-referencing to methods
	flattenMethods(self.getName(), methods);
    }
  
    /* visit a splitter */
    public void visitSplitter(SIRSplitter self,
			      SIRStream parent,
			      SIRSplitType type,
			      JExpression[] weights) {
	// create struct type (no - not needed anymore by runtime)
	// createStruct(self.getName(), JFieldDeclaration.EMPTY, EMPTY_LIST);
    }
  
    /* visit a joiner */
    public void visitJoiner(SIRJoiner self,
			    SIRStream parent,
			    SIRJoinType type,
			    JExpression[] weights) {
	// create struct type (no - not needed anymore by runtime)
	// createStruct(self.getName(), JFieldDeclaration.EMPTY, EMPTY_LIST);
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
	// don't do anything--visit on the way up
    }
  
    /* pre-visit a feedbackloop */
    public void preVisitFeedbackLoop(SIRFeedbackLoop self,
				     SIRStream parent,
				     JFieldDeclaration[] fields,
				     JMethodDeclaration[] methods,
				     JMethodDeclaration init,
				     int delay,
				     JMethodDeclaration initPath) {
	// don't do anything--visit on the way up
    }
  
    /**
     * POST-VISITS 
     */

    /**
     * Performs the standard post-visit for hierarchical nodes.
     */
    private void postVisit(String name, 
			   JFieldDeclaration[] fields,
			   JMethodDeclaration[] methods,
			   List children) {
	// create structure
	createStruct(name, fields, children);
	// if there are methods, add closure-referencing to methods
	if (methods!=null) {
	    flattenMethods(name, methods);
	}
    }
	    
    /* post-visit a pipeline */
    public void postVisitPipeline(SIRPipeline self,
				  SIRStream parent,
				  JFieldDeclaration[] fields,
				  JMethodDeclaration[] methods,
				  JMethodDeclaration init,
				  List elements) {
	postVisit(self.getName(), fields, methods, self.getChildren());
    }
  
    /* post-visit a splitjoin */
    public void postVisitSplitJoin(SIRSplitJoin self,
				   SIRStream parent,
				   JFieldDeclaration[] fields,
				   JMethodDeclaration[] methods,
				   JMethodDeclaration init) {
	postVisit(self.getName(), fields, methods, self.getParallelStreams());
    }
  
    /* post-visit a feedbackloop */
    public void postVisitFeedbackLoop(SIRFeedbackLoop self,
				      SIRStream parent,
				      JFieldDeclaration[] fields,
				      JMethodDeclaration[] methods,
				      JMethodDeclaration init,
				      int delay,
				      JMethodDeclaration initPath) {
	// make a list of body and loop
	List children = new LinkedList();
	children.add(self.getBody());
	children.add(self.getLoop());
	// do visit
	postVisit(self.getName(), fields, methods, children);
        // deal with initPath, too
        flattenMethod(self.getName(), initPath);
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
