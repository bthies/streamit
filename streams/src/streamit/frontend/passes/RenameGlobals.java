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

import java.util.Iterator;
import java.util.List;

/**
 * Front-end visitor passes that replaces global variable expressions
 * with a correct way to access global variables.
 * 
 * It relies on SymbolTableVisitor first visiting 'static' sections
 * and keeping information about the 'static' sections in the base
 * level of the symbol table.
 *
 * The traversal relies on FEReplacer via SymbolTableVisitor.
 * It has been necessary to extend FEReplacer to visit more of the
 * program.  We may still have holes, that will become apparant later.
 * 
 * This pass is simple: if an ExprVar is not occurring inside a
 * static section (identified by STREAM_GLOBAL) and is defined
 * in a static section (kind is KIND_GLOBAL) then replace 
 * ExprVar "v" with ExprField "TheGlobal.v".
 * 
 * There is a slight hack for library code generation which treats 
 * TheGlobal as a singleton object.
 *
 * @author  Janis Sermulins
 */

// in FEReplacer: visitSJDuplicate is a no-op
// visitSJRoundRobin, visitSJWeightedRR only visit weights
// visitStmtAdd visits getCreator(stmt) what of parameters?

public class RenameGlobals extends SymbolTableVisitor
{
    /**
     * libraryFormat is set to true when generating code for java/library.
     */
    private boolean libraryFormat;

    /**
     * global is set to true when visiting a 'static' (TheGlobal) construct.
     */
    private boolean global;

    public RenameGlobals(boolean libraryFormat)
    {
        super(null);
        this.libraryFormat = libraryFormat;
        global = false;
    }

    public Object visitStreamSpec(StreamSpec ss) {
        if (ss.getType() == StreamSpec.STREAM_GLOBAL) {
            global = true;
        }
        Object result = super.visitStreamSpec(ss);
        global = false;
        return result;
    }
    
    public Object visitExprVar(ExprVar var) {

        int kind = symtab.lookupKind(var.getName());
        if (!global && kind == SymbolTable.KIND_GLOBAL) {
            FEContext context = var.getContext();
            Expression global = new ExprVar(context, "TheGlobal");
            if (libraryFormat) {
                Expression exp = new ExprField(context, global,
                                               "__get_instance()");
                return new ExprField(context, exp, var.getName());
            } else {
                return new ExprField(context, global, var.getName());
            }
        }
        return var;
    }
}


