/**
 * 
 */
package at.dms.kjc.cluster;

import at.dms.kjc.CType;

/**
 * @author dimock
 *
 */
public class TapeFixedCopydown extends TapeFixedBase implements Tape {

    /**
     * Constructor
     */
    public TapeFixedCopydown(int src, int dest, CType type) {
        super(src,dest,type);
    }
    
    /** @return string for expression for *offset* items from pop position on tape. */
    protected String tailOffsetExpr(String offset) {
        return tailName + "+" + offset;
    }
    /** @return string for statements that increment the pop position by *numPosition* items. */
    protected String tailIncrementStmts(String numExpression) {
        return tailName + "+=" + numExpression + ";\n";
    }
    /** @return string for statement(s) that increment the pop position by 1. */
    protected String tailIncrementStmts() {
        return tailName + "++;";
    }
    /** @return string for statement(s) that increment the push position by 1. */
    protected String headIncrementStmts() {
        return headName + "++;";
    }

    

    /* (non-Javadoc)
     * @see at.dms.kjc.cluster.Tape#dataDeclarationH()
     */
    public String dataDeclarationH() {
        StringBuffer s = new StringBuffer();
        if (extra > 0) {
            s.append("//destination peeks: "
                    + extra + " extra items\n");
            s.append("#define __PEEK_BUF_SIZE_" + src + "_" + dst
                    + " " + extra + "\n");
        }
        s.append("#define __BUF_SIZE_MASK_" + src + "_" + dst
                + " (pow2ceil(" /*max(" + FixedBufferTape.getInitItems()
                + "," + FixedBufferTape.getSteadyItems()*/ + items + (FusionCode.mult == 1? "" : "*__MULT") /*+ ")"*/ + "+"
                +  extra + ")-1)\n");
        return s.toString();
    }

    
    /* (non-Javadoc)
     * @see at.dms.kjc.cluster.TapeBase#dataDeclaration()
     */
    @Override
    public String dataDeclaration() {
        String typ = ClusterUtils.CTypeToString(type);
        return 
            typ + " " + bufferName
                + "[__BUF_SIZE_MASK_" + src + "_" + dst
                + " + 1];\n"
                + "int " + headName + " = 0;\n"
                + "int " + tailName + " = 0;\n";
    }

    
    /* (non-Javadoc)
     * @see at.dms.kjc.cluster.Tape#topOfWorkIteration(at.dms.kjc.common.CodegenPrintWriter)
     */
    public String topOfWorkIteration() {
        if (extra > 0) {
            return
              "for (int __y = 0; __y < __PEEK_BUF_SIZE_"+src+"_"+dst+"; __y++) {\n"
            + "  " + bufferName + "[__y] = " + bufferName + "[__y + " + tailName +"];\n"
            + "}\n"
            + headName + " -= " + tailName + ";\n"
            + tailName + " = 0;\n";

            /*
            // Generate debugging code from old FusionCode.java"
            p.print("    if (HEAD_"+_s+"_"+_d+" - TAIL_"+_s+"_"+_d+" != __PEEK_BUF_SIZE_"+_s+"_"+_d+") {\n");
            p.print("      fprintf(stderr,\"head: %d\\n\", HEAD_"+_s+"_"+_d+");\n");
            p.print("      fprintf(stderr,\"tail: %d\\n\", TAIL_"+_s+"_"+_d+");\n");
            p.print("      assert(1 == 0);\n");
            p.print("    }\n");
            */

   
        } else {
            return
              headName + " = 0;\n"
            + tailName + " = 0;\n";
             
        }
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.cluster.Tape#pushPrefix()
     */
    public String pushPrefix() {
        return
        bufferName + "[" + headName + "]="; 
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.cluster.Tape#pushSuffix()
     */
    public String pushSuffix() {
        return
          ";\n"
        + headName + "++; "
        ;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.cluster.Tape#popExpr()
     */
    public String popExpr() {
        return popExprNoCleanup() + popExprCleanup();
    }

   public String popExprNoCleanup() {
        return bufferName + "[" + tailName + "]";
    }

   public String popExprCleanup() {
       return "; " + tailName + "++";
   }

    /* (non-Javadoc)
     * @see at.dms.kjc.cluster.Tape#popNStmt(int)
     */
    public String popNStmt(int N) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.cluster.Tape#peekPrefix()
     */
    public String peekPrefix() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.cluster.Tape#peekSuffix()
     */
    public String peekSuffix() {
        // TODO Auto-generated method stub
        return null;
    }

}
