package at.dms.kjc.raw;

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
import java.math.BigInteger;

//remove all print statements in code
public class RemovePrintStatements implements FlatVisitor {
    
    public static void doIt(FlatNode top) {
	top.accept(new RemovePrintStatements(), null, true);
    }
    
    public void visitNode(FlatNode node) {
	if (node.isFilter()) {
	    SIRFilter filter = (SIRFilter)node.contents;
	    for (int i = 0; i < filter.getMethods().length; i++)
		filter.getMethods()[i].accept(new RemovePrintStatementsHelper());
	}
    }

    static class RemovePrintStatementsHelper extends SLIRReplacingVisitor {


	public Object visitPrintStatement(SIRPrintStatement oldself,
					  JExpression exp) {

	    SIRPrintStatement self = (SIRPrintStatement)
		super.visitPrintStatement(oldself, exp);
	
	    return new JExpressionStatement(null, self.getArg(), null);
	}
    
    }
}
