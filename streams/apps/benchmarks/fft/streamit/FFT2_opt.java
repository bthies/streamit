import streamit.library.*;
import streamit.library.io.*;

class Combine_optDFT extends Filter
{
    Combine_optDFT(int i, int j)
    {
        super(i, j);
    }
    float wn_r, wn_i;
    int nWay, nMax;
    public void init(int n, int nmax)
    {
        nWay = n;
	nMax = nmax;
        input = new Channel(Float.TYPE, 2 * nmax);
        output = new Channel(Float.TYPE, 2 * nmax);
	wn_r = (float) Math.cos(2 * 3.141592654 / ((double) n));
        wn_i = (float) Math.sin(2 * 3.141592654 / ((double) n));
    }

    public void work()
    {
        float[] results = new float[2 * nMax];
	int i;
	for (int k=0; k<nMax/nWay; k++) {
	    float w_r = 1;
	    float w_i = 0;
	    for (i = 0; i < nWay; i += 2)
		{
		    int offset = k*2*nWay+i;
		    float y0_r = input.peekFloat(offset);
		    float y0_i = input.peekFloat(offset+1);
		    float y1_r = input.peekFloat(offset+nWay);
		    float y1_i = input.peekFloat(offset+nWay + 1);
		    
		    float y1w_r = y1_r * w_r - y1_i * w_i;
		    float y1w_i = y1_r * w_i + y1_i * w_r;
		    
		    results[offset] = y0_r + y1w_r;
		    results[offset + 1] = y0_i + y1w_i;
		    
		    results[offset+nWay] = y0_r - y1w_r;
		    results[offset+nWay+1] = y0_i - y1w_i;
		    
		    float w_r_next = w_r * wn_r - w_i * wn_i;
		    float w_i_next = w_r * wn_i + w_i * wn_r;
		    w_r = w_r_next;
		    w_i = w_i_next;
		}
	}
	for (int k=0; k<nMax/nWay; k++)
	    for (i = 0; i < 2 * nWay; i++)
		    input.popFloat ();
	for (int k=0; k<nMax/nWay; k++)
	    for (i = 0; i < 2 * nWay; i++)
		    output.pushFloat(results[k*2*nWay+i]);
    }
}

class FFT_optReorderSimple extends Filter
{
    FFT_optReorderSimple (int i) { super (i); }
    
    int nWay;
    int totalData;
    
    public void init (int n)
    {
        nWay = n;
        totalData = nWay * 2;
        
        input = new Channel (Float.TYPE, n * 2);
        output = new Channel (Float.TYPE, n * 2);
    }
    
    public void work ()
    {
        int i;
        
        for (i = 0; i < totalData; i+=4)
        {
            output.pushFloat (input.peekFloat (i));
            output.pushFloat (input.peekFloat (i+1));
        }
        
        for (i = 2; i < totalData; i+=4)
        {
            output.pushFloat (input.peekFloat (i));
            output.pushFloat (input.peekFloat (i+1));
        }
        
        for (i=0;i<nWay;i++)
        {
            input.popFloat ();
            input.popFloat ();
        }
    }
}

class FFT_optReorder extends Pipeline 
{
    FFT_optReorder (int i) { super (i); }
    
    public void init (int nWay)
    {
	int i;
	for (i=1; i<(nWay/2); i*=2) {
	    add (new FFT_optReorderSimple (nWay/i));
	}
    }
}

/*
class FFT_optKernel1 extends Pipeline
{
    public FFT_optKernel1 (int i) { super (i); }
    public void init (final int nWay)
    {
        if (nWay > 2)
        {
            add (new SplitJoin ()
            {
                public void init ()
                {
                    setSplitter (ROUND_ROBIN (2));
                    add (new FFT_optKernel1 (nWay / 2));
                    add (new FFT_optKernel1 (nWay / 2));
                    setJoiner (ROUND_ROBIN (nWay));
                }
            });
        }
        add (new Combine_optDFT (nWay));
    }
}
*/

class FFT_optKernel2 extends SplitJoin
{
    public FFT_optKernel2(int i)
    {
        super(i);
    }
    public void init(final int nWay)
    {
	setSplitter(ROUND_ROBIN(nWay*2));
	for (int i=0; i<2; i++) {
	    add (new Pipeline() {
		    public void init() {
			add (new FFT_optReorder (nWay));
			for (int j=2; j<=nWay; j*=2) {
			    add(new Combine_optDFT (j, nWay));
			}
		    }
		});
	}
	setJoiner(ROUND_ROBIN(nWay*2));
    }
}

public class FFT2_opt extends StreamIt
{
    public static void main(String[] args)
    {
        new FFT2_opt().run(args);
    }
    public void init()
    {
	final int N = 64;
        add(new FFTTestSource(N));
	add(new FFT_optKernel2(N));
	/*
	add(new SplitJoin() {
		public void init() {
		    setSplitter(ROUND_ROBIN(2*N));
		    add(new FFT_optKernel2(N));
		    add(new FFT_optKernel2(N));
		    setJoiner(ROUND_ROBIN(2*N));
		}
	    });
	*/
	//add (new FloatPrinter());
        add(new FileWriter("output.dat", Float.TYPE));
    }
}

