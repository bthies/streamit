/**
 * Simple application used for regression testing of
 * field propagation.
 * $Id: FieldPropTest.java,v 1.3 2003-09-29 09:07:47 thies Exp $
 **/
import streamit.library.*;

class FieldPropTest extends StreamIt {
  static public void main (String [] t)
  {
    FieldPropTest test = new FieldPropTest ();
    test.run (t);
  }
  
  public void init ()
  {
    add (new DataSource());
    add (new OneToOne());
    add (new OneToTwo());
    add (new DataSink());
  }
}

class DataSource extends Filter {
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
  static final int F = 5;
  public void init() {
    input = new Channel(Integer.TYPE, 1);
    output= new Channel(Integer.TYPE, 1);
  }
  
  public void work() {
    output.pushInt(input.popInt());
  }
}


class OneToTwo extends Filter {
  int G;
  // sticking in something like int G = 5; doesn't work correctly;
  public void init() {
    G = 6;
    input = new Channel(Integer.TYPE, 1);
    output= new Channel(Integer.TYPE, 2);
  }
  
  public void work() {
    int temp_value = input.popInt();
    output.pushInt(temp_value + OneToOne.F);
    output.pushInt(temp_value + G);
  }
}
