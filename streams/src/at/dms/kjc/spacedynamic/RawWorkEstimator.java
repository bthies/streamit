package at.dms.kjc.spacedynamic;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.util.IRPrinter;
import at.dms.util.SIRPrinter;
import at.dms.kjc.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.raw.RawBackend;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.partition.*;
import at.dms.kjc.sir.lowering.fusion.*;
import at.dms.kjc.sir.lowering.fission.*;
import at.dms.kjc.spacetime.SpaceTimeBackend;
import at.dms.kjc.lir.*;
import java.util.*;
import at.dms.util.Utils;
import java.io.*;


public class RawWorkEstimator extends EmptyStreamVisitor
{
    public static SIRStructure[] structures;
    /**
     * Global indicator that we're in the process of simulating work.
     * Avoids threading some information through multiple levels.
     */
    public static boolean SIMULATING_WORK = false;
    /** 
     * Given a filter, run the work function on raw
     * and return the number of cycles for the work function
     **/
    public static long estimateWork(SIRFilter oldFilter)
    {
        // if we have an identity filter, just return 0 since these
        // aren't mapped onto Raw
        if (oldFilter instanceof SIRIdentity || 
            oldFilter instanceof SIRFileWriter ||
            oldFilter instanceof SIRFileReader) {
            return 0;
        }

	boolean oldHWICValue = KjcOptions.hwic;
        boolean oldMagicNetValue = KjcOptions.magic_net;
        boolean oldRateMatchValue = KjcOptions.ratematch;
        boolean oldSimulateWorkValue = KjcOptions.simulatework;
                 
        int oldOutputsValue = KjcOptions.outputs;
        RawChip oldRawChip = SpaceDynamicBackend.rawChip;
        SpaceDynamicBackend.rawChip = new RawChip(4, 4);
        
        long work = 0;
        //clone the Filter and create a dummy pipeline with just this
        //new cloned filter

        KjcOptions.simulatework = false;
        SIMULATING_WORK = true;
        
        SIRFilter filter = (SIRFilter)ObjectDeepCloner.deepCopy(oldFilter);

        Util.removeIO(filter);

        SpdStreamGraph streamGraph = SpdStreamGraph.constructStreamGraph(filter);
        SpdStaticStreamGraph ssg = (SpdStaticStreamGraph)streamGraph.getStaticSubGraphs()[0];
        ssg.scheduleAndCreateMults();
        //make a new directory and change the current working dir
        String dir = File.separator + "tmp" + File.separator + 
            filter.getName();
    
        File file = new File(dir);
        file.mkdir();

        // set magic net to false
        KjcOptions.magic_net = false;
        //set rate match to false
        KjcOptions.ratematch = false;
        //turn off output limiting
        KjcOptions.outputs = -1;
	//turn off hardware icaching
	KjcOptions.hwic = false;

        //VarDecl Raise to move array assignments up
        new VarDeclRaiser().raiseVars(filter);
    
        //VarDecl Raise to move peek index up so
        //constant prop propagates the peek buffer index
        new VarDeclRaiser().raiseVars(filter);

        // layout the components (assign filters to tiles)  
        streamGraph.getLayout().singleFilterAssignment();

        String tileNumber = streamGraph.getLayout().getTileNumber(ssg.getTopLevel()) + "";

        //remove print statements in the original app
        //if we are running with decoupled
        RemovePrintStatements.doIt(ssg.getTopLevel());
    
        SwitchCode.generate(streamGraph);
        
        //restore the i/o rates of the filter we created
        //done for code generation purposes
        Util.restoreIO((SIRFilter)ssg.getTopLevel().contents, oldFilter);
        
        //Generate the tile code
        
        RawExecutionCode rawExe = new RawExecutionCode(ssg);
        ssg.getTopLevel().accept(rawExe, null, true);
        
        if (KjcOptions.removeglobals) {
            RemoveGlobals.doit(ssg.getTopLevel());
        }
    
        // make structures header file in this directory
        if (KjcOptions.spacetime) {
            structures = SpaceTimeBackend.getStructures();
        }
        else if (KjcOptions.space) {
            structures = RawBackend.structures;
        }
        else {
            structures = SpaceDynamicBackend.structures;
        }
        
        StructureIncludeFile.doit(structures, streamGraph, dir);

        TileCode.generateCode(streamGraph);
        MakefileGenerator.createMakefile(streamGraph);
        SIMULATING_WORK = false;

        try {
            //copy the files 
            {
                System.out.println("Moving files to /tmp...");
                String[] cmdArray = new String[6];
                cmdArray[0] = "cp";
                cmdArray[1] = "tile" + tileNumber + ".c";
                cmdArray[2] = "Makefile.streamit";
                cmdArray[3] = "fileio.bc";
                cmdArray[4] = "sw" + tileNumber + ".s";
                cmdArray[5] = dir;    
                Process jProcess = Runtime.getRuntime().exec(cmdArray);
                jProcess.waitFor();
            
                jProcess.getInputStream().close();
                jProcess.getOutputStream().close();
                jProcess.getErrorStream().close();
            }
        
            //run the simulator
            {
                System.out.println("Running on the Simulator...");
                /*
                  String[] cmdArray = new String[8];
                  cmdArray[0] = "make";
                  cmdArray[1] = "-C";
                  cmdArray[2] = dir;
                  cmdArray[3] = "-f";
                  cmdArray[4] = "Makefile.streamit";
                  cmdArray[5] = "run";
                  cmdArray[6] = "&>";
                  cmdArray[7] = "/dev/null";
        
                  Process jProcess = Runtime.getRuntime().exec(cmdArray);
                */

                String[] cmdArray = 
                    {
                        "/bin/bash",
                        "-c",
                        "make -C " + dir + " -f Makefile.streamit run &> /dev/null"
                    };
        
            
                Process jProcess = 
                    Runtime.getRuntime().exec(cmdArray);
                /*
                  InputStreamReader output = new InputStreamReader(jProcess.getInputStream());
                  BufferedReader br = new BufferedReader(output);
                  try {
                  String str;
                  while ((str = br.readLine()) != null) {
                  System.out.println(str);
                  }
                  } catch (IOException e) {
                  System.err.println("Error reading stdout of child process in work estimation...");
                  }
                */
        
                jProcess.waitFor();

                jProcess.getInputStream().close();
                jProcess.getOutputStream().close();
                jProcess.getErrorStream().close();
        
    
                //dump the output so that the process does not hang on it
                /*
                  try {
                  InputStreamReader isr = new InputStreamReader(output);
                  BufferedReader br = new BufferedReader(isr);
                  String line = null;
                  while ((line = br.readLine()) != null) {
                  }
                  }
                  catch (Exception e) {
                  e.printStackTrace();
                  }
                */
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
            
                jProcess.getInputStream().close();
                jProcess.getOutputStream().close();
                jProcess.getErrorStream().close();
            }
        
        }
        catch (Exception e) {
            e.printStackTrace();
            Utils.fail("Error running the raw simulator for work estimation");
        }
    
        SpaceDynamicBackend.rawChip = oldRawChip;
        KjcOptions.simulatework = oldSimulateWorkValue;
        KjcOptions.magic_net = oldMagicNetValue;
        KjcOptions.ratematch = oldRateMatchValue;
        KjcOptions.outputs = oldOutputsValue;
	KjcOptions.hwic = oldHWICValue;
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
    
        IterFactory.createFactory().createIter(stream).accept(new RawWorkEstimator());
        System.out.println("Finished Testing Work Estimator ... ");
    }
    
    public void visitFilter(SIRFilter self,
                            SIRFilterIter iter) 
    {
        System.out.println(self.getName() + " " + estimateWork(self));
    }

}


