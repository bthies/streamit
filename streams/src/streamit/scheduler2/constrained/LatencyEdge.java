package streamit.scheduler2.constrained;

import streamit.misc.AssertedClass;
import streamit.misc.OMap;
import streamit.scheduler2.hierarchical.StreamInterface;
import java.math.BigInteger;

public class LatencyEdge extends AssertedClass
{
    final private LatencyNode src;
    final private int srcChannel;
    final int numSteadySrcExec;
    final private LatencyNode dst;
    final private int dstChannel;
    final int numSteadyDstExec;
    final private boolean srcUpstream;
    final private StreamInterface lowestCommonAncestor;

    /**
     * initDependencies is a map of dst phase execution # to 
     * src phase execution #. In order to execute the dst phase,
     * src must have been executed at least the corresponding phase #
     */
    final OMap initDependencies = new OMap();

    /**
     * steadyDependencies is a map of dst phase execution # to 
     * src phase execution #. In order to execute the dst phase,
     * src must have been executed at least the corresponding phase #
     * The data stored here starts from where initDependencies leaves off.
     * The data is valid for every full execution of a steady state between
     * the two nodes (adjusted for the number of steady state executions).
     */
    final OMap steadyDependencies = new OMap();

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
        StreamInterface _lowestCommonAncestor)
    {
        // do some trivial initializations
        {
            lowestCommonAncestor = _lowestCommonAncestor;

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

            numSteadySrcExec = dstData.divide(gcd).intValue();
            numSteadyDstExec = srcData.divide(gcd).intValue();
        }

        // compute the dependency of downstream on upstream
        // this takes initialization into account, thus there
        // are two stages in the depency: the initialization stage
        // and the steady stage.
        {
            // compute the initialization stage of data dependency
            // this is done in two steps (usually only one will be
            // required, but I never know which one ahead of time).
            // First I'll execute the entire initialization schedule
            // of the downstream operator. This will force some of the
            // execution of the upstream operator. Then I'll execute
            // any remaining phases of the intitialization of the
            // upstream operator, while computing which phase of the
            // downstream operator depends on the upstream operator.
            int dstStage = 0, srcStage = 0;
            int nDataInChannel = 0;
            {
                for (dstStage = 0;
                    dstStage < dst.getInitNumPhases();
                    dstStage++)
                {
                    int dataNeeded = dst.getPhasePeek(dstStage, inputChannel);

                    while (dataNeeded >= nDataInChannel)
                    {
                        nDataInChannel += src.getPhasePush(srcStage, outputChannel);
                        srcStage++;
                    }

                    initDependencies.insert(
                        new Integer(dstStage),
                        new Integer(srcStage - 1));

                    nDataInChannel -= dst.getPhasePop(dstStage, inputChannel);
                }
            }

            // the dst has been initialized, now finish initializing
            // the src
            {
                for (; srcStage < src.getInitNumPhases(); srcStage++)
                {
                    nDataInChannel += src.getPhasePush(srcStage, outputChannel);

                    while (nDataInChannel >= dst.getPhasePeek(dstStage, inputChannel))
                    {
                        initDependencies.insert(
                            new Integer(dstStage),
                            new Integer(srcStage - 1));
                        nDataInChannel -= dst.getPhasePop(dstStage, inputChannel);
                        dstStage++;
                    }
                }
            }

            // compute the steady state
            // this will start off where the initialization left off
            // and only execute as many times as necessary to pull
            // data into the dst. if the src doesn't execute enough times
            // that's OK, 'cause its last phases would not push any data
            {
                for (;
                    dstStage < dst.getInitNumPhases() + numSteadyDstExec;
                    dstStage++)
                {
                    int dataNeeded = dst.getPhasePeek(dstStage, inputChannel);

                    while (dataNeeded >= nDataInChannel)
                    {
                        nDataInChannel += src.getPhasePush(srcStage, outputChannel);
                        srcStage++;
                    }

                    steadyDependencies.insert(
                        new Integer(dstStage),
                        new Integer(srcStage - 1));

                    nDataInChannel -= dst.getPhasePop(dstStage, inputChannel);
                }
            }
        }
    }
}