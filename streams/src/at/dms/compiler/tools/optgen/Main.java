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
 * $Id: Main.java,v 1.2 2006-01-25 17:01:19 thies Exp $
 */

package at.dms.compiler.tools.optgen;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Hashtable;

/**
 * This class is the entry point for the Message generator.
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

        boolean     errorsFound = false;

        for (int i = 0; i < options.nonOptions.length; i++) {
            errorsFound = !processFile(options.nonOptions[i]);
        }

        return !errorsFound;
    }

    /*
     * Parse command line arguments.
     */
    private boolean parseArguments(String[] args) {
        options = new OptgenOptions();
        if (!options.parseCommandLine(args)) {
            return false;
        }
        if (options.nonOptions.length == 0) {
            System.err.println(OptgenMessages.NO_INPUT_FILE.getFormat());
            options.usage();
            return false;
        }
        return true;
    }

    private boolean processFile(String sourceFile) {
        if (! parseSource(sourceFile)) {
            return false;
        }
        if (options.release != null) {
            definition.setVersion(options.release);
        }
        if (! checkIdentifiers()) {
            return false;
        }
        if (! checkShortcuts()) {
            return false;
        }
        if (! buildInterfaceFile()) {
            return false;
        }
        return true;
    }

    /**
     *
     */
    private boolean parseSource(String sourceFile) {
        boolean     errorsFound = false;

        try {
            definition = DefinitionFile.read(sourceFile);
        } catch (OptgenError e) {
            System.err.println(e.getMessage());
            errorsFound = true;
        }

        return !errorsFound;
    }


    /**
     *
     */
    private boolean checkIdentifiers() {
        boolean     errorsFound = false;

        try {
            definition.checkIdentifiers();
        } catch (OptgenError e) {
            System.err.println(e.getMessage());
            errorsFound = true;
        }

        return !errorsFound;
    }

    /**
     *
     */
    private boolean checkShortcuts() {
        boolean     errorsFound = false;

        try {
            definition.checkShortcuts();
        } catch (OptgenError e) {
            System.err.println(e.getMessage());
            errorsFound = true;
        }

        return !errorsFound;
    }

    /**
     *
     */
    private boolean buildInterfaceFile() {
        String      prefix = definition.getPrefix();
        File        outputFile = new File(prefix + "Options.java");
        boolean     errorsFound = false;

        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)));

            definition.printFile(out);

            out.flush();
            out.close();
        } catch (java.io.IOException e) {
            System.err.println("I/O Exception on " + outputFile.getPath() + ": " + e.getMessage());
            errorsFound = true;
        }

        return !errorsFound;
    }

    // --------------------------------------------------------------------
    // DATA MEMBERS
    // --------------------------------------------------------------------

    private OptgenOptions       options;
    private DefinitionFile  definition;
}
