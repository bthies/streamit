import streamit.library.*;

/**
 * Test the linear filter analyzer when filters use fields.
 * And when structures are used for holding data.
 **/
public class LinearTest4 extends StreamIt {
    public static void main(String[] args) {
	StreamIt app = new LinearTest4();
	app.run(args);
    }
    public void init() {
	this.add(new Source());
	this.add(new FloatIdentity());
	this.add(new FloatFilter1());
	this.add(new FloatFilter2());
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
 * test linearity with a filter that uses fields.
 **/
class FloatFilter1 extends Filter {
    // fields used to calculate output
    float f1;
    float f2;

    
    public void init() {
	input = new Channel(Float.TYPE, 3);
	output = new Channel(Float.TYPE, 3);
    }
    public void work() {
	// f1 --> [0,1,1]+[0]
	f1 = input.popFloat() + input.popFloat();
	// f2 --> [1,0,0]
	f2 = input.popFloat();

	// col 3 = [0,0,0]+[0]
	output.pushFloat(0 );
	// col 2 = [1,1,1]+[0]
	output.pushFloat(f1 + f2);

	float t1 = 3*f1 - 2*f2;
	// col 1 = [-2,3,3]+[0]
	output.pushFloat(t1);
    }
}


/**
 * test linearity with a filter that uses fields from another class
 **/
class ExternalClass extends Structure{
    float data;
    float data2;
}
class FloatFilter2 extends Filter {
    
    public void init() {
	input = new Channel(Float.TYPE, 3);
	output = new Channel(Float.TYPE, 3);
    }
    public void work() {
	float t1 = input.popFloat();
	float t2 = input.popFloat();
	float t3 = input.popFloat();

	ExternalClass ec = new ExternalClass();
	ec.data = t1 + t2 + t3;

	// col3 = [1 1 1]
	output.pushFloat(ec.data);
	// col2 = [2 2 3]
	output.pushFloat(t1 + ec.data + ec.data);

	ec.data = 5-2;
	ec.data = ec.data + t3; // ec.data -> [1 0 0]+[3]
	ec.data += 5*t2; // ec.data -> [1 5 0]+[3]
	ec.data *= 7; // ec.data -> [7 35 0]+[21]

	ec.data2 = 1*t1 + 2*t2 + t3*3; // ec.data2 -> [3 2 1]+[0]

	// col1 = [10 37 1]+[21]
	output.pushFloat(ec.data + ec.data2);
    }
}
