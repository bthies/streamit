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
import at.dms.kjc.common.RawUtil;

/**
 * This class dumps the tile code for each filter into a file based on the tile
 * number assigned.
 */
public class FlatIRToCluster extends InsertTimers implements
                                                      StreamVisitor, CodeGenerator {

    /**
     * Set to true to print comments on code generation.
     * 
     * Currently just used for 'for' statment.
     * 
     * should move definition to ToCCommon...
     */
    
    static public boolean printCodegenComments = false;

    //  override ToC with info that we generate 'bool' for 'boolean', not 'int'
    protected boolean hasBoolType = true;

    //private boolean DEBUG = false;

    //protected String className;

    //protected boolean nl = true;

    // true if generating code for global struct
    protected boolean global = false;

    // true if push and pop from provided buffers!!
    protected boolean mod_push_pop = false;

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

    // fields for all of the vars names we introduce in the c code
    private final String FLOAT_HEADER_WORD = "__FLOAT_HEADER_WORD__";

    private final String INT_HEADER_WORD = "__INT_HEADER_WORD__";

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

    public void setGlobal(boolean g) {
        global = g;
    }

    public static void generateCode(FlatNode node) {
        SIRFilter contentsAsFilter = (SIRFilter) node.contents;
        // make sure SIRPopExpression's only pop one element
        // code generation doesn't handle generating multiple pops
        // from a single SIRPopExpression
        //RemoveMultiPops.doit(contentsAsFilter);
        
        FlatIRToCluster toC = new FlatIRToCluster(contentsAsFilter);
        // the code below only deals with user-defined filters.
        // on a PredefinedFilter we need to go generate special code.
        if (node.contents instanceof SIRPredefinedFilter) {
            IterFactory.createFactory().createIter(contentsAsFilter)
                .accept(toC);
            return;
        }

        // FieldInitMover.moveStreamInitialAssignments(contentsAsFilter;
        // FieldProp.doPropagate(contentsAsFilter);

        // Optimizations
        if (ClusterBackend.debugPrint)
            System.out.println("Optimizing "
                               + ((SIRFilter) node.contents).getName() + "...");

        Set destroyed_vars = new HashSet();

        // ArrayDestroyer arrayDest=new ArrayDestroyer();
        for (int i = 0; i < contentsAsFilter.getMethods().length; i++) {

            // do not optimize init work function
            if (node.contents instanceof SIRTwoStageFilter) {
                JMethodDeclaration init_work = ((SIRTwoStageFilter) node.contents)
                    .getInitWork();
                if (contentsAsFilter.getMethods()[i].equals(init_work)) {
                    continue;
                }
            }

            Unroller unroller;
            do {
                do {
                    // System.out.println("Unrolling..");
                    unroller = new Unroller(new Hashtable());
                    (contentsAsFilter).getMethods()[i].accept(unroller);
                } while (unroller.hasUnrolled());
                // System.out.println("Constant Propagating..");
                (contentsAsFilter).getMethods()[i].accept(new Propagator(
                                                                         new Hashtable()));
                // System.out.println("Unrolling..");
                unroller = new Unroller(new Hashtable());
                (contentsAsFilter).getMethods()[i].accept(unroller);
            } while (unroller.hasUnrolled());
            // System.out.println("Flattening..");
            (contentsAsFilter).getMethods()[i].accept(new BlockFlattener());
            // System.out.println("Analyzing Branches..");
            // (contentsAsFilter).getMethods()[i].accept(new
            // BranchAnalyzer());
            // System.out.println("Constant Propagating..");
            (contentsAsFilter).getMethods()[i].accept(new Propagator(
                                                                     new Hashtable()));

            if (KjcOptions.destroyfieldarray) {
                ArrayDestroyer arrayDest = new ArrayDestroyer();
                (contentsAsFilter).getMethods()[i].accept(arrayDest);
                arrayDest.addDestroyedLocals(destroyed_vars);
            }

            (contentsAsFilter).getMethods()[i].accept(new VarDeclRaiser());
        }

        // if(KjcOptions.destroyfieldarray) {
        // arrayDest.destroyFieldArrays(contentsAsFilter);
        // }

        DeadCodeElimination.doit(contentsAsFilter);

        IterFactory.createFactory().createIter(contentsAsFilter).accept(toC);
    }

    public FlatIRToCluster() {
        super();
    }

    public FlatIRToCluster(CodegenPrintWriter p) {
        super(p);
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

        HashSet sendsCreditsTo = LatencyConstraints
            .getOutgoingConstraints(self);
        boolean restrictedExecution = LatencyConstraints.isRestricted(self);
        boolean sendsCredits = (sendsCreditsTo.size() > 0);

        SIRPortal outgoing[] = SIRPortal.getPortalsWithSender(self);
        SIRPortal incoming[] = SIRPortal.getPortalsWithReceiver(self);

        Vector<SIRStream> sends_to = new Vector<SIRStream>();
        Vector<SIRStream> receives_from = new Vector<SIRStream>();

        FlatNode node = NodeEnumerator.getFlatNode(selfID);

        if (ClusterBackend.debugPrint)
            System.out.println("flat node: " + node);

        Integer init_int = (Integer) ClusterBackend.initExecutionCounts
            .get(node);
        int init_counts;

        if (init_int == null) {
            init_counts = 0;
        } else {
            init_counts = init_int.intValue();
        }

        int steady_counts = ((Integer) ClusterBackend.steadyExecutionCounts
                             .get(node)).intValue();

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

        int pop_n, peek_n, push_n;

        pop_n = self.getPopInt();
        peek_n = self.getPeekInt();
        push_n = self.getPushInt();

        p.print("// peek: " + peek_n + " pop: " + pop_n + " push " + push_n
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

        if (ClusterBackend.debugPrint)
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
        p.print("void check_messages__" + selfID + "();\n");
        p.print("void handle_message__" + selfID + "(netsocket *sock);\n");
        p.print("void send_credits__" + selfID + "();\n");

        p.newLine();

        // +=============================+
        // | Push / Pop |
        // +=============================+

        CType input_type = self.getInputType();
        CType output_type = self.getOutputType();

        NetStream in = RegisterStreams.getFilterInStream(self);
        NetStream out = RegisterStreams.getFilterOutStream(self);

        int out_pop_buffer_size = 10240;
        int out_pop_num_iters = 0;
        if (push_n != 0) {
            out_pop_num_iters = out_pop_buffer_size / push_n;
        }

        if (out != null) {
            // p.print(ClusterUtils.CTypeToString(output_type)
            // +" "+out.pop_buffer()+"["+push_n+"];\n");
            // p.print("int "+out.pop_index()+" = "+push_n+";\n");

            p.print(ClusterUtils.CTypeToString(output_type) + " "
                    + out.pop_buffer() + "[" + out_pop_buffer_size + "];\n");
            p.print("int " + out.pop_index() + " = "
                    + (out_pop_num_iters * push_n) + ";\n");

            p.newLine();
        }

        if (in != null) {

            FlatNode source_node = NodeEnumerator.getFlatNode(in.getSource());
            FlatNode my_node = NodeEnumerator.getFlatNode(selfID);

            // String pop_expr = null;

            FlatNode source_master = ClusterFusion.getLocalMaster(source_node);
            boolean source_fused = (source_master != null && source_master
                                    .equals(my_node));

            // if {
            // pop_expr = in.pop_name();
            // } else {
            // pop_expr = in.consumer_name()+".pop";
            // }

            int peek_buf_size = 1;

            while (peek_buf_size < peek_n) {
                peek_buf_size *= 2;
            }

            p.print(ClusterUtils.CTypeToString(input_type) + " __pop_buf__"
                    + selfID + "[" + peek_buf_size + "];\n");
            p.print("int __head__" + selfID + ";\n");
            p.print("int __tail__" + selfID + ";\n");
            p.newLine();

            int extra = peek_n - pop_n;

            // assert (extra >= 0);
            if (extra < 0)
                extra = 0;

            p.print("inline void __init_pop_buf__" + selfID + "() {\n");

            int init_pop_count = extra;

            if (self instanceof SIRTwoStageFilter) {

                SIRTwoStageFilter ff = (SIRTwoStageFilter)self;

                int init_peek = ff.getInitPeekInt();
                int init_pop = ff.getInitPopInt();
                if (init_pop > init_peek) init_peek = init_pop;

                init_pop_count = init_peek;

                if (extra > (init_peek - init_pop))
                    init_pop_count += extra - (init_peek - init_pop);
            }

            if (init_pop_count > 0) {
                //p.println("// FlatIRToCluster_1");
                if (init_pop_count > 1) {
                    p.print("  for (int i=0; i<" + init_pop_count + "; i++) {\n");
                    p.print("    __pop_buf__" + selfID + "[i]=");
                } else {
                    p.print("    __pop_buf__" + selfID + "[0]=");
                }
                if (source_fused) {
                    p.print(in.pop_name() + "();\n");
                } else {
                    p.print(in.consumer_name() + ".pop();\n");
                }
                if (init_pop_count > 1) {
                    p.print("  }\n");
                }
            }
            p.print("  __tail__" + selfID + "=0;\n");
            p.print("  __head__" + selfID + "=" + init_pop_count + ";\n");

            /*
             * 
             * if (source_fused) {
             * 
             * for (int y = 0; y < extra; y++) { int index = y + pop_n;
             * 
             * p.print(" __pop_buf__"+selfID+"["+index+"] =
             * "+in.pop_name()+"();\n"); } } else {
             * 
             * if (pop_n < 8) {
             * 
             * for (int i = 0; i < extra; i++) { p.print("
             * __pop_buf__"+selfID+"["+(pop_n+i)+"] =
             * "+in.consumer_name()+".pop();\n"); }
             *  } else {
             * 
             * 
             * p.print("
             * "+in.consumer_name()+".pop_items(&__pop_buf__"+selfID+"["+pop_n+"],
             * "+extra+");\n"); }
             *  }
             */

            p.print("}\n");
            p.newLine();

            p.print("void save_peek_buffer__" + selfID
                    + "(object_write_buffer *buf) {\n");
            p.print("  int i = 0, offs = __tail__" + selfID + ";\n");
            p.print("  while (offs != __head__" + selfID + ") {\n");
            p.print("    buf->write(&__pop_buf__" + selfID + "[offs], sizeof("
                    + ClusterUtils.CTypeToString(input_type)
                    + "));\n    offs++;\n    offs&=" + (peek_buf_size - 1)
                    + ";\n    i++;\n");
            p.print("  }\n");
            // p.print(" fprintf(stderr,\"buf size: %d\\n\", i);\n");
            p.print("  assert(i == " + extra + ");\n");
            p.print("}\n");
            p.newLine();

            p.print("void load_peek_buffer__" + selfID
                    + "(object_write_buffer *buf) {\n");
            if (extra > 0) {
                if (extra > 1) {
                    //p.println("// FlatIRToCluster_2");
                    p.print("  for (int i = 0; i < " + extra + "; i++) {\n");
                    p.print("     buf->read(&__pop_buf__" + selfID + "[i] , sizeof("
                            + ClusterUtils.CTypeToString(input_type) + "));\n");
                    p.print("  }\n");
                } else {
                    p.print("     buf->read(&__pop_buf__" + selfID + "[0] , sizeof("
                            + ClusterUtils.CTypeToString(input_type) + "));\n");
                }
            }
            p.print("  __tail__" + selfID + "=0;\n");
            p.print("  __head__" + selfID + "=" + extra + ";\n");
            p.print("}\n");
            p.newLine();


            p.print("inline void __update_pop_buf__" + selfID + "() {\n");

            if (peek_n <= pop_n) {

                // no peek beyond pop

                // TODO: candidate for partial unrolling?
                if (pop_n > 0) {
                    //p.println("// FlatIRToCluster_3");
                    if (pop_n > 1) {
                        p.println("for (int i = 0; i < " + pop_n + "; i++) {");
                        p.indent();
                        p.print("  __pop_buf__" + selfID + "[i]=");
                    } else {
                        p.print("  __pop_buf__" + selfID + "[0]=");
                    }
                    if (source_fused) {
                        p.println(in.pop_name() + "();");
                    } else {
                        p.println(in.consumer_name() + ".pop();");
                    }
                    if (pop_n > 1) {
                        p.outdent();
                        p.println("}");
                    }
                }
                
                /* The following compile-time unrolling caused gcc to thrash
                 * on very large buffer sizes.  replaced with run-time loop.

                 for (int i = 0; i < pop_n; i++) {
                 p.print("  __pop_buf__" + selfID + "[" + i + "]=");

                 if (source_fused) {
                 p.print(in.pop_name() + "();");
                 } else {
                 p.print(in.consumer_name() + ".pop();");
                 }
                 p.newLine();
                 }
                */
                p.print("  __tail__" + selfID + " = 0;\n");
                p.print("  __head__" + selfID + " = " + pop_n + ";\n");

            } else {

                // peek beyond pop => circular buffer

                // TODO: candidate for partial unrolling?
                if (pop_n > 0) {
                    //p.println("// FlatIRToCluster_4");
                    if (pop_n > 1) {
                        p.println("for (int i = 0; i < " + pop_n + "; i++) {");
                        p.indent();
                    }
                    p.print("  __pop_buf__" + selfID + "[__head__" + selfID
                            + "]=");
                    if (source_fused) {
                        p.println(in.pop_name() + "();");
                    } else {
                        p.println(in.consumer_name() + ".pop();");
                    }
                    p.print("__head__" + selfID + "++;");
                    p.print("__head__" + selfID + "&=" + (peek_buf_size - 1)
                            + ";\n");
                    if (pop_n > 1) {
                        p.outdent();
                        p.println("}");
                    }
                }
                
                /* The following compile-time unrolling caused gcc to thrash
                 * on very large buffer sizes.  replaced with run-time loop.

                 for (int i = 0; i < pop_n; i++) {

                 p.print("  __pop_buf__" + selfID + "[__head__" + selfID
                 + "]=");

                 if (source_fused) {
                 p.print(in.pop_name() + "();");
                 } else {
                 p.print(in.consumer_name() + ".pop();");
                 }

                 p.print("__head__" + selfID + "++;");
                 p.print("__head__" + selfID + "&=" + (peek_buf_size - 1)
                 + ";\n");
                 }
                */
            }

            /*
             * p.print(" __pop_index__"+selfID+" = 0;\n");
             * 
             * int extra = peek_n - pop_n; for (int y = 0; y < extra; y++) { int
             * index = y + pop_n; p.print(" __pop_buf__"+selfID+"["+y+"] =
             * __pop_buf__"+selfID+"["+index+"];\n"); }
             * 
             * if (source_fused) {
             * 
             * for (int y = 0; y < pop_n; y++) { int index = y + extra;
             * p.print(" __pop_buf__"+selfID+"["+index+"] =
             * "+in.pop_name()+"();\n"); } } else {
             * 
             * if (pop_n < 8) {
             * 
             * for (int i = 0; i < pop_n; i++) { p.print("
             * __pop_buf__"+selfID+"["+(extra+i)+"] =
             * "+in.consumer_name()+".pop();\n"); }
             *  } else {
             * 
             * p.print("
             * "+in.consumer_name()+".pop_items(&__pop_buf__"+selfID+"["+extra+"],
             * "+pop_n+");\n"); }
             *  }
             */

            p.print("}\n");
            p.newLine();

            int s = in.getSource();
            int d = in.getDest();

            p.print("#ifdef __FUSED_" + s + "_" + d + "\n");

            // the filter is fused with its source

            p.newLine();

            p.print("  extern " + ClusterUtils.CTypeToString(input_type)
                    + " BUFFER_" + s + "_" + d + "[];\n");

            p.print("  extern volatile int HEAD_" + s + "_" + d + ";\n");
            p.print("  extern volatile int TAIL_" + s + "_" + d + ";\n");

            // p.print(" extern int HEAD_"+s+"_"+d+";\n");
            // p.print(" extern int TAIL_"+s+"_"+d+";\n");

            p.newLine();

            // pop from fusion buffer

            p.print("  inline " + ClusterUtils.CTypeToString(input_type)
                    + " __pop__" + selfID + "() {\n");

            p.print("    " + ClusterUtils.CTypeToString(input_type)
                    + " res=BUFFER_" + s + "_" + d + "[TAIL_" + s + "_" + d
                    + "];\n");
            p.print("    TAIL_" + s + "_" + d + "++;\n");
            p.print("    #ifndef __NOMOD_" + s + "_" + d + "\n");
            p.print("    TAIL_" + s + "_" + d + "&=__BUF_SIZE_MASK_" + s + "_"
                    + d + ";\n");
            p.print("    #endif\n");
            p.print("    return res;\n");

            p.print("  }\n");
            p.newLine();

            // pop from fusion buffer with argument

            p.print("  inline " + ClusterUtils.CTypeToString(input_type)
                    + " __pop__" + selfID + "(int n) {\n");

            p.print("    " + ClusterUtils.CTypeToString(input_type)
                    + " res=BUFFER_" + s + "_" + d + "[TAIL_" + s + "_" + d
                    + "];\n");
            p.print("    TAIL_" + s + "_" + d + "+=n;\n");
            p.print("    #ifndef __NOMOD_" + s + "_" + d + "\n");
            p.print("    TAIL_" + s + "_" + d + "&=__BUF_SIZE_MASK_" + s + "_"
                    + d + ";\n");
            p.print("    #endif\n");
            p.print("    return res;\n");

            p.print("  }\n");
            p.newLine();

            // peek from fusion buffer

            p.print("  inline " + ClusterUtils.CTypeToString(input_type) + " "
                    + ClusterUtils.peekName(selfID) + "(int offs) {\n");
            p.print("    #ifdef __NOMOD_" + s + "_" + d + "\n");
            p.print("    return BUFFER_" + s + "_" + d + "[TAIL_" + s + "_" + d
                    + "+offs];\n");
            p.print("    #else\n");
            p.print("    return BUFFER_" + s + "_" + d + "[(TAIL_" + s + "_"
                    + d + "+offs)&__BUF_SIZE_MASK_" + s + "_" + d + "];\n");
            p.print("    #endif\n");

            p.print("  }\n");
            p.newLine();

            // pop (the source is not fused)

            p.print("#else //!__FUSED_" + s + "_" + d + "\n");
            p.newLine();

            p.print("  inline " + ClusterUtils.CTypeToString(input_type)
                    + " __pop__" + selfID + "() {\n");

            if (peek_n <= pop_n) {
                p.print("    return __pop_buf__" + selfID + "[__tail__"
                        + selfID + "++];\n");
            } else {
                p.print("    " + ClusterUtils.CTypeToString(input_type)
                        + " res=__pop_buf__" + selfID + "[__tail__" + selfID
                        + "];");
                p.print("__tail__" + selfID + "++;");
                p.print("__tail__" + selfID + "&=" + (peek_buf_size - 1)
                        + ";\n");
                p.print("    return res;\n");
            }

            // p.print(" return
            // __pop_buf__"+selfID+"[__pop_index__"+selfID+"++];\n");
            p.print("  }\n");
            p.newLine();

            // pop with argument (the source is not fused)

            p.print("  inline " + ClusterUtils.CTypeToString(input_type)
                    + " __pop__" + selfID + "(int n) {\n");

            if (peek_n <= pop_n) {
                p.print("    " + ClusterUtils.CTypeToString(input_type) + " result = __pop_buf__" + selfID + "[__tail__"
                        + selfID + "];\n");
                p.print("    __tail__" + selfID + "+=n;\n");
                p.print("    return result;\n");
            } else {
                p.print("    " + ClusterUtils.CTypeToString(input_type)
                        + " res=__pop_buf__" + selfID + "[__tail__" + selfID
                        + "];");
                p.print("__tail__" + selfID + "+=n;");
                p.print("__tail__" + selfID + "&=" + (peek_buf_size - 1)
                        + ";\n");
                p.print("    return res;\n");
            }

            // p.print(" return
            // __pop_buf__"+selfID+"[__pop_index__"+selfID+"++];\n");
            p.print("  }\n");
            p.newLine();

            p.print("  inline " + ClusterUtils.CTypeToString(input_type) + " "
                    + ClusterUtils.peekName(selfID) + "(int offs) {\n");

            if (peek_n <= pop_n) {
                p.print("    return __pop_buf__" + selfID + "[(__tail__"
                        + selfID + "+offs)];\n");
            } else {
                p.print("    return __pop_buf__" + selfID + "[(__tail__"
                        + selfID + "+offs)&" + (peek_buf_size - 1) + "];\n");
            }

            // p.print(" return __pop_buf__"+selfID
            // +"[__pop_index__"+selfID+" + offs];\n");
            p.print("  }\n");
            p.newLine();

            p.print("#endif\n");
            p.newLine();

        } else {

            p.print("inline " + ClusterUtils.CTypeToString(input_type)
                    + " __init_pop_buf__" + selfID + "() {}\n");
            p.print("inline " + ClusterUtils.CTypeToString(input_type)
                    + " __update_pop_buf__" + selfID + "() {}\n");

            p.print("void save_peek_buffer__" + selfID
                    + "(object_write_buffer *buf) {}\n");
            p.print("void load_peek_buffer__" + selfID
                    + "(object_write_buffer *buf) {}\n");

            p.newLine();
        }

        if (out != null) {

            FlatNode dst_node = NodeEnumerator.getFlatNode(out.getDest());
            FlatNode my_node = NodeEnumerator.getFlatNode(selfID);

            // String push_expr = null;

            FlatNode dst_master = ClusterFusion.getLocalMaster(dst_node);
            FlatNode my_master = ClusterFusion.getLocalMaster(my_node);

            int s = out.getSource();
            int d = out.getDest();

            // check if the destination node is fused

            p.print("#ifdef __FUSED_" + s + "_" + d + "\n");

            p.newLine();
            p.print("  extern " + ClusterUtils.CTypeToString(output_type)
                    + " BUFFER_" + s + "_" + d + "[];\n");
            p.print("  extern int HEAD_" + s + "_" + d + ";\n");
            p.print("  extern int TAIL_" + s + "_" + d + ";\n");
            p.newLine();

            p.print("  inline void " + ClusterUtils.pushName(selfID) + "("
                    + ClusterUtils.CTypeToString(output_type) + " data) {\n");

            p.print("    BUFFER_" + s + "_" + d + "[HEAD_" + s + "_" + d
                    + "]=data;\n");
            p.print("    HEAD_" + s + "_" + d + "++;\n");
            p.print("    #ifndef __NOMOD_" + s + "_" + d + "\n");
            p.print("    HEAD_" + s + "_" + d + "&=__BUF_SIZE_MASK_" + s + "_"
                    + d + ";\n");
            p.print("    #endif\n");

            p.print("  }\n");
            p.newLine();

            // if not fused use the producer's push function

            p.print("#else //!__FUSED_" + s + "_" + d + "\n");
            p.newLine();

            p.print("  inline void " + ClusterUtils.pushName(selfID) + "("
                    + ClusterUtils.CTypeToString(output_type) + " data) {\n");

            if (dst_master != null && dst_master.equals(my_node)) {
                p.print("  " + out.push_name() + "(data);\n");
            } else if (my_master != null && my_master.equals(dst_node)) {
                p.print("  " + out.pop_buffer() + "[" + out.pop_index()
                        + "++] = data;\n");
            } else {
                p.print("  " + out.producer_name() + ".push(data);\n");
            }

            p.print("  }\n");

            p.newLine();
            p.print("#endif");
            p.newLine();

        }

        if (in != null) {
            p.print("void " + in.push_name() + "("
                    + ClusterUtils.CTypeToString(input_type) + " data) {}\n");
            p.newLine();
        }

        p.newLine();

        if (out != null) {

            // p.print("void
            // "+ClusterExecutionCode.rawMain+"__"+selfID+"();\n");
            p.print("void " + ClusterUtils.getWorkName(self, selfID)
                    + "(int);\n\n");

            p.print(ClusterUtils.CTypeToString(output_type) + " "
                    + out.pop_name() + "() {\n");
            p.print("  int _tmp;\n");

            /*
             * p.print(" if ("+out.pop_index()+" == "+push_n+") {\n"); p.print("
             * int tmp = __number_of_iterations_"+selfID+";\n"); p.print("
             * __number_of_iterations_"+selfID+" = 1;\n"); p.print("
             * "+out.pop_index()+" = 0;\n"); p.print("
             * "+ClusterExecutionCode.rawMain+"__"+selfID+"();\n"); p.print("
             * __number_of_iterations_"+selfID+" = tmp - 1;\n"); p.print("
             * "+out.pop_index()+" = 0;\n");
             */
            p.print("  if (" + out.pop_index() + " == "
                    + (out_pop_num_iters * push_n) + ") {\n");

            p.print("    " + out.pop_index() + " = 0;\n");

            if (out_pop_num_iters > 0) {
                //p.println("// FlatIRToCluster_5");
                if (out_pop_num_iters > 1) {
                    p.print("    for (_tmp = 0; _tmp < " + out_pop_num_iters
                            + "; _tmp++) {\n");
                }
                p.print("      //check_status__" + selfID + "();\n");
                p.print("      check_messages__" + selfID + "();\n");
                p.print("      __update_pop_buf__" + selfID + "();\n");
                p.print("      " + ClusterUtils.getWorkName(self, selfID)
                        + "(1);\n");
                p.print("      //send_credits__" + selfID + "();\n");
                if (out_pop_num_iters > 1) {
                    p.print("    }\n");
                }
            }
            
            p.print("    " + out.pop_index() + " = 0;\n");

            /*
             * p.print(" int tmp = __number_of_iterations_"+selfID+";\n");
             * p.print(" __number_of_iterations_"+selfID+" =
             * "+out_pop_num_iters+";\n"); p.print(" "+out.pop_index()+" =
             * 0;\n"); p.print("
             * "+ClusterExecutionCode.rawMain+"__"+selfID+"();\n"); p.print("
             * __number_of_iterations_"+selfID+" = tmp -
             * "+out_pop_num_iters+";\n"); p.print(" "+out.pop_index()+" =
             * 0;\n");
             */

            p.print("  }\n");

            p.print("  return " + out.pop_buffer() + "[" + out.pop_index()
                    + "++];\n");

            p.print("}\n");
            p.newLine();
        }

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
        
        p.println("\n\n#ifdef BUFFER_MERGE\n");
        
        if (!(self instanceof SIRFileReader) &&
            !(self instanceof SIRFileWriter) &&
            (in != null && out != null)) {
            
            p.println();
            p.println("void "+work.getName()+"__"+selfID+"__mod(int ____n, "+input_type+" *____in, "+output_type+" *____out) {");
            p.print("  for (; (0 < ____n); ____n--)\n");
            
            mod_push_pop = true;
            work.getBody().accept(this);
            mod_push_pop = false;
            
            p.println("}\n");
            
        }
        
        if (!(self instanceof SIRFileReader) &&
            !(self instanceof SIRFileWriter) &&
            (in != null && out != null)) {
            
            p.println();
            p.println("void "+work.getName()+"__"+selfID+"__mod2(int ____n, "+input_type+" *____in, "+output_type+" *____out, int s1, int s2) {");
            p.print("  for (; (0 < ____n); (____n--, ____in+=s1, ____out+=s2))\n");
            
            mod_push_pop = true;
            work.getBody().accept(this);
            mod_push_pop = false;
            
            p.println("}\n");
            
        }
        
        p.println("\n#endif // BUFFER_MERGE\n\n");
        
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

        if (self instanceof SIRPredefinedFilter) {
            BuiltinsCodeGen.predefinedFilterWork((SIRPredefinedFilter) self,
                                                 selfID, p);
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

        p.print("\nvoid check_messages__" + selfID + "() {\n");

        p.print("  message *msg, *last = NULL;\n");

        if (restrictedExecution) {
            p.print("  while (__credit_" + selfID + " <= __counter_" + selfID
                    + ") {\n");
        }

        if (! KjcOptions.standalone) { 
            p.print("#ifndef __CLUSTER_STANDALONE\n");

            {
                Iterator i = receives_from.iterator();
                while (i.hasNext()) {
                    int src = NodeEnumerator.getSIROperatorId((SIRStream) i.next());
                    p.print("  if (__msg_sock_" + src + "_" + selfID
                            + "in->data_available()) {\n    handle_message__"
                            + selfID + "(__msg_sock_" + src + "_" + selfID
                            + "in);\n  } // if\n");
                }
            }

            p.print("#endif\n");
        }
        if (restrictedExecution) {
            p.print("  } // while \n");
        }

        //p.println("// FlatIRToCluster_6");
        p.print("  for (msg = __msg_stack_" + selfID
                + "; msg != NULL; msg = msg->next) {\n");
        p.print("    if (msg->execute_at <= __counter_" + selfID + ") {\n");

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

                            for (int a = 0; a < param_count; a++) {
                                if (portal_method_params[a].toString().equals(
                                                                              "int")) {
                                    p.print("        int p" + a
                                            + " = msg->get_int_param();\n");
                                }
                                if (portal_method_params[a].toString().equals(
                                                                              "float")) {
                                    p.print("        float p" + a
                                            + " = msg->get_float_param();\n");
                                }
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

        p.print("    } else { last = msg; }\n");
        p.print("  } // for \n");

        p.print("}\n");

        // +=============================+
        // | Handle Message |
        // +=============================+

        p.print("\nvoid handle_message__" + selfID + "(netsocket *sock) {\n");
        p.print("  int size = sock->read_int();\n");

        if (restrictedExecution) {
            p.print("  if (size == -1) { // a credit message received\n");
            p.print("    __credit_" + selfID + " = sock->read_int();\n");
            p.print("    return;\n");
            p.print("  }\n");
        }

        p.print("  int index = sock->read_int();\n");
        p.print("  int iteration = sock->read_int();\n");
        p
            .print("  fprintf(stderr,\"Message receieved! thread: "
                   + selfID
                   + ", method_index: %d excute at iteration: %d\\n\", index, iteration);\n");

        p.print("  if (iteration > 0) {\n");
        p.print("    message *msg = new message(size, index, iteration);\n");
        p.print("    msg->read_params(sock);\n");
        p.print("    __msg_stack_" + selfID
                + " = msg->push_on_stack(__msg_stack_" + selfID + ");\n");
        p.print("    return;\n");
        p.print("  }\n");

        // SIRPortal[] portals = SIRPortal.getPortalsWithReceiver(self);

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

                    p.print("  if (index == " + i + ") {\n");

                    for (int t = 0; t < methods.length; t++) {

                        String thread_method_name = methods[t].getName();

                        if (thread_method_name.startsWith(method_name)
                            && thread_method_name.charAt(length) == '_'
                            && thread_method_name.charAt(length + 1) == '_') {

                            int param_count = methods[t].getParameters().length;

                            for (int a = 0; a < param_count; a++) {
                                if (portal_method_params[a].toString().equals(
                                                                              "int")) {
                                    p.print("    int p" + a
                                            + " = sock->read_int();\n");
                                }
                                if (portal_method_params[a].toString().equals(
                                                                              "float")) {
                                    p.print("    float p" + a
                                            + " = sock->read_float();\n");
                                }
                            }

                            p.print("    " + thread_method_name + "__" + selfID
                                    + "(");
                            for (int a = 0; a < param_count; a++) {
                                if (a > 0)
                                    p.print(", ");
                                p.print("p" + a);
                            }
                            p.print(");\n");
                        }
                    }
                    p.print("  }\n");
                }
            }
        }

        p.print("}\n");

        // +=============================+
        // | Send Credits |
        // +=============================+

        Iterator constrIter;
        constrIter = sendsCreditsTo.iterator();
        while (constrIter.hasNext()) {
            LatencyConstraint constraint = (LatencyConstraint) constrIter
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
            LatencyConstraint constraint = (LatencyConstraint) constrIter
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
        if (filter != null && filter instanceof SIRPredefinedFilter
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

        if (expr instanceof JArrayInitializer) {
            declareInitializedArray(type, ident, expr);
        } else {

            printDecl (type, ident);

            if (expr != null) {
                p.print ("\t= ");
                expr.accept (this);
            } 
            else if (type.isOrdinal())
                p.print(" = 0");
            else if (type.isFloatingPoint())
                p.print(" = 0.0f");
            else if (type.isArrayType())
                p.print(" = {0}");

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

        boolean oldStatementContext = statementContext;
        p.print("for (");
        statementContext = false; // expressions here separated by ';'
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
        statementContext = true;
        p.indent();
        body.accept(this);
        p.outdent();
        p.newLine();
        p.print("}");
        statementContext = oldStatementContext;
    }

    /*
     * prints a compound statement (2-argument form)
     */

    /**
     * prints a compound statement
     */
    public void visitCompoundStatement(JStatement[] body) {
        boolean oldStatementContext = statementContext;
        statementContext = true;
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
        statementContext = oldStatementContext;
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

        //we want single precision versions of the math functions
        boolean wrapping = false;
        if (Utils.isMathMethod(prefix, ident)) {
            // also insert profiling code for math functions
            wrapping = true;
            super.beginWrapper("FUNC_" + ident.toUpperCase());
            // RMR { math functions are converted to use their floating-point counterparts;
            // to do this, some function names are prepended with a 'f', and others have an
            // 'f' appended to them
            if (Utils.mathMethodRequiresFloatPrefix(prefix, ident)) {
                p.print("f");
                p.print(ident);
            } else { // by fault emit a float prefix on math functions
                p.print(ident);
                p.print("f");
            }
            // } RMR
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
            boolean oldStatementContext = statementContext;
            lastLeft = left;
            printLParen();
            statementContext = false;
            left.accept(this);
            p.print(" = ");
            right.accept(this);
            statementContext = oldStatementContext;
            printRParen();
            return;
        }

        // copy arrays element-wise
        boolean arrayType = ((left.getType()!=null && left.getType().isArrayType()) ||
                             (right.getType()!=null && right.getType().isArrayType()));
        if (arrayType && !(right instanceof JNewArrayExpression)) {

            CArrayType type = (CArrayType)right.getType();
            String dims[] = this.makeArrayStrings(type.getDims());

            // dims should never be null now that we have static array
            // bounds
            assert dims != null;
            /*
            // if we cannot find the dim, just create a pointer copy
            if (dims == null) {
            boolean oldStatementContext = statementContext;
            lastLeft = left;
            printLParen();
            statementContext = false;
            left.accept(this);
            p.print(" = ");
            right.accept(this);
            statementContext = oldStatementContext;
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

        boolean oldStatementContext = statementContext;
        lastLeft = left;
        printLParen();
        statementContext = false;
        left.accept(this);
        p.print(" = ");
        right.accept(this);
        statementContext = oldStatementContext;
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

                if (ClusterBackend.debugPrint) {
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

                // int and float have size of 4 bytes
                size += num_params * 4;

                for (int t = 0; t < methods.length; t++) {

                    if (ClusterBackend.debugPrint)
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

                p.print("\n{\n#ifdef __CLUSTER_STANDALONE\n\n");

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
                                + "+" + max + "+1)-1);\n");

                    } else {

                        p.print("sdep_" + selfID + "_" + dst
                                + "->getSrcPhase4DstPhase(__counter_" + selfID
                                + "+" + max + "+1)-1);\n");

                    }

                } else {
                    p.print("-1);\n");
                }

                p.print("  __msg->alloc_params("+num_params*4+");\n");

                if (params != null) {
                    for (int t = 0; t < method_params.length; t++) {

                        String method_params_string = method_params[t].toString();

                        p.print("  __msg->push_" + method_params_string + "(");
                        params[t].accept(this);

                        //if (t < method_params.length - 1) { p.print(", "); }
                        
                        p.print(");\n");
                    }
                }


                p.print("  __msg_stack_"+dst+" = __msg->push_on_stack(__msg_stack_"+dst+");\n");

                p.print("\n#else\n\n");

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
                                + "+" + max + "+1)-1);\n");

                    } else {

                        p.print("  __msg_sock_" + selfID + "_" + dst
                                + "out->write_int(sdep_" + selfID + "_" + dst
                                + "->getSrcPhase4DstPhase(__counter_" + selfID
                                + "+" + max + "+1)-1);\n");

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

                        String method_params_string = method_params[t]
                            .toString();
                        p.print("  __msg_sock_" + selfID + "_" + dst
                                + "out->write_");
                        if (method_params_string == "int"
                            || method_params_string == "float") {
                            p.print(method_params_string + "(");
                        } else {
                            p.print("unsupported/* " + method_params_string
                                    + " */ (");
                        }
                        //          if (method_params[t].toString().equals("int")) {
                        //              p.print("__msg_sock_"+selfID+"_"+dst+"out->write_int(");
                        //          }
                        //          if (method_params[t].toString().equals("float")) {
                        //              p.print("__msg_sock_"+selfID+"_"+dst+"out->write_float(");
                        //          }

                        // print out the parameter!
                        //                        PRINT_MSG_PARAM = t;
                        //                        params[2].accept(this);
                        //                        PRINT_MSG_PARAM = -1;
                        params[t].accept(this);
                        if (t < method_params.length - 1) { p.print(", "); }
                        
                        p.print(");\n");
                    }
                }

                p.print("\n#endif\n}\n");

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

        //NetStream in = RegisterStreams.getFilterInStream(filter);
        //print(in.consumer_name()+".peek(");

        if (mod_push_pop) {
            p.print("(*(____in+");
            num.accept(this);
            p.print("))");
            return;
        }

        p.print(ClusterUtils.peekName(selfID) + "(");
        num.accept(this);
        p.print(")");

        //Utils.fail("FlatIRToCluster should see no peek expressions");
    }

    public void visitPopExpression(SIRPopExpression self, CType tapeType) {

        //NetStream in = RegisterStreams.getFilterInStream(filter);
        if (self.getNumPop() == 1)  {
            if (mod_push_pop) {
                p.print("(*____in++)");
                return;
            }
            p.print("__pop__" + selfID + "()");
        } else {
            if (mod_push_pop) {
                p.print("assert(false);");
            }
            p.print("__pop__" + selfID + "(" + self.getNumPop() + ")");
        }
    }

    //    public void visitPrintStatement: in superclass ToCCommon

    private void pushScalar(SIRPushExpression self, CType tapeType,
                            JExpression val) {
        if (mod_push_pop) {
            p.print("((*____out++)=");
            val.accept(this);
            p.print(")");
            return;
        }

        p.print(ClusterUtils.pushName(selfID) + "(");
        val.accept(this);
        p.print(")");

        //NetStream out = RegisterStreams.getFilterOutStream(filter);
        //p.print(out.producer_name()+".push(");

        //p.print(Util.staticNetworkSendPrefix(tapeType));
        //p.print(Util.staticNetworkSendSuffix());

    }

    public void pushClass(SIRPushExpression self, CType tapeType,
                          JExpression val) {

        p.print(ClusterUtils.pushName(selfID) + "(");
        val.accept(this);
        p.print(")");

        //turn the push statement into a call of
        //the structure's push method
        //p.print("push" + tapeType + "(&");
        //val.accept(this);
        //p.print(")");
    }

    private void pushArray(SIRPushExpression self, CType tapeType,
                           JExpression val) {
        CType baseType = ((CArrayType) tapeType).getBaseType();
        String dims[] = this.makeArrayStrings(((CArrayType)tapeType).getDims());

        for (int i = 0; i < dims.length; i++) {
            p.print("for (" + CommonConstants.ARRAY_INDEX + i + " = 0; "
                    + CommonConstants.ARRAY_INDEX + i + " < " + dims[i]
                    + " ; " + CommonConstants.ARRAY_INDEX + i + "++)\n");
        }

        // AD: never tested to my knowledge.  Also, decoupled is only defined for RAW backends
        //        if (KjcOptions.altcodegen || KjcOptions.decoupled) {
        //            p.print("{\n");
        //            // p.print(Util.CSTOVAR + " = ");
        //            val.accept(this);
        //            for (int i = 0; i < dims.length; i++) {
        //                p.print("[" + CommonConstants.ARRAY_INDEX + i + "]");
        //            }
        //            p.print(";\n}\n");
        //        } else {
        p.print("{");
        p.print("static_send((" + baseType + ") ");
        val.accept(this);
        for (int i = 0; i < dims.length; i++) {
            p.print("[" + CommonConstants.ARRAY_INDEX + i + "]");
        }
        p.print(");\n}\n");
        //        }
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
