/**
 * Simple StreamIT program with automatic initialization
 * of field members (these are moved to the init function by the compiler);
 **/
import streamit.library.*;

class FieldInit extends StreamIt {
  static public void main (String [] t)
    {
        FieldInit test = new FieldInit ();
        test.run (t);
    }
    public void init ()
    {
	add (new DataSource());
	//add (new OneToOne());
	//add (new OneToTwo());
	add (new DataSink());
    }
}

class DataSource extends Filter {
    int x;
    int FOO = 5;
    int BAR[] = new int[FOO];
    
    public void init() {
	// pushes 1 item onto the tape each iteration
	int i = 5;
	BAR[3] = 2;
	output = new Channel(Integer.TYPE, 1); 
	this.x = 0;
    }
    public void work() {
	output.pushInt(this.x + BAR[3]);
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







class OneToOne extends Filter {
    public void init() {
	input = new Channel(Integer.TYPE, 1);
	output= new Channel(Integer.TYPE, 1);
    }

    public void work() {
	output.pushInt(input.popInt());
    }
}


class OneToTwo extends Filter {
  public void init() {
    input = new Channel(Integer.TYPE, 1);
    output= new Channel(Integer.TYPE, 2);
  }
  
  public void work() {
    int temp_value = input.popInt();
    output.pushInt(temp_value);
    output.pushInt(temp_value);
  }
}
