/*
 *  Copyright 2001 Massachusetts Institute of Technology
 *
 *  Permission to use, copy, modify, distribute, and sell this software and its
 *  documentation for any purpose is hereby granted without fee, provided that
 *  the above copyright notice appear in all copies and that both that
 *  copyright notice and this permission notice appear in supporting
 *  documentation, and that the name of M.I.T. not be used in advertising or
 *  publicity pertaining to distribution of the software without specific,
 *  written prior permission.  M.I.T. makes no representations about the
 *  suitability of this software for any purpose.  It is provided "as is"
 *  without express or implied warranty.
 */

import java.lang.Math.*;
import streamit.library.*;

/*
 * Software equalizer.  This version uses n+1 low-pass filters directly,
 * as opposed to n band-pass filters, each with two low-pass filters.
 * The important observation is that we have bands 1-2, 2-4, 4-8, ...
 * This means that we should run an LPF for each intermediate frequency,
 * rather than two per band.  Calculating this in StreamIt isn't that bad.
 * For a four-band equalizer:
 *
 *              |
 *             DUP
 *    +---------+---------+
 *    |         |         |
 *    |        DUP        |
 *    |    +----+----+    |
 *    |    |    |    |    |
 *   16    8    4    2    1
 *    |    |    |    |    |
 *    |  (dup)(dup)(dup)  |
 *    |    |    |    |    |
 *    |    +----+----+    |
 *    |       RR(2)       |
 *    |         |         |
 *    +---------+---------+
 *       WRR(1,2(n-1),1)
 *              |
 *            (a-b)
 *              |
 *            SUM(n)
 *              |
 *
 * It's straightforward to change the values of 1, 16, and n.  Coming out
 * of the EqualizerSplitJoin is 16 8 8 4 4 2 2 1; we can subtract and scale
 * these as appropriate to equalize.
 */

class FloatNAdder extends Filter
{
    int N;

    public FloatNAdder(int count)
    {
        super(count);
    }
    public void init (final int count)
    {
        N = count;
        input = new Channel (Float.TYPE, count, count);
        output = new Channel (Float.TYPE, 1);
    }

    public void work() {
        float sum = 0.0f;
        int i;
        for (i = 0; i < N; i++)
            sum += input.popFloat();
        output.pushFloat(sum);
    }
}

class FloatDiff extends Filter
{
    public void init()
    {
        input = new Channel(Float.TYPE, 2, 2);
        output = new Channel(Float.TYPE, 1);
    }
    public void work() 
    {
        output.pushFloat(input.peekFloat(0) - input.peekFloat(1));
	input.popFloat();
	input.popFloat();
    }
}

class FloatDup extends Filter
{
    public void init()
    {
        input = new Channel(Float.TYPE, 1, 1);
        output = new Channel(Float.TYPE, 2);
    }
    public void work()
    {
        float val = input.popFloat();
        output.pushFloat(val);
        output.pushFloat(val);
    }
}

class EqualizerInnerPipeline extends Pipeline 
{
    public EqualizerInnerPipeline(float rate, float freq)
    {
        super(rate, freq);
    }
    public void init(final float rate, final float freq)
    {
        add(new LowPassFilter(rate, freq, 64, 0));
        add(new FloatDup());
    }
}

class EqualizerInnerSplitJoin extends SplitJoin
{
    public EqualizerInnerSplitJoin(float rate, float low, float high, int bands)
    {
        super(rate, low, high, bands);
    }
    public void init(final float rate, final float low, final float high,
                     final int bands)
    {
        int i;
        setSplitter(DUPLICATE());
        for (i = 0; i < bands - 1; i++)
            add(new EqualizerInnerPipeline
                (rate,
                 (float)java.lang.Math.exp
                 ((i+1) *
                  (java.lang.Math.log(high) - java.lang.Math.log(low)) /
                  bands + java.lang.Math.log(low))));
        setJoiner(ROUND_ROBIN(2));
    }
}

class EqualizerSplitJoin extends SplitJoin {

    public EqualizerSplitJoin(float rate, float low, float high, int bands)
    {
        super(rate, low, high, bands);
    }

    public void init(final float rate, final float low, final float high,
                     final int bands)
    {
        // To think about: gains.
	
	setSplitter(DUPLICATE());
        add(new LowPassFilter(rate, high, 64, 0));
        add(new EqualizerInnerSplitJoin(rate, low, high, bands));
        add(new LowPassFilter(rate, low, 64, 0));
	setJoiner(WEIGHTED_ROUND_ROBIN(1, (bands-1)*2, 1));
    }
}

/**
 * Class Equalizer
 *
 * Implements an Equalizer for an FM Radio
 */

public class Equalizer extends Pipeline {

    public Equalizer(float rate)
    {
        super(rate);
    }

    public void init(final float rate)
    {
        final int bands = 10;
        final float low = 55;
        final float high = 1760;
	add(new EqualizerSplitJoin(rate, low, high, bands));
        add(new FloatDiff());
	add(new FloatNAdder(bands));
    }
}
    











