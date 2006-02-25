package at.dms.kjc.spacetime;

import at.dms.kjc.*;
import at.dms.util.Utils;
import at.dms.kjc.sir.*;
import java.util.HashMap;
import java.util.List;

/**
 * ww
 * 
 * 
 * @author mgordon
 *
 */
public class ComputeCodeStore {
    public static String main = "__RAWMAIN__";

    // set to false if you do not want to generate
    // the work functions calls or inline them
    // useful for debugging
    private static final boolean CODE = true;

    protected JFieldDeclaration[] fields;

    protected JMethodDeclaration[] methods;

    // this method calls all the initialization routines
    // and the steady state routines...
    protected JMethodDeclaration rawMain;

    protected RawTile parent;

    protected JBlock steadyLoop;

    // the block that executes each tracenode's init schedule
    protected JBlock initBlock;

    // this hash map holds RawExecutionCode that was generated
    // so when we see the filter for the first time in the init
    // and if we see the same filter again, we do not have to
    // add the functions again...
    HashMap rawCode;

    public ComputeCodeStore(RawTile parent) {
        this.parent = parent;
        rawCode = new HashMap();
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

        assert buffer.getRotationLength() == 1 : 
            "The buffer connected to a file reader / writer cannot rotate!";
        parent.setMapped();
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
     * port on the gdn and send the data to <dest>
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
        parent.setMapped();
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
     * Generate code on the compute process to send over <words> words of data
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
     * Generate code to receive <words> words into a dummy volatile variable on 
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
     * Add a dram command at the current position of this code compute code 
     * for either the init stage (including primepump) or the steady state.
     * If in the init stage, don't rotate buffers.  If in init or primepump,
     * add the code to the init stage.  
     * 
     * @param read True if we want to issue a read, false if write
     * @param init true if init
     * @param primepump true if primepump
     * @param bytes The number of bytes
     * @param buffer The address to load/store
     * @param presynched True if we want all other dram commands to finish before this
     * one is issued
     * @param staticNet True if we want to use the static network, false if gdn
     */    
    public void addDRAMCommand(boolean read, boolean init, boolean primepump, int bytes,
            OffChipBuffer buffer, boolean presynched,
            boolean staticNet) {
        assert bytes > 0 : "trying to generate a dram command of size 0";
        assert !buffer.redundant() : "Trying to generate a dram command for a redundant buffer!";
                        
        parent.setMapped();
        String functName = "raw_streaming_dram" + (!staticNet ? "_gdn" : "") + 
            "_request_" +
            (read ? "read" : "write") + (presynched ? "_presynched" : "");
        
        //the name of the rotation struction we are using...
        String rotStructName = buffer.getIdent(read);
 
        // the args for the streaming dram command

        // cachelines that are needed
        assert bytes % RawChip.cacheLineBytes == 0 : "transfer size for dram must be a multiple of cache line size";

        int cacheLines = bytes / RawChip.cacheLineBytes;
        JExpression[] args = {
                new JNameExpression(null, null, rotStructName + "->buffer"), 
                new JIntLiteral(1),
                new JIntLiteral(cacheLines) };
        
        // the dram command
        JMethodCallExpression call = new JMethodCallExpression(null, functName,
                                                               args);

        // send over the address
        JFieldAccessExpression dynNetSend = 
            new JFieldAccessExpression(null,
                    new JThisExpression(null), Util.CGNOINTVAR);

        JNameExpression bufAccess = new JNameExpression(null, null, 
                rotStructName + "->buffer");
        //the assignment that will perform the dynamic network send...
        JAssignmentExpression assExp = new JAssignmentExpression(null,
                                                                 dynNetSend, bufAccess);
        //The rotation expression
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
            //only rotate in init during primepump
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
     * Add a dram read command at the current position of this code compute code 
     * for either the init stage (including primepump) or the steady state over the
     * gdn and send it to the <dest> tile.  Don't rotate the buffer we are in the init
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
        
        parent.setMapped();
        String functName = "raw_streaming_dram_gdn_request_read" + 
        (presynched ? "_presynched" : "") + "_dest";
        
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
    public void addTraceSteady(FilterInfo filterInfo) {
        parent.setMapped();
        RawExecutionCode exeCode;

        // check to see if we have seen this filter already
        if (rawCode.containsKey(filterInfo.filter)) {
            exeCode = (RawExecutionCode) rawCode.get(filterInfo.filter);
        } else {
            // otherwise create the raw ir code
            // if we can run linear or direct communication, run it
            if (filterInfo.isLinear())
                exeCode = new Linear(parent, filterInfo);
            else if (DirectCommunication.testDC(filterInfo))
                exeCode = new DirectCommunication(parent, filterInfo);
            else
                exeCode = new BufferedCommunication(parent, filterInfo);
            addTraceFieldsAndMethods(exeCode, filterInfo);
        }
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

    private void addTraceFieldsAndMethods(RawExecutionCode exeCode,
                                          FilterInfo filterInfo) {
        // add the fields of the trace
        addFields(exeCode.getVarDecls());
        // add the helper methods
        addMethods(exeCode.getHelperMethods());
        // add the init function
        addInitFunctionCall(filterInfo);
    }

    private void addInitFunctionCall(FilterInfo filterInfo) {
        // create the params list, for some reason
        // calling toArray() on the list breaks a later pass
        List paramList = filterInfo.filter.getParams();
        JExpression[] paramArray;
        if (paramList == null || paramList.size() == 0)
            paramArray = new JExpression[0];
        else
            paramArray = (JExpression[]) paramList.toArray(new JExpression[0]);

        JMethodDeclaration init = filterInfo.filter.getInit();
        if (init != null)
            rawMain.addStatementFirst(new JExpressionStatement(null,
                                                               new JMethodCallExpression(null, new JThisExpression(null),
                                                                                         filterInfo.filter.getInit().getName(), paramArray),
                                                               null));
        else
            System.out.println(" ** Warning: Init function is null");

    }

    public void addTracePrimePump(FilterInfo filterInfo) {
        parent.setMapped();
        RawExecutionCode exeCode;
        JMethodDeclaration primePump;
        
        // check to see if we have seen this filter already
        if (rawCode.containsKey(filterInfo.filter)) {
            exeCode = (RawExecutionCode) rawCode.get(filterInfo.filter);
        } else {
            // otherwise create the raw ir code
            // if we can run linear or direct communication, run it
            if (filterInfo.isLinear())
                exeCode = new Linear(parent, filterInfo);
            else if (DirectCommunication.testDC(filterInfo))
                exeCode = new DirectCommunication(parent, filterInfo);
            else
                exeCode = new BufferedCommunication(parent, filterInfo);
            addTraceFieldsAndMethods(exeCode, filterInfo);
        }
        
        primePump = exeCode.getPrimePumpMethod();
        if (primePump != null && !hasMethod(primePump) && CODE) {
            addMethod(primePump);
        }   
       
        // now add a call to the init stage in main at the appropiate index
        // and increment the index
        initBlock.addStatement(new JExpressionStatement(null,
                new JMethodCallExpression(null, new JThisExpression(null),
                        primePump.getName(), new JExpression[0]), null));
    }

    public void addTraceInit(FilterInfo filterInfo) {
        parent.setMapped();
        RawExecutionCode exeCode;

        // if we can run direct communication, run it
        if (filterInfo.isLinear())
            exeCode = new Linear(parent, filterInfo);
        else if (DirectCommunication.testDC(filterInfo))
            exeCode = new DirectCommunication(parent, filterInfo);
        else
            exeCode = new BufferedCommunication(parent, filterInfo);

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

    public void sendConstToSwitch(int constant, boolean init) {
        JStatement send = RawExecutionCode.constToSwitchStmt(constant);

        if (init)
            initBlock.addStatement(send);
        else
            steadyLoop.addStatement(send);
    }

    /**
     * Bill's code adds method <meth> to this, if <meth> is not already
     * registered as a method of this. Requires that <method> is non-null.
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
     * Adds <f> to the fields of this. Does not check for duplicates.
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
     * adds field <field> to this, if <field> is not already registered as a
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
     * Adds <m> to the methods of this. Does not check for duplicates.
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
     * declaration of <meth> in its method array.
     */
    public boolean hasMethod(JMethodDeclaration meth) {
        for (int i = 0; i < methods.length; i++)
            if (meth == methods[i])
                return true;
        return false;
    }
    
    public JMethodDeclaration[] getMethods() {
        return methods;
    }

    public JFieldDeclaration[] getFields() {
        return fields;
    }

    public JMethodDeclaration getMainFunction() {
        return rawMain;
    }
}
