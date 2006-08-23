/**
 * 
 */
package at.dms.kjc.cluster;

import at.dms.kjc.CType;

/**
 * Class of tapes with fixed size buffers.
 * Common elements go here, the rest in subclasses.
 * 
 * @author dimock
 *
 */
public abstract class TapeFixedBase extends TapeBase implements Tape {
    /**
     * Maximum number of items ever on the tape: for buffer size.
     */
    protected int items;
    /**
     * (Maximum) number of unpopped items left on tape for next iteration.
     */
    protected int extra;   
    /** Name of buffer containing tape items */
    protected String bufferName;
    /** Name of variable with offset for push onto tape */
    protected String headName;
    /** Name of variable with offset for pop from tape */
    protected String tailName; //
    
    /**
     * @param source
     * @param dest
     * @param type
     */
    public TapeFixedBase(int source, int dest, CType type) {
        super(source, dest, type);
        items = FixedBufferTape.bufferSize(source,dest,null,false);
        extra = FixedBufferTape.getRemaining(source,dest);
        bufferName = "BUFFER_" + source + "_" + dest;
        headName = "HEAD_"   + source + "_" + dest;
        tailName = "TAIL_"   + source + "_" + dest;
    }
    
    /* common code for subclasses. */

    public String upstreamDeclarationExtern() {
        StringBuffer s = new StringBuffer();
        if (NodeEnumerator.getFlatNode(src).isFilter()) {
            s.append("extern " + typeString
                    + " " + bufferName + "[];\n");
            s.append("extern int " + headName + ";\n");
            s.append("extern int " + tailName + ";\n");
            s.append("\n");
        }
        return s.toString();
     }
    
    public String upstreamDeclaration() {
        StringBuffer s = new StringBuffer();
        if (NodeEnumerator.getFlatNode(src).isFilter()) {
            s.append("\n");

            s.append("inline void " + ClusterUtils.pushName(src) + "("
                    + typeString
                    + " data) {\n");
            //p.indent();
            s.append(bufferName + "[" + headName + "]=data;\n");
            s.append(headIncrementStmts() + "\n");
            //p.outdent();
            s.append("}\n");
            s.append("\n");
            s.append("\n");
        }
        return s.toString();
       
    }
    
    public String downstreamDeclarationExtern() {
        StringBuffer s = new StringBuffer();
        if (NodeEnumerator.getFlatNode(dst).isFilter()) {
             s.append("extern " + typeString
                    + " " + bufferName + "[];\n");
            s.append("extern int " + headName + ";\n");
            s.append("extern int " + tailName + ";\n");
            s.append("\n");
        }
        return s.toString();
     }
     
    
    public String downstreamDeclaration() {
        StringBuffer s = new StringBuffer();
        if (NodeEnumerator.getFlatNode(dst).isFilter()) {
            // pop from fusion buffer

            s.append("inline " + typeString + " __pop__" + dst + "() {\n");
            // p.indent();
            s.append(typeString + " res=" + bufferName + "[" + tailName
                    + "];\n");
            s.append(tailIncrementStmts() + "\n");
            s.append("return res;\n");
            // p.outdent();
            s.append("}\n");
            s.append("\n");

            // pop from fusion buffer with argument

            s.append("inline " + typeString + " __pop__" + dst + "(int n) {\n");
            // p.indent();
            s.append(typeString + " res=" + bufferName + "[" + tailName
                    + "];\n");
            s.append(tailIncrementStmts("n")+ "\n");
            s.append("return res;\n");
            // p.outdent();
            s.append("}\n");
            s.append("\n");

            // peek from fusion buffer

            s.append("inline " + typeString + " " + ClusterUtils.peekName(dst)
                    + "(int offs) {\n");
            // p.indent();
            s.append("return " + bufferName + "[" + tailOffsetExpr("offs")
                    + "];\n");
            // p.outdent();
            s.append("}\n");
            s.append("\n");

        
        }
        return s.toString();
    }
        
    /**
     * @return string for expression for *offset* items from pop position on
     *         tape.
     */
    protected abstract String tailOffsetExpr(String offset);
    /**
     * @return string for statements that increment the pop position by
     *         *numPosition* items.
     */
    protected abstract String tailIncrementStmts(String numExpression);
    /** @return string for statement(s) that increment the pop position by 1. */
    protected abstract String tailIncrementStmts();
    protected abstract String headIncrementStmts();
    

    /* (non-Javadoc)
     * @see at.dms.kjc.cluster.Tape#upstreamCleanup()
     */
    public String upstreamCleanup() {
        return "";
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.cluster.Tape#downstreamCleanup()
     */
    public String downstreamCleanup() {
        return "";
    }

    public String pushbackInit(int numberToPushBack) {
        return "";
    }
    
    public String pushbackPrefix() {
        return bufferName +"[" + headName + "] = ";
    }
    
    public String pushbackSuffix() {
        return "; " + headName + "++";
    }
    
    public String pushbackCleanup() {
        return "";
    }
}
