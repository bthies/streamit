/*
 * Copyright 2003 by the Massachusetts Institute of Technology.
 *
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in
 * advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 * M.I.T. makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is"
 * without express or implied warranty.
 */

package streamit.frontend.nodes;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import java.util.ArrayList;

/**
 * Container class containing all of the state for a StreamIt stream
 * type.  A StreamSpec may or may not have a name; if there is no
 * name, this is an anonymous stream.  It also has a type (as an
 * integer), a stream type (with I/O data types), a parameter list, a
 * list of variable declarations (as <code>Statement</code>s; they
 * should all actually be <code>StmtVarDecl</code>s), and a list of
 * function declarations (as <code>Function</code> objects).  The
 * stream type may be <code>null</code>, in which case the compiler
 * will need to determine the stream type on its own.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: StreamSpec.java,v 1.14 2003-10-09 19:51:00 dmaze Exp $
 */
public class StreamSpec extends FENode
{
    private int type;
    private StreamType st;
    private String name;
    private List params;
    private List vars;
    private List funcs;

    /** Stream type constant for a filter. */
    public static final int STREAM_FILTER = 1;
    /** Stream type constant for a pipeline. */
    public static final int STREAM_PIPELINE = 2;
    /** Stream type constant for a split-join. */
    public static final int STREAM_SPLITJOIN = 3;
    /** Stream type constant for a feedback loop. */
    public static final int STREAM_FEEDBACKLOOP = 4;
    
    /**
     * Creates a new stream specification given its name, a list of
     * variables, and a list of functions.
     *
     * @param context  front-end context indicating file and line
     *                 number for the specification
     * @param type     STREAM_* constant indicating the type of
     *                 stream object
     * @param st       stream type giving input and output types of
     *                 the stream object
     * @param name     string name of the object
     * @param params   list of <code>Parameter</code> that are formal
     *                 parameters to the stream object
     * @param vars     list of <code>StmtVarDecl</code> that are
     *                 fields of a filter stream
     * @param funcs    list of <code>Function</code> that are member
     *                 functions of the stream object
     */
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
    
    /**
     * Creates a new stream specification given its name and the text
     * of its init function.  Useful for composite streams that have
     * no other functions.
     *
     * @param context  front-end context indicating file and line
     *                 number for the specification
     * @param type     STREAM_* constant indicating the type of
     *                 stream object
     * @param st       stream type giving input and output types of
     *                 the stream object
     * @param name     string name of the object
     * @param params   list of <code>Parameter</code> that are formal
     *                 parameters to the stream object
     * @param init     statement containing initialization code for
     *                 the object
     */
    public StreamSpec(FEContext context, int type, StreamType st,
                      String name, List params, Statement init)
    {
        this(context, type, st, name, params, Collections.EMPTY_LIST,
             Collections.singletonList(Function.newInit(init.getContext(),
                                                        init)));
    }

    /**
     * Returns the type of this, as one of the integer constants above.
     *
     * @return  integer type of the stream object
     */
    public int getType()
    {
        return type;
    }

    /**
     * Returns the type of this, as a String (Pipeline, SplitJoin, FeedbackLoop, Filter).
     *
     * @return  String type of the stream object
     */
    public String getTypeString() {
	switch(type) {
	case STREAM_FILTER:
	    return "Filter";
	case STREAM_PIPELINE:
	    return "Pipeline";
	case STREAM_SPLITJOIN:
	    return "SplitJoin";
	case STREAM_FEEDBACKLOOP:
	    return "FeedbackLoop";
	}
	return null;
    }

    /**
     * Returns the stream type (I/O data types) of this.
     *
     * @return  stream type containing input and output types of the
     *          stream, or null if a stream type was not explicitly
     *          included in the code
     */
    public StreamType getStreamType()
    {
        return st;
    }

    /**
     * Returns the name of this, or null if this is an anonymous stream.
     *
     * @return  string name of the object, or null for an anonymous stream
     */
    public String getName()
    {
        return name;
    }

    /**
     * Returns the formal parameters of the stream object.
     *
     * @return  list of {@link Parameter}
     */
    public List getParams()
    {
        return params;
    }
    
    /**
     * Returns the field variables declared in this, as a list of
     * Statements.  Each of the statements will probably be a
     * {@link StmtVarDecl}.
     *
     * @return  list of {@link Statement}
     */
    public List getVars()
    {
        return vars;
    }
    
    /**
     * Returns the functions declared in this, as a list of Functions.
     *
     * @return  list of {@link Function}
     */
    public List getFuncs()
    {
        return funcs;
    }

    /**
     * Returns the init function declared in this, or null.  If multiple
     * init functions are declared (probably an error), returns one
     * arbitrarily.
     *
     * @return  function containing the initialiation code
     */
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

    /**
     * Returns the work function declared in this, or null.  If multiple
     * work functions are declared (probably an error), returns one
     * arbitrarily.
     *
     * @return  function containing steady-state work code, or null for
     *          non-filters
     */
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

    /**
     * Returns the function with a given name contained in this, or
     * null.  name should not be null.  If multiple functions are
     * declared with the same name (probably an error), returns one
     * arbitrarily.
     *
     * @return  function named name, or null
     */
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

    /**
     * Accept a front-end visitor.
     *
     * @param v  front-end visitor to accept
     * @return   object returned from the visitor
     * @see      FEVisitor#visitStreamSpec
     */
    public Object accept(FEVisitor v)
    {
        return v.visitStreamSpec(this);
    }
}
