/*
 * Copyright (C) 1990-2001 DMS Decision Management Systems Ges.m.b.H.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * $Id: Main.java,v 1.1 2001-08-30 16:32:27 thies Exp $
 */

package at.dms.classfile;

import java.io.*;


/**
 * This class allow to test the classfile reader / writer system by reading / writing
 * every class given as parameters.
 */
public class Main {

  // --------------------------------------------------------------------
  // ENTRY POINT
  // --------------------------------------------------------------------

  /**
   * Entry point to the disassembler
   */
  public static void main(String[] args) {
    if (!parseArguments(args)) {
      System.exit(1);
    }

    String[]	infiles = options.nonOptions;

    if (infiles.length == 0) {
      options.usage();
      System.err.println(ClassfileMessages.NO_INPUT_FILE);
      System.exit(1);
    } else {
      boolean	errorsFound = false;

      for (int i = 0; i < infiles.length; i++) {
	errorsFound |= doFile(new File(infiles[i]));
      }

      if (errorsFound) {
	System.exit(1);
      }
    }
  }

  // --------------------------------------------------------------------
  // ACCESSORS
  // --------------------------------------------------------------------

  /*
   * Parse command line arguments.
   */
  private static boolean parseArguments(String[] args) {
    options = new ClassfileOptions();
    if (!options.parseCommandLine(args)) {
      return false;
    }
    return true;
  }

  /**
   * Read all classfiles
   */
  private static boolean doFile(File f) {
    boolean		errorsFound = false;

    if (f.isDirectory()) {
      // if this is a directory, walk each file/dir in that directory
      String[]		files = f.list();

      for (int i = 0; files != null && i < files.length; i++) {
	errorsFound |= doFile(new File(f, files[i]));
      }
    } else if (f.getName().endsWith(".class")) {
      // otherwise, if this is a class file, parse it!
      try {
	readFile(f.getName(), f);
      } catch (Exception exc) {
	System.err.println("Can't load file " + f);
	errorsFound = true;
      }
    }

    return errorsFound;
  }

  /**
   * Read a classfile
   */
  private static void readFile(String f, File file) throws Exception {
    ClassInfo		classInfo;

    System.err.println("Read " + f + (options.inter ? " interface" : ""));
    // choose your reader:
    FileInputStream s = new FileInputStream(file);
    int		size = s.available();
    if (size > data.length) {
      data = new byte[size];
    } else if (size == 0) {
      s.close();
      return;
    }
    s.read(data);
    s.close();

    for (int i = 0; i < options.repeat; i++) {
      if (i > 0) {
	System.err.println(".");
      }
      InputStream is = new ByteArrayInputStream(data, 0, size);
      classInfo = new ClassInfo(new DataInputStream(is), options.inter);
      is.close();
      if (!options.inter && options.destination != null) {
	classInfo.write(options.destination);
      }
    }
  }

  // --------------------------------------------------------------------
  // DATA MEMBERS
  // --------------------------------------------------------------------

  private static ClassfileOptions	options;
  private static byte[]			data = new byte[100000];
}
