package at.dms.kjc.sir.lowering;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import java.util.List;
import java.util.ListIterator;
import java.util.LinkedList;
import java.util.HashMap;

/**
 * This serves to flatten a stream structure by encapsulating the
 * state in a set of closures instead of as a hierarchy.
 */
public class Flattener extends at.dms.util.Utils implements SIRVisitor {
    /**
     * List of the class declarations defined during traversal
     */
    private LinkedList structs;
    /**
     * mapping from stream structure (SIROperator) to name (String) of
     * structure in target program
     */
    private HashMap names;

    /**
     * Creates a new flattener.
     */
    private Flattener(HashMap names) {
	this.structs = new LinkedList();
    }

    /**
     * Returns an array of class declaration's corresponding to the
     * state structures used within <toplevel>.  Also mutates the
     * stream structure within <toplevel> so that each function within
     * a stream takes its state as the first parameter, and references
     * fields via the state.
     */
    public static JClassDeclaration[] flatten(SIROperator toplevel) {
	Flattener f = new Flattener(Namer.name(toplevel));
	toplevel.accept(f);
	return (JClassDeclaration[])f.structs.toArray();
    }

    /**
     * Retrieves the name for <str> (requires that it's been given a name).
     */
    private String getName(SIROperator str) {
	return (String)names.get(str);
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
	    String typeName = (String)names.get(children.get(i));
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
	createStruct(getName(self), fields, EMPTY_LIST);
	// add parameters to all the methods
	for (int i=0; i<methods.length; i++) {
	    addParameter(getName(self), methods[i]);
	}
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
	createStruct(getName(self), fields, elements);
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

class Namer extends at.dms.util.Utils implements SIRVisitor {
    /**
     * mapping from stream structure (SIROperator) to name (String) of
     * structure in target program
     */
    private HashMap names;
    /**
     * Prefix of numbers used for naming
     */
    private LinkedList namePrefix;

    
    /**
     * Make a new namer.
     */
    private Namer() {
	this.names = new HashMap();
	this.namePrefix = new LinkedList();
    }

    /**
     * Return mapping of stream objects to names.
     */
    public static HashMap name(SIROperator str) {
	Namer namer = new Namer();
	str.accept(namer);
	return namer.names;
    }

    /**
     * Associates a name with <str>, saving the result in <names>.
     */
    private void addName(SIROperator str) {
	StringBuffer name = new StringBuffer();
	// start name with list of positions, e.g. 1_2_1_
	for (ListIterator e = namePrefix.listIterator(); e.hasNext(); ) {
	    name.append(e.next());
	    name.append("_");
	}
	// end name with the class of the IR object
	String suffix = splitQualifiedName(str.getClass().toString(), '.')[1];
	// associate name with <str>
	names.put(str, name);
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
	addName(self);
	// increment the count on the prefix names
	Integer old = (Integer)namePrefix.removeLast();
	namePrefix.add(new Integer(old.intValue()+1));
    }

    /** 
     * visit a splitter 
     */
    public void visitSplitter(SIRSplitter self,
			      SIRStream parent,
			      SIRSplitType type,
			      int[] weights) {
	fail("Not supported yet.");
    }

    /** 
     * visit a joiner 
     */
    public void visitJoiner(SIRJoiner self,
			    SIRStream parent,
			    SIRJoinType type,
			    int[] weights) {
	fail("Not supported yet.");
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
	addName(self);
	// start counting children with namePrefix
	namePrefix.add(new Integer(1));
    }

    /* pre-visit a splitjoin */
    public void preVisitSplitJoin(SIRSplitJoin self,
				  SIRStream parent,
				  JFieldDeclaration[] fields,
				  JMethodDeclaration[] methods,
				  JMethodDeclaration init) {
	fail("Not supported yet.");
    }

    /* pre-visit a feedbackloop */
    public void preVisitFeedbackLoop(SIRFeedbackLoop self,
				     SIRStream parent,
				     JFieldDeclaration[] fields,
				     JMethodDeclaration[] methods,
				     JMethodDeclaration init,
				     int delay,
				     JMethodDeclaration initPath) {
	fail("Not supported yet.");
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
	// stop counting children by removing a digit from namePrefix
	namePrefix.removeLast();
    }

    /* post-visit a splitjoin */
    public void postVisitSplitJoin(SIRSplitJoin self,
				   SIRStream parent,
				   JFieldDeclaration[] fields,
				   JMethodDeclaration[] methods,
				   JMethodDeclaration init) {
	fail("Not supported yet.");
    }


    /* post-visit a feedbackloop */
    public void postVisitFeedbackLoop(SIRFeedbackLoop self,
				      SIRStream parent,
				      JFieldDeclaration[] fields,
				      JMethodDeclaration[] methods,
				      JMethodDeclaration init,
				      int delay,
				      JMethodDeclaration initPath) {
	fail("Not supported yet.");
    }
}
