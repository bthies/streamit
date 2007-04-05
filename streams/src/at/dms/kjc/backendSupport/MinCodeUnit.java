package at.dms.kjc.backendSupport;

import java.util.HashMap;

import at.dms.kjc.CType;
import at.dms.kjc.JFieldDeclaration;
import at.dms.kjc.JMethodDeclaration;
import at.dms.kjc.lir.LIRStreamType;
import at.dms.kjc.sir.AttributeStreamVisitor;
import at.dms.kjc.sir.SIRCodeUnit;
import at.dms.kjc.sir.SIRStream;

/**
 * Minimum usable implementation of SIRCodeUnit interface.
 * 
 * @author dimock
 *
 */
public class MinCodeUnit implements SIRCodeUnit {
    
    private class Unit extends SIRStream implements SIRCodeUnit {

        private static final long serialVersionUID = 6966018850839857336L;

        @Override
        public CType getInputType() {
            throw new AssertionError("unusable method");
        }

        @Override
        public CType getOutputType() {
            throw new AssertionError("unusable method");
        }

        @Override
        public int getPopForSchedule(HashMap[] counts) {
            throw new AssertionError("unusable method");
        }

        @Override
        public int getPushForSchedule(HashMap[] counts) {
            throw new AssertionError("unusable method");
        }

        @Override
        public LIRStreamType getStreamType() {
            throw new AssertionError("unusable method");
        }

        @Override
        public Object accept(AttributeStreamVisitor v) {
            throw new AssertionError("unusable method");
        }
        
    }
    
    private Unit theImplementation;
    
    /**
     * Create a new MinCodeUnit.
     * @param fields  Array of field declarations.  May be empty if adding later.
     * @param methods Array of method declarations.  May be empty if adding later.
     */
    public MinCodeUnit(JFieldDeclaration[] fields, JMethodDeclaration[]methods) {
        theImplementation = new Unit();
        theImplementation.setFields(fields);
        theImplementation.setMethods(methods);
    }

    public void addField(JFieldDeclaration field) {
        theImplementation.addField(field);
    }

    public void addFields(JFieldDeclaration[] fields) {
        theImplementation.addFields(fields);
    }

    public void addMethod(JMethodDeclaration method) {
        theImplementation.addMethod(method);
    }

    public void addMethods(JMethodDeclaration[] methods) {
        theImplementation.addMethods(methods);
    }

    public JFieldDeclaration[] getFields() {
        return theImplementation.getFields();
    }

    public JMethodDeclaration[] getMethods() {
        return theImplementation.getMethods();
    }

    public void setFields(JFieldDeclaration[] fields) {
        theImplementation.setFields(fields);
    }

    public void setMethods(JMethodDeclaration[] methods) {
        theImplementation.setMethods(methods);
    }
}

