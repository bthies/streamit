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

package streamit.frontend.passes;

import streamit.frontend.nodes.*;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Convert anonymous streams into named streams.  Anonymous streams
 * provide various implementation challenges; it can be more
 * convenient to deal with only named streams.  This pass finds
 * anonymous streams declared in
 * <code>streamit.frontend.nodes.SCAnon</code> stream creators.  Each
 * stream is converted to a new top-level stream declaration, and the
 * stream creator is replaced with a named stream creator for the new
 * stream.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: NameAnonymousStreams.java,v 1.2 2003-12-01 21:20:45 dmaze Exp $
 */
public class NameAnonymousStreams extends SymbolTableVisitor
{
    List newStreams;
    TempVarGen varGen;
    
    public NameAnonymousStreams(TempVarGen varGen)
    {
        super(null);
        newStreams = new java.util.ArrayList();
        this.varGen = varGen;
    }

    /**
     * Get the list of new streams created as a result of visiting
     * anonymous stream creators.  This list is also added into the
     * <code>Program</code> object the top-level visitor returns.
     *
     * @return list of newly created
     *         {@link streamit.frontend.nodes.StreamSpec} objects
     */
    public List getNewStreams()
    {
        return newStreams;
    }

    public Object visitProgram(Program prog)
    {
        prog = (Program)super.visitProgram(prog);
        // Combine prog's stream list and newStreams.
        List streams = new java.util.ArrayList();
        streams.addAll(prog.getStreams());
        streams.addAll(newStreams);
        return new Program(prog.getContext(), streams, prog.getStructs());
    }

    public Object visitSCAnon(SCAnon creator)
    {
        // First, replace things in children.
        StreamSpec newSpec = (StreamSpec)creator.getSpec().accept(this);
        
        // Wrap this in an empty symbol table, and look for free
        // variables.
        final Set freeVars = new java.util.TreeSet();
        newSpec.accept(new SymbolTableVisitor(null) {
                public Object visitExprVar(ExprVar expr)
                {
                    Object result = super.visitExprVar(expr);
                    if (!(symtab.hasVar(expr.getName())))
                        freeVars.add(expr.getName());
                    return result;
                }
            });

        // Create a top-level StreamSpec for this.
        String specName = "Anon_" + varGen.nextVar();
        List formals = new java.util.ArrayList();
        List params = new java.util.ArrayList();
        for (Iterator iter = freeVars.iterator(); iter.hasNext(); )
        {
            String name = (String)iter.next();
            Expression var = new ExprVar(creator.getContext(), name);
            // Arrays are special: we need an extra parameter for
            // their length.
            Type type = getType(var);
            if (type instanceof TypeArray)
            {
                params.add(((TypeArray)type).getLength());
                formals.add(new Parameter
                            (new TypePrimitive(TypePrimitive.TYPE_INT),
                             "_len_" + name));
            }
            params.add(var);
            formals.add(new Parameter(getType(var), name));
        }
        StreamSpec namedSpec = new StreamSpec(newSpec.getContext(),
                                              newSpec.getType(),
                                              newSpec.getStreamType(),
                                              specName,
                                              formals,
                                              newSpec.getVars(),
                                              newSpec.getFuncs());
        newStreams.add(namedSpec);
        
        // Also replace the stream creator.  The only thing we
        // need to create here is the list of parameters.
        return new SCSimple(creator.getContext(),
                            specName,
                            Collections.EMPTY_LIST,
                            params,
                            Collections.EMPTY_LIST);
    }
}
