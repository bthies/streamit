package at.dms.kjc.tilera;

import at.dms.kjc.JBlock;
import at.dms.kjc.JEmittedTextExpression;
import at.dms.kjc.JExpressionStatement;
import at.dms.kjc.slicegraph.*;

/**
 * This class models the rotating structure that is needed when a tile uses double buffered communication
 * and rotating buffers.  A rotating structure of addresses is kept at a source tile that stores
 * the addresses of the rotating buffers of the source tile so that the dma commands can write to 
 * the appropriate location in memory in the destination's tile.
 * 
 * @author mgordon
 *
 */
public class SourceAddressRotation extends RotatingBuffer {
    /** The InputBuffer this rotation models */
    protected InputRotatingBuffer inputBuffer;
    
    public SourceAddressRotation(Tile tile, InputRotatingBuffer buf, FilterSliceNode dest, Edge edge) {
        super(edge, dest, tile);
        this.parent = tile;
        this.inputBuffer = buf;
        setBufferSize();
        bufType = buf.bufType;
        this.ident = buf.getIdent() + "_addr_";
        writeRotStructName = this.getIdent() + "rot_struct";
        currentWriteRotName = this.getIdent() + "write_current";
        currentWriteBufName = this.getIdent() + "_write_buf";
        this.rotationLength = buf.getRotationLength();
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
            
            TileCodeStore cs = this.parent.getComputeCode();
            
            //create the pointer that will point to the buffer constituent on the dest tile
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
        TileCodeStore cs = parent.getComputeCode();
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
        block.addStatement(endOfRotationSetup());
        cs.addStatementToBufferInit(block);
    }
}
