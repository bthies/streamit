package at.dms.kjc.sir.lowering;

import at.dms.compiler.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.lir.*;

import java.util.HashMap;

/**
 * This visitor renames every variable, method, and field to a globally
 * unique name.
 */
public class RenameAll extends SLIRReplacingVisitor
{
    /** How many variables have been renamed.  Used to uniquify names. */
    static private int counter = 0;

    private String newName(String oldName)
    {
        String name = oldName + "_" + counter;
        counter++;
        return name;
    }
    
    /** Inner class to keep track of the variables we've looked at. */
    private class RASymbolTable
    {
        HashMap syms;
        RASymbolTable parent;

        RASymbolTable()
        {
            this(null);
        }
        RASymbolTable(RASymbolTable parent)
        {
            this.syms = new HashMap();
            this.parent = parent;
        }
        String nameFor(String name)
        {
            if (syms.containsKey(name))
                return (String)syms.get(name);
            else if (parent != null)
                return parent.nameFor(name);
            else
                return name;
        }
        void addName(String oldName, String newName)
        {
            syms.put(oldName, newName);
        }
        void addName(String oldName)
        {
            addName(oldName, newName(oldName));
        }
        RASymbolTable getParent()
        {
            return parent;
        }
    }

    private RASymbolTable symtab;

    public RenameAll()
    {
        super();
        symtab = new RASymbolTable();
    }

    /**
     * Rename components of an arbitrary SIRStream in place.
     */
    public SIRFilter renameFilter(SIRFilter str)
    {
        RASymbolTable ost = symtab;
        symtab = new RASymbolTable(ost);
        findDecls(str.getFields());
        findDecls(str.getMethods());
        JFieldDeclaration[] newFields =
            new JFieldDeclaration[str.getFields().length];
        for (int i = 0; i < str.getFields().length; i++)
            newFields[i] = (JFieldDeclaration)str.getFields()[i].accept(this);

        // Rename all of the methods; notice when we see init
        // and work functions.
        JMethodDeclaration oldInit = str.getInit();
        JMethodDeclaration oldWork = str.getWork();
        JMethodDeclaration newInit = null, newWork = null;
        JMethodDeclaration[] newMethods =
            new JMethodDeclaration[str.getMethods().length];
        for (int i = 0; i < str.getMethods().length; i++)
        {
            JMethodDeclaration oldMeth = str.getMethods()[i];
            JMethodDeclaration newMeth =
                (JMethodDeclaration)oldMeth.accept(this);
            if (oldInit == oldMeth) newInit = newMeth;
            if (oldWork == oldMeth) newWork = newMeth;
            newMethods[i] = newMeth;
        }

        SIRFilter nf = new SIRFilter(str.getParent(),
                                     newName(str.getIdent()),
                                     newFields,
                                     newMethods,
                                     str.getPeek(),
                                     str.getPop(),
                                     str.getPush(),
                                     newWork,
                                     str.getInputType(),
                                     str.getOutputType());
        nf.setInit(newInit);
        symtab = ost;
        return nf;
    }

    public void findDecls(JPhylum[] stmts)
    {
        for (int i = 0; i < stmts.length; i++)
        {
            if (stmts[i] instanceof JFieldDeclaration)
            {
                JFieldDeclaration fd = (JFieldDeclaration)stmts[i];
                symtab.addName(fd.getVariable().getIdent());
            }
            if (stmts[i] instanceof JMethodDeclaration)
            {
                JMethodDeclaration md = (JMethodDeclaration)stmts[i];
                symtab.addName(md.getName());
            }
            if (stmts[i] instanceof JLocalVariable)
            {
                JLocalVariable lv = (JLocalVariable)stmts[i];
                symtab.addName(lv.getIdent());
            }
            if (stmts[i] instanceof JVariableDeclarationStatement)
            {
                JVariableDeclarationStatement vds =
                    (JVariableDeclarationStatement)stmts[i];
                JVariableDefinition[] defs = vds.getVars();
                for (int j = 0; j < defs.length; j++)
                    symtab.addName(defs[j].getIdent());
            }
        }
    }

    public Object visitBlockStatement(JBlock self, JavaStyleComment[] comments)
    {
        RASymbolTable ost = symtab;
        symtab = new RASymbolTable(ost);
        JStatement[] stmts = self.getStatementArray();
        JStatement[] newstmts = new JStatement[stmts.length];
        findDecls(stmts);
        for (int i = 0; i < stmts.length; i++)
            newstmts[i] = (JStatement)stmts[i].accept(this);
        symtab = ost;
        return new JBlock(self.getTokenReference(), newstmts, comments);
    }

    public Object visitFieldDeclaration(JFieldDeclaration self,
                                        int modifiers,
                                        CType type,
                                        String ident,
                                        JExpression expr)
    {
        JVariableDefinition vardef =
            (JVariableDefinition)self.getVariable().accept(this);
        return new JFieldDeclaration(self.getTokenReference(),
                                     vardef,
                                     null,
                                     null);
    }

    public Object visitFormalParameters(JFormalParameter self,
                                        boolean isFinal,
                                        CType type,
                                        String ident)
    {
        return new JFormalParameter(self.getTokenReference(),
                                    0, //desc?
                                    type,
                                    symtab.nameFor(ident),
                                    isFinal);
    }

    public Object visitVariableDefinition(JVariableDefinition self,
                                          int modifiers,
                                          CType type,
                                          java.lang.String ident,
                                          JExpression expr)
    {
        return new JVariableDefinition(self.getTokenReference(),
                                       modifiers, type,
                                       symtab.nameFor(ident),
                                       expr != null ?
                                         (JExpression)expr.accept(this) :
                                         null);
    }

    // Hmm.  Are there anonymous creations at this point?  Ignore for now.

    public Object visitNameExpression(JNameExpression self,
                                      JExpression prefix,
                                      String ident)
    {
        return new JNameExpression(self.getTokenReference(),
                                   (JExpression)prefix.accept(this),
                                   symtab.nameFor(ident));
    }

    public Object visitMethodCallExpression(JMethodCallExpression self,
                                            JExpression prefix,
                                            String ident,
                                            JExpression[] args)
    {
        JExpression[] newArgs = new JExpression[args.length];
        for (int i = 0; i < args.length; i++)
            newArgs[i] = (JExpression)args[i].accept(this);
        return new JMethodCallExpression(self.getTokenReference(),
                                         (JExpression)prefix.accept(this),
                                         symtab.nameFor(ident),
                                         newArgs);
    }

    public Object visitMethodDeclaration(JMethodDeclaration self,
                                         int modifiers,
                                         CType returnType,
                                         String ident,
                                         JFormalParameter[] parameters,
                                         CClassType[] exceptions,
                                         JBlock body)
    {
        // Enter a scope, and insert the parameters.
        RASymbolTable ost = symtab;
        symtab = new RASymbolTable(ost);
        findDecls(parameters);
        // Now rename our arguments and produce a new method declaration.
        JFormalParameter[] newParams = new JFormalParameter[parameters.length];
        for (int i = 0; i < parameters.length; i++)
            newParams[i] = (JFormalParameter)parameters[i].accept(this);
        JMethodDeclaration newdecl =
            new JMethodDeclaration(self.getTokenReference(),
                                   modifiers,
                                   returnType,
                                   symtab.nameFor(ident),
                                   newParams,
                                   exceptions,
                                   (JBlock)body.accept(this),
                                   null, null);
        // Return to previous symtab.
        symtab = ost;
        return newdecl;
    }        

    public Object visitFieldExpression(JFieldAccessExpression self,
                                       JExpression left,
                                       String ident)
    {
        return new JFieldAccessExpression(self.getTokenReference(),
                                          (JExpression)left.accept(this),
                                          symtab.nameFor(ident));
    }
}
