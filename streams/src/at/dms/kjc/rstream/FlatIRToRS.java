package at.dms.kjc.rstream;

import at.dms.kjc.common.*;
import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.FlatVisitor;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.iterator.*;
import at.dms.util.Utils;
import java.util.List;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.HashMap;
import java.io.*;
import at.dms.compiler.*;
import at.dms.kjc.sir.lowering.*;
import java.util.Hashtable;
import at.dms.util.SIRPrinter;

/**
 * This class converts the Stream IR (which references the Kopi Java IR)
 * to C code and dumps it to a file, str.c.    
 *
 *
 * @author Michael Gordon
 */
public class FlatIRToRS extends ToC implements StreamVisitor
{
    /** if true generate do loops when identified **/
    private final boolean GENERATE_DO_LOOPS = false;
    /** the hashmap of for loops -> do loops **/   
    private HashMap doloops;
    /** the current filter we are visiting **/
    private SIRFilter filter;

    
    /**
     * The entry method to this C conversion pass.  Given a flatnode containing
     * the single fused filter of the application, optimize the SIR code, if
     * enabled, and then generate then convert to C code and dump to a file.
     *
     * @param node The flatnode containing the single filter of the application.
     *
     */
    public static void generateCode(FlatNode node) 
    {
	FlatIRToRS toC = new FlatIRToRS((SIRFilter)node.contents);
		
	//optimizations...
	System.out.println
	    ("Optimizing SIR ...");

	ArrayDestroyer arrayDest=new ArrayDestroyer();

	//iterate over all the methods, calling the magic below...
	for (int i = 0; i < ((SIRFilter)node.contents).getMethods().length; i++) {
	    JMethodDeclaration method=((SIRFilter)node.contents).getMethods()[i];
	    	    
	    if (!KjcOptions.nofieldprop) {
		Unroller unroller;
		do {
		    do {
			unroller = new Unroller(new Hashtable());
			method.accept(unroller);
		    } while (unroller.hasUnrolled());
		    
		    method.accept(new Propagator(new Hashtable()));
		    unroller = new Unroller(new Hashtable());
		    method.accept(unroller);
		} while(unroller.hasUnrolled());
		
		method.accept(new BlockFlattener());
		method.accept(new Propagator(new Hashtable()));
	    } 
	    else
		method.accept(new BlockFlattener());
	    method.accept(arrayDest);
	    method.accept(new VarDeclRaiser());
	}
	
	if(KjcOptions.destroyfieldarray)
	   arrayDest.destroyFieldArrays((SIRFilter)node.contents);
	   /*	
	     try {
	     SIRPrinter printer1 = new SIRPrinter();
	     IterFactory.createFactory().createIter((SIRFilter)node.contents).accept(printer1);
	     printer1.close();
	     }
	     catch (Exception e) 
	     {
	     }
	*/
	//find all do loops, 
	toC.doloops = IDDoLoops.doit(node);
	//now iterate over all the methods and generate the c code.
        IterFactory.createFactory().createIter((SIRFilter)node.contents).accept(toC);
    }
    
    
    public FlatIRToRS()
    {
	this.str = new StringWriter();
        this.p = new TabbedPrintWriter(str);
	doloops = new HashMap();
    }

    
    public FlatIRToRS(SIRFilter f) {
	this.filter = f;
	this.str = new StringWriter();
        this.p = new TabbedPrintWriter(str);
	doloops = new HashMap();

    }


    /**
     * The main entry point of the visiting done by this class. 
     * print out c includes, visit the methods, and then generate
     * the main function in the c code that calls the driver function
     * that controls execution.
     *
     * @param self The filter we are visiting
     *
     */

    public void visitFilter(SIRFilter self,
			    SIRFilterIter iter) {
	//Entry point of the visitor

	print("#include <stdlib.h>\n");
	print("#include <math.h>\n\n");

	//if there are structures in the code, include
	//the structure definition header files
	//if (StrToRStream.structures.length > 0) 
	//    print("#include \"structs.h\"\n");
		
	//Visit fields declared in the filter class
	JFieldDeclaration[] fields = self.getFields();
	for (int i = 0; i < fields.length; i++)
	   fields[i].accept(this);
	
	//visit methods of filter, print the declaration first
	declOnly = true;
	JMethodDeclaration[] methods = self.getMethods();
	for (int i =0; i < methods.length; i++)
	    methods[i].accept(this);
	
	//now print the functions with body
	declOnly = false;
	for (int i =0; i < methods.length; i++) {
	    methods[i].accept(this);	
	}
	
	print("int main() {\n");
	
	//execute the main function
	print(Names.main + "();\n");
	
	//return 0 even though this should never return!
	print("  return 0;\n");
	//closes main()
	print("}\n");
       
	createFile();
    }

    private void createFile() {
	System.out.println("Code for application written to str.c");
	try {
	    FileWriter fw = new FileWriter("str.c");
	    fw.write(str.toString());
	    fw.close();
	}
	catch (Exception e) {
	    System.err.println("Unable to write tile code file for filter " +
			       filter.getName());
	}
    }

    //stack allocate the array
    protected void stackAllocateArray(String ident) {
        //find the dimensions of the array!!
        String dims[] =
            ArrayDim.findDim(new FlatIRToRS(), filter.getFields(), method, ident);
	
        for (int i = 0; i < dims.length; i++)
            print("[" + dims[i] + "]");
        return;
    }

    
    /**
     * prints an assignment expression
     */
    public void visitAssignmentExpression(JAssignmentExpression self,
                                          JExpression left,
                                          JExpression right) {

	//do not print class creation expression
	if (passParentheses(right) instanceof JQualifiedInstanceCreation ||
	    passParentheses(right) instanceof JUnqualifiedInstanceCreation ||
	    passParentheses(right) instanceof JQualifiedAnonymousCreation ||
	    passParentheses(right) instanceof JUnqualifiedAnonymousCreation)
	    return;

	//print the correct code for array assignment
	//this must be run after renaming!!!!!!
	if (left.getType() == null || right.getType() == null) {
	    lastLeft=left;
	    print("(");
	    left.accept(this);
	    print(" = ");
	    right.accept(this);
	    print(")");
	    return;
 	}

	if ((left.getType().isArrayType()) &&
	     ((right.getType().isArrayType() || right instanceof SIRPopExpression) &&
	      !(right instanceof JNewArrayExpression))) {
	    	    
	    String ident = "";
	    	    
	    if (left instanceof JFieldAccessExpression) 
		ident = ((JFieldAccessExpression)left).getIdent();
	    else if (left instanceof JLocalVariableExpression) 
		ident = ((JLocalVariableExpression)left).getVariable().getIdent();
	    else 
		Utils.fail("Assigning an array to an unsupported expression of type " + left.getClass() + ": " + left);
	    
	    String[] dims = ArrayDim.findDim(new FlatIRToRS(), filter.getFields(), method, ident);
	    //if we cannot find the dim, just create a pointer copy
	    if (dims == null) {
		lastLeft=left;
		print("(");
		left.accept(this);
		print(" = ");
		right.accept(this);
		print(")");
		return;
	    }
	    print("{\n");
	    print("int ");
	    //print the index var decls
	    for (int i = 0; i < dims.length -1; i++)
		print(Names.ARRAY_COPY + i + ", ");
	    print(Names.ARRAY_COPY + (dims.length - 1));
	    print(";\n");
	    for (int i = 0; i < dims.length; i++) {
		print("for (" + Names.ARRAY_COPY + i + " = 0; " + Names.ARRAY_COPY + i +  
		      " < " + dims[i] + "; " + Names.ARRAY_COPY + i + "++)\n");
	    }
	    left.accept(this);
	    for (int i = 0; i < dims.length; i++)
		print("[" + Names.ARRAY_COPY + i + "]");
	    print(" = ");
	    right.accept(this);
	    for (int i = 0; i < dims.length; i++)
		print("[" + Names.ARRAY_COPY + i + "]");
	    print(";\n}\n");
	    return;
	}

	//stack allocate all arrays when not in init function
	//done at the variable definition
	if (right instanceof JNewArrayExpression &&
 	    (left instanceof JLocalVariableExpression) && !isInit) {
	    //	    (((CArrayType)((JNewArrayExpression)right).getType()).getArrayBound() < 2)) {

	    //get the basetype and print it 
	    CType baseType = ((CArrayType)((JNewArrayExpression)right).getType()).getBaseType();
	    print(baseType + " ");
	    //print the identifier
	    left.accept(this);
	    //print the dims of the array
	    String ident;
	    ident = ((JLocalVariableExpression)left).getVariable().getIdent();
	    stackAllocateArray(ident);
	    return;
	}
           

	
	lastLeft=left;
        print("(");
        left.accept(this);
        print(" = ");
        right.accept(this);
        print(")");
    }
    


    /**
     * prints a variable declaration statement
     */
    public void visitVariableDefinition(JVariableDefinition self,
                                        int modifiers,
                                        CType type,
                                        String ident,
                                        JExpression expr) {
        //we want to stack allocate all arrays not in the init
        //we convert an assignment statement into the stack allocation statement'
        //so, just remove the var definition, if the new array expression
        //is not included in this definition, just remove the definition,
        //when we visit the new array expression we will print the definition...
        if (type.isArrayType() && !isInit) {
            String[] dims = ArrayDim.findDim(new FlatIRToRS(), filter.getFields(), method, ident);
            //but only do this if the array has corresponding
            //new expression, otherwise don't print anything.
            if (expr instanceof JNewArrayExpression) {
                //print the type
                print(((CArrayType)type).getBaseType() + " ");
                //print the field identifier
                print(ident);
                //print the dims
                stackAllocateArray(ident);
                print(";");
                return;
            }
            else if (dims != null)
                return;
            else if (expr instanceof JArrayInitializer) {
                print(((CArrayType)type).getBaseType() + " " +
                      ident + "[" + ((JArrayInitializer)expr).getElems().length + "] = ");
                expr.accept(this);
                print(";");
                return;
            }
        }
    if (expr!=null) {
            printLocalType(type);
        } else {
            print(type);
        }
        print(" ");
        print(ident);
        if (expr != null) {
            print(" = ");
            expr.accept(this);
        } else {
            if (type.isOrdinal())
                print (" = 0");
            else if (type.isFloatingPoint())
                print(" = 0.0f");
        }

        print(";\n");

    }

  
    /**
     * prints a method declaration
     */
    public void visitMethodDeclaration(JMethodDeclaration self,
                                       int modifiers,
                                       CType returnType,
                                       String ident,
                                       JFormalParameter[] parameters,
                                       CClassType[] exceptions,
                                       JBlock body) {
        newLine();
	// print(CModifier.toString(modifiers));
	print(returnType);
	print(" ");
	//just print initPath() instead of initPath<Type>
	if (ident.startsWith("initPath"))
	    print("initPath"); 
	else 
	    print(ident);
	print("(");
	int count = 0;
	
	for (int i = 0; i < parameters.length; i++) {
	    if (count != 0) {
		print(", ");
	    }
	    parameters[i].accept(this);
	    count++;
	}
	print(")");
	
	//print the declaration then return
	if (declOnly) {
	    print(";");
	    return;
	}

	//set the current method we are visiting
	method = self;

	//set is init for dynamically allocating arrays...
	if (filter != null &&
	    self.getName().startsWith("init"))
	    isInit = true;
	

        print(" ");
        if (body != null) 
	    body.accept(this);
        else 
            print(";");

        newLine();
	isInit = false;
	method = null;
    }

    // ----------------------------------------------------------------------
    // STATEMENT
    // ----------------------------------------------------------------------   

    /**
     * prints a for statement
     */
    public void visitForStatement(JForStatement self,
                                  JStatement forInit,
                                  JExpression forCond,
                                  JStatement forIncr,
                                  JStatement body) {
	JStatement init;
	JExpression cond;
	JStatement incr;

	//check if this is a do loop
	if (GENERATE_DO_LOOPS && doloops.containsKey(self)) {
	    DoLoopInformation doInfo = (DoLoopInformation)doloops.get(self);
	    //System.out.println("Induction Var: " + doInfo.induction);
	    //System.out.println("init exp: " + doInfo.init);		       
	    //System.out.println("cond exp: " + doInfo.cond);
	    //System.out.println("incr exp: " + doInfo.incr);

	    print("doloop (");
	    print(doInfo.induction.getType());
	    print(" ");
	    print(doInfo.induction.getIdent());
	    print(" = ");
	    init = doInfo.init;
	    cond = doInfo.cond;
	    incr = doInfo.incr;
	} else { //normal for loop
	    print("for (");
	    init = forInit;
	    cond = forCond;
	    incr = forIncr;
	}
	
	if (init != null) {
	    init.accept(this);
	    //the ; will print in a statement visitor
	}
	
	print(" ");
	if (cond != null) {
	    cond.accept(this);
	}
	    //cond is an expression so print the ;
	print("; ");
	if (incr != null) {
	    FlatIRToRS l2c = new FlatIRToRS(filter);
	    l2c.doloops = this.doloops;
	    incr.accept(l2c);
	    // get String
	    String str = l2c.getString();
	    // leave off the trailing semicolon if there is one
	    if (str.endsWith(";")) {
		print(str.substring(0, str.length()-1));
	    } else { 
		print(str);
	    }
	}
	
	print(") ");
	
        print("{");
	newLine();
        pos += TAB_SIZE;
        body.accept(this);
        pos -= TAB_SIZE;
        newLine();
	print("}");
    }

    /**
     * prints a method call expression
     */
    public void visitMethodCallExpression(JMethodCallExpression self,
                                          JExpression prefix,
                                          String ident,
                                          JExpression[] args) {

	assert (!ident.equals(Names.receiveMethod)) :
	    "Error: RStream code generation should not see network receive method";
	
        print(ident);
	
	//we want single precision versions of the math functions
	if (Utils.isMathMethod(prefix, ident)) 
	    print("f");
	    
	print("(");
	if (args != null) {
            for (int i = 0; i < args.length; i++) {
                if (i != 0) {
                    print(", ");
                }
		/* this is a hack but there is no other way to do it,
		   if we are currently visiting fscanf and we are at the 3rd
		   argument, prepend an & to get the address and pass the pointer 
		   to the fscanf
		*/
		if (ident == Names.fscanf && i == 2)
		    print("&");
                args[i].accept(this);
            }
        }
        print(")");
    }

    public JExpression passParentheses(JExpression exp) 
    {
	while (exp instanceof JParenthesedExpression)
	    exp = ((JParenthesedExpression)exp).getExpr();
	
	return exp;
    }
    

    public void visitPeekExpression(SIRPeekExpression self,
                                    CType tapeType,
                                    JExpression num)
    {
	assert false : "RStream code generation should not see a pop statement";
    }
    
    public void visitPopExpression(SIRPopExpression self,
                                   CType tapeType)
    {
	assert false : "RStream code generation should not see a pop statement";
    }
    
    public void visitPrintStatement(SIRPrintStatement self,
                                    JExpression exp)
    {
	CType type = null;

	
	try {
	    type = exp.getType();
	}
	catch (Exception e) {
	    System.err.println("Cannot get type for print statement");
	    type = CStdType.Integer;
	}
	
	if (type.equals(CStdType.Boolean))
	    {
		Utils.fail("Cannot print a boolean");
	    }
	else if (type.equals(CStdType.Byte) ||
		 type.equals(CStdType.Integer) ||
		 type.equals(CStdType.Short))
	    {
		print("printf(\"%d\\n\", "); 
		exp.accept(this);
		print(");");
	    }
	else if (type.equals(CStdType.Char))
	    {
		print("printf(\"%d\\n\", "); 
		exp.accept(this);
		print(");");
	    }
	else if (type.equals(CStdType.Float))
	    {
		print("printf(\"%f\\n\", ");
		exp.accept(this);
		print(");");
	    }
        else if (type.equals(CStdType.Long))
	    {
		print("printf(\"%d\\n\", "); 
		exp.accept(this);
		print(");");
	    }
	else if (type.equals(CStdType.String)) 
	    {
		print("printf(\"%s\\n\", "); 
		exp.accept(this);
		print(");");
	    }
	else
	    {
		assert false : "Unprintable type";
	    }
    }

    
    public void visitPushExpression(SIRPushExpression self,
                                    CType tapeType,
                                    JExpression val)
    {
	assert false : "RStream Front-end should not see a push statement";
    }    

      // ----------------------------------------------------------------------
    // UNUSED STREAM VISITORS
    // ----------------------------------------------------------------------

    /* pre-visit a pipeline */
    public void preVisitPipeline(SIRPipeline self,
				 SIRPipelineIter iter) 
    {
    }
    

    /* pre-visit a splitjoin */
    public void preVisitSplitJoin(SIRSplitJoin self,
				  SIRSplitJoinIter iter)
    {
    }
    

    /* pre-visit a feedbackloop */
    public void preVisitFeedbackLoop(SIRFeedbackLoop self,
				     SIRFeedbackLoopIter iter)
    {
    }
    

    /**
     * POST-VISITS 
     */
	    
    /* post-visit a pipeline */
    public void postVisitPipeline(SIRPipeline self,
				  SIRPipelineIter iter) {
    }
    

    /* post-visit a splitjoin */
    public void postVisitSplitJoin(SIRSplitJoin self,
				   SIRSplitJoinIter iter) {
    }
    

    /* post-visit a feedbackloop */
    public void postVisitFeedbackLoop(SIRFeedbackLoop self,
				      SIRFeedbackLoopIter iter) {
    } 

     public void visitPhasedFilter(SIRPhasedFilter self,
                                  SIRPhasedFilterIter iter) {
        // This is a stub; it'll get filled in once we figure out how phased
        // filters should actually work.
    }
}
