package at.dms.kjc.spacedynamic;

import at.dms.kjc.flatgraph.FlatNode;
//import at.dms.util.IRPrinter;
//import at.dms.util.SIRPrinter;
import at.dms.kjc.*;
//import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
//import at.dms.kjc.sir.lowering.partition.*;
//import at.dms.kjc.sir.lowering.fusion.*;
//import at.dms.kjc.sir.lowering.fission.*;
//import at.dms.kjc.lir.*;
import java.util.*;
import at.dms.util.Utils;
import java.io.*;
//import at.dms.kjc.flatgraph.FlatNode;
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
    private SpdStaticStreamGraph ssg;

    static 
    {
        rand = KjcOptions.fixseed? new Random(17): new Random();
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
    
    public IMEMEstimation(SpdStaticStreamGraph ssg) 
    {
        this.ssg = ssg;
    }
    
    /**
     * Returns true iff all filters in <pre>str</pre> fit in IMEM.  Each filter
     * is measured independently (assuming 1 filter per tile).
     */
    public static boolean testMe(SpdStaticStreamGraph ssg, FlatNode top) 
    {
        IMEMEstimation visitor = new IMEMEstimation(ssg);
        top.accept(visitor, null, true);
        return visitor.everythingFits;
    }
    

    //returns true if it fits in IMEM
    public boolean fits(SIRFilter oldFilter) 
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
        boolean oldSimulateWorkValue = KjcOptions.simulatework;
        
        int oldOutputsValue = KjcOptions.outputs;
        //turn off simlate work while testing IMEM...
        KjcOptions.simulatework = false;
        
        //clone the Filter and create a dummy pipeline with just this
        //new cloned filter
        SIRFilter filter = (SIRFilter)ObjectDeepCloner.deepCopy(oldFilter);

        Util.removeIO(filter);

        //just call this 
        SpdStreamGraph streamGraph = SpdStreamGraph.constructStreamGraph(filter);
        SpdStaticStreamGraph fakeSSG = (SpdStaticStreamGraph)streamGraph.getStaticSubGraphs()[0];
        fakeSSG.scheduleAndCreateMults();
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
    
        //VarDecl Raise to move array assignments up
        new VarDeclRaiser().raiseVars(filter);
    
        //VarDecl Raise to move peek index up so
        //constant prop propagates the peek buffer index
        new VarDeclRaiser().raiseVars(filter);

        // layout the components (assign filters to tiles)  
        streamGraph.getLayout().singleFilterAssignment();
        
        String tileNumber = streamGraph.getLayout().getTileNumber(fakeSSG.getTopLevel()) + "";

        SwitchCode.generate(streamGraph);
        
        //restore the i/o rates of the filter we created
        //done for code generation purposes
        Util.restoreIO((SIRFilter)fakeSSG.getTopLevel().contents, oldFilter);
        
        //Generate the tile code
        if (!containsRawMain(filter)) {
            RawExecutionCode rawExe = new RawExecutionCode(fakeSSG);
            fakeSSG.getTopLevel().accept(rawExe, null, true);
        }
    
        if (KjcOptions.removeglobals) {
            RemoveGlobals.doit(fakeSSG.getTopLevel());
        }
    
        // make structures header file in this directory
        StructureIncludeFile.doit(SpaceDynamicBackend.structures, streamGraph, dir);
        TileCode.generateCode(streamGraph);
        MakefileGenerator.createMakefile(streamGraph);
    
        try {
            //move the files 
            {
                System.out.println("moving...");
                System.out.flush();
                String[] cmdArray = new String[6];
                cmdArray[0] = "mv";
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
            //now see if the compile succeeds
            {
                System.out.println("build...");
                /*
                  String[] cmdArray = new String[8];
                  cmdArray[0] = "make";
                  cmdArray[1] = "-C";
                  cmdArray[2] = dir;
                  cmdArray[3] = "-f";
                  cmdArray[4] = "Makefile.streamit";
                  cmdArray[5] = "tile" + tileNumber + ".s";
                  cmdArray[6] = "&>";
                  cmdArray[7] = "/dev/null";
                */
        
                String[] cmdArray = 
                    {
                        "/bin/bash",
                        "-c",
                        "make -C " + dir + " -f Makefile.streamit tile" + tileNumber + ".s  &> /dev/null"
                    };

                Process jProcess = Runtime.getRuntime().exec(cmdArray);
                /*
                  InputStreamReader output = new InputStreamReader(jProcess.getInputStream());
                  BufferedReader br = new BufferedReader(output);
                  try {
                  String str;
                  while ((str = br.readLine()) != null) {
                  //System.out.println(str);
                  }
                  } catch (IOException e) {
                  System.err.println("Error reading stdout of child process in work estimation...");
                  }
                */

                jProcess.waitFor();
        
                /*
                //dump the output so that the process does not hang on it
                InputStream output = jProcess.getInputStream();
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
        
                //jProcess.waitFor();

                //set the return value based on the exit code of the make 
                //assert (jProcess.exitValue() == 0) : 
                //    "Failure to build C file for IMEM estimation";
            
                if (jProcess.exitValue() != 0) {
                    fits = false;
                }
                
                jProcess.getInputStream().close();
                jProcess.getOutputStream().close();
                jProcess.getErrorStream().close();
            }

            //if gcc did not die and we have already decided that it does not fit
            if (fits) {
                //now determine if the filter fit in IMEM
                {
                    System.out.println("verify imem...");
                    /*
                     String[] cmdArray = new String[8];
                     cmdArray[0] = "make";
                     cmdArray[1] = "-C";
                     cmdArray[2] = dir;
                     cmdArray[3] = "-f";
                     cmdArray[4] = "Makefile.streamit";
                     cmdArray[5] = "verify_imem";
                     cmdArray[6] = "&>";
                     cmdArray[7] = "/dev/null";
                     */
                    
                    String[] cmdArray = 
                    {
                            "/bin/bash",
                            "-c",
                            "make -C " + dir + " -f Makefile.streamit verify_imem  &> /dev/null"
                    };
                    Process jProcess = Runtime.getRuntime().exec(cmdArray);
                    /*  
                     InputStreamReader output = new InputStreamReader(jProcess.getInputStream());
                     BufferedReader br = new BufferedReader(output);
                     try {
                     String str;
                     while ((str = br.readLine()) != null) {
                     //System.out.println(str);
                      }
                      } catch (IOException e) {
                      System.err.println("Error reading stdout of child process in work estimation...");
                      }
                      */
                    
                    jProcess.waitFor();
                    
                    /*
                     //dump the output so that the process does not hang on it
                      InputStream output = jProcess.getInputStream();
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
                    //set the return value based on the exit code of the make 
                    fits = (jProcess.exitValue() == 0);
                    
                    jProcess.getInputStream().close();
                    jProcess.getOutputStream().close();
                    jProcess.getErrorStream().close();
                }
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
    
                jProcess.getInputStream().close();
                jProcess.getOutputStream().close();
                jProcess.getErrorStream().close();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            Utils.fail("Error running the raw simulator for work estimation");
        }

        TESTING_IMEM = false;
        KjcOptions.simulatework = oldSimulateWorkValue;
        KjcOptions.magic_net = oldMagicNetValue;
        KjcOptions.ratematch = oldRateMatchValue;
        KjcOptions.outputs = oldOutputsValue;
        //do something with layout
        //Layout.setLayout(oldLayout);
        return fits;
    }
    
    /**
     * Return true if filter contains a declaration of the RAWMAIN function.
     * 
     * @param filter 
     * @return true if filter contains a declaration of the RAWMAIN function.
     */
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

