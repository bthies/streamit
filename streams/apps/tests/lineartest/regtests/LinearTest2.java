import streamit.*;

/**
 * Simple StreamIt application that contains some linear filters
 * that we can hopefully identify with the linear analysis pass
 * that I am going to be writing.
 **/
public class LinearTest2 extends StreamIt {
    public static void main(String[] args) {
	StreamIt app = new LinearTest2();
	app.run(args);
    }
    public void init() {
	this.add(new Source());
	this.add(new FloatIdentity());
	this.add(new FloatFilter());
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
 * This is the filter which contains the work function that
 * I used to test the linear analysis.
 **/
class FloatFilter extends Filter {
    public void init() {
	input = new Channel(Float.TYPE, 3);
	output = new Channel(Float.TYPE, 3);
    }
    public void work() {
	// should result in 3rd col: [1,1,1] + 0
	output.pushFloat(input.peekFloat(0) +
			 input.peekFloat(1) +
			 input.peekFloat(2));

	// should result in 2nd col: [3,2,10] + 6
	output.pushFloat(1 +
			 2*input.peekFloat(1) +
			 input.peekFloat(2)*3 +
			 5 +
			 2*input.peekFloat(0)*5);

	// should result in 1st col: [0,0,0] + -11	
	output.pushFloat(-11);

	// pop off the values from the input tape
	input.popFloat();
	input.popFloat();
	input.popFloat();
    }
}
