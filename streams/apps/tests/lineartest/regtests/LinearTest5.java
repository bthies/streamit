import streamit.library.*;

/**
 * Simple streamit program to test linear filters. This test
 * tests arrays.
 **/
public class LinearTest5 extends StreamIt {
    public static void main(String[] args) {
	StreamIt app = new LinearTest5();
	app.run(args);
    }
    public void init() {
	this.add(new Source());
	this.add(new FloatFilter1());
	this.add(new FloatFilter2());
	this.add(new FloatFilter3());
	this.add(new FloatFilter4());
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
 * Just make an array of weights in work function and use them to calcluate the output.
 **/
class FloatFilter1 extends Filter {
    public void init() {
	input = new Channel(Float.TYPE, 3);
	output = new Channel(Float.TYPE, 1);
    }
    public void work() {
	float[] weights = new float[4];
	// set up the weights
	weights[0] = (float)1.2;
	weights[1] = (float)4.9;
	weights[2] = (float)3.2;
	weights[3] = (float)4.2;
	    
	float t1 = input.popFloat();
	float t2 = input.popFloat();
	float t3 = input.popFloat();

	// now, push out weighted combos of t1,t2,t3 based
	// on the array weights.

	// col3 = [3.2 4.9 1.2]+[4.2]
	output.pushFloat(t1*weights[0] +
			 weights[1]*t2 +
			 t3*weights[2] +
			 weights[3]);
    }
}


/**
 * Store intermediate values in arrays this time, along with using the weights array
 **/
class FloatFilter2 extends Filter {
    public void init() {
	input = new Channel(Float.TYPE, 3);
	output = new Channel(Float.TYPE, 3);
    }
    public void work() {
	float[] weights = new float[4];
	// set up the weights
	weights[0] = 1;
	weights[1] = 2;
	weights[2] = 3;
	weights[3] = 4;
	    
	float t1 = input.popFloat();
	float t2 = input.popFloat();
	float t3 = input.popFloat();
	
	// calculate the output into the intermediate array and then
	// push its values
	float[] intermediate = new float[3];
	intermediate[0] = (t1 +
			   weights[0]*weights[1]*t2 +
			   weights[2]*t3*weights[3]);

	// intermediate[0] -> [12 2 1]+[0]

	intermediate[1] = intermediate[0];
	// intermediate[1] -> [12 2 1]+[0]
	intermediate[1] += intermediate[1];
	// intermediate[1] -> [24 4 2]+[0]
	intermediate[1] = 3 + intermediate[1];
	// intermediate[1] -> [24 4 2]+[3]

	intermediate[2] = t1 + t2 + 5*t3 + 7;
	// intermediate[2] -> [5 1 1 ]+[7]	
	intermediate[2] += intermediate[1] + intermediate[0] + t3;
	// intermediate[2] -> [42 7 4]+[10]

	// push out the values we just calculated
	// col3 = [12 2 1]+[0] 
	output.pushFloat(intermediate[0]);
	// col2 = [24 4 2]+[3]
	output.pushFloat(intermediate[1]);
	// col1 = [42 7 4]+[10]
	output.pushFloat(intermediate[2]);
    }
}


/** test out arrays with initializers **/
class FloatFilter3 extends Filter {
    public void init() {
	input = new Channel(Float.TYPE, 3);
	output = new Channel(Float.TYPE, 3);
    }
    public void work() {
	float[] weights = new float[4];
	for (int i=0; i<4; i++) {
	    weights[i] = i+1;
	}
	float t1 = input.popFloat();
	float t2 = input.popFloat();
	float t3 = input.popFloat();

	// col3 = [4 3 2]+[1]
	output.pushFloat(t3*weights[3] +
			 t2*weights[2] +
			 t1*weights[1] +
			 weights[0]);

	// make sure that the semantics of arrays starting with all zeros
	// in maintained.
	float[] zeroarray = new float[2];
	zeroarray[0] = 0;
	t3 = zeroarray[0];
	// col2 = [0 1 1]+[0]
	output.pushFloat(t1+t2+t3);

	// make sure, for that matter, that normal variables are initialized to zero
	//float newvar; // you can't acutally do this because the JLS says that you have to
	// initialize all variables before use.
	float newvar=0;
	t2 = newvar;
	// col1 = [0 0 1]+[0]
	output.pushFloat(t1+t2+t3);
    }
}



/**
 * Test a simple FIR type filter with weights that are
 * calculated both in init and weights that are calculated
 * in the actual work function;
 **/
class FloatFilter4 extends Filter {
    final int SIZE = 5;
    float[] weightField = new float[SIZE];
    
    public void init() {
	input = new Channel(Float.TYPE, SIZE);
	output = new Channel(Float.TYPE, 2);
	for (int i=0; i<SIZE; i++) {
	    weightField[i] = i*2;
	}
    }
    public void work() {
	float[] weights;

	// first, use the weights calculated in the init function
	float sum=0;
	for (int i=0; i<SIZE; i++) {
	    sum += input.peekFloat(i)*weightField[i];
	}

	// col2=[8 6 4 2 0]+0;
	output.pushFloat(sum);

	// now calculate weights in the work function
	weights = new float[SIZE];
	for (int i=0; i<SIZE; i++) {
	    weights[i] = i+3;
	}

	// calculate the sum
	sum = 0;
	for (int i=0; i<SIZE; i++) {
	    sum += weights[i]*input.peekFloat(i);
	}
	
	// col1 = [7 6 5 4 3]+0
	output.pushFloat(sum);

	
	// finally, pop the appropriate number from the buffer
	for (int i=0; i<SIZE; i++) {
	    input.popFloat();
	}

	
    }
}
