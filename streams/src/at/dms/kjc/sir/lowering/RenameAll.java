package at.dms.kjc.sir.lowering;

import at.dms.compiler.*;
import at.dms.kjc.*;
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
public class RenameAll extends SLIRReplacingVisitor
{
    /** How many variables have been renamed.  Used to uniquify names. */
    static private int counter = 0;

    private String newName(String oldName)
    {
        String name = oldName + "__" + counter;
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

    private RASymbolTable symtab, classsymtab;

    /**
     * Make this private since it's dangerous to reuse the same
     * renameall construct to rename across multiple filters
     */
    private RenameAll()
    {
        super();
        symtab = new RASymbolTable();
    }

    /**
     * Renames the contents of <f1> but does not change the identity
     * of the filter itself.
     */
    public static void renameFilterContents(SIRFilter f1) {
	RenameAll ra = new RenameAll();
	SIRFilter f2 = ra.renameFilter(f1);
	f1.copyState(f2);
    }

    /**
     * Renames the contents of all filters in that are connected to
     * <str> or a parent of <str>.
     */
    public static void renameAllFilters(SIRStream str) {
	SIRStream toplevel = str;
	while (toplevel.getParent()!=null) {
	    toplevel = toplevel.getParent();
	}
	// name the stream structure
	IterFactory.createIter(toplevel).accept(new EmptyStreamVisitor() {
		/* visit a filter */
		public void visitFilter(SIRFilter self,
					SIRFilterIter iter) {
		    RenameAll.renameFilterContents(self);
		}
	    });
    }

    /**
     * Rename components of an arbitrary SIRStream in place.
     */
    private SIRFilter renameFilter(SIRFilter str)
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

        // Rename all of the methods; notice when we see init
        // and work functions.
        JMethodDeclaration oldInit = str.getInit();
        JMethodDeclaration oldWork = str.getWork();

	// also for twostagefilters... this is messy and should be
	// made more general somehow (just wait for general phased filters.)
	JMethodDeclaration oldInitWork = null;
	if (str instanceof SIRTwoStageFilter) {
	    oldInitWork = ((SIRTwoStageFilter)str).getInitWork();
	}

        JMethodDeclaration newInit = null, newWork = null, newInitWork = null;
        JMethodDeclaration[] newMethods =
            new JMethodDeclaration[str.getMethods().length];
        for (int i = 0; i < str.getMethods().length; i++)
        {
            JMethodDeclaration oldMeth = str.getMethods()[i];
            JMethodDeclaration newMeth =
                (JMethodDeclaration)oldMeth.accept(this);
            if (oldInit == oldMeth) newInit = newMeth;
            if (oldWork == oldMeth) newWork = newMeth;
            if (oldInitWork == oldMeth) newInitWork = newMeth;
            newMethods[i] = newMeth;
        }

	SIRFilter nf;
	if (str instanceof SIRTwoStageFilter) {
	    Utils.assert(oldInitWork!=null);
	    Utils.assert(newInitWork!=null);
	    SIRTwoStageFilter two = (SIRTwoStageFilter)str;
	    nf = new SIRTwoStageFilter(two.getParent(),
				       newName(two.getIdent()),
				       newFields,
				       newMethods,
				       (JExpression)two.getPeek().accept(this),
				       (JExpression)two.getPop().accept(this),
				       (JExpression)two.getPush().accept(this),
				       newWork,
				       two.getInitPeek(),
				       two.getInitPop(),
				       two.getInitPush(),
				       newInitWork,
				       two.getInputType(),
				       two.getOutputType());
	} else {
	    nf = new SIRFilter(str.getParent(),
                               newName(str.getIdent()),
                               newFields,
                               newMethods,
                               (JExpression)str.getPeek().accept(this),
                               (JExpression)str.getPop().accept(this),
                               (JExpression)str.getPush().accept(this),
                               newWork,
                               str.getInputType(),
                               str.getOutputType());
	}
	    
	// replace any init call to <str> in the parent with an init
	// call to <nf> -- DON'T DO THIS since it messes up mutation case.
	// replaceParentInit(str, nf);

	nf.setInit(newInit);
        symtab = ost;
        return nf;
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
        return new JMethodCallExpression(self.getTokenReference(),
                                         (JExpression)prefix.accept(this),
                                         classsymtab.nameFor(ident),
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
                                          classsymtab.nameFor(ident),
					  self.getField());
    }
}
