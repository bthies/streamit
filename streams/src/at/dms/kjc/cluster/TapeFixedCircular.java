/**
 * 
 */
package at.dms.kjc.cluster;

import at.dms.kjc.CType;
import at.dms.kjc.CArrayType;
import at.dms.kjc.JExpression;
import at.dms.kjc.JIntLiteral;

/**
 * @author dimock
 *
 */
public class TapeFixedCircular extends TapeFixedBase implements Tape {
    protected String maskName;
    /**
     * @param source
     * @param dest
     * @param type
     */
    public TapeFixedCircular(int source, int dest, CType type) {
        super(source, dest, type);
        maskName = "__BUF_SIZE_MASK_" + source + "_" + dest;
    }

    /** @return string for expression for *offset* items from pop position on tape. */
    protected String tailOffsetExpr(String offset) {
        return
        "("+ tailName+ "+" + offset + ")&" + maskName;
    }
    /** @return string for statements that increment the pop position by *numPosition* items. */
    protected String tailIncrementStmts(String numExpression) {
        return
        tailName + "+=" + numExpression + ";\n"
        + tailName + "&=" + maskName + ";\n";
    }
    /** @return string for statement(s) that increment the pop position by 1. */
    protected String tailIncrementStmts() {
        return
        tailName + "++;\n"
        + tailName + "&=" + maskName + ";\n";
    }
    /** @return string for statement(s) that increment the push position by 1. */
    protected String headIncrementStmts() {
        return
        headName + "++;\n"
        + headName + "&=" + maskName + ";\n";
    }
   
    /* (non-Javadoc)
     * @see at.dms.kjc.cluster.TapeBase#dataDeclarationH()
     */
    @Override
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
        if (type instanceof CArrayType) {
            CArrayType typa = (CArrayType)type;
            String retval = "";
            retval += typa.getBaseType();
            retval += " " + bufferName
            + "[__BUF_SIZE_MASK_" + src + "_" + dst
            + " + 1]";
            for (JExpression d : typa.getDims()) {
                retval += "[" + ((JIntLiteral)d).intValue() + "]";
            }
            retval += ";\n"
                + "int " + headName + " = 0;\n"
                + "int " + tailName + " = 0;\n";
            return retval;
        }
        return 
            typeString + " " + bufferName
                + "[__BUF_SIZE_MASK_" + src + "_" + dst
                + " + 1];\n"
                + "int " + headName + " = 0;\n"
                + "int " + tailName + " = 0;\n";
    }

    /**
     * Code to emit at top of work iteration.
     * <br/>None for circular buffer.
     * @see at.dms.kjc.cluster.Tape#topOfWorkIteration(at.dms.kjc.common.CodegenPrintWriter)
     */
    @Override
    public String topOfWorkIteration() {
        return "";
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.cluster.TapeBase#pushPrefix()
     */
    @Override
    public String pushPrefix() {
        return
        bufferName + "[" + headName + "]="; 
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.cluster.TapeBase#pushSuffix()
     */
    @Override
    public String pushSuffix() {
        return
        ";\n"
      + headName + "++; "
      + headName + "&=" + maskName
      ;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.cluster.TapeBase#popExpr()
     */
    @Override
    public String popExpr() {
        return popExprNoCleanup() + popExprCleanup();
    }

    public String popExprNoCleanup() {
        return 
        bufferName + "[" + tailName + "]";
    }
    
    public String popExprCleanup() {
        return
          "; "
        + tailName + "++; "
        + tailName + "&=" + maskName
        ;
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.cluster.TapeBase#popNStmt(int)
     */
    @Override
    public String popNStmt(int N) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.cluster.TapeBase#peekPrefix()
     */
    @Override
    public String peekPrefix() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see at.dms.kjc.cluster.TapeBase#peekSuffix()
     */
    @Override
    public String peekSuffix() {
        // TODO Auto-generated method stub
        return null;
    }

}
