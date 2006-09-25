
package at.dms.kjc.sir.lowering.fusion;

import java.util.*;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;


public class ReplaceVarDecls extends SLIRReplacingVisitor {
    
    private HashMap<JVariableDefinition, Integer> var_names; // String (Ident) -> Integer
    private FindVarDecls find_obj;

    ReplaceVarDecls(HashMap<JVariableDefinition, Integer> var_names, FindVarDecls find_obj) {
        this.var_names = var_names;
        this.find_obj = find_obj;
    }

    public Object visitVariableDeclarationStatement(JVariableDeclarationStatement self,
                                                    JVariableDefinition[] vars) 
    {
    
        LinkedList<JVariableDefinition> new_vars = new LinkedList<JVariableDefinition>();
        LinkedList<JExpressionStatement> new_statements = new LinkedList<JExpressionStatement>();
    
        for (int i = 0; i < vars.length; i++) {

            if (!var_names.containsKey(vars[i])) {
                // the variable has not been eliminated

                // if statement declares only one variable return the statement
                if (vars.length == 1) return self;

                // otherwise add the variable to the list
                new_vars.add(vars[i]);

            } else {

                // the variable has been eliminated
                if (vars[i].getType().getTypeID() == CType.TID_INT) {
                    if (vars[i].getValue() != null) {           
                        Integer name = 
                            var_names.get(vars[i]);
                        JVariableDefinition var = find_obj.getIntVar(name); 
                        JLocalVariableExpression var_expr = new JLocalVariableExpression(null, var); 
                        JExpression expr = new JAssignmentExpression(null, var_expr, vars[i].getValue());
                        new_statements.addLast(new JExpressionStatement(null, expr, null));
                    }
                }
        
        
                if (vars[i].getType().getTypeID() == CType.TID_FLOAT) {
                    if (vars[i].getValue() != null) {           
                        Integer name = 
                            var_names.get(vars[i]);
                        JVariableDefinition var = find_obj.getFloatVar(name);
                        JLocalVariableExpression var_expr = new JLocalVariableExpression(null, var); 
                        JExpression expr = new JAssignmentExpression(null, var_expr, vars[i].getValue());
                        new_statements.addLast(new JExpressionStatement(null, expr, null));
                    }
                }
            }
        
        }

        // make sure that all variables are either renamed or none is renamed
        // this is because Unroller/VarDeclRaiser do not correctly handle
        // varaible declarations inside of a JCompoundStatement, so
        // we must return a JVariableDeclarationStatement or a JCompoundStatement
        // that does not contain declarations.
    
        assert (new_vars.size() == 0 || new_statements.size() == 0);

        if (new_vars.size() > 0) {

            JVariableDefinition new_array[] = new_vars.toArray(new JVariableDefinition[0]);
            self.setVars(new_array);
            return self;

        } else {

            JExpressionStatement new_array[] = new_statements.toArray(new JExpressionStatement[0]);
            return new JCompoundStatement(null, new_array);

        }
    }

    
    public Object visitLocalVariableExpression(JLocalVariableExpression self,
                                               String ident) {

        if (var_names.containsKey(self.getVariable())) {

            // variable has been eliminated
            if (self.getType().getTypeID() == CType.TID_INT) {
                Integer name = var_names.get(self.getVariable());
                JVariableDefinition var = find_obj.getIntVar(name);
                return new JLocalVariableExpression(null, var);
            }

            if (self.getType().getTypeID() == CType.TID_FLOAT) {
                Integer name = var_names.get(self.getVariable());
                JVariableDefinition var = find_obj.getFloatVar(name);
                return new JLocalVariableExpression(null, var);
            }
        }

        // variable has not been eliminated
        return self;
    }

}
