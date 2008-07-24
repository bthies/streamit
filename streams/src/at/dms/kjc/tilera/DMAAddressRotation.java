package at.dms.kjc.tilera;

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
    /** The tile this rotation structure is declared on */
    protected Tile tile;
    /** The InputBuffer this rotation models */
    protected InputRotatingBuffer inputBuffer;
    
    public DMAAddressRotation(Tile tile, InputRotatingBuffer buf, FilterSliceNode dest, Edge edge) {
        super(edge, dest, tile);
        this.tile = tile;
        this.inputBuffer = buf;
        setBufferSize();
        bufType = buf.bufType;
        rotStructName = buf.getIdent() + "_addr_rot_struct";
        currentRotName = buf.getIdent() + "_addr_rot_current";
        currentBufName = buf.getIdent() + "_cur_addr_buf";
        //set the names of the individual address buffers that constitute this rotation
        setBufferNames();
    }
    
    public void setBufferSize() {
        if (inputBuffer == null)
            return;
        bufSize = inputBuffer.getBufferSize();
    }
}
