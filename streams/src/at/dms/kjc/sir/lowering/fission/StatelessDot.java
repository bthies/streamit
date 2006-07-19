package at.dms.kjc.sir.lowering.fission;

import java.util.*;
import java.io.*;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;

/**
 * This class extends the main streamit dot printer to color nodes
 * that are stateless.
 **/
public class StatelessDot extends StreamItDot {

    public StatelessDot(PrintStream outputstream) {
        super(outputstream);
    }

    /**
     * Given original node label 'origLabel', makes node suitable for
     * stateless highlighting.
     */
    private String makeStatelessLabel(SIROperator op, String origLabel) {
        String color;
        if (op instanceof SIRFilter) {
            if (StatelessDuplicate.hasMutableState((SIRFilter)op)) {
                color = "white";
            } else {
                color = "dodgerblue";
            }
        } else {
            color = "dodgerblue";
        }
        return origLabel +
            "\" color=\"" + color + "\" style=\"filled";
    }

    /* visit a filter */
    public Object visitFilter(SIRFilter self,
                              JFieldDeclaration[] fields,
                              JMethodDeclaration[] methods,
                              JMethodDeclaration init,
                              JMethodDeclaration work,
                              CType inputType, CType outputType)
    {
        return new NamePair(makeLabelledNode(makeStatelessLabel(self, self.getName())));
    }

    /* visit a splitter */
    public Object visitSplitter(SIRSplitter self,
                                SIRSplitType type,
                                JExpression[] expWeights)
    {
        String label = type.toString();
        // try to add weights to label
        try {
            int[] weights = self.getWeights();
            label += "(";
            for (int i=0; i<weights.length; i++) {
                label += weights[i];
                if (i!=weights.length-1) {
                    label+=",";
                }
            }
            label += ")";
        } catch (Exception e) {}
        // Create an empty node and return it.
        return new NamePair(makeLabelledNode(makeStatelessLabel(self, label)));
    }
    
    /* visit a joiner */
    public Object visitJoiner(SIRJoiner self,
                              SIRJoinType type,
                              JExpression[] expWeights)
    {
        String label = type.toString();
        // try to add weights to label
        try {
            int[] weights = self.getWeights();
            label += "(";
            for (int i=0; i<weights.length; i++) {
                label += weights[i];
                if (i!=weights.length-1) {
                    label+=",";
                }
            }
            label += ")";
        } catch (Exception e) {}
        return new NamePair(makeLabelledNode(makeStatelessLabel(self, label)));
    }

    /**
     * Prints dot graph of <pre>str</pre> to <pre>filename</pre>
     */
    public static void printGraph(SIRStream str, String filename) {
        try {
            FileOutputStream out = new FileOutputStream(filename);
            StatelessDot dot = new StatelessDot(new PrintStream(out));
            dot.print("digraph streamit {\n");
            str.accept(dot);
            dot.print("}\n");
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
