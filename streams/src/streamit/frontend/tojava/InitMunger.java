/*
 * InitMunger.java: base class for visitors that add init statements
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: InitMunger.java,v 1.1 2002-09-23 14:52:22 dmaze Exp $
 */

package streamit.frontend.tojava;

import streamit.frontend.nodes.*;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import java.util.ArrayList;

/**
 * Base class for visitors that add statements to classes' init functions.
 */
abstract public class InitMunger extends FEReplacer
{
    public static Function findInit(FEContext context, List fns)
    {
        for (Iterator iter = fns.iterator(); iter.hasNext(); )
        {
            Function fn = (Function)iter.next();
            if (fn.getCls() == Function.FUNC_INIT)
                return fn;
        }
        
        // No init function; create an empty one.
        return Function.newInit(context,
                                new StmtBlock(context,
                                              Collections.EMPTY_LIST));
    }    
}
