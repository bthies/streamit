package streamit.scheduler;

import streamit.misc.DestroyedClass;
import streamit.misc.UniquePairContainer;
import streamit.misc.Pair;
import java.util.Map;
import java.util.HashMap;
import streamit.scheduler.iriter./*persistent.*/
Iterator;
import streamit.scheduler.iriter./*persistent.*/
FilterIter;
import streamit.scheduler.iriter./*persistent.*/
PipelineIter;
import streamit.scheduler.iriter./*persistent.*/
SplitJoinIter;
import streamit.scheduler.iriter./*persistent.*/
FeedbackLoopIter;
import streamit.scheduler.Schedule;

/* $Id: ScheduleBuffers.java,v 1.4 2002-07-02 03:37:42 karczma Exp $ */

/**
 * This class uses a valid schedule and an iterator to determine 
 * the size of buffers required to execute the schedule.  The class
 * makes an assumption that the buffers will not be shared.
 * 
 * @version 2
 * @author  Michal Karczmarek
 */

public class ScheduleBuffers extends DestroyedClass
{
    final private Map bufferSizes = new HashMap();
    final private UniquePairContainer pairs = new UniquePairContainer();

    public ScheduleBuffers(Iterator root)
    {
        traverseStream(root);
    }

    public int getBufferSizeBetween(
        streamit.scheduler.iriter.Iterator userBefore,
        streamit.scheduler.iriter.Iterator userAfter)
    {
        Iterator before = (Iterator) user2persistent.get(userBefore);
        Iterator after = (Iterator) user2persistent.get(userAfter);
        Pair beforeAfter = pairs.getPair(before, after);
        BufferStatus status = (BufferStatus) bufferSizes.get(beforeAfter);
        return status.getBufferSize();
    }

    final private Map user2persistent = new HashMap();
    final private Map firstChildStream = new HashMap();
    final private Map lastChildStream = new HashMap();
    final private Map workFunctions = new HashMap();

    final private Map targetBufferBefore = new HashMap();
    final private Map targetBufferAfter = new HashMap();

    private Iterator getFirstStream(Iterator stream)
    {
        Object firstStream = firstChildStream.get(stream);

        // does this stream have a first child?
        if (firstStream != null)
        {
            // yes - maybe that first child also has a first child?
            return getFirstStream((Iterator) firstStream);
        }
        else
        {
            // no - just return self then
            return stream;
        }
    }

    private Iterator getLastStream(Iterator stream)
    {
        Object lastStream = lastChildStream.get(stream);

        // does this stream have a last child?
        if (lastStream != null)
        {
            // yes - maybe that last child also has a last child?
            return getLastStream((Iterator) lastStream);
        }
        else
        {
            // no - just return self then
            return stream;
        }
    }

    private void traverseStream(Iterator stream)
    {
        user2persistent.put(stream /*.getPersistentIterator()*/
        , stream);

        if (stream.isFilter() != null)
        {
            FilterIter filter = stream.isFilter();

            // store all the filter's init functions
            {
                int nStage = 0;
                for (; nStage < filter.getNumInitStages(); nStage++)
                {
                    workFunctions.put(
                        pairs.getPair(
                            filter.getInitFunctionStage(nStage),
                            stream),
                        new Integer(nStage));
                }
            }

            // store all the filter's work functions
            {
                int nPhase = 0;
                for (; nPhase < filter.getNumWorkPhases(); nPhase++)
                {
                    workFunctions.put(
                        pairs.getPair(
                            filter.getWorkFunctionPhase(nPhase),
                            stream),
                        new Integer(nPhase));
                }
            }
        }
        else if (stream.isPipeline() != null)
        {
            PipelineIter pipeline = stream.isPipeline();

            // traverse all the children
            {
                int nChild = 0;
                for (; nChild < pipeline.getNumChildren(); nChild++)
                {
                    traverseStream(pipeline.getChild(nChild));
                }
            }

            // store the first and lastchildren of this pipeline            
            firstChildStream.put(stream, pipeline.getChild(0));
            lastChildStream.put(
                stream,
                pipeline.getChild(pipeline.getNumChildren() - 1));

            // enter all the buffers:
            {
                int nChild = 0;
                for (; nChild < pipeline.getNumChildren() - 1; nChild++)
                {
                    Iterator childBefore = pipeline.getChild(nChild);
                    Iterator childAfter = pipeline.getChild(nChild + 1);

                    Pair pair = pairs.getPair(childBefore, childAfter);
                    BufferStatus buffer = new BufferStatus();

                    // buffer pair:
                    bufferSizes.put(pair, buffer);

                    // buffer after:
                    targetBufferAfter.put(
                        getLastStream(childBefore),
                        buffer);

                    // buffer before:
                    targetBufferBefore.put(
                        getFirstStream(childAfter),
                        buffer);
                }
            }
        }
        else if (stream.isSplitJoin() != null)
        {
            SplitJoinIter splitjoin = stream.isSplitJoin();

            // traverse all the children
            {
                int nChild = 0;
                for (; nChild < splitjoin.getNumChildren(); nChild++)
                {
                    traverseStream(splitjoin.getChild(nChild));
                }
            }

            // store all the split's work functions
            {
                int nPhase = 0;
                for (; nPhase < splitjoin.getSplitterNumWork(); nPhase++)
                {
                    workFunctions.put(
                        pairs.getPair(
                            splitjoin.getSplitterWork(nPhase),
                            stream),
                        new Integer(nPhase));
                }
            }

            // store all the join's work functions
            {
                int nPhase = 0;
                for (; nPhase < splitjoin.getJoinerNumWork(); nPhase++)
                {
                    workFunctions.put(
                        pairs.getPair(
                            splitjoin.getJoinerWork(nPhase),
                            stream),
                        new Integer(nPhase));
                }
            }

            // enter all the buffers:
            {
                int nChild = 0;
                for (; nChild < splitjoin.getNumChildren(); nChild++)
                {
                    Iterator child = splitjoin.getChild(nChild);

                    // create a buffer between sj and the child
                    // (the one at the top of the sj, 
                    // above child, below splitter
                    {
                        Pair pair = pairs.getPair(stream, child);
                        BufferStatus buffer = new BufferStatus();

                        // buffer pair:
                        bufferSizes.put(pair, buffer);

                        // buffer before the child:
                        targetBufferBefore.put(
                            getFirstStream(child),
                            buffer);

                        // buffer after the splitter:
                        // don't store this data, as it will be ambiguous
                        // handle this as a special case when actually running
                        // the schedule
                    }

                    // create a buffer between sj and the child
                    // (the one at the bottom of the sj, 
                    // below child, above joiner)
                    {
                        Pair pair = pairs.getPair(child, stream);
                        BufferStatus buffer = new BufferStatus();

                        // buffer pair:
                        bufferSizes.put(pair, buffer);

                        // buffer after the child:
                        targetBufferAfter.put(
                            getLastStream(child),
                            buffer);

                        // buffer before the joiner:
                        // don't store this data, as it will be ambiguous
                        // handle this as a special case when actually running
                        // the schedule
                    }
                }
            }
        }
        else if (stream.isFeedbackLoop() != null)
        {
            // not done yet
            ASSERT(false);
        }
        else
            ERROR("stream variable is not a known stream type!");
    }

    private class BufferStatus
    {
        int bufferMax = 0;
        int bufferCurrent = 0;

        void pushData(int numData)
        {
            bufferCurrent += numData;
            bufferMax = MAX(bufferMax, bufferCurrent);
        }

        void popData(int numData)
        {
            bufferCurrent -= numData;
            ASSERT(bufferCurrent >= 0);
        }

        void peekData(int numData)
        {
            ASSERT(bufferCurrent >= numData);
        }

        public int getBufferSize()
        {
            return bufferMax;
        }
    }

    public void computeBuffersFor(Schedule schedule)
    {
        int numExecs = schedule.getNumReps();
        for (; numExecs > 0; numExecs--)
        {
            // is this a schedule that contains other schedules?
            if (!schedule.isBottomSchedule())
            {
                // yes - just run through other schedules!
                int schedNum = 0;
                for (; schedNum < schedule.getNumPhases(); schedNum++)
                {
                    computeBuffersFor(schedule.getSubSched(schedNum));
                }
            }
            else
            {
                // no - this is a bona-fide work function
                Object workFunc = schedule.getWorkFunc();
                Iterator workStream =
                    (Iterator) user2persistent.get(
                        schedule.getWorkStream());

                // figure out what object contributed this work function:
                Pair workInfo = pairs.getPair(workFunc, workStream);
                int numWork =
                    ((Integer) workFunctions.get(workInfo)).intValue();

                if (workStream.isFilter() != null)
                {
                    FilterIter filter = workStream.isFilter();
                    int peekAmount, popAmount, pushAmount;

                    // check if the function is a work or init function
                    // and get appropriate peek/pop/push values
                    if (filter.getWorkFunctionPhase(numWork) == workFunc)
                    {
                        // work function
                        peekAmount = filter.getPeekPhase(numWork);
                        popAmount = filter.getPopPhase(numWork);
                        pushAmount = filter.getPushPhase(numWork);
                    }
                    else
                    {
                        // init function
                        ASSERT(
                            filter.getInitFunctionStage(numWork)
                                == workFunc);

                        peekAmount = filter.getInitPeekStage(numWork);
                        popAmount = filter.getInitPushStage(numWork);
                        pushAmount = filter.getInitPushStage(numWork);
                    }

                    // update the buffers appropriately

                    // update buffer before, only if it's used
                    if (peekAmount > 0)
                    {
                        BufferStatus bufferBefore =
                            (BufferStatus) targetBufferBefore.get(
                                workStream);
                        bufferBefore.peekData(peekAmount);
                        bufferBefore.popData(popAmount);
                    }

                    // update buffer after only if it's used
                    if (pushAmount > 0)
                    {
                        BufferStatus bufferAfter =
                            (BufferStatus) targetBufferAfter.get(
                                workStream);

                        bufferAfter.pushData(pushAmount);
                    }
                }
                else if (workStream.isPipeline() != null)
                {
                    ERROR("Pipeline is not allowed to have any work functions!");
                }
                else if (workStream.isSplitJoin() != null)
                {
                    SplitJoinIter sj = workStream.isSplitJoin();
                    int popAmount, pushAmount;

                    // check if the function is a splitter or joiner function
                    // and get appropriate pop/push values
                    // also update the internal buffers!
                    if (sj.getSplitterWork(numWork) == workFunc)
                    {
                        // splitter function
                        popAmount = sj.getSplitPop(numWork);
                        pushAmount = 0;
                        
                        // push data into the internal splitter buffers
                        int nChild;
                        int pushWeights[] = sj.getSplitPushWeights(numWork);
                        for (nChild = 0; nChild < sj.getNumChildren(); nChild++)
                        {
                            Iterator firstChild = getFirstStream(sj.getChild(nChild));
                            BufferStatus bufferBefore = (BufferStatus) targetBufferBefore.get(firstChild);
                            bufferBefore.pushData(pushWeights[nChild]);
                        }
                    } else {
                        // joiner function
                        popAmount = 0;
                        pushAmount = sj.getJoinPush(numWork);
                        
                        // push data into the internal joiner buffers
                        int nChild;
                        int popWeights[] = sj.getJoinPopWeights(numWork);
                        for (nChild = 0; nChild < sj.getNumChildren(); nChild++)
                        {
                            Iterator lastChild = getLastStream(sj.getChild(nChild));
                            BufferStatus bufferAfter = (BufferStatus) targetBufferAfter.get(lastChild);
                            bufferAfter.popData(popWeights[nChild]);
                        }
                    }

                    // update buffer before, only if it's used
                    if (popAmount > 0)
                    {
                        BufferStatus bufferBefore =
                            (BufferStatus) targetBufferBefore.get(
                                workStream);
                        bufferBefore.popData(popAmount);
                    }

                    // update buffer after only if it's used
                    if (pushAmount > 0)
                    {
                        BufferStatus bufferAfter =
                            (BufferStatus) targetBufferAfter.get(
                                workStream);

                        bufferAfter.pushData(pushAmount);
                    }
                }
                else if (workStream.isFeedbackLoop() != null)
                {
                    // not done yet
                    ASSERT(false);
                }
                else
                    ERROR("stream variable is not a known stream type!");
            }
        }

    }
}
