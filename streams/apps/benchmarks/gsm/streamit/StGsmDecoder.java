import streamit.*;
import java.lang.*;

class RPEDecodeFilter extends Filter 
{
    Channel input = new Channel(Integer.TYPE, 1);
    Channel output = new Channel(Integer.TYPE, 1);
    int[] mXmc;
    static short[] FAC = {29218, 26215, 23832, 21846, 20165, 18725, 17476, 16384};
    int[] xmp;
    int[] ep;

    public void init() 
    {
	mXmc = new int[13];   //mSequence
	//others are in work() method	
	xmp = new int[13]; //intermediary
	ep = new int[40];  //output
    }

    public void work() 
    {
	/**
	 *  Inputs to RPEDecodeFilter: Xmaxc - Maximum value of the 13 bits, 
	 *                     Xmc - 13 bit sample
	 *                     Mc - 2 bit grid positioning
	 */
	
	for (int i = 0; i < 13; i++)
	{
	    mXmc[i] = input.popInt();
	}
	int xmaxc = input.popInt();    //mRpeMagnitude
	int mc = input.popInt();       //mRpeGridPosition

	 
	//Get the exponent, mantissa of Xmaxc:
       
	int exp = 0;
	if (xmaxc > 15)
	    {
		exp = (xmaxc >> 3) - 1;
	    }
	int mant = xmaxc - (exp << 3);
	
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
			mant = mant << 1 | 1;
			exp--;
		    }
		mant -= 8;
	    }
	
	/* 
	 *  This part is for decoding the RPE sequence of coded Xmc[0..12]
	 *  samples to obtain the xMp[0..12] array.  Table 4.6 is used to get
	 *  the mantissa of xmaxc (FAC[0..7]).
	 */
	
	int temp;
	int temp1 = FAC[mant];
	int temp2 = 6 - exp;
	int temp3 = 1 << (temp2 - 1);

	for (int i = 0; i < 13; i++)
	    {
		temp = (mXmc[i] << 1) - 7;    //3 bit unsigned
		temp <<= 12;                 //16 bit signed
		temp = temp1 * temp;
		temp += temp3;
		xmp[i] = temp >> temp2;
	    }

	/**
	 *  This procedure computes the reconstructed long term residual signal
	 *  ep[0..39] for the LTP analysis filter.  The inputs are the Mc
	 *  which is the grid position selection and the xMp[0..12] decoded
	 *  RPE samples which are upsampled by a factor of 3 by inserting zero
	 *  values.
	 */
         //output!

	for(int k = 0; k < 40; k++)
	    {
		ep[k] = 0;
	    }
	for(int i = 0; i < 12; i++)
	    {
		ep[mc + (3 * i)] = xmp[i];
	    } 
	//output the sucker!
	for (int i = 0; i < ep.length; i++)
	{
	    output.pushInt(ep[i]);
	}
    }
}

class LTPFilter extends Filter 
{
    Channel input = new Channel(Integer.TYPE, 1);
    Channel output = new Channel(Integer.TYPE, 1);

    static int[] QLB = {3277, 11469, 21299, 32767};
    int[] drp;
    int nrp;
   
    public void init() {
	drp = new int[160];
	nrp = 40;   //initial condition
    }

    public void work()  //output:  drpp
    {
	int mBcr = input.popInt();  //mLtpGain
	int mNcr = input.popInt();  //mLtpOffset
	//do it!
	for (int i = 0; i < 160; i++)
	{
	    drp[i] = input.popInt(); //drp from AdditionUpdateFilter
	}
	int nr = mNcr;
	if ((mNcr < 40) || (mNcr > 120))
	{
	    nr = nrp;
	}
	nrp = nr;

	//Decoding of the LTP gain mBcr
	int brp = QLB[mBcr];
	int drpp = 1;
	for (int i = 121; i < 161; i++)
	{
	    drpp = brp * drp[i - nr];   //delay and multiply operations
	} 
	
	output.pushInt(drpp);   //eh!?!?  I can't pass around primitives?!
    }
}

class AdditionUpdateFilter extends Filter 
{
    Channel input = new Channel(Integer.TYPE, 1);
    Channel output = new Channel(Integer.TYPE, 1);
    
    int[] ep;
    int[] drp;
    public void init() 
    {
	ep = new int[40]; //input
	drp = new int[160];  //output
	for (int i = 0; i < drp.length; i++)
	{
	    drp[i] = 0;   //initial conditions
	}
    }

    public void work() 
    {
	//get inputs:
	for (int i = 0; i < 40; i++)
	{
	    ep[i] = input.popInt();
	}
	int drpp = input.popInt();

	for (int j = 121; j < 160; j++)  //add part!
	{
	    drp[j] = ep[j - 121] + drpp;
	}
	for (int k = 0; k < 120; k++)    //update part!
	{
	    drp[k] = drp[k + 40];
	}
	
	for (int i = 0; i < drp.length; i++)  //output!
	{
	    output.pushInt(drp[i]);
	}
    }
}

class ShortTermSynthFilter extends Filter 
{
    Channel input = new Channel(Integer.TYPE, 1);
    Channel output = new Channel(Integer.TYPE, 1);

    static int[] INVA = {13107, 13107, 13107, 13107, 19223, 17476, 31454, 29708};
    static int[] MIC = {-32, -32, -16, -16, -8, -8, -4, -4};
    static int[] B = {0, 0, 2048, -2560, 94, -1792};
    
    int[] mdrp;   //input
    int[] mLARc;  //input
    int[] mLARpp; //intermediary
    int[] mprevLARpp; //intermediary
    int[] mLARp;  //intermediary
    int[] mrrp; //intermediary
    int[] wt; //temporary array
    int[] v; //temporary array
    int[] sr; //output!

    public void init() 
    {
	mdrp = new int[40];
	mLARc = new int[8];
	mLARpp = new int[8];
	mprevLARpp = new int[8];
	for(int i = 0; i < mprevLARpp.length; i++)
	{
	    mprevLARpp[i] = 0;
	}
	mLARp = new int[8];
	mrrp = new int[8];
	wt = new int[160];
	v = new int[9];
	for (int i = 0; i < v.length; i++)
	{
	    v[i] = 0;
	}
	sr = new int[160];
    }

    public void work() 
    {
	for (int i = 0; i < mLARc.length; i++)
	{
	    mLARc[i] = input.popInt();   //fix inputs!!
	}
	for (int i = 0; i < mdrp.length; i++)
	{
	    mdrp[i] = input.popInt();
	}
	
	//Decoding of the coded Log-Area ratios:
	for (int i = 0; i < 8; i++)
	{
	    int temp1 = (mLARc[i] + MIC[i]) << 10;
	    int temp2 = B[i] << 10;
	    temp1 = temp1 - temp2;
	    temp1 = INVA[i] * temp1;
	    mLARpp[i] = temp1 + temp1;
	}
	    //Computation of the quantized reflection coefficients

	//Interpolation of mLARpp to get mLARp:
	for (int k = 0; k < 13; k++)
	{
	    for(int i = 0; i < 8; i++)
	    {
		mLARp[i] = (mprevLARpp[i] >> 2) + (mLARpp[i] >> 2);
		mLARp[i] = mLARp[i] + (mprevLARpp[i] >> 1);
	    }
	}
	for (int k = 13; k < 27; k++)
	{
	    for (int i = 0; i < 8; i++)
	    {
		mLARp[i] = (mprevLARpp[i] >> 1) + (mLARpp[i] >> 1);
	    }
	}
	for (int k = 27; k < 39; k++)
        {
	    for (int i = 0; i < 8; i++)
	    {
		mLARp[i] = (mprevLARpp[i] >> 2) + (mLARpp[i] >> 2);
		mLARp[i] = mLARp[i] + (mLARpp[i] >> 1);
	    }
	}
	for (int k = 40; k < 160; k++)
	{
	    for (int i = 0; i < 8; i++)
	    {
		mLARp[i] = mLARpp[i];
	    }
	}
	//set current LARpp to previous:
	for (int j = 0; j < mprevLARpp.length; j++)
	{
	    mprevLARpp[j] = mLARpp[j];
	}

	//Compute mrrp[0..7] from mLARp[0...7]
	for (int i = 0; i < 8; i++)
	{
	    int temp = Math.abs(mLARp[i]);
	    if (temp < 11059)
	    {
		temp = temp << 1;
	    }
	    else 
	    {
		if (temp < 20070)
		{
		    temp = temp + 11059;
		}
		else
		{
		    temp = (temp >> 2) + 26112;
		}
	    }
	    mrrp[i] = temp;
	    if (mLARp[i] < 0)
	    {
		mrrp[i] = 0 - mrrp[i];
	    }
	}

	//Short term synthesis filtering:  uses drp[0..39] and rrp[0...7] 
	// to produce sr[0...159].  A temporary array wt[0..159] is used.
	for (int k = 0; k < 40; k++)
	{
	    wt[k] = mdrp[k];
	}
	for (int k = 0; k < 40; k++)
	{
	    wt[40+k] = mdrp[k];
	}
	for (int k = 0; k < 40; k++)
	{
	    wt[80+k] = mdrp[k];
	}
	for (int k = 0; k < 40; k++)
	{
	    wt[120+k] = mdrp[k];
	}
	//below is supposed to be from index_start to index_end...how is
	//this different from just 0 to 159?
	for (int k = 0; k < 160; k++)
	{
	    int sri = wt[k];
	    for (int i = 1; i < 8; i++)
	    {
		sri = sri - (mrrp[8-i] * v[8-i]);
		v[9-i] = v[8-i] + (mrrp[8-i] * sri);
	    }
	    sr[k] = sri;
	    v[0] = sri;
	}
	
	for (int j = 0; j < sr.length; j++)
	{
	    output.pushInt(sr[j]);
	}
    }
}

class PostProcessingFilter extends Filter 
{
    int[] mSr;  //input
    int[] sro; //output of PostProcessing, input to upscaling/truncation
    int[] srop; //output
    int msr;

    Channel input = new Channel(Integer.TYPE, 1);
    Channel output = new Channel(Integer.TYPE, 1);

    public void init() 
    {
	mSr = new int[160];
	sro = new int[160];
	srop = new int[160];
	msr = 0; //initial condition
    }

    public void work() 
    {
	for (int i = 0; i < mSr.length; i++)
	{
	    mSr[i] = input.popInt();
	}

	//De-emphasis filtering!
	for (int k = 0; k < mSr.length; k++)
	{
	    int temp = mSr[k] + (msr * 28180);
	    msr = temp;
	    sro[k] = msr;
	}

	//upscaling of output signal:
	for (int k = 0; k < srop.length; k++)
	{
	    srop[k] = sro[k] + sro[k];
	}

	//truncation of the output variable:
	for (int k = 0; k < srop.length; k++)
	{
	    srop[k] = srop[k] >> 3;
	    srop[k] = srop[k] << 3;
	}
	
	for (int j = 0; j < srop.length; j++)
	{
	    output.pushInt(srop[j]);
	}
    }
}

class DecoderFeedback extends FeedbackLoop
{
    public void init()
    {
	this.setJoiner(ROUND_ROBIN ());
	this.setBody(new AdditionUpdateFilter());
	this.setSplitter(DUPLICATE ());
	this.setLoop(new LTPFilter());
    }
}





public class StGsmDecoder extends StreamIt 
{
    //include variables for parsing here!
    public static void main(String args[]) 
    {
	new StGsmDecoder().run(); 
	
    }

    public void init() {
	this.add(new RPEDecodeFilter());
	this.add(new DecoderFeedback());
	this.add(new ShortTermSynthFilter());
	this.add(new PostProcessingFilter());
    }
}
 


