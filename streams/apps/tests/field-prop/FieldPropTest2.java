/**
 * Simple application used for regression testing of
 * field propagation. This one uses final fields that are not static
 * and assigns them values in the init function.
 * $Id: FieldPropTest2.java,v 1.2 2003-09-29 09:07:47 thies Exp $
 **/
import streamit.library.*;

class FieldPropTest2 extends StreamIt {
  static public void main (String [] t)
    {
        FieldPropTest2 test = new FieldPropTest2 ();
        test.run (t);
    }

    public void init ()
    {
	add (new DataSource2());
	add (new OneToOne2());
	add (new OneToTwo2());
	add (new DataSink2());
    }
}

class DataSource2 extends Filter {
    int x;
    public void init() {
	// pushes 1 item onto the tape each iteration
	output = new Channel(Integer.TYPE, 1); 
	this.x = 0;
    }
    public void work() {
	output.pushInt(this.x);
	this.x++;
    }
}

class DataSink2 extends Filter {
    public void init() {
	// pops 1 item from the tape each iteration
	input = new Channel(Integer.TYPE, 1); 
    }

    public void work() {
	System.out.println(input.popInt());
    }
}





class OneToOne2 extends Filter {
    static final int F = 7;
    public void init() {
	input = new Channel(Integer.TYPE, 1);
	output= new Channel(Integer.TYPE, 1);
    }

    public void work() {
	output.pushInt(input.popInt());
    }
}


class OneToTwo2 extends Filter {
    static final int G = 6;
    public void init() {
	input = new Channel(Integer.TYPE, 1);
	output= new Channel(Integer.TYPE, 2);
  }
  
  public void work() {
    int temp_value = input.popInt();
    output.pushInt(temp_value + OneToOne2.F);
    output.pushInt(temp_value + G);
  }
}

