package at.dms.kjc.spacetime;

import at.dms.kjc.common.*;
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
import at.dms.compiler.*;

/**
 * This class returns the c code (a string) for a given raw tile
 */
public class TraceIRtoC extends ToC
{
    //the raw tile we are generating code for
    private RawTile tile;
    
    public TraceIRtoC(RawTile tile) 
    {
	this.tile = tile;
	this.str = new StringWriter();
	this.p = new TabbedPrintWriter(str);
    }

    public TraceIRtoC() 
    {
	this.tile = null;
	this.str = new StringWriter();
	this.p = new TabbedPrintWriter(str);
    }
    
    
    /** 
     * The entry point to create C code for a RawTile
     **/
    public void createCCode() 
    {
	//optimize the SIR code, if not disabled
	optimizations();
	
	//write header 
	generateHeader();

	//generate the fields
	for (int i = 0; i < tile.getComputeCode().getFields().length; i++)
	    tile.getComputeCode().getFields()[i].accept(this);
	
	print("\n");
	
	//print the pointers to the off chip buffers
	if (!KjcOptions.magicdram)
	    print(CommunicateAddrs.getFields(tile));

	//visit methods of tile, print the declaration first
	declOnly = true;
	for (int i = 0; i < tile.getComputeCode().getMethods().length; i++)
	    tile.getComputeCode().getMethods()[i].accept(this);

	//generate the methods
	declOnly = false;
	JMethodDeclaration mainMethod=tile.getComputeCode().getMainFunction();
	for (int i = 0; i < tile.getComputeCode().getMethods().length; i++) {
	    JMethodDeclaration method=tile.getComputeCode().getMethods()[i];
	    if(method!=mainMethod) //Manually inline main method so inline asm labels don't repeat
		method.accept(this);
	}
	
	//print the method that sends/recvs block addresses
	if (!KjcOptions.magicdram) {
	    print(CommunicateAddrs.getFunction(tile));
	    //print(CommunicateAddrs.getFreeFunction(tile));
	}
	
	//generate the entry function for the simulator
	print("void begin(void) {\n");
	//if we are using the magic network, 
	//use a magic instruction to initialize the magic fifos
	if (KjcOptions.magic_net)
	    print("  __asm__ volatile (\"magc $0, $0, 1\");\n");

	//call the raw_init() function for the static network
	if (!KjcOptions.decoupled && !KjcOptions.magic_net) {
	    print("  raw_init();\n");
	    //print("  raw_init2();\n");
	}

	if (!KjcOptions.magicdram) 
	    print("  " + CommunicateAddrs.functName + "();\n");
	
	//print(tile.getComputeCode().getMainFunction().getName() + "();\n");
	mainMethod.getBody().accept(this); //Inline Main method
	print("};\n");
    }
	
    private void optimizations() 
    {
	ArrayDestroyer arrayDest=new ArrayDestroyer();
	for (int i = 0; i < tile.getComputeCode().getMethods().length; i++) {
	    if (!KjcOptions.nofieldprop) {
		 Unroller unroller;
		 do {
		     do {
			 unroller = new Unroller(new Hashtable());
			 tile.getComputeCode().getMethods()[i].accept(unroller);
		     } while(unroller.hasUnrolled());
		     tile.getComputeCode().getMethods()[i].accept(new Propagator(new Hashtable()));
		     unroller = new Unroller(new Hashtable());
		     tile.getComputeCode().getMethods()[i].accept(unroller);
		 } while(unroller.hasUnrolled());
		 tile.getComputeCode().getMethods()[i].accept(new BlockFlattener());
		 tile.getComputeCode().getMethods()[i].accept(new Propagator(new Hashtable()));
	     } else
		 tile.getComputeCode().getMethods()[i].accept(new BlockFlattener());
	     
	    //tile.getComputeCode().getMethods()[i].accept(arrayDest);
	     tile.getComputeCode().getMethods()[i].accept(new VarDeclRaiser());
	}
	
    }
    
    private void generateHeader() 
    {
	print("#include <raw.h>\n");
	print("#include <stdlib.h>\n");
	print("#include <math.h>\n\n");

	//if we are number gathering and this is the sink, generate the dummy
	//vars for the assignment of the print expression.
	/*
	  if (KjcOptions.numbers > 0 && NumberGathering.successful &&
	  self == NumberGathering.sink.contents) {
	  print ("int dummyInt;\n");
	  print ("float dummyFloat;\n");
	  }
	*/
	
	if (KjcOptions.altcodegen && !KjcOptions.decoupled){
	    print("register float " + Util.CSTOFPVAR + " asm(\"$csto\");\n");
	    print("register float " + Util.CSTIFPVAR + " asm(\"$csti\");\n");
	    print("register int " + Util.CSTOINTVAR + " asm(\"$csto\");\n");
	    print("register int " + Util.CSTIINTVAR + " asm(\"$csti\");\n");
	    print("register int " + Util.CGNOINTVAR + " asm(\"$cgno\");\n");
	}
	
	if (KjcOptions.decoupled) {
	    print("float " + Util.CSTOFPVAR + ";\n");
	    print("float " + Util.CSTIFPVAR + ";\n");
	    print("int " + Util.CSTOINTVAR + ";\n");
	    print("int " + Util.CSTIINTVAR + ";\n");
	}
	
	//if there are structures in the code, include
	//the structure definition header files
	//	if (SpaceTimeBackend.structures.length > 0) 
	// print("#include \"structs.h\"\n");

	//print the extern for the function to init the 
	//switch, do not do this if we are compiling for
	//a uniprocessor
	if (!KjcOptions.standalone) {
	    print("void raw_init();\n");
	    print("void raw_init2();\n");
	}

	if (SpaceTimeBackend.FILTER_DEBUG_MODE) {
	    print("void static_send_print(");
	    print("int i) {\n");
	    print("\tprint_int(i);\n");
	    print("\tstatic_send(i);\n");
	    print("}\n\n");
	    print("void static_send_print_f(");
	    print("float f) {\n");
	    print("\tprint_float(f);\n");
	    print("\tstatic_send(f);\n");
	    print("}\n\n");
	}
    }


     /**
     * prints a for statement
     */
    public void visitForStatement(JForStatement self,
                                  JStatement init,
                                  JExpression cond,
                                  JStatement incr,
                                  JStatement body) {
        print("for (");
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
	    TraceIRtoC l2c = new TraceIRtoC(tile);
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
        pos += TAB_SIZE;
        body.accept(this);
        pos -= TAB_SIZE;
        newLine();
        print("}");
    }

    protected void stackAllocateArray(String ident){
	//find the dimensions of the array!!
	String dims[] = 
	    ArrayDim.findDim(new TraceIRtoC(), tile.getComputeCode().getFields(), method, ident);
	
	for (int i = 0; i < dims.length; i++)
	    print("[" + dims[i] + "]");
	return;
    }
    
     /**
     * prints a variable declaration statement
     */
    public void visitVariableDefinition(JVariableDefinition self,
                                        int modifiers,
                                        CType type,
                                        String ident,
                                        JExpression expr) {
	//we want to stack allocate all arrays
	//we convert an assignment statement into the stack allocation statement'
	//so, just remove the var definition, if the new array expression
	//is not included in this definition, just remove the definition,
	//when we visit the new array expression we will print the definition...
	if (type.isArrayType() && !isInit) {
	    String[] dims = ArrayDim.findDim(new TraceIRtoC(), tile.getComputeCode().getFields(), method, ident);
	    //but only do this if the array has corresponding 
	    //new expression, otherwise don't print anything.
	    if (expr instanceof JNewArrayExpression) {
		//print the type
		print(((CArrayType)type).getBaseType());
		print(" ");
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
		declareInitializedArray(type, ident, expr);
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
	}
	else if (type.isOrdinal())
	    print (" = 0");
	else if (type.isFloatingPoint())
	    print(" = 0.0f");

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
	
	//If this is the raw Main function then set is work to true
	//used for stack allocating arrays
	   
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

	method = self;
	
	//set is init for dynamically allocating arrays...
	if (self.getName().startsWith("init"))
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
     * Generates code to receive an array type into the buffer
     **/
    public void popArray(JExpression arg, CArrayType type) 
    {
	String dims[] = Util.makeString(type.getDims());
	
	//print the array indices
	for (int i = 0; i < dims.length; i++) {
	    print("for (" + RawExecutionCode.ARRAY_INDEX + i + " = 0; " +
		  RawExecutionCode.ARRAY_INDEX + i + " < " + dims[i] + " ; " +
		  RawExecutionCode.ARRAY_INDEX + i + "++)\n");
	}
	
	print("{");
	//print out the receive assembly
	print(Util.staticNetworkReceivePrefix());
	//print out the buffer variable and the index
	arg.accept(this);
	//now append the remaining dimensions
	for (int i = 0; i < dims.length; i++) {
		print("[" + RawExecutionCode.ARRAY_INDEX + i + "]");
	    }
	//finish up the receive assembly
	print(Util.staticNetworkReceiveSuffix(type.getBaseType()));
	print("}");
    }
    
    //for rate matching, we want to store the value of the item pushed  
    //into the output buffer and increment the sendbufferindex
    //args[0] is the item we want to push...
    private void rateMatchPush(JExpression[] args) 
    {
	print("(" + RawExecutionCode.sendBuffer);
	print("[++" + RawExecutionCode.sendBufferIndex + "] = ");
	args[0].accept(this);
	print(")");
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
	    
	    String[] dims = ArrayDim.findDim(new TraceIRtoC(), tile.getComputeCode().getFields(), method, ident);
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
	    print(baseType);
	    print(" ");
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
     * prints a method call expression
     */
    public void visitMethodCallExpression(JMethodCallExpression self,
                                          JExpression prefix,
                                          String ident,
                                          JExpression[] args) {
        /*
          if (ident != null && ident.equals(JAV_INIT)) {
          return; // we do not want generated methods in source code
          }
        */


	//supress the call to memset if the array is of size 0
	//if (ident.equals("memset")) {
	//    String[] dims = ArrayDim.findDim(filter, ((JLocalVariableExpression)args[0]).getIdent());
	//    if (dims[0].equals("0"))
	//	return;
	//}

	//generate the inline asm instruction to execute the 
	//receive if this is a receive instruction
	if (ident.equals(RawExecutionCode.receiveMethod)) {
	    print(Util.staticNetworkReceivePrefix());
	    visitArgs(args,0);
	    print(Util.staticNetworkReceiveSuffix(self.getType()));
	    return;  
	}
	
	//we are receiving an array type, call the popArray method
	if (ident.equals(RawExecutionCode.arrayReceiveMethod)) {
	    popArray(args[0], (CArrayType)self.getType());
	    return;
	}
	
	if (ident.equals(RawExecutionCode.rateMatchSendMethod)) {
	    rateMatchPush(args);
	    return;
	}
          
        print(ident);

	//we want single precision versions of the math functions raw
	if (Utils.isMathMethod(prefix, ident)) 
	    print("f");

        print("(");
	
	//if this method we are calling is the call to a structure 
	//receive method that takes a pointer, we have to add the 
	//address of operator
	if (ident.startsWith(RawExecutionCode.structReceiveMethodPrefix))
	    print("&");

        int i = 0;
        /* Ignore prefix, since it's just going to be a Java class name.
        if (prefix != null) {
            prefix.accept(this);
            i++;
        }
        */
        visitArgs(args, i);
        print(")");
    }

    public void visitPeekExpression(SIRPeekExpression self,
                                    CType tapeType,
                                    JExpression num)
    {
	Utils.fail("TraceIRToC should see no peek expressions");
    }
    
    public void visitPopExpression(SIRPopExpression self,
                                   CType tapeType)
    {
	Utils.fail("TraceIRToC should see no pop expressions");
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
		if (!KjcOptions.standalone) {
		    //print("print_int(");
		    print("raw_test_pass_reg(");
		}
		
		else
		    print("printf(\"%d\\n\", "); 
		//print("gdn_send(" + INT_HEADER_WORD + ");\n");
		//print("gdn_send(");
		exp.accept(this);
		print(");");
	    }
	else if (type.equals(CStdType.Char))
	    {
		if (!KjcOptions.standalone) {
		    print("raw_test_pass_reg(");
		    //print("print_int(");
		}
		else
		    print("printf(\"%d\\n\", "); 
		//print("gdn_send(" + INT_HEADER_WORD + ");\n");
		//print("gdn_send(");
		exp.accept(this);
		print(");");
	    }
	else if (type.equals(CStdType.Float))
	    {
		if (!KjcOptions.standalone) {
		    print("raw_test_pass_reg(");
		    //print("print_float(");
		}
		else 
		    print("printf(\"%f\\n\", "); 
		//print("gdn_send(" + FLOAT_HEADER_WORD + ");\n");
		//print("gdn_send(");
		exp.accept(this);
		print(");");
	    }
        else if (type.equals(CStdType.Long))
	    {
		if (!KjcOptions.standalone) {
		    print("raw_test_pass_reg(");
		    //print("print_int(");
		}
		
		else
		    print("printf(\"%d\\n\", "); 
		//		print("gdn_send(" + INT_HEADER_WORD + ");\n");
		//print("gdn_send(");
		exp.accept(this);
		print(");");
	    }
	else if (type.equals(CStdType.String)) 
	    {
		if (!KjcOptions.standalone)
		    print("print_string(");
		else
		    print("printf(\"%s\\n\", "); 
		//		print("gdn_send(" + INT_HEADER_WORD + ");\n");
		//print("gdn_send(");
		exp.accept(this);
		print(");");
	    }
	else
	    {
		System.out.println("Unprintatble type");
		print("print_int(");
		exp.accept(this);
		print(");");
		//Utils.fail("Unprintable Type");
	    }
    }
    
    private void pushScalar(SIRPushExpression self,
			    CType tapeType,
			    JExpression val) 
    {
	print(Util.staticNetworkSendPrefix(tapeType));
	val.accept(this);
	print(Util.staticNetworkSendSuffix());
    }

    
    public void pushClass(SIRPushExpression self, 
			  CType tapeType,
			  JExpression val) 
    {
	//turn the push statement into a call of
	//the structure's push method
	print("push" + tapeType + "(&");
	val.accept(this);
	print(")");
    }
    

    private void pushArray(SIRPushExpression self, 
			   CType tapeType,
			   JExpression val) 
    {
	CType baseType = ((CArrayType)tapeType).getBaseType();
	String dims[] = Util.makeString(((CArrayType)tapeType).getDims());
	
	for (int i = 0; i < dims.length; i++) {
	    print("for (" + RawExecutionCode.ARRAY_INDEX + i + " = 0; " +
		  RawExecutionCode.ARRAY_INDEX + i + " < " + dims[i] + " ; " +
		  RawExecutionCode.ARRAY_INDEX + i + "++)\n");
	}

	if(KjcOptions.altcodegen || KjcOptions.decoupled) {
	    print("{\n");
	    //	    print(Util.CSTOVAR + " = ");
	    val.accept(this);
	    for (int i = 0; i < dims.length; i++) {
		print("[" + RawExecutionCode.ARRAY_INDEX + i + "]");
	    }
	    print(";\n}\n");
	} else {
	    print("{");
	    print("static_send((" + baseType + ") ");
	    val.accept(this);
	    for (int i = 0; i < dims.length; i++) {
		print("[" + RawExecutionCode.ARRAY_INDEX + i + "]");
	    }
	    print(");\n}\n");
	}
    }
    
    public void visitPushExpression(SIRPushExpression self,
                                    CType tapeType,
                                    JExpression val)
    {
	if (tapeType.isArrayType())
	    pushArray(self, tapeType, val);
	else if (tapeType.isClassType())
	    pushClass(self, tapeType, val);
	else 
	    pushScalar(self, tapeType, val);
    }
    
    public void visitRegReceiverStatement(SIRRegReceiverStatement self,
                                          JExpression portal,
					  SIRStream receiver, 
					  JMethodDeclaration[] methods)
    {
        print("register_receiver(");
        portal.accept(this);
        print(", data->context, ");
        print(self.getItable().getVarDecl().getIdent());
        print(", LATENCY_BEST_EFFORT);");
        // (But shouldn't there be a latency field in here?)
    }
    
    public void visitRegSenderStatement(SIRRegSenderStatement self,
                                        String fn,
                                        SIRLatency latency)
    {
        print("register_sender(this->context, ");
        print(fn);
        print(", ");
        latency.accept(this);
        print(");");
    }

    /**
     * prints InlineAssembly code
     */
    public void visitInlineAssembly(InlineAssembly self,String[] asm,String[] input,String[] clobber) {
	print("asm volatile(\"");
	if(asm.length>0)
	    print(asm[0]);
	for(int i=1;i<asm.length;i++)
	    print("\\n\\t"+asm[i]);
	print("\"::");
	if(input.length>0)
	    print(input[0]);
	for(int i=1;i<input.length;i++)
	    print(","+input[i]);
	if(clobber.length>0) {
	    print(":\""+clobber[0]);
	    for(int i=1;i<clobber.length;i++)
		print("\",\""+clobber[i]);
	    print("\"");
	}
	print(");");
    }

}
