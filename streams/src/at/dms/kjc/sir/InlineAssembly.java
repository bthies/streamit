package at.dms.kjc.sir;

import at.dms.kjc.*;
import at.dms.util.Utils;
import java.util.ArrayList;
//import at.dms.compiler.PositionedError;
//import at.dms.compiler.TokenReference;
//import at.dms.compiler.JavaStyleComment;

/**
 */
public class InlineAssembly extends JStatement {
    //private String asm;
    private ArrayList inst;
    private ArrayList input;
    private ArrayList clobber;

    public InlineAssembly() {
	super(null,null);
	//this.asm=asm;
	inst=new ArrayList();
	input=new ArrayList();
	clobber=new ArrayList();
    }
    
    public InlineAssembly(String asm) {
	super(null,null);
	//this.asm=asm;
	inst=new ArrayList();
	inst.add(asm);
	input=new ArrayList();
	clobber=new ArrayList();
    }
    
    public void add(String instr) {
	inst.add(instr);
    }

    public void addInput(String input) {
	this.input.add(input);
    }

    public void addClobber(String clobber) {
	this.clobber.add(clobber);
    }
    
    public String[] getInstructions() {
	String[] out=new String[inst.size()];
	inst.toArray(out);
	return out;
    }

    public String[] getInput() {
	String[] out=new String[input.size()];
	input.toArray(out);
	return out;
    }

    public String[] getClobber() {
	String[] out=new String[clobber.size()];
	clobber.toArray(out);
	return out;
    }

    public Object accept(AttributeVisitor p) {
	//Utils.fail("accept(AttributeVisitor) not supported by InlineAssembly");
	return null;
    }

    public void genCode(CodeSequence code) {
	Utils.fail("genCode(CodeSequence) not supported by InlineAssembly");
    }

    public void analyse(CBodyContext context) {
	Utils.fail("analyse(CBodyContext) not supported by InlineAssembly");
    }

    public void accept(KjcVisitor p) {
	if(p instanceof SLIREmptyVisitor)
	    ((SLIREmptyVisitor)p).visitInlineAssembly(this,getInstructions(),getInput(),getClobber());
    }
}




