import streamit.library.*;

/**
 * Simple StreamIt application that contains some linear filters
 * that we can hopefully identify with the linear analysis pass
 * that I am going to be writing.
 **/
public class LinearTest7 extends StreamIt {
    public static void main(String[] args) {
	StreamIt app = new LinearTest7();
	app.run(args);
    }
    public void init() {
	this.add(new Source());
	this.add(new FloatToInt());
	this.add(new IntFilter0());
	this.add(new IntFilter1());
	this.add(new IntFilter2());
	this.add(new IntFilter3());
	this.add(new IntFilter4());
	this.add(new IntFilter5());
	this.add(new IntFilter6());	
	this.add(new IntToFloat());
	this.add(new Sink());
    }
}


/**
 * Single, monotonically increasing source of floating point numbers.
 **/
class Source extends Filter {
    float INCREMENT = (float).01;
    float currentValue = 0;
    float MAX = 1;
    public void init() {
	output = new Channel(Float.TYPE, 1);
	this.currentValue = 0;
    }
    public void work() {
	output.pushFloat(this.currentValue);
	this.currentValue += INCREMENT;
	if (this.currentValue >= MAX) {
	    this.currentValue = 0;
	}
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
 * Convert float to int
 **/
class FloatToInt extends Filter {
    public void init() {
	input = new Channel(Float.TYPE, 1);
	output = new Channel(Integer.TYPE, 1);
    }
    public void work() {
	output.pushInt((int)input.popFloat());
    }
}
/**
 * Convert int to float
 **/
class IntToFloat extends Filter {
    public void init() {
	input = new Channel(Integer.TYPE, 1);
	output = new Channel(Float.TYPE, 1);
    }
    public void work() {
	output.pushFloat(input.popInt());
    }
}



/**
 * test using multiplication
 **/
class IntFilter0 extends Filter {
    public void init() {
	input = new Channel(Integer.TYPE, 3);
	output = new Channel(Integer.TYPE, 3);
    }
    public void work() {
	int i1 = input.popInt();
	int i2 = input.popInt();
	int i3 = input.popInt();

	// col 3 = [0 0 2]+[0]
	output.pushInt(i1*2);
	// col 2 = [0 3 0]+[0]
	output.pushInt(3*i2);
	// col 1 = [4 0 0]+[0]
	output.pushInt(i3*2*2);
    }
}



/**
 * test using the bit shift operators
 **/
class IntFilter1 extends Filter {
    public void init() {
	input = new Channel(Integer.TYPE, 3);
	output = new Channel(Integer.TYPE, 2);
    }
    public void work() {
	int i1 = input.popInt();
	int i2 = input.popInt();
	int i3 = input.popInt();

	// col 2 = [4 3 2]+[3]
	output.pushInt(i1*2 + 3*i2 + 4*i3 + 3);
	// col 1 = [1 .25 2]+[80]
	output.pushInt((i1<<1) + (i2>>2) + ((i3<<3)>>3) + (5<<4));
	
    }
}


/**
 * test really simple integer filters to see if they are linear
 **/
class IntFilter2 extends Filter {
    public void init() {
	input = new Channel(Integer.TYPE, 3);
	output = new Channel(Integer.TYPE, 2);
    }
    public void work() {
	int i1 = input.popInt();
	int i2 = input.popInt();
	int i3 = input.popInt();

	// col2 = [9 6 30+[4]
	output.pushInt(i1*3 + i2*6 + 9*i3 + 4);
	// col1= [2 3 4]+[0]
	output.pushInt(i1 + i2 + i3 + i1 + i2 + i3 + i1 + i1 +i2);
	    
    }
}

/**
 * test simple shift operations.
 **/
class IntFilter3 extends Filter {
    public void init() {
	input = new Channel(Integer.TYPE, 3);
	output = new Channel(Integer.TYPE, 1);
    }
    public void work() {
	int i1 = input.popInt();
	int i2 = input.popInt();
	int i3 = input.popInt();
	
	// col1= [8 4 2]+[8]
	output.pushInt((i1<<1) + (i2<<2) + (i3<<3) + (1<<3));
	    
    }
}


/**
 * ___Really___ Simple shift expressions
 **/
class IntFilter4 extends Filter {
    public void init() {
	input = new Channel(Integer.TYPE, 3);
	output = new Channel(Integer.TYPE, 1);
    }
    public void work() {
	int i1 = input.popInt();
	int i2 = input.popInt();
	int i3 = input.popInt();
	
	// col1= [0 0 2]+[0]
	output.pushInt(i1<<1);
	    
    }
}

/**
 * Another simple shift expressions
 **/
class IntFilter5 extends Filter {
    public void init() {
	input = new Channel(Integer.TYPE, 3);
	output = new Channel(Integer.TYPE, 3);
    }
    public void work() {
	int i1 = input.popInt();
	int i2 = input.popInt();
	int i3 = input.popInt();
	
	// col3= [0 0 4]+[0]
	output.pushInt(i1<<2);

	// col2= [0 8 0]+[0]
	output.pushInt(i2<<3);

	// col2= [16 0 0]+[0]
	output.pushInt(i3<<4);

    }
}

/**
 * Simple shift expressions
 * Start combining the expressions
 **/
class IntFilter6 extends Filter {
    public void init() {
	input = new Channel(Integer.TYPE, 3);
	output = new Channel(Integer.TYPE, 3);
    }
    public void work() {
	int i1 = input.popInt();
	int i2 = input.popInt();
	int i3 = input.popInt();
	
	// col3= [0 4 4]+[0]
	output.pushInt((i1<<2) + (i2<<2));

	// col2= [0 8 0]+[0]
	output.pushInt(i2<<3);

	// col2= [16 0 0]+[0]
	output.pushInt(i3<<4);

    }
}


