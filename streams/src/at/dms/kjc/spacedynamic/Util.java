package at.dms.kjc.spacedynamic;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.util.Utils;
import at.dms.kjc.common.CommonUtils;
import java.util.List;
import at.dms.kjc.sir.lowering.*;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.HashMap;
import java.io.*;

/**
 * This class contains various function used by multiple passes
 */
public class Util extends at.dms.util.Utils {
    public static String CSTOINTVAR = "__csto_integer__";

    public static String CSTOFPVAR = "__csto_float__";

    public static String CSTIFPVAR = "__csti_float__";

    public static String CSTIINTVAR = "__csti_integer__";

    public static String CGNOINTVAR = "__cgno_integer__";

    public static String CGNOFPVAR = "__cgno_float__";

    public static String CGNIFPVAR = "__cgni_float__";

    public static String CGNIINTVAR = "__cgni_integer__";

    
    /**
     * Return the number of items node produces during one firing during
     * steady-state.  For filters it is the pop, for splitters and joiners it is
     * 1.
     * 
     * @param node The str in question
     * @return The number of items produced per firing in the steady-state
     */
    public static int getItemsProduced(FlatNode node) {
        if (node.isFilter())
            return ((SIRFilter)node.contents).getPopInt();
        else 
            return 1;
    }
    
    
    // returns true if this filter is mapped
    public static boolean countMe(SIRFilter filter) {
        return !(filter instanceof SIRIdentity
                 || filter instanceof SIRFileWriter || filter instanceof SIRFileReader);
    }

    /**
     * Given a filter flatnode <node>, return the closest upstream node 
     * that is assigned to a tile or ioport. 
     * @param layout: needed to find whether the node is assigned
     * @param node: must be a SIRFilter
     * @return a FlatNode or null if could not find an upstream node.
     */
    public static FlatNode getFilterUpstreamAssigned(Layout layout, FlatNode node) {
        assert node.isFilter();
        FlatNode upstream = node.incoming[0];
        while (!Layout.assignToAComputeNode(upstream)) {
            if (upstream.inputs < 1)
                return null;
            upstream = upstream.incoming[0];
        }
        
        return upstream;    
    }
    
    /**
     * Given a filter flatnode <node>, return the closest downstream node 
     * that is assigned to a tile or ioport. 
     * @param layout: needed to find whether the node is assigned
     * @param node: must be a SIRFilter
     * @return a FlatNode or null if could not find a downstream node.
     */
    public static FlatNode getFilterDownstreamAssigned(Layout layout, FlatNode node) {
        assert node.isFilter();
        FlatNode downstream = node.getEdges()[0];
        while (!Layout.assignToAComputeNode(downstream)) {
            if (downstream.ways < 1)
                return null;
            downstream = downstream.getEdges()[0];
        }
        
        return downstream;    
    }
    
    public static int nextPow2(int i) {
        String str = Integer.toBinaryString(i);
        if (str.indexOf('1') == -1)
            return 0;
        int bit = str.length() - str.indexOf('1');
        int ret = (int) Math.pow(2, bit);
        if (ret == i * 2)
            return i;
        return ret;

    }

    /*
     * get the execution Count of the previous node
     */
    public static int getCount(HashMap counts, FlatNode node) {
        Integer count = ((Integer) counts.get(node));

        if (count == null)
            return 0;
        return count.intValue();
    }

    /*
     * get the execution count of the previous node
     */
    public static int getCountPrev(HashMap counts, FlatNode prev, FlatNode node) {
        if (!(prev.contents instanceof SIRSplitter))
            return getCount(counts, prev);

        // if (((SIRSplitter)prev.contents).getType() == SIRSplitType.DUPLICATE)
        // return getCount(counts, prev);

        // prev is a splitter
        double rate = getRRSplitterWeight(prev, node);
        return ((int) (rate * (double) getCount(counts, prev)));
    }

    // get the percentage of items sent from splitter prev to node
    public static double getRRSplitterWeight(FlatNode prev, FlatNode node) {
        // prev is a splitter
        int sumWeights = 0;
        for (int i = 0; i < prev.ways; i++)
            sumWeights += prev.weights[i];
        int thisWeight = -1;
        for (int i = 0; i < prev.ways; i++) {
            if (prev.getEdges()[i] != null && prev.getEdges()[i].equals(node)) {
                thisWeight = prev.weights[i];
                break;
            }
        }

        if (thisWeight == -1)
            Utils.fail("Splitter not connected to node: " + prev + "->" + node);
        return ((double) thisWeight) / ((double) sumWeights);
    }
    
    /**
     * LCM of a and b.
     * @param a
     * @param b
     * @return int[the lcm, a's multipler, b's multipler]
     */
    public static int[] lcm(int a,int b) {
        int mulA=1,mulB=1;
        int accumA=a,accumB=b;
        while(accumA!=accumB)
            if(accumA<accumB) {
                accumA+=a;
                mulA++;
            } else {
                accumB+=b;
                mulB++;
            }
        assert ((a*mulA) == (b*mulB)) && ((a*mulA) == (accumA)); 
        return new int[]{accumA,mulA,mulB};
    }
    
    /*
     * for a given CType return the size (number of elements that need to be
     * sent when routing).
     */
    public static int getTypeSize(CType type) {

        if (!(type.isArrayType() || type.isClassType()))
            return 1;
        else if (type.isArrayType()) {
            int elements = 1;
            int dims[] = CommonUtils.makeArrayInts(((CArrayType) type).getDims());

            for (int i = 0; i < dims.length; i++) {
                elements *= dims[i];
            }
            return elements;
        } else if (type.isClassType()) {
            int size = 0;
            for (int i = 0; i < type.getCClass().getFields().length; i++) {
                size += getTypeSize(type.getCClass().getFields()[i].getType());
            }
            return size;
        }
        Utils.fail("Unrecognized type");
        return 0;
    }

    public static int getTypeSize(SIRStructure struct) {
        int sum = 0;

        for (int i = 0; i < struct.getFields().length; i++) {
            sum += getTypeSize(struct.getFields()[i].getType());
        }
        return sum;
    }


    public static String[] makeString(JExpression[] dims) {
        String[] ret = new String[dims.length];

        for (int i = 0; i < dims.length; i++) {
            FlatIRToC ftoc = new FlatIRToC();
            dims[i].accept(ftoc);
            ret[i] = ftoc.getPrinter().getString();
        }
        return ret;
    }

    /*
     * public static String networkReceivePrefix(boolean dynamic) { assert
     * KjcOptions.altcodegen; return ""; }
     */

    public static String networkReceive(boolean dynamic, CType tapeType) {
//        assert KjcOptions.altcodegen;
        if (dynamic) {
            if (tapeType.isFloatingPoint())
                return CGNIFPVAR;
            else
                return CGNIINTVAR;
        } else {
            if (tapeType.isFloatingPoint())
                return CSTIFPVAR;
            else
                return CSTIINTVAR;
        }
    }

    public static String networkSendPrefix(boolean dynamic, CType tapeType) {
//        assert KjcOptions.altcodegen;
        StringBuffer buf = new StringBuffer();
        if (dynamic) {
            if (tapeType.isFloatingPoint())
                buf.append(CGNOFPVAR);
            else
                buf.append(CGNOINTVAR);
        } else {
            if (tapeType.isFloatingPoint())
                buf.append(CSTOFPVAR);
            else
                buf.append(CSTOINTVAR);
        }

        buf.append(" = (" + tapeType + ")");
        return buf.toString();
    }

    public static String networkSendSuffix(boolean dynamic) {
//        assert KjcOptions.altcodegen;
        return "";
    }

    /**
     * @return the FlatNodes that are directly downstream of the
     * given flatnode and are themselves assigned a tile in the
     * layout 
     */
    public static HashSet<FlatNode> getAssignedEdges(Layout layout, FlatNode node) {
        HashSet<FlatNode> set = new HashSet<FlatNode>();

        if (node == null)
            return set;

        for (int i = 0; i < node.getEdges().length; i++)
            getAssignedEdgesHelper(layout, node.getEdges()[i], set);

        return set;
    }

    private static void getAssignedEdgesHelper(Layout layout, FlatNode node,
                                               HashSet<FlatNode> set) {
        if (node == null)
            return;
        else if (layout.isAssigned(node)) {
            set.add(node);
            return;
        } else {
            for (int i = 0; i < node.getEdges().length; i++)
                getAssignedEdgesHelper(layout, node.getEdges()[i], set);
        }
    }

    // get all filters/joiners that are directly connected downstream to this
    // node, but go thru all splitters. The node itself is a joiner or
    // filter, NOTE, THIS HAS NOT BEEN TESTED BUT IT SHOULD WORK, I DID NOT
    // NEED IT FOR WHAT I WROTE IT FOR
    public static HashSet<Object> getDirectDownstream(FlatNode node) {
        if (node == null || node.isSplitter())
            Utils
                .fail("getDirectDownStream(...) error. Node not filter or joiner.");
        if (node.ways > 0)
            return getDirectDownstreamHelper(node.getEdges()[0]);
        else
            return new HashSet<Object>();
    }

    private static HashSet<Object> getDirectDownstreamHelper(FlatNode current) {
        if (current == null)
            return new HashSet<Object>();
        else if (current.isFilter() || current.isJoiner()) {
            HashSet<Object> ret = new HashSet<Object>();
            ret.add(current);
            return ret;
        } else if (current.isSplitter()) {
            HashSet<Object> ret = new HashSet<Object>();

            for (int i = 0; i < current.ways; i++) {
                if (current.weights[i] != 0)
                    SpaceDynamicBackend.addAll(ret,
                                               getDirectDownstreamHelper(current.getEdges()[i]));
            }
            return ret;
        }
        return null;
    }

    public static SIRFilter getSinkFilter(SIRStream stream) {
        if (stream instanceof SIRFilter)
            return (SIRFilter) stream;
        else if (stream instanceof SIRPipeline)
            return getSinkFilter(((SIRPipeline) stream)
                                 .get(((SIRPipeline) stream).size() - 1));
        else
            assert false : "Calling getSinkFilter() on Stream with non-filter sink";
        return null;
    }

    public static SIRFilter getSourceFilter(SIRStream stream) {
        if (stream instanceof SIRFilter)
            return (SIRFilter) stream;
        else if (stream instanceof SIRPipeline)
            return getSourceFilter(((SIRPipeline) stream).get(0));
        else
            assert false : "Calling getSourceFilter() on Stream with non-filter source";
        return null;
    }

    /**
     * Set the i/o rates of f1 to the i/o rates of f2.
     * 
     * @param f1 Set this filter to the i/o rates of f2.
     * @param f2 Set f1 to this filters i/o rates.
     */
    public static void restoreIO(SIRFilter f1, SIRFilter f2) {
        f1.setPush(new JIntLiteral(f2.getPushInt()));
        f1.setPeek(new JIntLiteral(f2.getPeekInt()));
        f1.setPop(new JIntLiteral(f2.getPopInt()));
        if (f1 instanceof SIRTwoStageFilter) {
            assert f2 instanceof SIRTwoStageFilter;
            SIRTwoStageFilter f12 = (SIRTwoStageFilter)f1;
            SIRTwoStageFilter f22 = (SIRTwoStageFilter)f2;
            f12.setInitPop(f22.getInitPopInt());
            f12.setInitPeek(f22.getInitPeekInt());
            f12.setInitPush(f22.getInitPushInt());
        }
        f1.setInputType(f2.getInputType());
        f1.setOutputType(f2.getOutputType());
    }
    
    /** set the push and pop/peek rates to 0 and the input / output types to void * */
    public static void removeIO(SIRFilter filter) {
        filter.setPush(new JIntLiteral(0));
        //filter.setOutputType(CStdType.Void);
        filter.setPop(new JIntLiteral(0));
        filter.setPeek(new JIntLiteral(0));
        if (filter instanceof SIRTwoStageFilter) {
            SIRTwoStageFilter two = (SIRTwoStageFilter)filter;
            two.setInitPop(0);
            two.setInitPeek(0);
            two.setInitPush(0);
        }
        //filter.setInputType(CStdType.Void);
    }

    /** Given a flatnode of a joiner or a filter that is mapped, get the schedule
     * of mapped nodes that it pushes to (so disregard unmapped splitters and joiners).  
     * 
     * This only works on nodes whose output is split at most once!
     * 
     * 
     * @param layout
     * @param node
     * @return The schedule of nodes that are the assigned sinks of this filter.
     */
    public static FlatNode[] getSendingSchedule(Layout layout, FlatNode node) {
        assert layout.isAssigned(node);
        
        if (node.ways < 1)
            return new FlatNode[0];
        
        //only handle filters whose output is split once
        if (node.ways > 0 && node.getEdges()[0].isSplitter()) {
            for (int j = 0; j < node.getEdges()[0].ways; j++) {
                if (node.getEdges()[0].getEdges()[j].isSplitter())
                    assert false : "getSendingSchedule() cannot handle this filter!";
            }
        }
        
        assert node.ways == 1;
        LinkedList<FlatNode> schedule = new LinkedList<FlatNode>();
        //  get the downstream filter
        FlatNode downstream = node.getEdges()[0];
        if (downstream.isSplitter()) {
            //if the node downstream is a splitter, then add the downstream filters
            //of this splitter to the schedule according to the weights...
            for (int i = 0; i < downstream.ways; i++) {
                assert !downstream.getEdges()[i].isSplitter();
                for (int j = 0; j < downstream.weights[i]; j++) {
                    //System.out.println("Adding " + downstream.edges[i] + " to the sending schedule."); 
                    schedule.add(downstream.getEdges()[i]);
                }
            }
        } else {
            //if not a splitter, just add the node...
            schedule.add(downstream);
            //System.out.println("Adding " + downstream + " to the sending schedule."); 
        }
                        
        return schedule.toArray(new FlatNode[0]);
    }

}
