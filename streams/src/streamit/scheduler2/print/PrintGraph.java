package streamit.scheduler2.print;

import java.io.File;
import java.io.FileOutputStream;
import java.io.DataOutputStream;
import streamit.scheduler2.iriter.FilterIter;
import streamit.scheduler2.iriter.PipelineIter;
import streamit.scheduler2.iriter.SplitJoinIter;
import streamit.scheduler2.iriter.FeedbackLoopIter;
import streamit.scheduler2.iriter.IteratorBase;
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

    public void printStream(Iterator iter, DataOutputStream outputStream)
    {
        String iterType;
        if (iter.isFilter() != null)
        {
            iterType = "Filter";
        }
        else if (iter.isPipeline() != null)
        {
            iterType = "Pipeline";
        }
        else if (iter.isSplitJoin() != null)
        {
            iterType = "SplitJoin";
        }
        else if (iter.isFeedbackLoop() != null)
        {
            iterType = "FeedbackLoop";
        }
        else
        {
            ERROR("Invalid type of an iterator!");
            iterType = null;
        }

        File outputFile;
        FileOutputStream fileOutputStream;

        try
        {
            if (outputStream == null)
            {
                outputFile = new File(getName(iter) + ".java");
                fileOutputStream = new FileOutputStream(outputFile);
                outputStream = new DataOutputStream(fileOutputStream);
            }

            outputStream.writeBytes("import streamit.*;\n\n");

            outputStream.writeBytes(
                "public class "
                    + getName(iter)
                    + " extends "
                    + iterType
                    + "\n");
            outputStream.writeBytes("{\n");

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

            outputStream.writeBytes("}\n");
        }
        catch (Throwable e)
        {
            ERROR(e);
        }

    }

    public String getName(Object obj)
    {
        String name = obj.toString();
        return name
            .replace('@', '_')
            .replace('$', '_')
            .replace('.', '_')
            .replace(' ', '_')
            .replace('=', '_')
            .replace(',', '_');
    }

    public String getName(IteratorBase iter)
    {
        return getName(iter.getObject());
    }

    public String getUniqueName(IteratorBase iter)
    {
        return getName(iter.getObject()) + "_" + iter.getObject().hashCode();
    }

    public void printFilter(FilterIter filter, DataOutputStream outputStream)
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

    public void printPipe(PipelineIter pipeline, DataOutputStream outputStream)
    {
        try
        {
            String lastPrinted = null;

            // Print this within a subgraph.
            outputStream.writeBytes(
                "subgraph cluster_" + getUniqueName(pipeline) + " {\n");
            outputStream.writeBytes("label = \"" + getName(pipeline) + "\";\n");

            // Walk through each of the elements in the pipeline.
            int nChild;
            for (nChild = 1; nChild < pipeline.getNumChildren(); nChild++)
            {
                Iterator child = pipeline.getChild(nChild);
                ASSERT(child);

                printStream(child, outputStream);

                String topChild = getUniqueTopStreamName(child);
                String bottomChild = getUniqueBottomStreamName(child);

                printEdge(lastPrinted, topChild, outputStream);

                // Update the known edges.
                lastPrinted = bottomChild;
            }

            outputStream.writeBytes("}\n");
        }
        catch (Throwable e)
        {
            ERROR(e);
        }
        for (int nChild = 0; nChild < pipeline.getNumChildren(); nChild++)
        {
            printStream(pipeline.getChild(nChild), outputStream);
        }
    }

    public void printSJ(SplitJoinIter sj, DataOutputStream outputStream)
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
                    + "split"
                    + "\" ]\n");
            outputStream.writeBytes(
                getUniqueBottomStreamName(sj.getUnspecializedIter())
                    + " [ label=\""
                    + "join"
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

    public void printFL(FeedbackLoopIter fl, DataOutputStream outputStream)
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
                    + "join"
                    + "\" ]\n");
            outputStream.writeBytes(
                getUniqueBottomStreamName(fl.getUnspecializedIter())
                    + " [ label=\""
                    + "split"
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

    public void printEdge(
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

    public String getUniqueTopStreamName(Iterator iter)
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

    public String getUniqueBottomStreamName(Iterator iter)
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
