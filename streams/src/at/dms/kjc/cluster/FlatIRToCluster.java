package at.dms.kjc.cluster;

import at.dms.kjc.common.MacroConversion;
import at.dms.kjc.common.CodeGenerator;
import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.iterator.*;
import at.dms.util.Utils;
import java.util.Iterator;
import java.util.HashSet;
import java.io.*;
import at.dms.compiler.*;
import at.dms.kjc.sir.lowering.*;
import java.util.*;
import at.dms.kjc.common.CodegenPrintWriter;
import at.dms.kjc.common.CommonConstants;
import at.dms.kjc.common.CommonUtils;
//import at.dms.kjc.common.RawUtil;

/**
 * This class dumps the tile code for each filter into a file based on the tile
 * number assigned.
 */
public class FlatIRToCluster extends InsertTimers implements
                                                      StreamVisitor, CodeGenerator {
    /** set to true when declarations are local (so in methods, not in fields).  
     * Tested to determine whether to add declarations. */
    private boolean declsAreLocal = false;

    /**
     * Set to true to print comments on code generation.
     * 
     * Currently just used for 'for' statment.
     * 
     * should move definition to ToCCommon...
     */
    
    static public final boolean printCodegenComments = false;

    //private boolean DEBUG = false;

    //protected String className;

    //protected boolean nl = true;

    // true if generating code for global struct
    protected boolean global = false;

// code based on this not tested and not maintained.
// If re-enabled in this class, need to uncomment some
// lines in FusionCode.java
//    // true if push and pop from provided buffers *(___in++) and *(___out++)
//    protected boolean mod_push_pop = false;

    // ?? set up in ClusterCode.generateGlobal
    public String helper_package = null;

    // current filter during sub-visits to visitFilter
    public SIRFilter filter = null;

    // List of statements to invoke before end of thread to
    // perform cleanup;
    protected Vector<String> cleanupCode = new Vector<String>();
    
    // method names communicated from visitFilter to visitMethodCallExpression
    private Set<String> method_names = new HashSet<String>();

    // ALWAYS!!!!
    // true if we are using the second buffer management scheme
    // circular buffers with anding
    public boolean debug = false;// true;

    public boolean isInit = false;

    /** &gt; 0 if in a for loop header during visit * */
    private int forLoopHeader = 0;

    // private static int filterID = 0;

    // used in hack for trying to print parameters for message sends when in
    // library format.  Should not be needed now that message sends have a
    // reasonable format for backends.
    //    int PRINT_MSG_PARAM = -1;

    //    private static int byteSize(CType type) {
    //        if (type instanceof CIntType)
    //            return 4;
    //        if (type instanceof CFloatType)
    //            return 4;
    //        if (type instanceof CDoubleType)
    //            return 8;
    //        return 0;
    //    }

    /**
     * Enable / disable code generation for a static section.
     */
    public void setGlobal(boolean g) {
        global = g;
    }
    
    /**
     * Code generation for a SIRFilter FlatNode.
     * <br/>
     * For splitter and joiner see {@link ClusterCode}
     * @param node
     */
    public static void generateCode(FlatNode node) {
        generateCode((SIRFilter) node.contents);
    }


    /**
     * Code generation for a SIRFilter.
     * <br/>
     * For splitter and joiner see {@link ClusterCode}
     * @param node
     */
    public static void generateCode(SIRFilter filter) {
        // make sure SIRPopExpression's only pop one element
        // code generation doesn't handle generating multiple pops
        // from a single SIRPopExpression
        //RemoveMultiPops.doit(contentsAsFilter);
        
        FlatIRToCluster toC = new FlatIRToCluster(filter);
        // the code below only deals with user-defined filters.
        // on a PredefinedFilter we need to go generate special code.
        if (filter instanceof SIRFileReader || filter instanceof SIRFileWriter) {
            IterFactory.createFactory().createIter(filter)
                .accept(toC);
            return;
        }

        // FieldInitMover.moveStreamInitialAssignments(contentsAsFilter;
        // FieldProp.doPropagate(contentsAsFilter);

        // Optimizations
        if (ClusterBackend.debugging)
            System.out.println("Optimizing "
                               + filter.getName() + "...");
        
//        System.err.println("Filter at FlatIRToCluster");
//        SIRToStreamIt.run(contentsAsFilter,
//                new JInterfaceDeclaration[0],
//                new SIRInterfaceTable[0],
//                new SIRStructure[0],
//                new SIRGlobal[0]);

        (new FinalUnitOptimize(){
            protected boolean optimizeThisMethod(SIRCodeUnit unit, JMethodDeclaration method) {
                return !(unit instanceof SIRTwoStageFilter 
                        && method == ((SIRTwoStageFilter)unit).getInitWork());
            }
        }).optimize(filter);

//        System.err.println("Filter after DCE");
//        SIRToStreamIt.run(contentsAsFilter,
//                new JInterfaceDeclaration[0],
//                new SIRInterfaceTable[0],
//                new SIRStructure[0],
//                new SIRGlobal[0]);
        
        IterFactory.createFactory().createIter(filter).accept(toC);
    }

    public FlatIRToCluster() {
        super();
        // tell ToC that we generate 'bool' for 'boolean', not 'int'
        hasBoolType = true;
    }

    public FlatIRToCluster(CodegenPrintWriter p) {
        super(p);
        // tell ToC that we generate 'bool' for 'boolean', not 'int'
        hasBoolType = true;
    }

    public FlatIRToCluster(SIRFilter f) {
        super();
        this.filter = f;
        // RMR { in the case that a statement is visited and not a filter,
        // as is the case when the increment statement (from for loop) is 
        // visited, selfID == -1 and hence needs to be set properly here
        this.selfID = NodeEnumerator.getSIROperatorId(f); // needed by the class
        // } RMR
        // circular = false;
        // tell ToC that we generate 'bool' for 'boolean', not 'int'
        hasBoolType = true;
    }

    //    
    // /**
    // * Close the stream at the end
    // */
    // public void close() {
    // p.close();
    // }

    /*
     * public void visitStructure(SIRStructure self, SIRStream parent,
     * JFieldDeclaration[] fields) { p.print("struct " + self.getIdent() + "
     * {\n"); for (int i = 0; i < fields.length; i++) fields[i].accept(this);
     * p.print("};\n"); }
     */

    private int selfID = -1;

    public void visitFilter(SIRFilter self, SIRFilterIter iter) {
        // create code for this filter to include in 
        // ClusterCodegenerator.generateRunFunction
        cleanupCode = new Vector<String>();

        //PPAnalyze pp = new PPAnalyze();
        //self.getWork().accept(pp);
        
        filter = self;
        // RMR { now this is redundant since it is set in the constructor
        // selfID = NodeEnumerator.getSIROperatorId(self); // needed by the class
        // } RMR

        try {
            p = new CodegenPrintWriter(new FileWriter("thread" + selfID + ".cpp"));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        method_names = new HashSet<String>();
        JMethodDeclaration[] meth = self.getMethods();
        for (int i = 0; i < meth.length; i++) {
            method_names.add(meth[i].getName());
        }

        HashSet<LatencyConstraint> sendsCreditsTo = LatencyConstraints
            .getOutgoingConstraints(self);
        
        // execution can only be restricted in the multi-threaded
        // cluster case; in standalone mode, the schedule is fixed, so
        // waiting for credits leads to infinite loop
        boolean restrictedExecution = (LatencyConstraints.isRestricted(self) &&
                                       !KjcOptions.standalone);
        boolean sendsCredits = ! sendsCreditsTo.isEmpty();

        SIRPortal outgoing[] = SIRPortal.getPortalsWithSender(self);
        SIRPortal incoming[] = SIRPortal.getPortalsWithReceiver(self);

        Vector<SIRStream> sends_to = new Vector<SIRStream>();
        Vector<SIRStream> receives_from = new Vector<SIRStream>();

        FlatNode node = NodeEnumerator.getFlatNode(selfID);

        if (ClusterBackend.debugging)
            System.out.println("flat node: " + node);

        Integer init_int = ClusterBackend.initExecutionCounts
            .get(node);
        int init_counts;

        if (init_int == null) {
            init_counts = 0;
        } else {
            init_counts = init_int.intValue();
        }

        int steady_counts = ClusterBackend.steadyExecutionCounts
                             .get(node).intValue();

        for (int t = 0; t < outgoing.length; t++) {
            SIRStream[] receivers = outgoing[t].getReceivers();
            for (int i = 0; i < receivers.length; i++) {
                sends_to.add(receivers[i]);
            }
        }

        for (int t = 0; t < incoming.length; t++) {
            SIRPortalSender[] senders = incoming[t].getSenders();
            for (int i = 0; i < senders.length; i++) {
                receives_from.add(senders[i].getStream());
            }
        }


        p.print("// peek: " + self.getPeekString() 
                + " pop: " + self.getPopString() 
                + " push " + self.getPushString()
                + "\n");
        p.print("// init counts: " + init_counts + " steady counts: "
                + steady_counts + "\n");
        p.newLine();

        int data = DataEstimate.filterGlobalsSize(self);

        CodeEstimate est = CodeEstimate.estimate(self);

        // int code = CodeEstimate.estimateCode(self);
        // int locals = CodeEstimate.estimateLocals(self);
        int code = est.getCodeSize();
        int locals = est.getLocalsSize();

        if (ClusterBackend.debugging)
            System.out.println("[globals: " + data + " locals: " + locals
                               + " code: " + code + "]");

        ClusterCodeGenerator gen = new ClusterCodeGenerator(self, self
                                                            .getFields());

        // A lot of stuff including printing field initializers.
        gen.generatePreamble(this, p); 

        // +=============================+
        // | Method Declarations |
        // +=============================+

        JMethodDeclaration work = self.getWork();

        // visit methods of filter, print the declaration first
        setDeclOnly(true);
        JMethodDeclaration[] methods = self.getMethods();
        for (int i = 0; i < methods.length; i++) {
            if (!methods[i].equals(work)) {
                methods[i].accept(this);
            }
        }

        p.newLine();
        p.print("inline void check_status__" + selfID + "();\n");
        if (SIRPortal.getPortalsWithReceiver(filter).length > 0) { 
            p.print("void check_messages__" + selfID + "();\n");
            p.print("void handle_message__" + selfID + "(netsocket *sock, int* credit);\n");
        }
        if (sendsCredits) {
            p.print("void send_credits__" + selfID + "();\n");
        }
        p.newLine();

        // +=============================+
        // | Push / Pop |
        // +=============================+

        CType input_type = self.getInputType();
        CType output_type = self.getOutputType();

        Tape in = RegisterStreams.getFilterInStream(self);
        Tape out = RegisterStreams.getFilterOutStream(self);

        // function prototype for work function
        p.print("void " + ClusterUtils.getWorkName(self, selfID)
                + "(int);\n\n");

        // declarations for input tape, pop, peek.
        if (in != null) {
            p.print(in.downstreamDeclarationExtern());
            p.print(in.downstreamDeclaration());
        } 
        
        // declarations for output tape, push.
        if (out != null) {
            p.print(out.upstreamDeclarationExtern());
            p.print(out.upstreamDeclaration());
        }
        
//        else {
//            if (!KjcOptions.standalone) {
//                // need uninteresting declarations if
//                // filter has no predecessor in graph and
//                // generating code for cluster WITHOUT standalone
//                p.print("inline " + ClusterUtils.CTypeToString(input_type)
//                        + " __init_pop_buf__" + selfID + "() {}\n");
//                p.print("inline " + ClusterUtils.CTypeToString(input_type)
//                        + " __update_pop_buf__" + selfID + "() {}\n");
//
//                p.print("void save_peek_buffer__" + selfID
//                        + "(object_write_buffer *buf) {}\n");
//                p.print("void load_peek_buffer__" + selfID
//                        + "(object_write_buffer *buf) {}\n");
//
//                p.newLine();
//            }
//        }

// W.T.F.  why should filter need even dummy routine to push
// onto its input tape?
//        if (in != null) {
//	    if (! KjcOptions.standalone) {
//            p.print("void " + in.push_name() + "("
//                    + ClusterUtils.CTypeToString(input_type) + " data) {}\n");
//            p.newLine();
//	    }
//        }


        // +=============================+
        // | Declare timers |
        // +=============================+

        // declare profiling timers
        if (KjcOptions.profile) {
            p.println("extern proc_timer timers[];");
        }
        p.newLine();

        if ((self instanceof SIRFileReader) ||
            (self instanceof SIRFileWriter)) {
            
            if (self instanceof SIRFileReader) { p.println("#include <FileReader.h>"); }
            if (self instanceof SIRFileWriter) { p.println("#include <FileWriter.h>"); }
            
            p.println("int __file_descr__"+selfID+";\n");
        }

        // +=============================+
        // | Method Bodies |
        // +=============================+



        setDeclOnly(false);
        for (int i = 0; i < methods.length; i++) {
            if (!methods[i].equals(work))
                methods[i].accept(this);
            // methods[i].accept(this);
        }


        if ((self instanceof SIRFileReader) ||
            (self instanceof SIRFileWriter)) {
            
            if (self instanceof SIRFileReader) {
                
                p.println("void save_file_pointer__"+selfID+"(object_write_buffer *buf)");
                p.println("{ buf->write_int(FileReader_getpos(__file_descr__"+selfID+")); }\n");
                
                p.println("void load_file_pointer__"+selfID+"(object_write_buffer *buf)");
                p.println("{ FileReader_setpos(__file_descr__"+selfID+", buf->read_int()); }\n");
            }
            
            if (self instanceof SIRFileWriter) {
                
                p.println("void save_file_pointer__"+selfID+"(object_write_buffer *buf)");
                p.println("{ FileWriter_flush(__file_descr__"+selfID+");");
                p.println("  buf->write_int(FileWriter_getpos(__file_descr__"+selfID+")); }\n");
                
                p.println("void load_file_pointer__"+selfID+"(object_write_buffer *buf)");
                p.println("{ FileWriter_setpos(__file_descr__"+selfID+", buf->read_int()); }\n");
            }
            
        } else {
            
            p.println("void save_file_pointer__"+selfID+"(object_write_buffer *buf) {}");
            p.println("void load_file_pointer__"+selfID+"(object_write_buffer *buf) {}");
            
        }

        // +=============================+
        // | Work Function (int ____n) |
        // +=============================+

// produced code was never tested, and has not been maintained.
//        boolean printed_BUFFER_MERGE = false;
//        
//        if (!(self instanceof SIRFileReader) &&
//            !(self instanceof SIRFileWriter) &&
//            (in != null && out != null)) {
//            
//            if (! printed_BUFFER_MERGE) {
//                p.println("\n\n#ifdef BUFFER_MERGE\n");
//                printed_BUFFER_MERGE = true;
//            }
//            p.println();
//            p.println("void "+work.getName()+"__"+selfID+"__mod(int ____n, "+input_type+" *____in, "+output_type+" *____out) {");
//            p.print("  for (; (0 < ____n); ____n--)\n");
//            
//            mod_push_pop = true;
//            work.getBody().accept(this);
//            mod_push_pop = false;
//            
//            p.println("}\n");
//            
//        }
//        
//        if (!(self instanceof SIRFileReader) &&
//            !(self instanceof SIRFileWriter) &&
//            (in != null && out != null)) {
//            
//            if (! printed_BUFFER_MERGE) {
//                p.println("\n\n#ifdef BUFFER_MERGE\n");
//                printed_BUFFER_MERGE = true;
//            }
//            p.println();
//            p.println("void "+work.getName()+"__"+selfID+"__mod2(int ____n, "+input_type+" *____in, "+output_type+" *____out, int s1, int s2) {");
//            p.print("  for (; (0 < ____n); (____n--, ____in+=s1, ____out+=s2))\n");
//            
//            mod_push_pop = true;
//            work.getBody().accept(this);
//            mod_push_pop = false;
//            
//            p.println("}\n");
//            
//        }
//        
//        if (printed_BUFFER_MERGE) {
//        p.println("\n#endif // BUFFER_MERGE\n\n");
//        }
        
        JBlock block = new JBlock(null, new JStatement[0], null);
        
        JVariableDefinition counter = new JVariableDefinition(null, 0,
                                                              CStdType.Integer, "____n", new JIntLiteral(0));
        
        /*
         * 
         * JVariableDefinition tmp = new JVariableDefinition(null, 0,
         * CStdType.Integer, "____tmp", new JLocalVariableExpression(null,
         * counter)); // int ____tmp = ____n;
         * 
         */

        JVariableDefinition iter_counter = new JVariableDefinition(null, 0,
                                                                   CStdType.Integer, "__counter_" + selfID, null);

        JStatement init = new JEmptyStatement(null, null);

        // VariableDeclarationStatement(null, tmp, null);

        JExpression decrExpr = new JPostfixExpression(null,
                                                      Constants.OPE_POSTDEC, new JLocalVariableExpression(null,
                                                                                                          counter));


        JExpression incrExpr = new JPostfixExpression(null,
                                                      Constants.OPE_POSTINC, new JLocalVariableExpression(null,
                                                                                                          iter_counter));

        JStatement decr = new JExpressionStatement(null, decrExpr, null);
        //JStatement incr = new JExpressionStatement(null, incrExpr, null);

        JExpression arr[] = new JExpression[2];
        arr[0] = decrExpr;
        arr[1] = incrExpr;

        JExpressionListStatement list = new JExpressionListStatement(null, arr, null);

        JExpression cond = new JRelationalExpression(null, Constants.OPE_LT,
                                                     new JIntLiteral(0), new JLocalVariableExpression(null, counter));

        if (/*self instanceof SIRPredefinedFilter*/ 
                self instanceof SIRFileReader || self instanceof SIRFileWriter) {
            // special code generated for file readers and file writers
            BuiltinsCodeGen.predefinedFilterWork((SIRPredefinedFilter) self, selfID, p);
        } else {

            block.addStatement(new JForStatement(null, init, cond, 
                                                 (outgoing.length + incoming.length == 0 ? decr : list), 
                                                 work.getBody(),
                                                 new JavaStyleComment[] {
                                                     new JavaStyleComment("FlatIRToCluster work function", true,
                                                                          false, false)
                                                 }));

            // __counter_X = __counter_X + ____n;

            /*
             * block.addStatement(new JExpressionStatement( null, new
             * JAssignmentExpression (null, new JLocalVariableExpression (null,
             * iter_counter), new JAddExpression(null, new
             * JLocalVariableExpression (null, iter_counter), new
             * JLocalVariableExpression (null, counter))), null));
             * 
             * 
             */

            JFormalParameter param = new JFormalParameter(null, 0,
                                                          CStdType.Integer, "____n", true);
            JFormalParameter params[] = new JFormalParameter[1];
            params[0] = param;
            JMethodDeclaration work_n = new JMethodDeclaration(null,
                                                               at.dms.kjc.Constants.ACC_PUBLIC, CStdType.Void, work
                                                               .getName(), params, CClassType.EMPTY, block, null,
                                                               null);

            work_n.accept(this);
        }

        // +=============================+
        // | Check Messages |
        // +=============================+

        if (SIRPortal.getPortalsWithReceiver(filter).length > 0) {
        
        p.print("\nvoid check_messages__" + selfID + "() {\n");

        p.print("  message *msg, *last = NULL;\n");

        if (restrictedExecution) {
            p.print("  while (");
            Iterator<SIRStream> i = receives_from.iterator();
            while (i.hasNext()) {
                int src = NodeEnumerator.getSIROperatorId(i.next());
                p.print("__credit_" + src + "_" + selfID + " <= __counter_" + selfID);
                if (i.hasNext()) p.print(" || ");
            }
            p.print(") {\n");
        }

        if (! KjcOptions.standalone) { 
            //p.print("#ifndef __CLUSTER_STANDALONE\n");

            {
                Iterator<SIRStream> i = receives_from.iterator();
                while (i.hasNext()) {
                    int src = NodeEnumerator.getSIROperatorId(i.next());
                    p.print("  while (__msg_sock_" + src + "_" + selfID
                            + "in->data_available()) {\n    handle_message__"
                            + selfID + "(__msg_sock_" + src + "_" + selfID
                            + "in, &__credit_" + src + "_" + selfID +");\n  } // if\n");
                }
            }

            //p.print("#endif // __CLUSTER_STANDALONE\n");
        }
        if (restrictedExecution) {
            p.print("  } // while \n");
        }

        p.print("  for (msg = __msg_stack_" + selfID
                + "; msg != NULL; msg = msg->next) {\n");
        p.print("    if (msg->execute_at == __counter_" + selfID + ") {\n");

        SIRPortal[] portals = SIRPortal.getPortalsWithReceiver(self);

        // this block will need to be adjusted to receive from
        // multiple portals of different types (different interfaces)
        if (portals.length > 0) {

            CClass pclass = portals[0].getPortalType().getCClass();

            CMethod pmethods[] = pclass.getMethods();

            for (int i = 0; i < pmethods.length; i++) {

                CMethod portal_method = pmethods[i];
                CType portal_method_params[] = portal_method.getParameters();

                String method_name = portal_method.getIdent();

                int length = method_name.length();

                if (!method_name.startsWith("<") && !method_name.endsWith(">")) {

                    p.print("      if (msg->method_id == " + i + ") {\n");

                    for (int t = 0; t < methods.length; t++) {

                        String thread_method_name = methods[t].getName();

                        if (thread_method_name.startsWith(method_name)
                            && thread_method_name.charAt(length) == '_'
                            && thread_method_name.charAt(length + 1) == '_') {

                            int param_count = methods[t].getParameters().length;

                            // declare variables for parameters
                            for (int a = 0; a < param_count; a++) {
                                p.print("        ");
                                printDecl(portal_method_params[a], "p"+a);
                                p.print(";\n");
                            }

                            // assign to parameters
                            for (int a = 0; a < param_count; a++) {
                                msgGet(portal_method_params[a], "p" + a);
                            }

                            p.print("        " + thread_method_name + "__"
                                    + selfID + "(");
                            for (int a = 0; a < param_count; a++) {
                                if (a > 0)
                                    p.print(", ");
                                p.print("p" + a);
                            }
                            p.print(");\n");
                        }
                    }
                    p.print("      }\n");
                }
            }

            p.print("      if (last != NULL) { \n");
            p.print("        last->next = msg->next;\n");
            p.print("      } else {\n");
            p.print("        __msg_stack_" + selfID + " = msg->next;\n");
            p.print("      }\n");
            p.print("      delete msg;\n");

        }

        p.print("    } else if (msg->execute_at > __counter_" + selfID + ") {\n");
        p.print("      last = msg;\n");
        p.print("    } else { // msg->execute_at < __counter_" + selfID + "\n");
        p.print("      message::missed_delivery();\n");
        p.print("    }");
        p.print("  } // for \n");

        p.print("}\n");

        // +=============================+
        // | Handle Message |
        // +=============================+

        p.print("\nvoid handle_message__" + selfID + "(netsocket *sock, int* credit) {\n");
        p.print("  int size = sock->read_int();\n");

        if (restrictedExecution) {
            p.print("  if (size == -1) { // a credit message received\n");
            p.print("    *credit = sock->read_int();\n");
            p.print("    return;\n");
            p.print("  }\n");
        }

        p.print("  int index = sock->read_int();\n");
        p.print("  int iteration = sock->read_int();\n");
        p
            .print("  //fprintf(stderr,\"Message receieved! thread: "
                   + selfID
                   + ", method_index: %d excute at iteration: %d\\n\", index, iteration);\n");

        p.print("  message *msg = new message(size, index, iteration);\n");
        p.print("  msg->read_params(sock);\n");
        p.print("  __msg_stack_" + selfID
                + " = msg->push_on_stack(__msg_stack_" + selfID + ");\n");
        p.print("}\n");
        }
        // +=============================+
        // | Send Credits |
        // +=============================+

        if (sendsCredits) {
        Iterator<LatencyConstraint> constrIter;
        constrIter = sendsCreditsTo.iterator();
        while (constrIter.hasNext()) {
            LatencyConstraint constraint = constrIter
                .next();
            int receiver = NodeEnumerator.getSIROperatorId(constraint
                                                           .getReceiver());

            int sourcePhase = constraint.getSourceSteadyExec();

            p.print("int __init_" + selfID + "_" + receiver + " = "
                    + constraint.getSourceInit() + ";\n");
            p.print("int __source_phase_" + selfID + "_" + receiver + " = "
                    + sourcePhase + ";\n");
            p.print("int __dest_phase_" + selfID + "_" + receiver + " = "
                    + constraint.getDestSteadyExec() + ";\n");
            p.print("int __current_" + selfID + "_" + receiver + " = 0;\n");
            p.print("int __dest_offset_" + selfID + "_" + receiver + " = 0;\n");

            p.print("int __dependency_" + selfID + "_" + receiver + "[] = {");

            int y;

            for (y = 0; y < sourcePhase - 1; y++) {
                p.print(constraint.getDependencyData(y) + ",");
            }

            p.print(constraint.getDependencyData(y) + "};\n");

            p.newLine();
        }

        p.print("\ninline void send_credits__" + selfID + "() {\n");

        p.print("  int tmp;\n");

        // HashSet sendsCreditsTo =
        // LatencyConstraints.getOutgoingConstraints(self);
        // boolean restrictedExecution = LatencyConstraints.isRestricted(self);
        // boolean sendsCredits;

        constrIter = sendsCreditsTo.iterator();
        while (constrIter.hasNext()) {
            LatencyConstraint constraint = constrIter
                .next();

            int receiver = NodeEnumerator.getSIROperatorId(constraint
                                                           .getReceiver());

            if (LatencyConstraints.isMessageDirectionDownstream(self,
                                                                constraint.getReceiver())) {

                // message and credit is sent downstream

                p.print("  if (__counter_" + selfID + " > __init_" + selfID
                        + "_" + receiver + ") {\n");
                p.print("    tmp = __dependency_" + selfID + "_" + receiver
                        + "[__current_" + selfID + "_" + receiver + "];\n");
                p.print("    if (tmp > 0) {\n");
                p.print("      __msg_sock_" + selfID + "_" + receiver
                        + "out->write_int(-1);\n");
                p.print("      __msg_sock_" + selfID + "_" + receiver
                        + "out->write_int(tmp + __dest_offset_" + selfID + "_"
                        + receiver + ");\n");
                p.print("    }\n");
                p.print("    __current_" + selfID + "_" + receiver
                        + " = (__current_" + selfID + "_" + receiver
                        + " + 1) % __source_phase_" + selfID + "_" + receiver
                        + ";\n");
                p
                    .print("    if (__current_" + selfID + "_" + receiver
                           + " == 0) __dest_offset_" + selfID + "_"
                           + receiver + " += __dest_phase_" + selfID + "_"
                           + receiver + ";\n");
                p.print("  }\n");
            } else {

                // message and credit is sent upstream

                p.print("  if (__counter_" + selfID + " == 0) {\n");
                p.print("    __msg_sock_" + selfID + "_" + receiver
                        + "out->write_int(-1);\n");
                p.print("    __msg_sock_" + selfID + "_" + receiver
                        + "out->write_int(__init_" + selfID + "_" + receiver
                        + ");\n");
                p.print("  } else {\n");
                p.print("    tmp = __dependency_" + selfID + "_" + receiver
                        + "[__current_" + selfID + "_" + receiver + "];\n");
                p.print("    if (tmp > 0) {\n");
                p.print("      __msg_sock_" + selfID + "_" + receiver
                        + "out->write_int(-1);\n");
                p.print("      __msg_sock_" + selfID + "_" + receiver
                        + "out->write_int(tmp + __dest_offset_" + selfID + "_"
                        + receiver + ");\n");
                p.print("    }\n");
                p.print("    __current_" + selfID + "_" + receiver
                        + " = (__current_" + selfID + "_" + receiver
                        + " + 1) % __source_phase_" + selfID + "_" + receiver
                        + ";\n");
                p
                    .print("    if (__current_" + selfID + "_" + receiver
                           + " == 0) __dest_offset_" + selfID + "_"
                           + receiver + " += __dest_phase_" + selfID + "_"
                           + receiver + ";\n");
                p.print("  }\n");

            }
        }

        p.print("}\n");
        }
        
        // +=============================+
        // | Cluster Main |
        // +=============================+

        Vector run = 
            gen.generateRunFunction(filter.getInit().getName() + "__"
                                    + selfID, 
                                    ClusterUtils.getWorkName(filter, selfID),
                                    cleanupCode);

        for (int i = 0; i < run.size(); i++) {
            p.print(run.elementAt(i).toString());
        }

        p.close();
    }
    
    /** 
     * generate code for assigning message contents fo a variable.
     * @param typ     CType of message
     * @param varName name of variable
     */
    private void msgGet(CType typ, String varName) {
        if (typ instanceof CBooleanType || typ instanceof CNumericType) {
            // read primitives by calling function and assigning result
            p.print("        " + varName + " = msg->get_"
                    + CommonUtils.CTypeToString(typ, hasBoolType)
                    + "_param();\n");
        } else if (typ.isArrayType() && ( ((CArrayType)typ).getBaseType() instanceof CBooleanType ||
                                          ((CArrayType)typ).getBaseType() instanceof CNumericType)) {
            // read arrays of primitive types by passing array pointer and length
            CArrayType arrType = (CArrayType) typ;
            CType baseType = arrType.getBaseType();
            p.print("        msg->get_"
                    + CommonUtils.CTypeToString(baseType,
                            hasBoolType)
                    + "_array_param(("
                    + CommonUtils.CTypeToString(baseType,
                            hasBoolType) + "*)" + varName + ", "
                    + arrType.getTotalNumElements() + ");\n");
        } else {
            // everything else (structure types, arrays of structures):
            // get total length into a void* pointer
            // [note that this could work for all cases above, too...]
            p.print("        msg->get_custom_param((void*)" + varName + ", " +
                    "sizeof(" + typ + " ));\n");
        }
    }
    
    /**
     * Generate code to push the contents of a message. 
     * @param exp
     * @param typ
     */
    private void msgPush(JExpression exp, CType typ) {
        if (typ instanceof CBooleanType || typ instanceof CNumericType) {
            // push primitives by passing only value
            p.print("  __msg->push_"
                    + CommonUtils.CTypeToString(typ, hasBoolType) + "(");
            exp.accept(this);
            p.print(");\n");
        }  else if (typ.isArrayType() && (((CArrayType)typ).getBaseType() instanceof CBooleanType ||
                                          ((CArrayType)typ).getBaseType() instanceof CNumericType)) {
            // push arrays of primitives by passing array pointer and length
            CArrayType arrType = (CArrayType) typ;
            CType baseType = arrType.getBaseType();
            p.print("  __msg->push_"
                    + CommonUtils.CTypeToString(baseType, hasBoolType)
                    + "_array(("
                    + CommonUtils.CTypeToString(baseType, hasBoolType)
                    + "*)");
            exp.accept(this);
            p.print(", " + arrType.getTotalNumElements() + ");\n");
        } else {
            // everything else (structure types, arrays of structures):
            // push pointer and total length (will be treated as void*)
            // [note that this could work for all cases above, too...]
            p.print("  __msg->push_custom_type((void*)");
            exp.accept(this);
            p.print(", sizeof(" + typ + "));\n");
        }
    }


    public void visitPhasedFilter(SIRPhasedFilter self, SIRPhasedFilterIter iter) {
        // This is a stub; it'll get filled in once we figure out how phased
        // filters should actually work.
    }

    /**
     * prints a method declaration
     */
    public void visitMethodDeclaration(JMethodDeclaration self, int modifiers,
                                       CType returnType, String ident, JFormalParameter[] parameters,
                                       CClassType[] exceptions, JBlock body) {
        declsAreLocal = true;
        if (filter != null && (filter instanceof SIRFileReader || filter instanceof SIRFileWriter)
            && self.getName().startsWith("init") && !isDeclOnly()) {
            BuiltinsCodeGen.predefinedFilterInit((SIRPredefinedFilter) filter,
                                                 returnType, ident + "__" + selfID, selfID, cleanupCode, p);
            return;
        }

        p.print(" ");
        // System.out.println(ident);

        // in the raw path we do not want to print the
        // prework or work function definition

        // this prevented some work functions from printing out
        /*
         * if (filter != null && (filter.getWork().equals(self) || (filter
         * instanceof SIRTwoStageFilter &&
         * ((SIRTwoStageFilter)filter).getInitWork().equals(self)))) return;
         */

        // try converting to macro
        if (MacroConversion.shouldConvert(self)) {
            MacroConversion.doConvert(self, isDeclOnly(), this);
            declsAreLocal = false;
            return;
        }

        if (helper_package == null)
            p.newLine();

        // p.print(CModifier.toString(modifiers));
        printType(returnType);
        p.print(" ");
        if (global)
            p.print("__global__");
        if (helper_package != null)
            p.print(helper_package + "_");
        p.print(ident);
        // this breaks initpath
        if (helper_package == null && !global
            && !ident.startsWith("__Init_Path_")) {
            p.print("__" + selfID);
        }
        p.print("(");
        int count = 0;

        for (int i = 0; i < parameters.length; i++) {
            if (count != 0) {
                p.print(", ");
            }
            parameters[i].accept(this);
            count++;
        }
        p.print(")");

        // print the declaration then return
        if (isDeclOnly()) {
            p.print(";");
            return;
        }

        method = self;

        // for testing: print out comments.
        //        {
        //            p.print("/* Method declaration comments:\n");
        //            JavaStyleComment[] comments;
        //            comments = body.getComments();
        //            if (comments != null) {
        //                for (int i = 0; i < comments.length; i++) {
        //                    JavaStyleComment c = comments[i];
        //                    p.print(c.getText());
        //                    if (c.isLineComment() || c.hadSpaceAfter()) {
        //                        p.newLine();
        //                    }
        //               }
        //           }
        //           p.print(" */\n");
        //        }

        // set is init for dynamically allocating arrays...
        if (filter != null && self.getName().startsWith("init")) {
            isInit = true;
        }
        if (body != null) {
            body.accept(this);
        } else {
            p.print(";");
        }
        p.newLine();
        isInit = false;
        declsAreLocal = false;
        method = null;
    }

    /*
     * Doesn't seem to be used private void dummyWork(int push) {
     * p.print("{\n"); p.print(" int i;\n"); p.print(" for(i = 0; i < " + push + ";
     * i++)\n"); p.print(" static_send(i);\n"); p.print("}\n"); }
     */

    /*
     * Doesn't seem to be used private int nextPow2(int i) { String str =
     * Integer.toBinaryString(i); if (str.indexOf('1') == -1) return 0; int bit =
     * str.length() - str.indexOf('1'); return (int)Math.pow(2, bit); }
     */

    // ----------------------------------------------------------------------
    // STATEMENT
    // ----------------------------------------------------------------------
    /*
     * prints a while statement
     */

    /*
     * prints a variable declaration statement
     */

    /*
     * I don't see why we need a new instance -- A.D. why not
     * p.print("["); dims[i].accept(this); p.print("]");
     */

    /*
     * FlatIRToCluster toC = new FlatIRToCluster(); dims[i].accept(toC);
     * p.print("[" + toC.getString() + "]"); } }
     */

    /**
     * prints a variable declaration statement
     */
    public void visitVariableDefinition(JVariableDefinition self,
                                        int modifiers, CType type, String ident, JExpression expr) {

        if ((modifiers & ACC_STATIC) != 0) {
            p.print ("static ");
            if ((modifiers & ACC_FINAL) != 0) {
                p.print ("const ");
            }
        }
        if (expr instanceof JArrayInitializer) {
            declareInitializedArray(type, ident, expr);
        } else {

            printDecl (type, ident);

            if (expr != null) {
                p.print ("\t= ");
                expr.accept (this);
            } 
// 05-Dec-2006: Do not initialize unless initialized in Kopi code.
// For arrays of vector type was creating code that could not be converted to C.
// For arrays from fusion was causing unnecessary overhead.
// Assume now that compiler writers know whether variable needs initailization!
            else if (declsAreLocal) {
                if (type.isOrdinal()) { p.print(" = 0"); }
                else if (type.isFloatingPoint()) {p.print(" = 0.0f"); }
                else if (type.isArrayType()) {
                    if (! (((CArrayType)type).getBaseType() instanceof CVectorType)
                     && ! (((CArrayType)type).getBaseType() instanceof CVectorTypeLow)) {
                            p.print(" = {0}");
                        } 
                    }
                else if (type.isClassType()) {
                    if (((CClassType)type).toString().equals("java.lang.String")) {
                        p.print(" = NULL;"); 
                    } else {
                        p.print(" = {0}");
                    }
                }
            }
            p.print(";/* " + type + " */");
        }
    }

    /*
     * prints a switch statement
     */

    /*
     * prints a return statement
     */

    /*
     * prints a labeled statement
     */

    /*
     * prints a if statement inherited from ToC
     */

    /*
     * declareInitializedArray Inherited from ToC
     */
    
    /**
     * prints a for statement
     */
    public void visitForStatement(JForStatement self, JStatement init,
                                  JExpression cond, JStatement incr, JStatement body) {
        if (printCodegenComments) {
            JavaStyleComment[] comments = self.getComments();
            if (comments != null && comments.length > 0) {
                p.println("/*");
                for (int i = 0; i < comments.length; i++) {
                    p.println(comments[i].getText());
                }
                p.println("*/");
            }
        }
        
        // be careful, if you return prematurely, decrement me
        forLoopHeader++;

        p.print("for (");
        if (init != null) {
            init.accept(this);
            // the ; will print in a statement visitor
        }

        p.print(" ");
        if (cond != null) {
            cond.accept(this);
        }
        // cond is an expression so print the ;
        p.print("; ");
        if (incr != null) {
            FlatIRToCluster l2c = new FlatIRToCluster(filter);
            incr.accept(l2c);
            // get String
            String str = l2c.p.getString();
            // leave off the trailing semicolon if there is one
            if (str.endsWith(";")) {
                p.print(str.substring(0, str.length() - 1));
            } else {
                p.print(str);
            }
        }

        forLoopHeader--;
        p.print(") ");

        p.print("{");
        p.indent();
        body.accept(this);
        p.outdent();
        p.newLine();
        p.print("}");
    }

    /*
     * prints a compound statement (2-argument form)
     */

    /**
     * prints a compound statement
     */
    public void visitCompoundStatement(JStatement[] body) {
        for (int i = 0; i < body.length; i++) {
            if (body[i] instanceof JIfStatement && i < body.length - 1
                && !(body[i + 1] instanceof JReturnStatement)) {
                p.newLine();
            }
            if (body[i] instanceof JReturnStatement && i > 0) {
                p.newLine();
            }

            p.newLine();
            body[i].accept(this);

            if (body[i] instanceof JVariableDeclarationStatement
                && i < body.length - 1
                && !(body[i + 1] instanceof JVariableDeclarationStatement)) {
                p.newLine();
            }
        }
    }

    /*
     * prints an expression statement
     */

    /*
     * prints an expression list statement
     */

    /*
     * prints a empty statement
     */
    public void visitEmptyStatement(JEmptyStatement self) {
        // if we are inside a for loop header, we need to print
        // the ; of an empty statement
        if (forLoopHeader > 0) {
            p.newLine();
            p.print(";");
        }
    }

    /*
     * prints a do statement
     */

    /*
     * prints a continue statement
     */

    /*
     * prints a break statement
     */

    /*
     * prints a block statement
     */

    /*
     * prints a type declaration statement
     */

    // ----------------------------------------------------------------------
    // EXPRESSION
    // ----------------------------------------------------------------------
    /*
     * prints an unary plus expression
     */

    /*
     * prints an unary minus expression
     */

    /*
     * prints a bitwise complement expression
     */

    /*
     * prints a logical complement expression
     */

    /*
     * prints a type name expression
     */

    /*
     * prints a this expression visitThisExpression (ToC)
     */

    /*
     * prints a super expression visitSuperExpression (ToC)
     */

    /*
     * prints a shift expression
     */

    /*
     * prints a relational expression visitRelationalExpression (ToC)
     */

    /*
     * prints a prefix expression
     */

    /*
     * prints a postfix expression
     */

    /*
     * prints a parenthesized expression
     */

    /*
     * prints a name expression
     */

    /*
     * prints an binary expression visitBinaryExpression (TOC)
     */

    /**
     * prints a method call expression
     */
    public void visitMethodCallExpression(JMethodCallExpression self,
                                          JExpression prefix, String ident, JExpression[] args) {

        //        if (PRINT_MSG_PARAM > -1) {
        //            visitArgs(args, 0);
        //            return;
        //        }

        /*
         * if (ident != null && ident.equals(JAV_INIT)) { return; // we do not
         * want generated methods in source code }
         */

        // generate the inline asm instruction to execute the
        // receive if this is a receive instruction
//        if (ident.equals(CommonConstants.receiveMethod)) {
//
//            // Do not generate this!
//
//            /*
//             * p.print(Util.staticNetworkReceivePrefix()); visitArgs(args,0);
//             * p.print(Util.staticNetworkReceiveSuffix(args[0].getType()));
//             */
//
//            return;
//        }

        if (prefix instanceof JTypeNameExpression) {
            JTypeNameExpression nexp = (JTypeNameExpression) prefix;
            String name = nexp.getType().toString();
            if (!name.equals("java.lang.Math"))
                p.print(name + "_");
        }

        boolean wrapping = false;
        // math functions are converted to use their floating-point counterparts;
        if (Utils.isMathMethod(prefix, ident)) {
            // also insert profiling code for math functions
            wrapping = true;
            super.beginWrapper("FUNC_" + ident.toUpperCase());
            p.print(Utils.cppMathEquivalent(prefix, ident));
        } else {
            p.print(ident);
        }


        if (!Utils.isMathMethod(prefix, ident) && ident.indexOf("::") == -1) {
            // don't rename the built-in math functions
            // don't rename calls to static functions

            if (method_names.contains(ident))
                p.print("__" + selfID);
        }
        p.print("(");

        // if this method we are calling is the call to a structure
        // receive method that takes a pointer, we have to add the
        // address of operator
        if (ident.startsWith(CommonConstants.structReceiveMethodPrefix))
            p.print("&");

        int i = 0;
        /*
         * Ignore prefix, since it's just going to be a Java class name. if
         * (prefix != null) { prefix.accept(this); i++; }
         */
        visitArgs(args, i);
        p.print(")");

        // close any profiling code we might have started
        if (wrapping) {
            super.endWrapper();
        }
    }

    /*
     * prints a local variable expression
     */

    /*
     * prints an equality expression
     */

    /*
     * prints a conditional expression
     */

    /*
     * prints a compound expression
     */

    /**
     * prints a field expression
     */
    public void visitFieldExpression(JFieldAccessExpression self,
                                     JExpression left, String ident) {
        if (ident.equals(JAV_OUTER_THIS)) {// don't generate generated fields
            p.print(left.getType().getCClass().getOwner().getType() + "->this");
            return;
        }
        if (ident.startsWith("RAND_MAX")) {
            p.print(ident);
            return;
        }
        int index = ident.indexOf("_$");
        if (index != -1) {
            p.print(ident.substring(0, index)); // local var
        } else {
            p.print("(");

            if (global) {
                if (left instanceof JThisExpression) {
                    p.print("__global__" + ident);
                } else {
                    left.accept(this);
                    p.print(".");
                    p.print(ident);
                }
            } else if (left instanceof JThisExpression) {
                p.print(ident + "__" + selfID);
            } else if (left instanceof JTypeNameExpression
                       && ((JTypeNameExpression) left).getType().toString()
                       .equals("TheGlobal")) {
                p.print("__global__");
                p.print(ident);
            } else {
                left.accept(this);
                p.print(".");
                p.print(ident);
            }

            p.print(")");
        }
    }

    /*
     * prints a cast expression
     */

    /*
     * prints a bitwise expression visitBitwiseExpression (ToC)
     */

    /**
     * prints an assignment expression
     */
    public void visitAssignmentExpression(JAssignmentExpression self,
                                          JExpression left, JExpression right) {

        if (left.getType() != null
            && left.getType().toString().endsWith("Portal")) {
            p.print("/* void */");
            return;
        }

        // print the correct code for array assignment
        // this must be run after renaming!!!!!!
        if (left.getType() == null || right.getType() == null) {
            lastLeft = left;
            printLParen();
            left.accept(this);
            p.print(" = ");
            right.accept(this);
            printRParen();
            return;
        }

        if (right instanceof SIRPopExpression) {
            CodegenPrintWriter oldWriter = p;
            p = new CodegenPrintWriter();
            left.accept(this);
            String leftString = p.getString();
            p = oldWriter;
            // note following line does not work with mod_push_pop
            // but mod_push_pop does not work with arrays over tapes.
            p.print(RegisterStreams.getFilterInStream(filter).assignPopToVar(leftString));
            return;
        }

        if (right instanceof SIRPeekExpression) {
            CodegenPrintWriter oldWriter = p;
            p = new CodegenPrintWriter();
            left.accept(this);
            String leftString = p.getString();
            p = new CodegenPrintWriter();
            ((SIRPeekExpression)right).getArg().accept(this);
            String argstring = p.getString();
            p = oldWriter;
            // note following line does not work with mod_push_pop
            // but mod_push_pop does not work with arrays over tapes.
            p.print(RegisterStreams.getFilterInStream(filter).assignPeekToVar(leftString,argstring));
            return;
        }

        
        // copy arrays element-wise
        boolean arrayType = ((left.getType()!=null && left.getType().isArrayType()) ||
                             (right.getType()!=null && right.getType().isArrayType()));
        if (arrayType && !(right instanceof JNewArrayExpression)) {
            CArrayType type;
            try {
                type = (CArrayType)right.getType();
            } catch (ClassCastException e) {
                System.err.println("Warning: copy from non-array type to array type: " + left + " = " + right);
                p.println("/* Warning: copy from non-array type to array type. */");
                type = (CArrayType)left.getType();
            }
            String dims[] = this.makeArrayStrings(type.getDims());

            // dims should never be null now that we have static array
            // bounds
            assert dims != null;
            /*
            // if we cannot find the dim, just create a pointer copy
            if (dims == null) {
            lastLeft = left;
            printLParen();
            left.accept(this);
            p.print(" = ");
            right.accept(this);
            printRParen();
            return;
            }
            */


            p.print("{\n");
            p.print("int ");
            // print the index var decls
            for (int i = 0; i < dims.length - 1; i++)
                p.print(CommonConstants.ARRAY_COPY + i + ", ");
            p.print(CommonConstants.ARRAY_COPY + (dims.length - 1));
            p.print(";\n");
            for (int i = 0; i < dims.length; i++) {
                //p.println("// FlatIRToCluster_7");
                p.print("for (" + CommonConstants.ARRAY_COPY + i + " = 0; "
                        + CommonConstants.ARRAY_COPY + i + " < " + dims[i]
                        + "; " + CommonConstants.ARRAY_COPY + i + "++)\n");
            }
            left.accept(this);
            for (int i = 0; i < dims.length; i++)
                p.print("[" + CommonConstants.ARRAY_COPY + i + "]");
            p.print(" = ");
            right.accept(this);
            for (int i = 0; i < dims.length; i++)
                p.print("[" + CommonConstants.ARRAY_COPY + i + "]");
            p.print(";\n}\n");

            return;
        }

        // stack allocate all arrays in the work function
        // done at the variable definition,
        if (!isInit && right instanceof JNewArrayExpression
            && (left instanceof JLocalVariableExpression)) {
            // (((CArrayType)((JNewArrayExpression)right).getType()).getArrayBound()
            // < 2)) {

            // get the basetype and print it
            CType baseType = ((CArrayType) ((JNewArrayExpression) right)
                              .getType()).getBaseType();
            printType(baseType);
            p.print(" ");
            // print the identifier

            // Before the ISCA hack this was how we printed the var ident
            // left.accept(this);

            String ident;
            ident = ((JLocalVariableExpression) left).getVariable().getIdent();

            //            if (KjcOptions.ptraccess) {
            //
            //                // HACK FOR THE ICSA PAPER, !!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            //                // turn all array access into access of a pointer pointing to
            //                // the array
            //                p.print(ident + "_Alloc");
            //
            //                // print the dims of the array
            //                printDecl(left.getType(), ident);
            //
            //                // print the pointer def and the assignment to the array
            //                p.print(";\n");
            //                p.print(baseType + " *" + ident + " = " + ident + "_Alloc");
            //            } else {
            //                // the way it used to be before the hack
            left.accept(this);
            // print the dims of the array
            printDecl(left.getType(), ident);
            //            }
            return;
        }

        // do not initialize class variables

        if (right instanceof JUnqualifiedInstanceCreation)
            return;

        lastLeft = left;
        printLParen();
        left.accept(this);
        p.print(" = ");
        right.accept(this);
        printRParen();

        p.print("/*" + left.getType() + "*/");
    }

    /*
     * prints an array length expression visitArrayLengthExpression (ToC)
     */

    /*
     * prints an array access expression visitArrayAccessExpression (ToC)
     */

    // ----------------------------------------------------------------------
    // STREAMIT IR HANDLERS
    // ----------------------------------------------------------------------
    /*
     * visitCreatePortalExpression, visitInitStatement, visitInterfaceTable,
     * visitLatency, visitLatencyMax, visitLatencyRange, visitLatencySet
     * in ToC
     */

    public void visitMessageStatement(SIRMessageStatement self,
                                      JExpression portal, String iname, String __ident,
                                      JExpression[] params, SIRLatency latency) {
        /*
          p.print("//send_" + iname + "_" + ident + "(");
          portal.accept(this);
          p.print(", ");
          latency.accept(this);
          if (params != null)
          for (int i = 0; i < params.length; i++)
          if (params[i] != null)
          {
          p.print(", ");
          params[i].accept(this);
          }
          p.print(");");
        */

        //String ident;
        int num_params = 0;

        if (portal instanceof SIRPortal) {
            SIRStream receivers[] = ((SIRPortal) portal).getReceivers();
            for (int i = 0; i < receivers.length; i++) {
                int dst = NodeEnumerator.getSIROperatorId(receivers[i]);

                if (ClusterBackend.debugging) {
                    System.out.println("/* iname: " + iname + " ident: "
                                       + self.getMessageName() + " type: "
                                       + ((SIRPortal) portal).getPortalType().getCClass()
                                       + "*/\n");
                    
                    System.out.println("params size: " + params.length);
                }

                p.print("/*");

                //                for (int y = 0;; y++) {
                //                    // this causes only msg param to be output
                //                    PRINT_MSG_PARAM = y;
                //                    params[2].accept(this);
                //                    if (PRINT_MSG_PARAM == -1)
                //                        break; // no more params!
                //                    num_params++;
                //                    p.print(",");
                //                }
                for (int j = 0; j < params.length; j++) {
                    params[j].accept(this);
                    num_params++;
                    if (j < params.length - 1) { p.print(",");}
                }
                
                p.print(" num_params: " + num_params + "*/\n");

                CClass pclass = ((SIRPortal) portal).getPortalType()
                    .getCClass();
                CMethod methods[] = pclass.getMethods();

                int index = -1;

                int size = 12; // size:4 mindex:4 exec_at:4

                // determine how large parameters are
                int param_size = 0;
                for (int j = 0; j < params.length; j++) {
                    param_size += params[j].getType().getSizeInC();
                }

                // int and float have size of 4 bytes
                size += param_size;

                for (int t = 0; t < methods.length; t++) {

                    if (ClusterBackend.debugging)
                        System.out.println("/* has method: " + methods[t] + " */\n");

                    if (methods[t].getIdent().equals(__ident)) {
                        index = t;
                        break;
                    }
                }

                /*
                  SIRStream dest_arr[] = ((SIRPortal)portal).getReceivers();

                  System.out.println("Handler name is: "+ident); 

                  String ident2 = ident+"__";

                  for (int t = 0; t < dest_arr.length; t++) {
                  JMethodDeclaration m[] = dest_arr[t].getMethods();
                  for (int z = 0; z < m.length; z++) {
                  String name = m[z].getName();
                  boolean guess = name.startsWith(ident2);
                  System.out.println("== "+name+" == "+(guess?"***":""));
                  if (guess) {
                  System.out.println("ModState: "+ModState.methodModsState(m[z]));
                  }
                  }
                  }

                  int index = -1;
                */

                CType method_params[] = methods[index].getParameters();

                /*
                  if (params != null) {
                  // int and float have size of 4 bytes
                  size += params.length * 4; 
                  }
                */

                p.println("\n{");
	      if (KjcOptions.standalone) {
		//p.println("#ifdef __CLUSTER_STANDALONE\n\n");

                p.print("  message* __msg = new message("+size+", "+index+", ");

                if (latency instanceof SIRLatencyMax) {

                    int max = ((SIRLatencyMax) latency).getMax();

                    SIRFilter sender = (SIRFilter) NodeEnumerator
                        .getOperator(selfID);
                    SIRFilter receiver = (SIRFilter) NodeEnumerator
                        .getOperator(dst);

                    if (LatencyConstraints.isMessageDirectionDownstream(sender,
                                                                        receiver)) {
                        p.print("sdep_" + selfID + "_" + dst
                                + "->getDstPhase4SrcPhase(__counter_" + selfID
                                + "+" + max + "+1)");
                        // subtract 1 in the downstream direction
                        // because message is delivered BEFORE given iter
                        p.print("-1);\n");
                    } else {
                        p.print("sdep_" + selfID + "_" + dst
                                + "->getSrcPhase4DstPhase(__counter_" + selfID
                                + "+" + max + "+1)");
                        // do not subtract 1 in the upstream direction
                        // because message is delivered AFTER given iter
                        p.print(");\n");
                    }

                } else {
                    p.print("-1);\n");
                }

                p.print("  __msg->alloc_params("+param_size+");\n");

                if (params != null) {
                    for (int t = 0; t < method_params.length; t++) {
                        msgPush(params[t],method_params[t]);
                    }
                }


                p.print("  __msg_stack_"+dst+" = __msg->push_on_stack(__msg_stack_"+dst+");\n");

	      } else { 
		//p.print("\n#else // __CLUSTER_STANDALONE\n\n");

                p.print("  __msg_sock_" + selfID + "_" + dst + "out->write_int("
                        + size + ");\n");

                p.print("  __msg_sock_" + selfID + "_" + dst + "out->write_int("
                        + index + ");\n");

                if (latency instanceof SIRLatencyMax) {

                    int max = ((SIRLatencyMax) latency).getMax();

                    //p.print("__msg_sock_"+selfID+"_"+dst+"out->write_int("+max+");");

                    SIRFilter sender = (SIRFilter) NodeEnumerator
                        .getOperator(selfID);
                    SIRFilter receiver = (SIRFilter) NodeEnumerator
                        .getOperator(dst);

                    if (LatencyConstraints.isMessageDirectionDownstream(sender,
                                                                        receiver)) {
                        p.print("  __msg_sock_" + selfID + "_" + dst
                                + "out->write_int(sdep_" + selfID + "_" + dst
                                + "->getDstPhase4SrcPhase(__counter_" + selfID
                                + "+" + max + "+1)");
                        // subtract 1 in the downstream direction
                        // because message is delivered BEFORE given iter
                        p.print("-1);\n");
                    } else {
                        p.print("  __msg_sock_" + selfID + "_" + dst
                                + "out->write_int(sdep_" + selfID + "_" + dst
                                + "->getSrcPhase4DstPhase(__counter_" + selfID
                                + "+" + max + "+1)");
                        // do not subtract 1 in the upstream direction
                        // because message is delivered AFTER given iter
                        p.print(");\n");
                    }

                } else {
                    p.print("  __msg_sock_" + selfID + "_" + dst
                            + "out->write_int(-1);\n");
                }

                assert params.length == method_params.length :
                    "Message send parameter list length " + params.length 
                    + " != formals list length " + method_params.length;
                ;
                
                if (params != null) {
                    for (int t = 0; t < method_params.length; t++) {

                        if (method_params[t].isArrayType()) {
                            // write base pointer and length
                            CArrayType arrType = (CArrayType)method_params[t];
                            p.print("  __msg_sock_" + selfID + "_" + dst
                                    + "out->write_" + arrType.getBaseType() + "_array((" + arrType.getBaseType() + "*)");
                            params[t].accept(this);
                            p.print(", " + ((CArrayType)method_params[t]).getTotalNumElements() + ");\n");
                        } else {
                            // write plain value of parameter
                            p.print("  __msg_sock_" + selfID + "_" + dst
                                    + "out->write_" + method_params[t] + "(");
                            params[t].accept(this);
                            p.print(");\n");
                        }
                    }
                }
	      }
              //  p.print("\n#endif // __CLUSTER_STANDALONE\n");
	      p.println("}");

            }
        }
    }

    public void visitDynamicToken(SIRDynamicToken self) {
        Utils.fail("Dynamic rates not yet supported in cluster backend.");
    }

    public void visitRangeExpression(SIRRangeExpression self) {
        Utils.fail("Dynamic rates not yet supported in cluster backend.");
    }

    public void visitPeekExpression(SIRPeekExpression self, CType tapeType,
                                    JExpression num) {

        //Tape in = RegisterStreams.getFilterInStream(filter);
        //print(in.consumer_name()+".peek(");

//        if (mod_push_pop) {
//            p.print("(*(____in+");
//            num.accept(this);
//            p.print("))");
//            return;
//        }

        p.print(RegisterStreams.getFilterInStream(filter).getPeekName());
        p.print("(");
        num.accept(this);
        p.print(")");

        //Utils.fail("FlatIRToCluster should see no peek expressions");
    }

    public void visitPopExpression(SIRPopExpression self, CType tapeType) {

        //Tape in = RegisterStreams.getFilterInStream(filter);
        if (self.getNumPop() == 1)  {
//            if (mod_push_pop) {
//                p.print("(*____in++)");
//                return;
//            }
            p.print(RegisterStreams.getFilterInStream(filter).getPopName());
            p.print("()");
        } else {
//            if (mod_push_pop) {
//                p.print("in += " + self.getNumPop() + ";\n");
//                return;
//            }
            p.print(RegisterStreams.getFilterInStream(filter).getPopName());
            p.print("(" + self.getNumPop() + ")");
        }
    }

    //    public void visitPrintStatement: in superclass ToCCommon

    private void pushScalar(SIRPushExpression self, CType tapeType,
                            JExpression val) {
//        if (mod_push_pop) {
//            p.print("((*____out++)=");
//            val.accept(this);
//            p.print(")");
//            return;
//        }

        p.print(RegisterStreams.getFilterOutStream(filter).getPushName());
        p.print("(");
        val.accept(this);
        p.print(")");

    }

    public void pushClass(SIRPushExpression self, CType tapeType,
                          JExpression val) {

        p.print(RegisterStreams.getFilterOutStream(filter).getPushName());
        p.print("(");
        val.accept(this);
        p.print(")");
    }
    
    private void pushArray(SIRPushExpression self, CType tapeType,
                           JExpression val) {
        // following commented-out code never worked.
        // push as if scalar and let the TapeXXXX.java functions
        // handle generation of push for arrays.
        // TODO need to have tapes do code generation here
        Tape outputTape = RegisterStreams.getFilterOutStream(filter);
        if (outputTape instanceof TapeDynrate) {
            p.print(outputTape.getPushName());
            p.print("(reinterpret_cast<" + ((TapeDynrate)(outputTape)).getTypeCastName() + ">(&");
            val.accept(this);
            p.print("))");
        } else {
            p.print(outputTape.getPushName());
            p.print("(");
            val.accept(this);
            p.print(")");
        }
    }

    public void visitPushExpression(SIRPushExpression self, CType tapeType,
                                    JExpression val) {
        if (tapeType.isArrayType())
            pushArray(self, tapeType, val);
        else if (tapeType.isClassType())
            pushClass(self, tapeType, val);
        else
            pushScalar(self, tapeType, val);
    }

    public void visitRegReceiverStatement(SIRRegReceiverStatement self,
                                          JExpression portal, SIRStream receiver, JMethodDeclaration[] methods) {
        p.print("register_receiver(");
        portal.accept(this);
        p.print(", data->context, ");
        p.print(self.getItable().getVarDecl().getIdent());
        p.print(", LATENCY_BEST_EFFORT);");
        // (But shouldn't there be a latency field in here?)
    }

    public void visitRegSenderStatement(SIRRegSenderStatement self, String fn,
                                        SIRLatency latency) {
        p.print("register_sender(this->context, ");
        p.print(fn);
        p.print(", ");
        latency.accept(this);
        p.print(");");
    }

    // ----------------------------------------------------------------------
    // UTILS
    // ----------------------------------------------------------------------

    /**
     * prints an array length expression
     */
    public void visitFormalParameters(JFormalParameter self, boolean isFinal,
                                      CType type, String ident) {
        String type_string = type.toString();
        if (type_string.equals("java.lang.String"))
            p.print("char*");
        else if (ident.indexOf("$") == -1) {
            printDecl(type, ident);
        } else {
            // does this case actually happen?  just preseving
            // semantics of previous version, but this doesn't make
            // sense to me.  --bft
            printType(type);
        }
    }

    // ----------------------------------------------------------------------
    // PROTECTED METHODS
    // ----------------------------------------------------------------------
    // ----------------------------------------------------------------------
    // UNUSED STREAM VISITORS
    // ----------------------------------------------------------------------

    /* pre-visit a pipeline */
    public void preVisitPipeline(SIRPipeline self, SIRPipelineIter iter) {
    }

    /* pre-visit a splitjoin */
    public void preVisitSplitJoin(SIRSplitJoin self, SIRSplitJoinIter iter) {
    }

    /* pre-visit a feedbackloop */
    public void preVisitFeedbackLoop(SIRFeedbackLoop self,
                                     SIRFeedbackLoopIter iter) {
    }

    /**
     * POST-VISITS 
     */

    /* post-visit a pipeline */
    public void postVisitPipeline(SIRPipeline self, SIRPipelineIter iter) {
    }

    /* post-visit a splitjoin */
    public void postVisitSplitJoin(SIRSplitJoin self, SIRSplitJoinIter iter) {
    }

    /* post-visit a feedbackloop */
    public void postVisitFeedbackLoop(SIRFeedbackLoop self,
                                      SIRFeedbackLoopIter iter) {
    }

}
