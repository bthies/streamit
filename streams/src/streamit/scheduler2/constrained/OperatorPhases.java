package streamit.scheduler2.constrained;

import streamit.misc.Misc;
import streamit.scheduler2.hierarchical.PhasingSchedule;

public class OperatorPhases extends Misc
{
    final int nPhases;
    final int nInputChannels;
    final int nOutputChannels;

    // these are double arrays of format [channel][phase]
    final int channelPeek[][];
    final int channelPop[][];
    final int channelPush[][];

    // these store the overall peek/push/pop values for every incoming
    // and outgoing channel for this operator
    final int overallChannelPeek[];
    final int overallChannelPop[];
    final int overallChannelPush[];

    // store whether the overall peek/pop/push amounts have changed since
    // last time they were computed
    boolean overallValuesValid;

    final PhasingSchedule phases[];

    public OperatorPhases(
        int _nPhases,
        int _nInputChannels,
        int _nOutputChannels)
    {
        nPhases = _nPhases;
        nInputChannels = _nInputChannels;
        nOutputChannels = _nOutputChannels;

        // allocate space for all the phase information
        // wish I could initialize these to -1!
        channelPeek = new int[nInputChannels][nPhases];
        channelPop = new int[nInputChannels][nPhases];
        channelPush = new int[nOutputChannels][nPhases];
        phases = new PhasingSchedule[nPhases];

        // allocate space for all overall data, and reset their counter
        overallChannelPeek = new int[nInputChannels];
        overallChannelPop = new int[nInputChannels];
        overallChannelPush = new int[nOutputChannels];
        overallValuesValid = true;
    }

    public void setPhaseInput(int peek, int pop, int phase, int channel)
    {
        ASSERT(channel >= 0 && channel < nInputChannels);
        ASSERT(phase >= 0 && phase < nPhases);
        ASSERT(pop >= 0 && peek >= pop);

        channelPeek[channel][phase] = peek;
        channelPop[channel][phase] = pop;
        overallValuesValid = false;
    }

    public void setPhaseOutput(int push, int phase, int channel)
    {
        ASSERT(channel >= 0 && channel < nOutputChannels);
        ASSERT(phase >= 0 && phase < nPhases);
        ASSERT(push >= 0);

        channelPush[channel][phase] = push;
        overallValuesValid = false;
    }

    public void setOperatorPhase(PhasingSchedule phase, int nPhase)
    {
        ASSERT(nPhase >= 0 && nPhase < nPhases);
        ASSERT(phase != null);

        phases[nPhase] = phase;
        overallValuesValid = false;
    }

    void calculateOverallThroughput()
    {
        // first copy all the e/o/u values to the overall arrays
        {
            int channel;
            for (channel = 0; channel < nInputChannels; channel++)
            {
                overallChannelPeek[channel] = channelPeek[channel][0];
                overallChannelPop[channel] = channelPop[channel][0];
            }

            for (channel = 0; channel < nOutputChannels; channel++)
            {
                overallChannelPush[channel] = channelPush[channel][0];
            }
        }

        // and now go from phase 1 and on (skipping phase 0) and collect
        // the rest of the e/o/u information
        int nPhase;
        for (nPhase = 1; nPhase < nPhases; nPhase++)
        {
            int channel;
            for (channel = 0; channel < nInputChannels; channel++)
            {
                overallChannelPeek[channel] =
                    MAX(
                        channelPeek[channel][nPhase]
                            + overallChannelPop[channel],
                        overallChannelPeek[channel]);
                overallChannelPop[channel] += channelPop[channel][nPhase];
            }
            for (channel = 0; channel < nOutputChannels; channel++)
            {
                overallChannelPush[channel] += channelPush[channel][nPhase];
            }
        }

        overallValuesValid = true;
    }

    int getOverallPeek(int channel)
    {
        ASSERT(channel < nInputChannels);

        if (!overallValuesValid)
            calculateOverallThroughput();
        return overallChannelPeek[channel];
    }

    int getOverallPop(int channel)
    {
        ASSERT(channel < nInputChannels);

        if (!overallValuesValid)
            calculateOverallThroughput();
        return overallChannelPop[channel];
    }

    int getOverallPush(int channel)
    {
        ASSERT(channel < nOutputChannels);

        if (!overallValuesValid)
            calculateOverallThroughput();
        return overallChannelPush[channel];
    }

    int getNumPhases()
    {
        return nPhases;
    }

    int getPhasePeek(int nPhase, int channel)
    {
        ASSERT(nPhase >= 0 && nPhase < nPhases);
        ASSERT(channel >= 0 && channel < nInputChannels);

        return channelPeek[channel][nPhase];
    }

    int getPhasePop(int nPhase, int channel)
    {
        ASSERT(nPhase >= 0 && nPhase < nPhases);
        ASSERT(channel >= 0 && channel < nInputChannels);

        return channelPop[channel][nPhase];
    }

    int getPhasePush(int nPhase, int channel)
    {
        ASSERT(nPhase >= 0 && nPhase < nPhases);
        ASSERT(channel >= 0 && channel < nOutputChannels);

        return channelPush[channel][nPhase];
    }
}
