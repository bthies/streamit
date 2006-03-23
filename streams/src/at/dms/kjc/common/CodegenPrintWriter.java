package at.dms.kjc.common;

import java.io.Writer;
import java.io.StringWriter;

/**
 * A PrintWriter incorporating common functions called from our code generators
 *
 * Delegates to at.dms.compiler.TabbedPrintWriter, which in turn delegates to
 * java.io.PrintWriter.
 * <br/>
 * Includes print and println at the base types commonly found in the compiler
 * and for String.  Printing of Object (as its toString()) is deliberately 
 * missing from this implementation since it was causing bugs in the printing 
 * of CType's.
 * To print a CType see {@link at.dms.kjc.common.ToCCommon#printType}.
 * <br/>
 * Includes an optional debugging feature to dump the caller's line number and 
 * test to be printed on a print, println, or printnl.
 *
 * @author Allyn Dimock
 */

public final class CodegenPrintWriter {
    // Constants
    private static final int DEFAULT_TAB_SIZE = 2;

    // Unused: TabbedPrintWriter hard-wires width.
    //    private static final int DEFAULT_WIDTH = 80;

    // Data
    private int tabSize = DEFAULT_TAB_SIZE;

    /**
     * Static field to set to get debug dumps from all
     * instantiations.
     */
    public static boolean defaultDebug = false;

    /**
     * How many stack frames to go up to print caller information.
     * 
     * This number is dependent on the internal coding of print routines!
     * The default value should tell you where a print routine was called
     * Setting the value larger may tell you what was being visited when the
     * print routine was called.
     */
    public static int defaultDebugDepth = 2;

    //  Unused: TabbedPrintWriter hard-wires width.
    //    private int width = DEFAULT_WIDTH;
    private at.dms.compiler.TabbedPrintWriter p;

    // can be set per instance after breakpoint at constructor
    private boolean debug = defaultDebug;

    // can be set per instance after breakpoint at constructor
    private int debugDepth = defaultDebugDepth;

    private StringWriter str = null;

    // Constructors

    private void moreSetup(Writer w) {
        p = new at.dms.compiler.TabbedPrintWriter(w);
        //      debug = defaultDebug;
        //      debugDepth = defaultDebugDepth;
    }

    /**
     * Construct with a Writer
     * 
     * @param w  A Writer that is already opened.
     *
     */

    public CodegenPrintWriter(Writer w) {
        if (w == null) {
            throw new java.lang.IllegalArgumentException("null input");
        }
        moreSetup(w);
    }

    /**
     * Construct with a StringWriter
     * 
     * The contents of the string writer can be accessed with 
     * {@link #getString()}
     *
     */

    public CodegenPrintWriter() {
        str = new java.io.StringWriter();
        moreSetup(str);
    }

    // Accessors
    /**
     * Get line number.
     * @return current line number.
     */
    public int getLine() {
        return p.getLine();
    }

    /**
     * Get current column.
     * @return current column number.
     */
    public int getColumn() {
        return p.getColumn();
    }

    /**
     * Get current amount of intentation.
     * @return number of spaces currently used for indentation
     */
    public int getIndentation() {
        return p.getPos();
    }

    /**
     * Set current amount of indentation.
     * You almost never want to use this, use {@link #indent()} and
     * {@link #outdent()} instead.
     * 
     * @param pos number of spaces to use for indentation
     */
    public void setIndentation(int pos) {
        p.setPos(pos);
    }

    /**
     * Set tab size to some number of spaces (if you don't like the default).
     * @param tabSize number of spaces added per {@link #indent()} and removed 
     *                per {@link #outdent()}
     */
    public void setTabSize(int tabSize) {
        this.tabSize = tabSize;
    }

    //  Unused: TabbedPrintWriter hard-wires width.
    //    public void setWidth(int width)     { this.width = width; }

    /**
     * Get string if you used constructor {@link #CodegenPrintWriter()}.
     * 
     * @return  string from string writer or null
     */
    public String getString() {
        if (str != null)
            return str.toString();
        else
            return null;
    }

    // debugging
    private final Throwable tracer = new Throwable();

    private String findCaller() {
        if (debugDepth < 0) {
            throw new IllegalArgumentException();
        }
        tracer.fillInStackTrace();
        return tracer.getStackTrace()[debugDepth + 1].toString();
    }

    private void debugPrint(String s) {
        System.err.print(findCaller() + ":  ");
        System.err.println(s);
    }

    // Printing

    /**
     * Increment indentation level.
     */
    public void indent() {
        p.add(tabSize);
    }

    /**
     * Decrement indentation level.
     */
    public void outdent() {
        p.sub(tabSize);
    }

    /**
     * Close underlying Writer, 
     * (and StringWriter if used {@link #CodegenPrintWriter()}.
     */
    public void close() {
        p.close();
        str = null;
    }

    /**
     * Start a new line.
     */
    public void newLine() {
        p.println();
    }

    /**
     * Synonym for {@link #newLine()}.
     */
    public void newline() {
        p.println();
    }

    /**
     * Print something in the current line.
     * 
     * @param s String to print.
     * 
     * Also overloaded for base types boolean, int, char, float, double. 
     */
    public void print(String s) {
        if (debug) {
            debugPrint(s);
        }
        p.print(s);
    }

    public void print(boolean b) {
        String s = "" + b;
        if (debug) {
            debugPrint(s);
        }
        p.print(s);
    }

    public void print(int i) {
        String s = "" + i;
        if (debug) {
            debugPrint(s);
        }
        p.print(s);
    }

    public void print(char c) {
        String s = "" + c;
        if (debug) {
            debugPrint(s);
        }
        p.print(s);
    }

    public void print(float f) {
        String s = "" + f;
        if (debug) {
            debugPrint(s);
        }
        p.print(s);
    }

    public void print(double d) {
        String s = "" + d;
        if (debug) {
            debugPrint(s);
        }
        p.print(s);
    }

    /**
     * Print something in the current line and terminate the line.
     *
     * @param s String to print.
     * 
     * Also overloaded at base types boolean, int, char, float, double. 
     * println() acts identically to {@link #newLine()}.
     */
    // println duplicates print functionality so that it doesn't have to
    // manipulate debugDepth, which is presumably infrequently used.
    public void println(String s) {
        if (debug) {
            debugPrint(s);
        }
        p.print(s);
        p.println();
    }

    public void println() { newLine(); }
    
    public void println(boolean b) {
        String s = "" + b;
        if (debug) {
            debugPrint(s);
        }
        p.print(s);
        p.println();
    }

    public void println(int i) {
        String s = "" + i;
        if (debug) {
            debugPrint(s);
        }
        p.print(s);
        p.println();
    }

    public void println(char c) {
        String s = "" + c;
        if (debug) {
            debugPrint(s);
        }
        p.print(s);
        p.println();
    }

    public void println(float f) {
        String s = "" + f;
        if (debug) {
            debugPrint(s);
        }
        p.print(s);
        p.println();
    }

    public void println(double d) {
        String s = "" + d;
        if (debug) {
            debugPrint(s);
        }
        p.print(s);
        p.println();
    }

    /**
     * Print a string, possibly containing \n characters.
     * 
     * Prints the string, breaking at \n characters and starting new
     * lines as needed at the current indentation level.  Local
     * indentation, in the forms of spaces at the beginning of lines
     * will be printed.
     * 
     * @param s  String to print
     */
    public void printSeveralLines(String s) {
        if (debug) {
            debugPrint(s);
        }
        int fromIndex = 0;
        for (int toIndex = s.indexOf("\n"); toIndex >= 0; 
             toIndex = s.indexOf("\n", fromIndex)) {
            String sSub = s.substring(fromIndex, toIndex);
            p.print(sSub);
            p.println();
            fromIndex = toIndex + 1;
        }
        p.print(s.substring(fromIndex));
    }
}
