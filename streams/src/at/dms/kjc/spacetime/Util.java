package at.dms.kjc.spacetime;

//import at.dms.kjc.common.CodegenPrintWriter;
import at.dms.kjc.slicegraph.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.util.Utils;
import java.util.Collection;
//import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.slicegraph.Edge;
import at.dms.kjc.slicegraph.FilterSliceNode;
import at.dms.kjc.slicegraph.Partitioner;
import at.dms.kjc.slicegraph.Slice;
import at.dms.kjc.slicegraph.SliceNode;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
//import java.util.TreeSet;
import java.util.HashSet;
import java.util.HashMap;
//import java.io.*;
import at.dms.kjc.spacetime.switchIR.*;
import java.util.Arrays;

/**
 *  A class with useful functions that span classes. 
 * 
 * 
**/
public class Util {
    public static String CSTOINTVAR = "__csto_integer__";

    public static String CSTOFPVAR = "__csto_float__";

    public static String CSTIFPVAR = "__csti_float__";

    public static String CSTIINTVAR = "__csti_integer__";

    public static String CGNOINTVAR = "__cgno_integer__";

    public static String CGNOFPVAR = "__cgno_float__";

    public static String CGNIFPVAR = "__cgni_float__";

    public static String CGNIINTVAR = "__cgni_integer__"; 

    // unique ID for each file reader/writer used to
    // generate var names...
    private static HashMap<PredefinedContent, String> fileVarNames;

    private static int fileID = 0;

    static {
        fileVarNames = new HashMap<PredefinedContent, String>();
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

    /**
     * Print to screen the mapping of multiplicities
     * 
     * @param map The HashMap.
     */
    public static void printExecutionCount(HashMap map) {
        Iterator keys = map.keySet().iterator();
        System.out.println("  *******  ");
        while(keys.hasNext()) {
            Object obj = keys.next();
            System.out.println(obj + " -> " + ((int[])map.get(obj))[0]);
        }
        System.out.println("  *******  ");
    }
    
    /**
     * For type return the number of words that it occupies.
     * 
     * @param type The type.
     * @return The number of words occupied by type.
     */
    public static int getTypeSize(CType type) {

        if (!(type.isArrayType() || type.isClassType()))
            return 1;
        else if (type.isArrayType()) {
            int elements = 1;
            int dims[] = Util.makeInt(((CArrayType) type).getDims());

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

    public static int[] makeInt(JExpression[] dims) {
        int[] ret = new int[dims.length];

        for (int i = 0; i < dims.length; i++) {
            if (!(dims[i] instanceof JIntLiteral))
                Utils
                    .fail("Array length for tape declaration not an int literal");
            ret[i] = ((JIntLiteral) dims[i]).intValue();
        }
        return ret;
    }

    public static CType getBaseType(CType type) {
        if (type.isArrayType())
            return ((CArrayType) type).getBaseType();
        return type;
    }

    public static String[] makeString(JExpression[] dims) {
        String[] ret = new String[dims.length];

        for (int i = 0; i < dims.length; i++) {
            TraceIRtoC ttoc = new TraceIRtoC();
            dims[i].accept(ttoc);
            ret[i] = ttoc.getPrinter().getString();
        }
        return ret;
    }
    
    /**
     * Return true if the sets contructed from list and array are equal.
     * 
     * @param list The list
     * @param array The array
     * @return true if the sets contructed from list and array are equal.
     */
    public static boolean setCompare(LinkedList list, Object[] array) {
        if (array.length == list.size()) {
            HashSet listSet = new HashSet();
            HashSet<Object> arraySet = new HashSet<Object>();
            for (int i = 0; i < array.length; i++) {
                listSet.add(list.get(i));
                arraySet.add(array[i]);
            }
            return listSet.equals(arraySet);
        }
        return false;
    }
    
    /**
     * Add the elements of addme to list.
     * 
     * @param list The list.
     * @param addme Add elements to list.
     */
    public static void add(LinkedList<Integer> list, int[] addme) {
        for (int i = 0; i < addme.length; i++)
            list.add(new Integer(addme[i]));
    }
    
    
    /**
     * Add the elements of addme to list.
     * 
     * @param list The list.
     * @param addme Add elements to list.
     */
    public static void add(LinkedList list, Object[] addme) {
        for (int i = 0; i < addme.length; i++)
            list.add(addme[i]);
    }
    
    /**
     * @param dynamic
     * @param tapeType
     * @return The code to receive a item from either the dynamic network 
     * or the static network based on the type.
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

    /**
     * @param slice
     * @param tile
     * @return True if <pre>trace</pre> has a filter that is mapped to <pre>tile</pre>.
     */
    public static boolean doesSliceUseTile(Slice slice, 
            RawTile tile, Layout layout) {
        SliceNode node = slice.getHead().getNext();
        //cycle thru the nodes and see if we can find a match 
        //of the coordinates for the tile and a filter trace
        while (node.isFilterSlice()) {
            if (tile == layout.getTile(node.getAsFilter()))
                return true;
            
            node = node.getNext();
        }
        
        return false;
    }
    
    
    /**
     * @param array
     * @return The median element of <pre>array</pre>.
     */
    public static int median(int[] array) {
        int[] sortMe = (int[])array.clone();
        Arrays.sort(sortMe);
        return sortMe[sortMe.length / 2];
    }
    
    /**
     * 
     * @param array
     * @return The mean of the elements of <pre>array</pre>.
     */
    public static double mean(int[] array) {
        return (double)sum(array) / (double)array.length;
    }
    
    /**
     * @param array
     * @return the sum of the elements of array.
     */
    public static int sum(int[] array) {
        int sum = 0;
        for (int i = 0; i < array.length; i++) {
            sum += array[i];
        }
        return sum;
    }
    
    /**
     * 
     * @param dynamic
     * @param tapeType
     * @return The prefix for sending an item over either the dynamic network or
     * the static network of the given type.  Should be used before the value to send
     * over the network is generated.
     */
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

    /**
     * @param dynamic
     * @return The suffix to append after the value is generated for 
     * a network send.
     */
    public static String networkSendSuffix(boolean dynamic) {
//        assert KjcOptions.altcodegen;
        return "";
    }

    // the size of the buffer between in, out for the steady state
    public static int steadyBufferSize(Edge edge) {
        return edge.steadyItems() * getTypeSize(edge.getType());
    }

    // the size of the buffer between in / out for the init stage
    public static int initBufferSize(Edge edge) {
        return edge.initItems() * getTypeSize(edge.getType());
    }
    
    public static int magicBufferSize(Edge edge) {
        // i don't remember why I have the + down there,
        // but i am not going to change
        return Math.max(steadyBufferSize(edge), initBufferSize(edge));
    }

    public static String getFileVar(PredefinedContent content) {
        if (content instanceof FileInputContent
            || content instanceof FileOutputContent) {
            if (!fileVarNames.containsKey(content))
                fileVarNames.put(content, new String("file" + fileID++));
            return fileVarNames.get(content);
        } else
            Utils.fail("Calling getFileVar() on non-filereader/filewriter");
        return null;
    }

    public static String getFileHandle(PredefinedContent content) {
        return "file_" + getFileVar(content);
    }

    public static String getOutputsVar(FileOutputContent out) {
        return "outputs_" + getFileVar(out);
    }

    // given <pre>i</pre> bytes, round <pre>i</pre> up to the nearest cache
    // line divisible int
    public static int cacheLineDiv(int i) {
        if (i % RawChip.cacheLineBytes == 0)
            return i;
        return (RawChip.cacheLineBytes - (i % RawChip.cacheLineBytes)) + i;
    }

    // helper function to add everything in a collection to the set
    public static void addAll(HashSet set, Collection c) {
        Iterator it = c.iterator();
        while (it.hasNext()) {
            set.add(it.next());
        }
    }

    /**
     * @param traces
     * @return An array of all the TraceNode in the <pre>traces</pre> array 
     * dictated by the order that the traces appear in <pre>traces</pre>. 
     */
    public static SliceNode[] sliceNodeArray(Slice[] traces) {
        LinkedList<SliceNode> trav = new LinkedList<SliceNode>();

        for (int i = 0; i < traces.length; i++) {
            SliceNode sliceNode = traces[i].getHead();
            while (sliceNode != null) {
                trav.add(sliceNode);
                sliceNode = sliceNode.getNext();
            }

        }
        
        return trav.toArray(new SliceNode[0]);
    }
    
    public static void sendConstFromTileToSwitch(RawTile tile, int c,
                                                 boolean init, boolean primePump, SwitchReg reg) {

        tile.getComputeCode().sendConstToSwitch(c, init || primePump);
        // add the code to receive the const
        MoveIns moveIns = new MoveIns(reg, SwitchIPort.CSTO);
        tile.getSwitchCode().appendIns(moveIns, (init || primePump));
    }

    /**
     * Return a sorted list of filter trace nodes for time only that does not 
     * include io traces.
     * 
     * @param partitioner
     * @return a sorted list of filter trace nodes for time only that does not 
     * include io traces.
     */
    public static LinkedList<FilterSliceNode> sortedFilterSlicesTime(Partitioner partitioner) {
        //now sort the filters by work
        LinkedList<FilterSliceNode> sortedList = new LinkedList<FilterSliceNode>();
        LinkedList<Slice> scheduleOrder;
 
  

        Slice[] tempArray = (Slice[]) partitioner.getSliceGraph().clone();
        Arrays.sort(tempArray, new CompareSliceBNWork(partitioner));
        scheduleOrder = new LinkedList<Slice>(Arrays.asList(tempArray));
        //reverse the list, we want the list in descending order!
        Collections.reverse(scheduleOrder);
  
        for (int i = 0; i < scheduleOrder.size(); i++) {
            Slice slice = scheduleOrder.get(i);
            //don't add io traces!
            /*if (partitioner.isIO(trace)) {
                continue;
            }*/
            assert slice.getNumFilters() == 1 : "Only works for Time!";
            sortedList.add(slice.getHead().getNextFilter());
        }
        
        return sortedList;
    }
    
    /**
     * Calculate the number of items the file writers write to files in
     * the schedule exeCounts.
     * 
     * @param str The toplevel container.
     * @param exeCounts The schedule.
     * @return the number of items the file writers write to files in
     * the schedule exeCounts.
     */
    public static int outputsPerSteady(SIRStream str, HashMap exeCounts) {
        if (str instanceof SIRFeedbackLoop) {
            SIRFeedbackLoop fl = (SIRFeedbackLoop) str;
            return outputsPerSteady(fl.getBody(), exeCounts) + 
            outputsPerSteady(fl.getLoop(), exeCounts);
        }
        if (str instanceof SIRPipeline) {
            SIRPipeline pl = (SIRPipeline) str;
            Iterator iter = pl.getChildren().iterator();
            int outputs = 0;
            while (iter.hasNext()) {
                SIRStream child = (SIRStream) iter.next();
                outputs += outputsPerSteady(child, exeCounts);
            }
            return outputs;
        }
        if (str instanceof SIRSplitJoin) {
            SIRSplitJoin sj = (SIRSplitJoin) str;
            Iterator<SIRStream> iter = sj.getParallelStreams().iterator();
            int outputs = 0;
            while (iter.hasNext()) {
                SIRStream child = iter.next();
                outputs += outputsPerSteady(child, exeCounts);
            }
            return outputs;
        }
        //update the comm and comp numbers...
        if (str instanceof SIRFilter) {
            if (str instanceof SIRFileWriter)
                return ((int[])exeCounts.get(str))[0];
            else
                return 0;
        }
        return 0;
    }
}