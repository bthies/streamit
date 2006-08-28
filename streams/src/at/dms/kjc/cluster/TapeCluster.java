/**
 * 
 */
package at.dms.kjc.cluster;

import at.dms.kjc.CType;
import at.dms.kjc.CStdType;
import at.dms.kjc.sir.*;
import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.common.CommonUtils;
import java.util.*;

/**
 * Janis' code for cluster edges pulled out
 * @author dimock
 *
 */
public class TapeCluster extends TapeBase implements Tape {
    /** name of push routine (upstream) */
    protected final String push_name;
    /** name of pop routine (downstream) */
    protected final String pop_name;
    /** name of pop buffer */
    protected final String pop_buffer;
    /** name of push buffer */
    protected final String push_buffer;
    /** a variable name for a tape offset (downstream) */
    protected final String pop_index;
    /** a variable name for a tape offset (upstream) */
    protected final String push_index;
    protected final String producer_name;
    protected final String consumer_name;
    protected String tapeName; // for debugging 
    
    TapeCluster(int source, int dest, CType type) {
        super(source,dest,type);
        push_name = "__push_"+source+"_"+dest;
        pop_name = "__pop_"+source+"_"+dest;
        push_buffer = "__push_buffer_"+source+"_"+dest;
        pop_buffer = "__pop_buffer_"+source+"_"+dest;
        push_index = "__push_index_"+source+"_"+dest;
        pop_index = "__pop_index_"+source+"_"+dest;
        producer_name = "__producer_"+source+"_"+dest;
        consumer_name = "__consumer_"+source+"_"+dest;
        tapeName = "cluster_"+source+"_"+dest;
    }

    /**
     * Consumer name for init code in ClusterCodeGeneration.
     * Do not use elsewhere.
     */
    public String getConsumerName() {
        return consumer_name;
    }
    
    public String getPushName() {
        return push_name;
    }
    
    public String getPopName() {
        return pop_name;
    }

    /**
     * Consumer name for init code in ClusterCodeGeneration.
     * Do not use elsewhere.
     */
    public String getProducerName() {
        return producer_name;
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.cluster.Tape#dataDeclarationH(at.dms.kjc.common.CodegenPrintWriter)
     */
    @Override
    public String dataDeclarationH() {
        // TODO Auto-generated method stub
        return "// dataDeclarationH " + tapeName + "\n";
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.cluster.Tape#dataDeclaration(at.dms.kjc.common.CodegenPrintWriter)
     */
    @Override
    public String dataDeclaration() {
        // TODO Auto-generated method stub
        return "// dataDeclaration " + tapeName + "\n";
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.cluster.Tape#downstreamDeclarationExtern(at.dms.kjc.common.CodegenPrintWriter)
     */
    @Override
    public String downstreamDeclarationExtern() {
        return "// downstreamDeclarationExtern " + tapeName + "\n";
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.cluster.Tape#downstreamDeclaration(at.dms.kjc.common.CodegenPrintWriter)
     */
    @Override
    public String downstreamDeclaration() {
        StringBuffer s = new StringBuffer();
        s.append("// downstreamDeclaration " + tapeName + ":\n");

 //       FlatNode source_node = NodeEnumerator.getFlatNode(src);
        FlatNode my_node = NodeEnumerator.getFlatNode(dst);
      if (my_node.contents instanceof SIRFilter) {
          SIRFilter f = (SIRFilter)my_node.contents;
          int peek_n = f.getPeekInt();
          int pop_n = f.getPopInt();
          
        // String pop_expr = null;

        int peek_buf_size = 1;
        while (peek_buf_size < peek_n) {
            peek_buf_size *= 2;
        }

        /* __pop_buf_DST, __head__DST, __tail__DST,  __init_pop_buf__DST
         * seem to have to do with copying out any data items that are
         * in the input buffer that are not popped in the current iteration */
        String headName = "__head__" + dst;
        String tailName = "__tail__" + dst;
        String bufName = "__pop_buf__" + dst;
        
        s.append(typeString); s.append(" ");
        s.append(bufName); s.append("["); s.append(peek_buf_size); s.append("];\n");
        s.append("int "); s.append(headName); s.append(";\n");
        s.append("int "); s.append(tailName); s.append(";\n");
        s.append("\n");

	// problem between "extra" and init_pop_count
        //int extra = FixedBufferTape.getRemaining(src,dst);
	// going back to old method:
	int extra = f.getPeekInt() - f.getPopInt();
	// end :going back to old method:"

        s.append("inline void __init_pop_buf__"); s.append(dst); s.append("() {\n");

	int init_pop_count = extra;
	{
	    if (f instanceof SIRTwoStageFilter) {

                SIRTwoStageFilter ff = (SIRTwoStageFilter)f;

                int init_peek = ff.getInitPeekInt();
                int init_pop = ff.getInitPopInt();
                if (init_pop > init_peek) init_peek = init_pop;

                init_pop_count = init_peek;

                if (extra > (init_peek - init_pop))
                    init_pop_count += extra - (init_peek - init_pop);
            }
	}

        if (init_pop_count > 0) {
            if (init_pop_count > 1) {
                s.append("  for (int i=0; i<"); s.append(init_pop_count); s.append("; i++) {\n    ");
                s.append(bufName); s.append("[i]=");
                  s.append(popExpr()); s.append(";\n");
                s.append("  }\n");
            } else {
                s.append("  "); s.append(bufName); s.append("[0]=");
                s.append(popExpr()); s.append(";\n");
            }
        }
        s.append("  "); s.append(tailName); s.append("=0;\n");
        s.append("  "); s.append(headName); s.append("="); s.append(init_pop_count); s.append(";\n");
        s.append("}\n");
        s.append("\n");

        // save_peek_buffer__DST and load_peek_buffer__DST
        // presumably something to do with checkpointing...
        // but using buffer called "__pop_buf__"DST above
        // and using different size and modular arithmetic.
        // XXX: presumably does not work any longer.
        s.append(
                "void save_peek_buffer__" + dst
                  + "(object_write_buffer *buf) {\n"
              + "  int i = 0, offs = " + tailName + ";\n"
              + "  while (offs != " + headName + ") {\n"
              + "    buf->write(&" + bufName + "[offs], sizeof("
                  + typeString
                  + "));\n    offs++;\n    offs&=" + (peek_buf_size - 1)
                  + ";\n    i++;\n"
              + "  }\n");
        // p.print(" fprintf(stderr,\"buf size: %d\\n\", i);\n");
        s.append("  assert(i == " + extra + ");\n");
        s.append("}\n");
        s.append("\n");

        s.append("void load_peek_buffer__" + dst
                + "(object_write_buffer *buf) {\n");
        if (extra > 0) {
            if (extra > 1) {
                s.append("  for (int i = 0; i < " + extra + "; i++) {\n");
                s.append("     buf->read(&" + bufName + "[i] , sizeof("
                        + typeString + "));\n");
                s.append("  }\n");
            } else {
                s.append("     buf->read(&" + bufName + "[0] , sizeof("
                        + typeString + "));\n");
            }
        }
        s.append("  " + tailName + "=0;\n");
        s.append("  " + headName + "=" + extra + ";\n");
        s.append("}\n\n");

        s.append("inline void __update_pop_buf__" + dst + "() {\n");

        if (peek_n <= pop_n) {

            // no peek beyond pop

            // TODO: candidate for partial unrolling?
            // complete unrolls of buffer updates were removed
            // as caused excessive file size to C++
            if (pop_n > 0) {
                if (pop_n > 1) {
                    s.append("for (int i = 0; i < " + pop_n + "; i++) {\n");
                    s.append("  " + bufName + "[i]=");
                } else {
                    s.append("  " + bufName + "[0]=");
                }
                s.append(popExpr()+";\n");
                if (pop_n > 1) {
                    s.append("}\n");
                }
            }
            s.append("  " + tailName + " = 0;\n");
            s.append("  " + headName + " = " + pop_n + ";\n");

        } else {

            // peek beyond pop => circular buffer

            // TODO: candidate for partial unrolling?
            if (pop_n > 0) {
                if (pop_n > 1) {
                    s.append("  for (int i = 0; i < " + pop_n + "; i++) {\n");
                }
                s.append("  " + bufName + "[" + headName
                        + "]=");
                s.append(popExpr()+";\n");
                s.append("  " + headName + "++;");
                s.append("  " + headName + "&=" + (peek_buf_size - 1)
                        + ";\n");
                if (pop_n > 1) {
                    s.append("}\n");
                }
            }
            
        }

        s.append("}\n\n");
        
        // pop
        s.append("\n");

        s.append("inline " + typeString + " "
                + ClusterUtils.popName(dst) + "() {\n");
        //p.indent();
        if (peek_n <= pop_n) {
            s.append("return __pop_buf__" + dst + "[__tail__"
                    + dst + "++];\n");
        } else {
            s.append(typeString
                    + " res=__pop_buf__" + dst + "[__tail__"
                    + dst + "];");
            s.append("__tail__" + dst + "++;");
            s.append("__tail__" + dst + "&=" + (peek_buf_size - 1)
                    + ";\n");
            s.append("return res;\n");
        }
        //p.outdent();
        s.append("}\n");
        s.append("\n");

        // pop with argument (the source is not fused)

        s.append("inline " + typeString + " "
                + ClusterUtils.popName(dst) + "(int n) {\n");
        //p.indent();
        if (peek_n <= pop_n) {
            s.append("" + typeString
                    + " result = __pop_buf__" + dst + "[__tail__"
                    + dst + "];\n");
            s.append("__tail__" + dst + "+=n;\n");
            s.append("return result;\n");
        } else {
            s.append(typeString
                    + " res=__pop_buf__" + dst + "[__tail__"
                    + dst + "];");
            s.append("__tail__" + dst + "+=n;");
            s.append("__tail__" + dst + "&=" + (peek_buf_size - 1)
                    + ";\n");
            s.append("return res;\n");
        }
        //p.outdent();
        s.append("}\n");
        s.append("\n");

        s.append("inline " + typeString
                + " " + ClusterUtils.peekName(dst)
                + "(int offs) {\n");
        //p.indent();
        if (peek_n <= pop_n) {
            s.append("return __pop_buf__" + dst + "[(__tail__"
                    + dst + "+offs)];\n");
        } else {
            s.append("return __pop_buf__" + dst + "[(__tail__"
                      + dst + "+offs)&" + (peek_buf_size - 1)
                      + "];\n");
        }
        //p.outdent();
        s.append("  }\n");
        s.append("\n");

        s.append("\n");
      } else if (my_node.contents instanceof SIRSplitter) {
          
          // Splitter: push through from upstream to output tapes.
          SIRSplitter splitter = (SIRSplitter)my_node.contents;
          int sum_of_weights = splitter.getSumOfWeights();
          
          // List of tapes with upstream end == downstream end of this.
          List<Tape> out = RegisterStreams.getNodeOutStreams(my_node.contents);

          if (splitter.getType().equals(SIRSplitType.DUPLICATE)) {
              // Duplicate splitter: push routine pushes "data"
              // on every output tape.
              s.append("void " + push_name +"("+ typeString +" data) {\n");
              for (Tape to : out) {
                if (s != null) {      
                  s.append("  " + to.pushPrefix() + "data" + to.pushSuffix() + ";\n");
                }
              }       
              s.append("}\n");
              s.append("\n");
          } else {
              // RoundRobin splitter: push routine puts data in push_buffer
              // if push_buffer is full then copies correct portion to each output tapes.
              s.append(typeString + " " + push_buffer + "[" + sum_of_weights + "];\n");
              s.append("int " + push_index + " = 0;\n");
              s.append("\n");
              s.append("void " + push_name +"("+ typeString +" data) {\n");
              s.append("  " + push_buffer + "[" + push_index + "++] = data;\n");
              s.append("  if (" + push_index + " == " + sum_of_weights + ") {\n");
              int offs = 0;
          
              for (int i = 0; i < out.size(); i++) {
                int num = splitter.getWeight(i);
                if (num != 0) {
                  Tape to = out.get(i);

                  s.append(to.pushManyItems(push_buffer,offs,num));
                  s.append("\n");
                  offs += num;
                }
              }

              s.append("    " + push_index + " = 0;\n");
              s.append("  }\n");
              s.append("}\n");
          }
      }
      
        
      return s.toString();
    }

    public String pushManyItems(String sourceBuffer, int sourceOffset, int numItems) {
        StringBuffer s = new StringBuffer();
        s.append(producer_name + ".push_items(&" + sourceBuffer + "[" + sourceOffset + "], " + numItems + ");");
        return s.toString();
    }

    public String popManyItems(String destBuffer, int destOffset, int numItems) {
        StringBuffer s = new StringBuffer();
        s.append("    " + consumer_name + ".pop_items(&" + destBuffer + "[" + destOffset + "], " + numItems + ");\n");
        return s.toString();
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.cluster.Tape#upstreamDeclarationExtern(at.dms.kjc.common.CodegenPrintWriter)
     */
    @Override
    public String upstreamDeclarationExtern() {
        if (NodeEnumerator.getFlatNode(src).isFilter()) {
        }
        // TODO Auto-generated method stub
        return "// upstreamDeclarationExtern " + tapeName + "\n";
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.cluster.Tape#upstreamDeclaration(at.dms.kjc.common.CodegenPrintWriter)
     */
    @Override
    public String upstreamDeclaration() {
        StringBuffer s = new StringBuffer();
        s.append("// upstreamDeclaration " + tapeName + "\n");
        FlatNode node = NodeEnumerator.getFlatNode(src);
        
        if (node.isFilter()) {
            // Janis used "pop_buffer" but this seems to
            // be upstream's initialization
            int out_pop_buffer_size = 10240;
            int out_pop_num_iters = 0;
            SIRFilter f = (SIRFilter)(node.contents);
            int push_n = f.getPushInt();
            if (push_n > 0) {
                out_pop_num_iters = out_pop_buffer_size / push_n;
            }
            s.append(
                typeString + " "
              + pop_buffer + "[" + out_pop_buffer_size + "];\n"
              + "int " + pop_index + " = "
              + (out_pop_num_iters * push_n) + ";\n");
            
            s.append("inline void " + ClusterUtils.pushName(src) + "("
                    + typeString
                    + " data) {\n");
            //p.indent();
           
            // slight difference in body between cluster and cluster fusion
            createPushRoutineBody(s, "data");
            //p.outdent();
            s.append("}\n");
            
            // W.T.F: why define a pop routine at the 
            // upstream end of the tape?  Presumably to
            // to check messages.
            s.append(typeString + " " + pop_name + "() {\n");
            s.append("  int _tmp;\n");

             s.append("  if (" + pop_index + " == "
                    + (out_pop_num_iters * push_n) + ") {\n");

            s.append("    " + pop_index + " = 0;\n");

            if (out_pop_num_iters > 0) {
                if (out_pop_num_iters > 1) {
                    s.append("    for (_tmp = 0; _tmp < " + out_pop_num_iters
                            + "; _tmp++) {\n");
                }
                s.append("      //check_status__" + src + "();\n");
                if (SIRPortal.getPortalsWithReceiver(f).length > 0) {
                    s.append("      check_messages__" + src + "();\n");
                }
                if (node.inputs > 0 && node.incoming[0] != null && CommonUtils.getOutputType(node.incoming[0]) != CStdType.Void) {
                    s.append("      __update_pop_buf__" + src + "();\n");
                }
                s.append("      " + ClusterUtils.getWorkName(f, src)
                        + "(1);\n");
                s.append("      //send_credits__" + src + "();\n");
                if (out_pop_num_iters > 1) {
                    s.append("    }\n");
                }
            }
            
            s.append("    " + pop_index + " = 0;\n");

             s.append("  }\n");

            s.append("  return " + pop_buffer + "[" + pop_index
                    + "++];\n");

            s.append("}\n");
            s.append("\n");
 
            
        } else if (node.isSplitter()) {
//            // Splitter: has a "pop" routine defined for each output tape.
//            // this is presumably "pop" for a node downstream of the splitter.
//            // This routine is a bit disgusting: if pop is called for a particular
//            // downstream node, then all the splitters other downstream nodes have
//            // values pushed to them!
//            SIRSplitter splitter = (SIRSplitter)node.contents;
//            int weight = node.getWeight(NodeEnumerator.getFlatNode(dst));
//            if (weight > 0) {
//            int way = NodeEnumerator.getIdOffset(dst,node.edges);
//
//            int split_ways = splitter.getWays();
//
//            Tape splittersIn = RegisterStreams.getFilterInStream(splitter);
//            List<Tape> splitterOuts = RegisterStreams.getNodeInStreams(splitter);
//
// 
//            if (splitter.getType().equals(SIRSplitType.DUPLICATE)) {
//                s.append(typeString + " " + pop_name + "() {\n");
//                s.append("  "+ typeString + " tmp = " + splittersIn.popExpr() + ";\n");
//                for (Tape splitterOut : splitterOuts) {
//                    if (splitterOut != this) {
//                        s.append("    "+ splitterOut.pushPrefix() + "tmp" + splitterOut.pushSuffix() + ";\n");
//                    }
//                }
//                s.append("    return tmp;\n");
//            
//            } else {
//                s.append(typeString + " " + pop_buffer + "[" + weight + "];\n");
//                s.append("int " + pop_index +" = " + weight + ";\n");
//                s.append("\n");
//                s.append(typeString + " " + pop_name + "() {\n");
//
//                int sum = splitter.getSumOfWeights();
//                int offs = 0;
//            
//                p.print("  "+ typeString + " tmp[" + sum + "];\n");
//                p.print("  if ("+_out.pop_index()+" == "+splitter.getWeight(ch)+") {\n");
//            
//                p.print("    "+in.consumer_name()+".pop_items(tmp, "+sum+");\n");
//                
//                for (int y = 0; y < out.size(); y++) {
//                    int num = splitter.getWeight(y);
//                    Tape s = out.get(y);
//                    if (s != null) {
//                        if (y == ch) {
//                            p.print("    memcpy(&tmp["+offs+"], "+_out.pop_buffer()+", "+num+" * sizeof("+baseType.toString()+"));\n");
//                        } else {
//                            p.print("    "+s.producer_name()+".push_items(&tmp["+offs+"], "+num+");\n");
//                        }
//                        offs += num;
//                    }
//                }
//
//                p.print("    " + pop_index + " = 0;\n");
//                p.print("  }\n");
//                p.print("  return " + pop_buffer + "[" + pop_index()+"++];\n");
//            }
//            
//            p.print("}\n");
//            p.newLine();
//            }
        } else {
            assert node.isJoiner();
            SIRJoiner joiner = (SIRJoiner)node.contents;
            int sum_of_weights = joiner.getSumOfWeights();

            // All tapes having joiner as their downstream end
            List<Tape> in = RegisterStreams.getNodeInStreams(joiner);

            // pop routine for downstream node popping from joiner.
            // Refills buffer from joiner's input tapes when empty.
            s.append(typeString + " "+ pop_buffer + "[" + sum_of_weights + "];\n");
            s.append("int " + pop_index + " = " + sum_of_weights + ";\n");
            s.append("\n");

            s.append(typeString + " " + pop_name + "() {\n");

            s.append("  if (" + pop_index + " == " + sum_of_weights + ") {\n");
            int _offs = 0;
            
            //int ways = joiner.getWays();
            for (int i = 0; i < in.size(); i++) {
              int num = joiner.getWeight(i);
              if (num != 0) {  
                Tape ti = in.get(i);
                s.append("    " + ti.popManyItems(pop_buffer, _offs, num));
                _offs += num;
              }
            }
            s.append("    " + pop_index + " = 0;\n");
            s.append("  }\n");
        
            s.append("  return " + pop_buffer + "[" + pop_index + "++];\n");
            s.append("}\n");
            s.append("\n");
          
        }
        
        return s.toString();
    }
    
    protected void createPushRoutineBody(StringBuffer s, String dataName) {
        s.append(pushPrefix() + dataName + pushSuffix() + ";\n");
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.cluster.Tape#topOfWorkIteration(at.dms.kjc.common.CodegenPrintWriter)
     */
    public String topOfWorkIteration() {
        return "// topOfWorkIteration " + tapeName + "\n";
    }
    

    /* (non-Javadoc)
     * @see at.dms.kjc.cluster.Tape#upstreamCleanup()
     */
    public String upstreamCleanup() {
        return null;
//  Eventually put cleanup into CLusterCodeGenerator but
// not yet.
//         producer_name + ".flush();\n"
//        + producer_name + ".get_socket()->close();\n"
//        + producer_name + ".delete_socket_obj();\n";
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.cluster.Tape#downstreamCleanup()
     */
    public String downstreamCleanup() {
        return 
        consumer_name + ".delete_socket_obj();\n";
    }


    /* (non-Javadoc)
     * @see at.dms.kjc.cluster.Tape#pushPrefix(at.dms.kjc.common.CodegenPrintWriter)
     */
    @Override
    public String pushPrefix() {
        return producer_name + ".push(";
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.cluster.Tape#pushSuffix(at.dms.kjc.common.CodegenPrintWriter)
     */
    @Override
    public String pushSuffix() {
        return ")";
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.cluster.Tape#popPrefix(at.dms.kjc.common.CodegenPrintWriter)
     */
    @Override
    public String popExpr() {
        return consumer_name + ".pop()";
    }

    public String popExprNoCleanup() {
        return popExpr();
    }
    
    public String popExprCleanup() {
        return "";
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.cluster.Tape#popNStmt(at.dms.kjc.common.CodegenPrintWriter)
     */
    @Override
    public String popNStmt(int N) {
        // TODO Auto-generated method stub
        return "";
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.cluster.Tape#peekPrefix(at.dms.kjc.common.CodegenPrintWriter)
     */
    @Override
    public String peekPrefix() {
        // TODO Auto-generated method stub
        return "";
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.cluster.Tape#peekSuffix(at.dms.kjc.common.CodegenPrintWriter)
     */
    @Override
    public String peekSuffix() {
        // TODO Auto-generated method stub
        return "";
    }

    /*
     *  (non-Javadoc)
     * @see at.dms.kjc.cluster.Tape#pushbackInit(int)
     */
    public String pushbackInit(int numberToPushBack) {
        return consumer_name + ".start_push(" + numberToPushBack + ");\n";
    }
    /*
     *  (non-Javadoc)
     * @see at.dms.kjc.cluster.Tape#pushbackPrefix()
     */
    public String pushbackPrefix() {
        return consumer_name + ".push(";
    }
    /*
     * 
     */
    public String pushbackSuffix() {
        return ")";
    }
    
    /* No cleanup statements
     *  (non-Javadoc)
     * @see at.dms.kjc.cluster.Tape#pushbackCleanup()
     */
    public String pushbackCleanup() {
        return "";
    }
}
