/**
 * 
 */
package at.dms.kjc.cluster;

import at.dms.kjc.CType;
import at.dms.kjc.CArrayType;
import at.dms.kjc.JExpression;
import at.dms.kjc.sir.SIRPredefinedFilter;
import at.dms.kjc.common.CommonUtils;

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
    
    /** in pop / peek refer to scalar as buffer[offset] but refer to array as &buffer[offset][0]...[0] */
    protected String popArrayRefPrefix;
    protected String popArrayRefSuffix;
    
    /** for debugging only */
    public int getItems() {
        return items;
    }
    
    /** for debugginf only */
    public int getExtra() {
        return extra;
    }
    
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
        popArrayRefPrefix = "";
        popArrayRefSuffix = "";
        if (type instanceof CArrayType) {
            popArrayRefPrefix = "&";
            JExpression[] dims = ((CArrayType)type).getDims();
            for (int i = 0; i < dims.length; i++) {
                popArrayRefSuffix += "[0]";
            }
        }
    }
    
    /* common code for subclasses. */

    @Override
    public String upstreamDeclarationExtern() {
        StringBuffer s = new StringBuffer();
        if (NodeEnumerator.getFlatNode(src).isFilter()) {
            s.append("extern " + typeString
                    + " " + bufferName + "[]");
            s.append(";\n");
            s.append("extern int " + headName + ";\n");
            s.append("extern int " + tailName + ";\n");
            s.append("\n");
        }
        return s.toString();
     }
    

    private static String pushArgName = "data";
    @Override
    public String upstreamDeclaration() {
        StringBuffer s = new StringBuffer();
        s.append("\n");

        ///////////
        // push(x)
        ///////////
        
        if (NodeEnumerator.getFlatNode(src).isFilter()) {
            // array: need push( basetype arg [dim1]...[dimn])
            // to handle pushing of array values as well as 
            // push (basetype* arg) which splitters and joiners
            // need for push(pop());
            // Luckily, filters need one, splitters and joiners the other.
            s.append("inline void " + push_name + "("
                    + CommonUtils.declToString(type, pushArgName, true)
                    + ") {\n");
            if (type instanceof CArrayType) {
                s.append("memcpy(" + bufferName + "[" + headName + "],"
                        + pushArgName + ", sizeof("
                        + CommonUtils.declToString(type, "", true) + "));\n");
            } else {
                s.append(bufferName + "[" + headName + "]=" + pushArgName
                        + ";\n");
            }
            s.append(headIncrementStmts() + "\n");
            s.append("}\n");
            s.append("\n");
        } else {
            s.append("inline void " + push_name + "(" + typeString
                    + " " + pushArgName + ") {\n");
            if (type instanceof CArrayType) {
                s.append("memcpy(" + bufferName + "[" + headName + "],"
                        + pushArgName + ", sizeof("
                        + CommonUtils.declToString(type, "", true) + "));\n");
            } else {
                s.append(bufferName + "[" + headName + "]=" + pushArgName
                        + ";\n");
            }
            s.append(headIncrementStmts() + "\n");
            s.append("}\n");
            s.append("\n");
        }

        s.append("\n");
        return s.toString();
       
    }
    
    @Override
    public String downstreamDeclarationExtern() {
        StringBuffer s = new StringBuffer();
        if (NodeEnumerator.getFlatNode(dst).isFilter()) {
            s.append("extern " + typeString
                    + " " + bufferName + "[]");
            s.append(";\n");
            s.append("extern int " + headName + ";\n");
            s.append("extern int " + tailName + ";\n");
            s.append("\n");
        }
        return s.toString();
     }
     
    @Override
   public String downstreamDeclaration() {
        StringBuffer s = new StringBuffer();

        //////////
        // pop()
        //////////
        
        s.append("inline " + typeString + " " + pop_name + "() {\n");
        // p.indent();
        s.append(typeString + " res=" + popArrayRefPrefix + bufferName + "["
                + tailName + "]" + popArrayRefSuffix + ";\n");
        s.append(tailIncrementStmts() + "\n");
        s.append("return res;\n");
        // p.outdent();
        s.append("}\n");
        s.append("\n");

        // splitters and joiners do not peek(offset) or pop(N);
        if (NodeEnumerator.getFlatNode(dst).isFilter()
                && ! (NodeEnumerator.getOperator(dst) instanceof SIRPredefinedFilter)) {

            //////////
            // pop(N)
            //////////

            s.append("inline " + "void" + " " + pop_name
                    + "(int n) {\n");
            s.append(tailIncrementStmts("n"));
            s.append("}\n");
            s.append("\n");

            //////////
            // peek(i)
            //////////

            s.append("inline " + typeString + " " + peek_name
                    + "(int offs) {\n");
            // p.indent();
            s.append("return " + popArrayRefPrefix + bufferName + "["
                    + tailOffsetExpr("offs") + "]" + popArrayRefSuffix + ";\n");
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
    @Override
    public String upstreamCleanup() {
        return "";
    }
    
    /* (non-Javadoc)
     * @see at.dms.kjc.cluster.Tape#downstreamCleanup()
     */
    @Override
    public String downstreamCleanup() {
        return "";
    }

    @Override
    public String pushbackInit(int numberToPushBack) {
        return "";
    }
    
    @Override
    public String pushbackPrefix() {
        return bufferName +"[" + headName + "] = ";
    }
    
    @Override
    public String pushbackSuffix() {
        return "; " + headName + "++";
    }
    
    @Override
    public String pushbackCleanup() {
        return "";
    }
    
    @Override
    public String assignPopToVar(String varName) {
        if (type instanceof CArrayType) {
            return "memcpy(" + varName
            + ", " + popExprNoCleanup() + ", sizeof(" + varName + ")); "
            + popExprCleanup() + ";\n";
        } else {
            return varName + " = " + popExpr() + ";\n";
        }
    }

    @Override
    public String assignPeekToVar(String varName, String offset) {
        String source = bufferName + "[" + tailOffsetExpr(""+offset) + "]";
        if (type instanceof CArrayType) {
            return "memcpy(" + varName
            + ", " + source + ", sizeof(" + varName + ")); ";
        } else {
            return varName + " = " + source + ";\n";
        }
    }
}
