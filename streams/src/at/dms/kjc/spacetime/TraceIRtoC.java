package at.dms.kjc.spacetime;

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
public class TraceIRtoC extends SLIREmptyVisitor //implements StreamVisitor
{
    //the raw tile we are generating code for
    private RawTile tile;
    
    //if this is true only print methods decls, not body
    public boolean declOnly = true;
    
    //the writer and its associated crap
    private TabbedPrintWriter p;
    protected StringWriter str; 
    protected int pos;
    protected int TAB_SIZE = 2;
    protected int WIDTH = 80;

    public TraceIRtoC(RawTile tile) 
    {
	this.tile = tile;
	this.str = new StringWriter();
	this.p = new TabbedPrintWriter(str);
	
	//optimize the SIR code, if not disabled
	optimizations();
	
	//now start generating the code
	createCCode();
    }

    public String getString() {
        if (str != null)
            return str.toString();
        else
            return null;
    }
    
    private void createCCode() 
    {
	//write header 
	generateHeader();
	
	//generate the fields
	for (int i = 0; i < tile.getComputeCode().getFields().length; i++)
	    tile.getComputeCode().getFields()[i].accept(this);
	
	//visit methods of filter, print the declaration first
	declOnly = true;
	for (int i = 0; i < tile.getComputeCode().getMethods().length; i++)
	    tile.getComputeCode().getMethods()[i].accept(this);

	//generate the methods
	declOnly = false;
	for (int i = 0; i < tile.getComputeCode().getMethods().length; i++)
	    tile.getComputeCode().getMethods()[i].accept(this);
	
	//generate the entry function for the simulator
	print("void begin(void) {\n");
	//if we are using the magic network, 
	//use a magic instruction to initialize the magic fifos
	if (KjcOptions.magic_net)
	    print("  __asm__ volatile (\"magc $0, $0, 1\");\n");

	//call the raw_init() function for the static network
	if (!KjcOptions.decoupled && !KjcOptions.magic_net) {
	    print("  raw_init();\n");
	    print("  raw_init2();\n");
	}
	
	print(tile.getComputeCode().getMainFunction() + "();\n");
	print("};\n");
    }
	
    private void optimizations() 
    {
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
	     
	     tile.getComputeCode().getMethods()[i].accept(new ArrayDestroyer());
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
	}
	
	if (KjcOptions.decoupled) {
	    print("float " + Util.CSTOFPVAR + ";\n");
	    print("float " + Util.CSTIFPVAR + ";\n");
	    print("int " + Util.CSTOINTVAR + ";\n");
	    print("int " + Util.CSTIINTVAR + ";\n");
	}
	
	//if there are structures in the code, include
	//the structure definition header files
	if (SpaceTimeBackend.structures.length > 0) 
	   print("#include \"structs.h\"\n");

	//print the extern for the function to init the 
	//switch, do not do this if we are compiling for
	//a uniprocessor
	if (!KjcOptions.raw_uni) {
	    print("void raw_init();\n");
	    print("void raw_init2();\n");
	}
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
}
