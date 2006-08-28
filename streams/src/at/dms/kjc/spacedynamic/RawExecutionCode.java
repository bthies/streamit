package at.dms.kjc.spacedynamic;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.FlatVisitor;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.util.*;
import at.dms.kjc.iterator.*;
import at.dms.util.Utils;
import java.util.List;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.HashMap;
import java.io.*;
import at.dms.compiler.*;
import at.dms.kjc.sir.lowering.*;
import java.util.Hashtable;
import java.math.BigInteger;

/**
 * This pass creates the SIR necessary for each filter of the StreamGraph to
 * execute on Raw. It creates SIR to handle the communication for filters by
 * possibly creating a buffer or a circular buffer (see
 * BufferedCommmunication.java) or no buffer (See DirectionCommunication.java).
 * 
 * @author mgordon
 * 
 */
public class RawExecutionCode extends at.dms.util.Utils implements FlatVisitor,
        Constants {


    /** if this is true, then inline the work function calls (for each of the 
	code generation schemes), if work remains a function and is called.
    */
    public static boolean INLINE_WORK = true;


    /** * fields for the var names we introduce ** */
    public static String recvBuffer = "__RECVBUFFER__";

    public static String recvBufferSize = "__RECVBUFFERSIZE__";

    public static String recvBufferBits = "__RECVBUFFERBITS__";

    // the output buffer for ratematching
    public static String sendBuffer = "__SENDBUFFER__";

    public static String sendBufferIndex = "__SENDBUFFERINDEX__";

    public static String rateMatchSendMethod = "__RATEMATCHSEND__";

    // recvBufferIndex points to the beginning of the tape
    public static String recvBufferIndex = "__RECVBUFFERINDEX__";

    // recvIndex points to the end of the tape
    public static String recvIndex = "_RECVINDEX__";

    public static String simpleIndex = "__SIMPLEINDEX__";

    public static String exeIndex = "__EXEINDEX__";

    public static String exeIndex1 = "__EXEINDEX__1__";

    public static String ARRAY_INDEX = "__ARRAY_RECEIVE_INDEX__";

    public static String ARRAY_COPY = "__ARRAY_COPY__";

    public static String initSchedFunction = "__RAWINITSCHED__";

    public static String steadySchedFunction = "__RAWSTEADYSCHED__";

    public static String receiveMethod = "static_receive_to_mem";

    public static String structReceivePrefix = "__popPointer";

    public static String structReceivePrefixStatic = structReceivePrefix
        + "Static";

    public static String structReceivePrefixDynamic = structReceivePrefix
        + "Dynamic";

    /** the name of the pop mehtod for each filter when the filter has
     * dynamic input and we are not inlining 
     */
    public static String popDynamic = "__pop_dynamic__";
    /** the name of the peek method for each filter when the filter has
     * dynamic input and we are not inlining the method 
     * */
    public static String peekDynamic = "__peek_dynamic__";
    
    public static String arrayReceiveMethod = "__array_receive__";

    public static String rawMain = "__RAWMAIN__";

    /** the current SSG we are traversing * */
    private SpdStaticStreamGraph ssg;

    /** the layout for the ssg we are traversing * */
    private Layout layout;

    /***************************************************************************
     * These next fields are set by calculateItems() see my thesis for a better
     * explanation
     **************************************************************************/

    /** number of items to receive between preWork() and work() * */
    private int bottomPeek = 0;

    /** number of times the filter fires in the init schedule * */
    private int initFire = 0;

    /** number of items to receive after initialization */
    private int remaining = 0;

    public RawExecutionCode(SpdStaticStreamGraph ssg) {
        this.ssg = ssg;
        this.layout = ((SpdStreamGraph)ssg.getStreamGraph()).getLayout();
    }

    public static void doit(SpdStreamGraph streamGraph) {

        for (int i = 0; i < streamGraph.getStaticSubGraphs().length; i++) {
            streamGraph.getStaticSubGraphs()[i].getTopLevel().accept(
                                                                     new RawExecutionCode((SpdStaticStreamGraph)streamGraph.getStaticSubGraphs()[i]),
                                                                     null, true);
            /*
             * SIRPrinter printer1 = new SIRPrinter("sir" +
             * streamGraph.getStaticSubGraphs()[i].toString() + ".out");
             * IterFactory.createFactory().createIter(streamGraph.getStaticSubGraphs()[i].getTopLevelSIR()).accept(printer1);
             * printer1.close();
             */
        }
    }

    public void visitNode(FlatNode node) {
        if (node.isFilter()) {
            SIRFilter filter = (SIRFilter) node.contents;

            // remove any multi-pop statement, as communication code
            // we are about to generate does not handle it
            RemoveMultiPops.doit(ssg.getTopLevelSIR());

            if (!Layout.assignToATile(node))
                return;
            //calculate various stats of the filter            
            calculateItems(filter);

            //System.out.print("Generating Raw Code: "
            //                 + node.contents.getName() + " ");

            //          attempt to generate direct communication code
            // (no buffer), if this returns true it was sucessful
            // and the code was produced
            if (bottomPeek == 0 && remaining == 0
                && DirectCommunication.doit(ssg, node)) {
                //System.out.println("(Direct Communication)");

                return;
            }

            //if we get here, then we need a buffer!
            if (ssg.isInput(node) || ssg.simulator instanceof NoSimulator) {             // dynamic input
                // create dynamic network code and a circular buffer
                (new BufferedDynamicCommunication(ssg, node)).doit();
            } else {
                // create static network code and a buffer (maybe circular or
                // linear)
                (new BufferedStaticCommunication(ssg, node, bottomPeek,
                                                 remaining, initFire)).doit();
            }
        }
    }

    // calcuate bottomPeek, initFire, remaining
    // see my thesis section 5.1.2
    public void calculateItems(SIRFilter filter) {
        int pop = filter.getPopInt();
        int peek = filter.getPeekInt();

        // set up prePop, prePeek
        int prePop = 0;
        int prePeek = 0;

        if (filter instanceof SIRTwoStageFilter) {
            prePop = ((SIRTwoStageFilter) filter).getInitPopInt();
            prePeek = ((SIRTwoStageFilter) filter).getInitPeekInt();
        }

        // the number of times this filter fires in the initialization
        // schedule
        initFire = 0;

        // initExec counts might be null if we're calling for work
        // estimation before exec counts have been determined.
        if (ssg.getExecutionCounts(true) != null) {
            //whatSystem.out.println(layout.getTile(filter));
            initFire = ssg.getMult(layout.getNode(layout.getTile(filter)), true);
        } else {
            // otherwise, we should be doing this only for work
            // estimation--check that the filter is the only thing in the graph
            assert filter.getParent() == null || filter.getParent().size() == 1
                && filter.getParent().getParent() == null : "Found null pointer where unexpected.";
        }

        // if this is not a twostage, fake it by adding to initFire,
        // so we always think the preWork is called
        if (!(filter instanceof SIRTwoStageFilter))
            initFire++;

        // the number of items produced by the upstream filter in
        // initialization
        int upStreamItems = 0;

        FlatNode node = layout.getNode(layout.getTile(filter));
        FlatNode previous = null;

        if (node.inputs > 0) {
            previous = node.incoming[0];
            // the number of items produced by the upstream splitter may not
            // equal the number of items that produced by the filter feeding the
            // splitter
            // now, since I do not map splitter, this discrepancy must be
            // accounted for.
            // We do not have an incoming buffer for ths splitter, so the data
            // must
            // trickle down to the filter(s) that the splitter feeds
            if (previous.contents instanceof SIRSplitter) {
                upStreamItems = getPrevSplitterUpStreamItems(previous, node);
            } else {
                // upstream not a splitter, just get the number of executions
                upStreamItems = getUpStreamItems(ssg.getExecutionCounts(true),
                                                 node);
            }
        }

        // see my thesis for an explanation of this calculation
        if (initFire - 1 > 0) {
            bottomPeek = Math.max(0, peek - (prePeek - prePop));
        } else
            bottomPeek = 0;

        remaining = upStreamItems
            - (prePeek + bottomPeek + Math.max((initFire - 2), 0) * pop);

        //System.out.println("Remaining for " + filter + " " + remaining + "("
        //                   + upStreamItems + " >>> "
        //                   + (prePeek + bottomPeek + Math.max((initFire - 2), 0) * pop)
        //                   + ")");
    }

    /*
     * node is not directly connected upstream to a splitter, this function will
     * calculate the number of items send to node
     */
    private int getUpStreamItems(HashMap executionCounts, FlatNode node) {
        // if the node has not incoming channels then just return 0
        if (node.inputs < 1)
            return 0;

        FlatNode previous = node.incoming[0];

        int prevPush = 0;

        // get the number of times the previous node executes in the
        // schedule
        int prevInitCount = Util.getCountPrev(executionCounts, previous, node);

        // get the push rate for the previous
        if (prevInitCount > 0) {
            if (previous.contents instanceof SIRSplitter
                || previous.contents instanceof SIRJoiner) {
                prevPush = 1;
            } else
                prevPush = ((SIRFilter) previous.contents).getPushInt();
        }

        // push * executions
        int upStreamItems = (prevInitCount * prevPush);

        // System.out.println("previous: " + previous.getName());
        // System.out.println("prev Push: " + prevPush + " prev init: " +
        // prevInitCount);

        // if the previous node is a two stage filter then count its initWork
        // in the initialItemsTo Receive
        if (previous != null && previous.contents instanceof SIRTwoStageFilter) {
            upStreamItems -= ((SIRTwoStageFilter) previous.contents)
                .getPushInt();
            upStreamItems += ((SIRTwoStageFilter) previous.contents)
                .getInitPushInt();
        }

        return upStreamItems;
    }

    /**
     * If the filter's upstream neighbor is a splitter we may have a problem
     * where the filter feeding the splitter produces more data than the
     * splitter passes on in the init stage. So we need to recognize this and
     * forward the data on to the filter(s) that the splitter feeds. Remember,
     * we do not map splitters so the data on the splitters input tape after the
     * init stage is over must go somewhere.
     */
    private int getPrevSplitterUpStreamItems(FlatNode prev, FlatNode node) {
        double roundRobinMult = 1.0;

        // there is nothing feeding this splitter, so just return 0
        if (prev.inputs < 1)
            return 0;

        // follow the channels backward until we get to a filter or joiner,
        // remembering the weights on the rr splitters that connect
        // the filter (joiner) to node, so we know the number of items passed to
        // <pre>node</pre>
        FlatNode current = prev;
        FlatNode downstream = node;
        while (current.contents instanceof SIRSplitter) {
            if (!(((SIRSplitter) current.contents).getType() == SIRSplitType.DUPLICATE))
                roundRobinMult *= Util.getRRSplitterWeight(current, downstream);
            if (current.inputs < 1)
                return 0;
            downstream = current;
            current = current.incoming[0];
        }

        // now current must be a joiner or filter
        // get the number of item current produces
        int currentUpStreamItems = 
	    getUpStreamItems(ssg
			     .getExecutionCounts(true), current.edges[0]);
        //System.out.println(currentUpStreamItems);
        /*
         * if (getUpStreamItems(ssg.getExecutionCounts(true), node) !=
         * ((int)(currentUpStreamItems * roundRobinMult))) System.out.println
         * ("***** CORRECTING FOR INCOMING SPLITTER BUFFER IN INIT SCHEDULE (" +
         * node.contents.getName() + " " + ((int)(currentUpStreamItems *
         * roundRobinMult)) + " vs. " +
         * getUpStreamItems(ssg.getExecutionCounts(true), node) + ") *****\n");
         */

        // return the number of items passed from current to node thru the
        // splitters
        // (some may be roundrobin so we must use the weight multiplier.
        return (int) (currentUpStreamItems * roundRobinMult);
    }



    /**
     * Returns a for loop that uses field <pre>var</pre> to count <pre>count</pre> times with the
     * body of the loop being <pre>body</pre>. If count is non-positive, just returns
     * empty (!not legal in the general case)
     */
    public static JStatement makeForLoop(JStatement body, JLocalVariable var,
                                         JExpression count) {
        if (body == null)
            return new JEmptyStatement(null, null);

        // if count==0, just return empty statement
        if (count instanceof JIntLiteral) {
            int intCount = ((JIntLiteral) count).intValue();
            if (intCount <= 0) {
                // return empty statement
                return new JEmptyStatement(null, null);
            }
            if (intCount==1) {
                return body;
            }
        }
        // make init statement - assign zero to <pre>var</pre>. We need to use
        // an expression list statement to follow the convention of
        // other for loops and to get the codegen right.
        JExpression initExpr[] = { new JAssignmentExpression(null,
                                                             new JLocalVariableExpression(null, var), new JIntLiteral(0)) };
        JStatement init = new JExpressionListStatement(null, initExpr, null);
        // make conditional - test if <pre>var</pre> less than <pre>count</pre>
        JExpression cond = new JRelationalExpression(null, Constants.OPE_LT,
                                                     new JLocalVariableExpression(null, var), count);
        JExpression incrExpr = new JPostfixExpression(null,
                                                      Constants.OPE_POSTINC, new JLocalVariableExpression(null, var));
        JStatement incr = new JExpressionStatement(null, incrExpr, null);

        return new JForStatement(null, init, cond, incr, body, null);
    }


    /** This method is used by the various code generation schemes to return the 
	statement that executes a work function call.  If we are inlining work, it will return
	the cloned work body block, otherwise it will return a method call for the work function. 
	Return a block so that we can add statements to the block if necessary.
    */
    public static JBlock executeWorkFunction(SIRFilter filter) 
    {
	if (INLINE_WORK) {
	    JBlock workInitBlock = 
		(JBlock)ObjectDeepCloner.
		deepCopy(filter.getWork().getBody());
	    return workInitBlock;
	}
	else {
	    JBlock block = new JBlock();
	    JMethodCallExpression workCall = 
		new JMethodCallExpression(null, new JThisExpression(null),
					  filter.getWork().getName(),
					  new JExpression[0]);
	    block.addStatement(new JExpressionStatement(null, workCall, null));
	    return block;
	}
	
    }

    public int getBottomPeek() {
        return bottomPeek;
    }

    public int getInitFire() {
        return initFire;
    }

    public int getRemaining() {
        return remaining;
    }

}
