import streamit.library.*;

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

	// should result in 2nd col: [3,2,10] + 7
	output.pushFloat(1 +
			 2*input.peekFloat(1) +
			 input.peekFloat(2)*3 +
			 2*3 +
			 2*input.peekFloat(0)*5);

	// should result in 1st col: [0,0,0] + -11	
	output.pushFloat(-11);

	// pop off the values from the input tape
	input.popFloat();
	input.popFloat();
	input.popFloat();
    }
}

/**
 * Use this class to make sure that peeks work correctly
 * when interspersed with pops.
 **/
class FloatFilter2 extends Filter {
    public void init() {
	input = new Channel(Float.TYPE, 4);
	output = new Channel(Float.TYPE, 3);
    }
    public void work() {
	// ok, first make a nice, normal column with peek expressions
	// result in col 3 being: [8, 5, 6, 12] + 3
	output.pushFloat(input.peekFloat(0) * 2 * 1 * 3 * 1 * 2 +
			 3 * 1 * 1 * 2 * input.peekFloat(1) +
			 5 * input.peekFloat(2) +
			 input.peekFloat(3) * 8 +
			 2+4+2+1-3-3);

	// pop off a result
	input.popFloat();

	// now, push using updated peek values and ensure that they are correct
	// result in col 2 being [4 3 2 0] + 7
	output.pushFloat(input.peekFloat(0) * 2 +
			 input.peekFloat(1) * 3 +
			 input.peekFloat(2) * 4 +
			 8-1);

	// pop again and figure out what is going on
	input.popFloat();

	// col 1 = [1 1 0 0] + 2
	output.pushFloat(input.peekFloat(0) +
			 input.peekFloat(1) +
			 2);

	// pop off the last 2 items
	input.popFloat();
	input.popFloat();
    }
}

/**
 * Use this class to test division
 **/
class FloatFilter3 extends Filter {
    public void init() {
	input = new Channel(Float.TYPE, 4);
	output = new Channel(Float.TYPE, 1);
    }
    public void work() {
	// col 1 = [1/5, 2/3, 11, 3/4] + 21/32
	output.pushFloat(input.peekFloat(3) / 5 +
			 (2*input.peekFloat(2)) / 3 +
			 11 * input.peekFloat(1) +
			 (3 * input.peekFloat(0)) / (2 * 2 * 2 / 2) +
			 ((float)21)/((float)32));
	input.popFloat();
	input.popFloat();
	input.popFloat();
	input.popFloat();
    }
}


/**
 * Use this one to make sure that we don't screw and do bad things with division
 **/
class FloatFilter4 extends Filter {
    public void init() {
	input = new Channel(Float.TYPE, 1);
	output = new Channel(Float.TYPE, 1);
    }
    public void work() {
	// non linear
	output.pushFloat(1/input.peekFloat(0));
	input.popFloat();
    }
}


