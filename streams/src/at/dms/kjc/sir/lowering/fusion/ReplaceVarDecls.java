
package at.dms.kjc.sir.lowering.fusion;

import java.util.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;

/*
public class ReplaceVarDecls extends SLIRReplacingVisitor {

    public static JStatement replaceIntDecls(JStatement body) {
	ReplaceVarDecls obj = new ReplaceVarDecls();
	return (JStatement)body.accept(obj);
    }

    HashMap var_names; // String (Ident) -> Integer

    int int_count;

    ReplaceVarDecls() {
	int_count = 0;
	var_names = new HashMap();
    }

    
    public Object visitVariableDeclarationStatement(JVariableDeclarationStatement self,
						    JVariableDefinition[] vars) 
    {

	LinkedList new_vars = new LinkedList();

	int i;

	for (i = 0; i < vars.length; i++) {
	    
	    if (vars[i].getType().getTypeID() == CType.TID_INT) {

		String ident = vars[i].getIdent();
		assert (!var_names.containsKey(ident));
		var_names.put(ident, new Integer(int_count++));
		
	    } else {

		new_vars.add(vars[i]);
	    }
	}

	JVariableDefinition new_array[] = new JVariableDefinition[new_vars.size()];

	i = 0;
	for (ListIterator li = new_vars.listIterator(); li.hasNext(); ) {
	    new_array[i++] = (JVariableDefinition)li.next();
	}

	self.setVars(new_array);
	return self;
    }

    
    public Object visitLocalVariableExpression(JLocalVariableExpression self,
					       String ident) {

	if (self.getType().getTypeID() == CType.TID_INT) {

	    assert (var_names.containsKey(ident));
	    Integer name = (Integer)var_names.get(ident);
	    
	    JVariableDefinition var = new JVariableDefinition(null, 
		 0, CStdType.Integer, "int_"+name.intValue(), null);
	    
	    return new JLocalVariableExpression(null, var);
	}

	return self;

    }

}

*/
