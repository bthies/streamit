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
import streamit.scheduler2.iriter.Iterator;
import streamit.misc.Pair;
import streamit.misc.OMap;
import streamit.misc.OMapIterator;

public class PrintProgram extends streamit.misc.AssertedClass
{
    public void printProgram(Iterator iter)
    {
        assert iter.isPipeline() != null;

        File outputFile;
        FileOutputStream fileOutputStream;
        DataOutputStream outputStream;

        try
        {
            outputFile = new File(getName(iter) + ".java");
            fileOutputStream = new FileOutputStream(outputFile);
            outputStream = new DataOutputStream(fileOutputStream);

            outputStream.writeBytes("import streamit.*;\n\n");

            outputStream.writeBytes(
                "public class " + getName(iter) + " extends StreamIt\n");
            outputStream.writeBytes("{\n");

            outputStream.writeBytes("  public static void main (String [] args)\n");
            outputStream.writeBytes("  {\n");
            outputStream.writeBytes("    new " + getName(iter) + " ().run (args);\n");
            outputStream.writeBytes("  }\n");

            printPipe(iter.isPipeline(), outputStream);

            outputStream.writeBytes("}\n");
        }
        catch (Throwable e)
        {
            ERROR(e);
        }
    }

    public void printStream(Iterator iter)
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
        DataOutputStream outputStream;

        try
        {
            outputFile = new File(getName(iter) + ".java");
            fileOutputStream = new FileOutputStream(outputFile);
            outputStream = new DataOutputStream(fileOutputStream);

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
        return name.replace('@', '_').replace('$', '_').replace('.','_').replace(' ','_').replace('=','_').replace(',','_');
    }

    public String getName(IteratorBase iter)
    {
        return getName(iter.getObject());
    }

    public void printFilter(FilterIter filter, DataOutputStream outputStream)
    {
        try
        {
            /*
             * map maps the object returned by getInitFunctionStage and 
             * getWorkFunctionPhase to a Pair<nFunction, isInit>
             */
            OMap map = new OMap();

            outputStream.writeBytes("  public void init ()\n  {\n");

            boolean peeks = false, pops = false, pushes = false;

            for (int nInitPhase = 0;
                nInitPhase < filter.getNumInitStages();
                nInitPhase++)
            {
                outputStream.writeBytes(
                    "    addInitPhase("
                        + filter.getInitPeekStage(nInitPhase)
                        + ", "
                        + filter.getInitPopStage(nInitPhase)
                        + ", "
                        + filter.getInitPushStage(nInitPhase)
                        + ", \""
                        + getName(filter.getInitFunctionStage(nInitPhase))
                        + "\");\n");
                map.insert(
                    filter.getInitFunctionStage(nInitPhase),
                    new Pair(new Integer(nInitPhase), new Boolean(true)));

                peeks |= (filter.getInitPeekStage(nInitPhase) != 0);
                pops |= (filter.getInitPopStage(nInitPhase) != 0);
                pushes |= (filter.getInitPushStage(nInitPhase) != 0);
            }

            for (int nWorkPhase = 0;
                nWorkPhase < filter.getNumWorkPhases();
                nWorkPhase++)
            {
                outputStream.writeBytes(
                    "    addSteadyPhase("
                        + filter.getPeekPhase(nWorkPhase)
                        + ", "
                        + filter.getPopPhase(nWorkPhase)
                        + ", "
                        + filter.getPushPhase(nWorkPhase)
                        + ", \""
                        + getName(filter.getWorkFunctionPhase(nWorkPhase))
                        + "\");\n");

                map.insert(
                    filter.getWorkFunctionPhase(nWorkPhase),
                    new Pair(new Integer(nWorkPhase), new Boolean(false)));

                peeks |= (filter.getPeekPhase(nWorkPhase) != 0);
                pops |= (filter.getPopPhase(nWorkPhase) != 0);
                pushes |= (filter.getPushPhase(nWorkPhase) != 0);
            }

            if (peeks || pops || pushes)
            {
                outputStream.writeBytes("    setIOTypes (");

                if (peeks || pops)
                {
                    // cannot peek or pop but not do the other!
                    assert peeks && pops;
                    outputStream.writeBytes("Integer.TYPE, ");
                }
                else
                    outputStream.writeBytes("null, ");

                if (pushes)
                {
                    outputStream.writeBytes("Integer.TYPE);\n");
                }
                else
                {
                    outputStream.writeBytes("null);\n");
                }
            }

            outputStream.writeBytes("  }\n");

            for (OMapIterator func = map.begin();
                !func.equals(map.end());
                func.next())
            {
                outputStream.writeBytes(
                    "  public void " + getName(func.getKey()) + " ()\n  {\n");
                int o, e, u;
                boolean isInitFunc =
                    ((Boolean) ((Pair)func.getData()).getSecond())
                        .booleanValue();
                int funcNum =
                    ((Integer) ((Pair)func.getData()).getFirst()).intValue();
                if (isInitFunc)
                {
                    e = filter.getInitPeekStage(funcNum);
                    o = filter.getInitPopStage(funcNum);
                    u = filter.getInitPushStage(funcNum);
                }
                else
                {
                    e = filter.getPeekPhase(funcNum);
                    o = filter.getPopPhase(funcNum);
                    u = filter.getPushPhase(funcNum);
                }

                if (e != 0)
                    outputStream.writeBytes("    input.peekInt(" + (e - 1) + ");\n");
                if (o != 0)
                {
                    outputStream.writeBytes(
                        "    for (int n = 0; n < " + o + "; n++)\n");
                    outputStream.writeBytes("      input.popInt();\n");
                }
                if (u != 0)
                {
                    outputStream.writeBytes(
                        "    for (int n = 0; n < " + u + "; n++)\n");
                    outputStream.writeBytes("      output.pushInt(0);\n");
                }

                outputStream.writeBytes("  }\n");
            }
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
            outputStream.writeBytes("  public void init ()\n  {\n");

            for (int nChild = 0; nChild < pipeline.getNumChildren(); nChild++)
            {
                outputStream.writeBytes(
                    "    add(new "
                        + getName(pipeline.getChild(nChild))
                        + " ());\n");
            }

            outputStream.writeBytes("  }\n");
        }
        catch (Throwable e)
        {
            ERROR(e);
        }
        for (int nChild = 0; nChild < pipeline.getNumChildren(); nChild++)
        {
            printStream(pipeline.getChild(nChild));
        }
    }

    public void printSJ(SplitJoinIter sj, DataOutputStream outputStream)
    {
        try
        {
            outputStream.writeBytes("  public void init ()\n  {\n");

            assert sj.getSplitterNumWork() == 1;

            if (sj.getSplitPop(0) == 0)
                outputStream.writeBytes("    setSplitter (NULL ());\n");
            else if (sj.getSplitPop(0) == 1)
                outputStream.writeBytes("    setSplitter (DUPLICATE ());\n");
            else
            {
                outputStream.writeBytes("    setSplitter (WEIGHTED_ROUND_ROBIN (");

                int nOut;
                for (nOut = 0; nOut < sj.getFanOut() - 1; nOut++)
                {
                    outputStream.writeBytes(sj.getSplitPushWeights(0)[nOut] + ", ");
                }
                outputStream.writeBytes(sj.getSplitPushWeights(0)[nOut] + "));\n");
            }
            for (int nChild = 0; nChild < sj.getNumChildren(); nChild++)
            {
                outputStream.writeBytes(
                    "    add(new " + getName(sj.getChild(nChild)) + " ());\n");
            }
            if (sj.getJoinPush(0) == 0)
                outputStream.writeBytes("    setJoiner (NULL ());\n");
	    else
            {
                outputStream.writeBytes("    setJoiner (WEIGHTED_ROUND_ROBIN (");

                int nIn;
                for (nIn = 0; nIn < sj.getFanIn() - 1; nIn++)
                {
                    outputStream.writeBytes(sj.getJoinPopWeights(0)[nIn] + ", ");
                }
                outputStream.writeBytes(sj.getJoinPopWeights(0)[nIn] + "));\n");
            }

            outputStream.writeBytes("  }\n");
        }
        catch (Throwable e)
        {
            ERROR(e);
        }
        for (int nChild = 0; nChild < sj.getNumChildren(); nChild++)
        {
            printStream(sj.getChild(nChild));
        }
    }

    public void printFL(FeedbackLoopIter fl, DataOutputStream outputStream)
    {
        try
        {
            outputStream.writeBytes("  public void init ()\n  {\n");

            assert fl.getSplitterNumWork() == 1;

            if (fl.getSplitPop(0) == 1)
                outputStream.writeBytes("    setSplitter (DUPLICATE ());\n");
            else
            {
                outputStream.writeBytes("    setSplitter (WEIGHTED_ROUND_ROBIN (");

                int nOut;
                for (nOut = 0; nOut < fl.getFanOut() - 1; nOut++)
                {
                    outputStream.writeBytes(fl.getSplitPushWeights(0)[nOut] + ", ");
                }
                outputStream.writeBytes(fl.getSplitPushWeights(0)[nOut] + "));\n");
            }

            outputStream.writeBytes("    setDelay (" + fl.getDelaySize() + ");\n");
            outputStream.writeBytes(
                "    setBody (new " + getName(fl.getBodyChild()) + " ());\n");
            outputStream.writeBytes(
                "    setLoop (new " + getName(fl.getLoopChild()) + " ());\n");

            {
                outputStream.writeBytes("    setJoiner (WEIGHTED_ROUND_ROBIN (");

                int nIn;
                for (nIn = 0; nIn < fl.getFanIn() - 1; nIn++)
                {
                    outputStream.writeBytes(fl.getJoinPopWeights(0)[nIn] + ", ");
                }
                outputStream.writeBytes(fl.getJoinPopWeights(0)[nIn] + "));\n");
            }

            outputStream.writeBytes("  }\n");
            
            outputStream.writeBytes("  public int initPathInt (int i)\n  {\n");
            outputStream.writeBytes("    return 0;\n");
            outputStream.writeBytes("  }\n");
        }
        catch (Throwable e)
        {
            ERROR(e);
        }
        printStream(fl.getBodyChild());
        printStream(fl.getLoopChild());
    }
}
