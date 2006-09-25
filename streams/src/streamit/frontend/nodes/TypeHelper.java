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

import java.util.List;
import java.util.Map;

import java.util.HashMap;

/**
 * Declares helper functions.
 *
 * @author  Janis Sermulins
 */

public class TypeHelper extends Type
{
    private FEContext context;
    private String name;
    private List<Function> funcs;
    private int cls;

    public static final int NORMAL_HELPERS = 0; 
    public static final int NATIVE_HELPERS = 1;
    
    /**
     * Creates a new helper. 
     *
     * @param context  file and line number the helper was declared in
     * @param name     name of the helper
     * @param funcs    list of <code>Function</code> containing the helper
     *                 functions
     */
    public TypeHelper(FEContext context, String name, List<Function> funcs, int cls)
    {
        this.context = context;
        this.name = name;
        this.funcs = funcs;
        this.cls = cls;
    }

    public TypeHelper(FEContext context, String name, List<Function> funcs)
    {
        this(context, name, funcs, NORMAL_HELPERS);
    }

    public int getCls() { return cls; }

    /**
     * Returns the context of the helper in the original source code.
     *
     * @return file name and line number the structure was declared in
     */
    public FEContext getContext()
    {
        return context;
    }
    
    /**
     * Returns the name of the helper package.
     *
     * @return the name of the helper package
     */
    public String getName()
    {
        return name;
    }
    
    /**
     * Returns the number of functions.
     *
     * @return the number of functions in the helper
     */
    public int getNumFuncs()
    {
        return funcs.size();
    }
    
    /**
     * Returns the specific functino.
     *
     * @param n zero-based index of the function to get
     * @return  the nth function field
     */
    public Function getFunction(int n)
    {
        return funcs.get(n);
    }

    public void setFunction(int n, Function func)
    {
        funcs.set(n, func);
    }
    
    // Remember, equality and such only test on the name.
    public boolean equals(Object other)
    {
        if (other instanceof TypeHelper)
            {
                TypeHelper that = (TypeHelper)other;
                return this.name.equals(that.name);
            }
        
        return false;
    }
    
    public int hashCode()
    {
        return name.hashCode();
    }
    
    public String toString()
    {
        return name;
    }
}
