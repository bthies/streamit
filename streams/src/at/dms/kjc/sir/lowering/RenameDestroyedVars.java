package at.dms.kjc.sir.lowering;

import java.util.*;
import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.lir.*;
import at.dms.compiler.JavaStyleComment;
import at.dms.compiler.JavadocComment;
import java.lang.Math;
import at.dms.compiler.TokenReference;


public class RenameDestroyedVars extends SLIRReplacingVisitor {

    private Set destroyedVars;
    
    private RenameDestroyedVars(Set vars) {
	destroyedVars = vars;
    }

    private HashMap live_vars; // CType -> Integer
    private HashMap max_live_vars; // CType -> Integer
    
    private boolean RENAME = false;
    private boolean assign = false;

    private HashMap first_assign; // first assign expression of a destroyed var
    private HashMap last_usage;   // last usage expresion of a destroyed var
    
    private HashMap var_alias; // assigned alias of variable Var -> New var
    private HashMap available_names; // type -> Stack (stack of available names for a type)

    public static void renameDestroyedVars(SIRFilter filter, Set destroyed_vars) {

	RenameDestroyedVars rename = new RenameDestroyedVars(destroyed_vars);

	JMethodDeclaration methods[] = filter.getMethods();
	for (int i = 0; i < methods.length; i++) {

	    rename.first_assign = new HashMap();
	    rename.last_usage = new HashMap();

	    rename.RENAME = false;
	    methods[i].accept(rename);

	    rename.RENAME = true;

	    rename.live_vars = new HashMap();
	    rename.max_live_vars = new HashMap();
	    rename.available_names = new HashMap();
	    rename.var_alias = new HashMap();

	    methods[i].accept(rename);

	    Set types = rename.max_live_vars.keySet();
	    Iterator iter = types.iterator();

	    while (iter.hasNext()) {
		Object type = iter.next();
		int num = ((Integer)rename.max_live_vars.get(type)).intValue();
		System.out.println("[Function: "+methods[i].getName()+" Type: "+type+" Max-live-destroyed-vars: "+num+"]");


		JBlock body = methods[i].getBody();
		Stack alias_stack = (Stack)rename.available_names.get(type);
		JVariableDefinition var;

		for (int y = 0; y < num; y++) {
		    assert (!alias_stack.empty());
		    var = (JVariableDefinition)alias_stack.pop();
		    body.addStatementFirst(new JVariableDeclarationStatement(null, var, null));
		}
	    }
	}
    }

    public Object visitAssignmentExpression(JAssignmentExpression self,
					    JExpression left,
					    JExpression right) {

	assign = false;
	JExpression newExp = (JExpression)right.accept(this);
	if (newExp!=null && newExp!=right) { self.setRight(newExp); }

	assign = false;
	if (left instanceof JLocalVariableExpression) { assign = true; }
	newExp = (JExpression)left.accept(this);
	if (newExp!=null && newExp!=left) { self.setLeft(newExp); }
	assign = false; // must set assign to false!!

	return self;
    }

    public Object visitCompoundAssignmentExpression(JCompoundAssignmentExpression self,
						    int oper,
						    JExpression left,
						    JExpression right) {

	assign = false;
	JExpression newExp = (JExpression)right.accept(this);
	if (newExp!=null && newExp!=right) { self.setRight(newExp); }

	assign = false;
	if (left instanceof JLocalVariableExpression) { assign = true; }
	newExp = (JExpression)left.accept(this);
	if (newExp!=null && newExp!=left) { self.setLeft(newExp); }
	assign = false; // must set assign to false!!

	return self;
    }


    public Object visitVariableDeclarationStatement(JVariableDeclarationStatement self,
						    JVariableDefinition[] vars) {

	if (!RENAME) return self;

	// We are in rename mode, eliminate destroyed variables

	ArrayList newVars = new ArrayList();
        for (int i = 0; i < vars.length; i++) {
	    if (!destroyedVars.contains(vars[i])) newVars.add(vars[i]);
	}

	if (newVars.size()>0) {
	    // if we have some vars, adjust us
	    self.setVars((JVariableDefinition[])newVars.toArray(new JVariableDefinition[0]));
	    return self;
	} else {
	    // otherwise, replace us with empty statement
	    return new JEmptyStatement(null, null);
	}
    }

    public Object visitLocalVariableExpression(JLocalVariableExpression self,
					       String ident) {
	JLocalVariable var = self.getVariable();
	if (destroyedVars.contains(var)) {
	    if (!RENAME) {

		// discover first assign and last use!
		if (assign) {
		    //System.out.println("destroyed variable: "+ident+" assigned to!");
		    if (!first_assign.containsKey(var)) {
			first_assign.put(var,self);
		    }
		} else {
		    //System.out.println("destroyed variable: "+ident+" used.");
		    last_usage.put(var,self);
		}

	    } else {

		// rename variables and find max number of live variables of each type

		CType type = self.getVariable().getType();

		Object o1 = first_assign.get(self.getVariable());
		Object o2 = last_usage.get(self.getVariable());

		if (self == o1) {

		    //System.out.println("live_vars++");

		    JVariableDefinition alias;

		    if (!live_vars.containsKey(type)) live_vars.put(type, new Integer(0));
		    if (!max_live_vars.containsKey(type)) max_live_vars.put(type, new Integer(0));
		    if (!available_names.containsKey(type)) available_names.put(type, new Stack());

		    int live = ((Integer)live_vars.get(type)).intValue() + 1;
		    live_vars.put(type, new Integer(live));

		    if (live > ((Integer)max_live_vars.get(type)).intValue()) {
			max_live_vars.put(type, new Integer(live));
			alias = new JVariableDefinition(null, 0, type, 
				     "__destroyed_"+type.toString()+"_"+live, null);
		    } else {
			Stack alias_stack = (Stack)available_names.get(type);
			assert (!alias_stack.empty());
			alias = (JVariableDefinition)alias_stack.pop();
		    }

		    var_alias.put(self.getVariable(), alias);

		    if (o2 == null) {
			// variable is never used so we can reuse its alias
			Stack alias_stack = (Stack)available_names.get(type);
			alias_stack.push(alias);
		    }
		}

		if (self == o2) {
		    //System.out.println("live_vars--");

		    assert (live_vars.containsKey(type));
		    int live = ((Integer)live_vars.get(type)).intValue() - 1;
		    
		    assert (live >= 0);
		    live_vars.put(type, new Integer(live));

		    Stack alias_stack = (Stack)available_names.get(type);
		    alias_stack.push(var_alias.get(self.getVariable()));
		}


		JVariableDefinition defn = (JVariableDefinition)var_alias.get(self.getVariable());
		return new JLocalVariableExpression(null, defn);

	    }
	}

	return self;
    }
}


