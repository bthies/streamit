package at.dms.kjc.cluster;


//TODO in upstream init function, init the tape as per top of dataDeclaratoinUpstream.

import at.dms.kjc.CType;
import at.dms.kjc.flatgraph.*;

//import at.dms.kjc.common.CommonUtils;

public class TapeDynrate extends TapeBase implements Tape {

    private final String tapeName;
    private final static String className = "dynTape";
    
    public TapeDynrate(int source, int dest, CType type) {
        super(source,dest,type);
        tapeName = "dyntape_" + source + "_" + dest;
    }
    
    @Override
    public String dataDeclarationH() {
        // a bit of a hack, but I don't have time to put the initialization
        // into the init function.
        
        int bufsize = 256;
        try {
            bufsize = FixedBufferTape.bufferSize(src,dst,null,false);
        } catch (AssertionError e) {
            /* with given SIRDynamicRatePolicy the sizes just do not make sense */
        }
        
        StringBuffer s = new StringBuffer();
        FlatNode srcnode = NodeEnumerator.getFlatNode(src);
        StaticStreamGraph ssg = ClusterBackend.streamGraph.parentMap.get(srcnode);
        FlatNode ssgTopNode = ssg.getTopLevel();
        int ssgTopNodeNumber = NodeEnumerator.getFlatNodeId(ssgTopNode);
        String workname = //ClusterUtils.getWorkName(NodeEnumerator.getOperator(src),src);
            ClusterUtils.getWorkName(ssgTopNode.contents,ssgTopNodeNumber);
        s.append("// dataDeclarationH " + tapeName + "\n");
        s.append("void " + workname + "(int);\n");
        s.append("#include \"" + className + ".h\"\n");
        s.append(className + "<" + typeString + "> " + tapeName);
        s.append(
                " = " 
                + className  + "<" + typeString + ">("
                + bufsize
                + ", "
                + workname
                + ");\n");
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
        StringBuffer s = new StringBuffer();
        if (NodeEnumerator.getFlatNode(dst).isFilter()) {
        s.append("// downstreamDeclaration " + tapeName + ":\n");

        // pop one
        s.append("inline " + typeString + " "
                + ClusterUtils.popName(dst) + "() {\n");
        s.append("  return " + tapeName + ".pop();\n");
        s.append("}\n\n");
        
        // pop many
        s.append("inline " + typeString + " " + ClusterUtils.popName(dst) + "(int n) {\n");
        // p.indent();
        s.append("  while (--n >= 0) {\n");
        s.append("    " + tapeName + ".pop();\n");
        s.append("  }\n");
        s.append("}\n\n");
        
        // peek
        s.append("inline " + typeString + " " + ClusterUtils.peekName(dst)
                + "(int offs) {\n");
        s.append("  return " + tapeName + ".peek(offs);\n");
        s.append("}\n\n");


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

        if (NodeEnumerator.getFlatNode(src).isFilter()) {
            s.append("\n");

            s.append("inline void " + ClusterUtils.pushName(src) + "("
                    + typeString
                    + " data) {\n");
            s.append("  " + tapeName + ".push(data);\n");
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

}
