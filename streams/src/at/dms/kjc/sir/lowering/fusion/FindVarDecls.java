
package at.dms.kjc.sir.lowering.fusion;

import java.util.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;

// Finds variable definitions and accesses that have type "Int" or "Float" 
// and replaces them with numbered variables. When fusing multiple operators
// this allows operators to reuse variables.

// calling procedure:
//    FindVarDecls findVarDecls = new FindVarDecls();
//
//    for each operator to be fused
//         block = (JBlock)findVarDecls.findAndReplace(block);
// 
//    findVarDecls.addVariableDeclarations(newBlock); 

public class FindVarDecls extends SLIREmptyVisitor {

    private int max_int_count;
    private int max_float_count;

    private int int_count;
    private int float_count;

    private HashMap<JVariableDefinition, Integer> var_names; // JVariableDefinition -> Integer

    private HashMap<Integer, JVariableDefinition> ints; // Integer -> JVariableDefinition
    private HashMap<Integer, JVariableDefinition> floats; // Integer -> JVariableDefinition
    
    public FindVarDecls() { 
        max_int_count = 0;
        max_float_count = 0;
        ints = new HashMap<Integer, JVariableDefinition>();
        floats = new HashMap<Integer, JVariableDefinition>();
    }

    public void newOperator() {
        int_count = 0;
        float_count = 0;
        var_names = new HashMap<JVariableDefinition, Integer>();
    }
    
    // reset tells if this operator should be assigned new variables

    public JStatement findAndReplace(JStatement body) {
    
        //if (reset)
        newOperator();

        //int_count = 0;
        //float_count = 0;
        //var_names = new HashMap();
        body.accept(this);
        //System.out.println("Found ints:"+int_count+" floats:"+float_count);
        if (int_count > max_int_count) { max_int_count = int_count; }
        if (float_count > max_float_count) { max_float_count = float_count; }
        ReplaceVarDecls replace = new ReplaceVarDecls(var_names, this);
        JBlock new_body = (JBlock)body.accept(replace);
        return new_body;
    }

    public int getMaxIntCount() { return max_int_count; }

    public int getMaxFloatCount() { return max_float_count; }
    
    public JVariableDefinition getIntVar(Integer index) { 
        if (!ints.containsKey(index)) {
            JVariableDefinition var = new JVariableDefinition(null, 
                                                              0, CStdType.Integer, "__int_"+index.toString(), null);
            ints.put(index, var);
            return var;
        }
        return ints.get(index);
    }

    public JVariableDefinition getFloatVar(Integer index) { 
        if (!floats.containsKey(index)) {
            JVariableDefinition var = new JVariableDefinition(null, 
                                                              0, CStdType.Float, "__float_"+index.toString(), null);
            floats.put(index, var);
            return var;
        }
        return floats.get(index);
    }

    public void visitVariableDeclarationStatement(JVariableDeclarationStatement self,
                                                  JVariableDefinition[] vars) {
        for (int i = 0; i < vars.length; i++) {
            CType type = vars[i].getType();

            if (type.isArrayType()) continue;

            if (type.getTypeID() == CType.TID_INT) {
                var_names.put(vars[i],new Integer(int_count));
                int_count++;
            }
            if (type.getTypeID() == CType.TID_FLOAT) {
                var_names.put(vars[i],new Integer(float_count));
                float_count++;
            }
        }
    }

    public void addVariableDeclarations(JBlock block) {

        for (int i = 0; i < getMaxIntCount(); i++) {
            JVariableDefinition var = getIntVar(new Integer(i));
            block.addStatementFirst(
                                    new JVariableDeclarationStatement(null, var, null));
        }

        for (int i = 0; i < getMaxFloatCount(); i++) {
            JVariableDefinition var = getFloatVar(new Integer(i));
            block.addStatementFirst(
                                    new JVariableDeclarationStatement(null, var, null));
        }
    }

}

