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
 * $Id: Compiler.java,v 1.5 2003-08-29 19:25:33 thies Exp $
 */

package at.dms.compiler;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.StringTokenizer;
import java.util.Vector;

import at.dms.util.Message;
import at.dms.util.MessageDescription;
import at.dms.util.Utils;

/**
 * This class defines the common behaviour of all KOPI compilers.
 */
public abstract class Compiler implements at.dms.kjc.DeepCloneable {

  /**
   * Creates a new compiler instance.
   */
  protected Compiler() {
    timestamp = System.currentTimeMillis();
  }

  /**
   * Sets the directory where to search for source files.
   * @param	dir		the name of the directory
   */
  protected void setWorkingDirectory(String dir) {
    this.workingDirectory = dir;
  }

  /**
   * Returns the timestamp of the compilation session,
   * identifying it uniquely.
   */
  public final long getTimestamp() {
    return timestamp;
  }

  /**
   * Creates an array of files from the specified array of file names.
   *
   * @param	names			an array of file names
   * @return	an array of files
   * @exception	UnpositionedError	at least one file does not exist
   */
  public File[] verifyFiles(String[] names) throws UnpositionedError {
    Vector	temp = new Vector(names.length);

    // replace "@file" by content of "file"
    for (int i = 0; i < names.length; i++) {
      if (! names[i].startsWith("@")) {
	temp.addElement(names[i]);
      } else {
	try {
	  readList(temp, workingDirectory, names[i]);
	} catch (IOException e) {
	  throw new UnpositionedError(CompilerMessages.INVALID_LIST_FILE,
				      names[i], e.getMessage());
	}
      }
    }

    File[]	files = new File[temp.size()];

    for (int i = 0; i < temp.size(); i++) {
      String	name;
      File	file;

      if (workingDirectory == null) {
	name = (String)temp.elementAt(i);
      } else {
	name = workingDirectory + File.separatorChar + (String)temp.elementAt(i);
      }
      file = new File(name);

      if (!file.exists()) {
	file = new File((String)temp.elementAt(i));
	if (!file.exists() || !file.isAbsolute()) {
	  throw new UnpositionedError(CompilerMessages.FILE_NOT_FOUND, temp.elementAt(i), null);
	}
      }
      files[i] = file;
    }

    return files;
  }

  /**
   * Takes a vector of file names an checks that each exists.
   * @param	files			a vector of names
   * @return	a vector of files known to exist
   * @exception	UnpositionedError	at least one file does not exist
   */
  public Vector verifyFiles(Vector names) throws UnpositionedError {
    return Utils.toVector(verifyFiles((String[])Utils.toArray(names, String.class)));
  }

  private void readList(Vector list, String workingDirectory, String name) throws IOException {
    File		file = new File((workingDirectory == null ? "" : workingDirectory + File.separatorChar) + name.substring(1));
    LineNumberReader	reader = new LineNumberReader(new FileReader(file));
    String		line;

    while ((line = reader.readLine()) != null) {
      StringTokenizer	tok = new StringTokenizer(line);

      while (tok.hasMoreTokens()) {
	list.addElement(tok.nextToken());
      }
    }
  }

  /**
   * Checks if destination is absolute or relative to working directory.
   */
  protected String checkDestination(String destination) {
    if (workingDirectory == null) {
      return destination;
    } else if (destination == null || destination.equals("")) {
      return workingDirectory;
    } else if (new File(destination).isAbsolute()) {
      return destination;
    } else {
      return workingDirectory + File.separatorChar + destination;
    }
  }

  /**
   * Runs a compilation session
   *
   * @param	dir		the working directory
   * @param	err		the diagnostic output stream
   * @param	args		the arguments to the compiler
   * @return	true iff the compilation succeeded
   */
  public boolean run(String dir, PrintWriter err, String[] args) {
    this.workingDirectory = dir;
    this.err = err;

    return run(args);
  }

  // --------------------------------------------------------------------
  // METHODS TO BE IMPLEMENTED BY SUBCLASSES
  // --------------------------------------------------------------------

  /**
   * Runs a compilation session
   *
   * @param	args		the arguments to the compiler
   * @return	true iff the compilation succeeded
   */
  public abstract boolean run(String[] args);

  /**
   * Reports a trouble (error or warning).
   *
   * @param	trouble		a description of the trouble to report.
   */
  public abstract void reportTrouble(PositionedError trouble);

  /**
   * Returns true iff comments should be parsed (false if to be skipped).
   */
  public abstract boolean parseComments();

  /**
   * Returns true iff compilation runs in verbose mode.
   */
  public abstract boolean verboseMode();

  // --------------------------------------------------------------------
  // DIAGNOSTIC OUTPUT
  // --------------------------------------------------------------------

  /**
   * Write a message to the diagnostic output.
   * @param	message		the formatted message
   */
  public void inform(UnpositionedError error) {
    inform(error.getMessage());
  }

  /**
   * Write a message to the diagnostic output.
   * @param	message		the formatted message
   */
  public void inform(PositionedError error) {
    inform(error.getMessage());
  }

  /**
   * Write a message to the diagnostic output.
   * @param	message		the formatted message
   */
  public void inform(Message message) {
    inform(message.getMessage());
  }

  /**
   * Write a message to the diagnostic output.
   * @param	description	the message description
   * @param	parameters	the array of parameters
   */
  public void inform(MessageDescription description, Object[] parameters) {
    inform(new Message(description, parameters));
  }

  /**
   * Write a message to the diagnostic output.
   * @param	description	the message description
   * @param	parameter1	the first parameter
   * @param	parameter2	the second parameter
   */
  public void inform(MessageDescription description, Object parameter1, Object parameter2) {
    inform(description, new Object[] { parameter1, parameter2 });
  }

  /**
   * Write a message to the diagnostic output.
   * @param	description	the message description
   * @param	parameter	the parameter
   */
  public void inform(MessageDescription description, Object parameter) {
    inform(description, new Object[] { parameter });
  }

  /**
   * Write a message to the diagnostic output.
   * @param	description	the message description
   */
  public void inform(MessageDescription description) {
    inform(description, null);
  }

  /**
   * Write a text to the diagnostic output.
   * @param	message		the message text
   */
  private void inform(String message) {
    err.println(message);
    err.flush();
  }

  // --------------------------------------------------------------------
  // DATA MEMBERS
  // --------------------------------------------------------------------

  // each compilation session is identified by a timestamp
    private /* final*/ long		timestamp; // removed final for cloner

  // the output stream for diagnostic messages
  private PrintWriter		err = new PrintWriter(System.err);

  // the directory where to search source files
  private String		workingDirectory;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() { at.dms.util.Utils.fail("Error in auto-generated cloning methods - deepClone was called on an abstract class."); return null; }

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.compiler.Compiler other) {
  other.timestamp = this.timestamp;
  other.err = (java.io.PrintWriter)at.dms.kjc.AutoCloner.cloneToplevel(this.err, other);
  other.workingDirectory = (java.lang.String)at.dms.kjc.AutoCloner.cloneToplevel(this.workingDirectory, other);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}
