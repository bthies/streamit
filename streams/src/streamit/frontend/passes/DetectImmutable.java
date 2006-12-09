/*
 * Copyright 2006 by the Massachusetts Institute of Technology.
 *
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in
 * advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 * M.I.T. makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is"
 * without express or implied warranty.
 */

package streamit.frontend.passes;

import streamit.frontend.nodes.*;
import java.util.*;

/**
 * Analyzes a program to determine which arrays are "immutable,"
 * in the sense that fields within the array never get assigned,
 * only read. The array reference may change to point to another
 * array, however. For instance, a statement like
 * "someArray[3] = 12;"
 * makes someArray mutable, whereas
 * "someArray = otherArray;"
 * has no effect on the mutability of either someArray or otherArray.
 * arrays passed in via message should automatically be immutable.
 * This pass does not change the stream graph in any way, but can be used
 * by subsequent passes to replace copy-by-value with copy-by-reference for
 * immutable array assignment. 
 * This pass works by going through the program and noting all of the
 * mutable arrays in each stream. If an array is never marked as mutable,
 * it is immutable. This pass is currently overly conservative, because
 * if a stream contains multiple arrays with the same name in different scopes,
 * all copies of the array with that name are marked as mutable if any one is
 * marked as mutable. It only looks at arrays for now, not structs or other
 * variables.
 * @author Matthew Drake &lt;madrake@gmail.com&gt;
 */
public class DetectImmutable extends SymbolTableVisitor
{
    class MutableArrayList {

        // This map keeps track of arrays in contexts (streams, functions) 
        // which are mutable
        // Its keys are Strings representing names of Streams,
        // and the values are names of arrays within that Stream which are mutable.
        Map<String, Set<String>> allMutableArrays = new HashMap<String, Set<String>>(); // <String, Set<String>>        

        MutableArrayList() {}
       
        boolean isImmutable(String context, String arrayName) 
        {
            if (arrayName == null)
                throw new IllegalArgumentException("Can't have null arrayName argument to isImmutable");
            Set<String> mutableArrays = getMutableArraysInStream(context);
            return !(mutableArrays.contains(arrayName));
        }
        
        Set<String> getMutableArraysInStream(String context) 
        {
            Set<String> mutableArrays = allMutableArrays.get(context);
            if (mutableArrays != null) 
                return mutableArrays;
            else
                return new HashSet<String>();
        }
        
        void markArrayAsMutable(String context, String arrayName) 
        {
            Set<String> mutableArrays = getMutableArraysInStream(context);
            mutableArrays.add(arrayName);
            allMutableArrays.put(context, mutableArrays);
        }
        
    }

    private MutableArrayList streamMutableArrays = new MutableArrayList();
    private MutableArrayList functionMutableArrays = new MutableArrayList();

    // Too bad Java doesn't have structures. Anyway, this creates a mapping
    // between variables used in a call to a function and function parameters
    // so that once all the relationships are known and mutabilities are known,
    // additional analysis can determine if any arrays that appear immutable
    // are mutated in other functions.
    List<String> caller;
    List<String> caller_variable_name;
    List<String> callee;
    List<String> callee_variable_name;

    // The name of the stream which arrays discovered as mutated should be
    // associated with.
    private String activeStreamName = null;
    private String activeFunctionName = null;

    /**
     * Create a new copy of the immutable detector.
     */
    public DetectImmutable()
    {
        super(null);
    }

    /**
     * Determines whether an array in a certain stream is immutable.
     * @param streamName the name of the stream in which the array resides
     * @param arrayName the name of the array
     * @return true if the array is immutable, false if mutable
     */
    public boolean isImmutable(String streamName, String arrayName) {
        return streamMutableArrays.isImmutable(streamName, arrayName);
    }

    //
    // The following methods overridden from SymbolTableVisitor:
    //

    public Object visitStmtAssign(StmtAssign stmt)
    {
        Expression lhs = stmt.getLHS();
        Expression rhs = stmt.getRHS();

        // We only care about mutable arrays for now.
        if (lhs instanceof ExprArray) 
            {
                String array_name;
                try {
                    array_name = ((ExprArray) lhs).getComponent().getName();
                } catch (ClassCastException _ignored_) {
                    // TODO: better handling for arrays that are parts of structs
                    return super.visitStmtAssign(stmt);
                }
                MutableArrayList mul;
                if (activeFunctionName == null ||
                    (symtab.hasVar(array_name) && 
                     SymbolTable.KIND_FIELD == (symtab.lookupKind(array_name)))) 
                    streamMutableArrays.markArrayAsMutable(activeStreamName, 
                                                           array_name);
                else
                    functionMutableArrays.markArrayAsMutable(activeFunctionName, 
                                                             array_name);
            }
        return super.visitStmtAssign(stmt);
    }

    public Object visitExprFunCall(ExprFunCall exp) 
    {
        // Note - if a function callout (i.e. to the math library)
        // tries to use a streamit array as a parameter, this will
        // cause an UnrecognizedVariableException. However,
        // 
        for (int position = 0; position < exp.getParams().size(); position++) {
            Expression param = (Expression) exp.getParams().get(position);
            if (param instanceof ExprVar) {
                ExprVar paramVar = (ExprVar) param;
                if (symtab.hasVar(paramVar.getName()) &&
                    symtab.lookupVar(paramVar.getName()) instanceof TypeArray) {
                    caller.add(activeFunctionName);
                    caller_variable_name.add(paramVar.getName());
                    callee.add(exp.getName());
                    callee_variable_name.add(((Parameter) 
                                              symtab.lookupFn(exp.getName()).
                                              getParams().get(position)).getName());
                }
            }
        }
        return super.visitExprFunCall(exp);
    }

    public Object visitStreamSpec(StreamSpec spec)
    {
        // Set the active stream name so when we visit an array we know which
        // stream with which to associate it.
        activeStreamName = spec.getName();
        caller = new ArrayList<String>();
        caller_variable_name = new ArrayList<String>();
        callee = new ArrayList<String>();
        callee_variable_name = new ArrayList<String>();
        functionMutableArrays = new MutableArrayList();

        if (activeStreamName == null)
            throw new RuntimeException("Anonymous or improperly named stream. " +
                                       "This pass must run after all anonymous " +
                                       "streams have been given unique names.");

        Object returnVal = super.visitStreamSpec(spec);

        // Here we perform the analysis across functions.
        // Essentially, if we have something like:
        // int[3] A;
        // work { blah(A); ... }
        // blah(int[3] B) { B[1] = 0 }
        // then we need to mark A as mutable, and this is the analysis that does
        // that across functions.
        boolean mutabilityChanged = true;
        while (mutabilityChanged) {
            mutabilityChanged = false;
            for (int i = 0; i < caller.size(); i++) {
                String caller_name = caller.get(i);
                String caller_variable = caller_variable_name.get(i);
                String callee_name = callee.get(i);
                String callee_variable = callee_variable_name.get(i);
                if (!functionMutableArrays.isImmutable(callee_name, callee_variable)) {
                    if (functionMutableArrays.isImmutable(caller_name, caller_variable)) {
                        functionMutableArrays.markArrayAsMutable(caller_name, 
                                                                 caller_variable);
                        mutabilityChanged = true;
                    }
                }
            }
        }
        for (Iterator<String> it = functionMutableArrays.
                 getMutableArraysInStream(null).iterator();
             it.hasNext();)
            streamMutableArrays.markArrayAsMutable(activeStreamName, it.next());

        return returnVal;
    }    

    public Object visitFunction(Function func) {
        activeFunctionName = func.getName();
        return super.visitFunction(func);
    }

}








