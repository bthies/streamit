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

public class IMEMEstimation implements FlatVisitor
{
    public static boolean TESTING_IMEM = false;
    /**
     * Whether or not everything this visitor has seen so far fits in
     * IMEM.
     */
    private boolean everythingFits = true;
    private static Random rand;
    private static String user;

    static 
    {
	rand = new Random(17);
	user = getUser();
    }

    public void visitNode(FlatNode node) {
	if (node.contents instanceof SIRFilter) {
	    SIRFilter filter = (SIRFilter)node.contents;
	    // don't do anything if we've already detected overflow
	    // earlier in graph.
	    if (!everythingFits) {
		return;
	    }
	    // otherwise, test for overflow at this node
	    if (!fits(filter)) {
		System.out.println("Filter " + filter.getName() + " does not fit in IMEM.");
		everythingFits = false;
	    } else {
		System.out.println("Filter " + filter.getName() + " fits in IMEM.");
	    }
	}
    }
    
    public IMEMEstimation() 
    {
	
    }
    
    /**
     * Returns true iff all filters in <str> fit in IMEM.  Each filter
     * is measured independently (assuming 1 filter per tile).
     */
    public static boolean testMe(FlatNode top) 
    {
	IMEMEstimation visitor = new IMEMEstimation();
	top.accept(visitor, null, true);
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
	
	int length = Math.min(dir.length() , 20);

	dir = dir.substring(0, length) + rand.nextInt(99999) + user;

	System.out.println("Checking IMEM Size (DIR: " + dir + ") ...");	     
	File file = new File(dir);
	file.mkdir();

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
	// make structures header file in this directory
	StructureIncludeFile.doit(RawBackend.structures, top, dir);

	TileCode.generateCode(top);
	MakefileGenerator.createMakefile();
	
	try {
	    //move the files 
	    {
		System.out.println("moving...");
		String[] cmdArray = new String[5];
		cmdArray[0] = "mv";
		cmdArray[1] = "tile0.c";
		cmdArray[2] = "Makefile.streamit";
		cmdArray[3] = "fileio.bc";
		cmdArray[4] = dir;    
		Process jProcess = Runtime.getRuntime().exec(cmdArray);
		jProcess.waitFor();
	    }
	    //now see if the compile succeeds
	    {
		System.out.println("build...");
		String[] cmdArray = new String[6];
		cmdArray[0] = "make";
		cmdArray[1] = "-C";
		cmdArray[2] = dir;
		cmdArray[3] = "-f";
		cmdArray[4] = "Makefile.streamit";
		cmdArray[5] = "tile0.s";
		Process jProcess = Runtime.getRuntime().exec(cmdArray);
		jProcess.waitFor();

		//set the return value based on the exit code of the make 
		assert (jProcess.exitValue() == 0) : 
		    "Failure to build C file for IMEM estimation";
	    }

	    //now determine if the filter fit in IMEM
	    {
		System.out.println("verify imem...");
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
		System.out.println("remove dir...");
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

    private static String getUser() 
    {
	String myvar = "";
	try {
	    Process p = Runtime.getRuntime().exec("whoami");
	    p.waitFor();
	    BufferedReader br = new BufferedReader
		( new InputStreamReader( p.getInputStream() ) );
	    myvar = br.readLine();
	}
	catch (Exception e) {
	    Utils.fail("Error getting user name");
	}
	return myvar;
    }
}

