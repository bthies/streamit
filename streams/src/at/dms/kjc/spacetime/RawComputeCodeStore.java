package at.dms.kjc.spacetime;

import at.dms.kjc.*;
import at.dms.kjc.backendSupport.Channel;
import at.dms.kjc.backendSupport.ComputeCodeStore;
import at.dms.kjc.backendSupport.ComputeNode;
import at.dms.kjc.backendSupport.FilterInfo;
import at.dms.kjc.backendSupport.Layout;
import at.dms.kjc.backendSupport.SchedulingPhase;
import at.dms.kjc.common.CommonUtils;
import at.dms.kjc.sir.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import at.dms.kjc.slicegraph.FilterContent;

/**
 * 
 * Repository for the compute code (SIR code that is converted to C
 * and mapped to the compute processor) for a tile.  This class is 
 * used by {@link at.dms.kjc.spacetime.Rawify} as it visits the schedules 
 * to generate the code that will implement the schedules. 
 * This class includes methods to add code to the 3 different schedules 
 * (init, primepump, steady) that are executed at each tile.
 * This class is then used by {@link at.dms.kjc.spacetime.SliceIRtoC} to 
 * convert the sir code for each tile to C code for each tile.
 * 
 * @author mgordon
 *
 */
public class RawComputeCodeStore extends ComputeCodeStore<RawTile> implements SIRCodeUnit{
    /* debug dump of operations on code buffer: */
    static boolean debugging = false;
    
    /* attempt to override value of static field in superclass */
    static { mainName = "__RAWMAIN__";}
    /** If true, generate presynched dram commands when needed */
    public static final boolean GEN_PRESYNCH = true;
    /** this hash map holds RawExecutionCode that was generated
     * so when we see the filter for the first time in the init
     * and if we see the same filter again, we do not have to
     * add the functions again
     */
    protected HashMap<FilterContent, RawExecutionCode> rawCode;
    /** this will be true with the first dram read of the steady state 
     * has been presynched, meaning that we have already seen a read
     * and presynched the first read of the steady-state, cough. 
     */
    private boolean firstLoadPresynchedInSteady;
    
    /**
     * Create a store for the compute code of tile.  
     * 
     * @param parent
     */
    public RawComputeCodeStore(RawTile parent) {
        super(parent);
        firstLoadPresynchedInSteady = false;
        rawCode = new HashMap<FilterContent, RawExecutionCode>();
        if (debugging) {System.err.println("RawComputeCodeStore " + parent);}
    }

    /**
     * Generate a command to read or write from a file daisy-chained to a i/o
     * port.  
     * 
     * @param read True if load, false store
     * @param init true if we want to append command to init stage, false steady
     * @param words the number of words to x-fer
     * @param buffer Used to get the port
     * @param staticNet True if static net, false gdn
     */
    public void addFileCommand(boolean read, boolean init, int words,
                               OffChipBuffer buffer, boolean staticNet) {
        if (debugging) {System.err.println("addFileCommand " + read + " " + init + " " + words + " " + staticNet);}
        assert !buffer.redundant() : "trying to generate a file command for a redundant buffer.";
        assert words > 0 : "trying to generate a file dram command of size 0";

        //assert buffer.getRotationLength() == 1 : 
        //    "The buffer connected to a file reader / writer cannot rotate!";
        parent.setComputes();
        String functName = "raw_streaming_dram" + 
            (staticNet ? "" : "_gdn") + 
            "_request_bypass_" +
            (read ? "read" : "write");
        
        String bufferName = buffer.getIdent(0);

        JExpression[] args = {
            new JFieldAccessExpression(null, new JThisExpression(null),
                                       bufferName), new JIntLiteral(words) };

        // the dram command
        JMethodCallExpression call = new JMethodCallExpression(null, functName,
                                                               args);

        if (init)
            initBlock.addStatement(new JExpressionStatement(null, call, null));
        else
            steadyLoop.addStatement(new JExpressionStatement(null, call, null));
    }

    /**
     * Generate a command to read from a file daisy-chained to a i/o
     * port on the gdn and send the data to <pre>dest</pre>
     * 
     * @param init true if we want to append command to init stage, false steady
     * @param words the number of words to x-fer
     * @param buffer Used to get the port
     * @param dest The tile to which to send the data.
     */
    public void addFileGDNReadCommand(SchedulingPhase whichPhase, int words,
                               OffChipBuffer buffer, RawTile dest) {
        if (debugging) {System.err.println("addFileGDNReadCommand " + whichPhase + " " + words + " " + dest);}

        assert !buffer.redundant() : "trying to generate a file command for a redundant buffer.";
        assert words > 0 : "trying to generate a file dram command of size 0";
        
        assert buffer.getRotationLength() == 1 : 
            "The buffer connected to a file reader / writer cannot rotate!";
        parent.setComputes();
        String functName = "raw_streaming_dram_gdn_request_bypass_read_dest";
        
        String bufferName = buffer.getIdent(0);

        JExpression[] args = 
        {
                new JFieldAccessExpression(null, new JThisExpression(null),bufferName), 
                new JIntLiteral(words),
                new JIntLiteral(dest.getY()),
                new JIntLiteral(dest.getX())
                };

        // the dram command
        JMethodCallExpression call = new JMethodCallExpression(null, functName,
                 args);
        switch (whichPhase) {
        case INIT:
        case PRIMEPUMP:
            initBlock.addStatement(new JExpressionStatement(null, call, null));
            break;
        case STEADY:
            steadyLoop.addStatement(new JExpressionStatement(null, call, null));
            break;
        }
    }

    
    /**
     * If we are generating a DRAM command for a store
     * over the gdn and the tile that is writing the data is different from the
     * home tile, and we cannot use any other synchronization, we must send 
     * a word over the static network to the tile writing the data for the store
     * from this tile to tell it that we have sent the dram command and it
     * can begin the store.
     * 
     * This function will generate the gdn store command and also inject
     * the synch word to the static network.
     * 
     * @param init
     * @param buffer
     */
    public void addGDNStoreCommandWithSynch(SchedulingPhase whichPhase, 
            int bytes, OffChipBuffer buffer) {
        if (debugging) {System.err.println("addGDNStoreCommandWithSynch " + whichPhase + " " + bytes);}
       
        addDRAMCommand(false, whichPhase, bytes, buffer, false);
        
        JAssignmentExpression ass = 
            new JAssignmentExpression(new JFieldAccessExpression(Util.CSTOINTVAR), 
                    new JIntLiteral(-1));
        JStatement statement = new JExpressionStatement(ass);
        
        switch (whichPhase) {
        case INIT:
        case PRIMEPUMP:
            initBlock.addStatement(statement);
            break;
        case STEADY:
            steadyLoop.addStatement(statement);
            break;
        }
    }
    
    /**
     * If we are generating a file command  for a store
     * over the gdn and the tile that is writing the data is different from the
     * home tile, and we cannot use any other synchronization, we must send 
     * a word over the static network to the tile writing the data for the store
     * from this tile to tell it that we have sent the dram command and it
     * can begin the store.
     * 
     *  This function will generate the gdn file store command and also inject
     *  the synch word to the static network.
     *  
     * @param init
     * @param words
     * @param buffer
     */
    public void addGDNFileStoreCommandWithSynch(boolean init, int words,
            OffChipBuffer buffer) {
        if (debugging) {System.err.println("addGDNFileStoreCommandWithSynch " + init + " " + words);}
        addFileCommand(false, init, words, buffer, false);
        
        JAssignmentExpression ass = 
            new JAssignmentExpression(new JFieldAccessExpression(Util.CSTOINTVAR), 
                    new JIntLiteral(-1));
        JStatement statement = new JExpressionStatement(ass);
        
        if (init)
            initBlock.addStatement(statement);
        else 
            steadyLoop.addStatement(statement); 
    }
    
    /**
     * Generate code on the compute process to send over <pre>words</pre> words of data
     * using a header that was already set previously.
     * 
     * @param dev
     * @param words The number of dummy words to send
     * @param init
     */
    public void dummyOutgoing(IODevice dev, int words, boolean init) {
        JBlock block = new JBlock();
        if (debugging) {System.err.println("dummyOutgoing " + init + " " + words + " " + dev);}

        assert words < RawChip.cacheLineWords : "Should not align more than cache-line size.";
        
        for (int i = 0; i < words; i++) {
            //send the header
            JStatement sendHeader = 
                new JExpressionStatement(
                        new JAssignmentExpression(new JFieldAccessExpression(Util.CGNOINTVAR),
                                new JFieldAccessExpression(TraceIRtoC.DYNMSGHEADER)));
            block.addStatement(sendHeader);
            //send over the opcode 13 to tell the dram that this is a data packet
            JStatement sendDataOpcode = 
                new JExpressionStatement(
                        new JAssignmentExpression(new JFieldAccessExpression(Util.CGNOINTVAR),
                                new JIntLiteral(RawChip.DRAM_GDN_DATA_OPCODE)));
            block.addStatement(sendDataOpcode);
            //send over negative one as the dummy value
            JStatement sendMinusOne = 
                new JExpressionStatement(
                        new JAssignmentExpression(new JFieldAccessExpression(Util.CGNOINTVAR),
                                new JIntLiteral(-1)));
            block.addStatement(sendMinusOne);
        }
        
        //append the statements to the appropriate code schedule
        if (init)
            initBlock.addStatement(block);
        else 
            steadyLoop.addStatement(block);
    }
    
    /**
     * Generate code to receive <pre>words</pre> words into a dummy volatile variable on 
     * the compute processor from gdn.
     * 
     * @param dev
     * @param words The number of words to disregard
     * @param init Which schedule should be append this code to?
     */
    public void disregardIncoming(IODevice dev, int words, boolean init) {
        if (debugging) {System.err.println("disregardIncoming " + init + " " + words + " " + dev);}
        JBlock block = new JBlock();

        assert words < RawChip.cacheLineWords : "Should not align more than cache-line size.";
        
        for (int i = 0; i < words; i++) {
            //receive the word into the dummy volatile variable
            JStatement receiveDummy = 
                new JExpressionStatement(
                        new JAssignmentExpression(new JFieldAccessExpression(TraceIRtoC.DUMMY_VOLATILE),
                                new JFieldAccessExpression(Util.CGNIINTVAR)));
            block.addStatement(receiveDummy);
        }
        
        //append the statements to the appropriate code schedule
        if (init)
            initBlock.addStatement(block);
        else 
            steadyLoop.addStatement(block);
    }
    
    /**
     * Decide if a dram command should be presynched at the given time based on 
     * the command and the buffer.
     * 
     * @param read
     * @param init
     * @param primepump
     * @param buffer
     * @return True if we should presynch, false if not.
     */
    public boolean shouldPresynch(boolean read, SchedulingPhase whichPhase, OffChipBuffer buffer) {
        if (debugging) {System.err.println("shouldPresynch " + whichPhase + " " + read);}
              
        //never presynch a write, double buffering and the execution
        //order assures that writes will always occurs after the data is read
        if (!read)
            return false;
        
        //now it is a read
        
        //always presynch in the init && primepump stage and if we are not software
        //pipeling!
        if (SchedulingPhase.isInitOrPrimepump(whichPhase) || SpaceTimeBackend.NO_SWPIPELINE) 
            return true;
        
        //now in steady
        
        //for the steady state presynch reads from input nodes that are 
        //necessary because intrabuffers are not rotated
        if (buffer.isIntraSlice() &&
                buffer.getSource().isInputSlice() && 
                !OffChipBuffer.unnecessary(buffer.getSource().getAsInput())) {
            return true;
        }
        
        // presynch reads from the filter->outputslice buffers because they
        // are not double buffered, but only if they are going to be split
        // so the outputslicenode is actually necessary
        if (buffer.isIntraSlice() &&
                buffer.getDest().isOutputSlice() &&
                !OffChipBuffer.unnecessary((buffer.getDest().getAsOutput())))
            return true;
        
        //we have to presynch the first read in the steady-state to make
        //sure that the last iteration of the loop finished writing everything...
        if (!firstLoadPresynchedInSteady) {
            firstLoadPresynchedInSteady = true;
            return true;
        }
        
        //no need to presynch
        return false;
    }
    
    /**
     * Add a dram command and possibly a rotation of the buffer 
     * at the current position of this code compute code 
     * for either the init stage (including primepump) or the steady state.
     * If in the init stage, don't rotate buffers.  If in init or primepump,
     * add the code to the init stage.  
     * 
     * @param read True if we want to issue a read, false if write
     * @param init true if init
     * @param primepump true if primepump
     * @param bytes The number of bytes
     * @param buffer The address to load/store
     * @param staticNet True if we want to use the static network, false if gdn
     */    
    public void addDRAMCommand(boolean read, SchedulingPhase whichPhase, int bytes,
            OffChipBuffer buffer, boolean staticNet) {
        if (debugging) {System.err.println("addDRAMCommand " + read + " " + whichPhase + " " + bytes + " " + staticNet);}
       
        assert bytes > 0 : "trying to generate a dram command of size 0";
        assert !buffer.redundant() : "Trying to generate a dram command for a redundant buffer!";
                
        boolean shouldPreSynch = shouldPresynch(read, whichPhase, buffer);
        
        parent.setComputes();
        
        //the name of the rotation struction we are using...
        String rotStructName = buffer.getIdent(read);
 
        // the args for the streaming dram command

        // cachelines that are needed
        assert bytes % RawChip.cacheLineBytes == 0 : "transfer size for dram must be a multiple of cache line size";

        int cacheLines = bytes / RawChip.cacheLineBytes;
        
        //generate the dram command and send the address of the x-fer
        JStatement dramCommand = 
            sirDramCommand(read, cacheLines, 
                    new JNameExpression(null, null, rotStructName + "->buffer"),
                    staticNet, shouldPreSynch, 
                    new JNameExpression(null, null, rotStructName + "->buffer"));
        
   
        //The rotation expression
        JAssignmentExpression rotExp = new JAssignmentExpression(null,
                new JFieldAccessExpression(null, new JThisExpression(null), rotStructName),
                new JNameExpression(null, null, rotStructName + "->next"));
        
        CommonUtils.println_debugging("Adding DRAM Command to " + parent + " "
                                 + buffer + " " + cacheLines);
        
        // add the statements to the appropriate stage
        switch (whichPhase) {
        case INIT:
            initBlock.addStatement(dramCommand);
            break;
        case PRIMEPUMP:
            initBlock.addStatement(dramCommand);
            //only rotate in init during primepump
            initBlock.addStatement(new JExpressionStatement(null, rotExp, null));
            break;
        case STEADY:
            steadyLoop.addStatement((JStatement)ObjectDeepCloner.deepCopy(dramCommand));
            //always rotate in the steady state
            steadyLoop
            .addStatement(new JExpressionStatement(null, 
                    (JAssignmentExpression)ObjectDeepCloner.deepCopy(rotExp), null));
            break;
        }
    }

    /**
     * Return sir code that will call the macro for a dram command to transfer
     * <pre>cacheLines</pre> cache-lines from a single address <pre>address</pre> using <pre>sampleAddress</pre>
     * to send the command to the correct dram port.
     * 
     * @param read True then load, false then store
     * @param cacheLines
     * @param sampleAddress An address that resides on the drams
     * @param staticNet If true, use static net, otherwise gdn
     * @param shouldPreSynch If true, generate a presynched command
     * @param address The address for the xfer
     * @return ...
     */
    public static JStatement sirDramCommand(boolean read, int cacheLines, JExpression sampleAddress,
            boolean staticNet, boolean shouldPreSynch, JExpression address) {
        if (debugging) {System.err.println("sirDramCommand " + read + " " + cacheLines + " " + staticNet + " " + shouldPreSynch + " " + sampleAddress + " " + address);}
        JBlock block = new JBlock();
        
        String functName = "raw_streaming_dram" + (!staticNet ? "_gdn" : "") + 
        "_request_" +
        (read ? "read" : "write") + (shouldPreSynch ? "_presynched" : "");
        
        JExpression[] args = {
                sampleAddress, 
                new JIntLiteral(1),
                new JIntLiteral(cacheLines) };
     
        // the dram command
        JMethodCallExpression call = new JMethodCallExpression(null, functName,
                                                               args);
        
        // send over the address
        JFieldAccessExpression dynNetSend = 
            new JFieldAccessExpression(null,
                    new JThisExpression(null), Util.CGNOINTVAR);
        

        //the assignment that will perform the dynamic network send...
        JAssignmentExpression assExp = new JAssignmentExpression(null,
                                                                 dynNetSend, address);
        
        block.addStatement(new JExpressionStatement(call));
        block.addStatement(new JExpressionStatement(assExp));
        
        return block;
    }
    
    /**
     * Add a dram read command at the current position of this code compute code 
     * for either the init stage (including primepump) or the steady state over the
     * gdn and send it to the <pre>dest</pre> tile.  Don't rotate the buffer we are in the init
     * stage.  
     * 
     * @param init true if init 
     * @param primepump true if primepump stage
     * @param bytes The number of bytes
     * @param buffer The address to load/store
     * @param presynched True if we want all other dram commands to finish before this
     * one is issued
     * @param dest The read's destination. 
     */    
    public void addDRAMGDNReadCommand(SchedulingPhase whichPhase, int bytes,
            OffChipBuffer buffer, boolean presynched, RawTile dest) {
        if (debugging) {System.err.println("addDRAMGDNReadCommand " + whichPhase + " " + bytes + " " + presynched + " " + dest);}
        

        assert bytes > 0 : "trying to generate a dram command of size 0";
        assert !buffer.redundant() : "Trying to generate a dram command for a redundant buffer!";
         
        boolean shouldPreSynch = presynched && GEN_PRESYNCH && 
                (SchedulingPhase.isInitOrPrimepump(whichPhase) || SpaceTimeBackend.NO_SWPIPELINE);
        
        parent.setComputes();
        String functName = "raw_streaming_dram_gdn_request_read" + 
        (shouldPreSynch ? "_presynched" : "") + "_dest";
        
        //the name of the rotation struction we are using...
        String rotStructName = buffer.getIdent(true);
 
        // the args for the streaming dram command

        // cachelines that are needed
        assert bytes % RawChip.cacheLineBytes == 0 : "transfer size for dram must be a multiple of cache line size";

        int cacheLines = bytes / RawChip.cacheLineBytes;
        JExpression[] args = {
                new JNameExpression(null, null, rotStructName + "->buffer"), 
                new JIntLiteral(1),
                new JIntLiteral(cacheLines), 
                new JIntLiteral(dest.getY()),
                new JIntLiteral(dest.getX())};
        
        // the dram command
        JMethodCallExpression call = new JMethodCallExpression(null, functName,
                                                               args);

        // send over the address
        JFieldAccessExpression dynNetSend = new JFieldAccessExpression(null,
                                                                       new JThisExpression(null), Util.CGNOINTVAR);

        JNameExpression bufAccess = new JNameExpression(null, null, 
                rotStructName + "->buffer");

        JAssignmentExpression assExp = new JAssignmentExpression(null,
                                                                 dynNetSend, bufAccess);
        JAssignmentExpression rotExp = new JAssignmentExpression(null,
                new JFieldAccessExpression(null, new JThisExpression(null), rotStructName),
                new JNameExpression(null, null, rotStructName + "->next"));
        
        
        CommonUtils.println_debugging("Adding DRAM Command to " + parent + " "
                                 + buffer + " " + cacheLines);
        // add the statements to the appropriate stage
        switch (whichPhase) {
        case INIT:
            initBlock.addStatement(new JExpressionStatement(null, call, null));
            initBlock
                .addStatement(new JExpressionStatement(null, assExp, null));
            break;
        case PRIMEPUMP:
            initBlock.addStatement(new JExpressionStatement(null, call, null));
            initBlock
                .addStatement(new JExpressionStatement(null, assExp, null));
            //only rotate the buffer in the primepump stage
            initBlock.addStatement(new JExpressionStatement(null, rotExp, null));
            break;
        case STEADY:
            steadyLoop.addStatement(new JExpressionStatement(null, 
                    (JMethodCallExpression)ObjectDeepCloner.deepCopy(call), null));
            steadyLoop
            .addStatement(new JExpressionStatement(null, 
                    (JAssignmentExpression)ObjectDeepCloner.deepCopy(assExp), null));
            steadyLoop
            .addStatement(new JExpressionStatement(null, 
                    (JAssignmentExpression)ObjectDeepCloner.deepCopy(rotExp), null));
           break;
        }
    }   
    
    
    /**
     * Add the code necessary to execution the filter of filterInfo
     * at the current position in the steady-state code of this store, 
     * given the layout. 
     * 
     * @param filterInfo The filter to add to the steady-state schedule.
     * @param layout The layout of the application.
     */
    public void addSliceSteady(FilterInfo filterInfo, Layout layout) {
        if (debugging) {System.err.println("addSliceSteady " + filterInfo);}
    
        RawExecutionCode exeCode;

        // check to see if we have seen this filter already
        if (rawCode.containsKey(filterInfo.filter)) {
            exeCode = rawCode.get(filterInfo.filter);
        } else {
            // otherwise create the raw ir code
            // if we can run linear or direct communication, run it
            if (filterInfo.isLinear())
                exeCode = new Linear(parent, filterInfo);
            else if (DirectCommunication.testDC(filterInfo))
                exeCode = new DirectCommunication(parent, filterInfo, layout);
            else
                exeCode = new BufferedCommunication(parent, filterInfo, layout);
            addSliceFieldsAndMethods(exeCode, filterInfo);
        }
        
        parent.setMapped();
        
        // add the steady state
        JBlock steady = exeCode.getSteadyBlock();
        if (CODE)
            steadyLoop.addStatement(steady);
        else
            // add a place holder for debugging
            steadyLoop.addStatement(new JExpressionStatement(null,
                                                             new JMethodCallExpression(null, new JThisExpression(null),
                                                                                       filterInfo.filter.toString(), new JExpression[0]),
                                                             null));

        /*
         * addMethod(steady); steadyLoop.addStatement(steadyIndex++, new
         * JExpressionStatement (null, new JMethodCallExpression(null, new
         * JThisExpression(null), steady.getName(), new JExpression[0]), null));
         */
    }

    /**
     * Called to add filterInfo's fields, helper methods, and init function call
     * as calculated by exeCode to the compute code store for this tile.  
     * 
     * @param exeCode The code to add.
     * @param filterInfo The filter.
     */
    private void addSliceFieldsAndMethods(RawExecutionCode exeCode,
                                          FilterInfo filterInfo) {
        // add the fields of the slice
        addFields(exeCode.getVarDecls());
        // add the helper methods
        addMethods(exeCode.getHelperMethods());
        // add the init function
        addInitFunctionCall(filterInfo.filter.getInit());
    }


    /**
     * Add filterInfo's prime pump block to the current position of the
     * primepump code for this tile.
     * 
     * @param filterInfo The filter.
     * @param layout The layout of the application.
     */
    public void addSlicePrimePump(FilterInfo filterInfo, Layout layout) {
        if (debugging) {System.err.println("addSlicePrimePump " + filterInfo);}
     
        RawExecutionCode exeCode;
        JMethodDeclaration primePump;
        
        // check to see if we have seen this filter already
        if (rawCode.containsKey(filterInfo.filter)) {
            exeCode = rawCode.get(filterInfo.filter);
        } else {
            // otherwise create the raw ir code
            // if we can run linear or direct communication, run it
            if (filterInfo.isLinear())
                exeCode = new Linear(parent, filterInfo);
            else if (DirectCommunication.testDC(filterInfo))
                exeCode = new DirectCommunication(parent, filterInfo, layout);
            else
                exeCode = new BufferedCommunication(parent, filterInfo, layout);
            addSliceFieldsAndMethods(exeCode, filterInfo);
        }
        
        primePump = exeCode.getPrimePumpMethod();
        if (primePump != null && !hasMethod(primePump) && CODE) {
            addMethod(primePump);
        }   
       
        parent.setMapped();
        
        // now add a call to the init stage in main at the appropiate index
        // and increment the index
        initBlock.addStatement(new JExpressionStatement(null,
                new JMethodCallExpression(null, new JThisExpression(null),
                        primePump.getName(), new JExpression[0]), null));
    }

    /**
     * Add filterInfo's init stage block at the current position of the init
     * stage for this code store.
     * 
     * @param filterInfo The filter.
     * @param layout The layout of the application.
     */
    public void addSliceInit(FilterInfo filterInfo, Layout layout) {
        if (debugging) {System.err.println("addSliceInit " + filterInfo);}
    
        RawExecutionCode exeCode;

        // if we can run direct communication, run it
        if (filterInfo.isLinear())
            exeCode = new Linear(parent, filterInfo);
        else if (DirectCommunication.testDC(filterInfo))
            exeCode = new DirectCommunication(parent, filterInfo, layout);
        else
            exeCode = new BufferedCommunication(parent, filterInfo, layout);

        // add this raw IR code to the rawCode hashmap
        // if the steady-state is on the same tile, don't
        // regenerate the IR code and add dups of the functions,
        // fields
        rawCode.put(filterInfo.filter, exeCode);

        // this must come before anything
        addSliceFieldsAndMethods(exeCode, filterInfo);
        // get the initialization routine of the phase
        JMethodDeclaration initStage = exeCode.getInitStageMethod();
        if (initStage != null) {
            parent.setMapped();
            // add the method
            if (CODE)
                addMethod(initStage);

            // now add a call to the init stage in main at the appropiate index
            // and increment the index
            initBlock.addStatement(new JExpressionStatement(null,
                                                            new JMethodCallExpression(null, new JThisExpression(null),
                                                                                      initStage.getName(), new JExpression[0]), null));
        }
    }
    
    /**
     * This function will create a presynch read command for each dram that 
     * does not have a filter assigned to it but initiates DRAM commands.
     * This will be placed at the current position of the steady state.
     *
     */
    public static void presynchEmptyTilesInSteady() {
        if (debugging) {System.err.println("presynchEmptyTilesInSteady");}
        Iterator<Channel> buffers = OffChipBuffer.getBuffers().iterator();
        HashSet<RawTile> visitedTiles = new HashSet<RawTile>();
        
        while (buffers.hasNext()) {
            OffChipBuffer buffer = (OffChipBuffer)buffers.next();
            if (buffer.redundant())
                continue;
            //don't do anything if a filter is mapped to this
            //tile
            if (buffer.getOwner().isMapped())
                continue;
            
            if (!visitedTiles.contains(buffer.getOwner())) {
                //remember that we have already presynched this tile
                visitedTiles.add(buffer.getOwner());
             
                //need sample address and address
                JExpression sampleAddress = 
                    new JNameExpression(null, null, 
                            buffer.getIdent(true) + "->buffer");
                    
                JExpression address =  new JNameExpression(null, null, 
                        buffer.getIdent(true) + "->buffer");
                
                //the dram command to send over, use the gdn
                JStatement dramCommand = RawComputeCodeStore.sirDramCommand(true, 1, 
                        sampleAddress, false, true, address);
                
                //now add the presynch command which is just 
                //a presynched read of length 1
                buffer.getOwner().getComputeCode().steadyLoop.addStatement
                (dramCommand);
                
                //now disregard the incoming data from the dram...
                //do this in two statement because there is an assert inside 
                //of gdnDisregardIncoming() that prevents you from disregarding
                //an entire cacheline and I like it there...
                buffer.getOwner().getComputeCode().steadyLoop.addStatement
                (RawExecutionCode.gdnDisregardIncoming(RawChip.cacheLineWords - 1));
                buffer.getOwner().getComputeCode().steadyLoop.addStatement
                (RawExecutionCode.gdnDisregardIncoming(1));
            }
        }
    }

    /**
     * Create a barrier in the init stage at this point in code generation.
     * 
     * @param chip The raw chip.
     */
    public static void barrier(RawChip chip, boolean init, boolean primepump) {
       if (debugging) {System.err.println("barrier " + init + " " + primepump);}
       Router router = new XYRouter();
        
        boolean steady = !init && !primepump;
        int stage = 1;
        if (steady)
            stage = 2;
        
        for (int i = 0; i < chip.getTotalTiles(); i++) {
            RawTile tile = chip.getTile(i);
            if (steady)
                tile.getComputeCode().steadyLoop.addStatement(RawExecutionCode.boundToSwitchStmt(100));
            else
                tile.getComputeCode().initBlock.addStatement(RawExecutionCode.boundToSwitchStmt(100));
            RawComputeNode[] dests =  new RawComputeNode[chip.getTotalTiles() - 1];
            int index = 0;
            for (int x = 0; x < chip.getTotalTiles(); x++) {
                ComputeNode dest = chip.getTile(x);
                if (dest != tile) {
                    JStatement rec = 
                        new JExpressionStatement(
                                new JAssignmentExpression(new JFieldAccessExpression(TraceIRtoC.DUMMY_VOLATILE),
                                        new JFieldAccessExpression(Util.CSTIINTVAR)));
                    if (steady) 
                        dest.getComputeCode().addSteadyLoopStatement(rec);
                    else 
                        dest.getComputeCode().addInitStatement(rec);
                    dests[index++] = chip.getTile(x);
                }
            }
            SwitchCodeStore.generateSwitchCode(router, tile, dests, stage);
        }
    }
    
    /**
     * This function will create a presynch read command for every dram that is
     * used in the program and add it to the current position in the init
     * block.  It will make sure that all dram write commands are finished
     * before anything else can be issued.  
     *
     */
    public static void presynchAllDramsInInit() {
        if (debugging) {System.err.println("presynchAllDramsInInit");}
        Iterator<Channel> buffers = OffChipBuffer.getBuffers().iterator();
        HashSet<RawTile> visitedTiles = new HashSet<RawTile>();
        
        while (buffers.hasNext()) {
            OffChipBuffer buffer = (OffChipBuffer)buffers.next();
            if (buffer.redundant())
                continue;
            if (!visitedTiles.contains(buffer.getOwner())) {
                //remember that we have already presynched this tile
                visitedTiles.add(buffer.getOwner());
             
                //need sample address and address
                JExpression sampleAddress = 
                    new JNameExpression(null, null, 
                            buffer.getIdent(true) + "->buffer");
                    
                JExpression address =  new JNameExpression(null, null, 
                        buffer.getIdent(true) + "->buffer");
                
                //the dram command to send over, use the gdn
                JStatement dramCommand = RawComputeCodeStore.sirDramCommand(true, 1, 
                        sampleAddress, false, true, address);
                /*
                //add a comment to the command
                JavaStyleComment comment = new JavaStyleComment("Stage Presynch Command", true, false, false);
                dramCommand.setComments(new JavaStyleComment[]{comment});
                */
                
                //now add the presynch command which is just 
                //a presynched read of length 1
                buffer.getOwner().getComputeCode().initBlock.addStatement
                (dramCommand);
                
                //now disregard the incoming data from the dram...
                //do this in two statement because there is an assert inside 
                //of gdnDisregardIncoming() that prevents you from disregarding
                //an entire cacheline and I like it there...
                buffer.getOwner().getComputeCode().initBlock.addStatement
                (RawExecutionCode.gdnDisregardIncoming(RawChip.cacheLineWords - 1));
                buffer.getOwner().getComputeCode().initBlock.addStatement
                (RawExecutionCode.gdnDisregardIncoming(1));
            }
        }
    }
    
    /**
     * Create code on the compute processor to send constant to 
     * the switch.  If init then append the code to the init schedule, 
     * otherwise steady-schedule.
     * <p>
     * It does nothing on the switch, that is left for somewhere else.
     * 
     * @param constant The number to send.
     * @param init if true add to init stage, otherwise add to steady.
     */
    public void sendConstToSwitch(int constant, boolean init) {
        if (debugging) {System.err.println("sendConstToSwitch " + init + " " + constant);}
        JStatement send = RawExecutionCode.constToSwitchStmt(constant);

        if (init)
            initBlock.addStatement(send);
        else
            steadyLoop.addStatement(send);
    }
}
