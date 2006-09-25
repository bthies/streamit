/*
 * Copyright 2003 by the Massachusetts Institute of Technology.
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

package streamit.library;

import java.lang.reflect.*;
import java.util.*;

import streamit.misc.DestroyedClass;

// an operator takes N inputs and produces N outputs.
// Never explicitly instantiated
public class Operator extends DestroyedClass
{
    ParameterContainer initParams;
    boolean initialized = false;

    /**
     * The number of items popped and pushed during the execution of
     * the current work function.  Only maintained for splitters,
     * joiners, filters (not containers).  (This is for Eclipse
     * debugger inspection.)
     */
    int currentPopped = 0, currentPushed = 0;
    /**
     * The total number of items pushed and popped so far.  Only
     * maintained for splitters, joiners, filters (not containers).
     */
    int totalPopped = 0, totalPushed = 0;
    /**
     * The maximum peek index referenced by the filter during the
     * execution of its current work function.  For example, this will
     * be "2" after the sequence "pop(); peek(1);" -- it counts the
     * pops.
     */
    int currentMaxPeek = -1;
    /**
     * The number of times this has executed its work or prework
     * function.  Incremented only AFTER the work function has
     * completely finished.
     */
    private int workExecutions = 0;
    /**
     * The queue of Message objects to be delivered in this, sorted by
     * increasing order of delivery time.  Everything in this list has
     * yet to be delivered.
     */
    private LinkedList<Message> messageQueue = new LinkedList<Message>();
    /**
     * The parent of this in the stream hierarchy (null for toplevel
     * stream).
     */
    private Stream parent;
    /**
     * The following methods relate to maintaining the
     * currentPopped/Pushed/MaxPeek described above...
     */
    /**
     * This function should be called right before any call to work()
     * on the operator.  (Could be cleaner by calling a delegate
     * doWork() first, but that would require a big overhaul.)
     * Currently, this clears the current popped, peeked, pushed
     * rates.
     */
    public void prepareToWork() {
        currentPopped = 0;
        currentPushed = 0;
        currentMaxPeek = -1;
        deliverMessages();
    }
    /**
     * Enqueues <pre>message</pre> for delivery to this.
     */
    public void enqueueMessage(Message m) {
        int currentTime = getSDEPExecutions(true, m.isDownstream());
        int deliveryTime = m.getDeliveryTime();

        // if we missed deadline, throw error
        if (deliveryTime < currentTime) {
            throw new RuntimeException("Missed a message delivery deadline to " + this + ".  Your " + "\n" +
                                       "program has a message latency that is too tight to be satisfied " + "\n" +
                                       "under the default schedule.  Might require constrained scheduling, " + "\n" +
                                       "or might represent an invalid (impossibly tight) message constraint." + "\n" +
                                       "DELIVERY TIME = " + deliveryTime + "\n" +
                                       "CURRENT TIME  = " + currentTime);
        }

        // maintain messageQueue by order of delivery time
        for (int i=0; i<messageQueue.size(); i++) {
            int other = messageQueue.get(i).getDeliveryTime();
            if (other > deliveryTime) {
                messageQueue.add(i, m);
                return;
            }
        }
        // otherwise add at end
        messageQueue.add(m);
    }
    /**
     * Delivers all messages that should be processed before the next
     * execution.
     */
    protected void deliverMessages() {
        while (messageQueue.size()>0) {
            Message m = messageQueue.get(0);
            // deliver before the next execution
            int currentTime = getSDEPExecutions(true, m.isDownstream());
            int deliveryTime = m.getDeliveryTime();

            if (deliveryTime==currentTime) {
                messageQueue.removeFirst();
                m.deliver(this);
            } else if (deliveryTime < currentTime) {
                // we check for this in enqueMessage
                assert false : "Message missed delivery deadline and should have been reported earlier.";
            } else {
                break;
            }
        }
    }
    
    /**
     * This function should be called right after any call to work()
     * on the operator.
     */
    public void cleanupWork() {
        // if we have not pushed or popped anything, throw exception.
        // Do not count this as an execution.
        if (currentPopped==0 && currentPushed==0) {
            throw new NoPushPopException(this.toString() + " did not push or pop anything.");
        }

        workExecutions++;
    }
    /**
     * Register a pop, push, or peek.
     */
    public void registerPop() {
        Profiler.registerPop();
        currentPopped++;
        totalPopped++;
        // update peek index in case we've popped items without
        // peeking them.
        if (currentPopped-1>currentMaxPeek) {
            currentMaxPeek = currentPopped-1;
        }
    }
    public void registerPush() {
        Profiler.registerPush();
        currentPushed++;
        totalPushed++;
    }
    public void registerPeek(int i) {
        currentMaxPeek = currentPopped + i;
    }

    /**
     * Returns the number of times this has executed a work or prework
     * function.  Only counts completely finished executions of work
     * or prework (not executions that are in progress.)
     */
    public int getWorkExecutions() {
        return workExecutions;
    }
    
    /**
     * Returns the number of times that this has executed with regards
     * to the SDEP timing of a message.  That is, returns the number
     * of "ticks" this filter has done with regards to messaging.
     *
     * @param   receiver   Whether or not this is receiving the message.
     *                     (If false, this was the sender of message).
     * @param   downstream Whether or not message sent downstream.
     *                     (If false, message was sent upstream).
     */
    public int getSDEPExecutions(boolean receiver, boolean downstream) {
        // for plain filters, return number of work executions
        return getWorkExecutions();
    }
    
    /**
     * Set parent.
     */
    public void setParent(Stream str) {
        this.parent = str;
    }
    /**
     * Get parent.
     */
    public Stream getParent() {
        return parent;
    }

    /**
     * Returns whether this is downstream of <pre>op</pre>.  An operator can be
     * either upstream, downstream, or in parallel to another.  If
     * this contains <pre>op</pre> or vice-versa, result is undefined.
     */
    public boolean isDownstreamOf(Operator op) {
        return compareStreamPosition(this, op) == 1;
    }

    /**
     * Returns whether this is upstream of <pre>op</pre>.  An operator can be
     * either upstream, downstream, or in parallel to another.  If
     * this contains <pre>op</pre> or vice-versa, result is undefined.
     */
    public boolean isUpstreamOf(Operator op) {
        return compareStreamPosition(this, op) == -1;
    }

    /**
     * Returns whether this is in parallel to <pre>op</pre> -- that is, whether
     * there does NOT exist a directed path in the stream graph
     * between these two ops, within the least-common-ancestor of the
     * two ops.  An operator can be either upstream, downstream, or in
     * parallel to another.  If this contains <pre>op</pre> or vice-versa,
     * result is undefined.
     */
    public boolean isParallelTo(Operator op) {
        return compareStreamPosition(this, op) == 0;
    }

    /**
     * Compares the stream position of <pre>op1</pre> and <pre>op2</pre>, determining
     * which one is upstream of the other (or if they are in
     * parallel).  The stream position is only considered within the
     * least common ancestor of the two ops.  For example, if two ops
     * are in parallel branches of a splitjoin, they are considered to
     * be in parallel even if that splitjoin is wrapped in a
     * feedbackloop.  If the ops are in the body and loop of feedback
     * loop, then the body stream is considered upstream.  Result is
     * undefined if either op contains the other.
     *
     * @return -1  if <pre>op1</pre> is downstream of <pre>op2</pre>
     * @return 0   if <pre>op1</pre> is in parallel with <pre>op2</pre>
     * @return 1   if <pre>op1</pre> is upstream of <pre>op2</pre>
     */
    public static int compareStreamPosition(Operator op1, Operator op2) {
        // make lists of parents.  Higher-level parents at front
        // of list.
        LinkedList[] parents = { new LinkedList(), new LinkedList() };
        Operator[] base = { op1, op2 };
        for (int i=0; i<parents.length; i++) {
            // include base in parents list for convenience
            parents[i].add(base[i]);
            Stream parent = base[i].getParent();
            // don't deal with streams that contain each other
            assert parent != op1 && parent != op2;
            while (parent!=null) {
                parents[i].addFirst(parent);
                parent = parent.getParent();
            }
        }

        // trace down lists to find common parent
        Stream leastCommonAncestor = null;
        while (parents[0].size()>0 && parents[1].size()>0) {
            if (parents[0].get(0)==parents[1].get(0)) {
                leastCommonAncestor = (Stream)parents[0].get(0);
                parents[0].remove(0);
                parents[1].remove(0);
            } else {
                break;
            }
        }
        assert leastCommonAncestor != null : "Two ops do not share a common ancestor.";
        
        // now decide upstream/downstream by container
        if (leastCommonAncestor instanceof Pipeline) {
            // the upstream op is the one whos next parent appears
            // first in the pipeline
            for (int i=0; i<((Pipeline)leastCommonAncestor).getNumChildren(); i++) {
                Stream child = ((Pipeline)leastCommonAncestor).getChild(i);
                if (child==parents[0].get(0)) {
                    // op1 is upstream
                    return 1;
                }
                if (child==parents[1].get(0)) {
                    // op2 is upstream
                    return -1;
                }
            }
            assert false : "Expected to find a stream's parent in pipeline.";
        }

        if (leastCommonAncestor instanceof SplitJoin) {
            // if either op is the splitter or joiner, then they are
            // upstream or downstream.  Otherwise streams are in parallel.
            if (op1==((SplitJoin)leastCommonAncestor).getSplitter() ||
                op2==((SplitJoin)leastCommonAncestor).getJoiner()) {
                // op1 is upstream
                return 1;
            }
            if (op2==((SplitJoin)leastCommonAncestor).getSplitter() ||
                op1==((SplitJoin)leastCommonAncestor).getJoiner()) {
                // op2 is upstream
                return -1;
            }
            // otherwise, streams are in parallel
            return 0;
        }

        if (leastCommonAncestor instanceof FeedbackLoop) {
            FeedbackLoop loop = (FeedbackLoop)leastCommonAncestor;
            // order of checking
            Operator[] order = { loop.getJoiner(), loop.getBody(), loop.getSplitter(), loop.getLoop() };
            // next parents for op1 and op2
            Operator[] next = { (Operator)parents[0].get(0), (Operator)parents[1].get(0) };
            // the result if a given op is upstream
            int[] result = { 1, -1};
            // return the first upstream parent
            for (int i=0; i<order.length; i++) {
                for (int j=0; j<next.length; j++) {
                    if (order[i]==next[j]) {
                        return result[j];
                    }
                }
            }
            assert false : "Could not find next parent in feedbackloop";
        }

        assert false : "Unrecognized stream type " + leastCommonAncestor.getClass();
        return 0;
    }
    
    public Operator(float x1, float y1, int z1)
    {
        initParams = new ParameterContainer ("float-float-int")
            .add("x1", x1)
            .add("y1", y1)
            .add("z1", z1);
    }
    
    public Operator(int a, float b)
    {
        initParams = new ParameterContainer("int-float")
            .add("a", a)
            .add("b", b);
    }

    public Operator(float[] a)
    {
        initParams = new ParameterContainer("float[]")
            .add("a", copyFloatArray1D(a));
    }

    public Operator(int a, float[] b)
    {
        initParams = new ParameterContainer("int-float[]")
            .add("a", a)
            .add("b", copyFloatArray1D(b));
    }

    public Operator(int a, int[] b)
    {
        initParams = new ParameterContainer("int-int[]")
            .add("a", a)
            .add("b", copyIntArray1D(b));
    }

    public Operator(int a, float[][] b)
    {
        initParams = new ParameterContainer("int-float[][]")
            .add("a", a)
            .add("b", copyFloatArray2D(b));
    }

    public Operator(int i1, int i2, float f)
    {
        initParams = new ParameterContainer("int-int-float")
            .add("i1", i1)
            .add("i2", i2)
            .add("f", f);
    }

    public Operator(boolean b1) 
    {
        initParams = new ParameterContainer("boolean")
            .add("b1", b1);
    }
  
    public Operator(int i1, boolean b1) 
    {
        initParams = new ParameterContainer("int-boolean")
            .add("i1", i1)
            .add("b1", b1);
    }

    public Operator(int i1, int i2, boolean b1) 
    {
        initParams = new ParameterContainer("int-int-boolean")
            .add("i1", i1)
            .add("i2", i2)
            .add("b1", b1);
    }

    public Operator(int a, int b, float[] c)
    {
        initParams = new ParameterContainer("int-int-float[]")
            .add("a", a)
            .add("b", b)
            .add("c", copyFloatArray1D(c));
    }

    public Operator(int a, int b, int c, float[] d)
    {
        initParams = new ParameterContainer("int-int-int-float[]")
            .add("a", a)
            .add("b", b)
            .add("c", c)
            .add("d", copyFloatArray1D(d));
    }

    public Operator (int a, float[] c, float[] d) 
    { 
        initParams = new ParameterContainer("int-float[]-float[]")
            .add("a",a)
            .add("c",copyFloatArray1D(c))
            .add("d",copyFloatArray1D(d));
    }
    
    public Operator(int a, int b, float[][] c)
    {
        initParams = new ParameterContainer("int-int-float[][]")
            .add("a", a)
            .add("b", b)
            .add("c", copyFloatArray2D(c));
    }
    
    public Operator(int i1, int i2, float f1, float f2)
    {
        initParams = new ParameterContainer("int-int-float-float")
            .add("i1", i1).add("i2", i2).add("f1", f1).add("f2", f2);
    }
 
    public Operator(int i1, int i2, float f1, float f2, float f3) {
        initParams = new ParameterContainer("int-int-float-float-float")
            .add("i1", i1).add("i2", i2).add("f1", f1).add("f2", f2).add("f3", f3);
    }

    public Operator(int i1, int i2, int i3, int i4, int i5, float f)
    {
        initParams = new ParameterContainer("int-int-int-int-int-float")
            .add("i1",i1)
            .add("i2",i2)
            .add("i3",i3)
            .add("i4",i4)
            .add("i5",i5)
            .add("f",f);
    }
    
    public Operator(int i1, int i2, int i3, int i4, int i5, int i6, float f1, float f2)
    {
        initParams = new ParameterContainer("int-int-int-int-int-int-float-float")
            .add("i1",i1)
            .add("i2",i2)
            .add("i3",i3)
            .add("i4",i4)
            .add("i5",i5)
            .add("i6",i6)
            .add("f1",f1)
            .add("f2",f2);
    }

    public Operator(int i1, int i2, int i3, int i4, int i5, int i6, int i7, float f1, float f2)
    {
        initParams = new ParameterContainer("int-int-int-int-int-int-int-float-float")
            .add("i1",i1)
            .add("i2",i2)
            .add("i3",i3)
            .add("i4",i4)
            .add("i5",i5)
            .add("i6",i6)
            .add("i7",i7)
            .add("f1",f1)
            .add("f2",f2);
    }

    public Operator(int a, int b, int c, int d, float[][] e)
    {
        initParams = new ParameterContainer("int-int-int-int-float[][]")
            .add("a", a)
            .add("b", b)
            .add("c", c)
            .add("d", d)
            .add("e", copyFloatArray2D(e));
    }

    public Operator(int a, int b, float[][] c, float[] d) {
        initParams = new ParameterContainer("int-int-float[][]-float[]")
            .add("a", a)
            .add("b", b)
            .add("c", copyFloatArray2D(c))
            .add("d", copyFloatArray1D(d));
    }

    public Operator(int a, int b, int c, float[][] d, float[] e) {
        initParams = new ParameterContainer("int-int-int-float[][]-float[]")
            .add("a", a)
            .add("b", b)
            .add("c", c)
            .add("d", copyFloatArray2D(d))
            .add("e", copyFloatArray1D(e));
    }

    public Operator(int a, boolean b, float c, float d, float[][] e, float[] f) {
        initParams = new ParameterContainer("int-boolean-float-float-float[][]-float[]")
            .add("a", a)
            .add("b", b)
            .add("c", c)
            .add("d", d)
            .add("e", copyFloatArray2D(e))
            .add("f", copyFloatArray1D(f));
    }

    public Operator(int a, int b, int c, int d, float[][] e, float[][] f)
    {
        initParams = new ParameterContainer("int-int-int-int-float[][]-float[][]")
            .add("a", a)
            .add("b", b)
            .add("c", c)
            .add("d", d)
            .add("e", copyFloatArray2D(e))
            .add("f", copyFloatArray2D(f));
    }

    public Operator(int a, int b, int c, float[][] x, float[][] y)
    {
        initParams = new ParameterContainer("int-int-int-float[][]-float[][]")
            .add("a", a)
            .add("b", b)
            .add("c", c)
            .add("x", copyFloatArray2D(x))
            .add("y", copyFloatArray2D(y));
    }

    public Operator(float a, int b)
    {
        initParams = new ParameterContainer("float-int")
            .add("a", a)
            .add("b", b);
    }

    public Operator(float a, float b)
    {
        initParams = new ParameterContainer("float-float")
            .add("a", a)
            .add("b", b);
    }

    public Operator(float a, float b, float c)
    {
        initParams = new ParameterContainer("float-float-float")
            .add("a", a)
            .add("b", b)
            .add("c", c);
    }

    public Operator(float a, float b, float c, int d, int e)
    {
        initParams = new ParameterContainer("float-float-float-int-int")
            .add("a", a)
            .add("b", b)
            .add("c", c)
            .add("d", d)
            .add("e", e);
    }

    public Operator(float a, float b, float c, int d, int e, int f)
    {
        initParams = new ParameterContainer("float-float-float-int-int-int")
            .add("a", a)
            .add("b", b)
            .add("c", c)
            .add("d", d)
            .add("e", e)
            .add("f", f);
    }

    public Operator(float a, float b, float c, float d, int e, int f)
    {
        initParams = new ParameterContainer("float-float-float-float-int-int")
            .add("a", a)
            .add("b", b)
            .add("c", c)
            .add("d", d)
            .add("e", e)
            .add("f", f);
    }
    
    public Operator(float a, float b, float c, float d)
    {
        initParams = new ParameterContainer("float-float-float-float")
            .add("a", a)
            .add("b", b)
            .add("c", c)
            .add("d", d);
    }

    public Operator(float a, float b, float c, float d, float e, float f, float g) {
        initParams = new ParameterContainer("float-float-float-float-float-float-float")
            .add("a", a)
            .add("b", b)
            .add("c", c)
            .add("d", d)
            .add("e", e)
            .add("f", f)
            .add("g", g);
    }

    public Operator(float x3, float y3, int z3, int a3)
    {
        initParams = new ParameterContainer ("float-float-int-int")
            .add("x3", x3)
            .add("y3", y3)
            .add("z3", z3)
            .add("a3", a3);
    }

    public Operator(float x3, float y3, int z3, int a3, int b3)
    {
        initParams = new ParameterContainer ("float-float-int-int-int")
            .add("x3", x3)
            .add("y3", y3)
            .add("z3", z3)
            .add("a3", a3)
            .add("b3", b3);
    }

    public Operator(float f1, float f2, int i1, int i2, int i3, int i4)
    {
        initParams = new ParameterContainer("float-float-int-int-int-int")
            .add("f1", f1)
            .add("f2", f2)
            .add("i1", i1)
            .add("i2", i2)
            .add("i3", i3)
            .add("i4", i4);
    }

    public Operator(float f1, float f2, int i1, int i2, int i3, int i4, int i5)
    {
        initParams = new ParameterContainer("float-float-int-int-int-int-int")
            .add("f1", f1)
            .add("f2", f2)
            .add("i1", i1)
            .add("i2", i2)
            .add("i3", i3)
            .add("i4", i4)
            .add("i5", i5);
    }

    public Operator(float f1, int i1, float[] f2, float[] f3, int i2) {
        initParams = new ParameterContainer("float-int-float[]-float[]-int")
            .add("f1", f1)
            .add("i1", i1)
            .add("f2", copyFloatArray1D(f2))
            .add("f3", copyFloatArray1D(f3))
            .add("i2", i2);
    }

    public Operator (int i1, int i2, int i3,
                     int i4, int i5, int i6, int i7, int i8, 
                     int i9, int i10, float f)
    {
        initParams = new ParameterContainer ("int-int-int-int-int-int-int-int-int-int-float")
            .add("i1", i1)
            .add("i2", i2)
            .add("i3", i3)
            .add("i4", i4)
            .add("i5", i5)
            .add("i6", i6)
            .add("i7", i7)
            .add("i8", i8)
            .add("i9", i9)
            .add("i10", i10)
            .add("f", f);
    }

    public Operator (int i1, int i2, int i3, int i4, 
                     int i5, int i6, int i7, int i8, int i9)
    {
        initParams = new ParameterContainer ("int-int-int-int-int-int-int-int-int")
            .add("i1", i1)
            .add("i2", i2)
            .add("i3", i3)
            .add("i4", i4)
            .add("i5", i5)
            .add("i6", i6)
            .add("i7", i7)
            .add("i8", i8)
            .add("i9", i9);
    }

    public Operator(int a, int b, float c, int d, float e)
    {
        initParams = new ParameterContainer ("int-int-float-int-float")
            .add("a", a)
            .add("b", b)
            .add("c", c)
            .add("d", d)
            .add("e", e);
    }

    public Operator(int a, int b, int c, float d, float e)
    {
        initParams = new ParameterContainer ("int-int-int-float-float")
            .add("a", a)
            .add("b", b)
            .add("c", c)
            .add("d", d)
            .add("e", e);
    }

    public Operator(int a, int b, int c, float[][] d)
    {
        initParams = new ParameterContainer ("int-int-int-float[]")
            .add("a", a)
            .add("b", b)
            .add("c", c)
            .add("d", copyFloatArray2D(d));
    }

    public Operator(int a, int b, int c, float d, int e)
    {
        initParams = new ParameterContainer ("int-int-int-float-int")
            .add("a", a)
            .add("b", b)
            .add("c", c)
            .add("d", d)
            .add("e", e);
    }

    public Operator(float x2, float y2, float z2, int a2, float b2)
    {
        initParams = new ParameterContainer ("float-float-float-int-float")
            .add("x2", x2)
            .add("y2", y2)
            .add("z2", z2)
            .add("a2", a2)
            .add("b2", b2);
    }

    public Operator(float x2, float y2, float z2, int a2)
    {
        initParams = new ParameterContainer ("float-float-float-int")
            .add("x2", x2)
            .add("y2", y2)
            .add("z2", z2)
            .add("a2", a2);
    }

    public Operator(int i1,int i2,int i3,float f1) {
        initParams = new ParameterContainer("int-int-int-float")
            .add("i1",i1)
            .add("i2",i2)
            .add("i3",i3)
            .add("f1",f1);
    }

    public Operator()
    {
        initParams = new ParameterContainer ("");
    }

    public Operator(int n)
    {
        initParams = new ParameterContainer ("int").add ("n", n);
    }

    public Operator(char c)
    {
        initParams = new ParameterContainer ("char").add ("c", c);
    }

    public Operator (int x, int y)
    {
        initParams = new ParameterContainer ("int-int").add ("x", x).add ("y", y);
    }

    public Operator (int x, int y, int z)
    {
        initParams = new ParameterContainer ("int-int-int").add ("x", x).add ("y", y).add("z", z);
    }

    public Operator (int x, int y, int z, int a)
    {
        initParams = new ParameterContainer ("int-int-int-int").add ("x", x).add ("y", y).add("z", z).add ("a", a);
    }

    public Operator (int a, int b, int c,
                     int d, int e)
    {
        initParams = new ParameterContainer ("int-int-int-int-int").add ("a", a).add ("b", b).add("c", c).add ("d", d).add ("e", e);
    }
    
    public Operator (int x, int y, int z,
                     int a, float b)
    {
        initParams = new ParameterContainer ("int-int-int-int-float").add ("x", x).add ("y", y).add("z", z).add ("a", a).add ("b", b);
    }

    public Operator (int x, int y, int z,
                     int a, int b, int c)
    {
        initParams = new ParameterContainer ("int-int-int-int-int-int").add ("x", x).add ("y", y).add("z", z).add ("a", a).add ("b", b).add("c", c);
    }

    public Operator (int i1, int i2, int i3,
                     int i4, int i5, int i6, int i7)
    {
        initParams = new ParameterContainer ("int-int-int-int-int-int-int") .add("i1", i1) .add("i2", i2). add("i3", i3) .add("i4", i4) .add("i5", i5) .add("i6", i6) .add("i7",i7);
    }


    public Operator (int x, int y, int z,
                     int a, int b, int c, int d, float f)
    {
        initParams = new ParameterContainer ("int-int-int-int-int-int-int-float").add ("x", x).add ("y", y).add("z", z).add ("a", a).add ("b", b).add("c", c).add("d",d).add("f",f);
    }

    public Operator(float f)
    {
        initParams = new ParameterContainer ("float").add ("f", f);
    }

    public Operator(String str)
    {
        initParams = new ParameterContainer ("String").add ("str", str);
    }

    public Operator(ParameterContainer params)
    {
        initParams = new ParameterContainer ("ParameterContainer").add ("params", params);
    }

    public Operator(int i1,
                    int i2, 
                    int i3, 
                    int i4, 
                    int i5, 
                    int i6, 
                    int i7, 
                    int i8, 
                    int i9, 
                    float f) {
        initParams = new ParameterContainer ("int-int-int-int-int-int-int-int-int-float").add ("i1", i1).add ("i2", i2).add ("i3", i3).add ("i4", i4).add ("i5", i5).add ("i6", i6).add ("i7", i7).add ("i8", i8).add ("i9", i9).add ("f", f);
    }


    public Operator(int i1,
                    int i2, 
                    int i3, 
                    int i4, 
                    int i5, 
                    int i6, 
                    float f) {
        initParams = new ParameterContainer ("int-int-int-int-int-int-float").add ("i1", i1).add ("i2", i2).add ("i3", i3).add ("i4", i4).add ("i5", i5).add ("i6", i6).add ("f", f);
    }

    public Operator(int a, int b, float[] c, float[] d)
    {
        initParams = new ParameterContainer("int-int-float[]-float[]")
            .add("a", a)
            .add("b", b)
            .add("c", copyFloatArray1D(c))
            .add("d", copyFloatArray1D(d));
    }

    public Operator(short s1, short s2, short s3) {
        initParams = new ParameterContainer("short-short-short")
            .add("s1", s1)
            .add("s2", s2)
            .add("s3", s3);
    }
    
    public Operator(Object o1) {
        initParams = new ParameterContainer("Object")
            .add("o1", o1);
    }
    
    public Operator(Object o1, int i1) {
        initParams = new ParameterContainer("Object-int")
            .add("o1", o1)
            .add("i1", i1);
    }
    
    public Operator(int i1, int i2, Object o1) {
        initParams = new ParameterContainer("int-int-Object")
            .add("i1", i1)
            .add("i2", i2)
            .add("o1", o1);
    }

    public Operator(Object o1, Object o2) {
        initParams = new ParameterContainer("Object-Object")
            .add("o1", o1)
            .add("o2", o2);
    }

    public Operator(Object o1, Object o2, Object o3) {
        initParams = new ParameterContainer("Object-Object-Object")
            .add("o1", o1)
            .add("o2", o2)
            .add("o3", o3);
    }

    // INIT FUNCTIONS ---------------------------------------------------------------------

    void invalidInitError ()
    {
        ERROR ("You didn't provide a valid init function in class " + getClass ().getName () + ".\nFilters now need init functions, even if they're empty.\nPerhaps you're passing parameters that don't have a valid prototype yet?\nCheck streams/docs/implementation-notes/library-init-functions.txt for instructions on adding new signatures to init functions.\n" + 
               "The init string you passed is: (" + initParams.getParamName() + ")." );
    }

    // initializatoin functions, to be over-ridden
    public void init(float x, float y, float z, int a, float b) { invalidInitError (); }
    
    // initializatoin functions, to be over-ridden
    public void init(float[] x) { invalidInitError (); }
    
    // initializatoin functions, to be over-ridden
    public void init(float x, float y, float z, int a) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(float x, float y) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(float x, float y, int a, int b, int c, int d)
    { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(float x, float y, int a, int b, int c, int d, int e)
    { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(float x, float y, int a, int b, int c)
    { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(float x, float y, int z) { invalidInitError (); }

    public void init(float a, float b, float c) { invalidInitError(); }

    public void init(float a, float b, float c, float d) { invalidInitError(); }

    public void init(float a, float b, float c, float d, float e, float f, float g) { invalidInitError(); }

    public void init(float a, float b, float c, int d, int e) { invalidInitError(); }

    public void init(float a, float b, float c, int d, int e, int f) { invalidInitError(); }

    public void init(float a, float b, float c, float d, int e, int f) { invalidInitError(); }

    public void init(float a, int b, float[] c, float[] d, int e) { invalidInitError(); }

    // initializatoin functions, to be over-ridden
    public void init(boolean b1) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int i1, boolean b1) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int i1, int i2, boolean b1) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int i1, int i2, float f) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int i1, int i2, float f1, float f2) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int i1, int i2, float f1, float f2, float f3) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int a, int b, float[][] c) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int a, float b[], float c[]) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int a, int b, float[][] c, float[] d) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int a, int b, int c, float[][] d, float[] e) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int a, boolean b, float c, float d, float[][] e, float[] f) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int a, int b, int c, float[] d) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int a, int b, int c, float[][] d) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int a, int b, int c, int d, float[][] e) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int a, int b, int c, int d, float[][] e, float[][] f) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int a, int b, float c, int d, float e) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int i1, int i2, int i3, int i4, int i5, float f) { invalidInitError (); }

    public void init(int i1, int i2, int i3, int i4, int i5, int i6, float f1, float f2) { invalidInitError (); }

    public void init(int i1, int i2, int i3, int i4, int i5, int i6, int i7, float f1, float f2) { invalidInitError (); }

    public void init(int i1, int i2, int i3, int i4, int i5, int i6, int i7) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int a, int b, int c, float d, float e) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int a, float b) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int a, int b, int c, float d, int e) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int a, int b, int c, int d, int e, int f, int g, float h){ invalidInitError (); }

    public void init (int n1, int n2, int n3,
                      int n4, int n5, int n6, int n7, int n8, 
                      int n9, int n10, float f) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(float x, float y, int z, int a) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init() { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int n) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int x, int y) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int x, float[] y) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int x, int[] y) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int x, float[][] y) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int x, int y, float[] z) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int x, int y, float[] z1, float[] z2) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int a, int b, int c, float[][] x, float[][] y) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(float x, int y) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int x, int y, int z) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int x, int y, int z, float f) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int x, int y, int z, int a) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int a, int b, int c, int d, int e) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int x, int y, int z, int a, float b) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(int x, int y, int z, int a, int b, int c) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(short s1, short s2, short s3) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(float f) { invalidInitError (); }

    // initialization functions, to be over-ridden
    public void init(char c) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init(String str) { invalidInitError (); }

    // initializatoin functions, to be over-ridden
    public void init (int i1,
                      int i2, 
                      int i3, 
                      int i4, 
                      int i5, 
                      int i6, 
                      int i7, 
                      int i8, 
                      int i9, 
                      float f)
    { 
        invalidInitError (); 
    }

    // initialization functions, to be over-ridden
    public void init (int i1,
                      int i2, 
                      int i3, 
                      int i4, 
                      int i5, 
                      int i6, 
                      int i7, 
                      int i8, 
                      int i9)
    { 
        invalidInitError (); 
    }
    
    
    // initializatoin functions, to be over-ridden
    public void init (int i1,
                      int i2, 
                      int i3, 
                      int i4, 
                      int i5, 
                      int i6, 
                      float f)
    { 
        invalidInitError (); 
    }
    
    public void init(Object o1) {
        invalidInitError();
    }
    
    
    public void init(Object o1, int i1) {
        invalidInitError();
    }

    public void init(int i1, int i2, Object o) {
        invalidInitError();
    }

    public void init(Object o1, Object o2) {
        invalidInitError();
    }
    
    public void init(Object o1, Object o2, Object o3) {
        invalidInitError();
    }
    
    // initializatoin functions, to be over-ridden
    public void init(ParameterContainer params) { invalidInitError (); }
    
    public static MessageStub MESSAGE_STUB;
    
    // initialize the MESSAGE_STUB
    static {
        MESSAGE_STUB = MessageStub.STUB;
    }
    
    // allSinks is used for scheduling the streaming graph
    public static LinkedList<Operator> allSinks;
    public static LinkedList<Operator> allSources;
    public static LinkedList<Operator> allFilters;
    public static HashSet<Channel> fullChannels;
    
    static {
        allSinks = new LinkedList<Operator> ();
        allSources = new LinkedList<Operator> ();
        allFilters = new LinkedList<Operator> ();
        fullChannels = new HashSet<Channel> ();
    }
    
    void addSink ()
    {
        allSinks.add (this);
    }
    
    void addSource ()
    {
        allSources.add (this);
    }
    
    void addFilter ()
    {
        allFilters.add (this);
    }
    
    /**
     * Returns whether or not anyone was able to execute.  If not, the
     * caller should quit because noone else is going to execute in
     * the future.
     */
    boolean runSinks ()
    {
        ListIterator<Operator> iter;
        
        if (!allSinks.isEmpty ())
            iter = allSinks.listIterator ();
        else
            iter = allFilters.listIterator ();

        // go over all the sinks
        int executions = 0;
        while (iter.hasNext ())
            {
                Operator sink;
                sink = iter.next ();
                assert sink != null;

                // get a snapshot of how much the sources have executed.
                // We will execute a sink until some source executes at
                // least once.  This is an effort to drain intermediate
                // buffers between sink executions -- otherwise it is not
                // clear in -nosched mode how often to execute sinks with
                // mismatching steady-state multiplicities.
                int beforeSink = getSourceExecs();
                int afterSink;
                do {
                    try {
                        sink.doWork();
                        executions++;
                    } catch (NoPushPopException e) {
                        // this indicates that an upstream filter has no
                        // more output to offer to the given sink, so we
                        // should switch to a different sink.
                        break;
                    }
                    afterSink = getSourceExecs();
                } while (afterSink == beforeSink);
            }

        // return whether or not someone executed
        return executions>0;
    }

    /**
     * Returns the total number of times that the sources have
     * executed.
     */
    int getSourceExecs() {
        int sourceExecs = 0;
        // can't use iterator here for performance reasons -- it is
        // the inner loop when the sinks are filewriters
        for (int i=0; i<allSources.size(); i++) {
            sourceExecs += allSources.get(i).getWorkExecutions();
        }
        return sourceExecs;
    }

    void drainChannels ()
    {
        while (!fullChannels.isEmpty ())
            {
                // empty any full channels:
                Iterator<Channel> fullChannel;
                fullChannel = fullChannels.iterator ();
            
                Channel ch = fullChannel.next ();
                assert ch != null;
            
                ch.getSink().doWork();
            }
    }


    void addFullChannel (Channel channel)
    {
        fullChannels.add (channel);
    }

    void removeFullChannel (Channel channel)
    {
        fullChannels.remove (channel);
    }

    public static void passOneData (Channel from, Channel to)
    {
        Class type = from.getType ();
        assert type == to.getType ();

        if (type == Integer.TYPE)
            {
                to.pushInt (from.popInt ());
            } else
                if (type == Short.TYPE)
                    {
                        to.pushShort (from.popShort ());
                    } else
                        if (type == Character.TYPE)
                            {
                                to.pushChar (from.popChar ());
                            } else
                                if (type == Float.TYPE)
                                    {
                                        to.pushFloat (from.popFloat ());
                                    } else
                                        if (type == Double.TYPE)
                                            {
                                                to.pushDouble (from.popDouble ());
                                            } else {
                                                to.push (from.pop ());
                                            }
    }

    public static void duplicateOneData (Channel from, Channel [] to)
    {
        Class type = from.getType ();
        assert to != null && type == to[0].getType ();

        if (type == Integer.TYPE)
            {
                int data = from.popInt ();

                int indx;
                for (indx = to.length - 1; indx >= 0; indx--)
                    {
                        to [indx] .pushInt (data);
                    }
            } else
                if (type == Short.TYPE)
                    {
                        short data = from.popShort ();

                        int indx;
                        for (indx = to.length - 1; indx >= 0; indx--)
                            {
                                to [indx] .pushShort (data);
                            }
                    } else
                        if (type == Character.TYPE)
                            {
                                char data = from.popChar ();

                                int indx;
                                for (indx = to.length - 1; indx >= 0; indx--)
                                    {
                                        to [indx] .pushChar (data);
                                    }
                            } else
                                if (type == Float.TYPE)
                                    {
                                        float data = from.popFloat ();

                                        int indx;
                                        for (indx = to.length - 1; indx >= 0; indx--)
                                            {
                                                to [indx] .pushFloat (data);
                                            }
                                    } else
                                        if (type == Double.TYPE)
                                            {
                                                double data = from.popDouble ();

                                                int indx;
                                                for (indx = to.length - 1; indx >= 0; indx--)
                                                    {
                                                        to [indx] .pushDouble (data);
                                                    }
                                            } else {
                                                Object data = from.pop ();

                                                int indx;
                                                for (indx = to.length - 1; indx >= 0; indx--)
                                                    {
                                                        to [indx] .push (data);
                                                    }
                                            }
    }

    // send a message to a handler that returns <pre>stub</pre> within <pre>delay</pre>
    // units of my input/output (to be specified more clearly...)
    public void sendMessage(MessageStub stub, int delay) {}

    // send a message to a handler that returns <pre>stub</pre> at the earliest
    // convenient time.
    public void sendMessage(MessageStub stub) {}

    protected static class MessageStub
    {
        private static MessageStub STUB = new MessageStub();
        private MessageStub() {}
    }

    // a prototype work function
    void work () { }

    public void doWork() {
        prepareToWork();
        work();
        cleanupWork();
    }

    // this function will take care of all appropriate calls to init:
    void setupOperator ()
    {
        // don't re-initialize
        if (initialized) return;

        callInit();
        connectGraph ();
        initialized = true;
    }

    // this function calls the init function, based on state in
    // the object
    protected void callInit ()
    {
        assert initParams != null;

        if(initParams.getParamName().equals(("int-int-float-float")))
            init (initParams.getIntParam("i1"),
                  initParams.getIntParam("i2"),
                  initParams.getFloatParam("f1"),
                  initParams.getFloatParam("f2"));
        else
            if(initParams.getParamName().equals(("int-int-float-float-float")))
                init (initParams.getIntParam("i1"),
                      initParams.getIntParam("i2"),
                      initParams.getFloatParam("f1"),
                      initParams.getFloatParam("f2"),
                      initParams.getFloatParam("f3"));
            else
                if(initParams.getParamName().equals(("int-int-float")))
                    init (initParams.getIntParam("i1"),
                          initParams.getIntParam("i2"),
                          initParams.getFloatParam("f"));
                else
                    if(initParams.getParamName().equals(("boolean")))
                        init (initParams.getBoolParam("b1"));
                    else
                        if(initParams.getParamName().equals(("int-boolean")))
                            init (initParams.getIntParam("i1"),
                                  initParams.getBoolParam("b1"));
                        else
                            if(initParams.getParamName().equals(("int-int-boolean")))
                                init (initParams.getIntParam("i1"),
                                      initParams.getIntParam("i2"),
                                      initParams.getBoolParam("b1"));
                            else
                                if(initParams.getParamName().equals("int-int-int-int-float"))
                                    init (initParams.getIntParam("x"),
                                          initParams.getIntParam("y"),
                                          initParams.getIntParam("z"),
                                          initParams.getIntParam("a"),
                                          initParams.getFloatParam("b"));
                                else
                                    if(initParams.getParamName().equals("int-int-int-int-int-float"))
                                        init (initParams.getIntParam("i1"),
                                              initParams.getIntParam("i2"),
                                              initParams.getIntParam("i3"),
                                              initParams.getIntParam("i4"),
                                              initParams.getIntParam("i5"),
                                              initParams.getFloatParam("f"));
                                    else

                                        if(initParams.getParamName().equals("int-int-int-int-int-int-float-float"))
                                            init (initParams.getIntParam("i1"),
                                                  initParams.getIntParam("i2"),
                                                  initParams.getIntParam("i3"),
                                                  initParams.getIntParam("i4"),
                                                  initParams.getIntParam("i5"),
                                                  initParams.getIntParam("i6"),
                                                  initParams.getFloatParam("f1"),
                                                  initParams.getFloatParam("f2"));
                                        else
                                            if(initParams.getParamName().equals("int-int-int-int-int-int-int-float-float"))
                                                init (initParams.getIntParam("i1"),
                                                      initParams.getIntParam("i2"),
                                                      initParams.getIntParam("i3"),
                                                      initParams.getIntParam("i4"),
                                                      initParams.getIntParam("i5"),
                                                      initParams.getIntParam("i6"),
                                                      initParams.getIntParam("i7"),
                                                      initParams.getFloatParam("f1"),
                                                      initParams.getFloatParam("f2"));
                                            else
                                                if(initParams.getParamName().equals("int-int-int-int-int-int-int"))
                                                    init (initParams.getIntParam("i1"),
                                                          initParams.getIntParam("i2"),
                                                          initParams.getIntParam("i3"),
                                                          initParams.getIntParam("i4"),
                                                          initParams.getIntParam("i5"),
                                                          initParams.getIntParam("i6"),
                                                          initParams.getIntParam("i7"));
                                                else
                                                    if(initParams.getParamName().equals("int-int-int-int-float[][]-float[][]"))
                                                        init (initParams.getIntParam("a"),
                                                              initParams.getIntParam("b"),
                                                              initParams.getIntParam("c"),
                                                              initParams.getIntParam("d"),
                                                              (float[][])initParams.getObjParam("e"),
                                                              (float[][])initParams.getObjParam("f"));
                                                    else
                                                        if(initParams.getParamName().equals("int-int-int-int-int-int"))
                                                            init (initParams.getIntParam("x"),
                                                                  initParams. getIntParam("y"),
                                                                  initParams.getIntParam("z"),
                                                                  initParams.getIntParam("a"),
                                                                  initParams.getIntParam("b"),
                                                                  initParams.getIntParam("c"));
                                                        else
                                                            if(initParams.getParamName().equals("float-float-float-int-int-int"))
                                                                init (initParams.getFloatParam("a"),
                                                                      initParams. getFloatParam("b"),
                                                                      initParams.getFloatParam("c"),
                                                                      initParams.getIntParam("d"),
                                                                      initParams.getIntParam("e"),
                                                                      initParams.getIntParam("f"));
                                                            else
                                                                if(initParams.getParamName().equals("float-float-float-int-int"))
                                                                    init (initParams.getFloatParam("a"),
                                                                          initParams. getFloatParam("b"),
                                                                          initParams.getFloatParam("c"),
                                                                          initParams.getIntParam("d"),
                                                                          initParams.getIntParam("e"));
                                                                else
                                                                    if(initParams.getParamName().equals("int-float"))
                                                                        init (initParams.getIntParam("a"),
                                                                              initParams.getFloatParam("b"));
                                                                    else
                                                                        if(initParams.getParamName().equals("int-float[]"))
                                                                            init (initParams.getIntParam("a"),
                                                                                  (float[])initParams.getObjParam("b"));
                                                                        else
                                                                            if(initParams.getParamName().equals("int-int[]"))
                                                                                init (initParams.getIntParam("a"),
                                                                                      (int[])initParams.getObjParam("b"));
                                                                            else
                                                                                if(initParams.getParamName().equals("float[]"))
                                                                                    init ((float[])initParams.getObjParam("a"));
                                                                                else
                                                                                    if(initParams.getParamName().equals("int-float[][]"))
                                                                                        init (initParams.getIntParam("a"),
                                                                                              (float[][])initParams.getObjParam("b"));
                                                                                    else
                                                                                        if(initParams.getParamName().equals("float-int"))
                                                                                            init (initParams.getFloatParam("a"),
                                                                                                  initParams.getIntParam("b"));
                                                                                        else
                                                                                            if(initParams.getParamName().equals("float-float"))
                                                                                                init (initParams.getFloatParam("a"),
                                                                                                      initParams.getFloatParam("b"));
                                                                                            else
                                                                                                if(initParams.getParamName().equals("float-float-int"))
                                                                                                    init (initParams.getFloatParam("x1"),
                                                                                                          initParams.getFloatParam("y1"),
                                                                                                          initParams.getIntParam("z1"));
                                                                                                else
                                                                                                    if(initParams.getParamName().equals("float-float-float-float"))
                                                                                                        init (initParams.getFloatParam("a"),
                                                                                                              initParams.getFloatParam("b"),
                                                                                                              initParams.getFloatParam("c"),
                                                                                                              initParams.getFloatParam("d"));
                                                                                                    else
                                                                                                        if(initParams.getParamName().equals("float-float-float-float-float-float-float"))
                                                                                                            init (initParams.getFloatParam("a"),
                                                                                                                  initParams.getFloatParam("b"),
                                                                                                                  initParams.getFloatParam("c"),
                                                                                                                  initParams.getFloatParam("d"),
                                                                                                                  initParams.getFloatParam("e"),
                                                                                                                  initParams.getFloatParam("f"),
                                                                                                                  initParams.getFloatParam("g"));
                                                                                                        else
                                                                                                            if(initParams.getParamName().equals("float-float-float-float-int-int"))
                                                                                                                init (initParams.getFloatParam("a"),
                                                                                                                      initParams.getFloatParam("b"),
                                                                                                                      initParams.getFloatParam("c"),
                                                                                                                      initParams.getFloatParam("d"),
                                                                                                                      initParams.getIntParam("e"),
                                                                                                                      initParams.getIntParam("f"));
                                                                                                            else
                                                                                                                if(initParams.getParamName().equals("float-float-int-int"))
                                                                                                                    init (initParams.getFloatParam("x3"),
                                                                                                                          initParams.getFloatParam("y3"),
                                                                                                                          initParams.getIntParam("z3"),
                                                                                                                          initParams.getIntParam("a3"));
                                                                                                                else
                                                                                                                    if(initParams.getParamName().equals("float-float-int-int-int-int"))
                                                                                                                        init(initParams.getFloatParam("f1"),
                                                                                                                             initParams.getFloatParam("f2"),
                                                                                                                             initParams.getIntParam("i1"),
                                                                                                                             initParams.getIntParam("i2"),
                                                                                                                             initParams.getIntParam("i3"),
                                                                                                                             initParams.getIntParam("i4"));
                                                                                                                    else
                                                                                                                        if(initParams.getParamName().equals("float-float-int-int-int-int-int"))
                                                                                                                            init(initParams.getFloatParam("f1"),
                                                                                                                                 initParams.getFloatParam("f2"),
                                                                                                                                 initParams.getIntParam("i1"),
                                                                                                                                 initParams.getIntParam("i2"),
                                                                                                                                 initParams.getIntParam("i3"),
                                                                                                                                 initParams.getIntParam("i4"),
                                                                                                                                 initParams.getIntParam("i5"));
                                                                                                                        else
                                                                                                                            if(initParams.getParamName().equals("float-float-int-int-int"))
                                                                                                                                init(initParams.getFloatParam("x3"),
                                                                                                                                     initParams.getFloatParam("y3"),
                                                                                                                                     initParams.getIntParam("z3"),
                                                                                                                                     initParams.getIntParam("a3"),
                                                                                                                                     initParams.getIntParam("b3"));
                                                                                                                            else
                                                                                                                                if(initParams.getParamName().equals("float-float-float-int-float"))
                                                                                                                                    init (initParams.getFloatParam("x2"),
                                                                                                                                          initParams.getFloatParam("y2"),
                                                                                                                                          initParams.getFloatParam("z2"),
                                                                                                                                          initParams.getIntParam("a2"),
                                                                                                                                          initParams.getFloatParam("b2"));
                                                                                                                                else
                                                                                                                                    if(initParams.getParamName().equals("float-int-float[]-float[]-int"))
                                                                                                                                        init (initParams.getFloatParam("f1"),
                                                                                                                                              initParams.getIntParam("i1"),
                                                                                                                                              (float[])initParams.getObjParam("f2"),
                                                                                                                                              (float[])initParams.getObjParam("f3"),
                                                                                                                                              initParams.getIntParam("i2"));
                                                                                                                                    else
                                                                                                                                        if(initParams.getParamName().equals("int-int-float-int-float"))
                                                                                                                                            init (initParams.getIntParam("a"),
                                                                                                                                                  initParams.getIntParam("b"),
                                                                                                                                                  initParams.getFloatParam("c"),
                                                                                                                                                  initParams.getIntParam("d"),
                                                                                                                                                  initParams.getFloatParam("e"));
                                                                                                                                        else
                                                                                                                                            if(initParams.getParamName().equals("int-int-int-float-float"))
                                                                                                                                                init (initParams.getIntParam("a"),
                                                                                                                                                      initParams.getIntParam("b"),
                                                                                                                                                      initParams.getIntParam("c"),
                                                                                                                                                      initParams.getFloatParam("d"),
                                                                                                                                                      initParams.getFloatParam("e"));
                                                                                                                                            else
                                                                                                                                                if(initParams.getParamName().equals("int-int-int-float-int"))
                                                                                                                                                    init (initParams.getIntParam("a"),
                                                                                                                                                          initParams.getIntParam("b"),
                                                                                                                                                          initParams.getIntParam("c"),
                                                                                                                                                          initParams.getFloatParam("d"),
                                                                                                                                                          initParams.getIntParam("e"));
                                                                                                                                                else
                                                                                                                                                    if(initParams.getParamName().equals("int-int-int-int-int"))
                                                                                                                                                        init (initParams.getIntParam("a"),
                                                                                                                                                              initParams.getIntParam("b"),
                                                                                                                                                              initParams.getIntParam("c"),
                                                                                                                                                              initParams.getIntParam("d"),
                                                                                                                                                              initParams.getIntParam("e"));
                                                                                                                                                    else
                                                                                                                                                        if(initParams.getParamName().equals("int-int-int-int-int-int-float"))
                                                                                                                                                            init (initParams.getIntParam("i1"),
                                                                                                                                                                  initParams.getIntParam("i2"),
                                                                                                                                                                  initParams.getIntParam("i3"),
                                                                                                                                                                  initParams.getIntParam("i4"),
                                                                                                                                                                  initParams.getIntParam("i5"),
                                                                                                                                                                  initParams.getIntParam("i6"),
                                                                                                                                                                  initParams.getFloatParam("f"));
                                                                                                                                                        else
                                                                                                                                                            if(initParams.getParamName().equals("int-int-float[]"))
                                                                                                                                                                init (initParams.getIntParam("a"),
                                                                                                                                                                      initParams.getIntParam("b"),
                                                                                                                                                                      (float[])initParams.getObjParam("c"));
                                                                                                                                                            else
                                                                                                                                                                if(initParams.getParamName().equals("int-int-int-float[]"))
                                                                                                                                                                    init (initParams.getIntParam("a"),
                                                                                                                                                                          initParams.getIntParam("b"),
                                                                                                                                                                          initParams.getIntParam("c"),
                                                                                                                                                                          (float[])initParams.getObjParam("d"));
                                                                                                                                                                else
                                                                                                                                                                    if(initParams.getParamName().equals("int-float[]-float[]"))
                                                                                                                                                                        init (initParams.getIntParam("a"),
                                                                                                                                                                              (float[])initParams.getObjParam("c"),
                                                                                                                                                                              (float[])initParams.getObjParam("d"));
                                                                                                                                                                    else
                                                                                                                                                                        if(initParams.getParamName().equals("int-int-float[]-float[]"))
                                                                                                                                                                            init (initParams.getIntParam("a"),
                                                                                                                                                                                  initParams.getIntParam("b"),
                                                                                                                                                                                  (float[])initParams.getObjParam("c"),
                                                                                                                                                                                  (float[])initParams.getObjParam("d"));
                                                                                                                                                                        else
                                                                                                                                                                            if(initParams.getParamName().equals("int-int-int-float[][]-float[][]"))
                                                                                                                                                                                init (initParams.getIntParam("a"),
                                                                                                                                                                                      initParams.getIntParam("b"),
                                                                                                                                                                                      initParams.getIntParam("c"),
                                                                                                                                                                                      (float[][])initParams.getObjParam("x"),
                                                                                                                                                                                      (float[][])initParams.getObjParam("y"));
                                                                                                                                                                            else
                                                                                                                                                                                if(initParams.getParamName().equals("int-int-float[][]"))
                                                                                                                                                                                    init (initParams.getIntParam("a"),
                                                                                                                                                                                          initParams.getIntParam("b"),
                                                                                                                                                                                          (float[][])initParams.getObjParam("c"));
                                                                                                                                                                                else
                                                                                                                                                                                    if(initParams.getParamName().equals("int-int-float[][]-float[]"))
                                                                                                                                                                                        init(initParams.getIntParam("a"),
                                                                                                                                                                                             initParams.getIntParam("b"),
                                                                                                                                                                                             (float[][])initParams.getObjParam("c"),
                                                                                                                                                                                             (float[])initParams.getObjParam("d"));
                                                                                                                                                                                    else
                                                                                                                                                                                        if(initParams.getParamName().equals("int-int-int-float[][]-float[]"))
                                                                                                                                                                                            init(initParams.getIntParam("a"),
                                                                                                                                                                                                 initParams.getIntParam("b"),
                                                                                                                                                                                                 initParams.getIntParam("c"),
                                                                                                                                                                                                 (float[][])initParams.getObjParam("d"),
                                                                                                                                                                                                 (float[])initParams.getObjParam("e"));
                                                                                                                                                                                        else
                                                                                                                                                                                            if(initParams.getParamName().equals("int-boolean-float-float-float[][]-float[]"))
                                                                                                                                                                                                init(initParams.getIntParam("a"),
                                                                                                                                                                                                     initParams.getBoolParam("b"),
                                                                                                                                                                                                     initParams.getFloatParam("c"),
                                                                                                                                                                                                     initParams.getFloatParam("d"),
                                                                                                                                                                                                     (float[][])initParams.getObjParam("e"),
                                                                                                                                                                                                     (float[])initParams.getObjParam("f"));
                                                                                                                                                                                            else
                                                                                                                                                                                                if(initParams.getParamName().equals("int-int-int-float"))
                                                                                                                                                                                                    init(initParams.getIntParam("i1"),
                                                                                                                                                                                                         initParams.getIntParam("i2"),
                                                                                                                                                                                                         initParams.getIntParam("i3"),
                                                                                                                                                                                                         initParams.getFloatParam("f1"));
                                                                                                                                                                                                else
                                                                                                                                                                                                    if(initParams.getParamName().equals("int-int-int-float[][]"))
                                                                                                                                                                                                        init (initParams.getIntParam("a"),
                                                                                                                                                                                                              initParams.getIntParam("b"),
                                                                                                                                                                                                              initParams.getIntParam("c"),
                                                                                                                                                                                                              (float[][])initParams.getObjParam("d"));
                                                                                                                                                                                                    else
                                                                                                                                                                                                        if(initParams.getParamName().equals("int-int-int-int-float[][]"))
                                                                                                                                                                                                            init (initParams.getIntParam("a"),
                                                                                                                                                                                                                  initParams.getIntParam("b"),
                                                                                                                                                                                                                  initParams.getIntParam("c"),
                                                                                                                                                                                                                  initParams.getIntParam("d"),
                                                                                                                                                                                                                  (float[][])initParams.getObjParam("e"));
                                                                                                                                                                                                        else
                                                                                                                                                                                                            if(initParams.getParamName().equals("int-int-int-int-int-int-int-float"))
                                                                                                                                                                                                                init (initParams.getIntParam("x"),
                                                                                                                                                                                                                      initParams.getIntParam("y"),
                                                                                                                                                                                                                      initParams.getIntParam("z"),
                                                                                                                                                                                                                      initParams.getIntParam("a"),
                                                                                                                                                                                                                      initParams.getIntParam("b"),
                                                                                                                                                                                                                      initParams.getIntParam("c"),
                                                                                                                                                                                                                      initParams.getIntParam("d"),
                                                                                                                                                                                                                      initParams.getFloatParam("f"));
                                                                                                                                                                                                            else
                                                                                                                                                                                                                if(initParams.getParamName().equals("int-int-int-int-int-int-int-int-int-float"))
                                                                                                                                                                                                                    init (initParams.getIntParam("i1"),
                                                                                                                                                                                                                          initParams.getIntParam("i2"),
                                                                                                                                                                                                                          initParams.getIntParam("i3"),
                                                                                                                                                                                                                          initParams.getIntParam("i4"),
                                                                                                                                                                                                                          initParams.getIntParam("i5"),
                                                                                                                                                                                                                          initParams.getIntParam("i6"),
                                                                                                                                                                                                                          initParams.getIntParam("i7"),
                                                                                                                                                                                                                          initParams.getIntParam("i8"),
                                                                                                                                                                                                                          initParams.getIntParam("i9"),
                                                                                                                                                                                                                          initParams.getFloatParam("f"));
                                                                                                                                                                                                                else
                                                                                                                                                                                                                    if(initParams.getParamName().equals("int-int-int-int-int-int-int-int-int"))
                                                                                                                                                                                                                        init (initParams.getIntParam("i1"),
                                                                                                                                                                                                                              initParams.getIntParam("i2"),
                                                                                                                                                                                                                              initParams.getIntParam("i3"),
                                                                                                                                                                                                                              initParams.getIntParam("i4"),
                                                                                                                                                                                                                              initParams.getIntParam("i5"),
                                                                                                                                                                                                                              initParams.getIntParam("i6"),
                                                                                                                                                                                                                              initParams.getIntParam("i7"),
                                                                                                                                                                                                                              initParams.getIntParam("i8"),
                                                                                                                                                                                                                              initParams.getIntParam("i9"));
                                                                                                                                                                                                                    else
                                                                                                                                                                                                                        if(initParams.getParamName().equals("int-int-int-int-int-int-int-int-int-int-float"))
                                                                                                                                                                                                                            init (initParams.getIntParam("i1"),
                                                                                                                                                                                                                                  initParams.getIntParam("i2"),
                                                                                                                                                                                                                                  initParams.getIntParam("i3"),
                                                                                                                                                                                                                                  initParams.getIntParam("i4"),
                                                                                                                                                                                                                                  initParams.getIntParam("i5"),
                                                                                                                                                                                                                                  initParams.getIntParam("i6"),
                                                                                                                                                                                                                                  initParams.getIntParam("i7"),
                                                                                                                                                                                                                                  initParams.getIntParam("i8"),
                                                                                                                                                                                                                                  initParams.getIntParam("i9"),
                                                                                                                                                                                                                                  initParams.getIntParam("i10"),
                                                                                                                                                                                                                                  initParams.getFloatParam("f"));
                                                                                                                                                                                                                        else
                                                                                                                                                                                                                            if(initParams.getParamName().equals("float-float-float-int"))
                                                                                                                                                                                                                                init (initParams.getFloatParam("x2"),
                                                                                                                                                                                                                                      initParams.getFloatParam("y2"),
                                                                                                                                                                                                                                      initParams.getFloatParam("z2"),
                                                                                                                                                                                                                                      initParams.getIntParam("a2"));
                                                                                                                                                                                                                            else if(initParams.getParamName().equals("Object"))
                                                                                                                                                                                                                                init(initParams.getObjectParam("o1"));
                                                                                                                                                                                                                            else if(initParams.getParamName().equals("Object-int"))
                                                                                                                                                                                                                                init(initParams.getObjectParam("o1"),
                                                                                                                                                                                                                                     initParams.getIntParam("i1"));

                                                                                                                                                                                                                            else if(initParams.getParamName().equals("int-int-Object"))
                                                                                                                                                                                                                                init(initParams.getIntParam("i1"),
                                                                                                                                                                                                                                     initParams.getIntParam("i2"),
                                                                                                                                                                                                                                     initParams.getObjectParam("o1"));

                                                                                                                                                                                                                            else if(initParams.getParamName().equals("Object-Object"))
                                                                                                                                                                                                                                init(initParams.getObjectParam("o1"),
                                                                                                                                                                                                                                     initParams.getObjectParam("o2"));
                                                                                                                                                                                                                            else if(initParams.getParamName().equals("Object-Object-Object"))
                                                                                                                                                                                                                                init(initParams.getObjectParam("o1"),
                                                                                                                                                                                                                                     initParams.getObjectParam("o2"),
                                                                                                                                                                                                                                     initParams.getObjectParam("o3")); else
                                                                                                                                                                                                                                         if (initParams.getParamName ().equals("int-int")) init (initParams.getIntParam ("x"), initParams.getIntParam ("y")); else
                                                                                                                                                                                                                                             if (initParams.getParamName ().equals("int-int-int")) init (initParams.getIntParam ("x"), initParams.getIntParam ("y"), initParams.getIntParam ("z")); else
                                                                                                                                                                                                                                                 if (initParams.getParamName ().equals("int-int-int-int")) init (initParams.getIntParam ("x"), initParams.getIntParam ("y"), initParams.getIntParam ("z"), initParams.getIntParam ("a")); else
                                                                                                                                                                                                                                                     if (initParams.getParamName ().equals("float-float-float")) init (initParams.getFloatParam ("a"), initParams.getFloatParam ("b"), initParams.getFloatParam ("c")); else
                                                                                                                                                                                                                                                         if (initParams.getParamName ().equals("short-short-short")) init (initParams.getShortParam ("s1"), initParams.getShortParam ("s2"), initParams.getShortParam ("s3")); else
                                                                                                                                                                                                                                                             if (initParams.getParamName ().equals("")) init (); else
                                                                                                                                                                                                                                                                 if (initParams.getParamName ().equals("int")) init (initParams.getIntParam ("n")); else
                                                                                                                                                                                                                                                                     if (initParams.getParamName ().equals("float")) init (initParams.getFloatParam ("f")); else
                                                                                                                                                                                                                                                                         if (initParams.getParamName ().equals("char")) init (initParams.getCharParam ("c")); else
                                                                                                                                                                                                                                                                             if (initParams.getParamName ().equals("String")) init (initParams.getStringParam ("str")); else
                                                                                                                                                                                                                                                                                 if (initParams.getParamName ().equals("ParameterContainer")) init ((ParameterContainer) initParams.getObjParam ("params"));
                                                                                                                                                                                                                                                                                 else {
                                                                                                                                                                                                                                                                                     // labels don't match - print an error
                                                                                                                                                                                                                                                                                     ERROR ("You didn't provide a correct if-else statement in setupOperator.\nPlease read streams/docs/implementation-notes/library-init-functions.txt for instructions.\n(paramName=" + initParams.getParamName() + ")");
                                                                                                                                                                                                                                                                                 }
    }

    public void connectGraph ()
    {
        throw new UnsupportedOperationException();
    }

    // ------------------------------------------------------------------
    // ------------------ all graph related functions -------------------
    // ------------------------------------------------------------------

    // get an IO field (input or output)
    // returns null if none present
    Channel[] getIOFields (String fieldName)
    {
        assert fieldName == "inputChannel" || fieldName == "outputChannel";

        Channel fieldsInstance [] = null;

        try
            {
                Class<? extends Operator> thisClass = this.getClass ();
                assert thisClass != null;

                Field ioField;
                ioField  = thisClass.getField (fieldName);

                Object fieldValue = ioField.get (this);

                if (ioField.getType ().isArray ())
                    {
                        fieldsInstance = (Channel []) fieldValue;
                    } else {
                        fieldsInstance = new Channel [1];
                        fieldsInstance [0] = (Channel) fieldValue;

                        if (fieldsInstance [0] == null) fieldsInstance = null;
                    }
            }
        catch (NoSuchFieldException noError)
            {
                // do not do anything here, this is NOT an error!
            }
        catch (Throwable error)
            {
                // this is all the other errors:
                error.getClass ();
                assert false : error.toString ();
            }

        return fieldsInstance;
    }

    Channel getIOField (String fieldName, int fieldIndex)
    {
        Channel field = null;

        {
            Channel fieldInstance[];
            fieldInstance = getIOFields (fieldName);

            if (fieldInstance != null)
                {
                    assert fieldInstance.length > fieldIndex;
                    field = fieldInstance [fieldIndex];
                }
        }

        return field;
    }

    void setIOField (String fieldName, int fieldIndex, Channel newChannel)
    {
        assert fieldName == "inputChannel" || fieldName == "outputChannel";

        Channel fieldsInstance [];

        try
            {
                Class<? extends Operator> thisClass = this.getClass ();
                assert thisClass != null;

                Field ioField;
                ioField  = thisClass.getField (fieldName);

                if (ioField.getType () == newChannel.getClass ())
                    {
                        assert fieldIndex == 0;
                        ioField.set (this, newChannel);
                    } else {
                        fieldsInstance = (Channel []) ioField.get (this);
                        assert fieldsInstance != null;
                        assert fieldsInstance.length > fieldIndex;

                        fieldsInstance [fieldIndex] = newChannel;
                    }

            }
        catch (Throwable error)
            {
                // this is all the other errors:
                assert false : error.toString ();
            }
    }


    static int[][][] copyIntArray3D(int[][][] input) {
        // according to streamit semantics, assume arrays are rectangular
        int[][][] result = new int[input.length][input[0].length][input[0][0].length];
        for (int i=0; i<input.length; i++) {
            for (int j=0; j<input[0].length; j++) {
                for (int k = 0; k<input[0][0].length; k++) {
                    result[i][j][k] = input[i][j][k];
                }
            }
        }
        return result;
    }        

    static float[][] copyFloatArray2D(float[][] input) {
        // according to streamit semantics, assume arrays are rectangular
        float[][] result = new float[input.length][input[0].length];
        for (int i=0; i<input.length; i++) {
            for (int j=0; j<input[0].length; j++) {
                result[i][j] = input[i][j];
            }
        }
        return result;
    }

    static int[][] copyIntArray2D(int[][] input) {
        // according to streamit semantics, assume arrays are rectangular
        int[][] result = new int[input.length][input[0].length];
        for (int i=0; i<input.length; i++) {
            for (int j=0; j<input[0].length; j++) {
                result[i][j] = input[i][j];
            }
        }
        return result;
    }

    static float[] copyFloatArray1D(float[] input) {
        // according to streamit semantics, assume arrays are rectangular
        float[] result = new float[input.length];
        for (int i=0; i<input.length; i++) {
            result[i] = input[i];
        }
        return result;
    }

    static int[] copyIntArray1D(int[] input) {
        // according to streamit semantics, assume arrays are rectangular
        int[] result = new int[input.length];
        for (int i=0; i<input.length; i++) {
            result[i] = input[i];
        }
        return result;
    }

    /**
     * This is what shows up on nodes in the dot graph output of the
     * library.
     */
    public String toString() {
        return getClass().getName();
    }

}
