/**
 *  StGsmDecoder:  
 *  Decoder portion of GSM Vocoder, java/streamit implementation
 *  Uses GSM Spec 06.10
 *  REVISED ON 3/15:  For bottleneck reasons, splits
 *  Short Term Synthesis into to portions:
 *  the reflection coefficient portion, and the short term
 *  synthesis filtering section
 *  - J. Wong
 */


import streamit.library.*;
import java.lang.*;

import java.lang.reflect.*;
import streamit.library.io.*;
        
class RPEDecodeFilter extends Filter 
{
    int[] mXmc;
    int[] FAC; 
    int[] xmp;
    int[] ep;

    #include "Helper.java"
    #define EP_LENGTH 40

    public void init() 
    {
	input = new Channel(Integer.TYPE, 15);
	output = new Channel(Integer.TYPE, 40);
	mXmc = new int[13];   //mSequence
	//others are in work() method	
	xmp = new int[13]; //intermediary
	ep = new int[EP_LENGTH];  //output

	////////////////////////////////////////////
	// assign the members of the FAC array
	FAC = new int[8];
	FAC[0] = 29218;
	FAC[1] = 26215;
	FAC[2] = 23832;
	FAC[3] = 21846;
	FAC[4] = 20165;
	FAC[5] = 18725;
	FAC[6] = 17476;
	FAC[7] = 16384;
	////////////////////////////////////////////
    }

    public void work() 
    {
	int i, k, xmaxc, mc, exp, mant, temp, temp1, temp2, temp3;
	/**
	 *  Inputs to RPEDecodeFilter: Xmaxc - Maximum value of the 13 bits, 
	 *                     Xmc - 13 bit sample
	 *                     Mc - 2 bit grid positioning
	 */
	
	for (i = 0; i < 13; i++)
	    {
		mXmc[i] = input.popInt();
	    }
	xmaxc = input.popInt();    //mRpeMagnitude
	mc = input.popInt();       //mRpeGridPosition

	 
	//Get the exponent, mantissa of Xmaxc:
       
	exp = 0;
	if (xmaxc > 15)
	    {
		exp = gsm_sub(intify(xmaxc >> 3),  1);
	    }
	mant = gsm_sub(xmaxc, intify(exp << 3));
	//System.err.println("mantissa is " + mant);
	//normalize mantissa 0 <= mant <= 7;
	
	if (mant == 0)
	    {
		exp = -4;
		mant = 7;
	    }
	else
	    {
		while (mant <= 7) 
		    {
			mant = intify(mant << 1 | 1);
			exp--;
		    }
		mant = gsm_sub(mant,  8);
	    }
	//System.err.println("normalized mantissa is " + mant);
	/* 
	 *  This part is for decoding the RPE sequence of coded Xmc[0..12]
	 *  samples to obtain the xMp[0..12] array.  Table 4.6 is used to get
	 *  the mantissa of xmaxc (FAC[0..7]).
	 */
	
	//System.err.println("Size of FAC is " + FAC.length);
	//System.err.println("Gets called at mant = " + mant);
	temp1 = FAC[mant];
	System.err.println("Temp1 is " + temp1 + " with exp " + exp);
	temp2 = gsm_sub( 6, exp);
	System.err.println("Temp2 is " + temp2);

	int blahblah = 1 >> 1;
	System.err.println("blahblah is " + blahblah);

	temp3 = intify(1 << gsm_sub(temp2,  1));
	System.err.println("Temp3 is " + temp3);
	for (i = 0; i < 13; i++)
	    {
		temp = gsm_sub(intify(mXmc[i] << 1),  7);    //3 bit unsigned
		temp <<= 12;                 //16 bit signed
		temp = gsm_mult_r(temp1, temp);
		temp = gsm_add(temp, temp3);
		xmp[i] = intify(temp >> temp2);
		//System.err.println("xmp[i] is " + xmp[i]);
	    }

	/**
	 *  This procedure computes the reconstructed long term residual signal
	 *  ep[0..39] for the LTP analysis filter.  The inputs are the Mc
	 *  which is the grid position selection and the xMp[0..12] decoded
	 *  RPE samples which are upsampled by a factor of 3 by inserting zero
	 *  values.
	 */
	//output!

	for(k = 0; k < 40; k++)
	    {
		ep[k] = 0;
	    }
	for(i = 0; i < 12; i++)
	    {
		//System.out.println("accessing " + (mc+(3*i)));
		ep[mc + (3 * i)] = xmp[i];
	    } 
	//output the sucker!
	for (i = 0; i < EP_LENGTH; i++)
	    {
		output.pushInt(ep[i]);
	    }
	//System.err.println("Got to RPEDecode!");
    }
}

class LTPFilter extends Filter 
{
    int[] QLB;
    int[] drp;
    int nrp;

#include "Helper.java"
   
    public void init() 
    {
	input = new Channel(Integer.TYPE, 162);
	output = new Channel(Integer.TYPE, 1);
	drp = new int[160];
	nrp = 40;   //initial condition

	////////////////////////////////////////////
	// assign the members of the QLB array
	QLB = new int[4];
	QLB[0] = 3277;
	QLB[1] = 11469;
	QLB[2] = 21299;
	QLB[3] = 32767;
	////////////////////////////////////////////
    }

    public void work()  //output:  drpp
    {
	int i, nr, brp, drpp;
	int mBcr = input.popInt();  //mLtpGain
	int mNcr = input.popInt();  //mLtpOffset
	//do it!
	for (i = 0; i < 160; i++)
	    {
		drp[i] = input.popInt(); //drp from AdditionUpdateFilter
	    }
	nr = mNcr;
	if ((mNcr < 40) || (mNcr > 120))
	    {
		nr = nrp;
	    }
	nrp = nr;

	//Decoding of the LTP gain mBcr
	brp = QLB[mBcr];
	drpp = 1;
	for (i = 121; i < 161; i++)
	    {
		drpp = gsm_mult_r(brp, drp[i - nr]);   //delay and multiply operations
	    } 
	
	output.pushInt(drpp);   
	//System.err.println("Got to LTPFilter!");
    }
}

class AdditionUpdateFilter extends Filter 
{       
    int[] ep;
    int[] drp;

#include "Helper.java"
#define DRP_LENGTH 160

    public void init() 
    {
	int i;
	input = new Channel(Integer.TYPE, 41);
	output = new Channel(Integer.TYPE, 160);
	ep = new int[40]; //input
	drp = new int[DRP_LENGTH];  //output
	for (i = 0; i < DRP_LENGTH; i++)
	    {
		drp[i] = 0;   //initial conditions
	    }
    }

    public void work() 
    {
	int i, j, k, drpp;
	//get inputs:
	for (i = 0; i < 40; i++)
	    {
		ep[i] = input.popInt();
	    }
	drpp = input.popInt();

	for (j = 121; j < 160; j++)  //add part!
	    {
		drp[j] = gsm_add(ep[j - 121], drpp);
	    }
	for (k = 0; k < 120; k++)    //update part!
	    {
		drp[k] = drp[k + 40];
	    }
	
	for (i = 0; i < DRP_LENGTH; i++)  //output!
	    {
		output.pushInt(drp[i]);
	    }
	//System.err.println("Got to Addition Update Filter!");
    }
}

class ReflectionCoeffLARppInternal extends Filter
{
#include "Helper.java"

    int INVA, MIC, B;

    public ReflectionCoeffLARppInternal(int INVA, int MIC, int B)
    {
        super(INVA, MIC, B);
    }
    
    public void init(final int INVA, final int MIC, final int B)
    {
        input = new Channel(Integer.TYPE, 1);
        output = new Channel(Integer.TYPE, 1);
        this.INVA = INVA;
        this.MIC = MIC;
        this.B = B;
    }

    public void work()
    {
        int LARc, LARpp, temp1, temp2;
        
        LARc = input.popInt();
        temp1 = intify((gsm_add(LARc, MIC)) << 10);
        temp2 = intify(B << 10);
        temp1 = gsm_sub(temp1, temp2);
        temp1 = gsm_mult_r(INVA, temp1);
        LARpp = gsm_add(temp1, temp1);
        output.pushInt(LARpp);
    }
}   

class ReflectionCoeffLARpp extends SplitJoin
{
    public void init()
    {
        setSplitter(ROUND_ROBIN());
        // NB: these numbers are all magic.
        add(new ReflectionCoeffLARppInternal(13107, -32, 0));
        add(new ReflectionCoeffLARppInternal(13107, -32, 0));
        add(new ReflectionCoeffLARppInternal(13107, -16, 2048));
        add(new ReflectionCoeffLARppInternal(13107, -16, -2560));
        add(new ReflectionCoeffLARppInternal(19223, -8, 94));
        add(new ReflectionCoeffLARppInternal(17476, -8, -1792));
        add(new ReflectionCoeffLARppInternal(31454, -4, -341));
        add(new ReflectionCoeffLARppInternal(29708, -4, -1144));
        setJoiner(ROUND_ROBIN());
    }
}

class ReflectionCoeffLARpInternal extends Filter
{
#include "Helper.java"

    int mprevLARpp;

    public void init()
    {
        input = new Channel(Integer.TYPE, 1);
        output = new Channel(Integer.TYPE, 1);
        mprevLARpp = 0;
    }
    
    public void work()
    {
        int i, j, k;
        int mLARp, mLARpp;
        
        mLARpp = input.popInt();
        // Jikes can't do a data-flow analysis.  Sigh.
        mLARp = 0;

        // The remainder of this could almost certainly be broken down
        // nicely into component filters with feedback loops.  Think
        // about this more later.  --dzm

	//Interpolation of mLARpp to get mLARp:
	for (k = 0; k < 13; k++)
        {
            mLARp = gsm_add(intify(mprevLARpp >> 2), intify(mLARpp >> 2));
            mLARp = gsm_add(mLARp,  intify(mprevLARpp >> 1));
        }

	for (k = 13; k < 27; k++)
            mLARp = gsm_add(intify(mprevLARpp >> 1), intify(mLARpp >> 1));

	for (k = 27; k < 39; k++)
        {
            mLARp = gsm_add(intify(mprevLARpp >> 2), intify(mLARpp >> 2));
            mLARp = gsm_add(mLARp, intify(mLARpp >> 1));
        }

        /* Visibly wrong; I think it's supposed to be mLARp = mLARp,
           which is a nop, so punt this loop entirely.  --dzm
	for (k = 40; k < 160; k++)
            mLARp = mLARpp;
        */

        mprevLARpp = mLARpp;
        output.pushInt(mLARp);
    }
}

class ReflectionCoeffLARp extends SplitJoin
{
    public void init()
    {
        setSplitter(ROUND_ROBIN());
        // Not just explicit parallelization; each of these is stateful.
        for (int i = 0; i < 8; i++)
            add(new ReflectionCoeffLARpInternal());
        setJoiner(ROUND_ROBIN());
    }
}

class ReflectionCoeffmrrp extends Filter
{
#include "Helper.java"

    public void init()
    {
        input = new Channel(Integer.TYPE, 1);
        output = new Channel(Integer.TYPE, 1);
    }

    public void work()
    {
        int mLARp, temp, mrrp;
        mLARp = input.popInt();
        temp = gsm_abs(mLARp);
        if (temp < 11059)
            temp = intify(temp << 1);
        else if (temp < 20070)
            temp = gsm_add(temp,  11059);
        else
            temp = gsm_add( (temp >> 2),  26112);
        mrrp = temp;
        if (mLARp < 0)
            mrrp = gsm_sub(0, mrrp);
        output.pushInt(mrrp);
    }
}

class ReflectionCoeffCalc extends Pipeline
{
    public void init()
    {
        add(new ReflectionCoeffLARpp());
        add(new ReflectionCoeffLARp());
        add(new ReflectionCoeffmrrp());
    }
}

class ReflectionCoeff extends SplitJoin
{
    public void init()
    {
        setSplitter(WEIGHTED_ROUND_ROBIN(120, 40, 8));
        // Decimate first 120:
        add(new Filter() {
                public void init() 
                {
                    this.input = new Channel(Integer.TYPE, 1);
		    this.output = new Channel(Integer.TYPE, 0);
                }
                public void work()
                {
                    this.input.popInt();
                }
            });
        // Copy next 40 (mdrp):
        add(new Identity(Integer.TYPE));
        // And generate mrrp from last 8.
        add(new ReflectionCoeffCalc());
        setJoiner(WEIGHTED_ROUND_ROBIN(0, 40, 8));
    }
}

class IntegerTermReorder extends Filter
{
    int mdrp[];
    int mrrp[];
    
    public void init()
    {
        input = new Channel(Integer.TYPE, 8 + 40);
        output = new Channel(Integer.TYPE, (8 + 1) * 40);
        mdrp = new int[40];
        mrrp = new int[8];
    }
    
    public void work()
    {
        int val;
        int i, j;
        
        // Read in mdrp and mrrp:
        for (j = 0; j < 40; j++)
            mdrp[j] = input.popInt();
        for (j = 0; j < 8; j++)
            mrrp[j] = input.popInt();

        // Now write out (mrrp, element of mdrp):
        for (i = 0; i < 40; i++)
        {
            for (j = 0; j < 8; j++)
                output.pushInt(mrrp[j]);
            output.pushInt(mdrp[i]);
        }
    }
}

class IntegerTermSynthCalc extends Filter
{
    int[] mrrp;
    int[] v;
 
#include "Helper.java"
#define V_LENGTH 9
#define MRRP_LENGTH 8
   
    public void init()
    {
        input = new Channel(Integer.TYPE, MRRP_LENGTH + 1);
        output = new Channel(Integer.TYPE, 1);
        mrrp = new int[MRRP_LENGTH];
	v = new int[V_LENGTH];
	for (int i = 0; i < V_LENGTH; i++)
	    v[i] = 0;
    }

    public void work()
    {
        int i;
        int sri;
        
        for (i = 0; i < MRRP_LENGTH; i++)
            mrrp[i] = input.popInt();
        sri = input.popInt();
        for (i = 1; i < 8; i++)
        {
            sri = gsm_sub(sri, gsm_mult(mrrp[8-i], v[8-i]));
            v[9-i] = gsm_add(v[8-i], gsm_mult_r(mrrp[8-i], sri));
        }
        v[0] = sri;
        output.pushInt(sri);
    }
}

class IntegerTermSynth extends Pipeline
{
    public void init()
    {
        add(new IntegerTermReorder());
        add(new IntegerTermSynthCalc());
    }
}

class LARInputFilter extends Filter
{
    //order of output: mLarParameters[0...8]
    int[] mdata;
    int[] single_frame;
    boolean donepushing;

#include "DecoderInput.java"
#define MDATA_LENGTH 260
#define SINGLE_FRAME_LENGTH 260

    public void init()
    {
	mdata = new int[MDATA_LENGTH];
	single_frame = new int[SINGLE_FRAME_LENGTH];
	input = new Channel(Integer.TYPE, 260);
	output = new Channel(Integer.TYPE, 8); 
	donepushing = false;
    }

    public void work()
    {
	int i, j, k;
	//int frame_index = 0;
	for (i = 0; i < MDATA_LENGTH; i++)
	    {
		mdata[i] = input.popInt();
	    }

	if (donepushing)
	    {
		//AssertedClass.SERROR("Done Pushing at LARInputFilter!");
	    }
	//for (j = 0; j < 584; j++)  //only pushing one in for now, should be 0 to 584
	//   {
		for (k = 0; k < SINGLE_FRAME_LENGTH; k++)
		    {
			single_frame[k] = mdata[k];
		    }
		getParameters(single_frame);
		//frame_index += 260;
	  
	
		//now, push the stuff on!
		for (i = 0; i < 8; i++)
		    {
			output.pushInt(mLarParameters[i]);
		    }
		donepushing = true;
		//    }
	//System.err.println("LARinputFilter gooo!");

    }
}
class PostProcessingFilter extends Filter 
{
    int msr;

#include "Helper.java"

    public void init() 
    {
	input = new Channel(Integer.TYPE, 1);
	output = new Channel(Integer.TYPE, 1);
	msr = 0; //initial condition
    }

    public void work() 
    {
	int a;
	int i, k, temp;
        
        temp = input.popInt();

	//De-emphasis filtering!
        temp = gsm_add(temp, gsm_mult_r(msr,  28180));
        msr = temp; // to next execution

	//upscaling of output signal:
        temp = gsm_add(temp, temp);
        
	//truncation of the output variable:
        temp = intify(temp / 8);
        temp = gsm_mult(temp, 8);
        
        output.pushInt(temp);
    }
}

class LTPInputFilter extends Filter
{
    //order of output: mLtpGain[4], mLtpOffset[4]
    int[] mdata;
    int[] single_frame;
    boolean donepushing;

#include "DecoderInput.java"
#define MDATA_LENGTH 260

    public void init()
    {
	mdata = new int[MDATA_LENGTH];
	single_frame = new int[SINGLE_FRAME_LENGTH];
	input = new Channel(Integer.TYPE, 260);
	output = new Channel(Integer.TYPE, 8);
	donepushing = false;
    }

    public void work()
    {
	int i, j, k;
	//int frame_index = 0;
	for (i = 0; i < MDATA_LENGTH; i++)
	    {
		mdata[i] = input.popInt();
	    }

	if (donepushing)
	    {
		//AssertedClass.SERROR("Done Pushing at LTPInputFilter!");
	    }
	//for (j = 0; j < 584; j++)  //only pushing one in for now, should be 0 to 584
	//  {
		for (k = 0; k < SINGLE_FRAME_LENGTH; k++)
		    {
			single_frame[k] = mdata[k];
		    }
		getParameters(single_frame);
		//frame_index += 260;
	  	  
	  
		//now, push the stuff on!
		for (i = 0; i < 4; i++)
		    {
			output.pushInt(mLtpGain[i]);
			output.pushInt(mLtpOffset[i]);
		    }
		//  }
	donepushing = true;
	//System.err.println("LTP Input filter gooooo!");
    }
}
class LTPPipeline extends Pipeline
{
    public void init()
    {
	this.add(new FileReader("IntBinaryDecoderInput1", Integer.TYPE));
	this.add(new LTPInputFilter());
	//this.add(new IntegerPrinter('g'));
    }
}
class LARPipeline extends Pipeline
{
    public void init()
    {
	this.add(new FileReader("IntBinaryDecoderInput1", Integer.TYPE));
	this.add(new LARInputFilter());
	//this.add(new IntegerPrinter());
    }
}

class LTPInputSplitJoin extends SplitJoin
{
    public void init()
    {
	this.setSplitter(WEIGHTED_ROUND_ROBIN (0, 1));
	this.add(new LTPPipeline());
	this.add(new Identity(Integer.TYPE));
	//this.add(new IntegerPrinter('d'));
	this.setJoiner(WEIGHTED_ROUND_ROBIN(2, 160)); //bcr, ncr, drp[0...159]
    }
}

class LTPLoopStream extends Pipeline
{
    public void init()
    {
	this.add(new LTPInputSplitJoin());
        //this.add(new IntegerPrinter('e'));
        this.add(new LTPFilter());
        //this.add(new IntegerPrinter('b')); 
    }
}
class DecoderFeedback extends FeedbackLoop
{
    public void init()
    {
	this.setDelay(1);
	this.setJoiner(WEIGHTED_ROUND_ROBIN (40, 1));  //sequence: ep[0....39], drpp
  	//this.setBody(new AdditionUpdateFilter());
	this.setBody(new StupidStream()); //debug 
	this.setSplitter(DUPLICATE ());   
	//note:  although drp[120...159] are all that are
	//       required for IntegerTermSynth, this is currently
	//       the simplest way to implement things, theinput will be filtered internally.
 	this.setLoop(new LTPLoopStream());
    }

    public int initPathInt(int index)
    {
	return 0;
    }
	
}


class StupidStream extends Pipeline
{
    public void init()
    {
	//this.add(new IntegerPrinter('a'));
	this.add(new AdditionUpdateFilter());
	//this.add(new IntegerPrinter('c'));
    }
}

class LARInputSplitJoin extends SplitJoin
{
    public void init()
    {
	this.setSplitter(WEIGHTED_ROUND_ROBIN (1, 0));  //we don't care about it going to in2
	this.add(new Identity(Integer.TYPE));
	this.add(new LARPipeline());	
	this.setJoiner(WEIGHTED_ROUND_ROBIN(160, 8));  //drp[0...160], LARc[0...7];
    }
}

class RPEInputFilter extends Filter
{
    //order of output: mSequence[0..13], mRpeMagnitude, mRpeGridPosition
    int[] mdata;
    int[] single_frame;
    boolean donepushing;

#include "DecoderInput.java"
    
    public void init()
    {
	mdata = new int[MDATA_LENGTH];
	single_frame = new int[SINGLE_FRAME_LENGTH];
	input = new Channel(Integer.TYPE, 260);
	output = new Channel(Integer.TYPE, 60); 
	donepushing = false;
    }

    public void work()
    {
	int i, j, k, a;
	//int frame_index = 0;
	//System.err.println("I get here!!!");
	for (i = 0; i < MDATA_LENGTH; i++)
	    {
		mdata[i] = input.popInt();
		
	    }

	if (donepushing)
	    {
		//AssertedClass.SERROR("Done Pushing at RPEInputFilter!");
	    }

	//for (j = 0; j < 584; j++)  //only pushing one in for now, should be 0 to 584
	//  {
		for (k = 0; k < SINGLE_FRAME_LENGTH; k++)
		    {
			single_frame[k] = mdata[k];
		    }
		getParameters(single_frame);
		//frame_index += 260;
	  
	  
		//now, push the stuff on!
		for (i = 0; i < 4; i++)
		    {
			for (a = 0; a < 13; a++)
			    {
				output.pushInt(mSequence[i+4*a]);
			    }
			output.pushInt(mRpeMagnitude[i]);
			output.pushInt(mRpeGridPosition[i]);
		    }
		donepushing = true;
		//  }
	//System.err.println("RPE Input Filter yeah!");
    }
}

/**
 *  Temporary addition:  
 *  takes in int[160] array, pushes out 40 bits at a time 
 *  so that LAR filtering is only applied after all four subframes
 *  are processed.
 */
class HoldFilter extends Filter
{
    int[] mDrp;

#define MDRP_LONG_LENGTH 160

    public void init()
    {
	input = new Channel(Integer.TYPE, 160);
	output = new Channel(Integer.TYPE, 40);
	mDrp = new int[MDRP_LONG_LENGTH];
    }

    public void work()
    {
	int i, j;
	for (i = 0; i < MDRP_LONG_LENGTH; i++)
	    {
		mDrp[i] = input.popInt();
	    }
	for (j = 0; j < 40; j++)
	    {
		output.pushInt(mDrp[j + 120]);
	    }
	//System.err.println("Hold filter go!");
    }
    
}

class IntegerPrinter extends Filter
{
    char c;
    IntegerPrinter (char c2)
    {
	super (c2);
    }
    public void init(char c2)
    {  
	input = new Channel(Integer.TYPE, 1);
	output = new Channel(Integer.TYPE, 1);
	this.c = c2;
    }

    public void work()
    {
	int a = input.popInt();
	System.out.println(c);
	System.out.println(a);
	output.pushInt(a);
    }
}

public class StGsmDecoder extends StreamIt 
{
    //include variables for parsing here!
    public static void main(String args[]) 
    {
	new StGsmDecoder().run(args); 
	
    }

    public void init() {
	
	this.add(new FileReader("IntBinaryDecoderInput1", Integer.TYPE));
	//this.add(new IdentityPrinter(1));
	this.add(new RPEInputFilter());
	//this.add(new IdentityPrinter(1));
	this.add(new RPEDecodeFilter());
	this.add(new IdentityPrinter(1));
	this.add(new DecoderFeedback());
	//this.add(new IntegerPrinter());
	this.add(new HoldFilter());
	this.add(new LARInputSplitJoin());
	this.add(new ReflectionCoeff());
	this.add(new IntegerTermSynth());
	this.add(new PostProcessingFilter());
	//System.err.println("gets here!");
	this.add(new IntegerPrint());
	//this.add(new FileWriter("BinaryDecoderOutput1Int", Integer.TYPE));	
    }
}
 
class IdentityPrinter extends Filter
{
    int rate;

     IdentityPrinter (int rate)
    {
	super (rate);
    }
    public void init(final int rate)
    {  
	this.rate = rate;
	input = new Channel(Integer.TYPE, rate);
	output = new Channel(Integer.TYPE, rate);
    }

    public void work()
    {
	//System.out.println("gets here with rate " + rate);
	for (int i = 0; i < rate; i++)
	    {
		int a = input.popInt();
		//System.out.println(a);
		output.pushInt(a);
	    }
	
    }
}


class IntegerPrint extends Filter
{
    IntegerPrint ()
    {
	super ();
    }
    public void init()
    {  
	input = new Channel(Integer.TYPE, 1);
    }

    public void work()
    {
	int a = input.popInt();
	//System.out.println(a);
    }
}



