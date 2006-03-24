/*
 * Copyright (C) 1990-2001 DMS Decision Management Systems Ges.m.b.H.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * $Id: Main.java,v 1.4 2006-03-24 20:48:35 dimock Exp $
 */

package at.dms.compiler.tools.antlr.compiler;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;

import at.dms.compiler.tools.antlr.runtime.FileLineFormatter;
import at.dms.compiler.tools.antlr.runtime.RecognitionException;
import at.dms.compiler.tools.antlr.runtime.TokenBuffer;
import at.dms.compiler.tools.antlr.runtime.TokenStreamException;
import at.dms.compiler.tools.antlr.runtime.Vector;
import at.dms.compiler.tools.common.Message;
import at.dms.compiler.tools.common.MessageDescription;

public class Main {

    /**
     * Entry point
     *
     * @param   args        the command line arguments
     */
    public static void main(String[] args) {
        boolean success;

        try {
            success = compile(args);
        } catch (Throwable e) {
            System.err.println("Internal error: " + e.toString());
            System.err.println("Please send a bug report to kopi@dms.at");
            System.err.println("including the following stack trace.");
            System.err.println();
            e.printStackTrace();

            success = false;
        }

        System.exit(success ? 0 : 1);
    }

    /**
     * Second entry point
     */
    public static boolean compile(String[] args) {
        return new Main().run(args);
    }

    /**
     * Runs a compilation session
     *
     * @param   args        the command line arguments
     */
    public boolean run(String[] args) {
        if (!parseArguments(args)) {
            return false;
        }

        if (!preprocess()) {
            return false;
        }

        if (!process()) {
            return false;
        }

        return true;
    }

    /**
     * Parse the argument list
     */
    private boolean parseArguments(String[] args) {
        options = new AntlrOptions();
        if (!options.parseCommandLine(args)) {
            return false;
        }
        if (options.nonOptions.length == 0) {
            options.usage();
            inform(AntlrMessages.NO_INPUT_FILE);
            return false;
        }
        return true;
    }

    // --------------------------------------------------------------------
    // PREPROCESSING
    // --------------------------------------------------------------------

    public boolean preprocess() {
        Hierarchy       theHierarchy;
        String[]        infiles;

        theHierarchy = new Hierarchy();

        infiles = options.nonOptions;
        for (int i = 0; i < infiles.length; i++) {
            try {
                theHierarchy.readGrammarFile(infiles[i]);
            } catch (FileNotFoundException fe) {
                Utils.toolError("file " + infiles[i] + " not found");
                return false;
            }
        }
        // do the actual inheritance stuff
        boolean complete = theHierarchy.verifyThatHierarchyIsComplete();
        if (!complete) {
            return false;
        }

        grammarFile = infiles[infiles.length - 1];

        theHierarchy.expandGrammarsInFile(grammarFile);

        GrammarFile     gf = theHierarchy.getFile(grammarFile);
        String      expandedFileName = gf.nameForExpandedGrammarFile(grammarFile);

        // generate the output file if necessary
        if (! expandedFileName.equals(grammarFile)) {
            try {
                gf.generateExpandedFile(this);              // generate file to feed ANTLR
                grammarFile = options.destination + System.getProperty("file.separator") + expandedFileName;
            } catch (IOException io) {
                Utils.toolError("cannot write expanded grammar file " + expandedFileName);
                return false;
            }
        }
        return true;
    }

    // --------------------------------------------------------------------
    // MAIN PROCESSING
    // --------------------------------------------------------------------

    public static final String version = "1.5A";

    /**
     * Was there an error during parsing or analysis?
     */
    protected boolean hasError = false;

    // SAS: changed for proper text io
    //  transient DataInputStream in = null;

    protected static String literalsPrefix = "LITERAL_";
    protected static boolean upperCaseMangledLiterals = false;

    /**
     * Perform processing on the grammar file.
     * implicit parameter?? The command-line arguments
     */
    public boolean process() {
        Reader f;

        f = getGrammarReader();

        TokenBuffer tokenBuf = new TokenBuffer(new ANTLRLexer(f));
        LLkAnalyzer analyzer = new LLkAnalyzer(this);
        MakeGrammar behavior = new MakeGrammar(this, analyzer);

        try {
            ANTLRParser p = new ANTLRParser(tokenBuf, behavior, this);
            p.setFilename(grammarFile);
            p.grammar();
            if (hasError) {
                System.err.println("Exiting due to errors.");
                return false;
            }

            JavaCodeGenerator codeGen = new JavaCodeGenerator();
      
            codeGen.setBehavior(behavior);
            codeGen.setAnalyzer(analyzer);
            codeGen.setTool(this);
            codeGen.gen();
        } catch (RecognitionException pe) {
            System.err.println("Unhandled parser error: " + pe.getMessage());
            return false;
        } catch (TokenStreamException io) {
            System.err.println("TokenStreamException: " + io.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Issue an error
     * @param s The message
     */
    public void error(String s) {
        hasError = true;
        System.err.println("error: " + s);
    }

    /**
     * Issue an error with line number information
     * @param s The message
     * @param file The file that has the error
     * @param line The grammar file line number on which the error occured
     */
    public void error(String s, String file, int line) {
        hasError = true;
        if ( file!=null ) {
            System.err.println(FileLineFormatter.getFormatter().getFormatString(file,line)+s);
        } else {
            System.err.println("line "+line+": "+s);
        }
    }

    public String getOutputDirectory() {
        return options.destination;
    }

    public PrintWriter openOutputFile(String f) throws IOException {
        return new PrintWriter(new FileWriter(options.destination + System.getProperty("file.separator") + f));
    }

    public Reader getGrammarReader() {
        try {
            return new FileReader(grammarFile);
        } catch (IOException e) {
            Utils.panic("Error: cannot open grammar file " + grammarFile);
            System.exit(1);
            return null;    // not reached
        }
    }

    // --------------------------------------------------------------------
    // DIAGNOSTICS
    // --------------------------------------------------------------------

    /**
     * Write a message to the diagnostic output.
     * @param   message     the formatted message
     */
    public void inform(Message message) {
        inform(message.getMessage());
    }

    /**
     * Write a message to the diagnostic output.
     * @param   description the message description
     * @param   parameters  the array of parameters
     */
    public void inform(MessageDescription description, Object[] parameters) {
        inform(new Message(description, parameters));
    }

    /**
     * Write a message to the diagnostic output.
     * @param   description the message description
     */
    public void inform(MessageDescription description) {
        inform(description, null);
    }

    /**
     * Write a text to the diagnostic output.
     * @param   message     the message text
     */
    private void inform(String message) {
        System.err.println(message);
        System.err.flush();
    }

    // ----------------------------------------------------------------------
    // DATA MEMBERS
    // ----------------------------------------------------------------------

    private AntlrOptions        options;
    /*!!!*/ String      grammarFile;
}
