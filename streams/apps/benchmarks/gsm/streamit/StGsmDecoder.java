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


import streamit.*;
import java.lang.*;

import java.lang.reflect.*;
import streamit.io.*;
        
class RPEDecodeFilter extends Filter 
{
    short[] mXmc;
    short[] FAC; 
    short[] xmp;
    short[] ep;

#include "Helper.java"
#define EP_LENGTH 40

    public void init() 
    {
	input = new Channel(Short.TYPE, 15);
	output = new Channel(Short.TYPE, 40);
	mXmc = new short[13];   //mSequence
	//others are in work() method	
	xmp = new short[13]; //intermediary
	ep = new short[EP_LENGTH];  //output

	////////////////////////////////////////////
	// assign the members of the FAC array
	FAC = new short[8];
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
	short i, k, xmaxc, mc, exp, mant, temp, temp1, temp2, temp3;
	/**
	 *  Inputs to RPEDecodeFilter: Xmaxc - Maximum value of the 13 bits, 
	 *                     Xmc - 13 bit sample
	 *                     Mc - 2 bit grid positioning
	 */
	
	for (i = 0; i < 13; i++)
	    {
		mXmc[i] = input.popShort();
	    }
	xmaxc = input.popShort();    //mRpeMagnitude
	mc = input.popShort();       //mRpeGridPosition

	 
	//Get the exponent, mantissa of Xmaxc:
       
	exp = 0;
	if (xmaxc > 15)
	    {
		exp = gsm_sub(shortify(xmaxc >> 3), (short) 1);
	    }
	mant = gsm_sub(xmaxc, shortify(exp << 3));
	
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
			mant = shortify(mant << 1 | 1);
			exp--;
		    }
		mant = gsm_sub(mant, (short) 8);
	    }
	
	/* 
	 *  This part is for decoding the RPE sequence of coded Xmc[0..12]
	 *  samples to obtain the xMp[0..12] array.  Table 4.6 is used to get
	 *  the mantissa of xmaxc (FAC[0..7]).
	 */
	
	temp1 = FAC[mant];
	temp2 = gsm_sub((short) 6, exp);
	temp3 = shortify(1 << gsm_sub(temp2, (short) 1));

	for (i = 0; i < 13; i++)
	    {
		temp = gsm_sub(shortify(mXmc[i] << 1), (short) 7);    //3 bit unsigned
		temp <<= 12;                 //16 bit signed
		temp = gsm_mult_r(temp1, temp);
		temp = gsm_add(temp, temp3);
		xmp[i] = shortify(temp >> temp2);
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
		output.pushShort(ep[i]);
	    }
	//System.err.println("Got to RPEDecode!");
    }
}

class LTPFilter extends Filter 
{
    short[] QLB;
    short[] drp;
    short nrp;

#include "Helper.java"
   
    public void init() 
    {
	input = new Channel(Short.TYPE, 162);
	output = new Channel(Short.TYPE, 1);
	drp = new short[160];
	nrp = 40;   //initial condition

	////////////////////////////////////////////
	// assign the members of the QLB array
	QLB = new short[4];
	QLB[0] = 3277;
	QLB[1] = 11469;
	QLB[2] = 21299;
	QLB[3] = 32767;
	////////////////////////////////////////////
    }

    public void work()  //output:  drpp
    {
	short i, nr, brp, drpp;
	short mBcr = input.popShort();  //mLtpGain
	short mNcr = input.popShort();  //mLtpOffset
	//do it!
	for (i = 0; i < 160; i++)
	    {
		drp[i] = input.popShort(); //drp from AdditionUpdateFilter
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
	
	output.pushShort(drpp);   
	//System.err.println("Got to LTPFilter!");
    }
}

class AdditionUpdateFilter extends Filter 
{       
    short[] ep;
    short[] drp;

#include "Helper.java"
#define DRP_LENGTH 160

    public void init() 
    {
	short i;
	input = new Channel(Short.TYPE, 41);
	output = new Channel(Short.TYPE, 160);
	ep = new short[40]; //input
	drp = new short[DRP_LENGTH];  //output
	for (i = 0; i < DRP_LENGTH; i++)
	    {
		drp[i] = 0;   //initial conditions
	    }
    }

    public void work() 
    {
	short i, j, k, drpp;
	//get inputs:
	for (i = 0; i < 40; i++)
	    {
		ep[i] = input.popShort();
	    }
	drpp = input.popShort();

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
		output.pushShort(drp[i]);
	    }
	//System.err.println("Got to Addition Update Filter!");
    }
}

class ReflectionCoeffFilter extends Filter 
{

    short[] INVA;
    short[] MIC;
    short[] B;
    
    short[] mdrpin; //input
    short[] mdrp;   //output
    short[] mLARc;  //input
    short[] mLARpp; //intermediary
    short[] mprevLARpp; //intermediary
    short[] mLARp;  //intermediary
    short[] mrrp; //output

#include "Helper.java"
#define MPREVLARPP_LENGTH 8
#define MDRPIN_LENGTH 160
#define MDRP_LENGTH 40
#define MLARC_LENGTH 8
#define MLARPP_LENGTH 8
#define MRRP_LENGTH 8

    public void init() 
    {
	short i;
	input = new Channel(Short.TYPE, 168);
	output = new Channel(Short.TYPE, 48);
	mdrpin = new short[MDRPIN_LENGTH];
	mdrp = new short[MDRP_LENGTH];
	mLARc = new short[MLARC_LENGTH];
	mLARpp = new short[MLARPP_LENGTH];
	mprevLARpp = new short[MPREVLARPP_LENGTH];
	for(i = 0; i < MPREVLARPP_LENGTH; i++)
	    {
		mprevLARpp[i] = 0;
	    }
	mLARp = new short[8];
	mrrp = new short[MRRP_LENGTH];


	//////////////////////////////////////////////////
	// assign elements of INVA array
	INVA = new short[8];
	INVA[0] = 13107;
	INVA[1] = 13107;
	INVA[2] = 13107;
	INVA[3] = 13107;
	INVA[4] = 19223;
	INVA[5] = 17476;
	INVA[6] = 31454;
	INVA[7] = 29708;
	//////////////////////////////////////////////////

	//////////////////////////////////////////////////
	// assign elements of MIC array
	MIC = new short[8];
	MIC[0] = -32;
	MIC[1] = -32;
	MIC[2] = -16;
	MIC[3] = -16;
	MIC[4] = -8;
	MIC[5] = -8;
	MIC[6] = -4;
	MIC[7] = -4;
	//////////////////////////////////////////////////

	//////////////////////////////////////////////////
	// assign elements of B array
	B = new short[8];
	B[0] = 0;
	B[1] = 0;
	B[2] = 2048;
	B[3] = -2560;
	B[4] = 94;
	B[5] = -1792;
	B[6] = -341;
	B[7] = -1144;
	//////////////////////////////////////////////////
    }

    public void work() 
    {
	int i;
	short j, temp, temp1, temp2, k, sri;
	for (i = 0; i < MDRPIN_LENGTH; i++)
	    {
		mdrpin[i] = input.popShort();
	    }
	//truncate to only get mdrpin[120...159]
	for (i = 0; i < MDRP_LENGTH; i++)
	    {
		mdrp[i] = mdrpin[i + 120];
	    }
	for (i = 0; i < MLARC_LENGTH; i++)
	    {
		mLARc[i] = input.popShort();   //fix inputs!!
	    }
	
	
	//Decoding of the coded Log-Area ratios:
	for (i = 0; i < 8; i++)
	    {
		temp1 = shortify((gsm_add(mLARc[i], MIC[i])) << 10);
		temp2 = shortify(B[i] << 10);
		temp1 = gsm_sub(temp1, temp2);
		temp1 = gsm_mult_r(INVA[i], temp1);
		mLARpp[i] = gsm_add(temp1, temp1);
	    }
	//Computation of the quantized reflection coefficients

	//Interpolation of mLARpp to get mLARp:
	for (k = 0; k < 13; k++)
	    {
		for(i = 0; i < 8; i++)
		    {
			mLARp[i] = gsm_add(shortify(mprevLARpp[i] >> 2), shortify(mLARpp[i] >> 2));
			mLARp[i] = gsm_add(mLARp[i],  shortify(mprevLARpp[i] >> 1));
		    }
	    }
	for (k = 13; k < 27; k++)
	    {
		for (i = 0; i < 8; i++)
		    {
			mLARp[i] = gsm_add(shortify(mprevLARpp[i] >> 1), shortify(mLARpp[i] >> 1));
		    }
	    }
	for (k = 27; k < 39; k++)
	    {
		for (i = 0; i < 8; i++)
		    {
			mLARp[i] = gsm_add(shortify(mprevLARpp[i] >> 2), shortify(mLARpp[i] >> 2));
			mLARp[i] = gsm_add(mLARp[i], shortify(mLARpp[i] >> 1));
		    }
	    }
	for (k = 40; k < 160; k++)
	    {
		for (i = 0; i < 8; i++)
		    {
			mLARp[i] = mLARpp[i];
		    }
	    }
	//set current LARpp to previous:
	for (j = 0; j < MPREVLARPP_LENGTH; j++)
	    {
		mprevLARpp[j] = mLARpp[j];
	    }

	//Compute mrrp[0..7] from mLARp[0...7]
	for (i = 0; i < 8; i++)
	    {
		temp = gsm_abs(mLARp[i]);
		if (temp < 11059)
		    {
			temp = shortify(temp << 1);
		    }
		else 
		    {
			if (temp < 20070)
			    {
				temp = gsm_add(temp, (short) 11059);
			    }
			else
			    {
				temp = gsm_add((short) (temp >> 2), (short) 26112);
			    }
		    }
		mrrp[i] = temp;
		if (mLARp[i] < 0)
		    {
			mrrp[i] = gsm_sub((short) 0, mrrp[i]);
		    }
		
	    }
	//push outputs
	for (j = 0; j < MDRP_LENGTH; j++)
	    {
		output.pushShort(mdrp[j]);
	    }
	for (j = 0; j < MRRP_LENGTH; j++)
	    {
		output.pushShort(mrrp[j]);
	    }
    }


}//ReflectionCoeffFilter

class ShortTermReorder extends Filter
{
    short mrrp[];
    
    public void init()
    {
        input = new Channel(Short.TYPE, 8 + 40);
        output = new Channel(Short.TYPE, (8 + 1) * 40);
        mrrp = new short[8];
    }
    
    public void work()
    {
        short val;
        int i, j;
        
        // Read in mrrp:
        for (j = 0; j < 8; j++)
            mrrp[j] = input.popShort();

        for (i = 0; i < 40; i++)
        {
            for (j = 0; j < 8; j++)
                output.pushShort(mrrp[j]);
            output.pushShort(input.popShort());
        }
    }
}

class ShortTermSynthCalc extends Filter
{
    short[] mrrp;
    short[] v;
 
#include "Helper.java"
#define V_LENGTH 9
#define MRRP_LENGTH 8
   
    public void init()
    {
        input = new Channel(Short.TYPE, MRRP_LENGTH + 1);
        output = new Channel(Short.TYPE, 1);
        mrrp = new short[MRRP_LENGTH];
	v = new short[V_LENGTH];
	for (int i = 0; i < V_LENGTH; i++)
	    v[i] = 0;
    }

    public void work()
    {
        int i;
        short sri;
        
        for (i = 0; i < MRRP_LENGTH; i++)
            mrrp[i] = input.popShort();
        sri = input.popShort();
        for (i = 1; i < 8; i++)
        {
            sri = gsm_sub(sri, gsm_mult(mrrp[8-i], v[8-i]));
            v[9-i] = gsm_add(v[8-i], gsm_mult_r(mrrp[8-i], sri));
        }
        v[0] = sri;
        output.pushShort(sri);
    }
}

class ShortTermSynth extends Pipeline
{
    public void init()
    {
        add(new ShortTermReorder());
        add(new ShortTermSynthCalc());
    }
}

class LARInputFilter extends Filter
{
    //order of output: mLarParameters[0...8]
    short[] mdata;
    short[] single_frame;
    boolean donepushing;

#include "DecoderInput.java"
#define MDATA_LENGTH 260
#define SINGLE_FRAME_LENGTH 260

    public void init()
    {
	mdata = new short[MDATA_LENGTH];
	single_frame = new short[SINGLE_FRAME_LENGTH];
	input = new Channel(Short.TYPE, 260);
	output = new Channel(Short.TYPE, 8); 
	donepushing = false;
    }

    public void work()
    {
	int i, j, k;
	//int frame_index = 0;
	for (i = 0; i < MDATA_LENGTH; i++)
	    {
		mdata[i] = input.popShort();
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
			output.pushShort(mLarParameters[i]);
		    }
		donepushing = true;
		//    }
	//System.err.println("LARinputFilter gooo!");

    }
}
class PostProcessingFilter extends Filter 
{
    short msr;

#include "Helper.java"

    public void init() 
    {
	input = new Channel(Short.TYPE, 1);
	output = new Channel(Short.TYPE, 1);
	msr = 0; //initial condition
    }

    public void work() 
    {
	int a;
	short i, k, temp;
        
        temp = input.popShort();

	//De-emphasis filtering!
        temp = gsm_add(temp, gsm_mult_r(msr, (short) 28180));
        msr = temp; // to next execution

	//upscaling of output signal:
        temp = gsm_add(temp, temp);
        
	//truncation of the output variable:
        temp = shortify(temp / 8);
        temp = gsm_mult(temp, (short)8);
        
        output.pushShort(temp);
    }
}

class LTPInputFilter extends Filter
{
    //order of output: mLtpGain[4], mLtpOffset[4]
    short[] mdata;
    short[] single_frame;
    boolean donepushing;

#include "DecoderInput.java"
#define MDATA_LENGTH 260

    public void init()
    {
	mdata = new short[MDATA_LENGTH];
	single_frame = new short[SINGLE_FRAME_LENGTH];
	input = new Channel(Short.TYPE, 260);
	output = new Channel(Short.TYPE, 8);
	donepushing = false;
    }

    public void work()
    {
	int i, j, k;
	//int frame_index = 0;
	for (i = 0; i < MDATA_LENGTH; i++)
	    {
		mdata[i] = input.popShort();
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
			output.pushShort(mLtpGain[i]);
			output.pushShort(mLtpOffset[i]);
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
	this.add(new FileReader("BinaryDecoderInput1", Short.TYPE));
	this.add(new LTPInputFilter());
	this.add(new ShortPrinter('g'));
    }
}
class LARPipeline extends Pipeline
{
    public void init()
    {
	this.add(new FileReader("BinaryDecoderInput1", Short.TYPE));
	this.add(new LARInputFilter());
	//this.add(new ShortPrinter());
    }
}

class LTPInputSplitJoin extends SplitJoin
{
    public void init()
    {
	this.setSplitter(WEIGHTED_ROUND_ROBIN (0, 1));
	this.add(new LTPPipeline());
	//this.add(new ShortIdentity());
	this.add(new ShortPrinter('d'));
	this.setJoiner(WEIGHTED_ROUND_ROBIN(2, 160)); //bcr, ncr, drp[0...159]
    }
}

class LTPLoopStream extends Pipeline
{
    public void init()
    {
	this.add(new LTPInputSplitJoin());
	this.add(new Pipeline() 
	    {
		public void init()
		{
		    this.add(new ShortPrinter('e'));
		    this.add(new LTPFilter());
		    this.add(new ShortPrinter('b')); 
		}
	    });
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
	//       required for ShortTermSynth, this is currently
	//       the simplest way to implement things, theinput will be filtered internally.
 	this.setLoop(new LTPLoopStream());
    }

    public short initPathShort(int index)
    {
	return 0;
    }
	
}


class StupidStream extends Pipeline
{
    public void init()
    {
	this.add(new ShortPrinter('a'));
	this.add(new AdditionUpdateFilter());
	this.add(new ShortPrinter('c'));
    }
}

class LARInputSplitJoin extends SplitJoin
{
    public void init()
    {
	this.setSplitter(WEIGHTED_ROUND_ROBIN (1, 0));  //we don't care about it going to in2
	this.add(new Identity(Short.TYPE));
	this.add(new LARPipeline());	
	this.setJoiner(WEIGHTED_ROUND_ROBIN(160, 8));  //drp[0...160], LARc[0...7];
    }
}

class RPEInputFilter extends Filter
{
    //order of output: mSequence[0..13], mRpeMagnitude, mRpeGridPosition
    short[] mdata;
    short[] single_frame;
    boolean donepushing;

#include "DecoderInput.java"
    
    public void init()
    {
	mdata = new short[MDATA_LENGTH];
	single_frame = new short[SINGLE_FRAME_LENGTH];
	input = new Channel(Short.TYPE, 260);
	output = new Channel(Short.TYPE, 60); 
	donepushing = false;
    }

    public void work()
    {
	int i, j, k, a;
	//int frame_index = 0;
	//System.err.println("I get here!!!");
	for (i = 0; i < MDATA_LENGTH; i++)
	    {
		mdata[i] = input.popShort();
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
				output.pushShort(mSequence[i+4*a]);
			    }
			output.pushShort(mRpeMagnitude[i]);
			output.pushShort(mRpeGridPosition[i]);
		    }
		donepushing = true;
		//  }
	//System.err.println("RPE Input Filter yeah!");
    }
}

/**
 *  Temporary addition:  
 *  takes in short[160] array, pushes out 40 bits at a time 
 *  so that LAR filtering is only applied after all four subframes
 *  are processed.
 */
class HoldFilter extends Filter
{
    short[] mDrp;

#define MDRP_LONG_LENGTH 160

    public void init()
    {
	input = new Channel(Short.TYPE, 160);
	output = new Channel(Short.TYPE, 40);
	mDrp = new short[MDRP_LONG_LENGTH];
    }

    public void work()
    {
	int i, j;
	for (i = 0; i < MDRP_LONG_LENGTH; i++)
	    {
		mDrp[i] = input.popShort();
	    }
	for (j = 0; j < 40; j++)
	    {
		output.pushShort(mDrp[j + 120]);
	    }
	//System.err.println("Hold filter go!");
    }
    
}

class ShortPrinter extends Filter
{
    char c;
    ShortPrinter (char c2)
    {
	super (c2);
    }
    public void init(char c2)
    {  
	input = new Channel(Short.TYPE, 1);
	output = new Channel(Short.TYPE, 1);
	this.c = c2;
    }

    public void work()
    {
	short a = input.popShort();
	System.out.println(c);
	System.out.println(a);
	output.pushShort(a);
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
	
	this.add(new FileReader("BinaryDecoderInput1", Short.TYPE));
	//this.add(new ShortPrinter());
	this.add(new RPEInputFilter());
	//this.add(new ShortPrinter());
	this.add(new RPEDecodeFilter());
	this.add(new DecoderFeedback());
	//this.add(new ShortPrinter());
	this.add(new HoldFilter());
	this.add(new LARInputSplitJoin());
	this.add(new ReflectionCoeffFilter());
	this.add(new ShortTermSynth());
	this.add(new PostProcessingFilter());
	//this.add(new ShortPrinter());
	this.add(new streamit.io.FileWriter("BinaryDecoderOutput1", Short.TYPE));	
    }
}
 






