package at.dms.kjc.sir.lowering;

import at.dms.util.*;
import at.dms.kjc.*;
import at.dms.kjc.iterator.*;
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
     * The renamed methods.  Map of old method name to new method name.
     * this maps interned strings -> strings
     */
    private final HashMap renamedMethods;

    /**
     * Creates a new structurer.
     */
    private Structurer() {
	this.structs = new LinkedList();
	this.flatMethods = new LinkedList();
	this.renamedMethods = new HashMap();
    }

    /**
     * Returns an array of class declaration's corresponding to the
     * state structures used within <toplevel>.  Also mutates the
     * stream structure within <toplevel> so that each function within
     * a stream takes its state as the first parameter, and references
     * fields via the state.  Finally, puts interface declarations
     * <inners> and interface tables <tables> in toplevel structure.
     */
    public static JClassDeclaration structure(SIRIterator toplevel,
					      JInterfaceDeclaration[] inners,
					      SIRInterfaceTable[] tables,
                                              SIRStructure[] structures) {
	Structurer structurer = new Structurer();
	toplevel.accept(structurer);
	return structurer.toFlatClass(inners, tables, structures);
    }

    /**
     * Returns a flattened class representing the stream structure
     * that was traversed.
     */
    private JClassDeclaration toFlatClass(JInterfaceDeclaration[] inners,
					  SIRInterfaceTable[] tables,
                                          SIRStructure[] structures) {
	// add <inners> to <structs>
	structs.addAll(Arrays.asList(inners));
        // Process structures
        for (int i=0; i<structures.length; i++)
            doStructure(structures[i]);
	// create a field declaration that is initialized to each interface
	// table in <tables>
	JFieldDeclaration[] fields = new JFieldDeclaration[tables.length];
	for (int i=0; i<fields.length; i++) {
	    fields[i] 
		= new JFieldDeclaration(/* tokref */ null,
					/* variable */ LoweringConstants.
					getInterfaceTableVariable(tables[i]),
					/* javadoc comment */ null,
					/* comments */ null);
	}

        /* addition of dummy field to struct Main */ 
        //Required for passing the C code through certain C compilers like MIPS/VIRAM cc. 
        //Those C compilers bail out with error if a struct is without any field.) - nmani 
        if (fields.length == 0)
        { 
	    fields = new JFieldDeclaration[1];
          
	    // define the dummy variable 
	    JVariableDefinition dummyvar = 
		new JVariableDefinition(/* tokenref */ null, 
					/* modifiers */ at.dms.kjc.
					Constants.ACC_PUBLIC,
					/* type - any type would do */ CStdType.Integer,
					/* identifier */ "__dummy__",
					/* initializer */ null);
	    // define the field for the dummyvar 
	    fields[0] = 
		new JFieldDeclaration(/* tokenref */ null, 
				      /* variable */ dummyvar, 
				      /* javadoc  */ null, 
				      /* comments */ null);
        } 

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
				     fields,
				     /* JMethodDeclaration[] methods */
				     (JMethodDeclaration[])
				     flatMethods.toArray(JMethodDeclaration.
							 EMPTY()),
				     /* JTypeDeclaration[] inners */
				     (JTypeDeclaration[])
				     structs.toArray(new JTypeDeclaration[0]),
				     /* JPhylum[] initializers */
				     JClassDeclaration.EMPTY,
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
	    String typeName = ((SIRStream)children.get(i)).getTypeNameInC();
	    // the name for the variable in the structure
	    String varName = ((SIRStream)children.get(i)).getRelativeName();
	    assert varName!=null:
                "Relative name null for " + 
                children.get(i) + " ; might be a child with a " + 
                "wrong parent field?";
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
				  JMethodDeclaration.EMPTY(),
				  /* JTypeDeclaration[] inners, */
				  JClassDeclaration.EMPTY,
				  /* JPhylum[] initializers, */
				  JClassDeclaration.EMPTY,
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
	// flatten each method
	for (int i=0; i<methods.length; i++) {
            flattenMethod(streamName, methods[i]);
	}
	// change method calls to refer to new name, and to pass a
	// data structure as the first argument
	for (int i=0; i<methods.length; i++) {
	    methods[i].accept(new SLIREmptyVisitor() {
		    public void visitMethodCallExpression(JMethodCallExpression
							  self,
							  JExpression prefix,
							  String ident,
							  JExpression[] args) {
			// do the super
			super.visitMethodCallExpression(self, prefix, 
							ident, args);
			// if we're calling one of our own methods...
			if (prefix instanceof JThisExpression) {
			    // if <ident> has been renamed...
			    String newName = (String)renamedMethods.get(ident);
			    if (newName!=null) {
				// rename the call
				self.setIdent(newName);
			    }
			    // add data argument
			    self.addArgFirst(LoweringConstants.getDataField());
			}
		    }
		});
	}
    }

    /**
     * Flatten a single method, in the same way flattenMethods()
     * does.
     */
    private void flattenMethod(String streamName,
                               JMethodDeclaration method) {
	// get new method name
	String newName = LoweringConstants.getMethodName(streamName, 
							 method.getName());
	// record change of name in <renamedMethods>
	renamedMethods.put(method.getName().intern(), newName);
        // rename the method
        method.setName(newName);
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

    /* visit a structure */
    public void doStructure(SIRStructure self) {
	JClassDeclaration classDecl = 
	    new JClassDeclaration(/* TokenReference where */
				  null,
				  /* int modifiers, */
				  at.dms.kjc.Constants.ACC_PUBLIC |
                                  at.dms.kjc.Constants.ACC_STATIC,
				  /* String ident,  */
				  self.getIdent(),
				  /* CClassType superClass, */
				  CStdType.Object,
				  /* CClassType[] interfaces, */
				  CClassType.EMPTY,
				  /* JFieldDeclaration[] fields, */
				  self.getFields(),
				  /* JMethodDeclaration[] methods, */
				  JMethodDeclaration.EMPTY(),
				  /* JTypeDeclaration[] inners, */
				  JClassDeclaration.EMPTY,
				  /* JPhylum[] initializers, */
				  JClassDeclaration.EMPTY,
				  /* JavadocComment javadoc, */
				  null,
				  /* JavaStyleComment[] comment */
				  null
				  );

	// add class to list
	structs.add(classDecl);
    }
     
    /* visit a filter */
    public void visitFilter(SIRFilter self,
			    SIRFilterIter iter) {
	// only worry about actual SIRFilter's, not special cases like
	// FileReader's and FileWriter's
	if (!self.needsWork()) {
	    return;
	}
	// create struct type
	createStruct(self.getTypeNameInC(), self.getFields(), EMPTY_LIST);
	// add tape parameters to work function
	//addTapeParameters(work);
	// add closure-referencing to methods
	flattenMethods(self.getTypeNameInC(), self.getMethods());
    }

    /* visit a phased filter */
    public void visitPhasedFilter(SIRPhasedFilter self,
                                  SIRPhasedFilterIter iter) {
	// create struct type
	createStruct(self.getTypeNameInC(), self.getFields(), EMPTY_LIST);
	// add tape parameters to work function
	//addTapeParameters(work);
	// add closure-referencing to methods
	flattenMethods(self.getTypeNameInC(), self.getMethods());
    }
  
    /**
     * PRE-VISITS 
     */
	    
    /* pre-visit a pipeline */
    public void preVisitPipeline(SIRPipeline self,
				 SIRPipelineIter iter) {
	// don't do anything--visit on the way up
    }
  
    /* pre-visit a splitjoin */
    public void preVisitSplitJoin(SIRSplitJoin self,
				  SIRSplitJoinIter iter) {
	// don't do anything--visit on the way up
    }
  
    /* pre-visit a feedbackloop */
    public void preVisitFeedbackLoop(SIRFeedbackLoop self,
				     SIRFeedbackLoopIter iter) {
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
				  SIRPipelineIter iter) {
	postVisit(self.getTypeNameInC(), self.getFields(), self.getMethods(), self.getChildren());
    }
  
    /* post-visit a splitjoin */
    public void postVisitSplitJoin(SIRSplitJoin self,
				   SIRSplitJoinIter iter) {
	postVisit(self.getTypeNameInC(), self.getFields(), self.getMethods(), self.getParallelStreams());
    }
  
    /* post-visit a feedbackloop */
    public void postVisitFeedbackLoop(SIRFeedbackLoop self,
				      SIRFeedbackLoopIter iter) {
	// make a list of body and loop
	List children = new LinkedList();
	children.add(self.getBody());
	children.add(self.getLoop());
	// do visit
	postVisit(self.getTypeNameInC(), self.getFields(), self.getMethods(), children);
        // deal with initPath, too
        //flattenMethod(self.getTypeNameInC(), initPath);
    }

    /**
     * This class replaces all references to local fields with a reference
     * to a state object that is passed as a parameter.
     */
    static class FieldResolver extends SLIREmptyVisitor {

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
		if (left.getType()==null) {
		    new RuntimeException("found null type of field in structurer for field " + self);
		}
	    }
	}
    
    }
}
