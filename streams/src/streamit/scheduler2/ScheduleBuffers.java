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

package streamit.scheduler2;

import streamit.misc.DestroyedClass;
import streamit.misc.UniquePairContainer;
import streamit.misc.Pair;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import streamit.scheduler2.iriter./*persistent.*/
Iterator;
import streamit.scheduler2.iriter./*persistent.*/
FilterIter;
import streamit.scheduler2.iriter./*persistent.*/
PipelineIter;
import streamit.scheduler2.iriter./*persistent.*/
SplitJoinIter;
import streamit.scheduler2.iriter./*persistent.*/
FeedbackLoopIter;
import streamit.scheduler2.Schedule;

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
        streamit.scheduler2.iriter.Iterator userBefore,
        streamit.scheduler2.iriter.Iterator userAfter)
    {
        Iterator before = (Iterator)user2persistent.get(userBefore);
        Iterator after = (Iterator)user2persistent.get(userAfter);
        Pair beforeAfter = pairs.getPair(before, after);
        BufferStatus status = (BufferStatus)bufferSizes.get(beforeAfter);
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
            return getFirstStream((Iterator)firstStream);
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
            return getLastStream((Iterator)lastStream);
        }
        else
        {
            // no - just return self then
            return stream;
        }
    }

    private void traverseStream(Iterator stream)
    {
        user2persistent.put(stream, stream);

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
                        targetBufferAfter.put(getLastStream(child), buffer);

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
            FeedbackLoopIter feedbackLoop = stream.isFeedbackLoop();
            Iterator body = feedbackLoop.getBodyChild();
            Iterator loop = feedbackLoop.getLoopChild();

            // traverse the children
            {
                traverseStream(body);
                traverseStream(loop);
            }

            // store all the split's work functions
            {
                int nPhase = 0;
                for (;
                    nPhase < feedbackLoop.getSplitterNumWork();
                    nPhase++)
                {
                    workFunctions.put(
                        pairs.getPair(
                            feedbackLoop.getSplitterWork(nPhase),
                            stream),
                        new Integer(nPhase));
                }
            }

            // store all the join's work functions
            {
                int nPhase = 0;
                for (; nPhase < feedbackLoop.getJoinerNumWork(); nPhase++)
                {
                    workFunctions.put(
                        pairs.getPair(
                            feedbackLoop.getJoinerWork(nPhase),
                            stream),
                        new Integer(nPhase));
                }
            }

            // store the body's buffers
            {

                // create a buffer between feedbackLoop and the body
                // (the one at the top of the feedbackLoop, 
                // above body, below joiner
                {
                    Pair pair = pairs.getPair(stream, body);
                    BufferStatus buffer = new BufferStatus();

                    // buffer pair:
                    bufferSizes.put(pair, buffer);

                    // buffer before the body:
                    targetBufferBefore.put(getFirstStream(body), buffer);

                    // buffer after the joiner:
                    // don't store this data, as it will be ambiguous
                    // handle this as a special case when actually running
                    // the schedule
                }

                // create a buffer between feedbackLoop and the body
                // (the one at the bottom of the feedbackLoop, 
                // below body, above splitter)
                {
                    Pair pair = pairs.getPair(body, stream);
                    BufferStatus buffer = new BufferStatus();

                    // buffer pair:
                    bufferSizes.put(pair, buffer);

                    // buffer after the body:
                    targetBufferAfter.put(getLastStream(body), buffer);

                    // buffer before the splitter:
                    // don't store this data, as it will be ambiguous
                    // handle this as a special case when actually running
                    // the schedule
                }
            }

            // store the loop's buffers
            {

                // create a buffer between feedbackLoop and the loop
                // (the one at the top of the feedbackLoop, 
                // above loop, below joiner
                {
                    Pair pair = pairs.getPair(loop, stream);
                    BufferStatus buffer = new BufferStatus();

                    // initialize the buffer with amount of data pushed
                    // due to delay initialization!
                    buffer.pushData(feedbackLoop.getDelaySize());

                    // buffer pair:
                    bufferSizes.put(pair, buffer);

                    // buffer before the loop:
                    targetBufferAfter.put(getLastStream(loop), buffer);

                    // buffer after the joiner:
                    // don't store this data, as it will be ambiguous
                    // handle this as a special case when actually running
                    // the schedule
                }

                // create a buffer between feedbackLoop and the loop
                // (the one at the bottom of the feedbackLoop, 
                // below loop, above splitter)
                {
                    Pair pair = pairs.getPair(stream, loop);
                    BufferStatus buffer = new BufferStatus();

                    // buffer pair:
                    bufferSizes.put(pair, buffer);

                    // buffer after the loop:
                    targetBufferBefore.put(getFirstStream(loop), buffer);

                    // buffer before the splitter:
                    // don't store this data, as it will be ambiguous
                    // handle this as a special case when actually running
                    // the schedule
                }
            }
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
            assert bufferCurrent >= 0;
        }

        void peekData(int numData)
        {
            assert bufferCurrent >= numData;
        }

        public int getBufferSize()
        {
            return bufferMax;
        }
    }

    private class BufferDelta
    {
        int bufferMax = 0;
        int bufferCurrent = 0;

        void pushData(int numData)
        {
            assert numData >= 0;
            bufferCurrent += numData;
            bufferMax = MAX(bufferMax, bufferCurrent);
        }

        void popData(int numData)
        {
            assert numData >= 0;
            bufferCurrent -= numData;
        }

        void combineWith(BufferDelta other)
        {
            bufferMax =
                MAX(bufferMax, bufferCurrent + other.getBufferExpansion());

            bufferCurrent += other.getBufferDelta();
        }

        public int getBufferDelta()
        {
            return bufferCurrent;
        }

        public int getBufferExpansion()
        {
            return bufferMax;
        }
    }

    /*
     * use the following structure to keep track of buffer deltas:
     * 
     * Map (phase -> delta map)
     * 
     * delta map = Map (buffer -> delta + null -> buffer set)
     * 
     * 
     */

    void addToMap(BufferStatus buffer, BufferDelta delta, Map map)
    {
        Set buffers = (Set) (map.get(null));
        buffers.add(buffer);

        map.put(buffer, delta);
    }

    void combineBufferExecutions(Map left, Map right)
    {
        Set buffers = (Set) (right.get(null));
        assert buffers != null;

        java.util.Iterator iter = buffers.iterator();
        while (iter.hasNext())
        {
            BufferStatus buffer = (BufferStatus)iter.next();
            BufferDelta delta = (BufferDelta)right.get(buffer);

            BufferDelta start = (BufferDelta)left.get(buffer);
            if (start == null)
            {
                start = new BufferDelta();
                addToMap(buffer, start, left);
            }

            start.combineWith(delta);
        }
    }

    Map phase2deltaMap = new HashMap();

    public void computeBuffersFor(Schedule schedule)
    {
        Map map = computeBuffersMap(schedule, 1);

        Set buffers = (Set)map.get(null);
        java.util.Iterator iter = buffers.iterator();
        while (iter.hasNext())
        {
            BufferStatus buffer = (BufferStatus)iter.next();
            BufferDelta delta = (BufferDelta)map.get(buffer);

            buffer.pushData(delta.getBufferExpansion());
            buffer.popData(
                delta.getBufferExpansion() - delta.getBufferDelta());
        }
    }

    Map computeBuffersMap(Schedule schedule, int numExecs)
    {
        Map deltas = (Map)phase2deltaMap.get(schedule);

        if (deltas == null)
        {
            // new a new deltas
            deltas = new HashMap();
            Set buffers = new HashSet();
            deltas.put(null, buffers);
            assert deltas.get(null) == buffers;

            // put the original code here
            {
                // is this a schedule that contains other schedules?
                if (!schedule.isBottomSchedule())
                {
                    // yes - just run through other schedules!
                    int schedNum = 0;
                    for (; schedNum < schedule.getNumPhases(); schedNum++)
                    {
                        Map subDeltas =
                            computeBuffersMap(
                                schedule.getSubSched(schedNum),
                                schedule.getSubSchedNumExecs(schedNum));
                        combineBufferExecutions(deltas, subDeltas);
                    }
                }
                else
                {
                    // no - this is a bona-fide work function
                    Object workFunc = schedule.getWorkFunc();
                    Iterator workStream =
                        (Iterator)user2persistent.get(schedule.getStream());

                    // figure out what object contributed this work function:
                    Pair workInfo = pairs.getPair(workFunc, workStream);
                    int numWork =
                        ((Integer)workFunctions.get(workInfo)).intValue();

                    if (workStream.isFilter() != null)
                    {
                        FilterIter filter = workStream.isFilter();
                        int peekAmount, popAmount, pushAmount;

                        // check if the function is a work or init function
                        // and get appropriate peek/pop/push values
                        if (filter.getWorkFunctionPhase(numWork)
                            == workFunc)
                        {
                            // work function
                            peekAmount = filter.getPeekPhase(numWork);
                            popAmount = filter.getPopPhase(numWork);
                            pushAmount = filter.getPushPhase(numWork);
                        }
                        else
                        {
                            // init function
                            assert filter.getInitFunctionStage(numWork)
                                == workFunc;

                            peekAmount = filter.getInitPeekStage(numWork);
                            popAmount = filter.getInitPopStage(numWork);
                            pushAmount = filter.getInitPushStage(numWork);
                        }

                        // update the buffers appropriately

                        // update buffer before, only if it's used
                        if (peekAmount > 0)
                        {
                            BufferStatus bufferBefore =
                                (BufferStatus)targetBufferBefore.get(
                                    workStream);
                            /*
                            bufferBefore.peekData(peekAmount);
                            bufferBefore.popData(popAmount);
                            */
                            BufferDelta smallDelta = new BufferDelta();
                            smallDelta.popData(popAmount);

                            buffers.add(bufferBefore);
                            deltas.put(bufferBefore, smallDelta);
                        }

                        // update buffer after only if it's used
                        if (pushAmount > 0)
                        {
                            BufferStatus bufferAfter =
                                (BufferStatus)targetBufferAfter.get(
                                    workStream);

                            /*
                            bufferAfter.pushData(pushAmount);
                            */

                            BufferDelta smallDelta = new BufferDelta();
                            smallDelta.pushData(pushAmount);

                            buffers.add(bufferAfter);
                            deltas.put(bufferAfter, smallDelta);
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
                        if (sj.getSplitterNumWork() > numWork
                            && sj.getSplitterWork(numWork) == workFunc)
                        {
                            // splitter function
                            popAmount = sj.getSplitPop(numWork);
                            pushAmount = 0;

                            // push data into the internal splitter buffers
                            int nChild;
                            int pushWeights[] =
                                sj.getSplitPushWeights(numWork);
                            for (nChild = 0;
                                nChild < sj.getNumChildren();
                                nChild++)
                            {
                                Iterator firstChild =
                                    getFirstStream(sj.getChild(nChild));
                                BufferStatus bufferBefore =
                                    (BufferStatus)targetBufferBefore.get(
                                        firstChild);

                                /*        
                                bufferBefore.pushData(pushWeights[nChild]);
                                */

                                BufferDelta smallDelta = new BufferDelta();
                                smallDelta.pushData(pushWeights[nChild]);

                                buffers.add(bufferBefore);
                                deltas.put(bufferBefore, smallDelta);
                            }
                        }
                        else
                        {
                            // joiner function
                            popAmount = 0;
                            pushAmount = sj.getJoinPush(numWork);

                            // push data into the internal joiner buffers
                            int nChild;
                            int popWeights[] =
                                sj.getJoinPopWeights(numWork);
                            for (nChild = 0;
                                nChild < sj.getNumChildren();
                                nChild++)
                            {
                                Iterator lastChild =
                                    getLastStream(sj.getChild(nChild));
                                BufferStatus bufferAfter =
                                    (BufferStatus)targetBufferAfter.get(
                                        lastChild);

                                /*
                                bufferAfter.popData(popWeights[nChild]);
                                */

                                BufferDelta smallDelta = new BufferDelta();
                                smallDelta.popData(popWeights[nChild]);

                                buffers.add(bufferAfter);
                                deltas.put(bufferAfter, smallDelta);
                            }
                        }

                        // update buffer before, only if it's used
                        if (popAmount > 0)
                        {
                            BufferStatus bufferBefore =
                                (BufferStatus)targetBufferBefore.get(
                                    workStream);

                            /*
                            bufferBefore.popData(popAmount);
                            */

                            BufferDelta smallDelta = new BufferDelta();
                            smallDelta.popData(popAmount);

                            buffers.add(bufferBefore);
                            deltas.put(bufferBefore, smallDelta);
                        }

                        // update buffer after only if it's used
                        if (pushAmount > 0)
                        {
                            BufferStatus bufferAfter =
                                (BufferStatus)targetBufferAfter.get(
                                    workStream);

                            /*
                            bufferAfter.pushData(pushAmount);
                            */

                            BufferDelta smallDelta = new BufferDelta();
                            smallDelta.pushData(pushAmount);

                            buffers.add(bufferAfter);
                            deltas.put(bufferAfter, smallDelta);
                        }
                    }
                    else if (workStream.isFeedbackLoop() != null)
                    {
                        FeedbackLoopIter feedbackLoop =
                            workStream.isFeedbackLoop();
                        Iterator body = feedbackLoop.getBodyChild();
                        Iterator loop = feedbackLoop.getLoopChild();

                        int popAmount, pushAmount;

                        // check if the function is a splitter or joiner function
                        // and get appropriate pop/push values
                        // also update the internal buffers!
                        if (feedbackLoop.getSplitterWork(numWork)
                            == workFunc)
                        {
                            // splitter function
                            popAmount = 0;
                            pushAmount =
                                feedbackLoop.getSplitPushWeights(
                                    numWork)[0];

                            // pop data from the internal body-splitter buffer
                            {
                                BufferStatus bodyBuffer =
                                    (BufferStatus)targetBufferAfter.get(
                                        getLastStream(body));

                                /*
                                bodyBuffer.popData(
                                    feedbackLoop.getSplitPop(numWork));
                                */

                                BufferDelta smallDelta = new BufferDelta();
                                smallDelta.popData(
                                    feedbackLoop.getSplitPop(numWork));

                                buffers.add(bodyBuffer);
                                deltas.put(bodyBuffer, smallDelta);
                            }

                            // push data to the internal splitter-loop buffer
                            {
                                BufferStatus loopBuffer =
                                    (BufferStatus)targetBufferBefore.get(
                                        getFirstStream(loop));

                                /*
                                loopBuffer.pushData(
                                    feedbackLoop.getSplitPushWeights(
                                        numWork)[1]);
                                */

                                BufferDelta smallDelta = new BufferDelta();
                                smallDelta.pushData(
                                    feedbackLoop.getSplitPushWeights(
                                        numWork)[1]);

                                buffers.add(loopBuffer);
                                deltas.put(loopBuffer, smallDelta);
                            }

                        }
                        else
                        {
                            // joiner function
                            popAmount =
                                feedbackLoop.getJoinPopWeights(numWork)[0];
                            pushAmount = 0;

                            // push data to the internal joiner-body buffer
                            {
                                BufferStatus bodyBuffer =
                                    (BufferStatus)targetBufferBefore.get(
                                        getFirstStream(body));

                                /*
                                bodyBuffer.pushData(
                                    feedbackLoop.getJoinPush(numWork));
                                */

                                BufferDelta smallDelta = new BufferDelta();
                                smallDelta.pushData(
                                    feedbackLoop.getJoinPush(numWork));

                                buffers.add(bodyBuffer);
                                deltas.put(bodyBuffer, smallDelta);
                            }

                            // pop data from the internal loop-joiner buffer
                            {
                                BufferStatus loopBuffer =
                                    (BufferStatus)targetBufferAfter.get(
                                        getLastStream(loop));

                                /*
                                loopBuffer.popData(
                                    feedbackLoop.getJoinPopWeights(
                                        numWork)[1]);
                                */

                                BufferDelta smallDelta = new BufferDelta();
                                smallDelta.popData(
                                    feedbackLoop.getJoinPopWeights(
                                        numWork)[1]);

                                buffers.add(loopBuffer);
                                deltas.put(loopBuffer, smallDelta);
                            }
                        }

                        // update buffer before, only if it's used
                        if (popAmount > 0)
                        {
                            BufferStatus bufferBefore =
                                (BufferStatus)targetBufferBefore.get(
                                    workStream);

                            /*
                            bufferBefore.popData(popAmount);
                            */

                            BufferDelta smallDelta = new BufferDelta();
                            smallDelta.popData(popAmount);

                            buffers.add(bufferBefore);
                            deltas.put(bufferBefore, smallDelta);
                        }

                        // update buffer after only if it's used
                        if (pushAmount > 0)
                        {
                            BufferStatus bufferAfter =
                                (BufferStatus)targetBufferAfter.get(
                                    workStream);

                            /*
                            bufferAfter.pushData(pushAmount);
                            */

                            BufferDelta smallDelta = new BufferDelta();
                            smallDelta.pushData(pushAmount);

                            buffers.add(bufferAfter);
                            deltas.put(bufferAfter, smallDelta);
                        }
                    }
                    else
                        ERROR("stream variable is not a known stream type!");
                    assert deltas.get(null) != null;
                }
            }

            phase2deltaMap.put(schedule, deltas);
        }

        // add deltas to itself schedule.getNumReps times
        if (numExecs > 1)
        {
            Object schedNumExecs =
                pairs.getPair(schedule, new Integer(numExecs));

            Map deltasNumExecs = (Map)phase2deltaMap.get(schedNumExecs);
            if (deltasNumExecs == null)
            {
                deltasNumExecs = new HashMap();
                deltasNumExecs.put(null, new HashSet());
                assert deltasNumExecs.get(null) != null;

                for (int n = 0; n < numExecs; n++)
                {
                    combineBufferExecutions(deltasNumExecs, deltas);
                }

                assert deltas.get(null) != null;
                assert deltasNumExecs.get(null) != null;

                phase2deltaMap.put(schedNumExecs, deltasNumExecs);
            }

            deltas = deltasNumExecs;
        }

        assert deltas != null;

        return deltas;

    }
}
