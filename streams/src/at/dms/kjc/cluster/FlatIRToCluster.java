package at.dms.kjc.cluster;

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
import java.util.Hashtable;
import java.util.Vector;

import at.dms.kjc.raw.*;

/**
 * This class dumps the tile code for each filter into a file based 
 * on the tile number assigned 
 */
public class FlatIRToCluster extends SLIREmptyVisitor implements StreamVisitor
{
    private boolean DEBUG = false;

    protected int				TAB_SIZE = 2;
    protected int				WIDTH = 80;
    protected int				pos;
    protected String                      className;

    protected TabbedPrintWriter		p;
    protected StringWriter                str; 
    protected boolean			nl = true;
    public boolean                   declOnly = true;
    public SIRFilter               filter;
    protected JMethodDeclaration   method;

    // ALWAYS!!!!
    //true if we are using the second buffer management scheme 
    //circular buffers with anding
    public boolean debug = false;//true;
    public boolean isInit = false;
    /** > 0 if in a for loop header during visit **/
    private int forLoopHeader = 0;
    
    //fields for all of the vars names we introduce in the c code
    private final String FLOAT_HEADER_WORD = "__FLOAT_HEADER_WORD__";
    private final String INT_HEADER_WORD = "__INT_HEADER_WORD__";


    private static int filterID = 0;
    
    //Needed to pass info from assignment to visitNewArray
    JExpression lastLeft;

    private static int byteSize(CType type) {
	if (type instanceof CIntType) return 4;
	if (type instanceof CFloatType) return 4;
	if (type instanceof CDoubleType) return 8;
	return 0;
    }

    public static void generateCode(FlatNode node) 
    {
	FlatIRToCluster toC = new FlatIRToCluster((SIRFilter)node.contents);
	//FieldInitMover.moveStreamInitialAssignments((SIRFilter)node.contents);
	//FieldProp.doPropagate((SIRFilter)node.contents);

	//Optimizations
	if(!KjcOptions.nofieldprop)
	    System.out.println
		("Optimizing "+
		 ((SIRFilter)node.contents).getName()+"...");

	

	for (int i = 0; i < ((SIRFilter)node.contents).getMethods().length; i++) {
	    if (!KjcOptions.nofieldprop) {
		
		Unroller unroller;
		do {
		    do {
			//System.out.println("Unrolling..");
			unroller = new Unroller(new Hashtable());
			((SIRFilter)node.contents).getMethods()[i].accept(unroller);
		    } while(unroller.hasUnrolled());
		    //System.out.println("Constant Propagating..");
		    ((SIRFilter)node.contents).getMethods()[i].accept(new Propagator(new Hashtable()));
		    //System.out.println("Unrolling..");
		    unroller = new Unroller(new Hashtable());
		    ((SIRFilter)node.contents).getMethods()[i].accept(unroller);
		} while(unroller.hasUnrolled());
		//System.out.println("Flattening..");
		((SIRFilter)node.contents).getMethods()[i].accept(new BlockFlattener());
		//System.out.println("Analyzing Branches..");
		//((SIRFilter)node.contents).getMethods()[i].accept(new BranchAnalyzer());
		//System.out.println("Constant Propagating..");
		((SIRFilter)node.contents).getMethods()[i].accept(new Propagator(new Hashtable()));
	    } else
		((SIRFilter)node.contents).getMethods()[i].accept(new BlockFlattener());
	    ((SIRFilter)node.contents).getMethods()[i].accept(new ArrayDestroyer());
	    ((SIRFilter)node.contents).getMethods()[i].accept(new VarDeclRaiser());
	}

	DeadCodeElimination.doit((SIRFilter)node.contents);
        IterFactory.createFactory().createIter((SIRFilter)node.contents).accept(toC);
    }
    
    public FlatIRToCluster() 
    {
	this.str = new StringWriter();
        this.p = new TabbedPrintWriter(str);
    }
    

    public FlatIRToCluster(TabbedPrintWriter p) {
        this.p = p;
        this.str = null;
        this.pos = 0;
    }
    
    public FlatIRToCluster(SIRFilter f) {
	this.filter = f;
	//	circular = false;
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

    private int selfID;
    
    public void visitFilter(SIRFilter self,
			    SIRFilterIter iter) {

	selfID = NodeEnumerator.getSIROperatorId(self); // needed by the class

	HashSet sendsCreditsTo = LatencyConstraints.getOutgoingConstraints(self);
	boolean restrictedExecution = LatencyConstraints.isRestricted(self); 
	boolean sendsCredits = (sendsCreditsTo.size() > 0);

	SIRPortal outgoing[] = SIRPortal.getPortalsWithSender(self);
	SIRPortal incoming[] = SIRPortal.getPortalsWithReceiver(self);
	
	Vector sends_to = new Vector();
	Vector receives_from = new Vector();

	FlatNode node = NodeEnumerator.getFlatNode(selfID);

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
	int code = CodeEstimate.estimateCode(self);
	int locals = CodeEstimate.estimateLocals(self);
	System.out.println("[globals: "+data+" locals: "+locals+" code: "+code+"]");

	ClusterCodeGenerator gen = new ClusterCodeGenerator(self, self.getFields());

	Vector pre = gen.generatePreamble();

	for (int i = 0; i < pre.size(); i++) {
	    print(pre.elementAt(i).toString());
	}

	CType input_type = self.getInputType();
	CType output_type = self.getOutputType();

	NetStream in = RegisterStreams.getFilterInStream(self);
	NetStream out = RegisterStreams.getFilterOutStream(self);

	int out_pop_buffer_size = 10240;
	int out_pop_num_iters = 0;
	if (push_n != 0) out_pop_num_iters = out_pop_buffer_size / push_n;

	if (out != null) {
	    //print(output_type.toString()+" "+out.pop_buffer()+"["+push_n+"];\n");
	    //print("int "+out.pop_index()+" = "+push_n+";\n");

	    print(output_type.toString()+" "+out.pop_buffer()+"["+out_pop_buffer_size+"];\n");
	    print("int "+out.pop_index()+" = "+(out_pop_num_iters*push_n)+";\n");
	    
	    print("\n");
	}

	if (in != null) {

	    FlatNode source_node = NodeEnumerator.getFlatNode(in.getSource());
	    FlatNode my_node = NodeEnumerator.getFlatNode(selfID);

	    //String pop_expr = null;

	    FlatNode source_master = ClusterFusion.getLocalMaster(source_node);
	    boolean source_fused = (source_master != null && source_master.equals(my_node)); 
	    
	    //if  { 
	    //	pop_expr = in.pop_name();
	    //} else {
	    //	pop_expr = in.consumer_name()+".pop";
	    //}

	    int peek_buf_size = 1;

	    while (peek_buf_size < peek_n) {
		peek_buf_size *= 2;
	    }

	    print(input_type.toString()+" __pop_buf__"+selfID+"["+peek_buf_size+"];\n");
	    print("int __head__"+selfID+";\n");
	    print("int __tail__"+selfID+";\n");
	    print("\n");

	    int extra = peek_n - pop_n;
	    
	    assert (extra >= 0);

	    print("inline void __init_pop_buf__"+selfID+"() {\n");
			
	    for (int i = 0; i < extra; i++) {	
		
		print("  __pop_buf__"+selfID+"["+i+"]=");
		
		if (source_fused) { 
		    print(in.pop_name()+"();\n");
		} else {
		    print(in.consumer_name()+".pop();\n");
		}
	    }

	    print("  __tail__"+selfID+"=0;\n");
	    print("  __head__"+selfID+"="+extra+";\n");
		

		/*

		if (source_fused) { 

		    for (int y = 0; y < extra; y++) {
			int index = y + pop_n;
			
			print("  __pop_buf__"+selfID+"["+index+"] = "+in.pop_name()+"();\n");
		    }
		} else {

		    if (pop_n < 8) {
			
			for (int i = 0; i < extra; i++) {			
			    print("  __pop_buf__"+selfID+"["+(pop_n+i)+"] = "+in.consumer_name()+".pop();\n");
			}
			
		    } else {

			
			print("  "+in.consumer_name()+".pop_items(&__pop_buf__"+selfID+"["+pop_n+"], "+extra+");\n");
		    }

		}
		*/
	    
	    print("}\n");
	    print("\n");

	    print("inline void __update_pop_buf__"+selfID+"() {\n");
	    
	    for (int i = 0; i < pop_n; i++) {
		
		print("  __pop_buf__"+selfID+"[__head__"+selfID+"]=");

		if (source_fused) { 
		    print(in.pop_name()+"();");
		} else {
		    print(in.consumer_name()+".pop();");
		}

		print("__head__"+selfID+"++;");
		print("__head__"+selfID+"&="+(peek_buf_size-1)+";\n");
	    }

	    /*
	    print("  __pop_index__"+selfID+" = 0;\n");

	    int extra = peek_n - pop_n;
	    for (int y = 0; y < extra; y++) {
		int index = y + pop_n;
		print("  __pop_buf__"+selfID+"["+y+"] = __pop_buf__"+selfID+"["+index+"];\n");
	    }

	    if (source_fused) {
	    
		for (int y = 0; y < pop_n; y++) {
		    int index = y + extra;
		    print("  __pop_buf__"+selfID+"["+index+"] = "+in.pop_name()+"();\n");
		}
	    } else {

		if (pop_n < 8) {
		    
		    for (int i = 0; i < pop_n; i++) {			
			print("  __pop_buf__"+selfID+"["+(extra+i)+"] = "+in.consumer_name()+".pop();\n");
		    }

		} else {
		    
		    print("  "+in.consumer_name()+".pop_items(&__pop_buf__"+selfID+"["+extra+"], "+pop_n+");\n");
		}

	    }
	    */
	    
	    print("}\n");
	    print("\n");

	    int s = in.getSource();
	    int d = in.getDest();

	    print("#ifdef __FUSED_"+s+"_"+d+"\n");
	    
	    // the filter is fused with its source 

	    print("\n");

	    print("  extern "+input_type.toString()+" BUFFER_"+s+"_"+d+"[];\n");
	    print("  extern int HEAD_"+s+"_"+d+";\n");
	    print("  extern int TAIL_"+s+"_"+d+";\n");
	    print("\n");

	    // pop from fusion buffer

	    print("  inline "+input_type.toString()+" __pop__"+selfID+"() {\n");

	    print("    "+input_type.toString()+" res=BUFFER_"+s+"_"+d+"[TAIL_"+s+"_"+d+"];\n");
	    print("    TAIL_"+s+"_"+d+"++;\n");
	    print("    #ifndef __NOPEEK_"+s+"_"+d+"\n");
	    print("    TAIL_"+s+"_"+d+"&=__BUF_SIZE_MASK_"+s+"_"+d+";\n");
	    print("    #endif\n");
	    print("    return res;\n");

	    print("  }\n");
	    print("\n");

	    // peek from fusion buffer

	    print("  inline "+input_type.toString()+" __peek__"+selfID+"(int offs) {\n");
	    print("    #ifdef __NOPEEK_"+s+"_"+d+"\n");
	    print("    return BUFFER_"+s+"_"+d+"[TAIL_"+s+"_"+d+"+offs];\n");
	    print("    #else\n");
	    print("    return BUFFER_"+s+"_"+d+"[(TAIL_"+s+"_"+d+"+offs)&__BUF_SIZE_MASK_"+s+"_"+d+"];\n");
	    print("    #endif\n");

	    print("  }\n");
	    print("\n");

	    // the source is not fused

	    print("#else //!__FUSED_"+s+"_"+d+"\n");
	    print("\n");

	    print("  inline "+input_type.toString()+" __pop__"+selfID+"() {\n");

	    print("    "+input_type.toString()+" res=__pop_buf__"+selfID+"[__tail__"+selfID+"];");
	    print("__tail__"+selfID+"++;");
	    print("__tail__"+selfID+"&="+(peek_buf_size-1)+";\n");
	    print("    return res;\n");

	    //print("  return __pop_buf__"+selfID+"[__pop_index__"+selfID+"++];\n");
	    print("  }\n");
	    print("\n");

	    print("  inline "+input_type.toString()+" __peek__"+selfID+"(int offs) {\n");
	    print("    return __pop_buf__"+selfID+"[(__tail__"+selfID+"+offs)&"+(peek_buf_size-1)+"];\n");
	    
	    //print("  return __pop_buf__"+selfID+"[__pop_index__"+selfID+" + offs];\n");
 	    print("  }\n");
	    print("\n");

 	    print("#endif\n");
	    print("\n");

	} else {
       
	    print("inline "+input_type.toString()+" __init_pop_buf__"+selfID+"() {}\n");
	    print("inline "+input_type.toString()+" __update_pop_buf__"+selfID+"() {}\n");
	    print("\n");
	}

	if (out != null) {

	    FlatNode dst_node = NodeEnumerator.getFlatNode(out.getDest());
	    FlatNode my_node = NodeEnumerator.getFlatNode(selfID);

	    String push_expr = null;

	    FlatNode dst_master = ClusterFusion.getLocalMaster(dst_node);
	    FlatNode my_master = ClusterFusion.getLocalMaster(my_node);

	    int s = out.getSource();
	    int d = out.getDest();
	    
	    // check if the destination node is fused
	    
	    print("#ifdef __FUSED_"+s+"_"+d+"\n");

	    print("\n");
	    print("  extern "+output_type.toString()+" BUFFER_"+s+"_"+d+"[];\n");
	    print("  extern int HEAD_"+s+"_"+d+";\n");
	    print("  extern int TAIL_"+s+"_"+d+";\n");
	    print("\n");

	    print("  inline void __push__"+selfID+"("+output_type.toString()+" data) {\n");

	    print("    BUFFER_"+s+"_"+d+"[HEAD_"+s+"_"+d+"]=data;\n");
	    print("    HEAD_"+s+"_"+d+"++;\n");
	    print("    #ifndef __NOPEEK_"+s+"_"+d+"\n");
	    print("    HEAD_"+s+"_"+d+"&=__BUF_SIZE_MASK_"+s+"_"+d+";\n");
	    print("    #endif\n");

	    print("  }\n");
	    print("\n");

	    // if not fused use the producer's push function

	    print("#else //!__FUSED_"+s+"_"+d+"\n");
	    print("\n");

	    print("  inline void __push__"+selfID+"("+output_type.toString()+" data) {\n");

	    if (dst_master != null && dst_master.equals(my_node)) {
       		print("  "+out.push_name()+"(data);\n");		    
	    } else if (my_master != null && my_master.equals(dst_node)) {
		print("  "+out.pop_buffer()+"["+out.pop_index()+"++] = data;\n");
	    } else {
		print("  "+out.producer_name()+".push(data);\n");
	    }
	    
	    print("  }\n");

	    print("\n");
	    print("#endif");
	    print("\n");

	}

	if (in != null) {
	    print("void "+in.push_name()+"("+input_type.toString()+" data) {}\n");
	    print("\n");
	}

	print("\n");

	if (out != null) {
	    
	    //print("void "+ClusterExecutionCode.rawMain+"__"+selfID+"();\n");
	    print("void "+self.getWork().getName()+"__"+selfID+"(int);\n\n");

	    print(output_type.toString()+" "+out.pop_name()+"() {\n");
	    print("  int _tmp;\n");
	    
	    /*
	    print("  if ("+out.pop_index()+" == "+push_n+") {\n");
	    print("    int tmp = __number_of_iterations_"+selfID+";\n");
	    print("    __number_of_iterations_"+selfID+" = 1;\n");
	    print("    "+out.pop_index()+" = 0;\n");
            print("    "+ClusterExecutionCode.rawMain+"__"+selfID+"();\n");
	    print("    __number_of_iterations_"+selfID+" = tmp - 1;\n");
	    print("    "+out.pop_index()+" = 0;\n");
	    */
	    print("  if ("+out.pop_index()+" == "+(out_pop_num_iters*push_n)+") {\n");

	    print("    "+out.pop_index()+" = 0;\n");

	    print("    for (_tmp = 0; _tmp < "+out_pop_num_iters+"; _tmp++) {\n");
	    print("      //check_status__"+selfID+"();\n");
	    print("      //check_messages__"+selfID+"();\n");
	    print("      __update_pop_buf__"+selfID+"();\n");
	    print("      "+self.getWork().getName()+"__"+selfID+"(1);\n");
	    print("      //send_credits_"+selfID+"();\n");
	    print("    }\n");

	    print("    "+out.pop_index()+" = 0;\n");


	    /*
	    print("    int tmp = __number_of_iterations_"+selfID+";\n");
	    print("    __number_of_iterations_"+selfID+" = "+out_pop_num_iters+";\n");
	    print("    "+out.pop_index()+" = 0;\n");
            print("    "+ClusterExecutionCode.rawMain+"__"+selfID+"();\n");
	    print("    __number_of_iterations_"+selfID+" = tmp - "+out_pop_num_iters+";\n");
	    print("    "+out.pop_index()+" = 0;\n");
	    */

	    print("  }\n");

	    print("  return "+out.pop_buffer()+"["+out.pop_index()+"++];\n");
	    
	    print("}\n");
	    print("\n");
	}

	//  +=============================+
	//  | Method Declarations         |
	//  +=============================+

	JMethodDeclaration work = self.getWork();

	//visit methods of filter, print the declaration first
	declOnly = true;
	JMethodDeclaration[] methods = self.getMethods();
	for (int i =0; i < methods.length; i++) {
	    if (!methods[i].equals(work)) methods[i].accept(this);
	}

	print("\n");
	print("inline void check_status__"+selfID+"();\n");
	print("void check_messages__"+selfID+"();\n");
	print("void handle_message__"+selfID+"(netsocket *sock);\n");
	print("void send_credits__"+selfID+"();\n");

	print("\n");

	//  +=============================+
	//  | Method Bodies               |
	//  +=============================+

	declOnly = false;
	for (int i =0; i < methods.length; i++) {
	    if (!methods[i].equals(work)) methods[i].accept(this);
	    //methods[i].accept(this);
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
				    null);
	

	JStatement init = new JEmptyStatement(null, null);


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
	
	
	JFormalParameter param = new JFormalParameter(null, 0, CStdType.Integer, "____n", true);
	JFormalParameter params[] = new JFormalParameter[1];
	params[0] = param;

	JMethodDeclaration work_n = 
	    new JMethodDeclaration(null, 
				   at.dms.kjc.Constants.ACC_PUBLIC,
				   CStdType.Void,
				   work.getName(),
				   params,
				   CClassType.EMPTY,
				   block,
				   null,
				   null);

	work_n.accept(this);

	

	//  +=============================+
	//  | Check Messages              |
	//  +=============================+

	print("\nvoid check_messages__"+selfID+"() {\n");

	print("  message *msg, *last = NULL;\n");


	if (restrictedExecution) {
	    print("  while (__credit_"+selfID+" <= __counter_"+selfID+") {\n");
	}
	
	{
	    Iterator i = receives_from.iterator();
	    while (i.hasNext()) {
		int src = NodeEnumerator.getSIROperatorId((SIRStream)i.next());
		print("  if (__msg_sock_"+src+"_"+selfID+"in->data_available()) {\n    handle_message__"+selfID+"(__msg_sock_"+src+"_"+selfID+"in);\n  } // if\n");
	    }
	}

	if (restrictedExecution) {
	    print("  } // while \n");
	}


	print("  for (msg = __msg_stack_"+selfID+"; msg != NULL; msg = msg->next) {\n");
	print("    if (msg->execute_at <= __counter_"+selfID+") {\n");


	SIRPortal[] portals = SIRPortal.getPortalsWithReceiver(self);

	/* there should be only one portal or none */

	if (portals.length == 1) {
	    
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

	print("    }\n");
	print("    last = msg;\n");
 	print("  } // for \n");

	print("}\n");
	
	//  +=============================+
	//  | Handle Message              |
	//  +=============================+

	
	print("\nvoid handle_message__"+selfID+"(netsocket *sock) {\n");
	print("  int size = sock->read_int();\n");
	
	if (restrictedExecution) {
	    print("  if (size == -1) { // a credit message received\n");
	    print("    __credit_"+selfID+" = sock->read_int();\n");
	    print("    return;\n");
	    print("  };\n");
	}

	print("  int index = sock->read_int();\n");
	print("  int iteration = sock->read_int();\n");
	print("  printf(\"Message receieved! thread: "+selfID+", method_index: %d excute at iteration: %d\\n\", index, iteration);\n");

	print("  if (iteration > 0) {\n");
	print("    message *msg = new message(size, index, iteration);\n");
	print("    msg->read_params(sock);\n");
	print("    __msg_stack_"+selfID+" = msg->push_on_stack(__msg_stack_"+selfID+");\n");
	print("    return;\n");
	print("  }\n");

	//SIRPortal[] portals = SIRPortal.getPortalsWithReceiver(self);

	/* there should be only one portal or none */

	if (portals.length == 1) {
	    
	    CClass pclass = portals[0].getPortalType().getCClass();

	    CMethod pmethods[] = pclass.getMethods();

	    for (int i = 0 ; i < pmethods.length; i++) {

		CMethod portal_method = pmethods[i];
		CType portal_method_params[] = portal_method.getParameters();

		String method_name = portal_method.getIdent();

		int length = method_name.length();

		if (!method_name.startsWith("<") && 
		    !method_name.endsWith(">")) {

		    print("  if (index == "+i+") {\n");

		    for (int t = 0; t < methods.length; t++) {
		    
			String thread_method_name = methods[t].getName();
			
			if (thread_method_name.startsWith(method_name) &&
			    thread_method_name.charAt(length) == '_' &&
			    thread_method_name.charAt(length + 1) == '_') {
			    
			    int param_count = methods[t].getParameters().length;

			    for (int a = 0; a < param_count; a++) {
				if (portal_method_params[a].toString().equals("int")) {
				    print("    int p"+a+" = sock->read_int();\n");
				}
				if (portal_method_params[a].toString().equals("float")) {
				    print("    float p"+a+" = sock->read_float();\n");
				}
			    }

			    print("    "+thread_method_name+"__"+selfID+"(");
			    for (int a = 0; a < param_count; a++) {
				if (a > 0) print(", ");
				print("p"+a);
			    }
			    print(");\n");
			}
		    }
		    print("  }\n");
		}
	    }
	}

	print("}\n");

	
	//  +=============================+
	//  | Send Credits                |
	//  +=============================+


	Iterator constrIter;
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
	
	print("\nvoid send_credits__"+selfID+"() {\n");

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

	Vector run = gen.generateRunFunction(filter.getInit().getName()+"__"+selfID, filter.getWork().getName()+"__"+selfID);

	for (int i = 0; i < run.size(); i++) {
	    print(run.elementAt(i).toString());
	}
       
	createFile(selfID);
    }

    public void visitPhasedFilter(SIRPhasedFilter self,
                                  SIRPhasedFilterIter iter) {
        // This is a stub; it'll get filled in once we figure out how phased
        // filters should actually work.
    }

    private void createFile(int thread_id) {

	System.out.println("Code for " + filter.getName() +
			   " written to thread" + thread_id +
			   ".cpp");
	try {
	    FileWriter fw = new FileWriter("thread" + thread_id + ".cpp");
	    fw.write(str.toString());
	    fw.close();
	}
	catch (Exception e) {
	    System.err.println("Unable to write tile code file for filter " +
			       filter.getName());
	}

    }
       			
    /**
     * prints a field declaration
     */
    public void visitFieldDeclaration(JFieldDeclaration self,
                                      int modifiers,
                                      CType type,
                                      String ident,
                                      JExpression expr) {
        /*
          if (ident.indexOf("$") != -1) {
          return; // dont print generated elements
          }
        */

	if (type.toString().endsWith("Portal")) return;

        newLine();
        // print(CModifier.toString(modifiers));

	//only stack allocate singe dimension arrays
	if (expr instanceof JNewArrayExpression) {
	    //print the basetype
	    print(((CArrayType)type).getBaseType());
	    print(" ");
	    //print the field identifier
	    print(ident);
	    //print the dims
	    stackAllocateArray(ident);
	    print(";");
	    return;
	} else if (expr instanceof JArrayInitializer) {
	    declareInitializedArray(type, ident, expr);
	    return;
	}

	if (type.toString().compareTo("boolean") == 0) {
	    print("bool");
	} else {
	    print(type);
	}

        print(" ");
        print(ident);
	print("__"+selfID);

        if (expr != null) {
            print("\t= ");
	    expr.accept(this);
        }   //initialize all fields to 0
	else if (type.isOrdinal())
	    print (" = 0");
	else if (type.isFloatingPoint())
	    print(" = 0.0f");

        print(";/* "+type+" size: "+byteSize(type)+" */\n");
    }

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

        newLine();
	// print(CModifier.toString(modifiers));
	print(returnType);
	print(" ");
	print(ident);
	// this breaks initpath
	if (!ident.startsWith("__Init_Path_")) {
	    print("__"+selfID);
	}
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

    private void dummyWork(int push) {
	print("{\n");
	print("  int i;\n");
	print("  for(i = 0; i < " + push + "; i++)\n");
	print("    static_send(i);\n");
	print("}\n");
    }

    private int nextPow2(int i) {
	String str = Integer.toBinaryString(i);
	if  (str.indexOf('1') == -1)
	    return 0;
	int bit = str.length() - str.indexOf('1');
	return (int)Math.pow(2, bit);
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

    private void printLocalArrayDecl(JNewArrayExpression expr) 
    {
	JExpression[] dims = expr.getDims();
	for (int i = 0 ; i < dims.length; i++) {
	    FlatIRToCluster toC = new FlatIRToCluster();
	    dims[i].accept(toC);
	    print("[" + toC.getString() + "]");
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

        // print(CModifier.toString(modifiers));
	//	System.out.println(ident);
	//System.out.println(expr);

	//if we are in a work function, we want to stack allocate all arrays
	//right now array var definition is separate from allocation
	//we convert an assignment statement into the stack allocation statement'
	//so, just remove the var definition
	if (type.isArrayType() && !isInit) {
	    String[] dims = ArrayDim.findDim(filter, ident);
	    //but only do this if the array has corresponding 
	    //new expression, otherwise don't print anything.
	    if (expr instanceof JNewArrayExpression) {
		//print the type
		print(((CArrayType)type).getBaseType());
		print(" ");
		//print the field identifier
		print(ident);
		//print the dims
		stackAllocateArray(ident);
		print(";");
		return;
	    }
	    else if (dims != null)
		return;
	    else if (expr instanceof JArrayInitializer) {
		declareInitializedArray(type, ident, expr);
		return;
	    }
	}
	
	if (expr!=null) {
	    printLocalType(type);
	} else {
	    print(type);
	}	    
        print(" ");
	print(ident);
        if (expr != null) {
	    print(" = ");
	    expr.accept(this);
	}
	else if (type.isOrdinal())
	    print (" = 0");
	else if (type.isFloatingPoint())
	    print(" = 0.0f");

        print(";/* "+type+" */");
        //print(";\n");

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
	    FlatIRToCluster l2c = new FlatIRToCluster(filter);
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
        /*
          if (ident != null && ident.equals(JAV_INIT)) {
          return; // we do not want generated methods in source code
          }
        */

	//generate the inline asm instruction to execute the 
	//receive if this is a receive instruction
	if (ident.equals(RawExecutionCode.receiveMethod)) {

	    //Do not generate this!
	    
	    /*
	    print(Util.staticNetworkReceivePrefix());
	    visitArgs(args,0);
	    print(Util.staticNetworkReceiveSuffix(args[0].getType()));
	    */


	    return;  
	}
	
        print(ident);
	
	if (!Utils.isMathMethod(prefix, ident) && 
	    ident.indexOf("::")==-1) {
	    // don't rename the built-in math functions
	    // don't rename calls to static functions
	    print("__"+selfID);
	}
        print("(");
	
	//if this method we are calling is the call to a structure 
	//receive method that takes a pointer, we have to add the 
	//address of operator
	if (ident.startsWith(RawExecutionCode.structReceiveMethodPrefix))
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
        int		index = ident.indexOf("_$");
        if (index != -1) {
            print(ident.substring(0, index));      // local var
        } else {
	    print("(");
            left.accept(this);
	    if (!(left instanceof JThisExpression))
		print(".");
            print(ident);
	    print("__"+selfID);
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
	
	if ((left.getType().isArrayType()) &&
	     ((right.getType().isArrayType() || right instanceof SIRPopExpression) &&
	      !(right instanceof JNewArrayExpression))) {
	    
	    String ident = "";
	    	    
	    if (left instanceof JFieldAccessExpression) 
		ident = ((JFieldAccessExpression)left).getIdent();
	    else if (left instanceof JLocalVariableExpression) 
		ident = ((JLocalVariableExpression)left).getVariable().getIdent();
	    else 
		Utils.fail("Assigning an array to an unsupported expression of type " + left.getClass() + ": " + left);
	    
	    String[] dims = ArrayDim.findDim(filter, ident);
	    //if we cannot find the dim, just create a pointer copy
	    if (dims == null) {
		lastLeft=left;
		print("(");
		left.accept(this);
		print(" = ");
		right.accept(this);
		print(")");
		return;
	    }
	    print("{\n");
	    print("int ");
	    //print the index var decls
	    for (int i = 0; i < dims.length -1; i++)
		print(RawExecutionCode.ARRAY_COPY + i + ", ");
	    print(RawExecutionCode.ARRAY_COPY + (dims.length - 1));
	    print(";\n");
	    for (int i = 0; i < dims.length; i++) {
		print("for (" + RawExecutionCode.ARRAY_COPY + i + " = 0; " + RawExecutionCode.ARRAY_COPY + i +  
		      " < " + dims[i] + "; " + RawExecutionCode.ARRAY_COPY + i + "++)\n");
	    }
	    left.accept(this);
	    for (int i = 0; i < dims.length; i++)
		print("[" + RawExecutionCode.ARRAY_COPY + i + "]");
	    print(" = ");
	    right.accept(this);
	    for (int i = 0; i < dims.length; i++)
		print("[" + RawExecutionCode.ARRAY_COPY + i + "]");
	    print(";\n}\n");
	    return;
	}

	//stack allocate all arrays in the work function 
	//done at the variable definition,
	if (!isInit && right instanceof JNewArrayExpression &&
 	    (left instanceof JLocalVariableExpression)) {
	    //	    (((CArrayType)((JNewArrayExpression)right).getType()).getArrayBound() < 2)) {

	    //get the basetype and print it 
	    CType baseType = ((CArrayType)((JNewArrayExpression)right).getType()).getBaseType();
	    print(baseType);
	    print(" ");
	    //print the identifier
	    
	    //Before the ISCA hack this was how we printed the var ident
	    //	    left.accept(this);
	    
	    String ident;
	    ident = ((JLocalVariableExpression)left).getVariable().getIdent();


	    
	    if (KjcOptions.ptraccess) {
		
		//HACK FOR THE ICSA PAPER, !!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		//turn all array access into access of a pointer pointing to the array
		print(ident + "_Alloc");
		
		//print the dims of the array
		stackAllocateArray(ident);

		//print the pointer def and the assignment to the array
		print(";\n");
		print(baseType + " *" + ident + " = " + ident + "_Alloc");
	    }
	    else {
		//the way it used to be before the hack
		 left.accept(this);
		 //print the dims of the array
		 stackAllocateArray(ident);
	    }
	    return;
	}
           

	
	lastLeft=left;
        print("(");
        left.accept(this);
        print(" = ");
        right.accept(this);
        print(")");


	print("/*"+left.getType()+"*/");
    }

    //stack allocate the array
    private void stackAllocateArray(String ident) {
	//find the dimensions of the array!!
	String dims[] = 
	    ArrayDim.findDim(filter, ident);
	
	for (int i = 0; i < dims.length; i++)
	    print("[" + dims[i] + "]");
	return;
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
                                      String ident,
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

	if (portal instanceof SIRPortal) {
	    SIRStream receivers[] = ((SIRPortal)portal).getReceivers();
	    for (int i = 0; i < receivers.length; i++) {
		int dst = NodeEnumerator.getSIROperatorId(receivers[i]);

		//print("/* iname: "+iname+" ident: "+ident+" type: "+((SIRPortal)portal).getPortalType().getCClass()+"*/\n");

		CClass pclass = ((SIRPortal)portal).getPortalType().getCClass();
		CMethod methods[] = pclass.getMethods();

		int index = -1;

		int size = 12; // size:4 mindex:4 exec_at:4

		for (int t = 0; t < methods.length; t++) {

		    if (methods[t].getIdent().equals(ident)) {
			index = t;
			break;
		    }
		    //print("/* has method: "+methods[t]+" */\n");
		}

		CType method_params[] = methods[index].getParameters();

		if (params != null) {

		    // in C++ int and float have size of 4 bytes

		    size += params.length * 4; 
		}

		print("__msg_sock_"+selfID+"_"+dst+"out->write_int("+size+");");

		print("__msg_sock_"+selfID+"_"+dst+"out->write_int("+index+");");

		if (latency instanceof SIRLatencyMax) {

		    int max = ((SIRLatencyMax)latency).getMax();

		    //print("__msg_sock_"+selfID+"_"+dst+"out->write_int("+max+");");

		    SIRFilter sender = (SIRFilter)NodeEnumerator.getOperator(selfID);
		    SIRFilter receiver = (SIRFilter)NodeEnumerator.getOperator(dst);

		    if (LatencyConstraints.isMessageDirectionDownstream(sender, receiver)) {
			
			print("__msg_sock_"+selfID+"_"+dst+"out->write_int(sdep_"+selfID+"_"+dst+"->getDstPhase4SrcPhase(__counter_"+selfID+"+"+max+"));");

		    } else {
		    
					    
			print("__msg_sock_"+selfID+"_"+dst+"out->write_int(sdep_"+selfID+"_"+dst+"->getSrcPhase4DstPhase(__counter_"+selfID+"+"+max+"));");
		    
		    }

		} else {

		    print("__msg_sock_"+selfID+"_"+dst+"out->write_int(-1);");
		}

		if (params != null) {
		    for (int t = 0; t < params.length; t++) {
			if (params[t] != null) {

			    if (method_params[t].toString().equals("int")) {
				print("__msg_sock_"+selfID+"_"+dst+"out->write_int(");
				params[t].accept(this);
				print(");");
			    }
			    if (method_params[t].toString().equals("float")) {
				print("__msg_sock_"+selfID+"_"+dst+"out->write_float(");
				params[t].accept(this);
				print(");");
			    }
			}
		    }
		}
		
		
	    }
	}
    }

    public void visitPeekExpression(SIRPeekExpression self,
                                    CType tapeType,
                                    JExpression num)
    {

	NetStream in = RegisterStreams.getFilterInStream(filter);
	//print(in.consumer_name()+".peek(");
	print("__peek__"+selfID+"(");
	num.accept(this);
	print(")");

	//Utils.fail("FlatIRToCluster should see no peek expressions");
    }
    
    public void visitPopExpression(SIRPopExpression self,
                                   CType tapeType)
    {

	NetStream in = RegisterStreams.getFilterInStream(filter);
	//print(in.consumer_name()+".pop()");
	print("__pop__"+selfID+"()");

	//Utils.fail("FlatIRToCluster should see no pop expressions");
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
		//		print("gdn_send(" + INT_HEADER_WORD + ");\n");
		//print("gdn_send(");
		exp.accept(this);
		print(");");
	    }
	else
	    {
		System.out.println("Unprintatble type");
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
	
	NetStream out = RegisterStreams.getFilterOutStream(filter);

	//print(out.producer_name()+".push(");
	print("__push__"+selfID+"(");
	val.accept(this);
	print(")");

	//print(Util.staticNetworkSendPrefix(tapeType));
	//print(Util.staticNetworkSendSuffix());

    }

    
    public void pushClass(SIRPushExpression self, 
			  CType tapeType,
			  JExpression val) 
    {
	//turn the push statement into a call of
	//the structure's push method
	print("push" + tapeType + "(&");
	val.accept(this);
	print(")");
    }
    

    private void pushArray(SIRPushExpression self, 
			   CType tapeType,
			   JExpression val) 
    {
	CType baseType = ((CArrayType)tapeType).getBaseType();
	String dims[] = Util.makeString(((CArrayType)tapeType).getDims());
	
	for (int i = 0; i < dims.length; i++) {
	    print("for (" + RawExecutionCode.ARRAY_INDEX + i + " = 0; " +
		  RawExecutionCode.ARRAY_INDEX + i + " < " + dims[i] + " ; " +
		  RawExecutionCode.ARRAY_INDEX + i + "++)\n");
	}

	if(KjcOptions.altcodegen || KjcOptions.decoupled) {
	    print("{\n");
	    //	    print(Util.CSTOVAR + " = ");
	    val.accept(this);
	    for (int i = 0; i < dims.length; i++) {
		print("[" + RawExecutionCode.ARRAY_INDEX + i + "]");
	    }
	    print(";\n}\n");
	} else {
	    print("{");
	    print("static_send((" + baseType + ") ");
	    val.accept(this);
	    for (int i = 0; i < dims.length; i++) {
		print("[" + RawExecutionCode.ARRAY_INDEX + i + "]");
	    }
	    print(");\n}\n");
	}
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
        print(type);
        if (ident.indexOf("$") == -1) {
            print(" ");
            print(ident);
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

    protected void print(String s) {
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
