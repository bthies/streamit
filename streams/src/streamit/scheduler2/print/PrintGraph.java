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

package streamit.scheduler2.print;

import java.io.File;
import java.io.FileOutputStream;
import java.io.DataOutputStream;
import streamit.scheduler2.iriter.FilterIter;
import streamit.scheduler2.iriter.PipelineIter;
import streamit.scheduler2.iriter.SplitJoinIter;
import streamit.scheduler2.iriter.FeedbackLoopIter;
import streamit.scheduler2.iriter.IteratorBase;
import streamit.scheduler2.iriter.SplitterIter;
import streamit.scheduler2.iriter.JoinerIter;
import streamit.scheduler2.iriter.Iterator;

public class PrintGraph extends streamit.misc.AssertedClass
{
    public void printProgram(Iterator iter)
    {
        ASSERT(iter.isPipeline());

        File outputFile;
        FileOutputStream fileOutputStream;
        DataOutputStream outputStream;

        try
        {
            outputFile = new File(getName(iter) + ".dot");
            fileOutputStream = new FileOutputStream(outputFile);
            outputStream = new DataOutputStream(fileOutputStream);

            outputStream.writeBytes("digraph streamit {\nsize=\"7.5,10\";");
            printStream(iter, outputStream);
            outputStream.writeBytes("}\n");
        }
        catch (Throwable e)
        {
            ERROR(e);
        }
    }

    void printStream(Iterator iter, DataOutputStream outputStream)
    {

        try
        {
            if (iter.isFilter() != null)
            {
                printFilter(iter.isFilter(), outputStream);
            }
            if (iter.isPipeline() != null)
            {
                printPipe(iter.isPipeline(), outputStream);
            }
            if (iter.isSplitJoin() != null)
            {
                printSJ(iter.isSplitJoin(), outputStream);
            }
            if (iter.isFeedbackLoop() != null)
            {
                printFL(iter.isFeedbackLoop(), outputStream);
            }
        }
        catch (Throwable e)
        {
            ERROR(e);
        }

    }

    String getName(Object obj)
    {
        String name = obj.toString();
	// if there is a dollar sign in the returned name, then this
	// indicates an anonymous class in the library.  We don't want
	// to label the anonymous nodes, so just return empty in this
	// case.
	if (name.indexOf("$")>-1) {
	    return "";
	} else {
	    return name
		.replace('@', '_')
		.replace('.', '_')
		.replace(' ', '_')
		.replace('=', '_')
		.replace(',', '_');
	}
    }

    String getName(IteratorBase iter)
    {
        return getName(iter.getObject());
    }

    String getUniqueName(IteratorBase iter)
    {
        return getName(iter.getObject()) + "_" + iter.getObject().hashCode();
    }

    String getNameForSplit(SplitterIter iter) {
	if (iter.getSplitterNumWork()>1) {
	    return "phased splitter";
	} else {
	    // detect duplicate if pop is unary
	    if (iter.getSplitPop(0)==1) {
		// duplicate splitter
		return "duplicate";
	    } else if (iter.getSplitPop(0)==0) {
		// null splitter
		return "roundrobin(0)";
	    } else {
		// roundrobin splitter... enumerate weights
		int weights[] = iter.getSplitPushWeights(0);
		StringBuffer result = new StringBuffer("roundrobin(");
		for (int i=0; i<weights.length; i++) { 
		    result.append(weights[i]);
		    if (i!=weights.length-1) {
			result.append(", ");
		    } else {
			result.append(")");
		    }
		}
		return result.toString();
	    }
	}
    }

    String getNameForJoin(JoinerIter iter) {
	if (iter.getJoinerNumWork()>1) {
	    return "phased joiner";
	} else if (iter.getJoinPush(0)==0) {
	    // null joiner
	    return "roundrobin(0)";
	} else {
	    int weights[] = iter.getJoinPopWeights(0);
	    StringBuffer result = new StringBuffer("roundrobin(");
	    for (int i=0; i<weights.length; i++) { 
		result.append(weights[i]);
		if (i!=weights.length-1) {
		    result.append(", ");
		} else {
		    result.append(")");
		}
	    }
	    return result.toString();
	}
    }

    void printFilter(FilterIter filter, DataOutputStream outputStream)
    {
        try
        {
            outputStream.writeBytes(
                getUniqueName(filter)
                    + " [ label=\""
                    + getName(filter)
                    + "\" ]\n");
        }
        catch (Throwable e)
        {
            ERROR(e);
        }
    }

    void printPipe(PipelineIter pipeline, DataOutputStream outputStream)
    {
        try
        {
            String lastPrinted = null;

            // Print this within a subgraph.
            outputStream.writeBytes(
                "subgraph cluster_" + getUniqueName(pipeline) + " {\n");
            outputStream.writeBytes("label = \"" + getName(pipeline) + "\";\n");

            // Walk through each of the elements in the pipeline.
            for (int nChild = 0; nChild < pipeline.getNumChildren(); nChild++)
            {
                Iterator child = pipeline.getChild(nChild);
                ASSERT(child);

                String topChild = getUniqueTopStreamName(child);
                String bottomChild = getUniqueBottomStreamName(child);

                printEdge(lastPrinted, topChild, outputStream);

                // Update the known edges.
                lastPrinted = bottomChild;
            }

            for (int nChild = 0; nChild < pipeline.getNumChildren(); nChild++)
            {
                printStream(pipeline.getChild(nChild), outputStream);
            }

            outputStream.writeBytes("}\n");
        }
        catch (Throwable e)
        {
            ERROR(e);
        }
    }

    void printSJ(SplitJoinIter sj, DataOutputStream outputStream)
    {
        try
        {
            // Create a subgraph again...
            outputStream.writeBytes(
                "subgraph cluster_" + getUniqueName(sj) + " {\n");
            outputStream.writeBytes("label = \"" + getName(sj) + "\";\n");

            // Visit the splitter and joiner to get their node names...
            outputStream.writeBytes(
                getUniqueTopStreamName(sj.getUnspecializedIter())
                    + " [ label=\""
		    + getNameForSplit(sj)
                    + "\" ]\n");
            outputStream.writeBytes(
                getUniqueBottomStreamName(sj.getUnspecializedIter())
                    + " [ label=\""
		    + getNameForJoin(sj)
                    + "\" ]\n");

            String splitName =
                getUniqueTopStreamName(sj.getUnspecializedIter());
            String joinName =
                getUniqueBottomStreamName(sj.getUnspecializedIter());

            // ...and walk through the body.
            int nChild;
            for (nChild = 0; nChild < sj.getNumChildren(); nChild++)
            {
                Iterator child = sj.getChild(nChild);
                printStream(child, outputStream);

                printEdge(
                    splitName,
                    getUniqueTopStreamName(child),
                    outputStream);
                printEdge(
                    getUniqueBottomStreamName(child),
                    joinName,
                    outputStream);
            }

            outputStream.writeBytes("}\n");
        }
        catch (Throwable e)
        {
            ERROR(e);
        }
    }

    void printFL(FeedbackLoopIter fl, DataOutputStream outputStream)
    {
        try
        {
            // Create a subgraph again...
            outputStream.writeBytes(
                "subgraph cluster_" + getUniqueName(fl) + " {\n");
            outputStream.writeBytes("label = \"" + getName(fl) + "\";\n");

            // Visit the splitter and joiner.
            outputStream.writeBytes(
                getUniqueTopStreamName(fl.getUnspecializedIter())
                    + " [ label=\""
		    + getNameForJoin(fl)
                    + "\" ]\n");
            outputStream.writeBytes(
                getUniqueBottomStreamName(fl.getUnspecializedIter())
                    + " [ label=\""
		    + getNameForSplit(fl)
                    + "\" ]\n");

            // Visit the body and the loop part.
            printStream(fl.getBodyChild(), outputStream);
            printStream(fl.getLoopChild(), outputStream);

            printEdge(
                getUniqueTopStreamName(fl.getUnspecializedIter()),
                getUniqueTopStreamName(fl.getBodyChild()),
                outputStream);
            printEdge(
                getUniqueBottomStreamName(fl.getBodyChild()),
                getUniqueBottomStreamName(fl.getUnspecializedIter()),
                outputStream);
            printEdge(
                getUniqueBottomStreamName(fl.getUnspecializedIter()),
                getUniqueTopStreamName(fl.getLoopChild()),
                outputStream);
            printEdge(
                getUniqueBottomStreamName(fl.getLoopChild()),
                getUniqueTopStreamName(fl.getUnspecializedIter()),
                outputStream);

            outputStream.writeBytes("}\n");
        }
        catch (Throwable e)
        {
            ERROR(e);
        }
    }

    void printEdge(
        String from,
        String to,
        DataOutputStream outputStream)
    {
        if (from == null || to == null)
            return;
        try
        {
            outputStream.writeBytes(from + " -> " + to + "\n");
        }
        catch (Throwable e)
        {
            ERROR(e);
        }
    }

    String getUniqueTopStreamName(Iterator iter)
    {
        if (iter.isFilter() != null)
        {
            return getUniqueName(iter);
        }
        if (iter.isPipeline() != null)
        {
            return getUniqueTopStreamName(iter.isPipeline().getChild(0));
        }
        if (iter.isSplitJoin() != null)
        {
            return getUniqueName(iter) + "_split";
        }
        if (iter.isFeedbackLoop() != null)
        {
            return getUniqueName(iter) + "_join";
        }

        ASSERT(0);
        return null;
    }

    String getUniqueBottomStreamName(Iterator iter)
    {
        if (iter.isFilter() != null)
        {
            return getUniqueName(iter);
        }
        if (iter.isPipeline() != null)
        {
            return getUniqueBottomStreamName(
                iter.isPipeline().getChild(
                    iter.isPipeline().getNumChildren() - 1));
        }
        if (iter.isSplitJoin() != null)
        {
            return getUniqueName(iter) + "_join";
        }
        if (iter.isFeedbackLoop() != null)
        {
            return getUniqueName(iter) + "_split";
        }

        ASSERT(0);
        return null;
    }

}
