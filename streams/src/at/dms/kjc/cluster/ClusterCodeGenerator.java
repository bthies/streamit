
package at.dms.kjc.cluster;

import java.io.*;
import java.lang.*;
import java.util.Vector;
import java.util.List;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.HashSet;
import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.FlatVisitor;
import at.dms.kjc.*;
import at.dms.kjc.cluster.*;
import at.dms.kjc.raw.Util;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.util.Utils;
import at.dms.compiler.TabbedPrintWriter;

class ClusterCodeGenerator {

    private SIROperator oper;
    private JFieldDeclaration fields[];
    
    private int id;
    
    private Vector data_in;
    private Vector data_out;

    private Vector msg_from;
    private Vector msg_to;

    private boolean restrictedExecution;
    private boolean sendsCredits;
    private HashSet sendsCreditsTo;

    private String TypeToC(CType t) {
	if (t.toString().compareTo("boolean") == 0) return "bool";
	return t.toString();
    } 
    
    public ClusterCodeGenerator(SIROperator oper, 
				   JFieldDeclaration fields[]) {
	
	this.oper = oper;
	this.fields = fields;

	id = NodeEnumerator.getSIROperatorId(oper);
	data_in = (Vector)RegisterStreams.getNodeInStreams(oper);
	data_out = (Vector)RegisterStreams.getNodeOutStreams(oper);

	if (oper instanceof SIRStream) {
	    SIRStream stream = (SIRStream)oper;
	    if (stream.getInputType().toString().compareTo("void") == 0) 
		data_in.clear();
	    if (stream.getOutputType().toString().compareTo("void") == 0) 
		data_out.clear();
	}

	msg_from = new Vector();
	msg_to = new Vector();

	restrictedExecution = false;
	sendsCreditsTo = new HashSet();
	sendsCredits = false;

	if (oper instanceof SIRFilter) {

	    SIRFilter f = (SIRFilter)oper;

	    restrictedExecution = LatencyConstraints.isRestricted(f); 
	    sendsCreditsTo = LatencyConstraints.getOutgoingConstraints(f);
	    sendsCredits = (sendsCreditsTo.size() > 0);

	    SIRPortal outgoing[] = SIRPortal.getPortalsWithSender(f);
	    SIRPortal incoming[] = SIRPortal.getPortalsWithReceiver(f);

	    for (int t = 0; t < outgoing.length; t++) {
		SIRStream[] receivers = outgoing[t].getReceivers();
		for (int i = 0; i < receivers.length; i++) {
		    msg_to.add(receivers[i]);
		}
	    }

	    for (int t = 0; t < incoming.length; t++) {
		SIRPortalSender[] senders = incoming[t].getSenders();
		for (int i = 0; i < senders.length; i++) {
		    msg_from.add(senders[i].getStream());
		}
	    }
	}
    }


    public Vector generatePreamble() {

	Vector r = new Vector();
	Iterator i;

	r.add("#include <stdlib.h>\n");
	r.add("#include <unistd.h>\n");
	r.add("#include <math.h>\n");	
	r.add("\n");
	r.add("#include <init_instance.h>\n");
        r.add("#include <mysocket.h>\n");
	r.add("#include <peek_stream.h>\n");
	r.add("#include <data_consumer.h>\n");
	r.add("#include <data_producer.h>\n");
	r.add("#include <object_write_buffer.h>\n");
	r.add("#include <save_state.h>\n");
	r.add("#include <sdep.h>\n");
	r.add("#include <message.h>\n");
	r.add("#include <timer.h>\n");
	r.add("#include <thread_info.h>\n");
	r.add("\n");

	r.add("extern int __max_iteration;\n");
	r.add("extern int __frequency_of_chkpts;\n");
	r.add("message *__msg_stack_"+id+";\n");
	r.add("int __number_of_iterations_"+id+";\n");
	r.add("int __counter_"+id+" = 0;\n");
	r.add("int __steady_"+id+" = 0;\n");
	r.add("int __tmp_"+id+" = 0;\n");
	r.add("int __tmp2_"+id+" = 0;\n");
	r.add("int *__state_flag_"+id+" = NULL;\n");
	r.add("thread_info *__thread_"+id+" = NULL;\n");

	if (restrictedExecution) {
	    r.add("int __credit_"+id+" = 0;\n");
	}
	
	i = msg_to.iterator();
	while (i.hasNext()) {
	    SIRStream str = (SIRStream)i.next();
	    r.add("sdep *sdep_"+id+"_"+NodeEnumerator.getSIROperatorId(str)+";\n");
	}

	r.add("\n");

	//  +=============================+
	//  | Fields                      |
	//  +=============================+

	for (int f = 0; f < fields.length; f++) {
	    CType type = fields[f].getType();
	    String ident = fields[f].getVariable().getIdent();
	    r.add(TypeToC(type)+" "+ident+"__"+id+";\n");
	}
	
	//  +=============================+
	//  | Communication Variables     |
	//  +=============================+

	i = data_in.iterator();
	while (i.hasNext()) {
	    NetStream in = (NetStream)i.next();
	    r.add("data_consumer "+in.consumer_name()+"; // "+in.getType()+"\n");
	    if (oper instanceof SIRFilter) {
		String type = ((SIRFilter)oper).getInputType().toString();
		r.add("peek_stream<"+type+"> "+in.name()+"in(&"+in.consumer_name()+");\n");
	    }
	}
	
	i = data_out.iterator();
	while (i.hasNext()) {
	    NetStream out = (NetStream)i.next();
	    r.add("data_producer "+out.producer_name()+"; // "+out.getType()+"\n");
	}
	
	i = msg_from.iterator();
	while (i.hasNext()) {
	    int src = NodeEnumerator.getSIROperatorId((SIRStream)i.next());
	    r.add("mysocket *__msg_sock_"+src+"_"+id+"in;\n");	
	}

	i = msg_to.iterator();
	while (i.hasNext()) {
	    int dst = NodeEnumerator.getSIROperatorId((SIRStream)i.next());
	    r.add("mysocket *__msg_sock_"+id+"_"+dst+"out;\n");
	}
	
	r.add("\n");

	//  +=============================+
	//  | Read / Write Thread         |
	//  +=============================+

	r.add("void __write_thread__"+id+"(object_write_buffer *buf) {\n");

	i = data_in.iterator();
	while (i.hasNext()) {
	    NetStream in = (NetStream)i.next();
	    r.add("  "+in.consumer_name()+".write_object(buf);\n");
	    if (oper instanceof SIRFilter) {
		r.add("  "+in.name()+"in.write_object(buf);\n");
	    }
	}

	for (int f = 0; f < fields.length; f++) {
	    CType type = fields[f].getType();
	    String ident = fields[f].getVariable().getIdent();
	    if (type.isArrayType()) {
		int size = 0;
		String dims[] = ArrayDim.findDim((SIRFilter)oper, ident);
		CType base = ((CArrayType)type).getBaseType();
		size = Integer.valueOf(dims[0]).intValue();
		r.add("  buf->write("+ident+"__"+id+", "+size+" * sizeof("+TypeToC(base)+"));\n");
	    } else {
		r.add("  buf->write(&"+ident+"__"+id+", sizeof("+TypeToC(type)+"));\n");
	    }
	}

	i = data_out.iterator();
	while (i.hasNext()) {
	    NetStream out = (NetStream)i.next();
	    r.add("  "+out.producer_name()+".write_object(buf);\n");
	}

	r.add("}\n");

	r.add("\n");	

	r.add("void __read_thread__"+id+"(object_write_buffer *buf) {\n");

	i = data_in.iterator();
	while (i.hasNext()) {
	    NetStream in = (NetStream)i.next();
	    r.add("  "+in.consumer_name()+".read_object(buf);\n");
	    if (oper instanceof SIRFilter) {
		r.add("  "+in.name()+"in.read_object(buf);\n");
	    }
	}

	for (int f = 0; f < fields.length; f++) {
	    CType type = fields[f].getType();
	    String ident = fields[f].getVariable().getIdent();
	    if (type.isArrayType()) {
		int size = 0;
		String dims[] = ArrayDim.findDim((SIRFilter)oper, ident);
		CType base = ((CArrayType)type).getBaseType();
		size = Integer.valueOf(dims[0]).intValue();
		r.add("  buf->read("+ident+"__"+id+", "+size+" *  sizeof("+TypeToC(base)+"));\n");
	    } else {
		r.add("  buf->read(&"+ident+"__"+id+", sizeof("+TypeToC(type)+"));\n");
	    }
	}

	i = data_out.iterator();
	while (i.hasNext()) {
	    NetStream out = (NetStream)i.next();
	    r.add("  "+out.producer_name()+".read_object(buf);\n");
	}

	r.add("}\n");

	r.add("\n");	

	//  +=============================+
	//  | Check Thread Status         |
	//  +=============================+
	
	r.add("inline void check_status__"+id+"() {\n");
	r.add("  check_thread_status(__state_flag_"+id+", __thread_"+id+");\n");
	r.add("}\n");

	r.add("\n");	

	r.add("void check_status_during_io__"+id+"() {\n");
	r.add("  check_thread_status_during_io(__state_flag_"+id+", __thread_"+id+");\n");
	r.add("}\n");

	r.add("\n");	

	//  +=============================+
	//  | Thread Info                 |
	//  +=============================+

	r.add("thread_info *__get_thread_info_"+id+"() {\n");

	r.add("  if (__thread_"+id+" != NULL) return __thread_"+id+";\n");
	r.add("  __thread_"+id+" = new thread_info("+id+", check_status_during_io__"+id+");\n");

	i = data_in.iterator();
	while (i.hasNext()) {
	    NetStream in = (NetStream)i.next();
	    r.add("  __thread_"+id+"->add_incoming_data_connection(new connection_info("+in.getSource()+","+in.getDest()+",&"+in.consumer_name()+"));\n");

	}
	
	i = data_out.iterator();
	while (i.hasNext()) {
	    NetStream out = (NetStream)i.next();
	    r.add("  __thread_"+id+"->add_outgoing_data_connection(new connection_info("+out.getSource()+","+out.getDest()+",&"+out.producer_name()+"));\n");

	}

	r.add("  __state_flag_"+id+" = __thread_"+id+"->get_state_flag();\n");
	r.add("  return __thread_"+id+";\n");
	r.add("}\n");
	
	r.add("\n");

	//  +=============================+
	//  | Declare Sockets             |
	//  +=============================+

	r.add("void __declare_sockets_"+id+"() {\n");

	i = data_in.iterator();
	while (i.hasNext()) {
	    NetStream in = (NetStream)i.next();
	    r.add("  init_instance::add_incoming("+in.getSource()+","+in.getDest()+", DATA_SOCKET);\n");
	}

	i = data_out.iterator();
	while (i.hasNext()) {
	    NetStream out = (NetStream)i.next();
	    r.add("  init_instance::add_outgoing("+out.getSource()+","+out.getDest()+", DATA_SOCKET);\n");
	}

	i = msg_from.iterator();
	while (i.hasNext()) {
	    int src = NodeEnumerator.getSIROperatorId((SIRStream)i.next());
	    r.add("  init_instance::add_incoming("+src+","+id+",MESSAGE_SOCKET);\n");
	}


	i = msg_to.iterator();
	while (i.hasNext()) {
	    int dst = NodeEnumerator.getSIROperatorId((SIRStream)i.next());
	    r.add("  init_instance::add_outgoing("+id+","+dst+",MESSAGE_SOCKET);\n");
	}

	r.add("}\n");

	r.add("\n");

	return r;
    }


    public Vector generateRunFunction(String init_f, String main_f) {
    
	Vector r = new Vector();
	Iterator i;

	r.add("\nvoid run_"+id+"() {\n");

	r.add("  int i;\n");
	r.add("  mysocket *sock;\n");

	i = data_in.iterator();
	while (i.hasNext()) {
	    NetStream in = (NetStream)i.next();

	    r.add("  sock = init_instance::get_incoming_socket("+in.getSource()+","+in.getDest()+",DATA_SOCKET);\n");
	    r.add("  sock->set_check_thread_status(check_status_during_io__"+id+");\n");
	    r.add("  sock->set_item_size(sizeof("+in.getType()+"));\n");
	    r.add("  "+in.consumer_name()+".set_socket(sock);\n");
	    
	}

	i = data_out.iterator();
	while (i.hasNext()) {
	    NetStream out = (NetStream)i.next();

	    r.add("  sock = init_instance::get_outgoing_socket("+out.getSource()+","+out.getDest()+",DATA_SOCKET);\n");
	    r.add("  sock->set_check_thread_status(check_status_during_io__"+id+");\n");
	    r.add("  sock->set_item_size(sizeof("+out.getType()+"));\n");
	    r.add("  "+out.producer_name()+".set_socket(sock);\n");
	    
	}

	i = msg_from.iterator();
	while (i.hasNext()) {
	    int src = NodeEnumerator.getSIROperatorId((SIRStream)i.next());
	    r.add("  __msg_sock_"+src+"_"+id+"in = init_instance::get_incoming_socket("+src+","+id+",MESSAGE_SOCKET);\n");
	}

	i = msg_to.iterator();
	while (i.hasNext()) {
	    int dst = NodeEnumerator.getSIROperatorId((SIRStream)i.next());
	    r.add("  __msg_sock_"+id+"_"+dst+"out = init_instance::get_outgoing_socket("+id+","+dst+",MESSAGE_SOCKET);\n");

	}

	i = msg_to.iterator();
	while (i.hasNext()) {
	
	    SIRFilter sender = (SIRFilter)oper;
	    SIRFilter receiver = (SIRFilter)i.next();

	    int fromID = id;
	    int toID = NodeEnumerator.getSIROperatorId(receiver);
	    
	    boolean downstream = LatencyConstraints.isMessageDirectionDownstream(sender, receiver);

	    r.add("\n  //SDEP from: "+fromID+" to: "+toID+";\n");
	    
	    streamit.scheduler2.constrained.Scheduler cscheduler =
		new streamit.scheduler2.constrained.Scheduler(ClusterBackend.topStreamIter);
		
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
		
	    for (int y = 0; y < dstInit + dstSteady + 1; y++) {
		r.add("  "+sdepname+"->setDst2SrcDependency("+y+","+sdep.getSrcPhase4DstPhase(y)+");\n");
	    }
	}


	if (init_f != null) r.add("  "+init_f+"();\n");

	r.add("  save_state::load_state("+id+", &__steady_"+id+", __read_thread__"+id+");\n");
	
	r.add("  __number_of_iterations_"+id+" = __max_iteration - __steady_"+id+";\n");

	if (main_f != null) r.add("  "+main_f+"();\n");

	r.add("  sleep(3);\n");

	r.add("}\n");

	return r;
    }
}
