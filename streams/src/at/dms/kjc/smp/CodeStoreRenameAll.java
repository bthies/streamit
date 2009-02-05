package at.dms.kjc.smp;

import at.dms.compiler.*;
import at.dms.kjc.*;
import at.dms.kjc.backendSupport.ComputeCodeStore;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.lir.*;
import at.dms.util.*;

import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;

/**
 * This visitor renames every variable, method, and field to a globally
 * unique name.
 */
public class CodeStoreRenameAll extends SLIRReplacingVisitor
{
    /** How many variables have been renamed.  Used to uniquify names. */
    static private int counter = 0;
    
    /** This renamer is used by the renameOverAllFilters, to rename
        across filters, keep names distinct over all the filter */
    static public CodeStoreRenameAll globalRenamer;
    
    /**
     * Return a new name of the form oldName__number
     * @param oldName prefix for new name
     * @return oldName + "__" + counter++  for some counter
     */
    public static String newName(String oldName)
    {
        String name = oldName + "__" + counter;
        counter++;
        return name;
    }
    
    /** Inner class to keep track of the variables we've looked at. */
    private class RASymbolTable
    {
        HashMap<String, String> syms;
        RASymbolTable parent;

        RASymbolTable()
        {
            this(null);
        }
        RASymbolTable(RASymbolTable parent)
        {
            this.syms = new HashMap<String, String>();
            this.parent = parent;
        }
        String nameFor(String name)
        {
            if (syms.containsKey(name))
                return syms.get(name);
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

    private RASymbolTable symtab, classsymtab;

    /**
     * Make this private since it's dangerous to reuse the same
     * renameall construct to rename across multiple filters
     */
    private CodeStoreRenameAll()
    {
        super();
        symtab = new RASymbolTable();
    }
    
    public static void renameOverAllCodeStores(List<ComputeCodeStore<?>> codeStores) {
        //reset the global renamer
        globalRenamer = new CodeStoreRenameAll();
        
        for (ComputeCodeStore<?> codeStore : codeStores)
            globalRenamer.renameCodeStore(codeStore);
    }

    private void renameCodeStore(ComputeCodeStore<?> str)
    {
        RASymbolTable ost = symtab;
        symtab = new RASymbolTable(ost);
        classsymtab = symtab;
        
        findDecls(str.getFields());
        findDecls(str.getMethods());
        
        JFieldDeclaration[] newFields =
            new JFieldDeclaration[str.getFields().length];
        for (int i = 0; i < str.getFields().length; i++)
            newFields[i] = (JFieldDeclaration)str.getFields()[i].accept(this);

        JMethodDeclaration[] newMethods =
            new JMethodDeclaration[str.getMethods().length];
        for (int i = 0; i < str.getMethods().length; i++)
            {
                JMethodDeclaration oldMeth = str.getMethods()[i];
                JMethodDeclaration newMeth =
                    (JMethodDeclaration)oldMeth.accept(this);
                newMethods[i] = newMeth;
                
                if(oldMeth.equals(str.getMainFunction()))
                    str.setMainFunction(newMeth);
            }

        str.setFields(newFields);
        str.setMethods(newMethods);
        
        symtab = ost;
    }

    private void findDecls(JPhylum[] stmts)
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
        for (int i = 0; i < stmts.length; i++) {
            newstmts[i] = (JStatement)stmts[i].accept(this);
        }
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
        // have to mutate this instead of replacing it, since some
        // local vars refer to the object.
        self.setIdent(symtab.nameFor(ident));

        // visit dimensions inside array fields
        if (type.isArrayType()) {
            JExpression[] dims = ((CArrayType)type).getDims();
            for (int i=0; i<dims.length; i++) {
                dims[i] = (JExpression)dims[i].accept(this);
            }
        }

        return self;
    }

    /**
     * renames local variable expression
     */
    /*public Object visitLocalVariableExpression(JLocalVariableExpression self,
      String ident) {
      self.getVariable().setIdent(symtab.nameFor(self.getVariable().getIdent()));
      return self;
      }*/
    
    public Object visitVariableDefinition(JVariableDefinition self,
                                          int modifiers,
                                          CType type,
                                          java.lang.String ident,
                                          JExpression expr)
    {
        // need to mutate this instead of returning a new one, since
        // there are local variable expressions lingering around which
        // reference it.
        self.setIdent(symtab.nameFor(ident));
        if (expr!=null) {
            self.setExpression((JExpression)expr.accept(this));
        }
        // visit static array dimensions
        if (type.isArrayType()) {
            JExpression[] dims = ((CArrayType)type).getDims();
            for (int i=0; i<dims.length; i++) {
                JExpression newExp = (JExpression)dims[i].accept(this);
                if (newExp !=null && newExp!=dims[i]) {
                    dims[i] = newExp;
                }
            }
        }
        return self;
    }

    public Object visitForStatement(JForStatement self,
                                    JStatement init,
                                    JExpression cond,
                                    JStatement incr,
                                    JStatement body) {
    
        RASymbolTable ost = symtab;
        symtab = new RASymbolTable(ost);
        JStatement[] temp = { init };
        findDecls(temp);
        Object result = super.visitForStatement(self, init, cond, incr, body);
        //System.err.println("switching symtab from " + symtab + " back to " + ost);
        symtab = ost;
        return result;
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
        JMethodCallExpression retval = new JMethodCallExpression(self.getTokenReference(),
                (JExpression)prefix.accept(this),
                classsymtab.nameFor(ident),
                newArgs);
        /* RMR { the method field may have been lost 
         * in which case preserve the tapeType which
         * is a backup field carrying the function
         * return type
         */
        if (self.getMethod() != null) 
        	retval.setMethod(self.getMethod());
        else 
        	retval.setType(self.getType());
        /* } RMR */
        return retval;
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
        // visit I/O rates
        newdecl.setPush((JExpression)self.getPush().accept(this));
        newdecl.setPeek((JExpression)self.getPeek().accept(this));
        newdecl.setPop((JExpression)self.getPop().accept(this));
        // Return to previous symtab.
        symtab = ost;
        return newdecl;
    }        

    
    public Object visitFieldExpression(JFieldAccessExpression self,
                                       JExpression left,
                                       String ident)
    {
        //visit the left expression
        JExpression newLeft = (JExpression)left.accept(this);
        //the identifier for this field
        //if this field access is embedded in another field
        //access, don't rename the variable because it is accessing
        //a member of a class and this is not renamed
        String newIdent = ident;
    
        //only rename the field access if it is not embedded in a field
        //access
        if (!(newLeft instanceof JFieldAccessExpression) &&
            !(newLeft instanceof JLocalVariableExpression))
            newIdent = classsymtab.nameFor(ident);
    

        JFieldAccessExpression fieldAccess =
            new JFieldAccessExpression(self.getTokenReference(),
                                       newLeft,
                                       newIdent,
                                       self.getField());

        return fieldAccess;
    }
}
