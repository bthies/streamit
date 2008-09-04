package at.dms.kjc.backendSupport;

import java.util.*;
import at.dms.kjc.slicegraph.*;
import java.io.*;

/** Dump a graph with info about slices and channels. */
public class DumpSlicesAndChannels {
    // dump the the completed partition to a dot file
    public static void dumpGraph(String filename, SIRSlicer slicer, BackEndFactory backendbits) {
        StringBuffer buf = new StringBuffer();
        buf.append("digraph Flattend {\n");
        buf.append("size = \"8, 10.5\";\n");

        for (int i = 0; i < slicer.getSliceGraph().length; i++) {
            Slice slice = slicer.getSliceGraph()[i];
            assert slice != null;
            buf.append(slice.hashCode() + " [ " + 
                    sliceName(slice, slicer, backendbits) + 
                    "\" ];\n");
            Edge[] outgoing = slice.getTail().getDestList();
            for (Edge e : outgoing) {
                assert e != null && e.getDest() != null;
                Slice next = e.getDest().getParent();
                buf.append(slice.hashCode() + " -> " + next.hashCode()
                            + " [label=\""
                            + channelName(e,backendbits)
                            + "\"];\n");
            }
        }

        buf.append("}\n");
        // write the file
        try {
            FileWriter fw = new FileWriter(filename);
            fw.write(buf.toString());
            fw.close();
        } catch (Exception e) {
            System.err.println("Could not print extracted slices");
        }
    }

    /** return a string for a channel. */
    private static  String channelName(Edge e, BackEndFactory backendbits) {
        StringBuffer out = new StringBuffer();
        Channel channel = backendbits.getChannel(e);
        if (channel == null) {
            out.append("??");
        } else { 
            out.append(channel.getClass().getSimpleName());
            out.append(" ");
            out.append(BufferSize.calculateSize(e));
            if (channel instanceof ChannelAsArray) {
                out.append("(");
                out.append(((ChannelAsArray)channel).getBufSize());
                out.append(")");
            }
            if (channel.rotationLength > 1) {
                out.append("*");
                out.append(channel.rotationLength);
            }
        }
        return out.toString();
    }
    
    /**return a string with all of the names of the filterslicenodes
     * and blue if linear. */
    private static  String sliceName(Slice slice, SIRSlicer slicer, BackEndFactory backendbits) {
        SliceNode node = slice.getHead();

        StringBuffer out = new StringBuffer();

        //do something fancy for linear slices!!!
        if (((FilterSliceNode)node.getNext()).getFilter().getArray() != null)
            out.append("color=cornflowerblue, style=filled, ");
        
        out.append("label=\"" + node.getAsInput().debugString(true));//toString());
        
        if (backendbits.sliceHasUpstreamChannel(node.getParent())) {
            out.append("  via " + channelName(node.getEdgeToNext(), backendbits) + "\\n");
        }
        
        node = node.getNext();
        while (node != null ) {
            if (node.isFilterSlice()) {
                FilterContent f = node.getAsFilter().getFilter();
                out.append("\\n" + node.toString() + "{"
                        + slicer.getFilterWork(node.getAsFilter())
                        + "}");
                if (f.isTwoStage())
                    out.append("\\npre:(peek, pop, push): (" + 
                            f.getPreworkPeek() + ", " + f.getPreworkPop() + "," + f.getPreworkPush());
                out.append(")\\n(peek, pop, push: (" + 
                        f.getPeekInt() + ", " + f.getPopInt() + ", " + f.getPushInt() + ")");
                out.append("\\nMult: init " + f.getInitMult() + ", steady " + f.getSteadyMult());
                out.append("\\n *** ");

                if (node.getNext() instanceof FilterSliceNode || backendbits.sliceHasDownstreamChannel(node.getParent())) {
                    out.append("  via " + channelName(node.getEdgeToNext(), backendbits) + "\\n");
                }
            }
            else {
                out.append("\\n" + node.getAsOutput().debugString(true));
            }
            /*else {
                //out.append("\\n" + node.toString());
            }*/
            node = node.getNext();
        }
        return out.toString();
    }
  
}
