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
 * $Id: Main.java,v 1.2 2002-12-18 06:28:50 karczma Exp $
 */

package at.dms.compiler.tools.include;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import at.dms.compiler.tools.common.CompilerMessages;

/**
 * This class is the entry point for the KOPI assembler.
 */
public class Main {

  // --------------------------------------------------------------------
  // CONSTRUCTORS
  // --------------------------------------------------------------------

  /**
   * Only main can construct Main
   */
  private Main(String[] args) {
    if (!parseArguments(args)) {
      System.exit(1);
    }

    String[]	infiles = options.nonOptions;

    if (infiles.length == 0) {
      options.usage();
      System.err.println(IncludeMessages.NO_INPUT_FILE.getFormat());
      System.exit(1);
    } else if (infiles.length > 1) {
      options.usage();
      System.exit(1);
    } else {
      try {
	run();
	System.exit(0);
      } catch (IncludeError e) {
	System.err.println(e.getMessage());
	System.exit(1);
      }
    }
  }

  // --------------------------------------------------------------------
  // IMPLEMENTATION
  // --------------------------------------------------------------------

  /**
   * Runs the process.
   */
  private void run() throws IncludeError {
    BufferedReader	reader;
    PrintWriter		writer;

    /* open input file */
    try {
      reader = new BufferedReader(new FileReader(options.nonOptions[0]));
    } catch (FileNotFoundException e) {
      throw new IncludeError(IncludeMessages.FILE_NOT_FOUND, options.nonOptions[0]);
    }

    /* open output file */
    if (options.output == null) {
      writer = new PrintWriter(System.out);
    } else {
      try {
	writer = new PrintWriter(new BufferedWriter(new FileWriter(options.output)));
      } catch (IOException e) {
	throw new IncludeError(IncludeMessages.CANNOT_OPEN_FILE, options.output, e.getMessage());
      }
    }

    try {
      String		line;

      while ((line = reader.readLine()) != null) {
	if (line.length() > 0 && line.startsWith(options.pattern)) {
	  includeFile(options.directory, line.substring(options.pattern.length()).trim(), writer);
	} else {
	  writer.println(line);
	}
      }
    } catch (IOException e) {
      throw new IncludeError(IncludeMessages.IO_EXCEPTION, options.nonOptions[0], e.getMessage());
    }

    /* close input file */
    try {
      reader.close();
    } catch (IOException e) {
      throw new IncludeError(IncludeMessages.IO_EXCEPTION, options.nonOptions[0], e.getMessage());
    }
    /* close output file */
    writer.flush();
    writer.close();
  }

  /**
   * Copies the content of the specified file in the output.
   * @param	directory	the directory of the file to include
   * @param	name		the name of the file to include
   * @param	writer		the output file writer
   */
  private void includeFile(String directory, String name, PrintWriter writer)
    throws IncludeError
  {
    BufferedReader	reader;

    try {
      reader = new BufferedReader(new FileReader(new File(directory, name)));
    } catch (FileNotFoundException e) {
      throw new IncludeError(IncludeMessages.FILE_NOT_FOUND, name);
    }

    try {
      String		line;

      while ((line = reader.readLine()) != null) {
	writer.println(line);
      }
      reader.close();
    } catch (IOException e) {
      throw new IncludeError(IncludeMessages.IO_EXCEPTION, name, e.getMessage());
    }
  }

  // --------------------------------------------------------------------
  // ENTRY POINT
  // --------------------------------------------------------------------

  /**
   * Entry point to the assembler
   */
  public static void main(String[] args) {
    new Main(args);
  }

  // --------------------------------------------------------------------
  // ACCESSORS
  // --------------------------------------------------------------------

  /*
   * Parse command line arguments.
   */
  private boolean parseArguments(String[] args) {
    options = new IncludeOptions();
    if (!options.parseCommandLine(args)) {
      return false;
    }
    return true;
  }

  // --------------------------------------------------------------------
  // DATA MEMBERS
  // --------------------------------------------------------------------

  private IncludeOptions	options;
}
