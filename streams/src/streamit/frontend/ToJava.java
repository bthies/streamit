package streamit.frontend;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import streamit.frontend.nodes.*;
import streamit.frontend.passes.*;
import streamit.frontend.tojava.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.iterator.*;
import at.dms.util.*;

class ToJava
{
    public int indent = 0;
    public String getIndent ()
    {
        int x = indent;
        String r = "";
        while (x-- > 0) r = r + "  ";
        return r;
    }

    public void printUsage()
    {
        System.err.println(
"streamit.frontend.ToJava: StreamIt syntax translator\n" +
"Usage: java streamit.frontend.ToJava [--output out.java] in.str ...\n" +
"\n" +
"Options:\n" +
"  --library      Output code suitable for the Java library\n" +
"  --help         Print this message\n" +
"  --output file  Write output to file, not stdout\n" +
"\n");
    }

    private boolean printHelp = false;
    private boolean libraryFormat = false;
    private String outputFile = null;
  private boolean straightToSIR = false;
    private List inputFiles = new ArrayList();

    public void doOptions(String[] args)
    {
        for (int i = 0; i < args.length; i++)
        {
            if (args[i].equals("--full"))
                ; // Accept but ignore for compatibility
            else if (args[i].equals("--help"))
                printHelp = true;
            else if (args[i].equals("--"))
            {
                // Add all of the remaining args as input files.
                for (i++; i < args.length; i++)
                    inputFiles.add(args[i]);
            }
            else if (args[i].equals("--output"))
                outputFile = args[++i];
            else if (args[i].equals("--library"))
                libraryFormat = true;
	    else if (args[i].equals("--ziggy"))
	      straightToSIR = true;
            else
                // Maybe check for unrecognized options.
                inputFiles.add(args[i]);
        }
    }

    public void run(String[] args)
    {
        doOptions(args);
        if (printHelp)
        {
            printUsage();
            return;
        }
        
        StreamItParserFE parser = null;
        Program prog = null;
        Writer outWriter;

        try
        {
            if (outputFile != null)
                outWriter = new FileWriter(outputFile);
            else
                outWriter = new OutputStreamWriter(System.out);
            outWriter.write("import streamit.*;\n");

            /* Might want to check whether this is actually necessary;
             * there have been complaints before. */
            outWriter.write("class Complex extends Structure {\n" +
                            "  public float real;\n" +
                            "  public float imag;\n" +
                            "}\n");

            for (Iterator iter = inputFiles.iterator(); iter.hasNext(); )
            {
                String fileName = (String)iter.next();
                InputStream inStream = new FileInputStream(fileName);
                DataInputStream dis = new DataInputStream(inStream);
                StreamItLex lexer = new StreamItLex(dis);
                parser = new StreamItParserFE(lexer);
                parser.setFilename(fileName);
                prog = parser.program();
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
                prog = (Program)prog.accept(new RenameBitVars());
                prog = (Program)prog.accept(new FindFreeVariables());
                if (!libraryFormat)
                    prog = (Program)prog.accept(new NoticePhasedFilters());
                prog = (Program)prog.accept(new DoComplexProp(varGen));
                prog = (Program)prog.accept(new TranslateEnqueue());
		prog = (Program)prog.accept(new InsertIODecls(libraryFormat));
                prog = (Program)prog.accept(new InsertInitConstructors());
                prog = (Program)prog.accept(new MoveStreamParameters());
                prog = (Program)prog.accept(new NameAnonymousFunctions());
		if (straightToSIR) {
		  SIRStream s = (SIRStream) prog.accept(new FEIRToSIR());
		  SIRPrinter sirPrinter = new SIRPrinter();
		  IterFactory.createIter(s).accept(sirPrinter);
		  sirPrinter.close();
		  Flattener.flatten(s, new JInterfaceDeclaration[0], new SIRInterfaceTable[0], new SIRStructure[0]);
		} else {
		  String javaOut = (String)prog.accept(new NodesToJava(null));
		  outWriter.write(javaOut);
		}
            }
            outWriter.flush();
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
            return;
        }
        
    }
    
    public static void main(String[] args)
    {
        new ToJava().run(args);
    }
}

