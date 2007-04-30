package at.dms.kjc.cluster;


//TODO in upstream init function, init the tape as per top of dataDeclaratoinUpstream.

import at.dms.kjc.CType;
import at.dms.kjc.flatgraph.*;
import at.dms.kjc.sir.SIRPredefinedFilter;
import at.dms.kjc.common.CommonUtils;
import at.dms.kjc.KjcOptions;
import at.dms.kjc.JExpression;
import at.dms.kjc.CArrayType;

/**
 * Dynamic-rate tapes linking nodes.
 * The tape is only one part of the story: the tape creates push, pop, etc routines
 * but to pass arrays over tapes we need to change some types so the calls to push,
 * pop, ets mat need to be wrapped in a cast or in a memcpy().
 * The translation functionality should be part of the tape class, but (alas) is
 * actually in the code emitters {@link FlatIRToCluster} and {@link ClusterCode}.
 * @author janiss refactored to unrecognizability by dimock
 *
 */
public class TapeDynrate extends TapeBase implements Tape {

    private final String tapeName;
    private final static String className = "dynTape";
    
    /** If array type, need a typdef to wrap array in a struct so can push or pop with struct*/
    private String typedefName = "";
    private String arrayTypedef = "";

    public TapeDynrate(int source, int dest, CType type) {
        super(source,dest,type);
        tapeName = "dyntape_" + source + "_" + dest;

        // in case of array type, push takes / pop returns 
        // a pointer to a struct type.
        if (type instanceof CArrayType) {
            typedefName = "_carrier_" + source + "_" + dest;
            typeString = typedefName + "*";
        }
    }
    
    @Override
    public String dataDeclarationH() {
        // a bit of a hack, but I don't have time to put the initialization
        // into the init function.
        int bufsize;
        if (type.isPrimitive()) {
            // is small: give it a buffer that holds several
            bufsize = 256;
        } else {
            // could be giant: let it grow from 1
            bufsize = 1;
        }
        try {
            bufsize = FixedBufferTape.bufferSize(src,dst,null,false);
        } catch (AssertionError e) {
            /* with given SIRDynamicRatePolicy the sizes just do not make sense */
        }
        
        StringBuffer s = new StringBuffer();
        String workname;
        
        if (KjcOptions.dynamicRatesEverywhere) {
            workname = ClusterUtils.getWorkName(NodeEnumerator
                    .getOperator(src), src);
        } else {
            // selective dynamic tapes, only works if upstream ssg is fused down
            // to a single filter.
            FlatNode srcnode = NodeEnumerator.getFlatNode(src);
            StaticStreamGraph ssg = ClusterBackend.streamGraph.parentMap
                    .get(srcnode);
            FlatNode ssgTopNode = ssg.getTopLevel();
            int ssgTopNodeNumber = NodeEnumerator.getFlatNodeId(ssgTopNode);
            workname = ClusterUtils.getWorkName(ssgTopNode.contents,
                    ssgTopNodeNumber);
        }

        // if array type then typedef for carrier type.
        
        if (type instanceof CArrayType) {
            s.append("typedef struct {" 
                + CommonUtils.declToString(type, "a", true));
            s.append(";} " + typedefName + ";\n");
        }

        // declare a dynTape class templated on carrier type if array else type.
        
        String declTypeString = (type instanceof CArrayType) ? typedefName : typeString;
        s.append("// dataDeclarationH " + tapeName + "\n");
        s.append("void " + workname + "(int);\n");
        s.append("#include \"" + className + ".h\"\n");
        s.append(className + "<" + declTypeString
                + "> " + tapeName);
        s.append(" = " + className + "<"
                + declTypeString + ">(" + bufsize
                + ", 1, " + workname + ");\n");
        return s.toString();
    }

    @Override
    public String dataDeclaration() {
        // TODO Auto-generated method stub
        return 
        "// dataDeclaration " + tapeName + "\n";
    }

    @Override
    public String downstreamDeclarationExtern() {
        // TODO Auto-generated method stub
        return //"// downstreamDeclarationExtern " + tapeName + "\n";
        //"extern " + className + "<" + typeString + "> " + tapeName + ";\n" //;
        /*+*/ "// downstreamDeclarationExtern " + tapeName + "\n";
    }

    @Override
    public String downstreamDeclaration() {
        if (! NodeEnumerator.getFlatNode(dst).isFilter() &&
                ! KjcOptions.dynamicRatesEverywhere) {
            return "";
        }
        StringBuffer s = new StringBuffer();
        s.append("// downstreamDeclaration " + tapeName + ":\n");

        /////////
        // pop()
        /////////
        
        s.append("inline " + typeString + " " + pop_name + "() {\n");
        // p.indent();
        if (type instanceof CArrayType) {
            s.append ("  static " + typedefName + " res = " + 
                    tapeName + ".pop();\n");
            s.append("  " + "return &res;\n");
        } else {
            s.append("  " + typeString + " res=" + 
                    tapeName + ".pop();\n");
            s.append("  " + "return res;\n");
        }
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
            s.append(tapeName +  ".popN(n);\n");
            s.append("}\n");
            s.append("\n");

            //////////
            // peek(i)
            //////////

            s.append("inline " + typeString + " " + peek_name
                    + "(int offs) {\n");
            // p.indent();
            if (type instanceof CArrayType) {
                s.append ("  static " + typedefName + " res = " + 
                        tapeName + ".peek(offs);\n");
                s.append("  " + "return &res;\n");
            } else {
                s.append("  return " + tapeName + ".peek(offs);\n");
            }
            // p.outdent();
            s.append("}\n");
            s.append("\n");

        }
        return s.toString();
    }

    @Override
    public String upstreamDeclarationExtern() {
        // TODO Auto-generated method stub
        return //"// upstreamDeclarationExtern " + tapeName + "\n";
        //"extern " + className + "<" + typeString + "> " + tapeName + ";\n" //;
       /*+*/ "// upstreamDeclarationExtern " + tapeName + "\n";
    }

    @Override
    public String upstreamDeclaration() {
        // TODO Auto-generated method stub
        StringBuffer s = new StringBuffer();
        s.append( "// upstreamDeclaration " + tapeName + "\n");

        if (NodeEnumerator.getFlatNode(src).isFilter() ||
                KjcOptions.dynamicRatesEverywhere) {
            s.append("\n");

            s.append("inline void " + push_name + "("
                    + typeString + " data"
                    + ") {\n");
            if (type instanceof CArrayType) {
                s.append ("  " + tapeName + ".push(*data);\n");
            } else {
                s.append("  " + tapeName + ".push(data);\n");
            }
            s.append("}\n\n");
        }       
        return s.toString();
    }

    @Override
    public String topOfWorkIteration() {
        // TODO Auto-generated method stub
        return "// topOfWorkIteration " + tapeName + "\n";
    }

    @Override
    public String upstreamCleanup() {
        // TODO Auto-generated method stub
        return "// upstreamCleanup " + tapeName + "\n";
    }

    @Override
    public String downstreamCleanup() {
        // TODO Auto-generated method stub
        return "// downstreamCleanup " + tapeName + "\n";
    }

    @Override
    public String pushPrefix() {
        // TODO Auto-generated method stub
        return 
        className + ".push(";
        //+ "/* pushPrefix" + tapeName + " */";
    }

    @Override
    public String pushSuffix() {
        // TODO Auto-generated method stub
        return 
        ")";
        //"/* pushSuffix" + tapeName + " */ ";
    }

    @Override
    public String popExpr() {
        // TODO Auto-generated method stub
        return className + ".pop()";
        //"/* popExpr" + tapeName + " */ ";
    }

    @Override
    public String popExprNoCleanup() {
        // TODO Auto-generated method stub
        return className + ".pop()";
        // + "/* popExprNoCleanup" + tapeName + " */ ";
    }

    @Override
    public String popExprCleanup() {
        // TODO Auto-generated method stub
        return ""; 
        //"/* popExprNoCleanup" + tapeName + " */ ";
    }

    @Override
    public String popNStmt(int N) {
        // TODO Auto-generated method stub
        return "// popNStmt " + tapeName + "\n";
    }

    @Override
    public String peekPrefix() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String peekSuffix() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String pushbackInit(int NumberToPush) {
        // TODO Auto-generated method stub
        return "// pushbackInit " + tapeName + "\n";
    }

    @Override
    public String pushbackPrefix() {
        // TODO Auto-generated method stub
        return //"/* pushbackPrefix" + tapeName + " */ ";
        pushPrefix();
    }

    @Override
    public String pushbackSuffix() {
        // TODO Auto-generated method stub
        return //"/* pushbackSuffix" + tapeName + " */ ";
        pushSuffix();
    }

    @Override
    public String pushbackCleanup() {
        // TODO Auto-generated method stub
        return "// pushbackCleanup " + tapeName + "\n";
    }
    
    @Override
    public String assignPopToVar(String varName) {
        if (type instanceof CArrayType) {
            return "memcpy(" + varName
            + ", " + pop_name + "()->a" +  ", sizeof(" + varName + "));\n";
        } else {
            return varName + " = " + pop_name + "();\n";
        }
    }

    @Override
    public String assignPeekToVar(String varName, String offset) {
        if (type instanceof CArrayType) {
            return "memcpy(" + varName
            + ", " + peek_name + "(" + offset + ")->a" +  ", sizeof(" + varName + "));\n";
        } else {
            return varName + " = " + peek_name + "(" + offset + ");\n";
        }
    }

    /**
     * returns the type to cast to, which is only relevant for arrays.
     * @return
     */
    public String getTypeCastName() {
        return typeString;
    }
    
}
