package at.dms.kjc.sir.lowering.fusion;

import streamit.scheduler.*;
import streamit.scheduler.simple.*;

import at.dms.util.IRPrinter;
import at.dms.util.Utils;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.lir.*;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;

public class Lifter {

    /**
     * Given that <pipe> is a pipeline containing only a single
     * filter, eliminate the pipeline.  For now, don't call this on
     * the outer-most pipeline--<pipe> must have a non-null parent.
     */
    public static void eliminatePipe(final SIRPipeline pipe) {
	// assert the clauses
	Utils.assert(pipe.size()==1 && 
		     pipe.get(0) instanceof SIRFilter &&
		     pipe.getParent()!=null);
	// find the filter of interest
	final SIRFilter filter = (SIRFilter)pipe.get(0);

	// rename the contents of <filter>
	new RenameAll().renameFilterContents(filter);

	// add a method call to filter's <init> from <pipe's> init function
	pipe.getInit().addStatement(
		    new JExpressionStatement(null,
			     new JMethodCallExpression(null, 
			     new JThisExpression(null),
			     filter.getInit().getName(),
			     (JExpression[])pipe.getParams(pipe.indexOf(filter)).toArray(new JExpression[0])),
				     null));

	// add all the methods and fields of <pipe> to <filter>
	filter.addFields(pipe.getFields());
	filter.addMethods(pipe.getMethods());
	filter.setInitWithoutReplacement(pipe.getInit());

	// set <pipe>'s old init function as <filter>'s init function

	// in parent, replace <pipe> with <filter>
	pipe.getParent().replace(pipe, filter);
    }
}
