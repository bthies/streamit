/*
 * MoveStreamParameters.java: make constructors and init functions
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: MoveStreamParameters.java,v 1.2 2002-09-23 14:52:22 dmaze Exp $
 */

package streamit.frontend.tojava;

import streamit.frontend.nodes.*;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import java.util.ArrayList;

/**
 * MoveStreamParameters moves stream parameters into fields of stream
 * classes, and creates constructors and populates init functions as
 * necessary.
 */
public class MoveStreamParameters extends InitMunger
{
    private Function makeConstructor(FEContext context, String name,
                                     List params)
    {
        // Create a helper function with a call to super().
        // Work from the bottom up.  Create the parameter list to the
        // call:
        List superParams = new ArrayList();
        for (Iterator iter = params.iterator(); iter.hasNext(); )
        {
            Parameter param = (Parameter)iter.next();
            Expression sp = new ExprVar(context, param.getName());
            superParams.add(sp);
        }
        Expression funCall = new ExprFunCall(context, "super", superParams);
        Statement stmtSuper = new StmtExpr(context, funCall);
        Statement stmtBlock =
            new StmtBlock(context, Collections.singletonList(stmtSuper));
        Function fn =
            Function.newHelper(context, name,
                               new TypePrimitive(TypePrimitive.TYPE_VOID),
                               params, stmtBlock);
        return fn;
    }

    private Function addInitParams(Function init, List params)
    {
        FEContext context = init.getContext();
        
        // The init function should have no parameters coming in;
        // completely replace its parameter list with params.  This
        // means we just need to replace the body.
        List body = new ArrayList();
        for (Iterator iter = params.iterator(); iter.hasNext(); )
        {
            Parameter param = (Parameter)iter.next();
            Expression eThis = new ExprVar(context, "this");
            Expression lhs = new ExprField(context, eThis, param.getName());
            Expression rhs = new ExprVar(context, param.getName());
            Statement stmt = new StmtAssign(context, lhs, rhs);
            body.add(stmt);
        }
        StmtBlock oldBody = (StmtBlock)init.getBody();
        body.addAll(oldBody.getStmts());
        Statement newBody = new StmtBlock(oldBody.getContext(), body);
        
        // Too many parts here, don't use Function.newInit().
        return new Function(context, init.getCls(), init.getName(),
                            init.getReturnType(), params, newBody);
    }

    public Object visitStreamSpec(StreamSpec spec)
    {
        if (spec.getParams().size() > 0)
        {
            // Okay, we have some parameters.  We need to add this
            // to the list of variables, create a constructor, and add
            // the parameters to the init function.
            List newVars = new ArrayList(spec.getVars());
            // The parameters are Parameter objects, but the variables
            // are Statements (StmtVarDecls).  Convert.
            for (Iterator iter = spec.getParams().iterator(); iter.hasNext(); )
            {
                Parameter param = (Parameter)iter.next();
                Statement varDecl = new StmtVarDecl(spec.getContext(),
                                                    param.getType(),
                                                    param.getName(), null);
                newVars.add(varDecl);
            }
            
            List newFuncs = new ArrayList(spec.getFuncs());

            // Create a constructor:
            Function constructor = makeConstructor(spec.getContext(),
                                                   spec.getName(),
                                                   spec.getParams());
            newFuncs.add(constructor);
            
            // Rewrite the init function:
            Function init = findInit(spec.getContext(), spec.getFuncs());
            newFuncs.remove(init);
            init = addInitParams(init, spec.getParams());
            newFuncs.add(init);

            // And create the new stream spec.
            spec = new StreamSpec(spec.getContext(), spec.getType(),
                                  spec.getStreamType(), spec.getName(),
                                  Collections.EMPTY_LIST, newVars, newFuncs);
        }
        return spec;
    }
}
