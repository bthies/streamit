package at.dms.kjc.raw;

import at.dms.kjc.common.StructureIncludeFile;
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
import at.dms.kjc.spacedynamic.SpaceDynamicBackend;
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
    public static int estimateWork(SIRFilter oldFilter)
    {
        // if we have an identity filter, just return 0 since these
        // aren't mapped onto Raw
        if (oldFilter instanceof SIRIdentity || 
            oldFilter instanceof SIRFileWriter ||
            oldFilter instanceof SIRFileReader) {
            return 0;
        }

        boolean oldDecoupledValue = KjcOptions.decoupled;
        boolean oldMagicNetValue = KjcOptions.magic_net;
        boolean oldRateMatchValue = KjcOptions.ratematch;
        int oldOutputsValue = KjcOptions.outputs;
        int oldCols = RawBackend.rawColumns;
        int oldRows = RawBackend.rawRows;
        RawBackend.rawColumns = 4;
        RawBackend.rawRows = 4;

        int work = 0;
        //clone the Filter and create a dummy pipeline with just this
        //new cloned filter
        SIRFilter filter = (SIRFilter)ObjectDeepCloner.deepCopy(oldFilter);
        SIRPipeline pipe = new SIRPipeline("top");
        LinkedList<Object> list = new LinkedList<Object>();
        list.add(filter);
        pipe.setChildren(list);

        //make a new directory and change the current working dir
        String dir = File.separator + "tmp" + File.separator + 
            filter.getName();
    
        File file = new File(dir);
        file.mkdir();

        // set decouple execution to true
        KjcOptions.decoupled = true;
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

        String tileNumber = Layout.getTileNumber(top) + "";

        //remove print statements in the original app
        //if we are running with decoupled
        RemovePrintStatements.doIt(top);
    
        //Generate the tile code
        RawExecutionCode.doit(top);

        if (KjcOptions.removeglobals) {
            RemoveGlobals.doit(top);
        }

        if (KjcOptions.spacetime) {
            structures = SpaceTimeBackend.structures;
        }
        else if (KjcOptions.space) {
            structures = RawBackend.structures;
        }
        else {
            structures = SpaceDynamicBackend.structures;
        }
        System.out.println(structures);
        StructureIncludeFile.doit(structures, top, dir);

        SIMULATING_WORK = true;
        TileCode.generateCode(top);
        SIMULATING_WORK = false;
        MakefileGenerator.createMakefile();

        try {
            //copy the files 
            {
                System.out.println("Moving files to /tmp...");
                String[] cmdArray = new String[5];
                cmdArray[0] = "cp";
                cmdArray[1] = "tile" + tileNumber + ".c";
                cmdArray[2] = "Makefile.streamit";
                cmdArray[3] = "fileio.bc";
                cmdArray[4] = dir;    
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
    
        RawBackend.rawColumns = oldCols;
        RawBackend.rawRows = oldRows;
        KjcOptions.decoupled = oldDecoupledValue;
        KjcOptions.magic_net = oldMagicNetValue;
        KjcOptions.ratematch = oldRateMatchValue;
        KjcOptions.outputs = oldOutputsValue;
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
    
        IterFactory.createFactory().createIter(stream).accept(new RawWorkEstimator());
        System.out.println("Finished Testing Work Estimator ... ");
    }
    
    public void visitFilter(SIRFilter self,
                            SIRFilterIter iter) 
    {
        System.out.println(self.getName() + " " + estimateWork(self));
    }

}


