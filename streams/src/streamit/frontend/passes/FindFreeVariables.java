package streamit.frontend.passes;

import streamit.frontend.nodes.*;

import java.util.Iterator;
import java.util.List;

/**
 * Front-end visitor pass to find free variables in anonymous streams.
 * The StreamIt language spec allows anonymous streams to use
 * compile-time constants from the enclosing code, with this typically
 * being induction variables and stream parameters.  In the Java code,
 * though, this is only allowed for variables declared final, or
 * fields of final objects.  Implementation-wise, this means that the
 * output Java code must contain a final wrapper variable when a free
 * variable in an anonymous stream (that is, one without a declaration
 * inside the anonymous stream) corresponds to a local.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: FindFreeVariables.java,v 1.3 2003-07-31 20:27:44 dmaze Exp $
 */
public class FindFreeVariables extends SymbolTableVisitor
{
    List freeVars;
    
    public FindFreeVariables()
    {
        super(null);
        freeVars = null;
    }

    public Object visitExprVar(ExprVar expr)
    {
        Object result = super.visitExprVar(expr);
        if (!(symtab.hasVar(expr.getName())))
            freeVars.add(expr.getName());
        return result;
    }

    public Object visitStreamSpec(StreamSpec spec)
    {
        // Skip all of this if the spec is named.
        if (spec.getName() != null)
            return super.visitStreamSpec(spec);
        
        List oldFreeVars = freeVars;
        freeVars = new java.util.ArrayList();
        // Wrap this in an empty symbol table.
        SymbolTable oldSymTab = symtab;
        symtab = new SymbolTable(null);
        Object result = super.visitStreamSpec(spec);
        symtab = oldSymTab;
        for (Iterator iter = freeVars.iterator(); iter.hasNext(); )
        {
            String name = (String)iter.next();
            // Is the variable free here, too?
            if (!symtab.hasVar(name))
                oldFreeVars.add(name);
            // Look up the variable in the symbol table; only print
            // if it's a local.
            else if (symtab.lookupKind(name) == SymbolTable.KIND_LOCAL)
            {
                final String was = name;
                final String wrapped = "_final_" + name;
                Type type = symtab.lookupVar(name);
                result = ((FENode)result).accept
                    (new SymbolTableVisitor(new SymbolTable(null)) {
                        public Object visitExprVar(ExprVar expr)
                        {
                            Object result = super.visitExprVar(expr);
                            try
                            {
                                symtab.lookupVar(expr.getName());
                            }
                            catch (UnrecognizedVariableException e)
                            {
                                if (expr.getName().equals(was))
                                    return new ExprVar(expr.getContext(),
                                                       wrapped);
                                // else fall through
                            }
                            return result;
                        }
                    });
                // Also insert a statement for that variable and add
                // it to the local symbol table.  addStatement will
                // add the statement *before* the one that includes this
                // StreamSpec, so we're set.  But only do this if
                // we haven't already; specifically, if symtab doesn't
                // contain the wrapped variable.
                if (!(symtab.hasVar(wrapped)))
                {
                    FEContext context = ((FENode)result).getContext();
                    Statement decl =
                        new StmtVarDecl(context, type, wrapped, null);
                    Statement assn =
                        new StmtAssign(context,
                                       new ExprVar(context, wrapped),
                                        new ExprVar(context, name));
                    addStatement(decl);
                    addStatement(assn);
                    symtab.registerVar(wrapped, type, decl,
                                       SymbolTable.KIND_LOCAL);
                }
            }
        }
        freeVars = oldFreeVars;
        return result;
    }
}
