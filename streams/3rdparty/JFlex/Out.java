/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * JFlex 1.3.2                                                             *
 * Copyright (C) 1998-2001  Gerwin Klein <lsf@jflex.de>                    *
 * All rights reserved.                                                    *
 *                                                                         *
 * This program is free software; you can redistribute it and/or modify    *
 * it under the terms of the GNU General Public License. See the file      *
 * COPYRIGHT for more information.                                         *
 *                                                                         *
 * This program is distributed in the hope that it will be useful,         *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of          *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the           *
 * GNU General Public License for more details.                            *
 *                                                                         *
 * You should have received a copy of the GNU General Public License along *
 * with this program; if not, write to the Free Software Foundation, Inc., *
 * 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA                 *
 *                                                                         *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

package JFlex;


import java.io.*;
import java.awt.TextArea;


/**
 * In this class all output to the java console is filtered.
 *
 * Use the switches VERBOSE, TIME and DUMP at compile time to determine
 * the verbosity of JFlex output. There is no switch for
 * suppressing error messages. VERBOSE and TIME can be overridden 
 * by command line paramters.
 *
 * Redirects output to a TextArea in GUI mode.
 *
 * Counts error and warning messages.
 *
 * <UL>
 * <LI>
 * VERBOSE should be switched on in all normal cases. 
 *         It is used for standard progress messages to the user.
 * </LI><LI>
 * TIME    can be set to true for performance measurements if 
 *         time statistics for the different stages of generation
 *         are to be printed.
 * </LI><LI>       
 * DUMP    this one gives you all the information you want (or not).
 *         BUT: prepare to wait.
 *         If only dump-information from specific classes is
 *         needed, compile all classes with DUMP=false first,
 *         then recompile this class and all the classes that
 *         are to dump their information with DUMP=true.
 * </LI>
 * </UL>
 *
 * @author Gerwin Klein
 * @version JFlex 1.3.2, $Revision: 1.1 $, $Date: 2001-08-30 16:25:34 $
 */
public final class Out implements ErrorMessages {

  public static final String NL = System.getProperty("line.separator");
  
  /**
   * If VERBOSE is false, no progress output will be generated
   */
  public static boolean VERBOSE     = true;

  /**
   * If TIME is true, jflex will print time statistics about the generation process
   */
  public static boolean TIME        = false;

  /**
   * If DUMP is true, you will be flooded with information (e.g. dfa tables).
   */
  public static boolean DUMP        = false;

  /**
   * If DOT is true, jflex will write graphviz .dot files for generated automata
   */
  public static boolean DOT         = false;
    
  /**
   * If DEBUG is true, additional verbose debug information is produced
   */
  public final static boolean DEBUG = false;

  public static int warnings;
  public static int errors;

  private static TextArea text;
  private static int col;

  public static void setGUIMode(TextArea text) {
    Out.text = text;
  }

  /**
   * All parts of JFlex, that want to report something about 
   * time statistic should use this method for their output.
   *
   * @message  the message to be printed
   */
  public static void time(String message) {
    if (!TIME) return;

    if (text == null)
      System.out.println(message);
    else {
      text.append(message);
      text.append(NL);
      col = 0;
    }
  }
  

  /**
   * All parts of JFlex, that want to report generation progress
   * should use this method for their output.
   *
   * @message  the message to be printed
   */
  public static void println(String message) {
    if (!VERBOSE) return;
    
    if (text == null)
      System.out.println(message);
    else {
      text.append(message);
      text.append(NL);
      col = 0;
    }
  }


  /**
   * All parts of JFlex, that want to report generation progress
   * should use this method for their output.
   *
   * @message  the message to be printed
   */
  public static void print(String message) {
    if (!VERBOSE) return;
    
    if (text == null)
      System.out.print(message);
    else {
      text.append(message);     
      col+= message.length();
      if (col >= 78) {
        text.append(NL);
        col = 0;
      }
    }
  }

  /**
   * Dump debug information to System.out
   *
   * Use like this 
   *
   * <code>if (Out.DEBUG) Out.debug(message)</code>
   *
   * to save performance during normal operation (when DEBUG
   * is turned off).
   */
  public static void debug(String message) {
    if (DEBUG) System.out.println(message); 
  }


  /**
   * All parts of JFlex, that want to provide dump information
   * should use this method for their output.
   *
   * @message the message to be printed 
   */
  public static void dump(String message) {
    if (DUMP) println(message);
  }

  
  /**
   * All parts of JFlex, that want to report error messages
   * should use this method for their output.
   *
   * @message  the message to be printed
   */
  private static void err(String message) {
    if (text == null)
      System.out.println(message);
    else {
      text.append(message);
      text.append(NL);
      col = 0;
    }
  }
  
  public static void checkErrors() {
    if (errors > 0) throw new GeneratorException();
  }
  
  public static void statistics() {    
    StringBuffer line = new StringBuffer(errors+" error");
    if (errors != 1) line.append("s");

    line.append(", "+warnings+" warning");
    if (warnings != 1) line.append("s");

    line.append(".");
    err(line.toString());
  }

  public static void resetCounters() {
    errors = 0;
    warnings = 0;
  }

  public static void warning(String message) {
    warnings++;

    err(NL+"Warning : "+message);
  }

  public static void warning(int message, int line) {
    warnings++;

    String msg = NL+"Warning";
    if (line > 0) msg = msg+" in line "+(line+1);

    err(msg+": "+messages[message]);
  }
  
  public static void error(String message) {
    errors++;
    err(NL+message);
  }

  public static void error(int message) {
    errors++;
    err(NL+"Error: "+messages[message] );
  }

  /**
   * IO error message for a file (displays file 
   * name in parentheses).
   */
  public static void error(int message, File file) {
    errors++;
    err(NL+"Error: "+messages[message]+" ("+file+")");
  }

  public static void error(File file, int message, int line, int column) {

    String msg = NL+"Error";
    if (file != null) msg += " in file \""+file+"\"";
    if (line >= 0) msg = msg+" (line "+(line+1)+")";

    try {
      err(msg+": "+NL+messages[message]);
    }
    catch (ArrayIndexOutOfBoundsException e) {
      err(msg);
    }

    if (line >= 0) {
      if (column >= 0)
        showError(file, line, column);
      else 
        showError(file, line);
    }
    else 
      errors++;
  }


  public static void showError(File file, int line, int column) {
    errors++;

    try {
      String ln = getLine(file, line);
      if (ln != null) {
        err( ln );

        if (column < 0) return;
        
        String t = "^";  
        for (int i = 0; i < column; i++) t = " "+t;  
        
        err(t);
      }
    }
    catch (IOException e) {
      /* silently ignore IO errors, don't show anything */
    }
  }


  public static void showError(File file, int line) {
    errors++;

    try {
      String ln = getLine(file, line);
      if (ln != null) err(ln);
    }
    catch (IOException e) {
      /* silently ignore IO errors, don't show anything */
    }
  }


  private static String getLine(File file, int line) throws IOException {
    BufferedReader reader = new BufferedReader(new FileReader(file));

    String msg = "";

    for (int i = 0; i <= line; i++)
      msg = reader.readLine();

    reader.close();
    
    return msg;
  }
}
