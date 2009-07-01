package at.dms.kjc.smp;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import at.dms.kjc.*;
import at.dms.kjc.slicegraph.*;

/**
 * This class models the rotating structure that is needed when a core uses double buffered communication
 * and rotating buffers.  A rotating structure of addresses is kept at a source core that stores
 * the addresses of the rotating buffers of the source core so that we can write to 
 * the appropriate location in memory in the destination's core.
 * 
 * @author mgordon
 *
 */
public class SourceAddressRotation extends RotatingBuffer {
    /** The InputBuffer this rotation models */
    protected InputRotatingBuffer inputBuffer;
    
    /** the name of the write rotation structure (always points to its head) */
    protected String writeRotStructName;
    /** the name of the pointer to the current write rotation of this buffer */
    protected String currentWriteRotName;
    /** the name of the pointer to the write buffer of the current rotation */
    protected String currentWriteBufName;
    
    /** 
     * Number of times each InputRotatingBuffer has been seen.  Used to create
     * SourceAddressRotations with unique idents
     */
    private static HashMap<InputRotatingBuffer, Integer> bufCount =
    	new HashMap<InputRotatingBuffer, Integer>();
    
    public SourceAddressRotation(Core core, InputRotatingBuffer buf, FilterSliceNode dest, Edge edge) {
        super(edge, dest, core);
        this.parent = core;
        this.inputBuffer = buf;
        
        setBufferSize();
        this.bufType = buf.bufType;
        this.rotationLength = buf.getRotationLength();
        
        Integer count = bufCount.get(buf);
        if(count == null)
        	count = new Integer(0);
        bufCount.put(buf, count + 1);
        
        this.ident = buf.getIdent() + "_addr_";
        
        int coreNum = parent.getCoreID();
        writeRotStructName = this.getIdent() + "rot_struct__" + count.intValue() + "__n" + coreNum;
        currentWriteRotName = this.getIdent() + "write_current__" + count.intValue() + "__n" + coreNum;
        currentWriteBufName = this.getIdent() + "_write_buf__" + count.intValue() + "__n" + coreNum;
        
        //set the names of the individual address buffers that constitute this rotation
        setBufferNames();
    }

    public InputRotatingBuffer getIntputRotatingBuffer() {
        return inputBuffer;
    }
    
    public void setBufferSize() {
        if (inputBuffer == null)
            return;
        bufSize = inputBuffer.getBufferSize();
    }
    
    public void declareBuffers() {
        for (int i = 0; i < rotationLength; i++) {
            
            CoreCodeStore cs = this.parent.getComputeCode();
            
            //create the pointer that will point to the buffer constituent on the dest core
            //cs.addStatementToBufferInit(new JExpressionStatement(new JEmittedTextExpression("// Generated in SourceAddressRotation.declareBuffers()")));
            cs.addStatementToBufferInit(new JExpressionStatement(new JEmittedTextExpression(this.getType().toString() + "* " + 
                    bufferNames[i])));
        }
    }
    
    /**
     * Generate the code to setup the structure of the rotating buffer 
     * as a circular linked list.
     */
    protected void setupRotation() {
        String temp = "__temp__";
        CoreCodeStore cs = parent.getComputeCode();
        //this is the typedef we will use for this buffer rotation structure
        String rotType = rotTypeDefPrefix + getType().toString();
        
        //add the declaration of the rotation buffer of the appropriate rotation type
        parent.getComputeCode().appendTxtToGlobal(rotType + " *" + writeRotStructName + ";\n");
        //add the declaration of the pointer that points to the current rotation in the rotation structure
        parent.getComputeCode().appendTxtToGlobal(rotType + " *" + currentWriteRotName + ";\n");
        //add the declaration of the pointer that points to the current buffer in the current rotation
        parent.getComputeCode().appendTxtToGlobal(bufType.toString() + " *" + currentWriteBufName + ";\n");
        
        JBlock block = new JBlock();
        
        //create a temp var
        if (this.rotationLength > 1)
            block.addStatement(Util.toStmt(rotType + " *" + temp));
        
        //create the first entry!!
        block.addStatement(Util.toStmt(writeRotStructName + " =  (" + rotType+ "*)" + "malloc(sizeof("
                + rotType + "))"));
        
        //modify the first entry
        block.addStatement(Util.toStmt(writeRotStructName + "->buffer = " + bufferNames[0]));
        if (this.rotationLength == 1) 
            block.addStatement(Util.toStmt(writeRotStructName + "->next = " + writeRotStructName));
        else {
            block.addStatement(Util.toStmt(temp + " = (" + rotType+ "*)" + "malloc(sizeof("
                    + rotType + "))"));    
            
            block.addStatement(Util.toStmt(writeRotStructName + "->next = " + 
                    temp));
            
            block.addStatement(Util.toStmt(temp + "->buffer = " + bufferNames[1]));
            
            for (int i = 2; i < this.rotationLength; i++) {
                block.addStatement(Util.toStmt(temp + "->next =  (" + rotType+ "*)" + "malloc(sizeof("
                        + rotType + "))"));
                block.addStatement(Util.toStmt(temp + " = " + temp + "->next"));
                block.addStatement(Util.toStmt(temp + "->buffer = " + bufferNames[i]));
            }
            
            block.addStatement(Util.toStmt(temp + "->next = " + writeRotStructName));
        }
        block.addStatement(Util.toStmt(currentWriteRotName + " = " + writeRotStructName));
        block.addStatement(Util.toStmt(currentWriteBufName + " = " + currentWriteRotName + "->buffer"));
        cs.addStatementToBufferInit(block);
    }
    

    protected List<JStatement> rotateStatements() {
        LinkedList<JStatement> list = new LinkedList<JStatement>();
        if (rotationLength > 1) {
            list.add(Util.toStmt(currentWriteRotName + " = " + currentWriteRotName + "->next"));
            list.add(Util.toStmt(currentWriteBufName + " = " + currentWriteRotName + "->buffer"));
        }
        return list;
    }
    
    public JFieldAccessExpression writeBufRef() {
        assert false;
        return null;
    }

	public JArrayAccessExpression readBufRef(JExpression offset) {
		assert false;
		return null;
	}
}
