/*
 * Function.java: a generic function declaration
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: Function.java,v 1.1 2002-09-04 15:12:56 dmaze Exp $
 */

package streamit.frontend.nodes;

import java.util.Collections;
import java.util.List;

/**
 * A Function represents a function declaration in a StreamIt program.
 * This may be an init function, work function, helper function, or
 * message handler.  A function has a class (one of the above), an
 * optional name, an optional parameter list, a return type (void for
 * anything other than helper functions), and a body.
 */
public class Function extends FENode
{
    // Classes:
    public static final int FUNC_INIT = 1;
    public static final int FUNC_WORK = 2;
    public static final int FUNC_HANDLER = 3;
    public static final int FUNC_HELPER = 4;
    
    private int cls;
    private String name; // or null
    private Type returnType;
    private List params;
    private Statement body;
    
    /** Internal constructor to create a new Function from all parts.
     * This is public so that visitors that want to create new objects
     * can, but you probably want one of the other creator functions. */
    public Function(FEContext context, int cls, String name,
                    Type returnType, List params, Statement body)
    {
        super(context);
        this.cls = cls;
        this.name = name;
        this.returnType = returnType;
        this.params = params;
        this.body = body;
    }
    
    /** Create a new init function given its body. */
    public static Function newInit(FEContext context, Statement body)
    {
        return new Function(context, FUNC_INIT, null,
                            new TypePrimitive(TypePrimitive.TYPE_VOID),
                            Collections.EMPTY_LIST, body);
    }

    /** Create a new message handler given its name (not null), parameters,
     * and body. */
    public static Function newHandler(FEContext context, String name,
                                      List params, Statement body)
    {
        return new Function(context, FUNC_HANDLER, name,
                            new TypePrimitive(TypePrimitive.TYPE_VOID),
                            params, body);
    }
    
    /** Create a new helper function given its parts. */
    public static Function newHelper(FEContext context, String name,
                                     Type returnType, List params,
                                     Statement body)
    {
        return new Function(context, FUNC_HELPER, name, returnType,
                            params, body);
    }

    /** Returns the class of this function as an integer. */
    public int getCls() 
    {
        return cls;
    }
    
    /** Returns the name of this function, or null if it is anonymous. */
    public String getName()
    {
        return name;
    }
    
    /** Returns the parameters of this function, as a List of Parameter
     * objects. */
    public List getParams()
    {
        return params;
    }
    
    /** Returns the return type of this function. */
    public Type getReturnType()
    {
        return returnType;
    }
    
    /** Returns the body of this function, as a single statement
     * (likely a StmtBlock). */
    public Statement getBody()
    {
        return body;
    }
    
    /** Accepts a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        // return v.visitFunction(this);
        return null;
    }
}
