import streamit.*;

class AFilter extends Filter 
{
    Channel input = new Channel(Float.TYPE, 1);
    Channel output = new Channel(Float.TYPE, 1);

       
    public void init() {}

    public void work() 
    {
	/**
	 *  Inputs to AFilter: Xmaxc - Maximum value of the 13 bits, 
	 *                     Xmc - 13 bit sample
	 *                     Mc - 2 bit grid positioning
	 */
	
	short xmaxc = 0;   //intializations temporary until file input is fixed
	short[] xmc = {}; 
	short mc = 0;

	short[] FAC = {29218, 26215, 23832, 21846, 20165, 18725, 17476, 16384};
 
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
	 *  This part is for decoding the RPE sequence of coded xMc[0..12]
	 *  samples to obtain the xMp[0..12] array.  Table 4.6 is used to get
	 *  the mantissa of xmaxc (FAC[0..7]).
	 */
	
	int temp;
	int temp1 = FAC[mant];
	int temp2 = 6 - exp;
	int temp3 = 1 << (temp2 - 1);
	int[] xmp = {};  // intermediary output!

	for (int i = 0; i < 13; i++)
	    {
		temp = (xmc[i] << 1) - 7;    //3 bit unsigned
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
	int[] ep = {};  //output!

	for(int k = 0; k < 40; k++)
	    {
		ep[k] = 0;
	    }
	for(int i = 0; i < 12; i++)
	    {
		ep[mc + (3 * i)] = xmp[i];
	    } 
    }
}

class BFilter extends Filter 
{
    Channel input = new Channel(Float.TYPE, 1);
    Channel output = new Channel(Float.TYPE, 1);

   
    public void init() {}

    public void work() 
    {
	//fill in details!
    }
}

class CFilter extends Filter 
{
    Channel input = new Channel(Float.TYPE, 1);
    Channel output = new Channel(Float.TYPE, 1);

   

    public void init() {}

    public void work() 
    {
	//fill in details!
    }
}

class DFilter extends Filter 
{
    Channel input = new Channel(Float.TYPE, 1);
    Channel output = new Channel(Float.TYPE, 1);

   

    public void init() {}

    public void work() 
    {
	//fill in details!
    }
}

class EFilter extends Filter 
{
    Channel input = new Channel(Float.TYPE, 1);
    Channel output = new Channel(Float.TYPE, 1);

   

    public void init() {}

    public void work() 
    {
	//fill in details!
    }
}

class DecoderFeedback extends FeedbackLoop
{
    public void init()
    {
	this.setJoiner(ROUND_ROBIN ());
	this.setBody(new BFilter());
	this.setSplitter(DUPLICATE ());
	this.setLoop(new CFilter());
    }
}





public class StGsmDecoder extends StreamIt 
{
    public static void main(String args[]) 
    {
	StGsmDecoder.run();
    }

    public void init() {
	this.add(new AFilter());
	this.add(new DecoderFeedback());
	this.add(new DFilter());
	this.add(new EFilter());
    }
}
 


