package at.dms.kjc.raw;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.util.Utils;
import java.util.List;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.HashMap;
import java.io.*;
import at.dms.compiler.*;
import at.dms.kjc.sir.lowering.*;


/* This class finds the init statement call for a given filter and
   returns a string representing the args of the init call */
public class InitArgument extends SLIREmptyVisitor 
{
    private static StringBuffer buf;
    private static SIRFilter targetFilter;
    
    public static String getInitArguments(SIRFilter tar) 
    {
	buf = new StringBuffer();
	targetFilter = tar;
	tar.getParent().getInit().accept(new InitArgument());
	return buf.toString();
    }
    
    public void visitInitStatement(SIRInitStatement self,
				   JExpression[] args,
				   SIRStream target) {
	if (target == targetFilter) {
	     System.out.println("Found init statement");
	     for (int i = 0; i < args.length; i++) {
		 FlatIRToC ftoc = new FlatIRToC();
		 args[i].accept(ftoc);
		 buf.append(ftoc.getString() + ",");
	     }
	     if (buf.length() > 0)
		 buf.setCharAt(buf.length() - 1, ' ');
	}
    }
}
