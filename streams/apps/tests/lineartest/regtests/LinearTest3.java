import streamit.library.*;

/**
 * Simple StreamIt application that contains some linear filters
 * that we can hopefully identify with the linear analysis pass
 * that I am going to be writing.
 **/
public class LinearTest3 extends StreamIt {
    public static void main(String[] args) {
	StreamIt app = new LinearTest3();
	app.run(args);
    }
    public void init() {
	this.add(new Source());
	this.add(new FloatIdentity());
	this.add(new FloatFilter1());
	this.add(new FloatFilter2());
	this.add(new FloatFilter3());
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
 * Test out very simple work function involving variables that take on
 * linear form.
 **/
class FloatFilter1 extends Filter {
    public void init() {
	input = new Channel(Float.TYPE, 3);
	output = new Channel(Float.TYPE, 3);
    }
    public void work() {
	float t1;
	t1= 5;

	// should generate 3rd col=[0,0,0] + [5]
	output.pushFloat(t1);

	float t2 = input.popFloat();
	float t3 = input.popFloat();
	float t4 = input.popFloat();

	t1 = (float)7.34;
	
	// should generate 2nd col=[6,5,1] + [7.34]
	output.pushFloat(t2 +
			 5 * t3 +
			 t4 * 6 + t1);

	// and for my final trick, do some computation.
	float t5 = 2 * t2;
	t5 = t5 + 3*t3;
	t5 += t5;

	// should generate 1st col=[1,6,4] + [0]
	output.pushFloat(t5+t4);
	
    }
}





/**
 * Test linearity with compound assignment expressions
 **/
class FloatFilter2 extends Filter {
    
    public void init() {
	input = new Channel(Float.TYPE, 4);
	output = new Channel(Float.TYPE, 3);
    }
    public void work() {
	float t1 = input.popFloat();
	float t2 = input.popFloat();
	float t3 = input.popFloat();
	float t4 = input.popFloat();

	// build up various combinations using compound assignment operators
	float t5 = 0;
	t5 += t1; // t5 --> [0 0 0 1]
	t5 += t5; // t5 --> [0 0 0 2]
	t5 += t5; // t5 --> [0 0 0 4]
	t5 += (t2 + t3); // t5 --> [0 1 1 4]
	t5 += t2;   // t5 --> [0 1 2 4]
	t5 += 5*t4; // t5 --> [5 1 2 4]

	float t6 = t1 + t2 + t4 + -12; // t6 --> [1 0 1 1]+[-12]
	t6 += 5; // t6 --> [1 0 1 1]+[-7]
	t6 += t6;// t6 --> [2 0 2 2]+[-14]
	t6 -= 2; // t6 --> [2 0 2 2]+[-16]
	t6 *= 3; // t6 --> [6 0 6 6]+[-48]
	t6 /= 2; // t6 --> [3 0 3 3]+[-24]

	float t7 = 1*t1 + 2*t2 + (1+2)*t3 + t4*(1+1+1+1) + 5; // t7 --> [4 3 2 1]+[5]
	t7 -= 3*t4 + t3; // t7 --> [1 2 2 1]+[5]
	t7 -= 5; // t7 --> [1 2 2 1]+[0]
	t7 += 3; // t7 --> [1 2 2 1]+[3]

	// col 3 = [5 1 2 4] + [0]
	output.pushFloat(t5);

	// col 2 = [3 0 3 3] + [-24]
	output.pushFloat(t6);

	// col 1 = [1 2 2 1] + [3]
	output.pushFloat(t7);
	

	
    }
}

/**
 * This filter is non linear -- make sure that the assignments are unmapped
 **/
class FloatFilter3 extends Filter {
    public void init() {
	input = new Channel(Float.TYPE, 3);
	output = new Channel(Float.TYPE, 1);
    }
    public void work() {
	float t1 = input.popFloat();
	float t2 = input.popFloat();
	float t3 = input.popFloat();
	float t4 = 3*t1 - 2*t2 + t3*32;

	t4 = t4 / t1;
	output.pushFloat(t4);
    }
}


