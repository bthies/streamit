package at.dms.kjc.spacetime;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import at.dms.kjc.*;
/**
 * This class will generate code to allocate the off chip buffers on the necessary
 * tiles and then communicate each buffer's global address to the tiles that need
 * to know the address.  It also sets the rotation length for the buffers based on the 
 * primepump schedule.
 * 
 * @author mgordon
 *
 */
public class CommunicateAddrs 
{
    public static String functName = "__snd_rvc_addrs__";
    public static String freeFunctName = "__free_init_bufs__";
    private static CommunicateAddrs commAddrs;
    private RawChip chip;
    private HashMap functions;
    private HashMap freeFunctions;
    private HashMap fields;
    private static Random rand;
    private SpaceTimeSchedule spaceTimeSchedule;         
    
    static 
    {
        rand = new Random(17);
    }
    
    
    public static String getFields(RawTile tile) 
    {
        return ((StringBuffer)commAddrs.fields.get(tile)).toString();
    }

    public CommunicateAddrs(RawChip chip, SpaceTimeSchedule stSchedule)
    {
        this.chip = chip;
        fields = new HashMap();
        functions = new HashMap();
        freeFunctions = new HashMap();
        spaceTimeSchedule = stSchedule;
    
        //add the StringBuffer for each tile
        for (int x = 0; x < chip.getXSize(); x++) {
            for (int y = 0; y < chip.getYSize(); y++) {
                RawTile tile = chip.getTile(x, y);
                functions.put(tile, new StringBuffer());
                freeFunctions.put(tile, new StringBuffer());
                fields.put(tile, new StringBuffer());
            }
        }
    
        Iterator buffers = OffChipBuffer.getBuffers().iterator();
        //iterate over the buffers and communicate each buffer
        //address from its declaring tile to the tile neighboring
        //the dram it is assigned to
        while (buffers.hasNext()) {
            OffChipBuffer buffer = (OffChipBuffer)buffers.next();
            //set the rotation length for the buffer
            if (buffer instanceof InterTraceBuffer)
                setRotationLength((InterTraceBuffer)buffer);
            
            //do nothing for redundant buffers
            if (buffer.redundant())
                continue;
        
            //the dram this buffer is mapped to
            StreamingDram dram = buffer.getDRAM();
            //the tiles that are mapped to this dram
            RawTile[] dramTiles = dram.getTiles();
            //the tile we are going to allocate this buffer on
            RawTile allocatingTile = null; 
            //if the neighboring tile is part of dramTiles
            //choose it for the allocating tile so we do not have
            //to communicate the address
            for (int i = 0; i < dramTiles.length; i++) {
                if (dramTiles[i] == dram.getNeighboringTile()) {
                    allocatingTile = dramTiles[i];
                    break;
                }   
            }
        
            //we could not allocate the buffer on the neighbor 
            //randomly pick a tile to allocate the buffer on
            if (allocatingTile == null) {
                allocatingTile = dramTiles[rand.nextInt(dramTiles.length)];
            }
            
            int rotationLength = buffer.getRotationLength();
                    
            //set the allocating tile to have compute code
            allocatingTile.setComputes();
            
            for (int i = 0; i < rotationLength; i++) {
                //allocate the steady buffer on the allocating tile
                ((StringBuffer)fields.get(allocatingTile)).append
                (buffer.getType().toString() + "* " + 
                        buffer.getIdent(i) + ";\n");
                
                //malloc the steady buffer
                ((StringBuffer)functions.get(allocatingTile)).append
                ("  " + buffer.getIdent(i) + " = (" + buffer.getType() + 
                        "*) malloc(32 + (" + buffer.getSize().toString() + " * sizeof(" +
                        buffer.getType() + ")));\n");
                //align the buffer
                ((StringBuffer)functions.get(allocatingTile)).append
                ("  " + buffer.getIdent(i) + " = ((u_int32_t)((char*)" + buffer.getIdent(i) +
                ") + 31) & 0xffffffe0;\n");
                
                //if allocator != neighbor, create declaration of 
                //pointer on neighbor and communicate the address for both init and steady...
                if (allocatingTile != dram.getNeighboringTile()) {
                    dram.getNeighboringTile().setComputes();
                    SpaceTimeBackend.println("Need to communicate buffer address from " + 
                            allocatingTile + " to " + dram.getNeighboringTile());
                    //generate the switch code to send the addresses
                    RawTile[] dest = {dram.getNeighboringTile()};
                    
                    //now for the steady
                    SwitchCodeStore.generateSwitchCode(allocatingTile, 
                            dest, 0);
                    
                    
                    //add the code to the owner to send the address to the
                    //static net for the steady
                    ((StringBuffer)functions.get(allocatingTile)).append
                    ("  " + Util.staticNetworkSendPrefix(CStdType.Integer) + 
                            buffer.getIdent(i) + 
                            Util.staticNetworkSendSuffix() + ";\n");
                    
                    
                    //add declaration of pointer to neighbor (steady)
                    ((StringBuffer)fields.get(dram.getNeighboringTile())).append
                    (buffer.getType().toString() + "* " + 
                            buffer.getIdent(i) + ";\n");
                    
                    
                    //add the code to receive the address into the pointer (steady)
                    ((StringBuffer)functions.get(dram.getNeighboringTile())).append
                    ("  " + Util.staticNetworkReceivePrefix() + 
                            buffer.getIdent(i) + 
                            Util.staticNetworkReceiveSuffix(CStdType.Integer) + ";\n");
                }
            }
        }
    }
    
    public static String getFreeFunction(RawTile tile) 
    {
        StringBuffer buf = new StringBuffer();

        //prepend the function name 
        buf.append("\nvoid " + freeFunctName + "() {\n");
        //append the closing } and 
        buf.append((StringBuffer)commAddrs.freeFunctions.get(tile));
        buf.append("}\n");
        return buf.toString();
    }
    

    public static String getFunction(RawTile tile) 
    {
        StringBuffer buf = new StringBuffer();

        //prepend the function name 
        buf.append("\nvoid " + functName + "() {\n");
        //append the closing } and 
        buf.append((StringBuffer)commAddrs.functions.get(tile));
        buf.append("}\n");
        return buf.toString();
    }   
    
    /**
     * Set the rotation length of the buffer based on the multiplicities 
     * of the source trace and the dest trace in the prime pump schedule and add one
     * so we can double buffer also!
     * 
     * @param buffer
     */
    private void setRotationLength(InterTraceBuffer buffer) {
        int sourceMult = spaceTimeSchedule.getPrimePumpMult(buffer.getSource().getParent());
        int destMult = spaceTimeSchedule.getPrimePumpMult(buffer.getDest().getParent());
        int length =  0;
        if (sourceMult != 0 && destMult != 0)
            length = sourceMult - destMult + 1; 
      
        buffer.setRotationLength(length);
    }
    
    public static void doit(RawChip chip, SpaceTimeSchedule stSchedule) 
    {
        commAddrs = new CommunicateAddrs(chip, stSchedule);
    }
}
