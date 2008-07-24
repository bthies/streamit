package at.dms.kjc.tilera;

import at.dms.kjc.JEmittedTextExpression;
import at.dms.kjc.JExpressionStatement;
import at.dms.kjc.slicegraph.*;

/**
 * This class models the rotating structure that is needed when a tile uses DMA communication
 * and rotating buffers.  A rotating structure of addresses is kept at a source tile that stores
 * the addesses of the rotating buffers of the source tile so that the dma commands can write to 
 * the appropriate location in memory in the destination's tile.
 * 
 * @author mgordon
 *
 */
public class DMAAddressRotation extends RotatingBuffer {
    /** The InputBuffer this rotation models */
    protected InputRotatingBuffer inputBuffer;
    
    public DMAAddressRotation(Tile tile, InputRotatingBuffer buf, FilterSliceNode dest, Edge edge) {
        super(edge, dest, tile);
        this.parent = tile;
        this.inputBuffer = buf;
        setBufferSize();
        bufType = buf.bufType;
        this.ident = buf.getIdent() + "_addr_";
        rotStructName = this.getIdent() + "rot_struct";
        currentRotName = this.getIdent() + "rot_current";
        currentBufName = this.getIdent() + "_cur_buf";
        this.rotationLength = buf.getRotationLength();
        //set the names of the individual address buffers that constitute this rotation
        setBufferNames();

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
}
