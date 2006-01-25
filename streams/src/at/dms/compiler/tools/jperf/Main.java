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
 * $Id: Main.java,v 1.3 2006-01-25 17:01:10 thies Exp $
 */

package at.dms.compiler.tools.jperf;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Vector;

import at.dms.compiler.tools.common.Utils;

/**
 * Main is the main driver of the package.
 * @author Jiejun KONG
 */

public class Main {

    // --------------------------------------------------------------------
    // INPUT HANDLING
    // --------------------------------------------------------------------

    /**
     * read and skip the declaration part
     * Assume that file is already open and
     * file pointer is already set correctly
     */
    private static String[] readHeader(RandomAccessFile input) throws java.io.IOException {
        Vector  lines = new Vector();

        // read until meet /^%%/
        while (true) {
            String  currentLine = null;

            // have met End Of File
            // According to source code of java.io.RandomAccessFile
            // a `null' return value means EOF
            if ((currentLine = input.readLine()) == null) {
                System.err.println("Unexpected EOF in declaration part: incomplete input file.");
                System.exit(-1);
            }

            if (currentLine.startsWith("%%")) {
                break;
            }

            lines.addElement(currentLine);
        }

        return (String[])Utils.toArray(lines, String.class);
    }

    /**
     * Reads keyword section
     * Assume that file is already open and
     * file pointer is already set correctly
     */
    private static String[] readKeywords(RandomAccessFile input) {
        Vector  lines = new Vector();

        while (true) {
            String  currentLine = null;

            try {
                // have met End Of File
                // According to source code of java.io.RandomAccessFile
                // a `null' return value means EOF
                if ((currentLine = input.readLine()) == null) {
                    break;
                }
            } catch (IOException e) {
                System.err.println("Error in reading file.");
                System.exit(-1);
            }

            if ((currentLine.charAt(0) == '%') && (currentLine.charAt(1) == '%')) {
                break;
            }

            // "//" leads comments
            if (currentLine.charAt(0) != '/' || currentLine.charAt(1) != '/') {
                lines.addElement(currentLine);
            }
        }

        return (String[])Utils.toArray(lines, String.class);
    }

    /**
     * Reads unction section
     * Assume that file is already open and
     * file pointer is already set correctly
     */
    private static String[] readFooter(RandomAccessFile input) throws java.io.IOException {
        Vector  lines = new Vector();

        while (true) {
            String  currentLine = null;

            if ((currentLine = input.readLine()) == null) {
                break;
            }

            lines.addElement(currentLine);
        }

        return (String[])Utils.toArray(lines, String.class);
    }

    // --------------------------------------------------------------------
    // MAIN
    // --------------------------------------------------------------------

    /**
     * The main driver.
     */
    public static void main(String[] args) throws java.io.IOException {
        parseOptions(args);

        RandomAccessFile    input = new RandomAccessFile(input_file_name, "r");

        String[]        header = readHeader(input);
        String[]        keywords = readKeywords(input);
        String[]        footer = readFooter(input);

        input.close();

        JPerf   generator = new JPerf(keywords, header, footer, loadFactor);

        generator.build();
        generator.genCode(output_file_name);
    }

    // process options from command line or AWT interface
    private static void parseOptions(String[] args) {
        for (int i=0; i<args.length; i++) {
            if (args[i].charAt(0) == '-') {
                if (args[i].startsWith("-C=")) {
                    loadFactor = new Double(args[i].substring(3)).doubleValue();
                } else {
                    usage(args);
                }
            } else {
                if (input_file_name == null) {
                    input_file_name = args[i];
                } else if (output_file_name == null) {
                    output_file_name = args[i];
                    if (output_file_name.indexOf('.') == -1) {
                        output_file_name = output_file_name.concat(".java");
                        System.err.println("Extension name of output file defaults to `java'.");
                    }
                } else {
                    usage(args);
                }
            }
        }

        if (input_file_name == null || output_file_name == null) {
            usage(args);
        }
    }

    // print usage information
    private static void usage(String[] args) {
        System.err.println("java PerfectHash <options> input_file_name output_file_name");
        System.err.println("where options are:");
        System.err.println("\t-C=<float>\t\tset load factor value to <float>");
        System.exit(-1);
    }

    // --------------------------------------------------------------------
    // DATA MEMBERS
    // --------------------------------------------------------------------

    // command line parameters
    private static double       loadFactor = 25.0/12.0;
    private static String       input_file_name = null;
    private static String       output_file_name = null;
}
