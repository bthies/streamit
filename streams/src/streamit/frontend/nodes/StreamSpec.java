/*
 * StreamSpec.java: specification of a named or anonymous stream
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: StreamSpec.java,v 1.9 2003-01-09 20:45:14 dmaze Exp $
 */

package streamit.frontend.nodes;

import java.util.Collections;
import java.util.List;

import java.util.ArrayList;

/**
 * StreamSpec is a container class that contains all of the state for
 * a StreamIt stream type.  A StreamSpec may or may not have a name;
 * if there is no name, this is an anonymous stream.  It also has a
 * type (as an integer), a stream type (with I/O data types), a
 * parameter list, a list of variable declarations (as Statements;
 * they should all actually be StmtVarDecls), and a list of function
 * declarations (as Function objects).  The stream type may be null,
 * in which case the compiler will need to determine the stream type
 * on its own.
 */
public class StreamSpec extends FENode
{
    private int type;
    private StreamType st;
    private String name;
    private List params;
    private List vars;
    private List funcs;

    public static final int STREAM_FILTER = 1;
    public static final int STREAM_PIPELINE = 2;
    public static final int STREAM_SPLITJOIN = 3;
    public static final int STREAM_FEEDBACKLOOP = 4;
    public static final int STREAM_PHASEDFILTER = 5;
    
    /** Creates a new stream specification given its name, a list of
     * variables, and a list of functions. */
    public StreamSpec(FEContext context, int type, StreamType st,
                      String name, List params, List vars, List funcs)
    {
        super(context);
        this.type = type;
        this.st = st;
        this.name = name;
        this.params = params;
        this.vars = vars;
        this.funcs = funcs;
    }
    
    /** Creates a new stream specification given its name and the text
     * of its init function.  Useful for composite streams that have
     * no other functions. */
    public StreamSpec(FEContext context, int type, StreamType st,
                      String name, List params, Statement init)
    {
        this(context, type, st, name, params, Collections.EMPTY_LIST,
             Collections.singletonList(Function.newInit(init.getContext(),
                                                        init)));
    }

    /** Returns the type of this, as one of the integer constants above. */
    public int getType()
    {
        return type;
    }

    /** Returns the stream type (I/O data types) of this. */
    public StreamType getStreamType()
    {
        return st;
    }

    /** Returns the name of this, or null if this is an anonymous stream. */
    public String getName()
    {
        return name;
    }

    /** Returns the parameters to this. */
    public List getParams()
    {
        return params;
    }
    
    /** Returns the variables declared in this, as a list of Statements. */
    public List getVars()
    {
        return vars;
    }
    
    /** Returns the functions declared in this, as a list of Functions. */
    public List getFuncs()
    {
        return funcs;
    }

    /** Returns the init function declared in this, or null.  If multiple
     * init functions are declared (probably an error), returns one
     * arbitrarily. */
    public Function getInitFunc()
    {
        for (Iterator iter = funcs.iterator(); iter.hasNext(); )
        {
            Function func = (Function)iter.next();
            if (func.getCls() == Function.FUNC_INIT)
                return func;
        }
        return null;
    }

    /** Returns the work function declared in this, or null.  If multiple
     * work functions are declared (probably an error), returns one
     * arbitrarily. */
    public FuncWork getWorkFunc()
    {
        for (Iterator iter = funcs.iterator(); iter.hasNext(); )
        {
            Function func = (Function)iter.next();
            if (func.getCls() == Function.FUNC_WORK)
                return (FuncWork)func;
        }
        return null;
    }

    /** Returns the function with a given name contained in this, or
     * null.  name should not be null.  If multiple functions are
     * declared with the same name (probably an error), returns one
     * arbitrarily. */
    public Function getFuncNamed(String name)
    {
        for (Iterator iter = funcs.iterator(); iter.hasNext(); )
        {
            Function func = (Function)iter.next();
            String fname = func.getName();
            if (fname != null && fname.equals(name))
                return func;
        }
        return null;
    }

    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitStreamSpec(this);
    }
}
