import streamit.library.*;

/**
 * Simple streamit program to test doing confluence operators in if statements
 **/
public class LinearTest6 extends StreamIt {
    public static void main(String[] args) {
	StreamIt app = new LinearTest6();
	app.run(args);
    }
    public void init() {
	this.add(new Source());
	this.add(new FloatIdentity());
	this.add(new NonLinearFilter());
	this.add(new NonLinearFilter2());
	this.add(new NonLinearFilter3());
	this.add(new LinearFilter1());
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
 * About the simplest filter you can get that computes a linear function,
 * with the added benefit of containing an if statement which basically does nothing.
 **/
class FloatIdentity extends Filter {
    public void init() {
	input = new Channel(Float.TYPE, 1);
	output = new Channel(Float.TYPE, 1);
    }
    public void work() {
	float t = input.popFloat();
	boolean test = t < 2;

	if (t<5) {
	    output.pushFloat(t);
	} else {
	    output.pushFloat(t);
	}
    }
}

/**
 * This is a non linear filter.
 * the different branches compute different functions
 **/
class NonLinearFilter extends Filter {
    public void init() {
	input = new Channel(Float.TYPE, 3);
	output = new Channel(Float.TYPE, 1);
    }
    public void work() {
	float t1 = input.popFloat();
	float t2 = input.popFloat();
	float t3 = input.popFloat();
	
	if (t1 > 5) {
	    output.pushFloat(t1+t2);
	} else {
	    output.pushFloat(t3);
	}
    }
}

/**
 * This is also non linear.
 * The different branches have different mappings
 **/
class NonLinearFilter2 extends Filter {
    public void init() {
	input = new Channel(Float.TYPE, 3);
	output = new Channel(Float.TYPE, 1);
    }
    public void work() {
	float t1 = input.popFloat();
	float t2 = input.popFloat();
	float t3 = input.popFloat();

	float t4;

	if (t1 > 5) {
	    t4 = t1;
	} else {
	    t4 = t2;
	}
	output.pushFloat(t4);
    }
}

/**
 * This is also non linear.
 * The different branches have different push counts
 **/
class NonLinearFilter3 extends Filter {
    public void init() {
	input = new Channel(Float.TYPE, 3);
	output = new Channel(Float.TYPE, 3);
    }
    public void work() {
	float t1 = input.popFloat();
	float t2 = input.popFloat();
	float t3 = input.popFloat();



	if (t1 > 5) {
	    output.pushFloat(t1);
	} else {
	    output.pushFloat(t2);
	    output.pushFloat(t3);
	}

	if (t1 <= 5) {
	    output.pushFloat(t1*2);
	} else {
	    output.pushFloat(t2*2);
	    output.pushFloat(t3*2);
	}

    }
}

/**
 * This is Linear!
 **/
class LinearFilter1 extends Filter {
    public void init() {
	input = new Channel(Float.TYPE, 3);
	output = new Channel(Float.TYPE, 1);
    }
    public void work() {
	float t1 = input.popFloat();
	float t2 = input.popFloat();
	float t3 = input.popFloat();

	float t4;

	if (t3 > 4) {
	    t4 = t1+t2+t3;
	} else {
	    t4 = t3+t2+t1;
	}
	// col1 = [1 1 1]
	output.pushFloat(t4);

    }
}
