package at.dms.kjc.raw;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.util.Utils;
import at.dms.util.*;
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
    private static boolean found;
    
    public static String getInitArguments(SIRFilter tar) 
    {
	buf = new StringBuffer();
	found = false;
	targetFilter = tar;
	tar.getParent().getInit().accept(new InitArgument());
	// make sure <tar>'s parent contains it
	if (!tar.getParent().contains(tar)) {
	    Utils.fail("This filter (visited through stream graph?) " + 
		       "points to a parent that doesn't contain " +
		       "it:\n  filter: " + tar + "\n  parent: " + 
		       tar.getParent());
	}
	if (!found) {
	    Utils.fail("init args not found for filter " + tar);
	}
	return buf.toString();
    }
    
    public void visitInitStatement(SIRInitStatement self,
				   JExpression[] args,
				   SIRStream target) {
	// make sure <target>'s parent contains it
	if (!target.getParent().contains(target)) {
	    Utils.fail("This filter (visited via SIRInitStatement descent) " + 
		       "points to a parent that doesn't contain " +
		       "it:\n  filter: " + target + "\n  parent: " + 
		       target.getParent());
	}
	if (target == targetFilter) {
	    found = true;
	    for (int i = 0; i < args.length; i++) {
		 FlatIRToC ftoc = new FlatIRToC();
		 args[i].accept(ftoc);
		 buf.append(ftoc.getString() + ",");
	     }
	     if (buf.length() > 0)
		 buf.setCharAt(buf.length() - 1, ' ');
	}
	else 
	    super.visitInitStatement(self, args, target);
    }
}
