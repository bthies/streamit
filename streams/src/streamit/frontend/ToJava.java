/*
 * Copyright 2003 by the Massachusetts Institute of Technology.
 *
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in
 * advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 * M.I.T. makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is"
 * without express or implied warranty.
 */

package streamit.frontend;
import java.io.*;
import java.util.List;
import java.util.Iterator;
import streamit.frontend.nodes.*;
import streamit.frontend.passes.*;
import streamit.frontend.tojava.*;

/**
 * Convert StreamIt programs to legal Java code.  This is the main
 * entry point for the StreamIt syntax converter.  Running it as
 * a standalone program reads the list of files provided on the
 * command line and produces equivalent Java code on standard
 * output or the file named in the <tt>--output</tt> command-line
 * parameter.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: ToJava.java,v 1.78 2006-09-25 13:54:52 dimock Exp $
 */
public class ToJava
{
    public void printUsage()
    {
        System.err.println(
                           "streamit.frontend.ToJava: StreamIt syntax translator\n" +
                           "Usage: java streamit.frontend.ToJava [--output out.java] in.str ...\n" +
                           "\n" +
                           "Options:\n" +
                           "  --library      Output code suitable for the Java library\n" +
                           "  --countops     Instrument code to count arith ops in Java library\n" +
                           "  --help         Print this message\n" +
                           "  --output file  Write output to file, not stdout\n" +
                           "\n");
    }

    private boolean printHelp = false;
    private boolean libraryFormat = false;
    private boolean countops = false;
    private String outputFile = null;
    private List<String> inputFiles = new java.util.ArrayList<String>();

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
                else if (args[i].equals("--countops"))
                    countops = true;
                else
                    // Maybe check for unrecognized options.
                    inputFiles.add(args[i]);
            }
    }

    /**
     * Generate a Program object that includes built-in structures
     * and streams with code, but no user code.
     *
     * @return a StreamIt program containing only built-in code
     */
    public static Program emptyProgram()
    {
        List streams = new java.util.ArrayList();
        List structs = new java.util.ArrayList();
        List helpers = new java.util.ArrayList();
        
        // Complex structure type:
        List fields = new java.util.ArrayList();
        List ftypes = new java.util.ArrayList();
        Type floattype = new TypePrimitive(TypePrimitive.TYPE_FLOAT);
        fields.add("real");
        ftypes.add(floattype);
        fields.add("imag");
        ftypes.add(floattype);
        TypeStruct complexStruct =
            new TypeStruct(null, "Complex", fields, ftypes);
        structs.add(complexStruct);

        // float2
        fields = new java.util.ArrayList();
        ftypes = new java.util.ArrayList();
        fields.add("x");
        ftypes.add(floattype);
        fields.add("y");
        ftypes.add(floattype);
        structs.add(new TypeStruct(null, "float2", fields, ftypes));

        // float3
        fields = new java.util.ArrayList();
        ftypes = new java.util.ArrayList();
        fields.add("x");
        ftypes.add(floattype);
        fields.add("y");
        ftypes.add(floattype);
        fields.add("z");
        ftypes.add(floattype);
        structs.add(new TypeStruct(null, "float3", fields, ftypes));

        // float4
        fields = new java.util.ArrayList();
        ftypes = new java.util.ArrayList();
        fields.add("x");
        ftypes.add(floattype);
        fields.add("y");
        ftypes.add(floattype);
        fields.add("z");
        ftypes.add(floattype);
        fields.add("w");
        ftypes.add(floattype);
        structs.add(new TypeStruct(null, "float4", fields, ftypes));
        
        fields = new java.util.ArrayList(); 
        ftypes = new java.util.ArrayList(); 
        TypeStruct stringStruct = new TypeStruct(null, "String", fields, ftypes);
        structs.add(stringStruct);

        return new Program(null, streams, structs, helpers);
    }

    /**
     * Read, parse, and combine all of the StreamIt code in a list of
     * files.  Reads each of the files in <code>inputFiles</code> in
     * turn and runs <code>streamit.frontend.StreamItParserFE</code>
     * over it.  This produces a
     * <code>streamit.frontend.nodes.Program</code> containing lists
     * of structures and streams; combine these into a single
     * <code>streamit.frontend.nodes.Program</code> with all of the
     * structures and streams.
     *
     * @param inputFiles  list of strings naming the files to be read
     * @return a representation of the entire program, composed of the
     *          code in all of the input files
     * @throws java.io.IOException if an error occurs reading the input
     *         files
     * @throws antlr.RecognitionException if an error occurs parsing
     *         the input files; that is, if the code is syntactically
     *         incorrect
     * @throws antlr.TokenStreamException if an error occurs producing
     *         the input token stream
     */
    public static Program parseFiles(List<String> inputFiles)
        throws java.io.IOException,
               antlr.RecognitionException, 
               antlr.TokenStreamException
    {
        Program prog = emptyProgram();
        for (Iterator<String> iter = inputFiles.iterator(); iter.hasNext(); )
            {
                String fileName = iter.next();
                InputStream inStream = new FileInputStream(fileName);
                DataInputStream dis = new DataInputStream(inStream);
                StreamItLex lexer = new StreamItLex(dis);
                StreamItParserFE parser = new StreamItParserFE(lexer);
                parser.setFilename(fileName);
                Program pprog = parser.program();
                if (pprog != null)
                    {
                        List newStreams, newStructs, newHelpers;
                        newStreams = new java.util.ArrayList();
                        newStreams.addAll(prog.getStreams());
                        newStreams.addAll(pprog.getStreams());
                        newStructs = new java.util.ArrayList();
                        newStructs.addAll(prog.getStructs());
                        newStructs.addAll(pprog.getStructs());
                        newHelpers = new java.util.ArrayList();
                        newHelpers.addAll(prog.getHelpers());
                        newHelpers.addAll(pprog.getHelpers());
                        prog = new Program(null, newStreams, newStructs, newHelpers);
                    }
            }
        return prog;
    }

    /**
     * Transform front-end code to have the Java syntax.  Goes through
     * a series of lowering passes to convert an IR tree from the
     * "new" syntax to the "old" Java syntax understood by the main
     * StreamIt compiler.  Conversion directed towards the StreamIt
     * Java library, as opposed to the compiler, has slightly
     * different output.
     *
     * @param prog  the complete IR tree to lower
     * @param libraryFormat  true if the program is being converted
     *        to run under the StreamIt Java library
     * @param varGen  object to generate unique temporary variable names
     * @return the converted IR tree
     */
    public static Program lowerIRToJava(Program prog, boolean libraryFormat,
                                        TempVarGen varGen)
    {

        /* What's the right order for these?  Clearly generic
         * things like MakeBodiesBlocks need to happen first.
         * I don't think there's actually a problem running
         * MoveStreamParameters after DoComplexProp, since
         * this introduces only straight assignments which the
         * Java front-end can handle.  OTOH,
         * MoveStreamParameters introduces references to
         * "this", which doesn't exist. */
        prog = (Program)prog.accept(new MakeBodiesBlocks());
        prog = (Program)prog.accept(new SeparateInitializers());
        prog = (Program)prog.accept(new DisambiguateUnaries(varGen));
        prog = (Program)prog.accept(new NoRefTypes());
        prog = (Program)prog.accept(new NameAnonymousStreams(varGen));
        //prog = (Program)prog.accept(new GenerateCopies(varGen));
        prog = (Program)prog.accept(new DoComplexProp(varGen));
        prog = (Program)prog.accept(new DoCompositeProp(varGen));
        prog = (Program)prog.accept(new ComplexToStruct());
        prog = (Program)prog.accept(new SeparateInitializers());
        prog = (Program)prog.accept(new EnqueueToFunction());
        prog = (Program)prog.accept(new InsertIODecls());

        //prog = (Program)prog.accept(new InsertInitConstructors(varGen));
        // separate field initializers after init constructors so that
        // constructor doesn't get generated for fields that already
        // have an initializer
        prog = (Program)prog.accept(new RenameGlobals(libraryFormat));
        prog = (Program)prog.accept(new SeparateFieldInitializers(libraryFormat));

        DetectImmutable immutableDetector = new DetectImmutable(); 
        prog = (Program)prog.accept(immutableDetector);

        // janiss: moved GenerateCopies and InsInitConstructors down
        prog = (Program)prog.accept(new GenerateCopies(varGen,
                                                       libraryFormat,
                                                       immutableDetector)); 
        prog = (Program)prog.accept(new InsertInitConstructors(varGen, 
                                                               libraryFormat,
                                                               immutableDetector)); 
        //        prog = (Program)prog.accept(new InsertInitConstructors2(varGen, 
        //                                                      libraryFormat));

        prog = (Program)prog.accept(new MoveStreamParameters());
        prog = (Program)prog.accept(new NameAnonymousFunctions());
        prog = (Program)prog.accept(new AssembleInitializers());
        prog = (Program)prog.accept(new TrimDumbDeadCode());
        return prog;
    }

    public int run(String[] args)
    {
        doOptions(args);
        if (printHelp)
            {
                printUsage();
                return 0;
            }
        
        Program prog = null;
        Writer outWriter;

        try
            {
                prog = parseFiles(inputFiles);
            }
        catch (java.io.IOException e)
            {
                e.printStackTrace(System.err);
                return 1;
            }
        catch (antlr.RecognitionException e)
            {
                e.printStackTrace(System.err);
                return 1;
            }
        catch (antlr.TokenStreamException e)
            {
                e.printStackTrace(System.err);
                return 1;
            }

        if (prog == null)
            {
                System.err.println("Compilation didn't generate a parse tree.");
                return 1;
            }

        prog = (Program)prog.accept(new StmtSendToHelper());
        prog = (Program)prog.accept(new RenameBitVars());
        if (!SemanticChecker.check(prog))
            return 1;
        prog = (Program)prog.accept(new AssignLoopTypes());
        if (prog == null)
            return 1;

        TempVarGen varGen = new TempVarGen(prog);
        prog = lowerIRToJava(prog, libraryFormat, varGen);

        try
            {
                if (outputFile != null)
                    outWriter = new FileWriter(outputFile);
                else
                    outWriter = new OutputStreamWriter(System.out);
                outWriter.write("import java.io.Serializable;\n");
                outWriter.write("import streamit.library.*;\n");
                outWriter.write("import streamit.library.io.*;\n");

                String javaOut =
                    (String)prog.accept(new NodesToJava(libraryFormat, countops, varGen));
                outWriter.write(javaOut);
                outWriter.flush();
            }
        catch (java.io.IOException e)
            {
                e.printStackTrace(System.err);
                return 1;
            }
        return 0;
    }
    
    public static void main(String[] args)
    {
        try {
            int result = new ToJava().run(args);
            System.exit(result); }
        catch (Throwable e) { 
            e.printStackTrace();
            System.exit(1);
        }
    }
}

