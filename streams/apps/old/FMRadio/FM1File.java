
import streamit.*;
import java.lang.Math;
import java.io.*;

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




/**
 * Class FloatFileWriter
 *
 * Outputs floats to a file
 */

class FloatFileWriter extends Filter {

    File outputFile;
    FileWriter out;
    float c;

    public FloatFileWriter (String output)
    {
        super ();
        try{
            outputFile = new File(output);
            out = new FileWriter(outputFile);
        }
        catch(FileNotFoundException e)
            {
                System.err.println("File not found: " + input + " exception: " + e);
            }
        catch(IOException e)
            {
                System.err.println("IO Exception: " + e);
            }
    }

    public void init()
    {
        input = new Channel (Float.TYPE, 1);
    }

    public void work() {
        try{
            //crude, but it'll do.
            c = input.popFloat();
            int d = Float.floatToIntBits(c);
            int out1 = (d&0xff000000)>>24;
            int out2 = (d&0x00ff0000)>>16;
            int out3 = (d&0x0000ff00)>>8;
            int out4 = (d&0x000000ff);
            out.write(out1);
            out.write(out2);
            out.write(out3);
            out.write(out4);
        }
        catch(IOException e)
            {
                System.err.println("IO Exception: " + e);
            }
    }
}












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




/**
 * Class FloatFileReader
 *
 * Inputs floats from a file and outputs them to it's output port.
 */

class FloatFileReader extends Filter {

    File inputFile;
    FileReader in;
    int c,d;


    public FloatFileReader (String input)
    {
        super ();
        try{
            inputFile = new File(input);
            in = new FileReader(inputFile);
        }
        catch(FileNotFoundException e)
            {
                System.err.println("File not found: " + input + " exception: " + e);
            }
    }

    public void init()
    {
        output = new Channel (Float.TYPE, 1);
    }

    public void work() {
	try{
	    //each read only does 1 byte.... take in four, and meld em
	    c = 0; //clear c
	    for(int i = 0; i<4; i++)
		if((d = in.read()) != -1)
		    {
			c += (d << ((3-i)<<3));
		    }
		else{
		    //System.err.println("End of file reached.");
		    System.exit(0);
		    //return;
		}
	    output.pushFloat(Float.intBitsToFloat(c));
	}
	catch(IOException e)
	    {
		System.err.println("IO Exception: " + e);
	    }
    }
}












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




/**
 * Class LowPassFilter
 *
 * Implements a Low Pass FIR Filter
 */

class LowPassFilter extends Filter {

    int numberOfTaps;
    float COEFF[];
    float cutoffFreq, samplingRate;
    int mDecimation;

    public LowPassFilter(float sampleRate, float cutFreq, int numTaps, int decimation)
    {
        super(sampleRate, cutFreq, numTaps, decimation);
    }

    public void init(float sampleRate, float cutFreq, int numTaps, int decimation)
    {
	mDecimation = decimation;
        input = new Channel (Float.TYPE, 1+mDecimation);
        output = new Channel (Float.TYPE, 1);

        //all frequencies are in hz
        samplingRate = sampleRate;
        cutoffFreq = cutFreq;
        numberOfTaps = numTaps;
        COEFF = new float[numTaps];

        float pi = (float)java.lang.Math.PI;
        //build the taps, and call super.init(taps[])
        float temptaps[] = new float[numberOfTaps];

        float m = numberOfTaps -1;
        //from Oppenheim and Schafer, m is the order of filter

        if(cutoffFreq == 0.0)
            {
                //Using a Hamming window for filter taps:
                float tapTotal = 0;

                for(int i=0;i<numberOfTaps;i++)
                    {
                        temptaps[i] = (float)(0.54 - 0.46*java.lang.Math.cos((2*pi)*(i/m)));
                        tapTotal += temptaps[i];
                    }

                //normalize all the taps to a sum of 1
                for(int i=0;i<numberOfTaps;i++)
                    {
                        temptaps[i] = temptaps[i]/tapTotal;
                    }
            }
        else{
            //ideal lowpass filter ==> Hamming window
            //has IR h[n] = sin(omega*n)/(n*pi)
            //reference: Oppenheim and Schafer

            float w = (2*pi) * cutoffFreq/samplingRate;

            for(int i=0;i<numberOfTaps;i++)
                {
                    //check for div by zero
                    if(i-m/2 == 0)
                        temptaps[i] = w/pi;
                    else
                        temptaps[i] = (float)(java.lang.Math.sin(w*(i-m/2)) / pi
                                       / (i-m/2) * (0.54 - 0.46
                                                    * java.lang.Math.cos((2*pi)*(i/m))));
                }
        }
        COEFF = temptaps;
        numberOfTaps = COEFF.length;
    }

    public void work() {
        float sum = 0;
        for (int i=0; i<numberOfTaps; i++) {
	    sum += input.peekFloat(i)*COEFF[i];
        }

        input.popFloat();
        for(int i=0;i<mDecimation;i++)
            input.popFloat();
        output.pushFloat(sum);
    }
}













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




/**
 * A local subtractor class.  It subtracts two floats.  It has input = 2 and
 * output = 1.
 */
class FloatSubtract extends Filter
{
    public void init ()
    {
        input = new Channel (Float.TYPE, 2);
        output = new Channel (Float.TYPE, 1);
    }

    public void work() {
        //subtract one from the other, round robin.
        output.pushFloat((float)(input.peekFloat(0)-input.peekFloat(1)));
        input.popFloat();
        input.popFloat();
    }
}

/**
 * Class BandPassFilter
 *
 * Implements a Low Pass FIR Filter
 */

class BandPassFilter extends Pipeline {

    int numberOfTaps;
    float samplingRate;
    int mDecimation;
    float mGain;
    float mLowFreq;
    float mHighFreq;

    public BandPassFilter(float sampleRate, float lowFreq, float highFreq, int numTaps, float gain)
    {
        super(sampleRate, lowFreq, highFreq, numTaps, gain);
    }


    public void init(float sampleRate, float lowFreq, float highFreq, int numTaps, float gain)
    {
        //all frequencies are in hz
        samplingRate = sampleRate;
        mHighFreq = highFreq;
        mLowFreq = lowFreq;
        mGain = gain;
        numberOfTaps = numTaps;

        add(new SplitJoin() {
                public void init () {
                    setSplitter(DUPLICATE());
                    add(new LowPassFilter(samplingRate, mHighFreq, numberOfTaps, 0));
                    add(new LowPassFilter(samplingRate, mLowFreq, numberOfTaps, 0));
                    setJoiner(ROUND_ROBIN());
                }
            });
        add (new FloatSubtract ());
    }

}













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



/**
 * Class FMDemodulator
 *
 * Implements an FM Demodulator 
 *
 */

class FMDemodulator extends Filter {
    
  float mGain;
  float sampleRate;
  float maxAmplitude;
  float modulationBandwidth;

    public FMDemodulator (float sampRate, float max, float bandwidth)
    {
	super ();
	sampleRate = sampRate;
	maxAmplitude = max;
	modulationBandwidth = bandwidth;
    }

    public void init()
    {
	input = new Channel (Float.TYPE, 1);
	output = new Channel (Float.TYPE, 1);

	mGain = maxAmplitude*(sampleRate
			    /(modulationBandwidth*(float)Math.PI));
    }

    public void work() {
	float temp = 0;
	//may have to switch to complex?
	temp = (float)((input.peekFloat(0)) * (input.peekFloat(1)));
	//if using complex, use atan2
	temp = (float)(mGain * Math.atan(temp));

	input.popFloat();
	output.pushFloat(temp);
    }
}
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




/**
 * A local adder class.  It adds four floats.  It has input = 4 and
 * output = 1.
 */
class FloatAdder extends Filter
{
    public void init ()
    {
        input = new Channel (Float.TYPE, 4);
        output = new Channel (Float.TYPE, 1);
    }

    public void work() {
        //subtract one from the other, round robin.
        output.pushFloat((float)(input.peekFloat(0)+input.peekFloat(1)+input.peekFloat(2)+input.peekFloat(3)));
        input.popFloat();
        input.popFloat();
    }
}

/**
 * Class Equalizer
 *
 * Implements an Equalizer for an FM Radio
 */

class Equalizer extends Pipeline {

    float samplingRate;
    float mGain1;
    float mGain2;
    float mGain3;
    float mGain4;

    public Equalizer(float rate)
    {
        super(rate);
    }

    public void init(float rate)
    {
        //input = new Channel (Float.TYPE, 1);
        //output = new Channel (Float.TYPE, 1);

	samplingRate = rate;
	mGain1 = 1;
	mGain2 = 1;
	mGain3 = 1;
	mGain4 = 1;
	
	add(new SplitJoin(){
		public void init(){
		    setSplitter(DUPLICATE());
		    add(new BandPassFilter(samplingRate, 1250, 2500, 50, mGain1));
		    add(new BandPassFilter(samplingRate, 2500, 5000, 50, mGain2));
		    add(new BandPassFilter(samplingRate, 5000, 10000, 50, mGain3));
		    add(new BandPassFilter(samplingRate, 10000, 20000, 50, mGain4));
		    setJoiner(ROUND_ROBIN());
		}
	    });
	add(new FloatAdder());
    }
}
    











/*
 * Early attempt at an FM Radio... probably junk
 */



class FMRadio extends Pipeline
{
    public FMRadio()
    {
	super();
    }

    public void init()
    {
        //input = new Channel (Float.TYPE, 1);
        //output = new Channel (Float.TYPE, 1);

	float samplingRate = 200000; //200khz sampling rate according to jeff at vanu
	float cutoffFrequency = 108000000; //guess... doesn't FM freq max at 108 Mhz? 
	int numberOfTaps = 100;
	float maxAmplitude = 27000;
	float bandwidth = 10000;
	//decimate 4 samples after outputting 1
        add(new LowPassFilter(samplingRate, cutoffFrequency, numberOfTaps, 4));
	add(new FMDemodulator(samplingRate, maxAmplitude, bandwidth));
	add(new Equalizer(samplingRate));
    }
}

        















/*
 * Test for testing LowPassFilter with float files
 */






public class FM1File extends StreamIt
{
    static public void main(String[] t)
    {
        FM1File test = new FM1File();
        test.run();
    }
    
    public void init()
    {
	add(new FloatFileReader("FmRadioIn200Float"));
	add(new FMRadio());
	add(new FloatFileWriter("radioOutput"));
    }
}

        















