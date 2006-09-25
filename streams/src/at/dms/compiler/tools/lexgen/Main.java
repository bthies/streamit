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
 * $Id: Main.java,v 1.4 2006-09-25 13:54:32 dimock Exp $
 */

package at.dms.compiler.tools.lexgen;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.Vector;

import at.dms.compiler.tools.jperf.JPerf;
import at.dms.compiler.tools.common.Utils;

/**
 * This class is the entry point for token file generator.
 */
public class Main {

    // --------------------------------------------------------------------
    // ENTRY POINT
    // --------------------------------------------------------------------

    /**
     * Program entry point.
     */
    public static void main(String[] args) {
        boolean success;

        success = new Main().run(args);

        System.exit(success ? 0 : 1);
    }

    // --------------------------------------------------------------------
    // CONSTRUCTORS
    // --------------------------------------------------------------------

    /**
     * Only main can construct Main.
     */
    private Main() {
    }

    /**
     * Runs a compilation session.
     *
     * @param   args        the command line arguments
     */
    private boolean run(String[] args) {
        if (!parseArguments(args)) {
            return false;
        }

        if (!parseSourceFiles(options.nonOptions)) {
            return false;
        }
        if (!checkIdentifiers()) {
            return false;
        }
        if (options.definition) {
            if (!buildDefinitionFile()) {
                return false;
            }
        }
        if (options.inter) {
            if (!buildInterfaceFile()) {
                return false;
            }
        }
        if (options.keywords) {
            if (!buildKeywordFile()) {
                return false;
            }
        }
        if (options.flexrules) {
            if (!buildFlexRulesFile()) {
                return false;
            }
        }
        return true;
    }

    /*
     * Parse command line arguments.
     */
    private boolean parseArguments(String[] args) {
        options = new LexgenOptions();

        if (! options.parseCommandLine(args)) {
            return false;
        }
        if (options.nonOptions.length == 0) {
            System.err.println(LexgenMessages.NO_INPUT_FILE.getFormat());
            options.usage();
            return false;
        }
        if (! options.definition && !options.inter && !options.keywords) {
            // nothing to do
            options.usage();
            return false;
        }
        if (options.tokens) {
            // generate token in interface file => generate interface file
            options.inter = true;
        }

        return true;
    }

    /**
     *
     */
    private boolean parseSourceFiles(String[] infiles) {
        boolean     errorsFound = false;

        definitions = new DefinitionFile[infiles.length];
        for (int i = 0; i < infiles.length; i++) {
            try {
                definitions[i] = DefinitionFile.read(infiles[i]);
            } catch (LexgenError e) {
                System.err.println(e.getMessage());
                errorsFound = true;
            }
        }

        return !errorsFound;
    }


    /**
     *
     */
    private boolean checkIdentifiers() {
        Hashtable<String, String>       identifiers = new Hashtable<String, String>();
        boolean     errorsFound = false;
        String      prefix = definitions[definitions.length - 1].getPrefix();
        int         cnt = MIN_TOKEN_ID;

        for (int i = 0; i < definitions.length; i++) {
            try {
                cnt = definitions[i].checkIdentifiers(identifiers, prefix, cnt);
            } catch (LexgenError e) {
                System.err.println(e.getMessage());
                errorsFound = true;
            }
        }

        return !errorsFound;
    }

    /**
     *
     */
    private boolean buildDefinitionFile() {
        String      vocabulary = definitions[definitions.length - 1].getVocabulary();
        String      prefix = definitions[definitions.length - 1].getPrefix();
        File        outputFile = new File(vocabulary + "TokenTypes.txt");

        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)));

            // !!! print file header

            // print vocabulary name
            out.println(vocabulary + " // output vocabulary");

            for (int i = 0; i < definitions.length; i++) {
                definitions[i].printDefinition(out, prefix);
            }

            out.flush();
            out.close();

            return true;
        } catch (IOException e) {
            System.err.println("I/O Exception on " + outputFile.getPath() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     *
     */
    private boolean buildInterfaceFile() {
        DefinitionFile  definition = definitions[definitions.length - 1];
        String      vocabulary = definition.getVocabulary();
        File        outputFile = new File(vocabulary + "TokenTypes.java");

        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)));

            // !!! print file header
            definition.printInterface(out,
                                      definitions.length == 1
                                      ? null
                                      : definitions[definitions.length - 2].getClassName(),
                                      options.tokens);

            out.flush();
            out.close();
            return true;
        } catch (IOException e) {
            System.err.println("I/O Exception on " + outputFile.getPath() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     *
     */
    private boolean buildFlexRulesFile() {
        DefinitionFile  definition = definitions[definitions.length - 1];
        String      vocabulary = definition.getVocabulary();
        File        outputFile = new File(vocabulary + "FlexRules.txt");

        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)));

            for (int i = 0; i < definitions.length; i++) {
                definitions[i].printFlexRules(out);
            }
            out.flush();
            out.close();
            return true;
        } catch (IOException e) {
            System.err.println("I/O Exception on " + outputFile.getPath() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     *
     */
    private boolean buildKeywordFile() {
        String      vocabulary = definitions[definitions.length - 1].getVocabulary();
        String      prefix = definitions[definitions.length - 1].getPrefix();
        String      packageName = definitions[definitions.length - 1].getPackageName();

        Vector      keywords = new Vector();
        Vector      types = new Vector();
        Vector      header = new Vector();
        Vector      footer = new Vector();

        // retrieve keywords to hash
        for (int i = 0; i < definitions.length; i++) {
            definitions[i].putKeywords(keywords, types, prefix);
        }

        // header
        header.addElement("// Generated by JPerf");
        header.addElement("package " + packageName + ";");
        header.addElement("import at.dms.compiler.antlr.extra.CToken;\n");
        header.addElement("/*package*/ final class " + vocabulary + "Keywords implements " + vocabulary + "TokenTypes {");
        header.addElement("  public static CToken lookup(final char[] data, int offset, int length) {");
        header.addElement("    return tokens[find(data, offset, length) + 1];");
        header.addElement("  }");
        header.addElement("  private static final CToken[] tokens = new CToken[] {\n    null,");
        for (int i = 0; i < types.size(); i++) {
            String  line = "    new CToken(" + (String)types.elementAt(i) + ", \"" + (String)keywords.elementAt(i) + "\")";

            if (i < types.size() - 1) {
                header.addElement(line + ",");
            } else {
                header.addElement(line);
            }
        }
        header.addElement("  };");

        // footer
        footer.addElement("}");

        // generate perfect hash function
        try {
            JPerf       perf;

            perf = new JPerf((String[])Utils.toArray(keywords, String.class),
                             (String[])Utils.toArray(header, String.class),
                             (String[])Utils.toArray(footer, String.class));
            perf.build();
            perf.genCode(vocabulary + "Keywords.java");
            return true;
        } catch (IOException e) {
            System.err.println("I/O Exception on " + vocabulary + "Keywords.java" + ": " + e.getMessage());
            return false;
        }
    }

    // --------------------------------------------------------------------
    // DATA MEMBERS
    // --------------------------------------------------------------------

    private LexgenOptions       options;
    private DefinitionFile[]    definitions;

    private static final int    MIN_TOKEN_ID = 4;
}
