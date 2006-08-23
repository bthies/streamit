// $Header: /afs/csail.mit.edu/group/commit/reps/projects/streamit/cvsroot/streams/src/at/dms/kjc/cluster/ClusterCodeGenerator.java,v 1.58 2006-08-23 19:24:59 dimock Exp $
package at.dms.kjc.cluster;

import java.util.*;

//import sun.security.krb5.internal.o;
import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.common.CodegenPrintWriter;

/**
 * A class that generates part of the threadX.cpp files that is shared
 * among splitters, joiners and filters.
 */

class ClusterCodeGenerator {

    private SIROperator oper;
    private JFieldDeclaration fields[];
    
    private int id;
    
    private List<Tape> data_in;
    private List<Tape> data_out;

    private Vector<SIRStream> msg_from;
    private Vector<SIRStream> msg_to;

    private boolean restrictedExecution;
    private int initCredit = 0;

    private boolean sendsCredits;
    private HashSet sendsCreditsTo;

    private FlatNode node;
    private boolean isEliminated; // true if eliminated by ClusterFusion
    private Set fusedWith;

    private int init_counts;
    private int steady_counts;
    private String work_function;

    /**
     * A constructor
     *
     * @param oper a {@link SIROperator}
     * @param fields an array of {@link JFieldDeclaration}s
     */

    public ClusterCodeGenerator(SIROperator oper, 
                                JFieldDeclaration fields[]) {
    
        this.oper = oper;
        this.fields = fields;

        id = NodeEnumerator.getSIROperatorId(oper);

        node = NodeEnumerator.getFlatNode(id);

        Integer init_int = (Integer)ClusterBackend.initExecutionCounts.get(node);
        if (init_int == null) { init_counts = 0; } else { init_counts = init_int.intValue(); }

        steady_counts = ((Integer)ClusterBackend.steadyExecutionCounts.get(node)).intValue();

        if (oper instanceof SIRFilter) {
            work_function = ClusterUtils.getWorkName(((SIRFilter)oper), id);
        } 

        if (oper instanceof SIRSplitter) {
            work_function = "__splitter_"+id+"_work";
        }

        if (oper instanceof SIRJoiner) {
            work_function = "__joiner_"+id+"_work";
        }

        isEliminated = ClusterFusion.isEliminated(node);
        fusedWith = ClusterFusion.fusedWith(node);

        data_in = RegisterStreams.getNodeInStreams(oper);
        data_out = RegisterStreams.getNodeOutStreams(oper);

        if (oper instanceof SIRStream) {
            SIRStream stream = (SIRStream)oper;
            if (stream.getInputType().toString().compareTo("void") == 0) 
                data_in = Collections.emptyList();
            if (stream.getOutputType().toString().compareTo("void") == 0) 
                data_out = Collections.emptyList();
        }

        msg_from = new Vector<SIRStream>();
        msg_to = new Vector<SIRStream>();

        restrictedExecution = false;
        sendsCreditsTo = new HashSet();
        sendsCredits = false;

        if (oper instanceof SIRFilter) {

            SIRFilter f = (SIRFilter)oper;

            restrictedExecution = LatencyConstraints.isRestricted(f); 
        
            /*
              if (restrictedExecution) {
              initCredit = LatencyConstraints.getInitCredit(f); 
              }
            */

            sendsCreditsTo = LatencyConstraints.getOutgoingConstraints(f);
            sendsCredits = (sendsCreditsTo.size() > 0);

            SIRPortal outgoing[] = SIRPortal.getPortalsWithSender(f);
            SIRPortal incoming[] = SIRPortal.getPortalsWithReceiver(f);

            for (int t = 0; t < outgoing.length; t++) {
                SIRStream[] receivers = outgoing[t].getReceivers();
                for (int i = 0; i < receivers.length; i++) {
                    if (!msg_to.contains(receivers[i])) {
                        msg_to.add(receivers[i]);
                    }
                }
            }

            for (int t = 0; t < incoming.length; t++) {
                SIRPortalSender[] senders = incoming[t].getSenders();
                for (int i = 0; i < senders.length; i++) {
                    if (!msg_from.contains(senders[i].getStream())) {
                        msg_from.add(senders[i].getStream());
                    }
                }
            }
        }
    }


    /**
     * Generates preamble of threadX.cpp file. This includes headers,
     * thread internal variables, communication variables, operator fields. 
     * And following functions: read/write thread, check thread status, 
     * init/get thread_info, declare_sockets, init_sockets, flush_sockets 
     * and peek_sockets.
     *
     * @param f2c a reference to {@link FlatIRToClutser} class
     * @param p print writer
     */

    public void generatePreamble(FlatIRToCluster f2c, CodegenPrintWriter p) {

        p.println("// ClusterFusion isEliminated: "+isEliminated+"");
        for (Iterator iter = fusedWith.iterator(); iter.hasNext();) {
            p.println("// Fused with: "+iter.next()+"");
        }

        p.println("");

        p.println("#include <stdlib.h>");
        p.println("#include <unistd.h>");
        p.println("#include <math.h>"); 
        p.println("");
        p.println("#include <init_instance.h>");
        p.println("#include <mysocket.h>");
        p.println("#include <object_write_buffer.h>");
        p.println("#include <save_state.h>");
        p.println("#include <sdep.h>");
        p.println("#include <message.h>");
        p.println("#include <timer.h>");
        p.println("#include <thread_info.h>");
        p.println("#include <consumer2.h>");          // for cluster edge incoming
        p.println("#include <consumer2p.h>");         // for cluster feedback edge incoming with >0 enqueues
        p.println("#include <producer2.h>");          // foe cluster edge outgoing
        // only include fft.h if we did a frequency transformation
        if (at.dms.kjc.sir.linear.frequency.LEETFrequencyReplacer.didTransform) {
            p.println("#include <fft.h>");
        }
        p.println("#include \"cluster.h\"");
        p.println("#include \"fusion.h\"");
        //p.println("#include \"structs.h\"");
        p.println("#include \"global.h\"");
        if (KjcOptions.countops) {
            p.println("#include \"profiler.h\"");
        }
        p.println("");

        //p.println("#include <peek_stream.h>");
        //p.println("#include <data_consumer.h>");
        //p.println("#include <data_producer.h>");
    
        p.println("extern int __max_iteration;");
        p.println("extern int __init_iter;");
        // any Operator with no successor is assumed to be last and will print timer
        boolean timer_print = true;
        for (Tape succ : data_out) { 
            if (succ != null) {
                timer_print = true;
                break;
            }
        }
        if (timer_print) {
            p.println("extern int __timer_enabled;");
        }
        p.println("extern int __frequency_of_chkpts;");
        p.println("extern volatile int __vol;");
        p.println("message *__msg_stack_"+id+";");

        p.println("int __number_of_iterations_"+id+";");
        p.println("int __counter_"+id+" = 0;");
        p.println("int __steady_"+id+" = 0;");
        p.println("int __tmp_"+id+" = 0;");
        p.println("int __tmp2_"+id+" = 0;");
        p.println("int *__state_flag_"+id+" = NULL;");
        p.println("thread_info *__thread_"+id+" = NULL;");

        if (restrictedExecution) {
            p.println("int __credit_"+id+" = "+initCredit+";");
        }
    
        for (Iterator i = msg_to.iterator(); i.hasNext();) {
            SIRStream str = (SIRStream)i.next();
            p.println("sdep *sdep_"+id+"_"+NodeEnumerator.getSIROperatorId(str)+";");
        }

        p.println("");
    
        //  +=============================+
        //  | Communication Variables     |
        //  +=============================+

        for (Tape in : data_in) {
          if (in != null && in instanceof TapeCluster) {
            if (! FixedBufferTape.isFixedBuffer(in.getSource(),in.getDest())) {
                if (oper instanceof SIRJoiner
                        && oper.getParent() instanceof SIRFeedbackLoop
                        && ((SIRFeedbackLoop)oper.getParent()).getDelayInt() > 0
                        && NodeEnumerator.getFlatNode(in.getSource()) ==
                            node.incoming[1]) {
                    p.println("consumer2p<"+ClusterUtils.CTypeToString(in.getType())+"> "+((TapeCluster)in).getConsumerName()+";");
                } else {
                    p.println("consumer2<"+ClusterUtils.CTypeToString(in.getType())+"> "+((TapeCluster)in).getConsumerName()+";");
                }
                p.println("extern "+ClusterUtils.CTypeToString(in.getType())+" "+((TapeCluster)in).getPopName()+"();");
            /*
              if (oper instanceof SIRFilter) {
              String type = ((SIRFilter)oper).getInputType().toString();
              p.println("peek_stream<"+type+"> "+in.name()+"in(&"+in.getConsumerName()+");");
              }
            */
            }
          }
        }
    
        for (Tape out : data_out) {
          if (out != null && out instanceof TapeCluster) {
            if (! FixedBufferTape.isFixedBuffer(out.getSource(),out.getDest())) {
                p.println("producer2<"+ClusterUtils.CTypeToString(out.getType())+"> "+((TapeCluster)out).getProducerName()+";");
                p.println("extern void "+((TapeCluster)out).getPushName()+"("+ClusterUtils.CTypeToString(out.getType())+");");
                p.println("    // this-part:"+ClusterFusion.getPartition(node)+" dst-part:"+ClusterFusion.getPartition(NodeEnumerator.getFlatNode(out.getDest()))+"");
            }
          }
        }   
    
        for (Iterator i = msg_from.iterator(); i.hasNext();) {
            int src = NodeEnumerator.getSIROperatorId((SIRStream)i.next());
            p.println("netsocket *__msg_sock_"+src+"_"+id+"in;");   
        }

        for (Iterator i = msg_to.iterator(); i.hasNext();) {
            SIRStream str = (SIRStream)i.next();
            int dst = NodeEnumerator.getSIROperatorId(str);
            p.println("netsocket *__msg_sock_"+id+"_"+dst+"out; // to " + str);
        }
    
        p.println("");

        //  +=============================+
        //  | Fields                      |
        //  +=============================+

        for (int i = 0; i < fields.length; i++) {
            JFieldDeclaration f = fields[i];
            CType fType = f.getType();
        
            if (fType.toString().endsWith("Portal")) {
                continue;   // no declaration for fields that are portals.
            }
            JVariableDefinition v = f.getVariable();
            // Can't ask field to accept since munging identifier!
            f2c.visitFieldDeclaration (f,
                                       v.getModifiers(),
                                       fType,
                                       v.getIdent()+"__"+id,
                                       v.getValue());
        }

        p.println("");

        //  +=============================+
        //  | Read / Write Thread         |
        //  +=============================+

        if (node.isFilter()) {
            if (node.inputs > 0 && node.incoming[0] != null) {
                p.println("void save_peek_buffer__" + id
                        + "(object_write_buffer *buf);");
                p.println("void load_peek_buffer__" + id
                        + "(object_write_buffer *buf);");
            }
            p.println("void save_file_pointer__" + id
                    + "(object_write_buffer *buf);");
            p.println("void load_file_pointer__" + id
                    + "(object_write_buffer *buf);");
            p.println("");
        }
        
	if (! KjcOptions.standalone) {
	//p.println("#ifndef __CLUSTER_STANDALONE\n");
        p.println("void __write_thread__"+id+"(object_write_buffer *buf) {");

        for (Tape in : data_in) {
          if (in != null && in instanceof TapeCluster) {
              TapeCluster inc = (TapeCluster)in;
            p.println("  "+inc.getConsumerName()+".write_object(buf);");
        

            /*
              if (oper instanceof SIRFilter) {
              p.println("  "+in.name()+"in.write_object(buf);");
              }
            */
          }
        }

        if (node.isFilter()) {
                if (node.inputs > 0 && node.incoming[0] != null) {
                    p.println("  save_peek_buffer__" + id + "(buf);");
                }
                p.println("  save_file_pointer__" + id + "(buf);");
            }
        
        for (int f = 0; f < fields.length; f++) {
            CType type = fields[f].getType();
            String ident = fields[f].getVariable().getIdent();

            DetectConst dc = DetectConst.getInstance((SIRFilter)oper);
            if (dc != null && dc.isConst(ident)) continue;

            if (type.isArrayType()) {
                int size = 0;
                String dims[] = (new FlatIRToCluster()).makeArrayStrings(((CArrayType)type).getDims());
                CType base = ((CArrayType)type).getBaseType();
                try {
                    size = Integer.valueOf(dims[0]).intValue();
                } catch (NumberFormatException ex) {
                    System.out.println("Warning! Could not estimate size of an array: "+ident);
                }
                p.println("  buf->write("+ident+"__"+id+", "+size+" * sizeof("+ClusterUtils.CTypeToString(base)+"));");
            } else {
                p.println("  buf->write(&"+ident+"__"+id+", sizeof("+ClusterUtils.CTypeToString(type)+"));");
            }
        }

        for (Tape out : data_out) {
          if (out != null && out instanceof TapeCluster) {
            p.println("  "+((TapeCluster)out).getProducerName()+".write_object(buf);");
          }
        }

        p.println("}");

        p.println("");  

        p.println("void __read_thread__"+id+"(object_write_buffer *buf) {");

    
        for (Tape in : data_in) {
          if (in != null && in instanceof TapeCluster) {
            p.println("  "+((TapeCluster)in).getConsumerName()+".read_object(buf);");

            /*
              if (oper instanceof SIRFilter) {
              p.println("  "+in.name()+"in.read_object(buf);");
              }
            */
          }
        }
        
        if (node.isFilter()) {
            if (node.inputs > 0 && node.incoming[0] != null) {
                p.println("  load_peek_buffer__" + id + "(buf);");
            }
            p.println("  load_file_pointer__" + id + "(buf);");
        }

        for (int f = 0; f < fields.length; f++) {
            CType type = fields[f].getType();
            String ident = fields[f].getVariable().getIdent();

            DetectConst dc = DetectConst.getInstance((SIRFilter)oper);
            if (dc != null && dc.isConst(ident)) continue;

            if (type.isArrayType()) {
                int size = 0;
                String dims[] = (new FlatIRToCluster()).makeArrayStrings(((CArrayType)type).getDims());
                CType base = ((CArrayType)type).getBaseType();
                try {
                    size = Integer.valueOf(dims[0]).intValue();
                } catch (NumberFormatException ex) {
                    System.out.println("Warning! Could not estimate size of an array: "+ident);
                }
                p.println("  buf->read("+ident+"__"+id+", "+size+" *  sizeof("+ClusterUtils.CTypeToString(base)+"));");
            } else {
                p.println("  buf->read(&"+ident+"__"+id+", sizeof("+ClusterUtils.CTypeToString(type)+"));");
            }
        }

        for (Tape out : data_out) {
          if (out != null && out instanceof TapeCluster) {
            p.println("  "+((TapeCluster)out).getProducerName()+".read_object(buf);");
          }
        }

        p.println("}");

        p.println("");  

        //  +=============================+
        //  | Check Thread Status         |
        //  +=============================+
    
        p.println("inline void check_status__"+id+"() {");
        p.println("  check_thread_status(__state_flag_"+id+", __thread_"+id+");");
        p.println("}");

        p.println("");  

        p.println("void check_status_during_io__"+id+"() {");
        p.println("  check_thread_status_during_io(__state_flag_"+id+", __thread_"+id+");");
        p.println("}");

        p.println("");  

        //  +=============================+
        //  | Fused Methods               |
        //  +=============================+

        if (!isEliminated) {
            Iterator iter2 = fusedWith.iterator();
            while (iter2.hasNext()) {
                FlatNode tmp = (FlatNode)iter2.next();
                int fid = NodeEnumerator.getFlatNodeId(tmp);
                p.println("extern void __declare_sockets_"+fid+"();");
                p.println("extern void __init_sockets_"+fid+"(void (*cs_fptr)());");
                p.println("extern void __flush_sockets_"+fid+"();");
                p.println("extern void __peek_sockets_"+fid+"();");
                p.println("extern void __init_thread_info_"+fid+"(thread_info *);");
                p.println("");
            }
        }

        //  +=============================+
        //  | Thread Info                 |
        //  +=============================+

        p.println("void __init_thread_info_"+id+"(thread_info *info) {");

        for (Tape in : data_in) {
          if (in != null && in instanceof TapeCluster) {
            p.println("  info->add_incoming_data_connection(new connection_info("+in.getSource()+","+in.getDest()+",&"+((TapeCluster)in).getConsumerName()+"));");
          }
        }
    
        for(Tape out : data_out) {
          if (out != null && out instanceof TapeCluster) {
            p.println("  info->add_outgoing_data_connection(new connection_info("+out.getSource()+","+out.getDest()+",&"+((TapeCluster)out).getProducerName()+"));");
          }
        }

        p.println("  __state_flag_"+id+" = info->get_state_flag();");

        if (!isEliminated) {
            Iterator _i = fusedWith.iterator();
            while (_i.hasNext()) {
                FlatNode tmp = (FlatNode)_i.next();
                int fid = NodeEnumerator.getFlatNodeId(tmp);
                p.println("  __init_thread_info_"+fid+"(info);");
            }
        }

        p.println("}");
        p.println("");

        p.println("thread_info *__get_thread_info_"+id+"() {");

        p.println("  if (__thread_"+id+" != NULL) return __thread_"+id+";");
        p.println("  __thread_"+id+" = new thread_info("+id+", check_status_during_io__"+id+");");
        p.println("  __init_thread_info_"+id+"(__thread_"+id+");");
        p.println("  return __thread_"+id+";");
    
        p.println("}");
        p.println("");

        //  +=============================+
        //  | Declare Sockets             |
        //  +=============================+

        p.println("void __declare_sockets_"+id+"() {");

        if (!isEliminated) {
            Iterator iter2 = fusedWith.iterator();
            while (iter2.hasNext()) {
                FlatNode tmp = (FlatNode)iter2.next();
                int fid = NodeEnumerator.getFlatNodeId(tmp);
                p.println("  __declare_sockets_"+fid+"();");
            }
        }

        for (Tape in : data_in) {
          if (in != null) {
            FlatNode tmp = NodeEnumerator.getFlatNode(in.getSource());
            if (!fusedWith.contains(tmp)) { 
                p.println("  init_instance::add_incoming("+in.getSource()+","+in.getDest()+", DATA_SOCKET);");
            }
          }
        }

        for (Tape out : data_out) {
          if (out != null) {
            FlatNode tmp = NodeEnumerator.getFlatNode(out.getDest());
            if (!fusedWith.contains(tmp)) {     
                p.println("  init_instance::add_outgoing("+out.getSource()+","+out.getDest()+", DATA_SOCKET);");
            }
          } 
        }

        for (Iterator i = msg_from.iterator(); i.hasNext();) {
            int src = NodeEnumerator.getSIROperatorId((SIRStream)i.next());
            p.println("  init_instance::add_incoming("+src+","+id+",MESSAGE_SOCKET);");
        }

        for(Iterator i = msg_to.iterator(); i.hasNext();) {
            int dst = NodeEnumerator.getSIROperatorId((SIRStream)i.next());
            p.println("  init_instance::add_outgoing("+id+","+dst+",MESSAGE_SOCKET);");
        }

        p.println("}");
        p.println("");

        //  +=============================+
        //  | Init Sockets                |
        //  +=============================+

        p.println("void __init_sockets_"+id+"(void (*cs_fptr)()) {");

        p.println("  mysocket *sock;");
        p.println("");

        if (!isEliminated) {
            Iterator iter2 = fusedWith.iterator();
            while (iter2.hasNext()) {
                FlatNode tmp = (FlatNode)iter2.next();
                int fid = NodeEnumerator.getFlatNodeId(tmp);
                p.println("  __init_sockets_"+fid+"(cs_fptr);");
            }
        }

        for (Tape in : data_in) {
          if (in != null && in instanceof TapeCluster) {
            FlatNode tmp = NodeEnumerator.getFlatNode(in.getSource());
            if (!fusedWith.contains(tmp)) {
                p.println("  sock = init_instance::get_incoming_socket("+in.getSource()+","+in.getDest()+",DATA_SOCKET);");
                p.println("  sock->set_check_thread_status(cs_fptr);");
                //p.println("  sock->set_item_size(sizeof("+in.getType()+"));");
                p.println("  "+((TapeCluster)in).getConsumerName()+".set_socket(sock);");
                p.println("  "+((TapeCluster)in).getConsumerName()+".init();");
                p.println("");
            }
          }
        }

        for (Tape out : data_out) {
          if (out != null && out instanceof TapeCluster) {
            FlatNode tmp = NodeEnumerator.getFlatNode(out.getDest());
            if (!fusedWith.contains(tmp)) {
                p.println("  sock = init_instance::get_outgoing_socket("+out.getSource()+","+out.getDest()+",DATA_SOCKET);");
                p.println("  sock->set_check_thread_status(cs_fptr);");
                //p.println("  sock->set_item_size(sizeof("+out.getType()+"));");
                p.println("  "+((TapeCluster)out).getProducerName()+".set_socket(sock);");
                p.println("  "+((TapeCluster)out).getProducerName()+".init();");
                p.println("");
            }
          }
        }

        for (Iterator i = msg_from.iterator(); i.hasNext();) {
            int src = NodeEnumerator.getSIROperatorId((SIRStream)i.next());
            p.println("  __msg_sock_"+src+"_"+id+"in = (netsocket*)init_instance::get_incoming_socket("+src+","+id+",MESSAGE_SOCKET);");
            p.println("");
        }

        for (Iterator i = msg_to.iterator(); i.hasNext();) {
            int dst = NodeEnumerator.getSIROperatorId((SIRStream)i.next());
            p.println("  __msg_sock_"+id+"_"+dst+"out = (netsocket*)init_instance::get_outgoing_socket("+id+","+dst+",MESSAGE_SOCKET);");
            p.println("");
        }

        p.println("}");
        p.println("");


        //  +=============================+
        //  | Flush Sockets               |
        //  +=============================+

        p.println("void __flush_sockets_"+id+"() {");

        if (!isEliminated) {
            Iterator iter2 = fusedWith.iterator();
            while (iter2.hasNext()) {
                FlatNode tmp = (FlatNode)iter2.next();
                int fid = NodeEnumerator.getFlatNodeId(tmp);
                p.println("  __flush_sockets_"+fid+"();");
            }
        }

        for(Tape out : data_out) {
          if (out != null && out instanceof TapeCluster) {
            FlatNode tmp = NodeEnumerator.getFlatNode(out.getDest());
            if (!fusedWith.contains(tmp)) {
                p.println("  "+((TapeCluster)out).getProducerName()+".flush();");
                p.println("  "+((TapeCluster)out).getProducerName()+".get_socket()->close();");
            }
          }
        }

        for(Tape out : data_out) {
          if (out != null && out instanceof TapeCluster) {
            FlatNode tmp = NodeEnumerator.getFlatNode(out.getDest());
            if (!fusedWith.contains(tmp)) {
                p.println("  "+((TapeCluster)out).getProducerName()+".delete_socket_obj();");
            }
          }
        }

        for (Tape in : data_in) {
          if (in != null && in instanceof TapeCluster) {
            FlatNode tmp = NodeEnumerator.getFlatNode(in.getSource());
            if (!fusedWith.contains(tmp)) {
                p.println("  "+((TapeCluster)in).getConsumerName()+".delete_socket_obj();");
            }
          }
        }

        p.println("}");
        p.println("");


        //  +=============================+
        //  | Peek Sockets                |
        //  +=============================+

        p.println("void __peek_sockets_"+id+"() {");

        if (!isEliminated) {
            Iterator iter2 = fusedWith.iterator();
            while (iter2.hasNext()) {
                FlatNode tmp = (FlatNode)iter2.next();
                int fid = NodeEnumerator.getFlatNodeId(tmp);
                p.println("  __peek_sockets_"+fid+"();");
            }
        }

        for (Tape in : data_in) {
          if (in != null && in instanceof TapeCluster) {
            FlatNode tmp = NodeEnumerator.getFlatNode(in.getSource());
            if (!fusedWith.contains(tmp)) {
                p.println("  "+((TapeCluster)in).getConsumerName()+".peek(0);");
            }
          }
        }

        p.println("}");

	//p.println("#endif // __CLUSTER_STANDALONE\n");

        p.println("");
	}
        return;
    }

    /**
     * Generates the end of threadX.cpp file. This includes functions:
     * init_state, main, init_sdep and run.
     *
     * @param init_f name of the init function
     * @param main_f not used!
     * @param cleanupCode list of strings representing cleanup code
     * @return a vector of strings representing the generated code
     */


    public Vector<String> generateRunFunction(String init_f, String main_f,
                                      List<String> cleanupCode) {
    
        Vector<String> r = new Vector<String>();

        r.add("\n");

        //  +=============================+
        //  | Init State                  |
        //  +=============================+

        if (!isEliminated) {
            Iterator iter2 = fusedWith.iterator();
            while (iter2.hasNext()) {
                FlatNode tmp = (FlatNode)iter2.next();
                int fid = NodeEnumerator.getFlatNodeId(tmp);
                r.add("extern void __init_state_"+fid+"();\n");
                r.add("\n");
            }
        }
        
	if (! KjcOptions.standalone) {
	//r.add("#ifndef __CLUSTER_STANDALONE\n");
        r.add("void __init_state_"+id+"() {\n");

        if (!isEliminated) {
            Iterator iter2 = fusedWith.iterator();
            while (iter2.hasNext()) {
                FlatNode tmp = (FlatNode)iter2.next();
                int fid = NodeEnumerator.getFlatNodeId(tmp);
                r.add("  __init_state_"+fid+"();\n");
            }
        }

        if (init_f != null) r.add("  "+init_f+"();\n");
        r.add("  if (save_state::load_state("+id+", &__steady_"+id+", __read_thread__"+id+") == -1) pthread_exit(NULL);\n");


        //r.add("  __number_of_iterations_"+id+" = __max_iteration - __steady_"+id+";\n");

        r.add("}\n");
	//r.add("#endif // __CLUSTER_STANDALONE\n");
        r.add("\n");
	}

        //  +=============================+
        //  | Main Function               |
        //  +=============================+
	if (! KjcOptions.standalone) {
        r.add("static void __main__"+id+"() {\n");
        r.add("  int _tmp; // modified\n");
        r.add("  int _steady = __steady_"+id+";\n");
        r.add("  int _number = __max_iteration;\n");
        r.add("\n");

     
        if (/*msg_to.size() > 0*/sendsCredits) {
            r.add("  send_credits__"+id+"();\n");
        }
        r.add("  if (_steady == 0) {\n");

        if (oper instanceof SIRJoiner && ClusterCode.feedbackJoineersNeedingPrep.contains(oper)) {
            r.add ("  __feedbackjoiner_"+ id +"_prep();\n");
        }

        if (oper instanceof SIRFilter && node.inputs > 0 && node.incoming[0] != null) {
            r.add("  __init_pop_buf__"+id+"();\n");
        }
    
        if (oper instanceof SIRTwoStageFilter) {
            r.add("    "+((SIRTwoStageFilter)oper).getInitWork().getName()+"__"+id+"();\n");
            init_counts--;
        }

        if (init_counts > 0) {
            r.add("    for (_tmp = 0; _tmp < "+init_counts+"; _tmp++) {\n");
            if (oper instanceof SIRFilter) {

		//r.add("      printf(\"thread"+id+" %d/"+init_counts+"\\n\", _tmp);\n");

                r.add("      //check_status__"+id+"();\n");

                r.add("      if (*__state_flag_"+id+" == EXIT_THREAD) exit_thread(__thread_"+id+");\n");

                if (msg_from.size() > 0) {
                    r.add("      check_messages__"+id+"();\n");
                }
                if (node.isFilter() && node.inputs > 0 && node.incoming[0] != null) {
                    r.add("      __update_pop_buf__"+id+"();\n");
                }
            }
            r.add("      "+work_function+"(1);\n");
            if (oper instanceof SIRFilter) {
		/*
                if (msg_to.size() > 0 || msg_from.size() > 0) {
                    r.add("      __counter_"+id+"++;"); 
                }
		*/
                if (/*msg_to.size() > 0*/sendsCredits) {
                    r.add("      send_credits__"+id+"();\n");
                }
            }
            r.add("    }\n");
        }

        r.add("  }\n");

        r.add("  _steady++;\n");
        //r.add("// ClusterCodeGenerator_2\n");
        r.add("  for (; _steady <= _number; _steady++) {\n");

        if (steady_counts > 1) {
            r.add("    for (_tmp = 0; _tmp < "+steady_counts+"; _tmp++) {\n");
        }

        if (oper instanceof SIRFilter) {
            r.add("      //check_status__"+id+"();\n");

            r.add("      if (*__state_flag_"+id+" == EXIT_THREAD) exit_thread(__thread_"+id+");\n");

            if (msg_from.size() > 0) {
                r.add("      check_messages__"+id+"();\n");
            }
            if (node.isFilter() && node.inputs > 0 && node.incoming[0] != null) {
                r.add("      __update_pop_buf__"+id+"();\n");
            }
        }
        r.add("      "+work_function+"(1);\n");
        if (oper instanceof SIRFilter) {
	    /*
            if (msg_to.size() > 0 || msg_from.size() > 0) {
                r.add("      __counter_"+id+"++;\n"); 
            }
	    */
            if (/*msg_to.size() > 0*/sendsCredits) {
                r.add("      send_credits__"+id+"();\n");
            }
        }

        if (steady_counts > 1) {
            r.add("    }\n");
        }

        r.add("#ifdef __CHECKPOINT_FREQ\n");
        r.add("    if (_steady % __CHECKPOINT_FREQ == 0)\n");
        r.add("      save_state::save_to_file(__thread_"+id+", _steady, __write_thread__"+id+");\n");
        r.add("#endif // __CHECKPOINT_FREQ\n");
    
        r.add("  }\n");
        r.add("}\n");
        r.add("\n");
        }


        if (msg_to.size() > 0) {
        r.add("void __init_sdep_"+id+"() {\n");
	
	for (Iterator i = msg_to.iterator(); i.hasNext(); ) {
    
            SIRFilter sender = (SIRFilter)oper;
            SIRFilter receiver = (SIRFilter)i.next();

            int fromID = id;
            int toID = NodeEnumerator.getSIROperatorId(receiver);
        
            boolean downstream = LatencyConstraints.isMessageDirectionDownstream(sender, receiver);

            r.add("\n  //SDEP from: "+fromID+" to: "+toID+";\n");
        
            streamit.scheduler2.constrained.Scheduler cscheduler =
                streamit.scheduler2.constrained.Scheduler.createForSDEP(ClusterBackend.topStreamIter);
        
            streamit.scheduler2.iriter.Iterator firstIter = 
                IterFactory.createFactory().createIter(sender);
            streamit.scheduler2.iriter.Iterator lastIter = 
                IterFactory.createFactory().createIter(receiver);   
        
            streamit.scheduler2.SDEPData sdep = null;

            if (downstream) {
                r.add("  //message sent downstream;\n");

                try {
                    sdep = cscheduler.computeSDEP(firstIter, lastIter);
                } catch (streamit.scheduler2.constrained.NoPathException ex) {}
        
            } else {
                r.add("  //message sent upstream;\n");
        
                try {
                    sdep = cscheduler.computeSDEP(lastIter, firstIter);
                } catch (streamit.scheduler2.constrained.NoPathException ex) {}
            }
        
            int srcInit = sdep.getNumSrcInitPhases();
            int srcSteady = sdep.getNumSrcSteadyPhases();
        
            int dstInit = sdep.getNumDstInitPhases();
            int dstSteady = sdep.getNumDstSteadyPhases();
        
            String sdepname = "sdep_"+fromID+"_"+toID;
        
            r.add("  "+sdepname+" = new sdep("+
                  srcInit+","+dstInit+","+
                  srcSteady+","+dstSteady+");\n");


            // This loop can get too large to unroll at compile time:
            // one example of size 1x10^6 cause gcc to crash.
            // If > threshold size then find contiguous sections with
            // the same sdep.getSrcPhase4DstPhase(y) and generate loops
            // rather than unrolling (ignoring threshold size for now).
        
            { 
                int prevVal = sdep.getSrcPhase4DstPhase(0);
                int prevValAt = 0;
                final String seg1 = "  "+sdepname+"->setDst2SrcDependency(";
                final String seg2 = ");\n";
            
                for (int y = 0; y < dstInit + dstSteady + 1; y++) {
                    int thisVal = sdep.getSrcPhase4DstPhase(y);
                    if (thisVal != prevVal) {
                        if (y == prevValAt + 1) {
                            r.add(seg1 + prevValAt + "," + prevVal + seg2);
                        } else { 
                            r.add("  for (int __i = " + prevValAt + "; __i < " 
                                  + y
                                  + "; __i++) {\n");
                            r.add("  " + seg1 + "__i," + prevVal + seg2);
                            r.add("  }\n");
                        }
                        prevValAt = y;
                        prevVal = thisVal;
                    }
                }
                r.add("  for (int __i = " + prevValAt + "; __i < " 
                      + (dstInit + dstSteady + 1)
                      + "; __i++) {\n");
                r.add("  " + seg1 + "__i," + prevVal + seg2);
                r.add("  }\n");
            }
        }

        r.add("}\n");
        r.add("\n");
        }
        
        //  +======================================+
        //  | Run Function (not in standalone mode)|
        //  +======================================+

        if (! KjcOptions.standalone) {
        //r.add("#ifndef __CLUSTER_STANDALONE\n");
        r.add("void run_"+id+"() {\n");

        r.add("  __init_sockets_"+id+"(check_status_during_io__"+id+");\n");

        if (msg_to.size() > 0) {
            r.add("  __init_sdep_"+id+"();\n");
        }
        //r.add("  __steady_"+id+" = __init_iter;\n");
        r.add("  __init_state_"+id+"();\n");

        r.add("\n");

        for (Tape out : data_out) {
          if (out != null) {
            r.add("  timer t1;\n");

            /*
              r.add("  //peek one item from all incoming data streams\n");
              i = data_in.iterator();
              while (i.hasNext()) {
              Tape in = (Tape)i.next();
              r.add("  "+in.getConsumerName()+".peek(0);\n");
              }
            */

            r.add("  __peek_sockets_"+id+"();\n");
            r.add("  t1.start();\n");
            break;
          }
        }

        //if (main_f != null) r.add("  "+main_f+"();\n");

        r.add("  __main__"+id+"();\n");

        for (Tape out : data_out) {
          if (out != null) {
            r.add("  t1.stop();\n");
            r.add("  if (__timer_enabled) t1.output(stderr);\n");
            break;
          }
        }

        r.add("\n");
    
        for (Iterator cleanIt = cleanupCode.iterator(); cleanIt.hasNext();) {
            r.add(cleanIt.next().toString());
        }

        r.add("  __flush_sockets_"+id+"();\n");

        r.add("  pthread_exit(NULL);\n");

        r.add("}\n");
	//r.add("#endif // __CLUSTER_STANDALONE\n");
        }
        return r;
    }
}
