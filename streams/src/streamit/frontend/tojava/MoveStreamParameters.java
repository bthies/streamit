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

package streamit.frontend.tojava;

import streamit.frontend.nodes.*;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import java.util.ArrayList;

/**
 * Move stream parameters into fields, and generate constructors and
 * init functions.  A StreamIt stream parameter is converted into a
 * class field in Java syntax.  The constructor and init function are
 * both modified to take the stream parameters as function parameters.
 * This pass creates constructors for all objects that have stream
 * parameters as well.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: MoveStreamParameters.java,v 1.14 2003-12-15 22:49:34 dmaze Exp $
 */
public class MoveStreamParameters extends InitMunger
{
    private Type objectType;
    
    public MoveStreamParameters()
    {
        super();
        objectType = new TypeStruct(null, "Object",
                                    Collections.EMPTY_LIST,
                                    Collections.EMPTY_LIST);
    }
    
    private Function addInitParams(Function init, List params)
    {
        FEContext context = init.getContext();
        
        // The init function should have no parameters coming in;
        // completely replace its parameter list with params.  This
        // means we just need to replace the body.
        List body = new ArrayList();
        List newParams = new ArrayList();
        for (Iterator iter = params.iterator(); iter.hasNext(); )
        {
            Parameter param = (Parameter)iter.next();
            String pName = "_param_" + param.getName();
            Expression lhs = new ExprVar(context, param.getName());
            Expression rhs = new ExprVar(context, pName);
            Type type = param.getType();
            param = new Parameter(type, pName);
            while (type instanceof TypeArray)
                type = ((TypeArray)type).getBase();
            if (type instanceof TypeStruct)
            {
                rhs = new ExprTypeCast(context, param.getType(), rhs);
                param = new Parameter(objectType, pName);
            }
            Statement stmt = new StmtAssign(context, lhs, rhs);
            body.add(stmt);
            newParams.add(param);
        }
        StmtBlock oldBody = (StmtBlock)init.getBody();
        body.addAll(oldBody.getStmts());
        Statement newBody = new StmtBlock(oldBody.getContext(), body);
        
        // Too many parts here, don't use Function.newInit().
        return new Function(context, init.getCls(), init.getName(),
                            init.getReturnType(), newParams, newBody);
    }

    // Return a function just like init, but with params as its
    // parameter list, doing no special work.
    private Function addInitParamsOnly(Function init, List params)
    {
        FEContext context = init.getContext();

        // As before.  We do actually need to make changes here,
        // if there are stream parameters that are Object type.
        // In that case, the parameters to the init function need
        // to be Object, and we need to make locals with the
        // right types.
        List body = new ArrayList();
        List newParams = new ArrayList();
        for (Iterator iter = params.iterator(); iter.hasNext(); )
        {
            Parameter param = (Parameter)iter.next();
            Type type = param.getType();
            while (type instanceof TypeArray)
                type = ((TypeArray)type).getBase();
            if (type instanceof TypeStruct)
            {
                String newName = "_obj_" + param.getName();
                Expression rhs = new ExprVar(context, newName);
                Expression cast =
                    new ExprTypeCast(context, param.getType(), rhs);
                body.add(new StmtVarDecl(context, param.getType(),
                                         param.getName(), cast));
                param = new Parameter(objectType, newName);
            }
            newParams.add(param);
        }
        StmtBlock oldBody = (StmtBlock)init.getBody();
        body.addAll(oldBody.getStmts());
        Statement newBody = new StmtBlock(oldBody.getContext(), body);
        
        return new Function(context, init.getCls(), init.getName(),
                            init.getReturnType(), newParams, newBody);
    }

    public Object visitStreamSpec(StreamSpec spec)
    {
        spec = (StreamSpec)super.visitStreamSpec(spec);
        
        if (spec.getParams().size() > 0)
        {
            List newFuncs = new ArrayList(spec.getFuncs());
            List newVars = new ArrayList(spec.getVars());

            if (spec.getType() == StreamSpec.STREAM_FILTER)
            {
                // Okay, we have some parameters.  We need to add this
                // to the list of variables and add the parameters to
                // the init function.
                // The parameters are Parameter objects, but the variables
                // are Statements (StmtVarDecls).  Convert.
                for (Iterator iter = spec.getParams().iterator();
                     iter.hasNext(); )
                {
                    Parameter param = (Parameter)iter.next();
                    FieldDecl field = new FieldDecl(spec.getContext(),
                                                    param.getType(),
                                                    param.getName(),
                                                    null);
                    newVars.add(field);
                }
            
                // Rewrite the init function:
                Function init = findInit(spec.getContext(), spec.getFuncs());
                newFuncs.remove(init);
                init = addInitParams(init, spec.getParams());
                newFuncs.add(init);
            }
            else
            {
                // Composite stream; the stream parameters only exist
                // within the context of the init function, no need to
                // create fields.  (In fact, this actively hurts.)
                Function init = findInit(spec.getContext(), spec.getFuncs());
                newFuncs.remove(init);
                init = addInitParamsOnly(init, spec.getParams());
                newFuncs.add(init);
            }

            // And create the new stream spec.
            spec = new StreamSpec(spec.getContext(), spec.getType(),
                                  spec.getStreamType(), spec.getName(),
                                  Collections.EMPTY_LIST, newVars, newFuncs);
        }
        return spec;
    }
}
