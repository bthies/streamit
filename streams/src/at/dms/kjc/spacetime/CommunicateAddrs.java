package at.dms.kjc.spacetime;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import at.dms.kjc.*;
import at.dms.kjc.backendSupport.ComputeNode;
import at.dms.kjc.common.CommonUtils;
import at.dms.kjc.slicegraph.Buffer;

/**
 * This class will generate code to allocate the off chip buffers on the necessary
 * tiles and then communicate each buffer's global address to the tiles that need
 * to know the address.   It will also generate the typedefs for the rotating buffers
 * and the functions that will set up the structs for rotation (a circular linked list of 
 * buffers). The structure is a circular linked list that has length equal to 
 * the rotation length of the buffer, where each entry in the list points
 * to a component (buffer) of the rotated buffer.
 * <p>
 * It also sets the rotation length for the buffers based on the 
 * primepump schedule.  
 * 
 * @author mgordon
 *
 */
public class CommunicateAddrs 
{
    /** name of the function that initializes the rotation structures */
    public static String rotationSetupFunction = "__setup_rotations__";
    /** name of the function that communicates the addresses on each tile */
    public static String functName = "__snd_rvc_addrs__";
    /** name of the function that de-allocates the rotation buffers */
    public static String freeFunctName = "__free_init_bufs__";
    /** prefix of the variable name for hte rotating buffers */
    public static String rotTypeDefPrefix = "__rotating_buffer_";
    /** The Address Communication pass for the application of this compile */ 
    private static CommunicateAddrs commAddrs;
    /** the raw chip */
    private RawProcElements chip;
    /** functions that we are creating, RawTile to StringBuffer */
    private HashMap<RawTile, StringBuffer> functions;
    /** de-allocate functions that we are creating, RawTile to StringBuffer */
    private HashMap<RawTile, StringBuffer> freeFunctions;
    /** fields we are creating in this pass, RawTile to StringBuffer */ 
    private HashMap<RawTile, StringBuffer> fields;
    /** random generator */
    private static Random rand;
    /** our space time scheduler with the schedules */
    private SpaceTimeSchedule spaceTimeSchedule;      
    /** functions for setup of the rotation structure for each tile, RawTile to StringBuffer*/
    private HashMap<RawTile, StringBuffer> rotationFunctions;
    /** The type defs for the rotating buffer structs for all the types used in
     * the program.
     */
    private static HashMap<CType, StringBuffer> typedefs;
    
    static 
    {
        rand = new Random(17);
    }
    
    /**
     * Return a string of C variable declarations that we create for the given
     * tile.
     * 
     * @param tile The raw tile.
     * @return For <pre>tile</pre> return the fields that represent the buffers as 
     * C variable declarations.
     */
    public static String getFields(ComputeNode tile) 
    {
        return commAddrs.fields.get(tile).toString();
    }

    /**
     * Generate code to allocate the off chip buffers on the necessary
     * tiles and then communicate each buffer's global address to the tiles that need
     * to know the address.   It will also generate the typedefs for the rotating buffers
     * and the functions that will set up the structs for rotation (a circular linked list of 
     * buffers).
     *
     * It also sets the rotation length for the buffers based on the 
     * primepump schedule.  
     * 
     * @param chip The raw chip.
     * @param stSchedule The space time schedule for the app.
     */
    private CommunicateAddrs(RawChip chip, SpaceTimeSchedule stSchedule)
    {
        Router xyRouter = new XYRouter();
        this.chip = chip;
        fields = new HashMap<RawTile, StringBuffer>();
        functions = new HashMap<RawTile, StringBuffer>();
        rotationFunctions = new HashMap<RawTile, StringBuffer>();
        freeFunctions = new HashMap<RawTile, StringBuffer>();
        spaceTimeSchedule = stSchedule;
        typedefs = new HashMap<CType, StringBuffer>(); 
        
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
    
        Iterator<Buffer> buffers = Buffer.getBuffers().iterator();
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
            rotationFunctions.get(homeTile).append(setupRotation(buffer, homeTile));
            
            for (int i = 0; i < rotationLength; i++) {
               
                
                //allocate the steady buffer on the allocating tile
                fields.get(allocatingTile).append
                (buffer.getType().toString() + "* " + 
                        buffer.getIdent(i) + ";\n");
                
                //malloc the steady buffer
                functions.get(allocatingTile).append
                ("  " + buffer.getIdent(i) + " = (" + buffer.getType() + 
                        "*) malloc(32 + (" + buffer.getSize().toString() + " * sizeof(" +
                        buffer.getType() + ")));\n");
                //align the buffer
                functions.get(allocatingTile).append
                ("  " + buffer.getIdent(i) + " = ((u_int32_t)((char*)" + buffer.getIdent(i) +
                ") + 31) & 0xffffffe0;\n");
                
                //if allocator != homeTile, create declaration of 
                //pointer on homeTile and communicate the address for both init and steady...
                if (allocatingTile != homeTile) {
                    homeTile.setComputes();
                    CommonUtils.println_debugging("Need to communicate buffer address from " + 
                            allocatingTile + " to " + homeTile);
                    //generate the switch code to send the addresses
                    RawTile[] dest = {homeTile};
                    
                    //now for the steady
                    SwitchCodeStore.generateSwitchCode(xyRouter, allocatingTile, 
                            dest, 0);
                    
                    
                    //add the code to the owner to send the address to the
                    //static net for the steady
                    functions.get(allocatingTile).append
                    ("  " + Util.networkSendPrefix(false, CStdType.Integer) + 
                            buffer.getIdent(i) + 
                            Util.networkSendSuffix(false) + ";\n");
                    
                    
                    //add declaration of pointer to hometile(steady)
                    fields.get(homeTile).append
                    (buffer.getType().toString() + "* " + 
                            buffer.getIdent(i) + ";\n");
                    
                    
                    //add the code to receive the address into the pointer (steady)
                    functions.get(homeTile).append
                    ("  " + buffer.getIdent(i) + " = " +  
                            Util.networkReceive(false, CStdType.Integer) + ";\n");
                }
                
                
            }
        }
    }
    
    /**
     * Generate the c code that will setup the rotation structure for buffer
     * assign to tile.  It creates a circular linked list that has length equal to 
     * the rotation length of the buffer, where each entry in the list is points
     * to a component of the rotated buffer.
     * 
     * @param buffer The buffer.
     * @param tile  The tile.
     * @return the c code that will setup the rotation structure for buffer
     * assign to tile. 
     */
    private String setupRotation(OffChipBuffer buffer, ComputeNode tile) {
                
        StringBuffer buf = new StringBuffer();
        String temp = "__temp__";
        
        assert buffer.getRotationLength() > 0 : buffer;
        
        String rotType = rotTypeDefPrefix + buffer.getType().toString();
        
        
//      add the declaration of the source rotation buffer of the appriopriate rotation type
        fields.get(tile).append(rotTypeDefPrefix + 
                buffer.getType().toString() + " *" + buffer.getIdent(true) + ";\n");
        
//      add the declaration of the dest rotation buffer of the appriopriate rotation type
        fields.get(tile).append(rotTypeDefPrefix + 
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
    
    /**
     * Given a tile return C function declaration for the function that
     * will setup the rotation structure for the buffers mapped to this tile.
     * 
     * @param tile The tile
     * @return The c function.
     */
    public static String getRotSetupFunct(ComputeNode tile) {
        StringBuffer buf = new StringBuffer();
        buf.append("\nvoid " + rotationSetupFunction + "() {\n");
        buf.append(commAddrs.rotationFunctions.get(tile));
        buf.append("}\n");
        return buf.toString();
    }
    
    /**
     * Return for <pre>tile</pre> return a function that will free the buffer memory.
     * 
     * @param tile The tile.
     * 
     * @return For <pre>tile</pre> return a function that will free the buffer memory.
     */
    public static String getFreeFunction(ComputeNode tile) 
    {
        StringBuffer buf = new StringBuffer();

        //prepend the function name 
        buf.append("\nvoid " + freeFunctName + "() {\n");
        //append the closing } and 
        buf.append(commAddrs.freeFunctions.get(tile));
        buf.append("}\n");
        return buf.toString();
    }
    

    /** 
     * For <pre>tile</pre> return the function that will allocate the buffers or 
     * get the addresses from the static network, this function will not perform the
     * setup of the rotation structures.
     * 
     * @param tile The tile. 
     * 
     * @return For <pre>tile</pre> return the function that will allocate the buffers or 
     * get the addresses from the static network.
     */
    public static String getFunction(ComputeNode tile) 
    {
        StringBuffer buf = new StringBuffer();

        //prepend the function name 
        buf.append("\nvoid " + functName + "() {\n");
        //append the closing } and 
        buf.append(commAddrs.functions.get(tile));
        buf.append("}\n");
        return buf.toString();
    }   
    
    /** 
     * Generate a string that has c code for the type defs for the
     * rotating buffer types. 
     * 
     * @return A string that has c code for the type defs for the
     * rotating buffer types. 
     */
    public static String getRotationTypes() {
        StringBuffer aggreg = new StringBuffer();
        Iterator<StringBuffer> types = typedefs.values().iterator();
        while (types.hasNext()) {
            StringBuffer buf = types.next();
            aggreg.append(buf);
        }
        return aggreg.toString();
    }
    
    /**
     * Generate code to allocate the off chip buffers on the necessary
     * tiles and then communicate each buffer's global address to the tiles that need
     * to know the address.   It will also generate the typedefs for the rotating buffers
     * and the functions that will set up the structs for rotation (a circular linked list of 
     * buffers).
     *
     * It also sets the rotation length for the buffers based on the 
     * primepump schedule.  
     * 
     * @param chip The raw chip.
     * @param stSchedule The space time schedule for the app.
     */
    public static void doit(RawChip chip, SpaceTimeSchedule stSchedule) 
    {
        commAddrs = new CommunicateAddrs(chip, stSchedule);
    }
}
