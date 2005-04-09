package at.dms.kjc.common;

/**
 * A CodeGenerator visits each node of the tree and prints to the
 * output file.  (Inspired by passes that need to interface to both
 * uniprocessor and Raw-type backends.)
 */
public interface CodeGenerator extends at.dms.kjc.SLIRVisitor {
    void print(String s);    
}
