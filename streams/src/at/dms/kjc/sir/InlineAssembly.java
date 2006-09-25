package at.dms.kjc.sir;

import at.dms.kjc.*;
import at.dms.util.Utils;
import java.util.ArrayList;
//import at.dms.compiler.PositionedError;
//import at.dms.compiler.TokenReference;
//import at.dms.compiler.JavaStyleComment;

/**
 * Statement used for generating inline assembly. Leverages gcc's asm
 * volatile keyword. Mostly holds Strings that are passed directly to
 * codegen but is structured slightly to allow automatic generation of
 * the appropriate asm volatile statement. See spacetime/TraceIRtoC
 * for example of how the output should be formatted. May hold a whole
 * block of assembly instructions.
 * @author jasperln
 */
public class InlineAssembly extends JStatement {
    private ArrayList<String> inst; //List of assembly instructions
    private ArrayList<String> input; //List of input (for loading/storing to mem)
    private ArrayList<String> clobber; //Registers that this instruction clobbers

    /**
     * Construct InlineAssembly containing no instructions.
     */
    public InlineAssembly() {
        super(null,null);
        inst=new ArrayList<String>();
        input=new ArrayList<String>();
        clobber=new ArrayList<String>();
    }
    
    /**
     * Construct InlineAssembly containing the initial instruction asm.
     * @param asm The initial instruction to include.
     */
    public InlineAssembly(String asm) {
        super(null,null);
        inst=new ArrayList<String>();
        inst.add(asm);
        input=new ArrayList<String>();
        clobber=new ArrayList<String>();
    }
    
    /**
     * Add single instruction.
     * @param instr The instruction to add.
     */
    public void add(String instr) {
        inst.add(instr);
    }

    /**
     * Add input to instruction block. Input may be label of memory
     * location. Inputs should be added in order they are used in
     * instruction block.
     * @param input The input label to add.
     */
    public void addInput(String input) {
        this.input.add(input);
    }

    /**
     * Add register clobbered by this instruction block. This is to
     * allow gcc to register allocate correctly.
     * @param clobber The register to add.
     */
    public void addClobber(String clobber) {
        this.clobber.add(clobber);
    }
    
    /**
     * Returns list of instructions.
     */
    public String[] getInstructions() {
        String[] out=new String[inst.size()];
        inst.toArray(out);
        return out;
    }

    /**
     * Returns list of input labels.
     */
    public String[] getInput() {
        String[] out=new String[input.size()];
        input.toArray(out);
        return out;
    }

    /**
     * Returns list of clobbered registers.
     */
    public String[] getClobber() {
        String[] out=new String[clobber.size()];
        clobber.toArray(out);
        return out;
    }

    /**
     * Dummy method to accept AttributeVisitor but not do
     * anything. InlineAssembly should be blackbox to everyone but
     * codegen.
     */
    public Object accept(AttributeVisitor p) {
        //Utils.fail("accept(AttributeVisitor) not supported by InlineAssembly");
        return null;
    }

    /**
     * Dummy method for genCode(CodeSequence). This is not needed by
     * InlineAssembly.
     */
    public void genCode(CodeSequence code) {
        Utils.fail("genCode(CodeSequence) not supported by InlineAssembly");
    }

    /**
     * Dummy method for analyse(CBodyContext context). This is not
     * needed by InlineAssembly.
     */
    public void analyse(CBodyContext context) {
        Utils.fail("analyse(CBodyContext) not supported by InlineAssembly");
    }

    /**
     * Main entry point for KjcVisitor. For SLIREmptyVisitor, forwards
     * to
     * p.visitInlineAssembly(this,getInstructions(),getInput(),getClobber()).
     */
    public void accept(KjcVisitor p) {
        if(p instanceof SLIREmptyVisitor)
            ((SLIREmptyVisitor)p).visitInlineAssembly(this,getInstructions(),getInput(),getClobber());
    }
}
