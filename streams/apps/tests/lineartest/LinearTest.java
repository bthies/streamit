import streamit.*;

/**
 * Simple StreamIt application that contains some linear filters
 * that we can hopefully identify with the linear analysis pass
 * that I am going to be writing.
 **/
public class LinearTest extends StreamIt {
    public static void main(String[] args) {
	StreamIt app = new LinearTest();
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
    float INCREMENT = .01f;
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
	float t1 = input.popFloat();
	float t2 = input.popFloat();
	float t3 = input.popFloat();
	output.pushFloat(t1+t2+t3);
	output.pushFloat(0);
	output.pushFloat(0);
	
    }
}
