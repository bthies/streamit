package at.dms.kjc.raw;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.util.IRPrinter;
import at.dms.util.SIRPrinter;
import at.dms.kjc.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.partition.*;
import at.dms.kjc.sir.lowering.fusion.*;
import at.dms.kjc.sir.lowering.fission.*;
import at.dms.kjc.lir.*;
import java.util.*;
import at.dms.util.Utils;
import java.io.*;
import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.FlatVisitor;

public class IMEMEstimation extends EmptyStreamVisitor
{
    public static boolean TESTING_IMEM = false;
    /**
     * Whether or not everything this visitor has seen so far fits in
     * IMEM.
     */
    private boolean everythingFits = true;
    
    public void visitFilter(SIRFilter self,
			    SIRFilterIter iter) {
	if (!fits(self)) {
	    System.out.println("Filter " + self.getName() + " does not fit in IMEM.");
	    everythingFits = false;
	} else {
	    System.out.println("Filter " + self.getName() + " fits in IMEM.");
	}
       
    }
    
    public IMEMEstimation() 
    {
	
    }
    
    /**
     * Returns true iff all filters in <str> fit in IMEM.  Each filter
     * is measured independently (assuming 1 filter per tile).
     */
    public static boolean testMe(SIRStream str) 
    {
	IMEMEstimation visitor = new IMEMEstimation();
	IterFactory.createFactory().createIter(str).accept(visitor);
	return visitor.everythingFits;
    }
    

    //returns true if it fits in IMEM
    public static boolean fits(SIRFilter oldFilter) 
    {
	// if we have an identity filter, just return 0 since these
	// aren't mapped onto Raw
	if (oldFilter instanceof SIRIdentity || 
	    oldFilter instanceof SIRFileWriter ||
	    oldFilter instanceof SIRFileReader) {
	    return true;
	}

	TESTING_IMEM = true;
	boolean fits = true;

	boolean oldMagicNetValue = KjcOptions.magic_net;
	boolean oldRateMatchValue = KjcOptions.ratematch;
	int oldOutputsValue = KjcOptions.outputs;

	//clone the Filter and create a dummy pipeline with just this
	//new cloned filter
	SIRFilter filter = (SIRFilter)ObjectDeepCloner.deepCopy(oldFilter);
	SIRPipeline pipe = new SIRPipeline("top");
	LinkedList list = new LinkedList();
	list.add(filter);
	pipe.setChildren(list);

	//make a new directory and change the current working dir
	String dir = File.separator + "tmp" + File.separator + 
	    filter.getName();
	
	File file = new File(dir);
	file.mkdir();

	// make structures header file in this directory
	StructureIncludeFile.doit(RawBackend.structures, dir);

	// set magic net to false
	KjcOptions.magic_net = false;
	//set rate match to false
	KjcOptions.ratematch = false;
	//turn off output limiting
	KjcOptions.outputs = -1;

	//make a new FlatGraph with only this filter...
	FlatNode top = new FlatNode(filter);
	
	//VarDecl Raise to move array assignments up
	new VarDeclRaiser().raiseVars(filter);
	
	//VarDecl Raise to move peek index up so
	//constant prop propagates the peek buffer index
	new VarDeclRaiser().raiseVars(filter);

	Layout oldLayout = Layout.getLayout();
	
	// layout the components (assign filters to tiles)	
	Layout.simAnnealAssign(top);

	//Generate the tile code
	if (!containsRawMain(filter))
	    RawExecutionCode.doit(top);

	if (KjcOptions.removeglobals) {
	    RemoveGlobals.doit(top);
	}

	TileCode.generateCode(top);
	MakefileGenerator.createMakefile();
	
	try {
	    //copy the files 
	    {
		String[] cmdArray = new String[5];
		cmdArray[0] = "cp";
		cmdArray[1] = "tile0.c";
		cmdArray[2] = "Makefile.streamit";
		cmdArray[3] = "fileio.bc";
		cmdArray[4] = dir;    
		Process jProcess = Runtime.getRuntime().exec(cmdArray);
		jProcess.waitFor();
	    }
	    
	    //now determine if the filter fit in IMEM
	    {
		System.out.println("Checking IMEM Size...");	     
		String[] cmdArray = new String[6];
		cmdArray[0] = "make";
		cmdArray[1] = "-C";
		cmdArray[2] = dir;
		cmdArray[3] = "-f";
		cmdArray[4] = "Makefile.streamit";
		cmdArray[5] = "verify_imem";
		Process jProcess = Runtime.getRuntime().exec(cmdArray);
		jProcess.waitFor();

		//set the return value based on the exit code of the make 
		fits = (jProcess.exitValue() == 0);
	    }

	    //remove the directory
	    {
 		String[] cmdArray = new String[3];
 		cmdArray[0] = "rm";
 		cmdArray[1] = "-rf";
 		cmdArray[2] = dir;
 		Process jProcess = Runtime.getRuntime().exec(cmdArray);
 		jProcess.waitFor();
	    }
	  
	}
	catch (Exception e) {
	    e.printStackTrace();
	    Utils.fail("Error running the raw simulator for work estimation");
	}
	

	TESTING_IMEM = false;
	KjcOptions.magic_net = oldMagicNetValue;
	KjcOptions.ratematch = oldRateMatchValue;
	KjcOptions.outputs = oldOutputsValue;
	Layout.setLayout(oldLayout);
	return fits;
    }
    
    private static boolean containsRawMain(SIRFilter filter) 
    {
	for (int i = 0; i < filter.getMethods().length; i++) {
	    if (filter.getMethods()[i].getName().equals(RawExecutionCode.rawMain))
		return true;
	}
	return false;
    }
    
}

