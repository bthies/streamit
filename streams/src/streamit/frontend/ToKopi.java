package streamit.frontend;
import java.io.*;
import java.util.List;
import java.util.Iterator;
import streamit.frontend.nodes.*;
import streamit.frontend.passes.*;
import streamit.frontend.tojava.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.iterator.*;
import at.dms.util.*;

/**
 * Read StreamIt programs and run them through the main compiler.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;,
 *          David Ziegler &lt;dziegler@cag.lcs.mit.edu&gt;
 * @version $Id: ToKopi.java,v 1.2 2003-07-02 21:44:37 dmaze Exp $
 */
public class ToKopi
{
    public void printUsage()
    {
        System.err.println(
"streamit.frontend.ToKopi: StreamIt compiler\n" +
"Usage: java streamit.frontend.ToKopi in.str ...\n" +
"\n" +
"Options:\n" +
"  --help         Print this message\n" +
"\n");
    }

    private boolean printHelp = false;
    private String outputFile = null;
    private List inputFiles = new java.util.ArrayList();

    public void doOptions(String[] args)
    {
        for (int i = 0; i < args.length; i++)
        {
            if (args[i].equals("--help"))
                printHelp = true;
            else if (args[i].equals("--"))
            {
                // Add all of the remaining args as input files.
                for (i++; i < args.length; i++)
                    inputFiles.add(args[i]);
            }
            else if (args[i].equals("--output"))
                outputFile = args[++i];
            else
                // Maybe check for unrecognized options.
                inputFiles.add(args[i]);
        }
    }

    public static Program lowerIRToJava(Program prog)
    {
        /* What's the right order for these?  Clearly generic
         * things like MakeBodiesBlocks need to happen first.
         * I don't think there's actually a problem running
         * MoveStreamParameters after DoComplexProp, since
         * this introduces only straight assignments which the
         * Java front-end can handle.  OTOH,
         * MoveStreamParameters introduces references to
         * "this", which doesn't exist. */
        TempVarGen varGen = new TempVarGen();
        prog = (Program)prog.accept(new MakeBodiesBlocks());
        prog = (Program)prog.accept(new DisambiguateUnaries(varGen));
        prog = (Program)prog.accept(new NoRefTypes());
        prog = (Program)prog.accept(new RenameBitVars());
        prog = (Program)prog.accept(new FindFreeVariables());
        prog = (Program)prog.accept(new NoticePhasedFilters());
        prog = (Program)prog.accept(new DoComplexProp(varGen));
        prog = (Program)prog.accept(new TranslateEnqueue());
        prog = (Program)prog.accept(new InsertInitConstructors());
        prog = (Program)prog.accept(new MoveStreamParameters());
        prog = (Program)prog.accept(new NameAnonymousFunctions());
        prog = (Program)prog.accept(new TrimDumbDeadCode());
        return prog;
    }

    public void run(String[] args)
    {
        doOptions(args);
        if (printHelp)
        {
            printUsage();
            return;
        }
        
        Program prog = null;
        Writer outWriter;

        try
        {
            prog = ToJava.parseFiles(inputFiles);
        }
        catch (java.io.IOException e) {e.printStackTrace(System.err);}
        catch (antlr.RecognitionException e) {e.printStackTrace(System.err);}
        catch (antlr.TokenStreamException e) {e.printStackTrace(System.err);}

        if (prog == null)
        {
            System.err.println("Compilation didn't generate a parse tree.");
            return;
        }

        prog = lowerIRToJava(prog);

        SIRStream s = (SIRStream) prog.accept(new FEIRToSIR());
        SIRPrinter sirPrinter = new SIRPrinter();
        IterFactory.createIter(s).accept(sirPrinter);
        sirPrinter.close();
        Flattener.flatten(s, new JInterfaceDeclaration[0],
                          new SIRInterfaceTable[0], new SIRStructure[0]);
    }
    
    public static void main(String[] args)
    {
        new ToKopi().run(args);
    }

}
