import streamit.library.*;

/**
 * Outputs a value of 1 at position 1, and 0 at all other positions.
 * Should elicit a sinusoidal response from a complex FFT.
 */
class FFTTestSource extends Filter
{
    int N;

    public FFTTestSource (int N) {
	super(N);
    }

    public void init (int N)
    {
        output = new Channel(Float.TYPE, 2*N);
	this.N=N;
    }

    public void work()
    {
	int i;
	output.pushFloat(0.0f);
	output.pushFloat(0.0f);
	output.pushFloat(1.0f);
	output.pushFloat(0.0f);
	for (i=0; i<2*(N-2); i++) {
	    output.pushFloat(0.0f);
	}
    }
}
