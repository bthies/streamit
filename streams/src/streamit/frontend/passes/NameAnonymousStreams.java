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
 * @version $Id: NameAnonymousStreams.java,v 1.9 2006-09-25 13:54:54 dimock Exp $
 */
public class NameAnonymousStreams extends SymbolTableVisitor
{
    List<StreamSpec> newStreams;
    TempVarGen varGen;
    
    public NameAnonymousStreams(TempVarGen varGen)
    {
        super(null);
        newStreams = new java.util.ArrayList<StreamSpec>();
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
    public List<StreamSpec> getNewStreams()
    {
        return newStreams;
    }

    public Object visitProgram(Program prog)
    {
        prog = (Program)super.visitProgram(prog);
        // Combine prog's stream list and newStreams.
        List<StreamSpec> streams = new java.util.ArrayList<StreamSpec>();
        streams.addAll(prog.getStreams());
        streams.addAll(newStreams);
        return new Program(prog.getContext(), streams, prog.getStructs(), prog.getHelpers());
    }

    public Object visitSCAnon(SCAnon creator)
    {
        // First, replace things in children.
        StreamSpec newSpec = (StreamSpec)creator.getSpec().accept(this);
        
        // Wrap this in an empty symbol table, and look for free
        // variables.
        final Set<String> freeVars = new java.util.TreeSet<String>();
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
        String specName = "AnonFilter_" +
            varGen.getPrefix() + varGen.nextVarNum();
        List<Object> formals = new java.util.ArrayList<Object>();
        List<Expression> params  = new java.util.ArrayList<Expression>();
        for (Iterator<String> iter = freeVars.iterator(); iter.hasNext(); )
            {
                String name = iter.next();
                Expression var = new ExprVar(creator.getContext(), name);
                // Arrays are special: we need an extra parameter for
                // their length.
                Type type = getType(var);
                Type formalType;
                if (type instanceof TypeArray)
                    {
                        // RMR { process all array dims
                        int dims = ((TypeArray)type).getDims();
                        for (int d = 0; d < dims; d++) {
                            String lengthName = genDimName(name, d);
                            params.add(((TypeArray)type).getLength());
                            formals.add(new Parameter
                                        (new TypePrimitive(TypePrimitive.TYPE_INT),
                                         lengthName));
                            type = ((TypeArray)type).getBase();
                        }

                        // reset all dim lengths of the array to reference the
                        // parameter passed in, rather than the old length
                        formalType = renameArrayLengths(getType(var), dims, 
                                                        name, newSpec);
                        // } RMR
                    } else {
                        formalType = getType(var);
                    }
                params.add(var);
                formals.add(new Parameter(formalType, name));
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

    // RMR { 
    /**
     * Construct a new type for the input <pre>array</pre> by 
     * recursively visiting each of its dimensions (i.e., base)
     * and respective renaming its lengths to references the new
     * parameters passed to the stream constructor (as generated
     * by genDimName()).
     *
     * @return array with renamed lengths for each dimension
     */
    private Type renameArrayLengths(Type array,  int dims, 
                                    String name, StreamSpec spec)
    {
        assert array instanceof TypeArray;
        String dimName = genDimName(name, ((TypeArray)array).getDims()-dims);

        if (dims == 1) {
            return new TypeArray(((TypeArray)array).getComponent(),
                                 new ExprVar(spec.getContext(), dimName));
        }
        else {
            return new TypeArray(renameArrayLengths(array, dims-1, name, spec),
                                 new ExprVar(spec.getContext(), dimName));
        }
    }

    /**
     * Generate a new name for the array dimension length
     */
    private String genDimName(String name, int dim)
    {
        return "_len_" + "d" + dim + "_" + name;
    }
    // } RMR
}
