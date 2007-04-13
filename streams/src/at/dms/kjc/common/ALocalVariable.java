package at.dms.kjc.common;

import at.dms.kjc.*;

/**
 * Package up declaration statement and use expression for a local variable. 
 * @author dimock
 */
public class ALocalVariable {

    /**
     * Create a new local variable, don't care about name so long as it is likely to be unique.
     * @param typ type for variable
     * @return {@link ALocalVariable} object with statement for declaration, expression for use.
     */
    public static ALocalVariable makeTmp(CType typ) {
        return new ALocalVariable(at.dms.kjc.sir.lowering.ThreeAddressCode.nextTemp(),typ);
    }
    
    /**
     * Create a new local variable with a specified name and type.
     * @param typ type for variable
     * @param varName name for variable
     * @return {@link ALocalVariable} object with statement for declaration, expression for use.
     */
    public static ALocalVariable makeVar(CType typ, String varName) {
        return new ALocalVariable(varName,typ);
    }

    private JVariableDefinition varDefn;
    private JVariableDeclarationStatement varDecl;
    private JLocalVariableExpression varRef;
        
    private ALocalVariable(String varName,CType typ) {
        varDefn = new JVariableDefinition(typ,varName);
        varDecl = new JVariableDeclarationStatement(varDefn);
        varRef = new JLocalVariableExpression(varDefn);
    }

    /**
     * Change variable name.
     * @param varName the new variable name to set (affects declaration and references)
     */
    public void setVarName(String varName) {
        varDefn.setIdent(varName);
    }

    /**
     * Get variable name
     * @return the variable name
     */
    public String getVarName() {
        return varDefn.getIdent();
    }
    
    /**
     * Set the initial value of the variable.
     * @param initExpr expression that will be produced for the variable declaration.
     */
    public void setVarInitializer(JExpression initExpr) {
        varDefn.setInitializer(initExpr);
    }
    
    /**
     * Get the expression for the initial value of the variable.
     * @return expression, will be null unless {@link #setVarInitializer(JExpression)} has been called (or equivalent hacking)
     */
    public JExpression getVarInitializer() {
        return varDefn.getValue();
    }

    /**
     * @return the variable declaration statement.
     */
    public JVariableDeclarationStatement getVarDecl() {
        return varDecl;
    }

    /**
     * @return expression referencing the variable.
     */
    public JLocalVariableExpression getVarRef() {
        return varRef;
    }
    
    /**
     * Get the definition of the variable (allowing hacking name, type, initializer...).
     * @return definition of variable.
     */
    public JVariableDefinition getVarDefn() {
        return varDefn;
    }
    
    
}
