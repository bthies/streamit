package at.dms.kjc.sir.linear.frequency;

/**
 * This class contains a "javafied" version of an FFT code
 * that I found on the internet. Originally I used it for
 * computing the coefficients at compile time. Now we compute the
 * coefficients at runtime with fftw.<p>
 *
 * This code is taken from http://www.intersrv.com/~dcross/fft.html
 * originally written by Don Cross (dcross@intersrv.com) and
 * javafied by AAL (aalamb@mit.edu) 10/28/2002.
 **/
public class LinearFFT {
    public static final int BITS_PER_WORD = 4 * 8;
    public static final int FORWARD = 0;
    public static final int REVERSE = 1;
    public static final double DDC_PI  = -3.14159265358979323846;

    public static boolean IsPowerOfTwo ( int x )
    {
	if (x < 2)
	    return false;
	    
	if ((x & (x-1)) != 0)        // Thanks to 'byang' for this cute trick!
	    return false;
	    
	return true;
    }


    public static int NumberOfBitsNeeded ( int PowerOfTwo )
    {
	int i;
	    
	if ( PowerOfTwo < 2 )
	    {
		throw new RuntimeException(">>> Error in fftmisc.c: argument " +
					   PowerOfTwo +
					   " to NumberOfBitsNeeded is too small.\n");
	    }

	for ( i=0; ; i++ )
	    {
		if ( (PowerOfTwo & (1 << i)) != 0 )
		    return i;
	    }
    }
	
	
    public static int ReverseBits ( int index, int NumBits )
    {
	int i, rev;
	    
	for ( i=rev=0; i < NumBits; i++ )
	    {
		rev = (rev << 1) | (index & 1);
		index >>= 1;
	    }
	    
	return rev;
    }


    public static double Index_to_frequency ( int NumSamples, int Index )
    {
	if ( Index >= NumSamples )
	    return 0.0;
	else if ( Index <= NumSamples/2 )
	    return (double)Index / (double)NumSamples;
	    
	return -(double)(NumSamples-Index) / (double)NumSamples;
    }
	
    public static void fft_float (int       NumSamples,
				  boolean   InverseTransform,
				  float[]   RealIn,
				  float[]   ImagIn,
				  float[]   RealOut,
				  float[]   ImagOut )
    {
	int NumBits;    /* Number of bits needed to store indices */
	int i, j, k, n;
	int BlockSize, BlockEnd;
	    
	double angle_numerator = 2.0 * DDC_PI;
	double tr, ti;     /* temp real, temp imaginary */
	    
	if ( !IsPowerOfTwo(NumSamples) )
	    {
		throw new RuntimeException("Error in fft():  NumSamples=" +
					   NumSamples +
					   " is not power of two\n");
	    }
	    
	if ( InverseTransform )
	    angle_numerator = -angle_numerator;
	    
	//CHECKPOINTER ( RealIn );
	//CHECKPOINTER ( RealOut );
	//CHECKPOINTER ( ImagOut );
	    
	NumBits = NumberOfBitsNeeded ( NumSamples );

	/*
	**   Do simultaneous data copy and bit-reversal ordering into outputs...
	*/
	    
	for ( i=0; i < NumSamples; i++ )
	    {
		j = ReverseBits ( i, NumBits );
		RealOut[j] = RealIn[i];
		ImagOut[j] = (ImagIn == null) ? 0.0f : ImagIn[i];
	    }
	    
	/*
	**   Do the FFT itself...
	*/
	    
	BlockEnd = 1;
	for ( BlockSize = 2; BlockSize <= NumSamples; BlockSize <<= 1 )
	    {
		double delta_angle = angle_numerator / (double)BlockSize;
		double sm2 = Math.sin ( -2 * delta_angle );
		double sm1 = Math.sin ( -delta_angle );
		double cm2 = Math.cos ( -2 * delta_angle );
		double cm1 = Math.cos ( -delta_angle );
		double w = 2 * cm1;
		double[] ar = new double[3];
		double[] ai = new double[3];
		double temp;
		    
		for ( i=0; i < NumSamples; i += BlockSize )
		    {
			ar[2] = cm2;
			ar[1] = cm1;
			    
			ai[2] = sm2;
			ai[1] = sm1;
			    
			for ( j=i, n=0; n < BlockEnd; j++, n++ )
			    {
				ar[0] = w*ar[1] - ar[2];
				ar[2] = ar[1];
				ar[1] = ar[0];
				    
				ai[0] = w*ai[1] - ai[2];
				ai[2] = ai[1];
				ai[1] = ai[0];
				    
				k = j + BlockEnd;
				tr = ar[0]*RealOut[k] - ai[0]*ImagOut[k];
				ti = ar[0]*ImagOut[k] + ai[0]*RealOut[k];
				    
				RealOut[k] = (float)(RealOut[j] - tr);
				ImagOut[k] = (float)(ImagOut[j] - ti);
				    
				RealOut[j] += tr;
				ImagOut[j] += ti;
			    }
		    }
		    
		BlockEnd = BlockSize;
	    }
	    
	/*
	**   Need to normalize if inverse transform...
	*/
	    
	if ( InverseTransform )
	    {
		double denom = (double)NumSamples;
		    
		for ( i=0; i < NumSamples; i++ )
		    {
			RealOut[i] /= denom;
			ImagOut[i] /= denom;
		    }
	    }
    }
}

