import streamit.library.*;

/**
 * Simple streamit application that tests using arrays of floats as intermediaries
 * in a calculation. This is necessary to recognize the maximum number of
 * filters as linear filters.
 **/
public class LinearTest8 extends StreamIt {
    public static void main(String[] args) {
	StreamIt app = new LinearTest8();
	app.run(args);
    }
    public void init() {
	this.add(new Source());
	this.add(new FloatIdentity());
	this.add(new FloatFilter1());
	this.add(new FloatFilter2());
	this.add(new CombineDFTNew(2));
	this.add(new CombineDFTNew(4));
	this.add(new CombineDFTNew(8));
	this.add(new CombineDFTOriginal(2));
	this.add(new CombineDFTOriginal(4));
	this.add(new CombineDFTOriginal(8));
	this.add(new ZeroArrayTest());
	this.add(new Sink());
    }
}


/**
 * Single, monotonically increasing source of floating point numbers.
 **/
class Source extends Filter {
    float INCREMENT = (float).01;
    float currentValue = 0;
    public void init() {
	output = new Channel(Float.TYPE, 1);
	this.currentValue = 0;
    }
    public void work() {
	output.pushFloat(this.currentValue);
	this.currentValue += INCREMENT;
    }
}

/**
 * Simple float sink that prints what it gets.
 **/
class Sink extends Filter {
    public void init() {
	input = new Channel(Float.TYPE, 1);
    }
    public void work() {
	System.out.println(input.popFloat());
    }
}

/**
 * About the simplest filter you can get that computes a linear function
 **/
class FloatIdentity extends Filter {
    public void init() {
	input = new Channel(Float.TYPE, 1);
	output = new Channel(Float.TYPE, 1);
    }
    public void work() {
	output.pushFloat(input.popFloat());
    }
}


/**
 * Compute some values into an array and then push them onto
 * the output tape.
 **/
class FloatFilter1 extends Filter {
    public void init() {
	input = new Channel(Float.TYPE, 3);
	output = new Channel(Float.TYPE, 3);

    }
    public void work() {
	float f1 = input.popFloat();
	float f2 = input.popFloat();
	float f3 = input.popFloat();

	float[] results = new float[3];
	results[0] = f1;
	results[1] = 2*f2;
	results[2] = 3*f3 + 4;

	// col3 = [0 0 1]+[0]
	output.pushFloat(results[0]);
	// col2 = [0 2 0]+[0]
	output.pushFloat(results[1]);
	// col1 = [3 0 0]+[4]
	output.pushFloat(results[2]);
    }
}


/**
 * Compute some values into an array and then push them onto
 * the output tape.
 **/
class FloatFilter2 extends Filter {
    final int SIZE = 3;
    public void init() {
	input = new Channel(Float.TYPE, SIZE);
	output = new Channel(Float.TYPE, SIZE);

    }
    public void work() {
	float f1 = input.peekFloat(0);
	float f2 = input.peekFloat(1);
	float f3 = input.peekFloat(2);

	float[] results = new float[SIZE];
	for (int i=0; i<SIZE; i++) {
	    results[i] = f1*i + f2*(i+1) + (i+2)*f3;
	}

	for (int i=0; i<SIZE; i++) {
	    output.pushFloat(results[i]);
	}
	for (int i=0; i<SIZE; i++) {
	    input.popFloat();
	}
    }
}


/**
 * This is a modified version of the Combiner class that lets me play around with
 * what exactly it does, and what exactly is preventing the array accesses
 * from being destructed.
 **/
class CombineDFTNew extends Filter
{
    CombineDFTNew(int i)
    {
        super(i);
    }
    float wn_r, wn_i;
    int nWay;
    public void init(int n)
    {
        nWay = n;
        input = new Channel(Float.TYPE, 2 * n);
        output = new Channel(Float.TYPE, 2 * n);
	wn_r = (float) Math.cos(2 * 3.141592654 / ((double) n));
        wn_i = (float) Math.sin(2 * 3.141592654 / ((double) n));
    }

    public void work()
    {
        int i;
        float w_r = 1;
        float w_i = 0;

	// my assigning the nway local 
	int localNWay = nWay;
	
	float results[];
        results = new float[2 * localNWay];
	
        for (i = 0; i < localNWay; i += 2)
        {
            float y0_r = input.peekFloat(i);
            float y0_i = input.peekFloat(i+1);
            float y1_r = input.peekFloat(localNWay + i);
            float y1_i = input.peekFloat(localNWay + i + 1);

            float y1w_r = y1_r * w_r - y1_i * w_i;
            float y1w_i = y1_r * w_i + y1_i * w_r;

            results[i] = y0_r + y1w_r;
            results[i + 1] = y0_i + y1w_i;

            results[localNWay + i] = y0_r - y1w_r;
            results[localNWay + i + 1] = y0_i - y1w_i;

            float w_r_next = w_r * wn_r - w_i * wn_i;
            float w_i_next = w_r * wn_i + w_i * wn_r;
            w_r = w_r_next;
            w_i = w_i_next;
        }

        for (i = 0; i < 2 * localNWay; i++)
        {
            input.popFloat ();
            output.pushFloat(results[i]);
        }
    }
}




/**
 * This class came directly out of the FFT implementation that we
 * have. It computes a bunch of linear stuff into an array that can
 * not be deconstructed by contprop/fieldprop and thus was
 * totally ignored by the linear analysis phase.
 **/
class CombineDFTOriginal extends Filter
{
    CombineDFTOriginal(int i)
    {
        super(i);
    }
    float wn_r, wn_i;
    int nWay;
    float results[];
    public void init(int n)
    {
        nWay = n;
        input = new Channel(Float.TYPE, 2 * n);
        output = new Channel(Float.TYPE, 2 * n);
	wn_r = (float) Math.cos(2 * 3.141592654 / ((double) n));
        wn_i = (float) Math.sin(2 * 3.141592654 / ((double) n));
        results = new float[2 * n];
    }

    public void work()
    {

        int i;
	int localNWay = nWay;
        float w_r = 1;
        float w_i = 0;
        for (i = 0; i < localNWay; i += 2)
        {
            float y0_r = input.peekFloat(i);
            float y0_i = input.peekFloat(i+1);
            float y1_r = input.peekFloat(localNWay + i);
            float y1_i = input.peekFloat(localNWay + i + 1);

            float y1w_r = y1_r * w_r - y1_i * w_i;
            float y1w_i = y1_r * w_i + y1_i * w_r;

            results[i] = y0_r + y1w_r;
            results[i + 1] = y0_i + y1w_i;

            results[localNWay + i] = y0_r - y1w_r;
            results[localNWay + i + 1] = y0_i - y1w_i;

            float w_r_next = w_r * wn_r - w_i * wn_i;
            float w_i_next = w_r * wn_i + w_i * wn_r;
            w_r = w_r_next;
            w_i = w_i_next;
        }

        for (i = 0; i < 2 * localNWay; i++)
        {
            input.popFloat ();
            output.pushFloat(results[i]);
        }
    }
}

class ZeroArrayTest extends Filter {
    public void init() {
	input  = new Channel(Float.TYPE, 1);
	output = new Channel(Float.TYPE, 1);
    }
    public void work() {
	float[] arr = new float[5];
	for (int i = 0; i < 5; i++) 
	    arr[i] = 0.0f;
	
	float sum = 0;
	for (int i=0 ;i<5; i++) {
	    sum += arr[i];
	}
	output.pushFloat(sum);
	input.popFloat();
    }
}


