/*
 * FEContext.java: explain where a front-end node came from
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: FEContext.java,v 1.2 2003-07-07 21:21:06 dmaze Exp $
 */

package streamit.frontend.nodes;

/**
 * A FEContext provides source locations and other context for a front-end
 * node.  It has a file name, line number, and column number.
 */
public class FEContext
{
    private String fileName;
    private int lineNumber, columnNumber;
    
    /** Create a new context object with no location information. */
    public FEContext()
    {
        this(null);
    }
    
    /** Create a new context object with a known filename but no
     * line information.
     *
     * @arg fileName  Name of the file, or null if it is unavailable
     */
    public FEContext(String fileName)
    {
        this(fileName, -1);
    }
    
    /** Create a new context object with a known filename and line
     * number but no column number.
     *
     * @arg fileName  Name of the file, or null if it is unavailable
     * @arg line      Line number, or -1 if it is unavailable
     */
    public FEContext(String fileName, int line)
    {
        this(fileName, line, -1);
    }
    
    /** Create a new context object with known filename, line number,
     * and column number.
     *
     * @arg fileName  Name of the file, or null if it is unavailable
     * @arg line      Line number, or -1 if it is unavailable
     * @arg col       Column number, or -1 if it is unavailable
     */
    public FEContext(String fileName, int line, int col)
    {
        this.fileName = fileName;
        lineNumber = line;
        columnNumber = col;
    }
    
    /** Get the name of the file this node appears in, or null if it is
     * unavailable. */
    public String getFileName() 
    {
        return fileName;
    }
    
    /** Get the line number this node begins on, or -1 if it is
     * unavailable. */
    public int getLineNumber()
    {
        return lineNumber;
    }
    
    /** Get the column number this node begins on, or -1 if it is
     * unavailable. */
    public int getColumnNumber()
    {
        return columnNumber;
    }

    /** Return the location this represents, in the form
     * "filename.str:line".  Omits the line number if it is unavailable,
     * and uses a default filename it that is unavilable. */
    public String getLocation()
    {
        String file = getFileName();
        if (file == null) file = "<unknown>";
        int line = getLineNumber();
        if (line >= 0) return file + ":" + line;
        return file;
    }

    public String toString()
    {
        return getLocation();
    }
}
