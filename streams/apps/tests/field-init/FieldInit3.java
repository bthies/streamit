/**
 * Simple StreamIT program with automatic initialization
 * of field members (these are moved to the init function by the compiler);
 **/
import streamit.library.*;

class FieldInit3 extends StreamIt {
  static public void main (String [] t)
    {
        FieldInit3 test = new FieldInit3 ();
        test.run (t);
    }
    public void init ()
    {
	add(new SimplePipe());
    }
}

class SimplePipe extends Pipeline {
    public void init() {
	add (new DataSource(1));
	add (new DataSink());
    }
}

class DataSource extends Filter {
    int x;

    public DataSource(int num) {
	super(num);

    }
    
    public void init(int num) {
	// pushes 1 item onto the tape each iteration
	output = new Channel(Integer.TYPE, 1); 
	this.x = num;
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
