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


public class RawWorkEstimator extends EmptyStreamVisitor
{
   /** 
     * Given a filter, run the work function on raw
     * and return the number of cycles for the work function
     **/
    public static int estimateWork(SIRFilter oldFilter)
    {
	// if we have an identity filter, just return 0 since these
	// aren't mapped onto Raw
	if (oldFilter instanceof SIRIdentity) {
	    return 0;
	}

	boolean oldDecoupledValue = KjcOptions.decoupled;
	boolean oldMagicNetValue = KjcOptions.magic_net;
	boolean oldRateMatchValue = KjcOptions.ratematch;

	int work = 0;
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
	// set decouple execution to true
	KjcOptions.decoupled = true;
	// set magic net to false
	KjcOptions.magic_net = false;
	//set rate match to false
	KjcOptions.ratematch = false;

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

	//remove print statements in the original app
	//if we are running with decoupled
	RemovePrintStatements.doIt(top);
	
	//Generate the tile code
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
	    
	    //run the simulator
	    {
		String[] cmdArray = new String[6];
		cmdArray[0] = "make";
		cmdArray[1] = "-C";
		cmdArray[2] = dir;
		cmdArray[3] = "-f";
		cmdArray[4] = "Makefile.streamit";
		cmdArray[5] = "run";
		Process jProcess = Runtime.getRuntime().exec(cmdArray);
		jProcess.waitFor();
	    }
	    
	    //open the results file and return the stats
	    work = readCycleCount(dir);
	    
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
	

	KjcOptions.decoupled = oldDecoupledValue;
	KjcOptions.magic_net = oldMagicNetValue;
	KjcOptions.ratematch = oldRateMatchValue;
	Layout.setLayout(oldLayout);
	return work;
    }

    //read the results of the simulation 
    //the output file is in an easy format
    private static int readCycleCount(String dir) throws Exception
    {
	FileReader fr = new FileReader(dir + File.separator + 
				       "work_est.out");
	char current;
	String value = "";

	while ((current = (char)fr.read()) != ' ') {
	    value = value + current;
	}
	
	return (new Integer(value)).intValue();
    }
    
    public static void test(SIRStream stream) 
    {
	System.out.println("Begin Testing Work Estimator ... ");
	
	IterFactory.createIter(stream).accept(new RawWorkEstimator());
	System.out.println("Finished Testing Work Estimator ... ");
    }
    
    public void visitFilter(SIRFilter self,
		       SIRFilterIter iter) 
    {
	System.out.println(self.getName() + " " + estimateWork(self));
    }

}


