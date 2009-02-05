package at.dms.kjc;

import at.dms.kjc.sir.*;
import java.lang.reflect.*;

/**
 * This provides the toplevel interface for StreaMIT.
 */
public class StreaMITMain {
    private static Object params[] = new Object[6];
    private static Class paramTypes[] = new Class[6];
    
    //Only use from backends after done with params
    public static void clearParams() {
        params[0]=null;
        params[1]=null;
        params[2]=null;
        params[3]=null;
        params[4]=null;
        params[5]=null;
        params=null;
        paramTypes[0]=null;
        paramTypes[1]=null;
        paramTypes[2]=null;
        paramTypes[3]=null;     
        paramTypes[4]=null;     
        paramTypes[5]=null;     
    }

    /**
     * Prints out C code for the program being compiled.
     */
    public static void compile(JCompilationUnit[] app) {
        //using the raw backend to generate uniprocessor code
        //so set the number of tiles to 1 and 
        //turn partitioning on...
        //make sure that cluster option has not been turned on
        if ((KjcOptions.standalone || KjcOptions.rstream) && KjcOptions.cluster == -1) {
            KjcOptions.raw = 1;
            KjcOptions.partition_dp = true;
        }

        if (KjcOptions.malloczeros) 
            System.err.println("\n***  --malloczeros enabled, make sure your raw simulator initializes memory with zeros ***\n");
        
//        if (KjcOptions.altcodegen &&
//            KjcOptions.standalone)
//            at.dms.util.Utils.fail("The options altcodegen and standalone are mutually exclusive.");
//
//        if (KjcOptions.altcodegen &&
//            KjcOptions.decoupled)
//            at.dms.util.Utils.fail("The options altcodegen and decoupled are mutually exclusive.");

        if (KjcOptions.outputs > 0 &&
            KjcOptions.numbers > 0)
            at.dms.util.Utils.fail("The options outputs and numbers are mutually exclusive.");
            
        if (KjcOptions.magic_net &&
            KjcOptions.decoupled)
            at.dms.util.Utils.fail("The options magic_net and decoupled are mutually exclusive.");

        if (KjcOptions.countops && (KjcOptions.cluster>0) && !KjcOptions.standalone) {
            at.dms.util.Utils.fail("To use --countops, you must also use --standalone.");
        }
        
        System.err.print("Starting Kopi2SIR...");

        Kopi2SIR k2s = new Kopi2SIR(app);
        SIRStream stream = null;
        for (int i = 0; i < app.length; i++) {
            //System.err.println("Visiting "+i+" of "+(app.length-1));
            SIRStream top = (SIRStream)app[i].accept(k2s);
            if (top != null) {
                stream = top;
            }
        }

        System.err.println(" done.");

        SemanticChecker.doCheck(stream);

        String backendClass = null;
        String backendMethod = "run";
        if (KjcOptions.backend != null) {
            backendClass = KjcOptions.backend;
        } else if (KjcOptions.graph) {
            backendClass = "streamit.eclipse.grapheditor.graph.GraphEncoder";
        } else if (KjcOptions.rstream) {
            backendClass = "at.dms.kjc.rstream.StrToRStream";
        } else if (KjcOptions.raw != -1) {
            if (KjcOptions.spacetime) {
                backendClass = "at.dms.kjc.spacetime.SpaceTimeBackend";
            } else if (KjcOptions.space) {
                backendClass = "at.dms.kjc.raw.RawBackend";
            } else {
                backendClass = "at.dms.kjc.spacedynamic.SpaceDynamicBackend";
            }
        } else if (KjcOptions.cell != -1) {
            backendClass = "at.dms.kjc.cell.CellBackend";
        } else if (KjcOptions.cluster != -1) {
            backendClass = "at.dms.kjc.cluster.ClusterBackend";
        } else if (KjcOptions.newSimple != -1) {
            backendClass = "at.dms.kjc.vanillaSlice.UniBackEnd";
        } else if (KjcOptions.tilera != -1) {
	    backendClass = "at.dms.kjc.tilera.TileraBackend";
        } else if (KjcOptions.smp != -1) {
            backendClass = "at.dms.kjc.smp.SMPBackend";
        }
	else {
            backendClass = "at.dms.kjc.sir.lowering.Flattener";
            // backendMethod = "flatten";
        }
        
        // To find a method, we need its name and signature.  To
        // invoke it, we need to stuff the parameters into an
        // Object[]; given this, it's easy to get the types.
        params[0] = stream;
        params[1] = k2s.getInterfaces();
        params[2] = k2s.getInterfaceTables();
        params[3] = k2s.getStructures();
        params[4] = k2s.getHelpers();
        params[5] = k2s.getGlobal();

        Method theMethod = null;
        Class theBackend = null;
        
        try {
            paramTypes[0] = Class.forName("at.dms.kjc.sir.SIRStream");
            paramTypes[5] = Class.forName("at.dms.kjc.sir.SIRGlobal");
            for (int i = 1; i < 5; i++) {
                paramTypes[i] = params[i].getClass(); }
        } catch (ClassNotFoundException e) {
            System.err.println("*** The class " + e.getMessage() +
                               " does not exist.");
            return;
        }

        try {
            theBackend = Class.forName(backendClass);
        } catch (ClassNotFoundException e) {
            System.err.println("*** The class " + e.getMessage() +
                               " does not exist.");
            return;
        }       
            
        try {
            theMethod = theBackend.getMethod(backendMethod, paramTypes);
        } catch (NoSuchMethodException e) {

            //try the old calling convention

            Object old_params[] = new Object[4];
            Class old_paramTypes[] = new Class[4];
            for (int i = 0; i < 4; i++) {
                old_params[i] = params[i];
                old_paramTypes[i] = paramTypes[i];
            }
            params = old_params;
            paramTypes = old_paramTypes;

            try {
                theMethod = theBackend.getMethod(backendMethod, paramTypes);
            } catch (NoSuchMethodException e2) {
                System.err.println("*** The backend method " +
                                   backendClass + "." + backendMethod + "()" +
                                   " does not exist.");
            }
        }

        stream=null;
        k2s=null;
        System.gc();

        try {
            theMethod.invoke(null, params);
        } catch (IllegalAccessException e) {
            System.err.println("*** Not allowed to invoke backend " +
                               backendClass);
            System.exit(1);
        } catch (InvocationTargetException e) {
            // Loses debugging information on the exception, sigh.
            // We can't blindly rethrow the exception because it might
            // not be a RuntimeException.  I hate Java.  Die as best we can.
            e.getTargetException().printStackTrace();
            System.exit(1);
        }

    }
}
