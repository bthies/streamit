/** Simple StreamIT program for testing purposes **/
import streamit.library.*;

class FieldInit2 extends StreamIt {
  static public void main (String [] t)
    {
        FieldInit2 test = new FieldInit2 ();
        test.run (t);
    }

    public void init ()
    {
	add (new DataSource2());
	add (new DataSink2());
    }
}

class DataSource2 extends Filter {
    int x;
    int[] FOO = new int[5];
    public void init() {
	// pushes 1 item onto the tape each iteration
	int i; i = 5;
	output = new Channel(Integer.TYPE, 1); 
	this.x = 0;
    }
    public void work() {
	output.pushInt(this.x + FOO[0]);
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
