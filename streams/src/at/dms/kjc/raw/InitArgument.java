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
public class InitArgument {
    public static String getInitArguments(SIRFilter tar) {
	// get parameters from parent
	SIRContainer parent = tar.getParent();
	List params = parent.getParams(parent.indexOf(tar));
	StringBuffer buf = new StringBuffer();

	// convert to string
	for (int i = 0; i < params.size(); i++) {
	    FlatIRToC ftoc = new FlatIRToC();
	    ((JExpression)params.get(i)).accept(ftoc);
	    buf.append(ftoc.getString() + ",");
	}
	if (buf.length() > 0) {
	    buf.setCharAt(buf.length() - 1, ' ');
	}

	// return
	return buf.toString();
    }
}
