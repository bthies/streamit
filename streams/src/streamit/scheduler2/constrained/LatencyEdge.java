package streamit.scheduler2.constrained;

import streamit.misc.Misc;
import streamit.scheduler2.SDEPData;
import java.math.BigInteger;

public class LatencyEdge extends Misc implements SDEPData
{
    final private boolean srcUpstream;

    final private LatencyNode src;
    final private int srcChannel;
    final private LatencyNode dst;
    final private int dstChannel;

    final int numSteadySrcExec;
    final int numSteadyDstExec;

    final int numInitSrcExec;
    final int numInitDstExec;

    /**
     * dst2srcDependency is an array src phase execution #s. In order 
     * to execute the dst phase x, src must have been executed at least 
     * dst2srcDependency[x] times.
     * 
     * This array has a size of (numInitDstExec + numSteadyDstExec + 1).
     * In order to find out dependency beyond this size, we need to
     * wrap around from element (numInitDstExec + numSteadyDstExec + 1) to
     * element (numInitDstExec + 1). (here I assume that everything starts
     * at 1, which it does) 
     */
    final int[] dst2srcDependency;

    /**
     * Constructs an edge between two consecutive nodes. upstream is the source 
     * of data, while downstream is the destination. This constructor is used 
     * for DATA (not latency) dependancies between two nodes.
     */
    LatencyEdge(
        LatencyNode upstream,
        int outputChannel,
        LatencyNode downstream,
        int inputChannel,
        int initDataInChannel)
    {
        // do some trivial initializations
        {
            src = upstream;
            srcChannel = outputChannel;
            dst = downstream;
            dstChannel = inputChannel;

            srcUpstream = true;
        }

        // now figure out how many times each of the two nodes
        // need to run to execute a full steady state execution
        // between them
        {
            BigInteger srcData =
                BigInteger.valueOf(src.getSteadyStatePush(srcChannel));
            BigInteger dstData =
                BigInteger.valueOf(dst.getSteadyStatePop(dstChannel));

            BigInteger gcd = srcData.gcd(dstData);
            BigInteger steadyStateData =
                srcData.multiply(dstData).divide(gcd);

            numSteadySrcExec =
                dstData.divide(gcd).intValue() * src.getSteadyNumPhases();
            numSteadyDstExec =
                srcData.divide(gcd).intValue() * dst.getSteadyNumPhases();

            // don't initialize (allocate) this array, 'cause I don't yet
            // know its size! I'll compute the number of phases requried for
            // initialization below, allocate the array, and go through the 
            // whole dependency (init and steady) at once.
        }

        // compute the dependency of downstream on upstream
        // this takes initialization into account, thus there
        // are two stages in the depency: the initialization stage
        // and the steady stage.
        {
            // compute the initialization stage of data dependency.
            // do this twice. the first time i'll just record how many
            // compute the initialization stage of data dependency
            // this is done in two steps (usually only one will be
            // required, but I never know which one ahead of time).
            // First I'll execute the entire initialization schedule
            // of the downstream operator. This will force some of the
            // execution of the upstream operator. Then I'll execute
            // any remaining phases of the intitialization of the
            // upstream operator, while computing which phase of the
            // downstream operator depends on the upstream operator.
            //
            // Do not record the dependency, just figure out how many phases
            // are required from the dst node to do it :) The dependncy will
            // be recorded later (below) together with the steady state
            // dependency in one fell swoop 
            {
                int dstStage = 0, srcStage = 0;
                int nDataInChannel = initDataInChannel;
                {
                    for (dstStage = 0;
                        dstStage < dst.getInitNumPhases();
                        dstStage++)
                    {
                        int dataNeeded =
                            dst.getPhasePeek(dstStage, inputChannel);

                        while (dataNeeded >= nDataInChannel)
                        {
                            nDataInChannel
                                += src.getPhasePush(srcStage, outputChannel);
                            srcStage++;
                        }

                        nDataInChannel
                            -= dst.getPhasePop(dstStage, inputChannel);
                    }
                }

                // the dst has been initialized, now finish initializing
                // the src
                {
                    for (; srcStage < src.getInitNumPhases(); srcStage++)
                    {
                        nDataInChannel
                            += src.getPhasePush(srcStage, outputChannel);

                        while (nDataInChannel
                            >= dst.getPhasePeek(dstStage, inputChannel))
                        {
                            nDataInChannel
                                -= dst.getPhasePop(dstStage, inputChannel);
                            dstStage++;
                        }
                    }
                }

                numInitSrcExec = srcStage;
                numInitDstExec = dstStage;
            }

            // allocate storage to keep track of the dependency
            dst2srcDependency =
                new int[numInitDstExec + numSteadyDstExec + 1];

            // compute the dependency for numInitDstExec + numSteadyDstExec
            // phases of the dst node.
            // this will do the initialization. if the src doesn't 
            // execute enough times that's OK, 'cause its last phases would 
            // not push any data
            {
                int dstStage = 0, srcStage = 0;
                int nDataInChannel = initDataInChannel;
                for (dstStage = 0;
                    dstStage < numInitDstExec + numSteadyDstExec;
                    dstStage++)
                {
                    int dataNeeded =
                        dst.getPhasePeek(dstStage, inputChannel);

                    while (dataNeeded > nDataInChannel)
                    {
                        nDataInChannel
                            += src.getPhasePush(srcStage, outputChannel);
                        srcStage++;
                    }

                    dst2srcDependency[dstStage+1] = srcStage;

                    nDataInChannel
                        -= dst.getPhasePop(dstStage, inputChannel);
                }
            }
        }
    }

    /**
     * Construct an edge that points from the node to itself.
     * 
     * This kind of an edge is used as an identity edge. I can combine this
     * edge with another edge that points from node -> node' or node' -> node
     * (combine them vertically) and get the other edge in return. 
     */
    public LatencyEdge(LatencyNode node)
    {
        src = node;
        srcChannel = -1;

        dst = node;
        dstChannel = -1;

        srcUpstream = true;

        numInitSrcExec = node.getInitNumPhases();
        numInitDstExec = node.getInitNumPhases();

        numSteadySrcExec = node.getSteadyNumPhases();
        numSteadyDstExec = node.getSteadyNumPhases();

        dst2srcDependency = new int[numInitDstExec + numSteadyDstExec + 1];

        // create the appropriate latency maps
        {
            int n;
            for (n = 0; n < numInitDstExec + numSteadyDstExec + 1; n++)
            {
                dst2srcDependency[n] = n;
            }
        }
    }

    /**
     * Construct an edge from two other edges.
     * The other two edges can either be in parallel (connect two same nodes, 
     * X->Y) or can be in series (connect three nodes X->Y->Z).
     * If we're dealing with the latter case, edge1 must connect X->Y, while
     * edge2 must connect Y->Z.
     */
    public LatencyEdge(LatencyEdge edge1, LatencyEdge edge2)
    {
        dstChannel = -1;
        srcChannel = -1;

        srcUpstream = true;

        if (edge1.getSrc() == edge2.getSrc()
            && edge1.getDst() == edge2.getDst())
        {
            // the two edges are in parallel
            src = edge1.getSrc();
            dst = edge1.getDst();

            // find out number of init phases
            {
                numInitDstExec =
                    MAX(
                        edge1.getNumDstInitPhases(),
                        edge2.getNumDstInitPhases());
                numInitSrcExec =
                    MAX(
                        edge1.getSrcPhase4DstPhase(numInitDstExec),
                        edge2.getSrcPhase4DstPhase(numInitDstExec));
            }

            // find out number of steady phases
            {
                BigInteger srcPhases1 =
                    BigInteger.valueOf(edge1.getNumSrcSteadyPhases());
                BigInteger srcPhases2 =
                    BigInteger.valueOf(edge2.getNumSrcSteadyPhases());

                BigInteger dstPhases1 =
                    BigInteger.valueOf(edge1.getNumDstSteadyPhases());
                BigInteger dstPhases2 =
                    BigInteger.valueOf(edge2.getNumDstSteadyPhases());

                BigInteger src1Mult =
                    srcPhases2.divide(srcPhases1.gcd(srcPhases2));
                BigInteger dst1Mult =
                    dstPhases2.divide(dstPhases1.gcd(dstPhases2));

                BigInteger multiplier1 =
                    src1Mult.divide(src1Mult.gcd(dst1Mult)).multiply(
                        dst1Mult);

                numSteadySrcExec =
                    srcPhases1.multiply(multiplier1).intValue();
                numSteadyDstExec =
                    dstPhases1.multiply(multiplier1).intValue();
            }

            // allocate storage to keep track of the dependency
            dst2srcDependency =
                new int[numInitDstExec + numSteadyDstExec + 1];

            // compute the dependency for numInitDstExec + numSteadyDstExec
            // phases of the dst node.
            // this will do the initialization. if the src doesn't 
            // execute enough times that's OK, 'cause its last phases would 
            // not push any data
            {
                for (int nPhase = 0;
                    nPhase < numInitDstExec + numSteadyDstExec + 1;
                    nPhase++)
                {
                    dst2srcDependency[nPhase] =
                        MAX(
                            edge1.getSrcPhase4DstPhase(nPhase),
                            edge2.getSrcPhase4DstPhase(nPhase));
                }
            }
        }
        else
        {
            // the two edges are in series
            // make sure they are, just in case though
            ASSERT(edge1.getDst() == edge2.getSrc());

            src = edge1.getSrc();
            dst = edge2.getDst();

            // find out number of init phases
            {
                numInitSrcExec =
                    MAX(
                        edge1.getNumSrcInitPhases(),
                        edge1.getSrcPhase4DstPhase(
                            edge2.getNumSrcInitPhases()));
                numInitDstExec =
                    MAX(
                        edge2.getNumDstInitPhases(),
                        edge2.getDstPhase4SrcPhase(
                            edge1.getNumDstInitPhases()));
            }

            // find out the number of steady phases
            {
                BigInteger dstPhases1 =
                    BigInteger.valueOf(edge1.getNumDstSteadyPhases());
                BigInteger srcPhases2 =
                    BigInteger.valueOf(edge2.getNumSrcSteadyPhases());

                int gcd = dstPhases1.gcd(srcPhases2).intValue();
                int phases1Mult = edge2.getNumSrcSteadyPhases() / gcd;
                int phases2Mult = edge1.getNumDstSteadyPhases() / gcd;

                numSteadySrcExec =
                    edge1.getNumSrcSteadyPhases() * phases1Mult;
                numSteadyDstExec =
                    edge2.getNumDstSteadyPhases() * phases2Mult;

            }

            // allocate storage to keep track of the dependency
            dst2srcDependency =
                new int[numInitDstExec + numSteadyDstExec + 1];

            // compute the dependency for numInitDstExec + numSteadyDstExec
            // phases of the dst node.
            // this will do the initialization. if the src doesn't 
            // execute enough times that's OK, 'cause its last phases would 
            // not push any data
            {
                for (int nPhase = 0;
                    nPhase < numInitDstExec + numSteadyDstExec + 1;
                    nPhase++)
                {
                    dst2srcDependency[nPhase] =
                        edge1.getSrcPhase4DstPhase(
                            edge2.getSrcPhase4DstPhase(nPhase));
                }
            }
        }
    }

    public LatencyNode getSrc()
    {
        return src;
    }

    public LatencyNode getDst()
    {
        return dst;
    }

    public int getNumDstInitPhases()
    {
        return numInitDstExec;
    }

    public int getNumSrcInitPhases()
    {
        return numInitSrcExec;
    }

    public int getNumDstSteadyPhases()
    {
        return numSteadyDstExec;
    }

    public int getNumSrcSteadyPhases()
    {
        return numSteadySrcExec;
    }

    public int getSrcPhase4DstPhase(int nDstPhase)
    {
        if (nDstPhase < numInitDstExec + 1)
        {
            return dst2srcDependency[nDstPhase];
        }
        else
        {
            int nSteadyStates =
                (nDstPhase - (numInitDstExec + 1)) / numSteadyDstExec;
            int nSmallerDstPhase =
                ((nDstPhase - (numInitDstExec + 1)) % numSteadyDstExec)
                    + numInitDstExec + 1;
            return dst2srcDependency[nSmallerDstPhase]
                + nSteadyStates * numSteadySrcExec;
        }
    }

    public int getDstPhase4SrcPhase(int nSrcPhase)
    {
        // first have to figure out if I need to "wrap around"
        int addDstPhase = 0;
        if (nSrcPhase >= numInitSrcExec + numSteadySrcExec + 1)
        {
            int fullExecs = (nSrcPhase - numInitSrcExec - 1) / numSteadySrcExec;
            addDstPhase = fullExecs * numSteadyDstExec;
            nSrcPhase =
                nSrcPhase - fullExecs * numSteadySrcExec;
        }

        int dstPhaseLow = 0,
            dstPhaseHigh = numInitDstExec + numSteadyDstExec;
        while (dstPhaseHigh - dstPhaseLow > 1)
        {
            int dstPhaseMid = (dstPhaseLow + dstPhaseHigh) / 2;
            if (dst2srcDependency[dstPhaseMid] > nSrcPhase)
            {
                dstPhaseHigh = dstPhaseMid;
            }
            else
            {
                dstPhaseLow = dstPhaseMid;
            }
        }

        int dstPhase;
        if (dst2srcDependency[dstPhaseHigh] <= nSrcPhase)
        {
            dstPhase = dstPhaseHigh;
        }
        else
        {
            dstPhase = dstPhaseLow;
        }

        return dstPhase + addDstPhase;
    }
}