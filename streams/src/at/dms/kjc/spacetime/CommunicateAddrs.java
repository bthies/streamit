package at.dms.kjc.spacetime;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import at.dms.kjc.*;
/**
 * This class will generate code to allocate the off chip buffers on the necessary
 * tiles and then communicate each buffer's global address to the tiles that need
 * to know the address.   It will also generate the typedefs for the rotating buffers
 * and the functions that will set up the structs for rotation (a circular linked list of 
 * buffers).
 *
 * It also sets the rotation length for the buffers based on the 
 * primepump schedule.  
 * 
 * @author mgordon
 *
 */
public class CommunicateAddrs 
{
    public static String rotationSetupFunction = "__setup_rotations__";
    public static String functName = "__snd_rvc_addrs__";
    public static String freeFunctName = "__free_init_bufs__";
    public static String rotTypeDefPrefix = "__rotating_buffer_";
    private static CommunicateAddrs commAddrs;
    private RawChip chip;
    private HashMap functions;
    private HashMap freeFunctions;
    private HashMap fields;
    private static Random rand;
    private SpaceTimeSchedule spaceTimeSchedule;         
    private HashMap rotationFunctions;
    /** The type defs for the rotating buffer structs for all the types used in
     * the program.
     */
    private static HashMap typedefs;
    
    static 
    {
        rand = new Random(17);
    }
    
    /**
     * @param tile
     * @return For <pre>tile</pre> return the fields that represent the buffers.
     */
    public static String getFields(RawTile tile) 
    {
        return ((StringBuffer)commAddrs.fields.get(tile)).toString();
    }

    /**
     * See class comments. 
     * 
     * @param chip
     * @param stSchedule
     */
    public CommunicateAddrs(RawChip chip, SpaceTimeSchedule stSchedule)
    {
        this.chip = chip;
        fields = new HashMap();
        functions = new HashMap();
        rotationFunctions = new HashMap();
        freeFunctions = new HashMap();
        spaceTimeSchedule = stSchedule;
        typedefs = new HashMap(); 
        
        //add the StringBuffer for each tile
        for (int x = 0; x < chip.getXSize(); x++) {
            for (int y = 0; y < chip.getYSize(); y++) {
                RawTile tile = chip.getTile(x, y);
                functions.put(tile, new StringBuffer());
                freeFunctions.put(tile, new StringBuffer());
                fields.put(tile, new StringBuffer());
                rotationFunctions.put(tile, new StringBuffer());
            }
        }
    
        Iterator buffers = OffChipBuffer.getBuffers().iterator();
        //iterate over the buffers and communicate each buffer
        //address from its declaring tile to the tile logically mapped to it
        //the dram it is assigned to
        while (buffers.hasNext()) {
            OffChipBuffer buffer = (OffChipBuffer)buffers.next();
            //do nothing for redundant buffers
            if (buffer.redundant())
                continue;
        
            //generate a typedef for the the rotating buffer of this type
            //if we have not seen this type before
            if (!typedefs.containsKey(buffer.getType())) {
                StringBuffer buf = new StringBuffer();
                buf.append("typedef struct __rotating_struct_" +
                        buffer.getType().toString() + "__" + 
                        " *__rot_ptr_" + buffer.getType().toString() + "__;\n");
                buf.append("typedef struct __rotating_struct_" + buffer.getType().toString() + "__ {\n");
                buf.append("\t" + buffer.getType().toString() + " *buffer;\n");
                buf.append("\t__rot_ptr_" + buffer.getType().toString() + "__ next;\n");
                buf.append("} " + rotTypeDefPrefix + buffer.getType().toString() + ";\n");
                
                typedefs.put(buffer.getType(), buf);
            }
            
            //the dram this buffer is mapped to
            StreamingDram dram = buffer.getDRAM();
            //the tile we are logically mapping this dram to
            RawTile homeTile = LogicalDramTileMapping.getOwnerTile(dram);
            //the tiles that are mapped to this dram by the hardware
            RawTile[] dramTiles = dram.getTiles();
            //the tile we are going to allocate this buffer on
            RawTile allocatingTile = null; 
            //if the owner tile is part of dramTiles
            //choose it for the allocating tile so we do not have
            //to communicate the address
            for (int i = 0; i < dramTiles.length; i++) {
                if (dramTiles[i] == homeTile) {
                    allocatingTile = dramTiles[i];
                    break;
                }   
            }
        
            //we could not allocate the buffer on the home tile 
            //randomly pick a tile to allocate the buffer on
            if (allocatingTile == null) {
                allocatingTile = dramTiles[rand.nextInt(dramTiles.length)];
            }
            
            int rotationLength = buffer.getRotationLength();
                    
            //set the allocating tile to have compute code
            allocatingTile.setComputes();
            
            //add the code necessary to set up the structure for rotation for this
            //rotating buffer 
            ((StringBuffer)rotationFunctions.get(homeTile)).append
            (setupRotation(buffer, homeTile));
            
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
                
                //if allocator != homeTile, create declaration of 
                //pointer on homeTile and communicate the address for both init and steady...
                if (allocatingTile != homeTile) {
                    homeTile.setComputes();
                    SpaceTimeBackend.println("Need to communicate buffer address from " + 
                            allocatingTile + " to " + homeTile);
                    //generate the switch code to send the addresses
                    RawTile[] dest = {homeTile};
                    
                    //now for the steady
                    SwitchCodeStore.generateSwitchCode(allocatingTile, 
                            dest, 0);
                    
                    
                    //add the code to the owner to send the address to the
                    //static net for the steady
                    ((StringBuffer)functions.get(allocatingTile)).append
                    ("  " + Util.networkSendPrefix(false, CStdType.Integer) + 
                            buffer.getIdent(i) + 
                            Util.networkSendSuffix(false) + ";\n");
                    
                    
                    //add declaration of pointer to hometile(steady)
                    ((StringBuffer)fields.get(homeTile)).append
                    (buffer.getType().toString() + "* " + 
                            buffer.getIdent(i) + ";\n");
                    
                    
                    //add the code to receive the address into the pointer (steady)
                    ((StringBuffer)functions.get(homeTile)).append
                    ("  " + buffer.getIdent(i) + " = " +  
                            Util.networkReceive(false, CStdType.Integer) + ";\n");
                }
                
                
            }
        }
    }
    
    private String setupRotation(OffChipBuffer buffer, RawTile tile) {
                
        StringBuffer buf = new StringBuffer();
        String temp = "__temp__";
        
        assert buffer.getRotationLength() > 0 : buffer;
        
        String rotType = rotTypeDefPrefix + buffer.getType().toString();
        
        
//      add the declaration of the source rotation buffer of the appriopriate rotation type
        ((StringBuffer)fields.get(tile)).append(rotTypeDefPrefix + 
                buffer.getType().toString() + " *" + buffer.getIdent(true) + ";\n");
        
//      add the declaration of the dest rotation buffer of the appriopriate rotation type
        ((StringBuffer)fields.get(tile)).append(rotTypeDefPrefix + 
                buffer.getType().toString() + " *" + buffer.getIdent(false) + ";\n");
        
        boolean[] vals = {true, false};
        
        //now generate the rotating buffer for both the source and the dest 
        for (int n = 0; n < 2; n++) {
            boolean read = vals[n];
            
            buf.append("{\n");
            //create a temp var
            buf.append("\t" + rotType + " *" + temp + ";\n");
            
            //create the first entry!!
            buf.append("\t" + buffer.getIdent(read) + " =  (" + rotType+ "*)" + "malloc(sizeof("
                    + rotType + "));\n");
            
            //modify the first entry
            buf.append("\t" + buffer.getIdent(read) + "->buffer = " + buffer.getIdent(0) + 
            ";\n");
            if (buffer.getRotationLength() == 1) 
                buf.append("\t" + buffer.getIdent(read) + "->next = " + buffer.getIdent(read) + 
                ";\n");
            else {
                buf.append("\t" + temp + " = (" + rotType+ "*)" + "malloc(sizeof("
                        + rotType + "));\n");    
                
                buf.append("\t" + buffer.getIdent(read) + "->next = " + 
                        temp + ";\n");
                
                buf.append("\t" + temp + "->buffer = " + buffer.getIdent(1) + ";\n");
                
                for (int i = 2; i < buffer.getRotationLength(); i++) {
                    buf.append("\t" + temp + "->next =  (" + rotType+ "*)" + "malloc(sizeof("
                            + rotType + "));\n");
                    buf.append("\t" + temp + " = " + temp + "->next;\n");
                    buf.append("\t" + temp + "->buffer = " + buffer.getIdent(i) + ";\n");
                }
                
                buf.append("\t" + temp + "->next = " + buffer.getIdent(read) + ";\n");
            }
            buf.append("}\n");
        }
        return buf.toString();
    }
    
    public static String getRotSetupFunct(RawTile tile) {
        StringBuffer buf = new StringBuffer();
        buf.append("\nvoid " + rotationSetupFunction + "() {\n");
        buf.append((StringBuffer)commAddrs.rotationFunctions.get(tile));
        buf.append("}\n");
        return buf.toString();
    }
    
    /**
     * @param tile
     * @return For <pre>tile</pre> return a function that will free the buffer memory.
     */
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
    

    /** 
     * @param tile
     * @return For <pre>tile</pre> return the function that will allocate the buffers or 
     * get the addresses from the static network.
     */
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
     * @return A string that has c code for the type defs for the
     * rotating buffer types. 
     */
    public static String getRotationTypes() {
        StringBuffer aggreg = new StringBuffer();
        Iterator types = typedefs.values().iterator();
        while (types.hasNext()) {
            StringBuffer buf = (StringBuffer)types.next();
            aggreg.append(buf);
        }
        return aggreg.toString();
    }
    
    public static void doit(RawChip chip, SpaceTimeSchedule stSchedule) 
    {
        commAddrs = new CommunicateAddrs(chip, stSchedule);
    }
}
