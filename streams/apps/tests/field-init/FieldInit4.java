/**
 * Simple StreamIT program with automatic initialization
 * of field members (these are moved to the init function by the compiler);
 * This file tests that the field declaration and initializations
 * in PipeLines, Splitters and Joiners, and RoundRobin work correctly
 **/
import streamit.library.*;

class FieldInit4 extends StreamIt {
  static public void main (String [] t)
    {
        FieldInit4 test = new FieldInit4 ();
        test.run (t);
    }
    public void init ()
    {
	add(new SimplePipe());
    }
}

class SimplePipe extends Pipeline {
    public void init() {
	final int offset = 7;
	final int x = 3 + offset;
	add (new DataSource(x));
	add (new DataSink());
    }
}

class DataSource extends Filter {
    int x = 5;

    public DataSource(int num) {
	super(num);

    }
    
    public void init(int num) {
	// pushes 1 item onto the tape each iteration
	output = new Channel(Integer.TYPE, 1); 
	this.x += num;
    }

    public void work() {
	output.pushInt(this.x);
	this.x++;
    }
}

class DataSink extends Filter {
    public void init() {
	// pops 1 item from the tape each iteration
	input = new Channel(Integer.TYPE, 1); 
    }

    public void work() {
	System.out.println(input.popInt());
    }
}
