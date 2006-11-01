package at.dms.kjc.spacetime;

import at.dms.compiler.JavaStyleComment;
import at.dms.kjc.*;
import at.dms.util.Utils;
import at.dms.kjc.sir.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import at.dms.kjc.slicegraph.FilterContent;

/**
 * 
 * Repository for the compute code (SIR code that is converted to C
 * and mapped to the compute processor) for a tile.  This class is 
 * used by {@link at.dms.kjc.spacetime.Rawify} as it visits the schedules 
 * to generate the code that will implement the schedules. 
 * This class includes methods to add code to the 3 different schedules 
 * (init, primepump, steady) that are executed at each tile.
 * This class is then used by {@link at.dms.kjc.spacetime.TraceIRtoC} to 
 * convert the sir code for each tile to C code for each tile.
 * 
 * @author mgordon
 *
 */
public class ComputeCodeStore implements SIRCodeUnit{
    /** If true, generate presynched dram commands when needed */
    public static final boolean GEN_PRESYNCH = true;
    /** the name of the main function for each tile */
    public static String main = "__RAWMAIN__";
    /** set to false if you do not want to generate
     * the work functions calls or inline them
     * useful for debugging 
     */
    private static final boolean CODE = true;
    /** The fields of the tile */
    protected JFieldDeclaration[] fields;
    /** the methods of this tile */
    protected JMethodDeclaration[] methods;
    /** this method calls all the initialization routines
     * and the steady state routines
     */
    protected JMethodDeclaration rawMain;
    /** The raw tile this compute code will be place on */
    protected RawTile parent;
    /** block for the steady-state, as calculated currently */
    protected JBlock steadyLoop;
    /** the block that executes each tracenode's init schedule, as calculated currently */
    protected JBlock initBlock;        
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
    public ComputeCodeStore(RawTile parent) {
        this.parent = parent;
        firstLoadPresynchedInSteady = false;
        rawCode = new HashMap<FilterContent, RawExecutionCode>();
        methods = new JMethodDeclaration[0];
        fields = new JFieldDeclaration[0];
        rawMain = new JMethodDeclaration(null, at.dms.kjc.Constants.ACC_PUBLIC,
                                         CStdType.Void, main, JFormalParameter.EMPTY, CClassType.EMPTY,
                                         new JBlock(null, new JStatement[0], null), null, null);

        // add the block for the init stage calls
        initBlock = new JBlock(null, new JStatement[0], null);
        rawMain.addStatement(initBlock);
        /*
         * //add the call to free the init buffers rawMain.addStatement(new
         * JExpressionStatement (null, new JMethodCallExpression(null, new
         * JThisExpression(null), CommunicateAddrs.freeFunctName, new
         * JExpression[0]), null));
         */
        // create the body of steady state loop
        steadyLoop = new JBlock(null, new JStatement[0], null);
        // add it to the while statement
        rawMain.addStatement(new JWhileStatement(null, new JBooleanLiteral(
                                                                           null, true), steadyLoop, null));
        addMethod(rawMain);
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
    public void addFileGDNReadCommand(boolean init, int words,
                               OffChipBuffer buffer, RawTile dest) {
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
        if (init)
            initBlock.addStatement(new JExpressionStatement(null, call, null));
        else
            steadyLoop.addStatement(new JExpressionStatement(null, call, null));
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
    public void addGDNStoreCommandWithSynch(boolean init, boolean primepump, 
            int bytes, OffChipBuffer buffer) {
        
        addDRAMCommand(false, init, primepump, bytes, buffer, false);
        
        JAssignmentExpression ass = 
            new JAssignmentExpression(new JFieldAccessExpression(Util.CSTOINTVAR), 
                    new JIntLiteral(-1));
        JStatement statement = new JExpressionStatement(ass);
        
        if (init || primepump)
            initBlock.addStatement(statement);
        else 
            steadyLoop.addStatement(statement);
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
    public boolean shouldPresynch(boolean read, boolean init, boolean 
            primepump, OffChipBuffer buffer) {
        
        //never presynch a write, double buffering and the execution
        //order assures that writes will always occurs after the data is read
        if (!read)
            return false;
        
        //now it is a read
        
        //always presynch in the init && primepump stage and if we are not software
        //pipeling!
        if (init || primepump || SpaceTimeBackend.NO_SWPIPELINE) 
            return true;
        
        //now in steady
        
        //for the steady state presynch reads from input nodes that are 
        //necessary because intrabuffers are not rotated
        if (buffer.isIntraTrace() &&
                buffer.getSource().isInputTrace() && 
                !OffChipBuffer.unnecessary(buffer.getSource().getAsInput())) {
            return true;
        }
        
        // presynch reads from the filter->outputtrace buffers because they
        // are not double buffered, but only if they are going to be split
        // so the outputtracenode is actually necessary
        if (buffer.isIntraTrace() &&
                buffer.getDest().isOutputTrace() &&
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
    public void addDRAMCommand(boolean read, boolean init, boolean primepump, int bytes,
            OffChipBuffer buffer, boolean staticNet) {
        
        assert bytes > 0 : "trying to generate a dram command of size 0";
        assert !buffer.redundant() : "Trying to generate a dram command for a redundant buffer!";
                
        boolean shouldPreSynch = shouldPresynch(read, init, primepump, buffer);
        
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
        
        SpaceTimeBackend.println("Adding DRAM Command to " + parent + " "
                                 + buffer + " " + cacheLines);
        
        // add the statements to the appropriate stage
        if (init || primepump) {
            initBlock.addStatement(dramCommand);
            //only rotate in init during primepump
            if (primepump)
                initBlock.addStatement(new JExpressionStatement(null, rotExp, null));
        } else {
            steadyLoop.addStatement((JStatement)ObjectDeepCloner.deepCopy(dramCommand));
            //always rotate in the steady state
            steadyLoop
            .addStatement(new JExpressionStatement(null, 
                    (JAssignmentExpression)ObjectDeepCloner.deepCopy(rotExp), null));
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
    public void addDRAMGDNReadCommand(boolean init, boolean primepump, int bytes,
            OffChipBuffer buffer, boolean presynched, RawTile dest) {
        

        assert bytes > 0 : "trying to generate a dram command of size 0";
        assert !buffer.redundant() : "Trying to generate a dram command for a redundant buffer!";
         
        boolean shouldPreSynch = presynched && GEN_PRESYNCH && 
                (init || primepump || SpaceTimeBackend.NO_SWPIPELINE);
        
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
        
        
        SpaceTimeBackend.println("Adding DRAM Command to " + parent + " "
                                 + buffer + " " + cacheLines);
        // add the statements to the appropriate stage
        if (init || primepump) {
            initBlock.addStatement(new JExpressionStatement(null, call, null));
            initBlock
                .addStatement(new JExpressionStatement(null, assExp, null));
            //only rotate the buffer in the primepump stage
            if (primepump)
                initBlock.addStatement(new JExpressionStatement(null, rotExp, null));
        } else {
            steadyLoop.addStatement(new JExpressionStatement(null, 
                    (JMethodCallExpression)ObjectDeepCloner.deepCopy(call), null));
            steadyLoop
            .addStatement(new JExpressionStatement(null, 
                    (JAssignmentExpression)ObjectDeepCloner.deepCopy(assExp), null));
            steadyLoop
            .addStatement(new JExpressionStatement(null, 
                    (JAssignmentExpression)ObjectDeepCloner.deepCopy(rotExp), null));
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
    public void addTraceSteady(FilterInfo filterInfo, Layout layout) {
     
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
            addTraceFieldsAndMethods(exeCode, filterInfo);
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
    private void addTraceFieldsAndMethods(RawExecutionCode exeCode,
                                          FilterInfo filterInfo) {
        // add the fields of the trace
        addFields(exeCode.getVarDecls());
        // add the helper methods
        addMethods(exeCode.getHelperMethods());
        // add the init function
        addInitFunctionCall(filterInfo);
    }

    /**
     * Add the call the init function as the first statement in the
     * init stage.
     * 
     * @param filterInfo The filter.
     */
    private void addInitFunctionCall(FilterInfo filterInfo) {
        // create the params list, for some reason
        // calling toArray() on the list breaks a later pass
       /*
        List paramList = filterInfo.filter.getParams();
       
        JExpression[] paramArray;
        if (paramList == null || paramList.size() == 0)
            paramArray = new JExpression[0];
        else
            paramArray = (JExpression[]) paramList.toArray(new JExpression[0]);
            */
        JMethodDeclaration init = filterInfo.filter.getInit();
        if (init != null)
            rawMain.addStatementFirst
            (new JExpressionStatement(null,
                    new JMethodCallExpression(null, new JThisExpression(null),
                            filterInfo.filter.getInit().getName(), new JExpression[0]),
                            null));
        else
            System.out.println(" ** Warning: Init function is null");

    }

    /**
     * Add filterInfo's prime pump block to the current position of the
     * primepump code for this tile.
     * 
     * @param filterInfo The filter.
     * @param layout The layout of the application.
     */
    public void addTracePrimePump(FilterInfo filterInfo, Layout layout) {
      
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
            addTraceFieldsAndMethods(exeCode, filterInfo);
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
    public void addTraceInit(FilterInfo filterInfo, Layout layout) {
     
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
        addTraceFieldsAndMethods(exeCode, filterInfo);
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
        Iterator<OffChipBuffer> buffers = OffChipBuffer.getBuffers().iterator();
        HashSet<RawTile> visitedTiles = new HashSet<RawTile>();
        
        while (buffers.hasNext()) {
            OffChipBuffer buffer = buffers.next();
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
                JStatement dramCommand = ComputeCodeStore.sirDramCommand(true, 1, 
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
            ComputeNode[] dests =  new ComputeNode[chip.getTotalTiles() - 1];
            int index = 0;
            for (int x = 0; x < chip.getTotalTiles(); x++) {
                RawTile dest = chip.getTile(x);
                if (dest != tile) {
                    JStatement rec = 
                        new JExpressionStatement(
                                new JAssignmentExpression(new JFieldAccessExpression(TraceIRtoC.DUMMY_VOLATILE),
                                        new JFieldAccessExpression(Util.CSTIINTVAR)));
                    if (steady) 
                        dest.getComputeCode().steadyLoop.addStatement(rec);
                    else 
                        dest.getComputeCode().initBlock.addStatement(rec);
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
        Iterator<OffChipBuffer> buffers = OffChipBuffer.getBuffers().iterator();
        HashSet<RawTile> visitedTiles = new HashSet<RawTile>();
        
        while (buffers.hasNext()) {
            OffChipBuffer buffer = buffers.next();
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
                JStatement dramCommand = ComputeCodeStore.sirDramCommand(true, 1, 
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
        JStatement send = RawExecutionCode.constToSwitchStmt(constant);

        if (init)
            initBlock.addStatement(send);
        else
            steadyLoop.addStatement(send);
    }

    /**
     * Bill's code adds method <pre>meth</pre> to this, if <pre>meth</pre> is not already
     * registered as a method of this. Requires that <pre>method</pre> is non-null.
     */
    public void addMethod(JMethodDeclaration method) {
        assert method != null;
        // see if we already have <method> in this
        for (int i = 0; i < methods.length; i++) {
            if (methods[i] == method) {
                return;
            }
        }
        // otherwise, create new methods array
        JMethodDeclaration[] newMethods = new JMethodDeclaration[methods.length + 1];
        // copy in new method
        newMethods[0] = method;
        // copy in old methods
        for (int i = 0; i < methods.length; i++) {
            newMethods[i + 1] = methods[i];
        }
        // reset old to new
        this.methods = newMethods;
    }
    
    /**
     * Set methods to m.
     * 
     * @param m The methods to install.
     */
    public void setMethods(JMethodDeclaration[] m) {
        this.methods = m;
    }

    /**
     * Adds <pre>f</pre> to the fields of this. Does not check for duplicates.
     */
    public void addFields(JFieldDeclaration[] f) {
        JFieldDeclaration[] newFields = new JFieldDeclaration[fields.length
                                                              + f.length];
        for (int i = 0; i < fields.length; i++) {
            newFields[i] = fields[i];
        }
        for (int i = 0; i < f.length; i++) {
            newFields[fields.length + i] = f[i];
        }
        this.fields = newFields;
    }

    /**
     * adds field <pre>field</pre> to this, if <pre>field</pre> is not already registered as a
     * field of this.
     */
    public void addField(JFieldDeclaration field) {
        // see if we already have <field> in this
        for (int i = 0; i < fields.length; i++) {
            if (fields[i] == field) {
                return;
            }
        }
        // otherwise, create new fields array
        JFieldDeclaration[] newFields = new JFieldDeclaration[fields.length + 1];
        // copy in new field
        newFields[0] = field;
        // copy in old fields
        for (int i = 0; i < fields.length; i++) {
            newFields[i + 1] = fields[i];
        }
        // reset old to new
        this.fields = newFields;
    }

    /**
     * Adds <pre>m</pre> to the methods of this. Does not check for duplicates.
     */
    public void addMethods(JMethodDeclaration[] m) {
        JMethodDeclaration[] newMethods = new JMethodDeclaration[methods.length
                                                                 + m.length];
        for (int i = 0; i < methods.length; i++) {
            newMethods[i] = methods[i];
        }
        for (int i = 0; i < m.length; i++) {
            newMethods[methods.length + i] = m[i];
        }
        this.methods = newMethods;
    }

    /**
     * @param meth
     * @return Return true if this compute code store already has the
     * declaration of <pre>meth</pre> in its method array.
     */
    public boolean hasMethod(JMethodDeclaration meth) {
        for (int i = 0; i < methods.length; i++)
            if (meth == methods[i])
                return true;
        return false;
    }
    
    /**
     * Return the methods of this store.
     * 
     * @return the methods of this store.
     */
    public JMethodDeclaration[] getMethods() {
        return methods;
    }

    /**
     * Return the fields of this store.  
     * 
     * @return the fields of this store.  
     */
    public JFieldDeclaration[] getFields() {
        return fields;
    }

    /**
     * Set the fields of this to f.
     * 
     * @param f The fields to install.
     */
    public void setFields(JFieldDeclaration[] f) {
        this.fields = f;
    }
    
    /**
     * Return the main function of this store that 
     * will execute the init, primepump, and loop the steady-schedule.
     * 
     * @return the main function.
     */
    public JMethodDeclaration getMainFunction() {
        return rawMain;
    }
}
