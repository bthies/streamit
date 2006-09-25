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
import java.util.List;
import java.util.ArrayList;

/**
 * Separate certain initializers for FIELDS into a field declaration
 * and an initializer in the init function.
 *
 * <pre>
 * int->int filter foo {
 *    int c = 4;
 *    init { ... }
 *    work { ... }
 * }
 * </pre>
 *
 * separate this into two statements like
 *
 * <pre>
 * int->int filter foo {
 *    int c;
 *    init { c = 4; }
 *    work { ... }
 * }
 * </pre>
 *
 */
public class SeparateFieldInitializers extends streamit.frontend.tojava.InitMunger
{
    /**
     * List of assignments to be added to init function.
     */
    private ArrayList<Statement> fieldInits;
    /**
     * Whether or not we're targeting the Java library.
     */
    private boolean libraryFormat;
    
    public SeparateFieldInitializers(boolean libraryFormat) {
        super();
        fieldInits = new ArrayList<Statement>();
        this.libraryFormat = libraryFormat;
    }

    public Object visitStreamSpec(StreamSpec spec) {
        // maintain a separate list of field inits per stream
        ArrayList<Statement> oldFieldInits = fieldInits;
        fieldInits = new ArrayList<Statement>();       

        spec = (StreamSpec)super.visitStreamSpec(spec);
    
        // now that we've visited whole thing, replace init with
        // version that has field assigments
        List<Function> fns = new ArrayList<Function>(spec.getFuncs());
        fns = replaceInitWithPrepended(spec.getContext(), 
                                       fns, 
                                       fieldInits);

        // And create the new stream spec.
        spec = new StreamSpec(spec.getContext(), spec.getType(),
                              spec.getStreamType(), spec.getName(),
                              spec.getParams(), spec.getVars(), fns);

        // restore old field inits for parent stream
        fieldInits = oldFieldInits;

        return spec;
    }

    public Object visitFieldDecl(FieldDecl field)
    {
        field = (FieldDecl)super.visitFieldDecl(field);

        List<Expression> newInits = new ArrayList<Expression>();
        for (int i = 0; i < field.getNumFields(); i++)
            {
                Expression init = field.getInit(i);
                // don't move array initializers, they need to stay.
                if (init!=null && !(init instanceof ExprArrayInit) &&
                    // Also, don't move anything of type array if we're going
                    // through the compiler path.
                    (libraryFormat || !(field.getType(i) instanceof TypeArray))) {
                    // move assignment to init function
                    FEContext context = init.getContext();
                    Expression lhs = new ExprVar(context, field.getName(i));
                    Expression rhs = init;
                    fieldInits.add(new StmtAssign(context, lhs, rhs));

                    // existing initializer becomes empty
                    init = null;
                }
                newInits.add(init);
            }
        return new FieldDecl(field.getContext(), field.getTypes(),
                             field.getNames(), newInits);
    }
}
