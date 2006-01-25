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
 * Replaces StmtSendMessage with HelperCalls where the receiver
 * of the message can not be found in the symbol table. This pass
 * is needed because parser can not distinguish between message
 * send statements and helper call statements!
 *
 * @author  Janis Sermulins
 */

public class StmtSendToHelper extends SymbolTableVisitor
{
    public StmtSendToHelper()
    {
        super(null);
    }

    
    public Object visitStmtSendMessage(StmtSendMessage stmt)
    {

        Expression receiver = (Expression)stmt.getReceiver();
        if (receiver instanceof ExprVar) {
            ExprVar var = (ExprVar)receiver;
            try {
                Type type = symtab.lookupVar(var);
            } catch (UnrecognizedVariableException ex) {
                return new StmtHelperCall(stmt.getContext(), 
                                          var.getName(), 
                                          stmt.getName(), 
                                          stmt.getParams()); 
            }
        }
        return stmt;
    }
}
