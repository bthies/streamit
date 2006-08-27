package at.dms.kjc.cluster;

import at.dms.kjc.common.*;
import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.FlatVisitor;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.iterator.*;
import at.dms.util.Utils;
import java.util.List;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.HashSet;
import java.io.*;
import at.dms.compiler.*;
import at.dms.kjc.sir.lowering.*;
import java.util.*;

/**
 * This class contains code generation for speculative execution,
 * where individual threads are represented by c++ classes.
 * <br/>
 * Unfortunately, this experimental version is not full-featured.
 * 
 * @deprecated
 */
@Deprecated public class FlatIRToCluster2 extends at.dms.kjc.common.ToCCommon implements StreamVisitor, CodeGenerator
{
    private boolean DEBUG = false;

    protected int               TAB_SIZE = 2;
    protected int               WIDTH = 80;
    protected int               pos;
    protected String                      className;

    protected TabbedPrintWriter     p;
    protected StringWriter                str; 
    protected boolean           nl = true;

    // true if generating code for global struct
    protected boolean    global = false; 

    public String                  helper_package = null;
    public boolean                 declOnly = false;
    public SIRFilter               filter = null;
    private Set<String>            method_names = new HashSet<String>();

    protected JMethodDeclaration   method;

    // ALWAYS!!!!
    //true if we are using the second buffer management scheme 
    //circular buffers with anding
    public boolean debug = false;//true;
    public boolean isInit = false;
    /** &gt; 0 if in a for loop header during visit **/
    private int forLoopHeader = 0;
    
    //fields for all of the vars names we introduce in the c code
    private final String FLOAT_HEADER_WORD = "__FLOAT_HEADER_WORD__";
    private final String INT_HEADER_WORD = "__INT_HEADER_WORD__";


    private static int filterID = 0;
    
    //Needed to pass info from assignment to visitNewArray
    JExpression lastLeft;

    int PRINT_MSG_PARAM = -1;

    public CodegenPrintWriter getPrinter() { return null; }

    public void setGlobal(boolean g) {
        global = g;
    }

    public static void generateCode(FlatNode node) 
    {
        FlatIRToCluster2 toC = new FlatIRToCluster2((SIRFilter)node.contents);
        //FieldInitMover.moveStreamInitialAssignments((SIRFilter)node.contents);
        //FieldProp.doPropagate((SIRFilter)node.contents);

        //Optimizations
        System.out.println("Optimizing "
                + ((SIRFilter) node.contents).getName() + "...");

    
        Set destroyed_vars = new HashSet();

        //ArrayDestroyer arrayDest=new ArrayDestroyer();
        for (int i = 0; i < ((SIRFilter)node.contents).getMethods().length; i++) {

            // do not optimize init work function
            if (node.contents instanceof SIRTwoStageFilter) {
                JMethodDeclaration init_work = ((SIRTwoStageFilter)node.contents).getInitWork();
                if (((SIRFilter)node.contents).getMethods()[i].equals(init_work)) {
                    continue;
                }
            }

                Unroller unroller;
            do {
                do {
                    // System.out.println("Unrolling..");
                    unroller = new Unroller(new Hashtable());
                    ((SIRFilter) node.contents).getMethods()[i]
                            .accept(unroller);
                } while (unroller.hasUnrolled());
                // System.out.println("Constant Propagating..");
                ((SIRFilter) node.contents).getMethods()[i]
                        .accept(new Propagator(new Hashtable()));
                // System.out.println("Unrolling..");
                unroller = new Unroller(new Hashtable());
                ((SIRFilter) node.contents).getMethods()[i].accept(unroller);
            } while (unroller.hasUnrolled());
            // System.out.println("Flattening..");
            ((SIRFilter) node.contents).getMethods()[i]
                    .accept(new BlockFlattener());
            // System.out.println("Analyzing Branches..");
            // ((SIRFilter)node.contents).getMethods()[i].accept(new
            // BranchAnalyzer());
            // System.out.println("Constant Propagating..");
            ((SIRFilter) node.contents).getMethods()[i].accept(new Propagator(
                    new Hashtable()));

            if (KjcOptions.destroyfieldarray) {
                ArrayDestroyer arrayDest = new ArrayDestroyer();
                ((SIRFilter)node.contents).getMethods()[i].accept(arrayDest);
                arrayDest.addDestroyedLocals(destroyed_vars);
            }

            ((SIRFilter)node.contents).getMethods()[i].accept(new VarDeclRaiser());
        }

        //if(KjcOptions.destroyfieldarray) {
        //   arrayDest.destroyFieldArrays((SIRFilter)node.contents);
        //}

        DeadCodeElimination.doit((SIRFilter)node.contents);

        IterFactory.createFactory().createIter((SIRFilter)node.contents).accept(toC);
    }
    
    public FlatIRToCluster2() 
    {
        this.str = new StringWriter();
        this.p = new TabbedPrintWriter(str);
    }
    

    public FlatIRToCluster2(TabbedPrintWriter p) {
        this.p = p;
        this.str = null;
        this.pos = 0;
    }
    
    public FlatIRToCluster2(SIRFilter f) {
        this.filter = f;
        //  circular = false;
        this.str = new StringWriter();
        this.p = new TabbedPrintWriter(str);
    }

    public String getString() {
        if (str != null)
            return str.toString();
        else
            return null;
    }
    
    /**
     * Close the stream at the end
     */
    public void close() {
        p.close();
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    /*  
        public void visitStructure(SIRStructure self,
        SIRStream parent,
        JFieldDeclaration[] fields)
        {
        print("struct " + self.getIdent() + " {\n");
        for (int i = 0; i < fields.length; i++)
        fields[i].accept(this);
        print("};\n");
        }
    */

    private int selfID = -1;
    

    public static String typeToC(CType t) {
        return typeToC(t, false);
    }

    public static String typeToC(CType t, boolean noVoid) {
        if (t.toString().compareTo("boolean") == 0) return "bool";
        //if (t.toString().compareTo("int") == 0) return "int";
        //if (t.toString().compareTo("float") == 0) return "float";
        if (t.toString().compareTo("void") == 0 && noVoid) return "int";
        return t.toString();
    } 

    public void visitFilter(SIRFilter self,
                            SIRFilterIter iter) {

        filter = self;
        selfID = NodeEnumerator.getSIROperatorId(self); // needed by the class

        int id = selfID;

        method_names = new HashSet<String>();
        JMethodDeclaration[] meth = self.getMethods();
        for (int i = 0; i < meth.length; i++) {
            method_names.add(meth[i].getName());
        }

        HashSet sendsCreditsTo = LatencyConstraints.getOutgoingConstraints(self);
        boolean restrictedExecution = LatencyConstraints.isRestricted(self); 
        boolean sendsCredits = (sendsCreditsTo.size() > 0);

        SIRPortal outgoing[] = SIRPortal.getPortalsWithSender(self);
        SIRPortal incoming[] = SIRPortal.getPortalsWithReceiver(self);
    
        Vector<SIRStream> sends_to = new Vector<SIRStream>();
        Vector<SIRStream> receives_from = new Vector<SIRStream>();

        FlatNode node = NodeEnumerator.getFlatNode(selfID);
        Iterator constrIter;

        System.out.println("flat node: "+node);
    
        Integer init_int = (Integer)ClusterBackend.initExecutionCounts.get(node);
        int init_counts;

        if (init_int == null) {
            init_counts = 0;
        } else {
            init_counts = init_int.intValue();
        }

        int steady_counts = ((Integer)ClusterBackend.steadyExecutionCounts.get(node)).intValue();


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

        print("// peek: "+peek_n+" pop: "+pop_n+" push "+push_n+"\n"); 
        print("// init counts: "+init_counts+" steady counts: "+steady_counts+"\n"); 
        print("\n");

        int data = DataEstimate.filterGlobalsSize(self);

        CodeEstimate est = CodeEstimate.estimate(self);

        //int code = CodeEstimate.estimateCode(self);
        //int locals = CodeEstimate.estimateLocals(self);
        int code = est.getCodeSize();
        int locals = est.getLocalsSize();

        System.out.println("[globals: "+data+" locals: "+locals+" code: "+code+"]");

        Vector data_in = (Vector)RegisterStreams.getNodeInStreams(self);
        Vector data_out = (Vector)RegisterStreams.getNodeOutStreams(self);

        JMethodDeclaration[] methods = self.getMethods();
        JFieldDeclaration[] fields = self.getFields();
        JMethodDeclaration work = self.getWork();

        CType input_type = self.getInputType();
        CType output_type = self.getOutputType();


        //NetStream in = RegisterStreams.getFilterInStream(self);
        //NetStream out = RegisterStreams.getFilterOutStream(self);

        //int out_pop_buffer_size = 10240;
        //int out_pop_num_iters = 0;
        //if (push_n != 0) out_pop_num_iters = out_pop_buffer_size / push_n;


        print("#include <stream_node.h>\n");

        constrIter = sendsCreditsTo.iterator();
        while (constrIter.hasNext()) {
            print("\n");
            LatencyConstraint constraint = (LatencyConstraint)constrIter.next();
            int receiver = NodeEnumerator.getSIROperatorId(constraint.getReceiver());

            int sourcePhase = constraint.getSourceSteadyExec();

            print("int __init_"+selfID+"_"+receiver+" = "+constraint.getSourceInit()+";\n");
            print("int __source_phase_"+selfID+"_"+receiver+" = "+sourcePhase+";\n");
            print("int __dest_phase_"+selfID+"_"+receiver+" = "+constraint.getDestSteadyExec()+";\n");
            print("int __current_"+selfID+"_"+receiver+" = 0;\n");
            print("int __dest_offset_"+selfID+"_"+receiver+" = 0;\n");


            print("int __dependency_"+selfID+"_"+receiver+"[] = {");

            int y;
        
            for (y = 0; y < sourcePhase - 1; y++) {
                print(constraint.getDependencyData(y)+",");
            }

            print(constraint.getDependencyData(y)+"};\n");
        }

        print("\n");

        //print("int *iteration__"+id+";\n");



        print("class thread"+selfID+" : public stream_node<"+
              typeToC(input_type,true)+","+typeToC(output_type,true)+"> {\n");

        //print("\n");
        //pos += TAB_SIZE;
        //print("\n");

        print("public:\n");
        print("\n");

        print("  thread"+selfID+"() : stream_node<"+
              typeToC(input_type,true)+","+typeToC(output_type,true)+">("+selfID+","+
              NodeEnumerator.getNumberOfNodes()+","+pop_n
              +","+peek_n+","+push_n);

        //print(",");
        //if (in != null) print(in.getSource()); else print("-1");
        //print(",");
        //if (out != null) print(out.getDest()); else print("-1");
    
        print(") {\n");

        pos += TAB_SIZE;

        {
            Iterator i;

            i = data_in.iterator();
            while (i.hasNext()) {
                NetStream _in = (NetStream)i.next();
                print("    add_input("+_in.getSource()+");\n");
            }
        
            i = data_out.iterator();
            while (i.hasNext()) {
                NetStream _out = (NetStream)i.next();
                print("    add_output("+_out.getDest()+");\n");
            }

            i = sends_to.iterator();
            while (i.hasNext()) {
                int dst = NodeEnumerator.getSIROperatorId((SIRStream)i.next());
                print("    add_msg_out("+dst+");\n");
            }

            i = receives_from.iterator();
            while (i.hasNext()) {
                int src = NodeEnumerator.getSIROperatorId((SIRStream)i.next());
                print("    add_msg_in("+src+");\n");
            }
        }

        pos -= TAB_SIZE;
        p.setPos(pos);
        print("  }\n");

        print("\n");

        //  +=============================+
        //  | Read / Write Thread         |
        //  +=============================+


        print("\n");
        print("  int state_size() {\n");
        print("    int res=0;\n");

        for (int f = 0; f < fields.length; f++) {
            CType type = fields[f].getType();
            String ident = fields[f].getVariable().getIdent();

            DetectConst dc = DetectConst.getInstance((SIRFilter)self);
            if (dc != null && dc.isConst(ident)) continue;

            if (type.isArrayType()) {
                int size = 0;
                String dims[] = this.makeArrayStrings(((CArrayType)type).getDims());
                CType base = ((CArrayType)type).getBaseType();
                try {
                    size = Integer.valueOf(dims[0]).intValue();
                } catch (NumberFormatException ex) {
                    System.out.println("Warning! Could not estimate size of an array: "+ident);
                }
                print("    res += "+size+" * sizeof("+typeToC(base)+");\n");
            } else {
                print("    res += sizeof("+typeToC(type)+");\n");
            }
        }

        {
            constrIter = sendsCreditsTo.iterator();
            while (constrIter.hasNext()) {
                LatencyConstraint constraint = (LatencyConstraint)constrIter.next();
                int receiver = NodeEnumerator.getSIROperatorId(constraint.getReceiver());
                print("    res += 2 * sizeof(int);\n");
            }
        }


        print("    return res;\n");
        print("  }\n");

        {
            //Iterator i;

            //r.add("void save_peek_buffer__"+id+"(object_write_buffer *buf);\n");
            //r.add("void load_peek_buffer__"+id+"(object_write_buffer *buf);\n");
            print("\n");

            print("  thread"+id+" *clone() {\n");
            print("    thread"+id+" *res = new thread"+id+"();\n");
            print("    res->clone_stream(this);\n"); 

            for (int f = 0; f < fields.length; f++) {
                CType type = fields[f].getType();
                String ident = fields[f].getVariable().getIdent();

                DetectConst dc = DetectConst.getInstance((SIRFilter)self);
                if (dc != null && dc.isConst(ident)) continue;

                if (type.isArrayType()) {
                    int size = 0;
                    String dims[] = this.makeArrayStrings(((CArrayType)type).getDims());
                    //CType base = ((CArrayType)type).getBaseType();
                    try {
                        size = Integer.valueOf(dims[0]).intValue();
                    } catch (NumberFormatException ex) {
                        System.out.println("Warning! Could not estimate size of an array: "+ident);
                    }
                    print("    for (int z = 0; z < "+size+"; z++)\n"); 
                    print("      res->"+ident+"__"+id+"[z] = "+ident+"__"+id+"[z];\n");
                } else {
                    print("    res->"+ident+"__"+id+" = "+ident+"__"+id+";\n");
                }
            }

            /*
              constrIter = sendsCreditsTo.iterator();
              while (constrIter.hasNext()) {
              LatencyConstraint constraint = (LatencyConstraint)constrIter.next();
              int receiver = NodeEnumerator.getSIROperatorId(constraint.getReceiver());
              print("    res->__current_"+selfID+"_"+receiver+" = __current_"+selfID+"_"+receiver+";\n");
              print("    res->__dest_offset_"+selfID+"_"+receiver+" = __dest_offset_"+selfID+"_"+receiver+";\n");
              }
            */

            /*
              i = data_out.iterator();
              while (i.hasNext()) {
              NetStream _out = (NetStream)i.next();
              print("    //"+_out.producer_name()+".write_object(buf);\n");
              }
            */

            print("    return res;\n");
            print("  }\n");

            print("\n");    

            print("  void save_state(object_write_buffer *buf) {\n");

            /*
              i = data_in.iterator();
              while (i.hasNext()) {
              NetStream _in = (NetStream)i.next();
              print("    //"+_in.consumer_name()+".write_object(buf);\n");
        

              //if (oper instanceof SIRFilter) {
              // r.add("  "+in.name()+"in.write_object(buf);\n");
              //}
              }
            */

            //r.add("  save_peek_buffer__"+id+"(buf);\n");

            for (int f = 0; f < fields.length; f++) {
                CType type = fields[f].getType();
                String ident = fields[f].getVariable().getIdent();

                DetectConst dc = DetectConst.getInstance((SIRFilter)self);
                if (dc != null && dc.isConst(ident)) continue;

                if (type.isArrayType()) {
                    int size = 0;
                    String dims[] = this.makeArrayStrings(((CArrayType)type).getDims());
                    CType base = ((CArrayType)type).getBaseType();
                    try {
                        size = Integer.valueOf(dims[0]).intValue();
                    } catch (NumberFormatException ex) {
                        System.out.println("Warning! Could not estimate size of an array: "+ident);
                    }
                    print("    buf->write("+ident+"__"+id+", "+size+" * sizeof("+typeToC(base)+"));\n");
                } else {
                    print("    buf->write(&"+ident+"__"+id+", sizeof("+typeToC(type)+"));\n");
                }
            }


            constrIter = sendsCreditsTo.iterator();
            while (constrIter.hasNext()) {
                LatencyConstraint constraint = (LatencyConstraint)constrIter.next();
                int receiver = NodeEnumerator.getSIROperatorId(constraint.getReceiver());
                print("    buf->write(&__current_"+selfID+"_"+receiver+", sizeof(int));\n");
                print("    buf->write(&__dest_offset_"+selfID+"_"+receiver+", sizeof(int));\n");
            }

            /*
              i = data_out.iterator();
              while (i.hasNext()) {
              NetStream _out = (NetStream)i.next();
              print("    //"+_out.producer_name()+".write_object(buf);\n");
              }
            */

            print("  }\n");

            print("\n");    

            print("  void load_state(object_write_buffer *buf) {\n");

            /*
              i = data_in.iterator();
              while (i.hasNext()) {
              NetStream _in = (NetStream)i.next();
              print("    //"+_in.consumer_name()+".read_object(buf);\n");

              //if (oper instanceof SIRFilter) {
              //r.add("  "+in.name()+"in.read_object(buf);\n");
              //}
              }
            */

            //r.add("  load_peek_buffer__"+id+"(buf);\n");

            for (int f = 0; f < fields.length; f++) {
                CType type = fields[f].getType();
                String ident = fields[f].getVariable().getIdent();

                DetectConst dc = DetectConst.getInstance((SIRFilter)self);
                if (dc != null && dc.isConst(ident)) continue;

                if (type.isArrayType()) {
                    int size = 0;
                    String dims[] = this.makeArrayStrings(((CArrayType)type).getDims());
                    CType base = ((CArrayType)type).getBaseType();
                    try {
                        size = Integer.valueOf(dims[0]).intValue();
                    } catch (NumberFormatException ex) {
                        System.out.println("Warning! Could not estimate size of an array: "+ident);
                    }
                    print("    buf->read("+ident+"__"+id+", "+size+" *  sizeof("+typeToC(base)+"));\n");
                } else {
                    print("    buf->read(&"+ident+"__"+id+", sizeof("+typeToC(type)+"));\n");
                }
            }


            constrIter = sendsCreditsTo.iterator();
            while (constrIter.hasNext()) {
                LatencyConstraint constraint = (LatencyConstraint)constrIter.next();
                int receiver = NodeEnumerator.getSIROperatorId(constraint.getReceiver());
                print("    buf->read(&__current_"+selfID+"_"+receiver+", sizeof(int));\n");
                print("    buf->read(&__dest_offset_"+selfID+"_"+receiver+", sizeof(int));\n");
            }

            /*
              i = data_out.iterator();
              while (i.hasNext()) {
              NetStream _out = (NetStream)i.next();
              print("    //"+_out.producer_name()+".read_object(buf);\n");
              }
            */

            print("  }\n");

            print("\n");    
    
        }

        //======================================


        print("  void init_state() {\n");

        print("    //super::init_state();\n");

        if (restrictedExecution) {
            print("    set_need_credit();\n");
        }

        {
            Iterator i;

            i = sends_to.iterator();
            while (i.hasNext()) {
        
                SIRFilter sender = self;
                SIRFilter receiver = (SIRFilter)i.next();
        
                int fromID = id;
                int toID = NodeEnumerator.getSIROperatorId(receiver);
        
                boolean downstream = LatencyConstraints.isMessageDirectionDownstream(sender, receiver);
        
                print("\n    //SDEP from: "+fromID+" to: "+toID+";\n");
        
                streamit.scheduler2.constrained.Scheduler cscheduler =
                    streamit.scheduler2.constrained.Scheduler.createForSDEP(ClusterBackend.topStreamIter);
        
                streamit.scheduler2.iriter.Iterator firstIter = 
                    IterFactory.createFactory().createIter(sender);
                streamit.scheduler2.iriter.Iterator lastIter = 
                    IterFactory.createFactory().createIter(receiver);   
        
                streamit.scheduler2.SDEPData sdep = null;
        
                if (downstream) {
                    print("    //message sent downstream;\n");
            
                    try {
                        sdep = cscheduler.computeSDEP(firstIter, lastIter);
                    } catch (streamit.scheduler2.constrained.NoPathException ex) {}
            
                } else {
                    print("    //message sent upstream;\n");
            
                    try {
                        sdep = cscheduler.computeSDEP(lastIter, firstIter);
                    } catch (streamit.scheduler2.constrained.NoPathException ex) {}
                }
        
                int srcInit = sdep.getNumSrcInitPhases();
                int srcSteady = sdep.getNumSrcSteadyPhases();
        
                int dstInit = sdep.getNumDstInitPhases();
                int dstSteady = sdep.getNumDstSteadyPhases();
        
                String sdepname = "sdep_"+fromID+"_"+toID;
        
                print("    "+sdepname+" = new sdep("+
                      srcInit+","+dstInit+","+
                      srcSteady+","+dstSteady+");\n");
        
                for (int y = 0; y < dstInit + dstSteady + 1; y++) {
                    print("    "+sdepname+"->setDst2SrcDependency("+y+","+sdep.getSrcPhase4DstPhase(y)+");\n");
                }
        
                print("\n");
            }
        }
        
        
        print("    "+self.getInit().getName()+/*"__"+id+*/"();\n");
        print("  }\n");


        
        //  +=============================+
        //  | Send Credits                |
        //  +=============================+

        //HashSet sendsCreditsTo = LatencyConstraints.getOutgoingConstraints(self);
        //boolean restrictedExecution = LatencyConstraints.isRestricted(self); 
        //boolean sendsCredits;

        print("\n  void send_credits() {\n");
        print("  int tmp;\n");

        constrIter = sendsCreditsTo.iterator();
        while (constrIter.hasNext()) {
            LatencyConstraint constraint = (LatencyConstraint)constrIter.next();

            int receiver = NodeEnumerator.getSIROperatorId(constraint.getReceiver());

            if (LatencyConstraints.isMessageDirectionDownstream(self, constraint.getReceiver())) {

                // message and credit is sent downstream

                print("  if (iteration > __init_"+selfID+"_"+receiver+") {\n");
                print("    tmp = __dependency_"+selfID+"_"+receiver+"[__current_"+selfID+"_"+receiver+"];\n");
                print("    if (tmp > 0) {\n");
                print("      send_credit("+receiver+", tmp + __dest_offset_"+selfID+"_"+receiver+");\n");

                //print("      get_msg_out_socket("+receiver+")->write_int(-1);\n");
                //print("      get_msg_out_socket("+receiver+")->write_int(tmp + __dest_offset_"+selfID+"_"+receiver+");\n");

                print("    }\n");
                print("    __current_"+selfID+"_"+receiver+" = (__current_"+selfID+"_"+receiver+" + 1) % __source_phase_"+selfID+"_"+receiver+";\n");
                print("    if (__current_"+selfID+"_"+receiver+" == 0) __dest_offset_"+selfID+"_"+receiver+" += __dest_phase_"+selfID+"_"+receiver+";\n");
                print("  }\n");   
            } else {

                // message and credit is sent upstream
        
        
                print("    send_credit("+receiver+", (++__current_"+selfID+"_"+receiver+")*1023);\n");

                /*
                  print("  if (iteration == 0) {\n");
                  print("    send_credit("+receiver+", __init_"+selfID+"_"+receiver+");\n");

                  //print("    __msg_sock_"+selfID+"_"+receiver+"out->write_int(-1);\n");
                  //print("    __msg_sock_"+selfID+"_"+receiver+"out->write_int(__init_"+selfID+"_"+receiver+");\n");

                  print("  } else {\n");   
                  print("    tmp = __dependency_"+selfID+"_"+receiver+"[__current_"+selfID+"_"+receiver+"];\n");
                  print("    if (tmp > 0) {\n");

                  print("    send_credit("+receiver+", tmp + __dest_offset_"+selfID+"_"+receiver+");\n");

                  //print("      get_msg_out_socket("+receiver+")->write_int(-1);\n");
                  //print("      get_msg_out_socket("+receiver+")->write_int(tmp + __dest_offset_"+selfID+"_"+receiver+");\n");
                  print("    }\n");   
                  print("    __current_"+selfID+"_"+receiver+" = (__current_"+selfID+"_"+receiver+" + 1) % __source_phase_"+selfID+"_"+receiver+";\n");
                  print("    if (__current_"+selfID+"_"+receiver+" == 0) __dest_offset_"+selfID+"_"+receiver+" += __dest_phase_"+selfID+"_"+receiver+";\n");
                  print("  }\n");   
                */
            }
        }


        print("  }\n");
        print("\n");


        print("  void exec_message(message *msg) {\n");

        SIRPortal[] portals = SIRPortal.getPortalsWithReceiver(self);

        // this block will need to be adjusted to receive from
        // multiple portals of different types (different interfaces)
        if (portals.length > 0) {

                CClass pclass = portals[0].getPortalType().getCClass();

                CMethod pmethods[] = pclass.getMethods();

                for (int i = 0 ; i < pmethods.length; i++) {

                    CMethod portal_method = pmethods[i];
                    CType portal_method_params[] = portal_method.getParameters();

                    String method_name = portal_method.getIdent();

                    int length = method_name.length();

                    if (!method_name.startsWith("<") && 
                        !method_name.endsWith(">")) {

                        print("    if (msg->method_id == "+i+") {\n");

                        for (int t = 0; t < methods.length; t++) {
            
                            String thread_method_name = methods[t].getName();
            
                            if (thread_method_name.startsWith(method_name) &&
                                thread_method_name.charAt(length) == '_' &&
                                thread_method_name.charAt(length + 1) == '_') {
                
                                int param_count = methods[t].getParameters().length;

                                for (int a = 0; a < param_count; a++) {
                                    if (portal_method_params[a].toString().equals("int")) {
                                        print("      int p"+a+" = msg->get_int_param();\n");
                                    }
                                    if (portal_method_params[a].toString().equals("float")) {
                                        print("      float p"+a+" = msg->get_float_param();\n");
                                    }
                                }

                                print("      "+thread_method_name+/*"__"+selfID+*/"(");
                                for (int a = 0; a < param_count; a++) {
                                    if (a > 0) print(", ");
                                    print("p"+a);
                                }
                                print(");\n");
                            }
                        }
                        print("    }\n");
                    }
                }
            }

        print("  }\n");
            
        print("\n//============================================================\n");
        print("\n");

        for (int f = 0; f < fields.length; f++) {
            CType type = fields[f].getType();
            String ident = fields[f].getVariable().getIdent();

            if (type.toString().endsWith("Portal")) continue;
            print(typeToC(type)+" "+ident+"__"+id+";\n");
        }

        {
            Iterator i = sends_to.iterator();
            if (i.hasNext()) {
                print("\n");
                while (i.hasNext()) {
                    SIRStream str = (SIRStream)i.next();
                    print("sdep *sdep_"+id+"_"+NodeEnumerator.getSIROperatorId(str)+";\n");
                }
            }
        }



        print("\n//============================================================\n");

        //  +=============================+
        //  | Method Bodies               |
        //  +=============================+

        declOnly = false;
        for (int i = 0; i < methods.length; i++) {
            String old_name = "";
            if (methods[i].equals(work)) {
                old_name = methods[i].getName();
                methods[i].setName("work");
            }
            methods[i].accept(this);
            if (methods[i].equals(work)) methods[i].setName(old_name);
        }

        //  +=============================+
        //  | Work Function (int ____n)   |
        //  +=============================+
    
        JBlock block = new JBlock(null, new JStatement[0], null);

        JVariableDefinition counter = 
            new JVariableDefinition(null, 
                                    0, 
                                    CStdType.Integer,
                                    "____n",
                                    new JIntLiteral(0));


//        JVariableDefinition iter_counter = 
//            new JVariableDefinition(null, 
//                                    0, 
//                                    CStdType.Integer,
//                                    "__counter_"+selfID,
//                                    null);
    

        JStatement init = new JEmptyStatement(null, null);

        //VariableDeclarationStatement(null, tmp, null);


        JExpression decrExpr = 
            new JPostfixExpression(null, 
                                   Constants.OPE_POSTDEC, 
                                   new JLocalVariableExpression(null,
                                                                counter));

        JStatement decr = 
            new JExpressionStatement(null, decrExpr, null);

        JExpression cond = 
            new JRelationalExpression(null,
                                      Constants.OPE_LT,
                                      new JIntLiteral(0),
                                      new JLocalVariableExpression(null,counter));


        block.addStatement(new JForStatement(null, init, cond, decr, work.getBody(),
                                             null));

        // __counter_X = __counter_X + ____n;
    
        JFormalParameter param = new JFormalParameter(null, 0, CStdType.Integer, "____n", true);
        JFormalParameter params[] = new JFormalParameter[1];
        params[0] = param;

        JMethodDeclaration work_n = 
            new JMethodDeclaration(null, 
                                   at.dms.kjc.Constants.ACC_PUBLIC,
                                   CStdType.Void,
                                   "work_n", //work.getName(),
                                   params,
                                   CClassType.EMPTY,
                                   block,
                                   null,
                                   null);

        work_n.accept(this);


        print("\n//============================================================\n");

        //  +=============================+
        //  | Push / Pop                  |
        //  +=============================+


        pos -= TAB_SIZE;

        print("};\n");
        print("\n");

        print("thread"+id+" *instance_"+id+" = NULL;\n");
        print("thread"+id+" *get_instance_"+id+"() {\n");
        print("    if (instance_"+id+" == NULL) instance_"+id+" = new thread"+id+"();\n");
        print("   instance_"+id+"->init_stream();\n");
        print("    return instance_"+id+";\n");
        print("}\n");
        print("\n");

        print("void __get_thread_info_"+id+"() { get_instance_"+id+"()->get_thread_info(); }\n");
        print("void __declare_sockets_"+id+"() { get_instance_"+id+"()->declare_sockets(); }\n");
        print("extern int __max_iteration;\n");
        print("void run_"+id+"() { get_instance_"+id+"()->run_simple("+init_counts+"+("+steady_counts+"*__max_iteration)); }\n"); 
        createFile(selfID);

        //return;
    

        //  +=============================+
        //  | Check Messages              |
        //  +=============================+

        print("\nvoid check_messages__"+selfID+"() {\n");

        print("  message *msg, *last = NULL;\n");


        if (restrictedExecution) {
            print("  while (");
            Iterator i = receives_from.iterator();
            while (i.hasNext()) {
                int src = NodeEnumerator.getSIROperatorId((SIRStream) i.next());
                print("__credit_" + src + "_" + selfID + " <= __counter_" + selfID);
                if (i.hasNext()) print(" || ");
            }
            print(") {\n");
        }
    
        {
            Iterator i = receives_from.iterator();
            while (i.hasNext()) {
                int src = NodeEnumerator.getSIROperatorId((SIRStream)i.next());
                print("  while (__msg_sock_"+src+"_"+selfID+"in->data_available()) {\n    handle_message__"+selfID+"(__msg_sock_"+src+"_"+selfID+"in, &__credit_" + src + "_" + selfID +");\n  } // if\n");
            }
        }

        if (restrictedExecution) {
            print("  } // while \n");
        }


        print("  for (msg = __msg_stack_"+selfID+"; msg != NULL; msg = msg->next) {\n");
        print("    if (msg->execute_at == __counter_"+selfID+") {\n");


        //SIRPortal[] portals = SIRPortal.getPortalsWithReceiver(self);

        // this block will need to be adjusted to receive from
        // multiple portals of different types (different interfaces)
        if (portals.length > 0) {
        
            CClass pclass = portals[0].getPortalType().getCClass();

            CMethod pmethods[] = pclass.getMethods();

            for (int i = 0 ; i < pmethods.length; i++) {

                CMethod portal_method = pmethods[i];
                CType portal_method_params[] = portal_method.getParameters();

                String method_name = portal_method.getIdent();

                int length = method_name.length();

                if (!method_name.startsWith("<") && 
                    !method_name.endsWith(">")) {

                    print("      if (msg->method_id == "+i+") {\n");

                    for (int t = 0; t < methods.length; t++) {
            
                        String thread_method_name = methods[t].getName();
            
                        if (thread_method_name.startsWith(method_name) &&
                            thread_method_name.charAt(length) == '_' &&
                            thread_method_name.charAt(length + 1) == '_') {
                
                            int param_count = methods[t].getParameters().length;

                            for (int a = 0; a < param_count; a++) {
                                if (portal_method_params[a].toString().equals("int")) {
                                    print("        int p"+a+" = msg->get_int_param();\n");
                                }
                                if (portal_method_params[a].toString().equals("float")) {
                                    print("        float p"+a+" = msg->get_float_param();\n");
                                }
                            }

                            print("        "+thread_method_name+"__"+selfID+"(");
                            for (int a = 0; a < param_count; a++) {
                                if (a > 0) print(", ");
                                print("p"+a);
                            }
                            print(");\n");
                        }
                    }
                    print("      }\n");
                }
            }
        
            print("      if (last != NULL) { \n");
            print("        last->next = msg->next;\n");
            print("      } else {\n");
            print("        __msg_stack_"+selfID+" = msg->next;\n");
            print("      }\n");
            print("      delete msg;\n");

        }

        print("    } else if (msg->execute_at > __counter_" + selfID + ") {\n");
        print("      last = msg;\n");
        print("    } else { // msg->execute_at < __counter_" + selfID + "\n");
        print("      message::missed_delivery();\n");
        print("    }");
        print("  } // for \n");

        print("}\n");
    
        //  +=============================+
        //  | Handle Message              |
        //  +=============================+

    
        print("\nvoid handle_message__"+selfID+"(netsocket *sock, int* credit) {\n");
        print("  int size = sock->read_int();\n");
    
        if (restrictedExecution) {
            print("  if (size == -1) { // a credit message received\n");
            print("    *credit = sock->read_int();\n");
            print("    return;\n");
            print("  };\n");
        }

        print("  int index = sock->read_int();\n");
        print("  int iteration = sock->read_int();\n");
        print("  printf(\"//Message receieved! thread: "+selfID+", method_index: %d excute at iteration: %d\\n\", index, iteration);\n");

        print("  message *msg = new message(size, index, iteration);\n");
        print("  msg->read_params(sock,16);\n");
        print("  __msg_stack_"+selfID+" = msg->push_on_stack(__msg_stack_"+selfID+");\n");
        print("}\n");
    
        //  +=============================+
        //  | Send Credits                |
        //  +=============================+


        //Iterator constrIter;
        constrIter = sendsCreditsTo.iterator();
        while (constrIter.hasNext()) {
            LatencyConstraint constraint = (LatencyConstraint)constrIter.next();
            int receiver = NodeEnumerator.getSIROperatorId(constraint.getReceiver());

            int sourcePhase = constraint.getSourceSteadyExec();

            print("int __init_"+selfID+"_"+receiver+" = "+constraint.getSourceInit()+";\n");
            print("int __source_phase_"+selfID+"_"+receiver+" = "+sourcePhase+";\n");
            print("int __dest_phase_"+selfID+"_"+receiver+" = "+constraint.getDestSteadyExec()+";\n");
            print("int __current_"+selfID+"_"+receiver+" = 0;\n");
            print("int __dest_offset_"+selfID+"_"+receiver+" = 0;\n");


            print("int __dependency_"+selfID+"_"+receiver+"[] = {");

            int y;
        
            for (y = 0; y < sourcePhase - 1; y++) {
                print(constraint.getDependencyData(y)+",");
            }

            print(constraint.getDependencyData(y)+"};\n");

            print("\n");
        }
    
        print("\ninline void send_credits__"+selfID+"() {\n");

        print("  int tmp;\n");

        

        //HashSet sendsCreditsTo = LatencyConstraints.getOutgoingConstraints(self);
        //boolean restrictedExecution = LatencyConstraints.isRestricted(self); 
        //boolean sendsCredits;

        constrIter = sendsCreditsTo.iterator();
        while (constrIter.hasNext()) {
            LatencyConstraint constraint = (LatencyConstraint)constrIter.next();

            int receiver = NodeEnumerator.getSIROperatorId(constraint.getReceiver());

            if (LatencyConstraints.isMessageDirectionDownstream(self, constraint.getReceiver())) {

                // message and credit is sent downstream

                print("  if (__counter_"+selfID+" > __init_"+selfID+"_"+receiver+") {\n");
                print("    tmp = __dependency_"+selfID+"_"+receiver+"[__current_"+selfID+"_"+receiver+"];\n");
                print("    if (tmp > 0) {\n");
                print("      __msg_sock_"+selfID+"_"+receiver+"out->write_int(-1);\n");
                print("      __msg_sock_"+selfID+"_"+receiver+"out->write_int(tmp + __dest_offset_"+selfID+"_"+receiver+");\n");
                print("    }\n");
                print("    __current_"+selfID+"_"+receiver+" = (__current_"+selfID+"_"+receiver+" + 1) % __source_phase_"+selfID+"_"+receiver+";\n");
                print("    if (__current_"+selfID+"_"+receiver+" == 0) __dest_offset_"+selfID+"_"+receiver+" += __dest_phase_"+selfID+"_"+receiver+";\n");
                print("  }\n");   
            } else {

                // message and credit is sent upstream
        
                print("  if (__counter_"+selfID+" == 0) {\n");
                print("    __msg_sock_"+selfID+"_"+receiver+"out->write_int(-1);\n");
                print("    __msg_sock_"+selfID+"_"+receiver+"out->write_int(__init_"+selfID+"_"+receiver+");\n");
                print("  } else {\n");   
                print("    tmp = __dependency_"+selfID+"_"+receiver+"[__current_"+selfID+"_"+receiver+"];\n");
                print("    if (tmp > 0) {\n");
                print("      __msg_sock_"+selfID+"_"+receiver+"out->write_int(-1);\n");
                print("      __msg_sock_"+selfID+"_"+receiver+"out->write_int(tmp + __dest_offset_"+selfID+"_"+receiver+");\n");
                print("    }\n");   
                print("    __current_"+selfID+"_"+receiver+" = (__current_"+selfID+"_"+receiver+" + 1) % __source_phase_"+selfID+"_"+receiver+";\n");
                print("    if (__current_"+selfID+"_"+receiver+" == 0) __dest_offset_"+selfID+"_"+receiver+" += __dest_phase_"+selfID+"_"+receiver+";\n");
                print("  }\n");   
        
            }
        }


        print("}\n");

        //  +=============================+
        //  | Cluster Main                |
        //  +=============================+

        //Vector run = gen.generateRunFunction(filter.getInit().getName()+"__"+selfID, filter.getWork().getName()+"__"+selfID);

        //for (int i = 0; i < run.size(); i++) {
        //    print(run.elementAt(i).toString());
        //}
       
        //createFile(selfID);
    }

    public void visitPhasedFilter(SIRPhasedFilter self,
                                  SIRPhasedFilterIter iter) {
        // This is a stub; it'll get filled in once we figure out how phased
        // filters should actually work.
    }

    private void createFile(int thread_id) {

        System.out.println("Code for " + filter.getName() +
                           " written to thread_" + thread_id +
                           ".cpp");
        try {
            FileWriter fw = new FileWriter("thread_" + thread_id + ".cpp");
            fw.write(str.toString());
            fw.close();
        }
        catch (Exception e) {
            System.err.println("Unable to write tile code file for filter " +
                               filter.getName());
        }

    }
                
    //     /**
    //      * prints a field declaration
    //      */
    //     public void visitFieldDeclaration(JFieldDeclaration self,
    //                                       int modifiers,
    //                                       CType type,
    //                                       String ident,
    //                                       JExpression expr) {
    //         /*
    //           if (ident.indexOf("$") != -1) {
    //           return; // dont print generated elements
    //           }
    //         */

    //  if (type.toString().endsWith("Portal")) return;

    //         newLine();
    //         // print(CModifier.toString(modifiers));

    //  //only stack allocate singe dimension arrays
    //  if (expr instanceof JNewArrayExpression) {
    //      //print the basetype
    //      print(((CArrayType)type).getBaseType());
    //      print(" ");
    //      //print the field identifier
    //      print(ident);
    //      //print the dims
    //      stackAllocateArray(ident);
    //      print(";");
    //      return;
    //  } else if (expr instanceof JArrayInitializer) {
    //      declareInitializedArray(type, ident, expr);
    //      return;
    //  }

    //  if (type.toString().compareTo("boolean") == 0) {
    //      print("bool");
    //  } else {
    //      print(type);
    //  }

    //         print(" ");
    //         print(ident);
    //  print("__"+selfID);

    //         if (expr != null) {
    //             print("\t= ");
    //      expr.accept(this);
    //         }   //initialize all fields to 0
    //  else if (type.isOrdinal())
    //      print (" = 0");
    //  else if (type.isFloatingPoint())
    //      print(" = 0.0f");

    //         print(";/* "+type+" size: "+byteSize(type)+" */\n");
    //     }

    /**
     * prints a method declaration
     */
    public void visitMethodDeclaration(JMethodDeclaration self,
                                       int modifiers,
                                       CType returnType,
                                       String ident,
                                       JFormalParameter[] parameters,
                                       CClassType[] exceptions,
                                       JBlock body) {
        //System.out.println(ident);
    
        //in the raw path we do not want to print the 
        //prework or work function definition
    

        // this prevented some work functions from printing out 
        /*
          if (filter != null && 
          (filter.getWork().equals(self) ||
          (filter instanceof SIRTwoStageFilter &&
          ((SIRTwoStageFilter)filter).getInitWork().equals(self))))
          return;
        */

        // try converting to macro
        if (MacroConversion.shouldConvert(self)) {
            MacroConversion.doConvert(self, declOnly, this);
            return;
        }

        if (helper_package == null) newLine();

        // print(CModifier.toString(modifiers));
        print(returnType);
        print(" ");
        if (global) print("__global__");
        if (helper_package != null) print(helper_package+"_");
        print(ident);
        // this breaks initpath

        /*
          if (helper_package == null && !global && 
          !ident.startsWith("__Init_Path_")) {
          print("__"+selfID);
          }
        */

        print("(");
        int count = 0;
    
        for (int i = 0; i < parameters.length; i++) {
            if (count != 0) {
                print(", ");
            }
            parameters[i].accept(this);
            count++;
        }
        print(")");
    
        //print the declaration then return
        if (declOnly) {
            print(";");
            return;
        }
    
        method = self;

        //set is init for dynamically allocating arrays...
        if (filter != null &&
            self.getName().startsWith("init"))
            isInit = true;

        print(" ");
        if (body != null) 
            body.accept(this);
        else 
            print(";");

        newLine();
        isInit = false;
        method = null;
    }

    
    // ----------------------------------------------------------------------
    // STATEMENT
    // ----------------------------------------------------------------------

    /**
     * prints a while statement
     */
    public void visitWhileStatement(JWhileStatement self,
                                    JExpression cond,
                                    JStatement body) {
        print("while (");
        cond.accept(this);
        print(") ");

        body.accept(this);
    }

    /**
     * prints a variable declaration statement
     */
    public void visitVariableDeclarationStatement(JVariableDeclarationStatement self,
                                                  JVariableDefinition[] vars) {
        for (int i = 0; i < vars.length; i++) {
            vars[i].accept(this);
        }
    }

    /**
     * prints a variable declaration statement
     */
    public void visitVariableDefinition(JVariableDefinition self,
                                        int modifiers,
                                        CType type,
                                        String ident,
                                        JExpression expr) {


        if (expr instanceof JArrayInitializer) {
            declareInitializedArray(type, ident, expr);
        } else {

            printDecl (type, ident);
        
            if (expr != null && !(expr instanceof JNewArrayExpression)) {
                p.print ("\t= ");
                expr.accept (this);
            } else if (type.isOrdinal())
                p.print(" = 0");
            else if (type.isFloatingPoint())
                p.print(" = 0.0f");

            p.print(";/* " + type + " */");
        }
    }


    /**
     * prints a switch statement
     */
    public void visitSwitchStatement(JSwitchStatement self,
                                     JExpression expr,
                                     JSwitchGroup[] body) {
        print("switch (");
        expr.accept(this);
        print(") {");
        for (int i = 0; i < body.length; i++) {
            body[i].accept(this);
        }
        newLine();
        print("}");
    }

    /**
     * prints a return statement
     */
    public void visitReturnStatement(JReturnStatement self,
                                     JExpression expr) {
        print("return");
        if (expr != null) {
            print(" ");
            expr.accept(this);
        }
        print(";");
    }

    /**
     * prints a labeled statement
     */
    public void visitLabeledStatement(JLabeledStatement self,
                                      String label,
                                      JStatement stmt) {
        print(label + ":");
        stmt.accept(this);
    }

    /**
     * prints a if statement
     */
    public void visitIfStatement(JIfStatement self,
                                 JExpression cond,
                                 JStatement thenClause,
                                 JStatement elseClause) {
        print("if (");
        cond.accept(this);
        print(") {");
        pos += thenClause instanceof JBlock ? 0 : TAB_SIZE;
        thenClause.accept(this);
        pos -= thenClause instanceof JBlock ? 0 : TAB_SIZE;
        if (elseClause != null) {
            if ((elseClause instanceof JBlock) || (elseClause instanceof JIfStatement)) {
                print(" ");
            } else {
                newLine();
            }
            print("} else {");
            pos += elseClause instanceof JBlock || elseClause instanceof JIfStatement ? 0 : TAB_SIZE;
            elseClause.accept(this);
            pos -= elseClause instanceof JBlock || elseClause instanceof JIfStatement ? 0 : TAB_SIZE;
        }
        print("}");
    }

    /**
     * Prints initialization for an array with static initializer, e.g., "int A[2] = {1,2};"
     *
     * To promote code reuse with other backends, inputs a visitor to
     * do the recursive call.
     */
    private void declareInitializedArray(CType type, String ident, JExpression expr) {
        print(((CArrayType)type).getBaseType()); // note this calls print(CType), not print(String)
        print(" " + ident);
        JArrayInitializer init = (JArrayInitializer)expr;
        while (true) {
            int length = init.getElems().length;
            print("[" + length + "]");
            if (length==0) { 
                // hope that we have a 1-dimensional array in
                // this case.  Otherwise we won't currently
                // get the type declarations right for the
                // lower pieces.
                break;
            }
            // assume rectangular arrays
            JExpression next = (JExpression)init.getElems()[0];
            if (next instanceof JArrayInitializer) {
                init = (JArrayInitializer)next;
            } else {
                break;
            }
        }
        print(" = ");
        expr.accept(this);
        print(";");
        return;
    }
                
    /**
     * prints a for statement
     */
    public void visitForStatement(JForStatement self,
                                  JStatement init,
                                  JExpression cond,
                                  JStatement incr,
                                  JStatement body) {
        //be careful, if you return prematurely, decrement me
        forLoopHeader++;

        print("for (");
        if (init != null) {
            init.accept(this);
            //the ; will print in a statement visitor
        }

        print(" ");
        if (cond != null) {
            cond.accept(this);
        }
        //cond is an expression so print the ;
        print("; ");
        if (incr != null) {
            FlatIRToCluster2 l2c = new FlatIRToCluster2(filter);
            incr.accept(l2c);
            // get String
            String str = l2c.getString();
            // leave off the trailing semicolon if there is one
            if (str.endsWith(";")) {
                print(str.substring(0, str.length()-1));
            } else { 
                print(str);
            }
        }

        forLoopHeader--;
        print(") ");

        print("{");
        pos += TAB_SIZE;
        body.accept(this);
        pos -= TAB_SIZE;
        newLine();
        print("}");
    }

    /**
     * prints a compound statement
     */
    public void visitCompoundStatement(JCompoundStatement self,
                                       JStatement[] body) {
        visitCompoundStatement(body);
    }

    /**
     * prints a compound statement
     */
    public void visitCompoundStatement(JStatement[] body) {
        for (int i = 0; i < body.length; i++) {
            if (body[i] instanceof JIfStatement &&
                i < body.length - 1 &&
                !(body[i + 1] instanceof JReturnStatement)) {
                newLine();
            }
            if (body[i] instanceof JReturnStatement && i > 0) {
                newLine();
            }

            newLine();
            body[i].accept(this);

            if (body[i] instanceof JVariableDeclarationStatement &&
                i < body.length - 1 &&
                !(body[i + 1] instanceof JVariableDeclarationStatement)) {
                newLine();
            }
        }
    }

    /**
     * prints an expression statement
     */
    public void visitExpressionStatement(JExpressionStatement self,
                                         JExpression expr) {
        expr.accept(this);
        print(";");
    }

    /**
     * prints an expression list statement
     */
    public void visitExpressionListStatement(JExpressionListStatement self,
                                             JExpression[] expr) {
        for (int i = 0; i < expr.length; i++) {
            if (i != 0) {
                print(", ");
            }
            expr[i].accept(this);
        }
        print(";");
    }

    /**
     * prints a empty statement
     */
    public void visitEmptyStatement(JEmptyStatement self) {
        //if we are inside a for loop header, we need to print 
        //the ; of an empty statement
        if (forLoopHeader > 0) {
            newLine();
            print(";");
        }
    }

    /**
     * prints a do statement
     */
    public void visitDoStatement(JDoStatement self,
                                 JExpression cond,
                                 JStatement body) {
        newLine();
        print("do ");
        body.accept(this);
        print("");
        print("while (");
        cond.accept(this);
        print(");");
    }

    /**
     * prints a continue statement
     */
    public void visitContinueStatement(JContinueStatement self,
                                       String label) {
        newLine();
        print("continue");
        if (label != null) {
            print(" " + label);
        }
        print(";");
    }

    /**
     * prints a break statement
     */
    public void visitBreakStatement(JBreakStatement self,
                                    String label) {
        newLine();
        print("break");
        if (label != null) {
            print(" " + label);
        }
        print(";");
    }

    /**
     * prints an expression statement
     */
    public void visitBlockStatement(JBlock self,
                                    JavaStyleComment[] comments) {
        print("{");
        pos += TAB_SIZE;
        visitCompoundStatement(self.getStatementArray());
        if (comments != null) {
            visitComments(comments);
        }
        pos -= TAB_SIZE;
        newLine();
        print("}");
    }

    /**
     * prints a type declaration statement
     */
    public void visitTypeDeclarationStatement(JTypeDeclarationStatement self,
                                              JTypeDeclaration decl) {
        decl.accept(this);
    }

    // ----------------------------------------------------------------------
    // EXPRESSION
    // ----------------------------------------------------------------------

    /**
     * prints an unary plus expression
     */
    public void visitUnaryPlusExpression(JUnaryExpression self,
                                         JExpression expr)
    {
        print("(");
        print("+");
        expr.accept(this);
        print(")");
    }

    /**
     * prints an unary minus expression
     */
    public void visitUnaryMinusExpression(JUnaryExpression self,
                                          JExpression expr)
    {
        print("(");
        print("-");
        expr.accept(this);
        print(")");
    }

    /**
     * prints a bitwise complement expression
     */
    public void visitBitwiseComplementExpression(JUnaryExpression self,
                                                 JExpression expr)
    {
        print("(");
        print("~");
        expr.accept(this);
        print(")");
    }

    /**
     * prints a logical complement expression
     */
    public void visitLogicalComplementExpression(JUnaryExpression self,
                                                 JExpression expr)
    {
        print("(");
        print("!");
        expr.accept(this);
        print(")");
    }

    /**
     * prints a type name expression
     */
    public void visitTypeNameExpression(JTypeNameExpression self,
                                        CType type) {
        print("(");
        print(type);
        print(")");
    }

    /**
     * prints a this expression
     */
    public void visitThisExpression(JThisExpression self,
                                    JExpression prefix) {
        //Utils.fail("This Expression encountered");
    }

    /**
     * prints a super expression
     */
    public void visitSuperExpression(JSuperExpression self) {
        Utils.fail("Super Expression Encountered");
    }

    /**
     * prints a shift expression
     */
    public void visitShiftExpression(JShiftExpression self,
                                     int oper,
                                     JExpression left,
                                     JExpression right) {
        print("(");
        left.accept(this);
        if (oper == OPE_SL) {
            print(" << ");
        } else if (oper == OPE_SR) {
            print(" >> ");
        } else {
            print(" >>> ");
        }
        right.accept(this);
        print(")");
    }

    /**
     * prints a shift expressiona
     */
    public void visitRelationalExpression(JRelationalExpression self,
                                          int oper,
                                          JExpression left,
                                          JExpression right) {
        print("(");
        left.accept(this);
        switch (oper) {
        case OPE_LT:
            print(" < ");
            break;
        case OPE_LE:
            print(" <= ");
            break;
        case OPE_GT:
            print(" > ");
            break;
        case OPE_GE:
            print(" >= ");
            break;
        default:
            Utils.fail("Unknown relational expression");
        }
        right.accept(this);
        print(")");
    }

    /**
     * prints a prefix expression
     */
    public void visitPrefixExpression(JPrefixExpression self,
                                      int oper,
                                      JExpression expr) {
        print("(");
        if (oper == OPE_PREINC) {
            print("++");
        } else {
            print("--");
        }
        expr.accept(this);
        print(")");
    }

    /**
     * prints a postfix expression
     */
    public void visitPostfixExpression(JPostfixExpression self,
                                       int oper,
                                       JExpression expr) {
        print("(");
        expr.accept(this);
        if (oper == OPE_POSTINC) {
            print("++");
        } else {
            print("--");
        }
        print(")");
    }

    /**
     * prints a parenthesed expression
     */
    public void visitParenthesedExpression(JParenthesedExpression self,
                                           JExpression expr) {
        print("(");
        expr.accept(this);
        print(")");
    }



    /**
     * prints an array allocator expression
     */
    public void visitNewArrayExpression(JNewArrayExpression self,
                                        CType type,
                                        JExpression[] dims,
                                        JArrayInitializer init)
    {
        /*print("(" + type + "*) calloc(");
          dims[0].accept(this);
          print(" , sizeof(");
          print(type);
          print("))");
          if (init != null) {
          init.accept(this);
          }*/
    
        // only print message parameter 
        if (PRINT_MSG_PARAM > -1) {
            if (init != null) {
                init.accept(this);
            }
            return;
        }

        print("("+ type);
        for (int y=0; y<dims.length;y++) {print("*");}
        print(")calloc(");
        dims[0].accept(this);
        print(", sizeof(");
        print(type);
        if(dims.length>1)
            print("*");
        print("))");
        if(dims.length>1) {
            for(int off=0;off<(dims.length-1);off++) {
                //Right now only handles JIntLiteral dims
                //If cast expression then probably a failure to reduce
                int num=((JIntLiteral)dims[off]).intValue();
                for(int i=0;i<num;i++) {
                    print(",\n");
                    //If lastLeft null then didn't come right after an assignment
                    lastLeft.accept(this);
                    print("["+i+"]=(");
                    print(type);
                    for (int y=0; y<(dims.length-1-off);y++) {print("*");}
                    print(")calloc(");
                    dims[off+1].accept(this);
                    print(", sizeof(");
                    print(type);
                    if(off<(dims.length-2))
                        print("*");
                    print("))");
                }
            }
        }
        if (init != null) {
            init.accept(this);
        }
    }

    /**
     * prints a name expression
     */
    public void visitNameExpression(JNameExpression self,
                                    JExpression prefix,
                                    String ident) {
        Utils.fail("Name Expression");
    
        print("(");
        if (prefix != null) {
            prefix.accept(this);
            print("->");
        }
        print(ident);
        print(")");
    }

    /**
     * prints an array allocator expression
     */
    public void visitBinaryExpression(JBinaryExpression self,
                                      String oper,
                                      JExpression left,
                                      JExpression right) {
        print("(");
        left.accept(this);
        print(" ");
        print(oper);
        print(" ");
        right.accept(this);
        print(")");
    }

    /**
     * prints a method call expression
     */
    public void visitMethodCallExpression(JMethodCallExpression self,
                                          JExpression prefix,
                                          String ident,
                                          JExpression[] args) {

        if (PRINT_MSG_PARAM > -1) {
            visitArgs(args, 0);
            return;
        }

        /*
          if (ident != null && ident.equals(JAV_INIT)) {
          return; // we do not want generated methods in source code
          }
        */

        //generate the inline asm instruction to execute the 
        //receive if this is a receive instruction
//        if (ident.equals(CommonConstants.receiveMethod)) {
//
//            //Do not generate this!
//        
//            /*
//              print(Util.staticNetworkReceivePrefix());
//              visitArgs(args,0);
//              print(Util.staticNetworkReceiveSuffix(args[0].getType()));
//            */
//
//
//            return;  
//        }
    
        if (prefix instanceof JTypeNameExpression) {
            JTypeNameExpression nexp = (JTypeNameExpression)prefix;
            String name = nexp.getType().toString();
            if (!name.equals("java.lang.Math")) print(name+"_");
        }

        print(ident);

        /*
          if (!Utils.isMathMethod(prefix, ident) && 
          ident.indexOf("::")==-1) {
          // don't rename the built-in math functions
          // don't rename calls to static functions
        
          if (method_names.contains(ident)) print("__"+selfID);
          }
        */

        print("(");
    
        //if this method we are calling is the call to a structure 
        //receive method that takes a pointer, we have to add the 
        //address of operator
        if (ident.startsWith(CommonConstants.structReceiveMethodPrefix))
            print("&");

        int i = 0;
        /* Ignore prefix, since it's just going to be a Java class name.
           if (prefix != null) {
           prefix.accept(this);
           i++;
           }
        */
        visitArgs(args, i);
        print(")");
    }

    /**
     * prints a local variable expression
     */
    public void visitLocalVariableExpression(JLocalVariableExpression self,
                                             String ident) {
        print(ident);
    }

    /**
     * prints an equality expression
     */
    public void visitEqualityExpression(JEqualityExpression self,
                                        boolean equal,
                                        JExpression left,
                                        JExpression right) {
        print("(");
        left.accept(this);
        print(equal ? " == " : " != ");
        right.accept(this);
        print(")");
    }

    /**
     * prints a conditional expression
     */
    public void visitConditionalExpression(JConditionalExpression self,
                                           JExpression cond,
                                           JExpression left,
                                           JExpression right) {
        print("(");
        cond.accept(this);
        print(" ? ");
        left.accept(this);
        print(" : ");
        right.accept(this);
        print(")");
    }

    /**
     * prints a compound expression
     */
    public void visitCompoundAssignmentExpression(JCompoundAssignmentExpression self,
                                                  int oper,
                                                  JExpression left,
                                                  JExpression right) {
        print("(");
        left.accept(this);
        switch (oper) {
        case OPE_STAR:
            print(" *= ");
            break;
        case OPE_SLASH:
            print(" /= ");
            break;
        case OPE_PERCENT:
            print(" %= ");
            break;
        case OPE_PLUS:
            print(" += ");
            break;
        case OPE_MINUS:
            print(" -= ");
            break;
        case OPE_SL:
            print(" <<= ");
            break;
        case OPE_SR:
            print(" >>= ");
            break;
        case OPE_BSR:
            print(" >>>= ");
            break;
        case OPE_BAND:
            print(" &= ");
            break;
        case OPE_BXOR:
            print(" ^= ");
            break;
        case OPE_BOR:
            print(" |= ");
            break;
        }
        right.accept(this);
        print(")");
    }

    /**
     * prints a field expression
     */
    public void visitFieldExpression(JFieldAccessExpression self,
                                     JExpression left,
                                     String ident)
    {
        if (ident.equals(JAV_OUTER_THIS)) {// don't generate generated fields
            print(left.getType().getCClass().getOwner().getType() + "->this");
            return;
        }
        int     index = ident.indexOf("_$");
        if (index != -1) {
            print(ident.substring(0, index));      // local var
        } else {
            print("(");

            if (global) {
                if (left instanceof JThisExpression) {
                    print("__global__"+ident);
                } else {
                    left.accept(this);
                    print(".");
                    print(ident);
                }
            } else if (left instanceof JThisExpression) {
                print(ident+"__"+selfID);
            } else if (left instanceof JTypeNameExpression &&
                       ((JTypeNameExpression)left).getType().toString().equals("TheGlobal")) {
                print("__global__");
                print(ident);
            } else {
                left.accept(this);
                print(".");
                print(ident);
            }

            print(")");
        }
    }

    /**
     * prints a cast expression
     */
    public void visitUnaryPromoteExpression(JUnaryPromote self,
                                            JExpression expr,
                                            CType type)
    {
        print("(");
        print("(");
        print(type);
        print(")");
        print("(");
        expr.accept(this);
        print(")");
        print(")");
    }

    /**
     * prints a compound assignment expression
     */
    public void visitBitwiseExpression(JBitwiseExpression self,
                                       int oper,
                                       JExpression left,
                                       JExpression right) {
        print("(");
        left.accept(this);
        switch (oper) {
        case OPE_BAND:
            print(" & ");
            break;
        case OPE_BOR:
            print(" | ");
            break;
        case OPE_BXOR:
            print(" ^ ");
            break;
        default:
            Utils.fail("Unknown relational expression");
        }
        right.accept(this);
        print(")");
    }

    /**
     * prints an assignment expression
     */
    public void visitAssignmentExpression(JAssignmentExpression self,
                                          JExpression left,
                                          JExpression right) {

        if (left.getType() != null && left.getType().toString().endsWith("Portal")) {
            print("/* void */");
            return;
        }

        //print the correct code for array assignment
        //this must be run after renaming!!!!!!
        if (left.getType() == null || right.getType() == null) {
            lastLeft=left;
            print("(");
            left.accept(this);
            print(" = ");
            right.accept(this);
            print(")");
            return;
        }
    
        // copy arrays element-wise
        boolean arrayType = ((left.getType()!=null && left.getType().isArrayType()) ||
                             (right.getType()!=null && right.getType().isArrayType()));
        if (arrayType && !(right instanceof JNewArrayExpression)) {
        
            CArrayType type = (CArrayType)right.getType();
            String dims[] = this.makeArrayStrings(((CArrayType)type).getDims());

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

            print("{\n");
            print("int ");
            //print the index var decls
            for (int i = 0; i < dims.length -1; i++)
                print(CommonConstants.ARRAY_COPY + i + ", ");
            print(CommonConstants.ARRAY_COPY + (dims.length - 1));
            print(";\n");
            for (int i = 0; i < dims.length; i++) {
                print("for (" + CommonConstants.ARRAY_COPY + i + " = 0; " + CommonConstants.ARRAY_COPY + i +  
                      " < " + dims[i] + "; " + CommonConstants.ARRAY_COPY + i + "++)\n");
            }
            left.accept(this);
            for (int i = 0; i < dims.length; i++)
                print("[" + CommonConstants.ARRAY_COPY + i + "]");
            print(" = ");
            right.accept(this);
            for (int i = 0; i < dims.length; i++)
                print("[" + CommonConstants.ARRAY_COPY + i + "]");
            print(";\n}\n");
            return;
        }

        //stack allocate all arrays in the work function 
        //done at the variable definition,
        if (!isInit && right instanceof JNewArrayExpression &&
            (left instanceof JLocalVariableExpression)) {
            //      (((CArrayType)((JNewArrayExpression)right).getType()).getArrayBound() < 2)) {

            //get the basetype and print it 
            CType baseType = ((CArrayType)((JNewArrayExpression)right).getType()).getBaseType();
            print(baseType);
            print(" ");
            //print the identifier
        
            //Before the ISCA hack this was how we printed the var ident
            //      left.accept(this);
        
            String ident;
            ident = ((JLocalVariableExpression)left).getVariable().getIdent();


        
//            if (KjcOptions.ptraccess) {
//        
//                //HACK FOR THE ICSA PAPER, !!!!!!!!!!!!!!!!!!!!!!!!!!!!!
//                //turn all array access into access of a pointer pointing to the array
//                print(ident + "_Alloc");
//        
//                //print the dims of the array
//                printDecl(left.getType(), ident);
//
//                //print the pointer def and the assignment to the array
//                print(";\n");
//                print(baseType + " *" + ident + " = " + ident + "_Alloc");
//            }
//            else {
//                //the way it used to be before the hack
                left.accept(this);
                //print the dims of the array
                printDecl(left.getType(), ident);
//            }
            return;
        }
           

        // do not initialize class variables

        if (right instanceof JUnqualifiedInstanceCreation) return;

    
        lastLeft=left;
        print("(");
        left.accept(this);
        print(" = ");

        right.accept(this);
        print(")");


        print("/*"+left.getType()+"*/");
    }

    /**
     * prints an array length expression
     */
    public void visitArrayLengthExpression(JArrayLengthExpression self,
                                           JExpression prefix) {
        Utils.fail("Array length expression not supported in streamit");
    
        prefix.accept(this);
        print(".length");
    }

    /**
     * prints an array length expression
     */
    public void visitArrayAccessExpression(JArrayAccessExpression self,
                                           JExpression prefix,
                                           JExpression accessor) {
        print("(");
        prefix.accept(this);
        print("[(int)");
        accessor.accept(this);
        print("]");
        print(")");
    }


    // ----------------------------------------------------------------------
    // STREAMIT IR HANDLERS
    // ----------------------------------------------------------------------

    public void visitCreatePortalExpression(SIRCreatePortal self) {
        print("create_portal()");
    }

    public void visitInitStatement(SIRInitStatement self,
                                   SIRStream stream)
    {
        print("/* InitStatement */");
    }

    public void visitInterfaceTable(SIRInterfaceTable self)
    {
        String iname = self.getIface().getIdent();
        JMethodDeclaration[] methods = self.getMethods();
        boolean first = true;
        
        print("{ ");
        for (int i = 0; i < methods.length; i++)
            {
                if (!first) print(", ");
                first = false;
                print(iname + "_" + methods[i].getName());
            }
        print("}");
    }
    
    public void visitLatency(SIRLatency self)
    {
        print("LATENCY_BEST_EFFORT");
    }
    
    public void visitLatencyMax(SIRLatencyMax self)
    {
        print("LATENCY_BEST_EFFORT");
    }
    
    public void visitLatencyRange(SIRLatencyRange self)
    {
        print("LATENCY_BEST_EFFORT");
    }
    
    public void visitLatencySet(SIRLatencySet self)
    {
        print("LATENCY_BEST_EFFORT");
    }

    public void visitMessageStatement(SIRMessageStatement self,
                                      JExpression portal,
                                      String iname,
                                      String __ident,
                                      JExpression[] params,
                                      SIRLatency latency)
    {
        /*
          print("//send_" + iname + "_" + ident + "(");
          portal.accept(this);
          print(", ");
          latency.accept(this);
          if (params != null)
          for (int i = 0; i < params.length; i++)
          if (params[i] != null)
          {
          print(", ");
          params[i].accept(this);
          }
          print(");");
        */

        String ident;
        int num_params = 0;

        if (portal instanceof SIRPortal) {
            SIRStream receivers[] = ((SIRPortal)portal).getReceivers();
            for (int i = 0; i < receivers.length; i++) {
                int dst = NodeEnumerator.getSIROperatorId(receivers[i]);

                System.out.println("/* iname: "+iname+" ident: "+self.getMessageName()+" type: "+((SIRPortal)portal).getPortalType().getCClass()+"*/\n");

                System.out.println("params size: "+params.length);

                ident = ((JStringLiteral)params[1]).stringValue();

                print("/*");

                for (int y = 0;; y++) {
                    // this causes only msg param to be output
                    PRINT_MSG_PARAM = y;
                    params[2].accept(this);
                    if (PRINT_MSG_PARAM == -1) break; // no more params!
                    num_params++;
                    print(",");
                }

                print(" num_params: "+num_params+"*/\n");

                /*
                  try {

                  print(((JStringLiteral)params[1]).stringValue());

                  // params[1].accept(this); // name of the portal method! 
            
                  print("\n");

                  for (int y = 0;; y++) {

                  // this causes only msg param to be output
                  PRINT_MSG_PARAM = y; 
                  params[2].accept(this);
                  if (PRINT_MSG_PARAM == -1) break; // no more params!
                  }

                  return;

                  } catch (Exception ex) {
                  ex.printStackTrace();
                  }
                */
        
                CClass pclass = ((SIRPortal)portal).getPortalType().getCClass();
                CMethod methods[] = pclass.getMethods();

                int index = -1;

                int size = 16; // size:4 mindex:4 s_iter:4 exec_at:4

                // int and float have size of 4 bytes
                size += num_params * 4;

                for (int t = 0; t < methods.length; t++) {

                    System.out.println("/* has method: "+methods[t]+" */\n");

                    if (methods[t].getIdent().equals(ident)) {
                        index = t;
                        break;
                    }
                }

                CType method_params[] = methods[index].getParameters();

                /*
                  if (params != null) {
                  // int and float have size of 4 bytes
                  size += params.length * 4; 
                  }
                */

        
                print("{ netsocket *sock = get_msg_out_socket("+dst+");\n");

                print("  sock->write_int("+size+");\n");
                print("  sock->write_int("+index+");\n");
                print("  sock->write_int(iteration);\n");

                if (latency instanceof SIRLatencyMax) {

                    int max = ((SIRLatencyMax)latency).getMax();

                    //print("__msg_sock_"+selfID+"_"+dst+"out->write_int("+max+");");

                    SIRFilter sender = (SIRFilter)NodeEnumerator.getOperator(selfID);
                    SIRFilter receiver = (SIRFilter)NodeEnumerator.getOperator(dst);

                    if (LatencyConstraints.isMessageDirectionDownstream(sender, receiver)) {
            
                        // subtract 1 at end in downstream direction
                        // because message is delivred BEFORE given iter
                        print("  sock->write_int(sdep_"+selfID+"_"+dst+"->getDstPhase4SrcPhase(iteration+"+max+"+1)-1);\n");

                    } else {

                        // do not subtract 1 at end in upstream
                        // direction because message is delivred
                        // AFTER given iter
                        print("  sock->write_int(sdep_"+selfID+"_"+dst+"->getSrcPhase4DstPhase(iteration+"+max+"+1));\n");
            
                    }

                } else {

                    print("  sock->write_int(-1);\n");
                }
        
                if (params != null) {
                    for (int t = 0; t < method_params.length; t++) {

                        if (method_params[t].toString().equals("int")) {
                            print("  sock->write_int(");
                        }
                        if (method_params[t].toString().equals("float")) {
                            print("  sock->write_float(");
                        }

                        // print out the parameter!
                        PRINT_MSG_PARAM = t;
                        params[2].accept(this);
                        PRINT_MSG_PARAM = -1;

                        print(");\n");
                    }
                }
        
                print("}\n");
            }
        }
    }

    public void visitDynamicToken(SIRDynamicToken self) {
        Utils.fail("Dynamic rates not yet supported in cluster backend.");
    }

    public void visitRangeExpression(SIRRangeExpression self) {
        Utils.fail("Dynamic rates not yet supported in cluster backend.");
    }

    public void visitPeekExpression(SIRPeekExpression self,
                                    CType tapeType,
                                    JExpression num)
    {

        //NetStream in = RegisterStreams.getFilterInStream(filter);
        //print(in.consumer_name()+".peek(");
        print("peek(");
        num.accept(this);
        print(")");

        //Utils.fail("FlatIRToCluster2 should see no peek expressions");
    }
    
    public void visitPopExpression(SIRPopExpression self,
                                   CType tapeType)
    {
        assert self.getNumPop() == 1: "Need support here for multiple pop";
        //NetStream in = RegisterStreams.getFilterInStream(filter);
        //print("get_consumer("+in.getSource()+")->pop()");
        print("pop()");
        //Utils.fail("FlatIRToCluster2 should see no pop expressions");
    }
    
    public void visitPrintStatement(SIRPrintStatement self,
                                    JExpression exp)
    {
        CType type = null;
    
        try {
            type = exp.getType();
        }
        catch (Exception e) {
            System.err.println("Cannot get type for print statement");
            type = CStdType.Integer;
        }
        
        if (type.equals(CStdType.Boolean))
            {
                Utils.fail("Cannot print a boolean");
            }
        else if (type.equals(CStdType.Byte) ||
                 type.equals(CStdType.Integer) ||
                 type.equals(CStdType.Short))
            {

                print("printf(\"%d\\n\", "); 
                //print("gdn_send(" + INT_HEADER_WORD + ");\n");
                //print("gdn_send(");
                exp.accept(this);
                print(");");
            }
        else if (type.equals(CStdType.Char))
            {
                print("printf(\"%d\\n\", "); 
                //print("gdn_send(" + INT_HEADER_WORD + ");\n");
                //print("gdn_send(");
                exp.accept(this);
                print(");");
            }
        else if (type.equals(CStdType.Float))
            {
                print("printf(\"%f\\n\", "); 
                //print("gdn_send(" + FLOAT_HEADER_WORD + ");\n");
                //print("gdn_send(");
                exp.accept(this);
                print(");");
            }
        else if (type.equals(CStdType.Long))
            {
                print("printf(\"%d\\n\", "); 
                //      print("gdn_send(" + INT_HEADER_WORD + ");\n");
                //print("gdn_send(");
                exp.accept(this);
                print(");");
            }
        else
            {
                System.out.println("Unprintable type");
                print("print_int(");
                exp.accept(this);
                print(");");
                //Utils.fail("Unprintable Type");
            }
    }
    
    private void pushScalar(SIRPushExpression self,
                            CType tapeType,
                            JExpression val) 
    {
        //NetStream out = RegisterStreams.getFilterOutStream(filter);
        print("push(");
        val.accept(this);
        print(")");
    
        //print("__push__"+selfID+"(");
        //print(out.producer_name()+".push(");
        //print(Util.staticNetworkSendPrefix(tapeType));
        //print(Util.staticNetworkSendSuffix());

    }

    
    public void pushClass(SIRPushExpression self, 
                          CType tapeType,
                          JExpression val) 
    {

        //NetStream out = RegisterStreams.getFilterOutStream(filter);
        print("push(");
        val.accept(this);
        print(")");

        //print("__push__"+selfID+"(");
        //turn the push statement into a call of
        //the structure's push method
        //print("push" + tapeType + "(&");
        //val.accept(this);
        //print(")");
    }
    

    private void pushArray(SIRPushExpression self, 
                           CType tapeType,
                           JExpression val) 
    {
        CType baseType = ((CArrayType)tapeType).getBaseType();
        String dims[] = this.makeArrayStrings(((CArrayType)tapeType).getDims());
    
        for (int i = 0; i < dims.length; i++) {
            print("for (" + CommonConstants.ARRAY_INDEX + i + " = 0; " +
                  CommonConstants.ARRAY_INDEX + i + " < " + dims[i] + " ; " +
                  CommonConstants.ARRAY_INDEX + i + "++)\n");
        }


// Not used or tested to my knowledge [AD]
//        if(KjcOptions.altcodegen || KjcOptions.decoupled) {
//            print("{\n");
//            //      print(Util.CSTOVAR + " = ");
//            val.accept(this);
//            for (int i = 0; i < dims.length; i++) {
//                print("[" + CommonConstants.ARRAY_INDEX + i + "]");
//            }
//            print(";\n}\n");
//        } else {
            print("{");
            print("static_send((" + baseType + ") ");
            val.accept(this);
            for (int i = 0; i < dims.length; i++) {
                print("[" + CommonConstants.ARRAY_INDEX + i + "]");
            }
            print(");\n}\n");
//        }
    }
    
    public void visitPushExpression(SIRPushExpression self,
                                    CType tapeType,
                                    JExpression val)
    {
        if (tapeType.isArrayType())
            pushArray(self, tapeType, val);
        else if (tapeType.isClassType())
            pushClass(self, tapeType, val);
        else 
            pushScalar(self, tapeType, val);
    }
    
    public void visitRegReceiverStatement(SIRRegReceiverStatement self,
                                          JExpression portal,
                                          SIRStream receiver, 
                                          JMethodDeclaration[] methods)
    {
        print("register_receiver(");
        portal.accept(this);
        print(", data->context, ");
        print(self.getItable().getVarDecl().getIdent());
        print(", LATENCY_BEST_EFFORT);");
        // (But shouldn't there be a latency field in here?)
    }
    
    public void visitRegSenderStatement(SIRRegSenderStatement self,
                                        String fn,
                                        SIRLatency latency)
    {
        print("register_sender(this->context, ");
        print(fn);
        print(", ");
        latency.accept(this);
        print(");");
    }


    // ----------------------------------------------------------------------
    // UTILS
    // ----------------------------------------------------------------------

    /**
     * prints an array length expression
     */
    public void visitSwitchLabel(JSwitchLabel self,
                                 JExpression expr) {
        newLine();
        if (expr != null) {
            print("case ");
            expr.accept(this);
            print(": ");
        } else {
            print("default: ");
        }
    }

    /**
     * prints an array length expression
     */
    public void visitSwitchGroup(JSwitchGroup self,
                                 JSwitchLabel[] labels,
                                 JStatement[] stmts) {
        for (int i = 0; i < labels.length; i++) {
            labels[i].accept(this);
        }
        pos += TAB_SIZE;
        for (int i = 0; i < stmts.length; i++) {
            newLine();
            stmts[i].accept(this);
        }
        pos -= TAB_SIZE;
    }

    /**
     * prints a boolean literal
     */
    public void visitBooleanLiteral(boolean value) {
        if (value)
            print(1);
        else
            print(0);
    }

    /**
     * prints a byte literal
     */
    public void visitByteLiteral(byte value) {
        print("((byte)" + value + ")");
    }

    /**
     * prints a character literal
     */
    public void visitCharLiteral(char value) {
        switch (value) {
        case '\b':
            print("'\\b'");
            break;
        case '\r':
            print("'\\r'");
            break;
        case '\t':
            print("'\\t'");
            break;
        case '\n':
            print("'\\n'");
            break;
        case '\f':
            print("'\\f'");
            break;
        case '\\':
            print("'\\\\'");
            break;
        case '\'':
            print("'\\''");
            break;
        case '\"':
            print("'\\\"'");
            break;
        default:
            print("'" + value + "'");
        }
    }

    /**
     * prints a double literal
     */
    public void visitDoubleLiteral(double value) {
        print("((float)" + value + ")");
    }

    /**
     * prints a float literal
     */
    public void visitFloatLiteral(float value) {
        print("((float)" + value + ")");
    }

    /**
     * prints a int literal
     */
    public void visitIntLiteral(int value) {
        print(value);
    }

    /**
     * prints a long literal
     */
    public void visitLongLiteral(long value) {
        print("(" + value + "L)");
    }

    /**
     * prints a short literal
     */
    public void visitShortLiteral(short value) {
        print("((short)" + value + ")");
    }

    /**
     * prints a string literal
     */
    public void visitStringLiteral(String value) {
        print('"' + value + '"');
    }

    /**
     * prints a null literal
     */
    public void visitNullLiteral() {
        print("null");
    }

    /**
     * prints an array length expression
     */
    public void visitFormalParameters(JFormalParameter self,
                                      boolean isFinal,
                                      CType type,
                                      String ident) {
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

    /**
     * prints an array length expression
     */
    public void visitArgs(JExpression[] args, int base) {
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                if (i + base != 0) {
                    print(", ");
                }
                args[i].accept(this);
            }
        }
    }

    /**
     * prints an array length expression
     */
    public void visitConstructorCall(JConstructorCall self,
                                     boolean functorIsThis,
                                     JExpression[] params)
    {
        newLine();
        print(functorIsThis ? "this" : "super");
        print("(");
        visitArgs(params, 0);
        print(");");
    }

    /**
     * prints an array initializer expression
     */
    public void visitArrayInitializer(JArrayInitializer self,
                                      JExpression[] elems)
    {

        // only print message param
        if (PRINT_MSG_PARAM > -1) {
            if (PRINT_MSG_PARAM < elems.length) {
                elems[PRINT_MSG_PARAM].accept(this);
            } else {
                PRINT_MSG_PARAM = -1;
            }
            return;
        }

        newLine();
        print("{");
        for (int i = 0; i < elems.length; i++) {
            if (i != 0) {
                print(", ");
            }
            elems[i].accept(this);
        }
        print("}");
    }

    
    // ----------------------------------------------------------------------
    // PROTECTED METHODS
    // ----------------------------------------------------------------------

    protected void newLine() {
        p.println();
    }

    // Special case for CTypes, to map some Java types to C types.
    protected void print(CType s) {
        if (s instanceof CArrayType) {
            print(((CArrayType)s).getElementType());
            print("*");
        }
        else if (s.getTypeID() == TID_BOOLEAN)
            print("int");
        else if (s.toString().endsWith("Portal"))
            // ignore the specific type of portal in the C library
            print("portal");
        else
            print(s.toString());
    }

    protected void printLocalType(CType s) 
    {
        if (s instanceof CArrayType){
            print(((CArrayType)s).getElementType()+"*");
        }
        else if (s.getTypeID() == TID_BOOLEAN)
            print("int");
        else if (s.toString().endsWith("Portal"))
            // ignore the specific type of portal in the C library
            print("portal");
        else
            print(s.toString());
    }

    protected void print(Object s) {
        print(s.toString());
    }

    public void print(String s) {
        p.setPos(pos);
        p.print(s);
    }

    protected void print(boolean s) {
        print("" + s);
    }

    protected void print(int s) {
        print("" + s);
    }

    protected void print(char s) {
        print("" + s);
    }

    protected void print(double s) {
        print("" + s);
    }

    // ----------------------------------------------------------------------
    // UNUSED STREAM VISITORS
    // ----------------------------------------------------------------------

    /* pre-visit a pipeline */
    public void preVisitPipeline(SIRPipeline self,
                                 SIRPipelineIter iter) 
    {
    }
    

    /* pre-visit a splitjoin */
    public void preVisitSplitJoin(SIRSplitJoin self,
                                  SIRSplitJoinIter iter)
    {
    }
    

    /* pre-visit a feedbackloop */
    public void preVisitFeedbackLoop(SIRFeedbackLoop self,
                                     SIRFeedbackLoopIter iter)
    {
    }
    

    /**
     * POST-VISITS 
     */
        
    /* post-visit a pipeline */
    public void postVisitPipeline(SIRPipeline self,
                                  SIRPipelineIter iter) {
    }
    

    /* post-visit a splitjoin */
    public void postVisitSplitJoin(SIRSplitJoin self,
                                   SIRSplitJoinIter iter) {
    }
    

    /* post-visit a feedbackloop */
    public void postVisitFeedbackLoop(SIRFeedbackLoop self,
                                      SIRFeedbackLoopIter iter) {
    }

}
