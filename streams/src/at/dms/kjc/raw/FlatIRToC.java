package at.dms.kjc.raw;

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

/**
 * This class dumps the tile code for each filter into a file based 
 * on the tile number assigned 
 */
public class FlatIRToC extends SLIREmptyVisitor implements StreamVisitor
{
    protected boolean			forInit;	// is on a for init
    protected int				TAB_SIZE = 2;
    protected int				WIDTH = 80;
    protected int				pos;
    protected String                      className;

    protected TabbedPrintWriter		p;
    protected StringWriter                str; 
    protected boolean			nl = true;
    public boolean                   declOnly = true;
    public SIRFilter               filter;
    //true if we are using the second buffer management scheme 
    //circular buffers with anding
    public boolean circular;
    public boolean debug = false;//true;
    public boolean isWork = false;
    
    //fields for all of the vars names we introduce in the c code
    private final String BUFFER_INDEX = "__i__";
    private final String TAPE_INDEX = "__count__";
    private final String BUFFER_SIZE = "__BUFFERSIZE__";
    private final String BITS = "__BITS__";
    private final String BUFFER = "__BUFFER__";
    private static String ARRAY_INDEX = "__ARRAY_INDEX__";
    private static String ARRAY_COPY = "__ARRAY_COPY__";

    private final String FLOAT_HEADER_WORD = "__FLOAT_HEADER_WORD__";
    private final String INT_HEADER_WORD = "__INT_HEADER_WORD__";


    //fields for the var names we introduce with the rate match code
    private static String recvBuffer = "__RECVBUFFER__";
    private static String sendBuffer = "__SENDBUFFER__";
    private static String sendBufferSize = "__SENDBUFFERSIZE__";
    private static String recvBufferSize = "__RECVBUFFERSIZE__";
    private static String sendBufferIndex = "__SENDBUFFERINDEX__";
    private static String recvBufferIndex = "__RECVBUFFERINDEX__";
    private static String exeIndex = "__EXEINDEX__";
    private static String sendIndex = "__SENDINDEX__";
    private static String recvIndex = "_RECVINDEX__";    
    private static String RECVBITS = "__RECVBITS__";  
    private static String SENDBITS = "__SENDBITS__";

    //true if we are in rate match mode and if this 
    //filter is not in a feedback loop
    private boolean ratematch = false;

    private static int filterID = 0;
    
    //Needed to pass info from assignment to visitNewArray
    JExpression lastLeft;

    public static void generateCode(FlatNode node) 
    {
	FlatIRToC toC = new FlatIRToC((SIRFilter)node.contents);

	//Optimizations
	for (int i = 0; i < ((SIRFilter)node.contents).getMethods().length; i++) {
	    if (StreamItOptions.constprop) {
		System.out.println("Optimizing "+((SIRFilter)node.contents).getMethods()[i].getName()+"..");
		Unroller unroller;
		do {
		    do {
			System.out.println("Unrolling..");
			unroller = new Unroller(new Hashtable());
			((SIRFilter)node.contents).getMethods()[i].accept(unroller);
		    } while(unroller.hasUnrolled());
		    System.out.println("Constant Propagating..");
		    ((SIRFilter)node.contents).getMethods()[i].accept(new Propagator(new Hashtable()));
		    System.out.println("Unrolling..");
		    unroller = new Unroller(new Hashtable());
		    ((SIRFilter)node.contents).getMethods()[i].accept(unroller);
		} while(unroller.hasUnrolled());
		System.out.println("Flattening..");
		((SIRFilter)node.contents).getMethods()[i].accept(new BlockFlattener());
		System.out.println("Analyzing Branches..");
		//((SIRFilter)node.contents).getMethods()[i].accept(new BranchAnalyzer());
		System.out.println("Constant Propagating..");
		((SIRFilter)node.contents).getMethods()[i].accept(new Propagator(new Hashtable()));
	    } else
		((SIRFilter)node.contents).getMethods()[i].accept(new BlockFlattener());
	    ((SIRFilter)node.contents).getMethods()[i].accept(new ArrayDestroyer());
	    ((SIRFilter)node.contents).getMethods()[i].accept(new VarDeclRaiser());
	}

        IterFactory.createIter((SIRFilter)node.contents).accept(toC);
    }
    
    public FlatIRToC() 
    {
	this.str = new StringWriter();
        this.p = new TabbedPrintWriter(str);
    }
    

    public FlatIRToC(TabbedPrintWriter p) {
        this.p = p;
        this.str = null;
        this.pos = 0;
    }
    
    public FlatIRToC(SIRFilter f) {
	this.filter = f;
	circular = false;
	this.str = new StringWriter();
        this.p = new TabbedPrintWriter(str);
	ratematch = StreamItOptions.ratematch && !f.insideFeedbackLoop();
    }

    public String getString() {
        if (str != null)
            return str.toString();
        else
            return null;
    }
    
    /**
     * Close the stream at the end
     */
    public void close() {
        p.close();
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    /*  
    public void visitStructure(SIRStructure self,
                               SIRStream parent,
                               JFieldDeclaration[] fields)
    {
        print("struct " + self.getIdent() + " {\n");
        for (int i = 0; i < fields.length; i++)
            fields[i].accept(this);
        print("};\n");
    }
    */
    
    //prints the buffer declaration for the filter
    //if size == -1 use BUFFER_SIZE as the size
    //otherwise use size
    private void bufferDeclaration(CType type, String buffer, String size) 
    {
	if (type.isArrayType()) {
	    CType baseType = ((CArrayType)type).getBaseType();
	    String dims[] = Util.makeString(((CArrayType)type).getDims());

	    print(baseType + " " + buffer + "[");
	    
	    
	    print(size + "]");
	    
	    for (int i = 0; i < dims.length; i++)
		print("[" + dims[i] + "]");
	    print(";\n");
	}
	else if (type.isClassType()) {
	}
	else {
	    print(type + 
		  " " + buffer + "[");
	    print(size);
	    print("];\n");
	}
    }
    
    
    public void visitFilter(SIRFilter self,
			    SIRFilterIter iter) {

	circular = !(self.getPeekInt() > 4 * self.getPopInt()) && !ratematch;
	

	//	System.out.println(self.getName());

	//Entry point of the visitor
	print("#include <raw.h>\n");
	print("#include <stdlib.h>\n");
	print("#include <math.h>\n\n");
		
	//print the inline asm 
        print("static inline void static_send_from_mem(void *val) instr_one_input(\"lw $csto,0(%0)\");\n");
        print("static inline void static_receive_to_mem(void *val) instr_one_input(\"sw $csti,0(%0)\");\n");

	//print the extern for the function to init the 
	//switch
	print("void raw_init();\n");
	    
	print("int " + TAPE_INDEX + " = -1;\n");
	print("int " + BUFFER_INDEX + ";\n");
	
	print("unsigned int " + FLOAT_HEADER_WORD + ";\n");
	print("unsigned int " + INT_HEADER_WORD + ";\n");

	if (ratematch) {
	    print("int " + recvBufferIndex + "= -1;\n");
	    print("int " + sendBufferIndex + "= -1;\n");
	}
	
	//print the declarations for the array indexs for pushing and popping
	//if this filter deals with arrays
	if (self.getInputType().isArrayType() || self.getOutputType().isArrayType()) {
	    int inputDim = 0, outputDim = 0, maxDim;
	    //find which array has the greatest dimensionality	   
	    if (self.getInputType().isArrayType())
		inputDim = ((CArrayType)self.getInputType()).getArrayBound();
	    if (self.getOutputType().isArrayType()) 
		outputDim = ((CArrayType)self.getOutputType()).getArrayBound();
	    maxDim = (inputDim > outputDim) ? inputDim : outputDim;
	    
	    for (int i = 0; i < maxDim; i++)
		print("int " + ARRAY_INDEX + i + ";\n");
	}
	
	if (ratematch) {
	    rateMatch(self);
	    return;
	}
	
	if (filter.getPeekInt() > 0) {
	    if (circular) {
		if (filter instanceof SIRTwoStageFilter) {
		    SIRTwoStageFilter two = (SIRTwoStageFilter)filter;
		    int buffersize = (two.getInitPeek() > two.getPeekInt()) ? two.getInitPeek() :
			two.getPeekInt();
		    buffersize = nextPow2(buffersize + 1);
		    print ("#define " + BUFFER_SIZE + " " + buffersize + "\n");
		    print ("#define " + BITS + " " + (buffersize - 1) + "\n");
		    bufferDeclaration(two.getInputType(), BUFFER, BUFFER_SIZE);
		}
		else{
		    int buffersize = nextPow2(filter.getPeekInt());
		    print ("#define " + BUFFER_SIZE + " " + buffersize + "\n");
		    print ("#define " + BITS + " " + (buffersize - 1) + "\n");
		    bufferDeclaration(filter.getInputType(), BUFFER, BUFFER_SIZE);
		}
	    }
	    else {
		if (filter instanceof SIRTwoStageFilter) {
		    SIRTwoStageFilter two = (SIRTwoStageFilter)filter;
		    int buffersize = (two.getInitPeek() > two.getPeekInt()) ? two.getInitPeek() :
			two.getPeekInt();
		    bufferDeclaration(two.getInputType(), BUFFER,
				      new Integer(buffersize).toString());
		}
		else {
		    bufferDeclaration(filter.getInputType(), BUFFER, 
				      new Integer(filter.getPeekInt()).toString());
		}
	    }
	}

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
	
	print("void begin(void) {\n");
	print("  raw_init();\n");
	print(FLOAT_HEADER_WORD + " = construct_dyn_hdr(3, 1, 0, 0, 0, 3, 0);\n");
	print(INT_HEADER_WORD + " = construct_dyn_hdr(3, 1, 1, 0, 0, 3, 0);\n");
	print("  " + self.getInit().getName() + "(");
	print(InitArgument.getInitArguments(self));
	print (");\n");
	if (self instanceof SIRTwoStageFilter) {
	    print("  " + ((SIRTwoStageFilter)self).getInitWork().getName() + "();\n");
	}
	print("  " + self.getWork().getName() + "();\n");
	print("}\n");
	
	createFile();
    }

    
    private void createFile() {
	System.out.println("Code for " + filter.getName() +
			   " written to tile" + Layout.getTileNumber(filter) +
			   ".c");
	try {
	    FileWriter fw = new FileWriter("tile" + Layout.getTileNumber(filter) + ".c");
	    fw.write(str.toString());
	    fw.close();
	}
	catch (Exception e) {
	    System.err.println("Unable to write tile code file for filter " +
			       filter.getName());
	}
    }
    
   			
    private void rateMatch(SIRFilter self) {

	int recvbufsize = CalcBufferSize.
	    getConsBufSize(Layout.getNode(Layout.getTile(self)));
	int sendbufsize = CalcBufferSize.
	    getProdBufSize(Layout.getNode(Layout.getTile(self)));
		
	//print the buffer declarations
	if (!self.getInputType().equals(CStdType.Void))
	    bufferDeclaration(self.getInputType(), recvBuffer, 
			      (new Integer(recvbufsize)).toString());

	if (!self.getOutputType().equals(CStdType.Void))
	bufferDeclaration(self.getOutputType(), sendBuffer, 
			  (new Integer(sendbufsize)).toString());
	
	print ("#define " + RECVBITS + " " + (recvbufsize - 1) + "\n");
	print ("#define " + SENDBITS + " " + (sendbufsize - 1) + "\n");
	
	print(" int " + exeIndex + ";\n");
	print(" int " + sendIndex + " = -1;\n");
	print(" int " + recvIndex + " = -1;\n");
	
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
	
	//get the buffer sizes


	print("void begin(void) {\n");
	print("  raw_init();\n");
	print(FLOAT_HEADER_WORD + " = construct_dyn_hdr(3, 1, 0, 0, 0, 3, 0);\n");
	print(INT_HEADER_WORD + " = construct_dyn_hdr(3, 1, 1, 0, 0, 3, 0);\n");
	print("  " + self.getInit().getName() + "(");
	print(InitArgument.getInitArguments(self));
	print (");\n");
	if (self instanceof SIRTwoStageFilter) {
	    print("  " + ((SIRTwoStageFilter)self).getInitWork().getName() + "();\n");
	}
	print("  " + self.getWork().getName() + "();\n");
	print("}\n");
	

	createFile();
    }
	


    /**
     * prints a field declaration
     */
    public void visitFieldDeclaration(JFieldDeclaration self,
                                      int modifiers,
                                      CType type,
                                      String ident,
                                      JExpression expr) {
        /*
          if (ident.indexOf("$") != -1) {
          return; // dont print generated elements
          }
        */

        newLine();
        // print(CModifier.toString(modifiers));
        print(type);
        print(" ");
        print(ident);
        if (expr != null) {
            print("\t= ");
            expr.accept(this);
        }
        print(";");
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
	    
	    // if (!parameters[i].isGenerated()) {
	    parameters[i].accept(this);
	    count++;
	    // }
	}
	print(")");
	
	if (declOnly)
	    {
		print(";");
		return;
	    }

        print(" ");
        if (body != null) {
	    //see if this is the work function
	    //if it is print the work header and trailer
	    if (filter != null) {
		isWork = filter.getWork() == self;
		boolean isInitWork = (filter instanceof SIRTwoStageFilter && 
				      ((SIRTwoStageFilter)filter).getInitWork() == self);
		if (isWork || isInitWork) {
		    int pop = isWork ? filter.getPopInt() : 
			((SIRTwoStageFilter)filter).getInitPop();
		    int peek = isWork ? filter.getPeekInt() : 
			((SIRTwoStageFilter)filter).getInitPeek();
		    int push = isWork ? filter.getPushInt() : 
			((SIRTwoStageFilter)filter).getInitPush();
		    if (ratematch) {
			if (isInitWork) {
			    printRateMatchInitWorkHeader(pop, peek);
			    body.accept(this);
			    printRateMatchInitWorkTrailer(push);
			}
			else {
			    printRateMatchWorkHeader(pop, peek, push, body);
			    body.accept(this);
			    printRateMatchWorkTrailer(push);
			}
		    }
		    else if (circular) {
			printCircularWorkHeader(isWork, pop, peek);
			body.accept(this);
			printCircularWorkTrailer(isWork, pop, peek);
		    } else {
			printWorkHeader(isWork, pop, peek);
			body.accept(this);
			printWorkTrailer(isWork, pop, peek);
		    }
		} else {
		    // not the work function
		    body.accept(this);
		}
	    } else {
		// no filter?
		body.accept(this);
	    }
        } else {
            print(";");
        }
        newLine();
	isWork = false;
    }

    private int nextPow2(int i) {
	String str = Integer.toBinaryString(i);
	if  (str.indexOf('1') == -1)
	    return 0;
	int bit = str.length() - str.indexOf('1');
	return (int)Math.pow(2, bit);
    }

    private void printRateMatchWorkHeader(int pop, int peek, int push, JBlock workBlock)  
    {
	print("{\n");

	Integer init = (Integer)RawBackend.initExecutionCounts.
	    get(Layout.getNode(Layout.getTile(filter)));
	//	Integer steady = (Integer)RawBackend.steadyExecutionCounts.
	//    get(Layout.getNode(Layout.getTile(filter)));
	
	int initCount = 0;
	int steadyCount = 
	    BlockExecutionCounts.getBlockCount(Layout.getNode(Layout.getTile(filter)));
	
	if (init != null) 
	    initCount = init.intValue();

	//if this is a two stage filter, count one of the initial executions 
	//as the initWork execution and decrement the number of times the 
	//work function is called in the init Schedule
	if (filter instanceof SIRTwoStageFilter) {
	    SIRTwoStageFilter two = (SIRTwoStageFilter)filter;
	    if (!(two.getInitPeek() == 0 && two.getInitPush() == 0 &&
		  two.getInitPop() == 0))
		initCount--;
	}
	

	//	if (steady != null)
	//    steadyCount = steady.intValue();
	
	//initialization
	//receive what the previous filter produces
	
	int prevInitCount = 0;
	int prevPush = 0;
	
	FlatNode node = Layout.getNode(Layout.getTile(filter));
	FlatNode previous = null;
	
	if (node.inputs > 0) {
	    previous = node.incoming[0];
	    prevInitCount = Util.getCountPrev(RawBackend.initExecutionCounts, 
					 previous, node);
	    if (prevInitCount > 0) {
		if (previous.contents instanceof SIRSplitter || 
		    previous.contents instanceof SIRJoiner) 
		    prevPush = 1;
		else
		    prevPush = ((SIRFilter)previous.contents).getPushInt();
	    }
	}
	
	int initialItemsToReceive = (prevInitCount * prevPush);
	
	//if the previous node is a two stage filter then count its initWork
	//in the initialItemsTo Receive
	if (previous != null && previous.contents instanceof SIRTwoStageFilter) {
	    initialItemsToReceive -= ((SIRTwoStageFilter)previous.contents).getPushInt();
	    initialItemsToReceive += ((SIRTwoStageFilter)previous.contents).getInitPush();
	}
	
	//subtract the number of items the initWork took from the previous 
	//stream for the initialization code.
	if (filter instanceof SIRTwoStageFilter) {
	    SIRTwoStageFilter two = (SIRTwoStageFilter)filter;
	    if (!(two.getInitPeek() == 0 && two.getInitPush() == 0 &&
		  two.getInitPop() == 0)) {
		initialItemsToReceive -= two.getInitPeek();
	    }
	}
	
	if (initialItemsToReceive > 0) {
	    print("for (" + exeIndex + " = 0; " + exeIndex + " < " + initialItemsToReceive +
		  "; " + exeIndex + "++)\n");
	    printReceive();
	}   
	
	//execute the work function
	if (initCount > 0) {
	    print("for (" + exeIndex + " = 0; " + exeIndex + " < " + initCount + "; " +
		  exeIndex + "++)\n");
	    workBlock.accept(this);
	    //send the data produced
	    print("for (" + exeIndex + " = 0; " + exeIndex + " < " + (push * initCount) +
		  "; " + exeIndex + "++)\n");
	    print("{\n");
	    printRateMatchSend();
	    //end the send
	    print(";\n");
	    //end the send loop
	    print("}\n");
	}
	
	//steady state
	
	print("while(1) {\n");
	
	if (pop > 0) {
	    //receive everything
	    print("for (" + exeIndex + " = 0; " + exeIndex + " < " + (pop * steadyCount) +
		  "; " + exeIndex + "++)\n");
	    printReceive();
	}
	
	//perform work for the prescibed iterations
	print("for (" + exeIndex + " = 0; " + exeIndex + " < " + steadyCount + "; " +
	      exeIndex + "++)\n");
	print("{");
   	
    }
    
    private void printRateMatchSend() 
    {
	Utils.fail("not used");
	CType tapeType = filter.getOutputType();
	
	if (tapeType.isArrayType()) {
	    CType baseType = ((CArrayType)tapeType).getBaseType();
	    String dims[] = Util.makeString(((CArrayType)tapeType).getDims());
	    
	    for (int i = 0; i < dims.length; i++) {
		print("for (" + ARRAY_INDEX + i + " = 0; " +
		      ARRAY_INDEX + i + " < " + dims[i] + " ; " +
		      ARRAY_INDEX + i + "++)\n");
	    }
	    print("{");
	    print("static_send_from_mem((void*)&(" + baseType + ") ");
	    print(sendBuffer + "[(++" + sendIndex + ") & " + SENDBITS + "]");
	    
	    for (int i = 0; i < dims.length; i++) {
		print("[" + ARRAY_INDEX + i + "]");
	    }
	    print(")");
	}
	else if(tapeType.isClassType()) {
	}
	else {
	    print("static_send((" + tapeType + ")");	    
	    print(sendBuffer + "[(++" + sendIndex + ") & " + SENDBITS + "])");
	}

    }
    
    
    private void printRateMatchWorkTrailer(int push) 
    {
	//	Integer steady = (Integer)RawBackend.steadyExecutionCounts.
	//    get(Layout.getNode(Layout.getTile(filter)));
	
	int steadyCount = 
	    BlockExecutionCounts.getBlockCount(Layout.getNode(Layout.getTile(filter)));
	
	//if (steady != null)
	//   steadyCount = steady.intValue();
	

	//close the execution loop
	print("}\n");

	/*	if (push > 0) {
	    //send everything
	    print("for (" + exeIndex + " = 0; " + exeIndex + " < " + (push * steadyCount) +
		  "; " + exeIndex + "++)\n");
	    print("{\n");
	    printRateMatchSend();
	    //end the send
	    print(";\n");
	    //end the send loop
	    print("}\n");
	    print(sendBufferIndex + " &= " + SENDBITS + ";\n");
	    print(sendIndex + " &= " + SENDBITS + ";\n");
	}
	*/
	print(recvBufferIndex + " &= " + RECVBITS + ";\n");
	print(recvIndex + " &= " + RECVBITS + ";\n");
       
	
	//end the while loop
	print("}\n");
	//end the method decl
	print("}\n");

    }

    private void printRateMatchInitWorkHeader(int pop, int peek) 
    {
	print("{\n");
	int initialItemsToReceive = peek;
	//receive init peek items
	if (initialItemsToReceive > 0) {
	    print("for (" + exeIndex + " = 0; " + exeIndex + " < " + initialItemsToReceive +
		  "; " + exeIndex + "++)\n");
	    printReceive();
	}   
    }
    
    private void printRateMatchInitWorkTrailer(int push) 
    {
	/*	if (push > 0) {
	    //send everything
	    print("for (" + exeIndex + " = 0; " + exeIndex + " < " + push +
		  "; " + exeIndex + "++)\n");
	    print("{\n");
	    printRateMatchSend();
	    //end the send
	    print(";\n");
	    //end the send loop
	    print("}\n");
	    print(sendBufferIndex + " &= " + SENDBITS + ";\n");
	    print(sendIndex + " &= " + SENDBITS + ";\n");
	    }*/
	print("}\n");
    }
    
    
    private void printCircularWorkHeader(boolean isSteadyState, int pop, int peek) 
    {
	printWorkHeader(isSteadyState, pop, peek);
    }
    
    private void printCircularWorkTrailer(boolean loop, int pop, int peek) 
    {
	
	if (peek > 0) {
	    print(TAPE_INDEX + " = " + TAPE_INDEX + " & " + BITS + ";\n");
	    print(" for (" + BUFFER_INDEX + " = " + TAPE_INDEX + " + 1 + " + (peek - pop) + 
		  "; " + BUFFER_INDEX + " < " + TAPE_INDEX + " + 1 + " + peek + 
		  "; " + BUFFER_INDEX + "++) \n");
	    printReceive();
	}
	if (loop) {
	    print(" }\n");
	}
	print("}\n");
    }
    
    private void printArrayReceive() 
    {
	CType tapeType = filter.getInputType();
	
	CType baseType = ((CArrayType)tapeType).getBaseType();
	String dims[] = Util.makeString(((CArrayType)tapeType).getDims());
	
	for (int i = 0; i < dims.length; i++) {
	    print("for (" + ARRAY_INDEX + i + " = 0; " +
		  ARRAY_INDEX + i + " < " + dims[i] + " ; " +
		  ARRAY_INDEX + i + "++)\n");
	}
	print("{\n");
	print("static_receive_to_mem((void*)&(");
	if (circular)
	    print("   " + BUFFER + "[" + BUFFER_INDEX + " & " + BITS + "]");
	else if (ratematch)
	    print("   " + recvBuffer + "[" + recvBufferIndex + " & " + RECVBITS + "]");
	else 
	    print("   " + BUFFER + "[" + BUFFER_INDEX + "]");
	
	for (int i = 0; i < dims.length; i++) 
	    print("[" + ARRAY_INDEX + i + "]");
	
	print("));\n");
	//print(" = ");
	//if (baseType.equals(CStdType.Float))
	//    print("static_receive_f();\n");
	//else 
	//print("static_receive();\n");
	print("}\n");
    }
    
    private void printClassReceive()
    {
    }
    
    private void printReceive() 
    {
	print("{\n");
	if (filter.getInputType().isArrayType())
	    printArrayReceive();
	else if (filter.getInputType().isClassType())
	    printClassReceive();
	else {
	    print("static_receive_to_mem((void*)&(");
	    if (circular)
		print("   " + BUFFER + "[" + BUFFER_INDEX + " & " + BITS + "]");
	    else if (ratematch)
		print("   " + recvBuffer + "[(++" + recvIndex + ") & " + RECVBITS + "]");
	    else 
		print("   " + BUFFER + "[" + BUFFER_INDEX + "]");
	    print("));\n");
	    //	    print(" = ");
	    //  if (filter.getInputType().equals(CStdType.Float))
	    //print("static_receive_f();\n");
	    // else 
	    //	print("static_receive();\n");
	    
	}
	print("}\n");
    }
    
    private void printWorkHeader(boolean isSteadyState, int pop, int peek) 
    {
	print("{\n");
	// keep in mind that a two-stage filter might already have
	// peeked some items from the network.  in this case, only
	// peek the extra that haven't been peeked yet.
	int bottomPeek;
	if (isSteadyState && filter instanceof SIRTwoStageFilter) {
	    bottomPeek = ((SIRTwoStageFilter)filter).getInitPeek();
	} else {
	    bottomPeek = 0;
	}
	if (bottomPeek < peek) {
	    //print("int i, " + TAPE_INDEX + " = -1;\n");
	    print("/* work header */\n");
	    //	print(filter.getInputType() + 
	    //       " buffer[" + filter.getPeekInt() + "];\n");
	    print(" for (" + BUFFER_INDEX + " = " + bottomPeek + "; " + BUFFER_INDEX + " < " + 
		  peek + "; " + BUFFER_INDEX + "++) \n");
	    printReceive();
	}
	if (isSteadyState) {
	    print(" while (1) {\n");
	}
	if (debug) print("   print_int("+   Layout.getTileNumber(filter) + ");\n");
    }
    
    private void printWorkTrailer(boolean loop, int pop, int peek) 
    {
	if (peek > 0) {
	    print("\n " + TAPE_INDEX + " = 0;\n");
	    if (peek != pop) {
		print("/* work trailer 0 */\n");
		print(" for (" + BUFFER_INDEX + " = " + pop + "; " + BUFFER_INDEX + " < " +
		      peek +
		      "; " + BUFFER_INDEX + "++)\n");
		print("   " + BUFFER + "[" + TAPE_INDEX + "++] = " + BUFFER + "[" + BUFFER_INDEX + "];\n");
	    }
	    
	    print("/* work trailer 1 */\n");
	    print(" for (" + BUFFER_INDEX + " = " + TAPE_INDEX + "; " + BUFFER_INDEX + " < " + 
		  peek + "; " + BUFFER_INDEX + "++) \n");
	    printReceive();
	    print(TAPE_INDEX + " = -1;\n");
	}
	if (loop) {
	    print(" }\n");
	}
	print ("}\n");
    }
    
    // ----------------------------------------------------------------------
    // STATEMENT
    // ----------------------------------------------------------------------

    /**
     * prints a while statement
     */
    public void visitWhileStatement(JWhileStatement self,
                                    JExpression cond,
                                    JStatement body) {
        print("while (");
        cond.accept(this);
        print(") ");

        body.accept(this);
    }

    /**
     * prints a variable declaration statement
     */
    public void visitVariableDeclarationStatement(JVariableDeclarationStatement self,
                                                  JVariableDefinition[] vars) {
        for (int i = 0; i < vars.length; i++) {
            vars[i].accept(this);
        }
    }

    private void printLocalArrayDecl(JNewArrayExpression expr) 
    {
	JExpression[] dims = expr.getDims();
	for (int i = 0 ; i < dims.length; i++) {
	    FlatIRToC toC = new FlatIRToC();
	    dims[i].accept(toC);
	    print("[" + toC.getString() + "]");
	}
    }
    

    /**
     * prints a variable declaration statement
     */
    public void visitVariableDefinition(JVariableDefinition self,
                                        int modifiers,
                                        CType type,
                                        String ident,
                                        JExpression expr) {
        // print(CModifier.toString(modifiers));
	//	System.out.println(ident);
	//System.out.println(expr);
	
	//if we are in a work function, we want to stack allocate all arrays
	//right now array var definition is separate from allocation
	//we convert an assignment statement into the stack allocation statement'
	//so, just remove the var definition
	if (isWork && type.isArrayType()) {
	     return;
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
        print(";");
    }


    /**
     * prints a switch statement
     */
    public void visitSwitchStatement(JSwitchStatement self,
                                     JExpression expr,
                                     JSwitchGroup[] body) {
        print("switch (");
        expr.accept(this);
        print(") {");
        for (int i = 0; i < body.length; i++) {
            body[i].accept(this);
        }
        newLine();
        print("}");
    }

    /**
     * prints a return statement
     */
    public void visitReturnStatement(JReturnStatement self,
                                     JExpression expr) {
        print("return");
        if (expr != null) {
            print(" ");
            expr.accept(this);
        }
        print(";");
    }

    /**
     * prints a labeled statement
     */
    public void visitLabeledStatement(JLabeledStatement self,
                                      String label,
                                      JStatement stmt) {
        print(label + ":");
        stmt.accept(this);
    }

    /**
     * prints a if statement
     */
    public void visitIfStatement(JIfStatement self,
                                 JExpression cond,
                                 JStatement thenClause,
                                 JStatement elseClause) {
        print("if (");
        cond.accept(this);
        print(") ");
        pos += thenClause instanceof JBlock ? 0 : TAB_SIZE;
        thenClause.accept(this);
        pos -= thenClause instanceof JBlock ? 0 : TAB_SIZE;
        if (elseClause != null) {
            if ((elseClause instanceof JBlock) || (elseClause instanceof JIfStatement)) {
                print(" ");
            } else {
                newLine();
            }
            print("else ");
            pos += elseClause instanceof JBlock || elseClause instanceof JIfStatement ? 0 : TAB_SIZE;
            elseClause.accept(this);
            pos -= elseClause instanceof JBlock || elseClause instanceof JIfStatement ? 0 : TAB_SIZE;
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
        forInit = true;
        if (init != null) {
            init.accept(this);
        } else {
            print(";");
        }
        forInit = false;

        print(" ");
        if (cond != null) {
            cond.accept(this);
        }
        print("; ");
	if (incr != null) {
	    FlatIRToC l2c = new FlatIRToC(filter);
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

    /**
     * prints a compound statement
     */
    public void visitCompoundStatement(JCompoundStatement self,
                                       JStatement[] body) {
        visitCompoundStatement(body);
    }

    /**
     * prints a compound statement
     */
    public void visitCompoundStatement(JStatement[] body) {
        for (int i = 0; i < body.length; i++) {
            if (body[i] instanceof JIfStatement &&
                i < body.length - 1 &&
                !(body[i + 1] instanceof JReturnStatement)) {
                newLine();
            }
            if (body[i] instanceof JReturnStatement && i > 0) {
                newLine();
            }

            newLine();
            body[i].accept(this);

            if (body[i] instanceof JVariableDeclarationStatement &&
                i < body.length - 1 &&
                !(body[i + 1] instanceof JVariableDeclarationStatement)) {
                newLine();
            }
        }
    }

    /**
     * prints an expression statement
     */
    public void visitExpressionStatement(JExpressionStatement self,
                                         JExpression expr) {
        expr.accept(this);
        if (!forInit) {
            print(";");
        }
    }

    /**
     * prints an expression list statement
     */
    public void visitExpressionListStatement(JExpressionListStatement self,
                                             JExpression[] expr) {
        for (int i = 0; i < expr.length; i++) {
            if (i != 0) {
                print(", ");
            }
            expr[i].accept(this);
        }
	print(";");
    }

    /**
     * prints a empty statement
     */
    public void visitEmptyStatement(JEmptyStatement self) {
        newLine();
        print(";");
    }

    /**
     * prints a do statement
     */
    public void visitDoStatement(JDoStatement self,
                                 JExpression cond,
                                 JStatement body) {
        newLine();
        print("do ");
        body.accept(this);
        print("");
        print("while (");
        cond.accept(this);
        print(");");
    }

    /**
     * prints a continue statement
     */
    public void visitContinueStatement(JContinueStatement self,
                                       String label) {
        newLine();
        print("continue");
        if (label != null) {
            print(" " + label);
        }
        print(";");
    }

    /**
     * prints a break statement
     */
    public void visitBreakStatement(JBreakStatement self,
                                    String label) {
        newLine();
        print("break");
        if (label != null) {
            print(" " + label);
        }
        print(";");
    }

    /**
     * prints an expression statement
     */
    public void visitBlockStatement(JBlock self,
                                    JavaStyleComment[] comments) {
        print("{");
        pos += TAB_SIZE;
        visitCompoundStatement(self.getStatementArray());
        if (comments != null) {
            visitComments(comments);
        }
        pos -= TAB_SIZE;
        newLine();
        print("}");
    }

    /**
     * prints a type declaration statement
     */
    public void visitTypeDeclarationStatement(JTypeDeclarationStatement self,
                                              JTypeDeclaration decl) {
        decl.accept(this);
    }

    // ----------------------------------------------------------------------
    // EXPRESSION
    // ----------------------------------------------------------------------

    /**
     * prints an unary plus expression
     */
    public void visitUnaryPlusExpression(JUnaryExpression self,
                                         JExpression expr)
    {
	print("(");
        print("+");
        expr.accept(this);
	print(")");
    }

    /**
     * prints an unary minus expression
     */
    public void visitUnaryMinusExpression(JUnaryExpression self,
                                          JExpression expr)
    {
	print("(");
        print("-");
        expr.accept(this);
	print(")");
    }

    /**
     * prints a bitwise complement expression
     */
    public void visitBitwiseComplementExpression(JUnaryExpression self,
						 JExpression expr)
    {
	print("(");
        print("~");
        expr.accept(this);
	print(")");
    }

    /**
     * prints a logical complement expression
     */
    public void visitLogicalComplementExpression(JUnaryExpression self,
						 JExpression expr)
    {
	print("(");
        print("!");
        expr.accept(this);
	print(")");
    }

    /**
     * prints a type name expression
     */
    public void visitTypeNameExpression(JTypeNameExpression self,
                                        CType type) {
	print("(");
        print(type);
	print(")");
    }

    /**
     * prints a this expression
     */
    public void visitThisExpression(JThisExpression self,
                                    JExpression prefix) {
	//Utils.fail("This Expression encountered");
    }

    /**
     * prints a super expression
     */
    public void visitSuperExpression(JSuperExpression self) {
        Utils.fail("Super Expression Encountered");
    }

    /**
     * prints a shift expression
     */
    public void visitShiftExpression(JShiftExpression self,
                                     int oper,
                                     JExpression left,
                                     JExpression right) {
	print("(");
        left.accept(this);
        if (oper == OPE_SL) {
            print(" << ");
        } else if (oper == OPE_SR) {
            print(" >> ");
        } else {
            print(" >>> ");
        }
        right.accept(this);
	print(")");
    }

    /**
     * prints a shift expressiona
     */
    public void visitRelationalExpression(JRelationalExpression self,
                                          int oper,
                                          JExpression left,
                                          JExpression right) {
	print("(");
        left.accept(this);
        switch (oper) {
        case OPE_LT:
            print(" < ");
            break;
        case OPE_LE:
            print(" <= ");
            break;
        case OPE_GT:
            print(" > ");
            break;
        case OPE_GE:
            print(" >= ");
            break;
        default:
            Utils.fail("Unknown relational expression");
	}
        right.accept(this);
	print(")");
    }

    /**
     * prints a prefix expression
     */
    public void visitPrefixExpression(JPrefixExpression self,
                                      int oper,
                                      JExpression expr) {
	print("(");
        if (oper == OPE_PREINC) {
            print("++");
        } else {
            print("--");
        }
        expr.accept(this);
	print(")");
    }

    /**
     * prints a postfix expression
     */
    public void visitPostfixExpression(JPostfixExpression self,
                                       int oper,
                                       JExpression expr) {
	print("(");
        expr.accept(this);
        if (oper == OPE_POSTINC) {
            print("++");
        } else {
            print("--");
        }
	print(")");
    }

    /**
     * prints a parenthesed expression
     */
    public void visitParenthesedExpression(JParenthesedExpression self,
                                           JExpression expr) {
        print("(");
        expr.accept(this);
        print(")");
    }



    /**
     * prints an array allocator expression
     */
    public void visitNewArrayExpression(JNewArrayExpression self,
                                        CType type,
                                        JExpression[] dims,
                                        JArrayInitializer init)
    {
        /*print("(" + type + "*) calloc(");
	  dims[0].accept(this);
	  print(" , sizeof(");
	  print(type);
	  print("))");
	  if (init != null) {
	  init.accept(this);
	  }*/
	print("calloc(");
        dims[0].accept(this);
        print(", sizeof(");
        print(type);
	if(dims.length>1)
	    print("*");
        print("))");
	if(dims.length>1) {
	    for(int off=0;off<(dims.length-1);off++) {
		//Right now only handles JIntLiteral dims
		//If cast expression then probably a failure to reduce
		int num=((JIntLiteral)dims[off]).intValue();
		for(int i=0;i<num;i++) {
		    print(",\n");
		    //If lastLeft null then didn't come right after an assignment
		    lastLeft.accept(this);
		    print("["+i+"]=calloc(");
		    dims[off+1].accept(this);
		    print(", sizeof(");
		    print(type);
		    if(off<(dims.length-2))
			print("*");
		    print("))");
		}
	    }
	}
        if (init != null) {
            init.accept(this);
        }
    }

    /**
     * prints a name expression
     */
    public void visitNameExpression(JNameExpression self,
                                    JExpression prefix,
                                    String ident) {
	Utils.fail("Name Expression");
	
	print("(");
        if (prefix != null) {
            prefix.accept(this);
            print("->");
        }
        print(ident);
	print(")");
    }

    /**
     * prints an array allocator expression
     */
    public void visitBinaryExpression(JBinaryExpression self,
                                      String oper,
                                      JExpression left,
                                      JExpression right) {
	print("(");
        left.accept(this);
        print(" ");
        print(oper);
        print(" ");
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

        print(ident);
        print("(");
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

    /**
     * prints a local variable expression
     */
    public void visitLocalVariableExpression(JLocalVariableExpression self,
                                             String ident) {
        print(ident);
    }

    /**
     * prints an equality expression
     */
    public void visitEqualityExpression(JEqualityExpression self,
                                        boolean equal,
                                        JExpression left,
                                        JExpression right) {
	print("(");
        left.accept(this);
        print(equal ? " == " : " != ");
        right.accept(this);
	print(")");
    }

    /**
     * prints a conditional expression
     */
    public void visitConditionalExpression(JConditionalExpression self,
                                           JExpression cond,
                                           JExpression left,
                                           JExpression right) {
	print("(");
        cond.accept(this);
        print(" ? ");
        left.accept(this);
        print(" : ");
        right.accept(this);
	print(")");
    }

    /**
     * prints a compound expression
     */
    public void visitCompoundAssignmentExpression(JCompoundAssignmentExpression self,
                                                  int oper,
                                                  JExpression left,
                                                  JExpression right) {
	print("(");
        left.accept(this);
        switch (oper) {
        case OPE_STAR:
            print(" *= ");
            break;
        case OPE_SLASH:
            print(" /= ");
            break;
        case OPE_PERCENT:
            print(" %= ");
            break;
        case OPE_PLUS:
            print(" += ");
            break;
        case OPE_MINUS:
            print(" -= ");
            break;
        case OPE_SL:
            print(" <<= ");
            break;
        case OPE_SR:
            print(" >>= ");
            break;
        case OPE_BSR:
            print(" >>>= ");
            break;
        case OPE_BAND:
            print(" &= ");
            break;
        case OPE_BXOR:
            print(" ^= ");
            break;
        case OPE_BOR:
            print(" |= ");
            break;
        }
        right.accept(this);
	print(")");
    }

    /**
     * prints a field expression
     */
    public void visitFieldExpression(JFieldAccessExpression self,
                                     JExpression left,
                                     String ident)
    {
        if (ident.equals(JAV_OUTER_THIS)) {// don't generate generated fields
            print(left.getType().getCClass().getOwner().getType() + "->this");
            return;
        }
        int		index = ident.indexOf("_$");
        if (index != -1) {
            print(ident.substring(0, index));      // local var
        } else {
	    print("(");
            left.accept(this);
            print(ident);
	    print(")");
        }
    }

    /**
     * prints a cast expression
     */
    public void visitUnaryPromoteExpression(JUnaryPromote self,
                                            JExpression expr,
                                            CType type)
    {
	print("(");
        print("(");
        print(type);
        print(")");
        print("(");
        expr.accept(this);
        print(")");
        print(")");
    }

    /**
     * prints a compound assignment expression
     */
    public void visitBitwiseExpression(JBitwiseExpression self,
                                       int oper,
                                       JExpression left,
                                       JExpression right) {
        print("(");
        left.accept(this);
        switch (oper) {
        case OPE_BAND:
            print(" & ");
            break;
        case OPE_BOR:
            print(" | ");
            break;
        case OPE_BXOR:
            print(" ^ ");
            break;
        default:
	    Utils.fail("Unknown relational expression");
        }
        right.accept(this);
        print(")");
    }

    /**
     * prints an assignment expression
     */
    public void visitAssignmentExpression(JAssignmentExpression self,
                                          JExpression left,
                                          JExpression right) {

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
		Utils.fail("Assigning an array to an unsupported expression");
	    
	    String[] dims = ArrayDim.findDim(filter, ident);
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
		print(ARRAY_COPY + i + ", ");
	    print(ARRAY_COPY + (dims.length - 1));
	    print(";\n");
	    for (int i = 0; i < dims.length; i++) {
		print("for (" + ARRAY_COPY + i + " = 0; " + ARRAY_COPY + i +  
		      " < " + dims[i] + "; " + ARRAY_COPY + i + "++)\n");
	    }
	    left.accept(this);
	    for (int i = 0; i < dims.length; i++)
		print("[" + ARRAY_COPY + i + "]");
	    print(" = ");
	    right.accept(this);
	    for (int i = 0; i < dims.length; i++)
		print("[" + ARRAY_COPY + i + "]");
	    print(";\n}\n");
	    return;
	}

	//stack allocate all arrays in the work function 
	//done at the variable definition
	if (isWork && right instanceof JNewArrayExpression &&
 	    (left instanceof JLocalVariableExpression)) {
	    
	    CType baseType = ((CArrayType)((JNewArrayExpression)right).getType()).getBaseType();
 	    print(baseType + " ");
	    
	    left.accept(this);
	    //find the dimensions of the array!!
 	    String ident;
	    ident = ((JLocalVariableExpression)left).getVariable().getIdent();

 	    String dims[] = 
 		ArrayDim.findDim(filter, ident);
	    
 	    for (int i = 0; i < dims.length; i++)
 		print("[" + dims[i] + "]");
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
     * prints an array length expression
     */
    public void visitArrayLengthExpression(JArrayLengthExpression self,
                                           JExpression prefix) {
	Utils.fail("Array length expression not supported in streamit");
	
        prefix.accept(this);
        print(".length");
    }

    /**
     * prints an array length expression
     */
    public void visitArrayAccessExpression(JArrayAccessExpression self,
                                           JExpression prefix,
                                           JExpression accessor) {
        print("(");
        prefix.accept(this);
        print("[(int)");
        accessor.accept(this);
        print("]");
        print(")");
    }


    // ----------------------------------------------------------------------
    // STREAMIT IR HANDLERS
    // ----------------------------------------------------------------------

    public void visitCreatePortalExpression(SIRCreatePortal self) {
        print("create_portal()");
    }

    public void visitInitStatement(SIRInitStatement self,
                                   SIRStream stream)
    {
        print("/* InitStatement */");
    }

    public void visitInterfaceTable(SIRInterfaceTable self)
    {
        String iname = self.getIface().getIdent();
        JMethodDeclaration[] methods = self.getMethods();
        boolean first = true;
        
        print("{ ");
        for (int i = 0; i < methods.length; i++)
        {
            if (!first) print(", ");
            first = false;
            print(iname + "_" + methods[i].getName());
        }
        print("}");
    }
    
    public void visitLatency(SIRLatency self)
    {
        print("LATENCY_BEST_EFFORT");
    }
    
    public void visitLatencyMax(SIRLatencyMax self)
    {
        print("LATENCY_BEST_EFFORT");
    }
    
    public void visitLatencyRange(SIRLatencyRange self)
    {
        print("LATENCY_BEST_EFFORT");
    }
    
    public void visitLatencySet(SIRLatencySet self)
    {
        print("LATENCY_BEST_EFFORT");
    }

    public void visitMessageStatement(SIRMessageStatement self,
                                      JExpression portal,
                                      String iname,
                                      String ident,
                                      JExpression[] params,
                                      SIRLatency latency)
    {
	print("send_" + iname + "_" + ident + "(");
        portal.accept(this);
        print(", ");
        latency.accept(this);
        if (params != null)
            for (int i = 0; i < params.length; i++)
                if (params[i] != null)
                {
                    print(", ");
                    params[i].accept(this);
                }
        print(");");
    }

    public void visitPeekExpression(SIRPeekExpression self,
                                    CType tapeType,
                                    JExpression num)
    {
        if (ratematch) {
	    print("(" + recvBuffer + "[" + recvBufferIndex + " + (");
	    num.accept(this);
	    print(") + 1 & " + RECVBITS + "])");
	}
	else {
	    print("(" + BUFFER + "[" + TAPE_INDEX + " + (");
	    num.accept(this);
	    print(") + 1");
	    if (circular)
		print(" & " + BITS + "");
	    print("])");
	}
    }
    
    public void visitPopExpression(SIRPopExpression self,
                                   CType tapeType)
    {
	if (ratematch) {
	    print("(" + recvBuffer + "[++" + recvBufferIndex +
		  " & " + RECVBITS + "])");
	}
	else {
	    print("(" + BUFFER + "[++" + TAPE_INDEX);
	    if (circular)
		print(" & " + BITS);
	    print("])");
	}
	
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
		//print("print_int(");
		print("gdn_send(" + INT_HEADER_WORD + ");\n");
		print("gdn_send(");
		exp.accept(this);
		print(");");
	    }
	else if (type.equals(CStdType.Char))
	    {
		//print("print_int(");
		print("gdn_send(" + INT_HEADER_WORD + ");\n");
		print("gdn_send(");
		exp.accept(this);
		print(");");
	    }
	else if (type.equals(CStdType.Float))
	    {
		//print("print_float(");
		print("gdn_send(" + FLOAT_HEADER_WORD + ");\n");
		print("gdn_send(");
		exp.accept(this);
		print(");");
	    }
        else if (type.equals(CStdType.Long))
	    {
		//print("print_int(");
		print("gdn_send(" + INT_HEADER_WORD + ");\n");
		print("gdn_send(");
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
	print("(static_send(");
	//temporary fix for type changing filters
	print("(" + tapeType + ")");
	
	val.accept(this);
        print("))");
    }

    private void pushArray(SIRPushExpression self, 
			   CType tapeType,
			   JExpression val) 
    {
	CType baseType = ((CArrayType)tapeType).getBaseType();
	String dims[] = Util.makeString(((CArrayType)tapeType).getDims());
	
	for (int i = 0; i < dims.length; i++) {
	    print("for (" + ARRAY_INDEX + i + " = 0; " +
		  ARRAY_INDEX + i + " < " + dims[i] + " ; " +
		  ARRAY_INDEX + i + "++)\n");
	}
	print("{");
	print("static_send((" + baseType + ") ");
	val.accept(this);
	for (int i = 0; i < dims.length; i++) {
	    print("[" + ARRAY_INDEX + i + "]");
	}
	print(");\n}\n");
    }

    public void pushClass(SIRPushExpression self, 
			   CType tapeType,
			   JExpression val) 
    {
    }
    
    
    public void visitPushExpression(SIRPushExpression self,
                                    CType tapeType,
                                    JExpression val)
    {
	//based on the type of the tape, call the approp function
	/*	if (ratematch) {
	    if (tapeType.isClassType()) {
	    }
	    else if (tapeType.isClassType()) {
		CType baseType = ((CArrayType)tapeType).getBaseType();
		String dims[] = Util.makeString(((CArrayType)tapeType).getDims());
		
		for (int i = 0; i < dims.length; i++) {
		    print("for (" + ARRAY_INDEX + i + " = 0; " +
			  ARRAY_INDEX + i + " < " + dims[i] + " ; " +
			  ARRAY_INDEX + i + "++)\n");
		}
		print("{");
		print(sendBuffer + "[(++" + sendBufferIndex + ") & " + SENDBITS + "]");
		for (int i = 0; i < dims.length; i++) {
		    print("[" + ARRAY_INDEX + i + "]");
		}
		print(" = (" + baseType + ") ");
		val.accept(this);
		print(");\n}\n");
	    }
	    else {
		print(sendBuffer + "[(++" + sendBufferIndex + ") & " + SENDBITS + "] = ");
		print("(" + tapeType + ")");
		val.accept(this);
		print(";\n");
	    }
	}
	else {*/
	    if (tapeType.isArrayType())
		pushArray(self, tapeType, val);
	    else if (tapeType.isClassType())
		pushClass(self, tapeType, val);
	    else 
		pushScalar(self, tapeType, val);
	    //	}
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


    // ----------------------------------------------------------------------
    // UTILS
    // ----------------------------------------------------------------------

    /**
     * prints an array length expression
     */
    public void visitSwitchLabel(JSwitchLabel self,
                                 JExpression expr) {
        newLine();
        if (expr != null) {
            print("case ");
            expr.accept(this);
            print(": ");
        } else {
            print("default: ");
        }
    }

    /**
     * prints an array length expression
     */
    public void visitSwitchGroup(JSwitchGroup self,
                                 JSwitchLabel[] labels,
                                 JStatement[] stmts) {
        for (int i = 0; i < labels.length; i++) {
            labels[i].accept(this);
        }
        pos += TAB_SIZE;
        for (int i = 0; i < stmts.length; i++) {
            newLine();
            stmts[i].accept(this);
        }
        pos -= TAB_SIZE;
    }

    /**
     * prints a boolean literal
     */
    public void visitBooleanLiteral(boolean value) {
        if (value)
            print(1);
        else
            print(0);
    }

    /**
     * prints a byte literal
     */
    public void visitByteLiteral(byte value) {
        print("((byte)" + value + ")");
    }

    /**
     * prints a character literal
     */
    public void visitCharLiteral(char value) {
        switch (value) {
        case '\b':
            print("'\\b'");
            break;
        case '\r':
            print("'\\r'");
            break;
        case '\t':
            print("'\\t'");
            break;
        case '\n':
            print("'\\n'");
            break;
        case '\f':
            print("'\\f'");
            break;
        case '\\':
            print("'\\\\'");
            break;
        case '\'':
            print("'\\''");
            break;
        case '\"':
            print("'\\\"'");
            break;
        default:
            print("'" + value + "'");
        }
    }

    /**
     * prints a double literal
     */
    public void visitDoubleLiteral(double value) {
        print("((float)" + value + ")");
    }

    /**
     * prints a float literal
     */
    public void visitFloatLiteral(float value) {
        print("((float)" + value + ")");
    }

    /**
     * prints a int literal
     */
    public void visitIntLiteral(int value) {
        print(value);
    }

    /**
     * prints a long literal
     */
    public void visitLongLiteral(long value) {
        print("(" + value + "L)");
    }

    /**
     * prints a short literal
     */
    public void visitShortLiteral(short value) {
        print("((short)" + value + ")");
    }

    /**
     * prints a string literal
     */
    public void visitStringLiteral(String value) {
        print('"' + value + '"');
    }

    /**
     * prints a null literal
     */
    public void visitNullLiteral() {
        print("null");
    }

    /**
     * prints an array length expression
     */
    public void visitFormalParameters(JFormalParameter self,
                                      boolean isFinal,
                                      CType type,
                                      String ident) {
        print(type);
        if (ident.indexOf("$") == -1) {
            print(" ");
            print(ident);
        }
    }

    /**
     * prints an array length expression
     */
    public void visitArgs(JExpression[] args, int base) {
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                if (i + base != 0) {
                    print(", ");
                }
                args[i].accept(this);
            }
        }
    }

    /**
     * prints an array length expression
     */
    public void visitConstructorCall(JConstructorCall self,
                                     boolean functorIsThis,
                                     JExpression[] params)
    {
        newLine();
        print(functorIsThis ? "this" : "super");
        print("(");
        visitArgs(params, 0);
        print(");");
    }

    /**
     * prints an array initializer expression
     */
    public void visitArrayInitializer(JArrayInitializer self,
                                      JExpression[] elems)
    {
        newLine();
        print("{");
        for (int i = 0; i < elems.length; i++) {
            if (i != 0) {
                print(", ");
            }
            elems[i].accept(this);
        }
        print("}");
    }

    
    // ----------------------------------------------------------------------
    // PROTECTED METHODS
    // ----------------------------------------------------------------------

    protected void newLine() {
        p.println();
    }

    // Special case for CTypes, to map some Java types to C types.
    protected void print(CType s) {
	if (s instanceof CArrayType){
            print(((CArrayType)s).getElementType());
            print("*");
        }
        else if (s.getTypeID() == TID_BOOLEAN)
            print("int");
        else if (s.toString().endsWith("Portal"))
	    // ignore the specific type of portal in the C library
	    print("portal");
	else
            print(s.toString());
    }

    protected void printLocalType(CType s) 
    {
	if (s instanceof CArrayType){
	    print(((CArrayType)s).getElementType());
	}
        else if (s.getTypeID() == TID_BOOLEAN)
            print("int");
        else if (s.toString().endsWith("Portal"))
	    // ignore the specific type of portal in the C library
	    print("portal");
	else
            print(s.toString());
    }

    protected void print(Object s) {
        print(s.toString());
    }

    protected void print(String s) {
        p.setPos(pos);
        p.print(s);
    }

    protected void print(boolean s) {
        print("" + s);
    }

    protected void print(int s) {
        print("" + s);
    }

    protected void print(char s) {
        print("" + s);
    }

    protected void print(double s) {
        print("" + s);
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

}
