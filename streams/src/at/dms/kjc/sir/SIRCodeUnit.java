package at.dms.kjc.sir;

import at.dms.kjc.*;

/**
 * Interface of accessors for classes that define methods and field 
 * variables.  It is useful for optimization and code transformation passes
 * operate on the IR of multiple backends.
 * 
 * @author mgordon
 *
 */
public interface SIRCodeUnit {

    /**
     * Return the methods of this unit.
     * 
     * @return the methods of this unit.
     */
    public JMethodDeclaration[] getMethods();
    /**
     * Add this method to the end of the methods array.
     * 
     * Do not check for duplication.
     * 
     * @param method The method to add.
     */
    public void addMethod(JMethodDeclaration method);
    /**
     * Adds methods to the methods of this if it not already in the 
     * methods container.
     * 
     * @param methods The methods to attempt to add to the end of the methods.
     */
    public void addMethods(JMethodDeclaration[] methods);
    /**
     * Install methods as the new methods array.  Disregarding the 
     * old methods array.
     * 
     * @param methods The new methods to install.
     */
    public void setMethods(JMethodDeclaration[] methods);
    
    
    /**
     * Return the fields of this unit.
     * @return the fields of this unit.
     */
    public JFieldDeclaration[] getFields();
    /**
     * Adds field to this, if field is not already registered
     * as a field of this. 
     * 
     * @param field The field to attempt to add.
     */
    public void addField(JFieldDeclaration field);
   
    /**
     * Adds fields to the end fields of this. 
     * 
     * Does not check for duplicates.
     * 
     * @param fields The fields to add to the end.
     */
    public void addFields(JFieldDeclaration[] fields);
    
    /**
     * Set the fields of this to fields, overwriting the old 
     * array.
     * 
     * @param fields The fields to install.
     */
    public void setFields(JFieldDeclaration[] fields);
    
}
