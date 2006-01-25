package at.dms.kjc.common;

/**
 * A CodeGenerator visits each node of the tree and prints to the
 * output file.  (Inspired by passes that need to interface to both
 * uniprocessor and Raw-type backends.)
 */
public interface CodeGenerator extends at.dms.kjc.SLIRVisitor {
    /** 
     * 
     * @return a printer for interleaving code created inside code generators
     *         with code created outside of descendents of classes implementing
     *         CodeGenerator.
     */
    public CodegenPrintWriter getPrinter();
}
