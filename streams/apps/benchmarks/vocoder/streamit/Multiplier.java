import streamit.library.*;
import streamit.library.io.*;

class Multiplier extends Filter {
  public void init() {
    input = new Channel(Float.TYPE, 2);
    output = new Channel(Float.TYPE, 1);
  }

  public void work() {
    output.pushFloat(input.popFloat() * input.popFloat());
  }
}

class ConstMultiplier extends Filter {
  float c;
  boolean first = true;

  public void init(float mult) {
    this.c = mult;
    input = new Channel(Float.TYPE, 1);
    output = new Channel(Float.TYPE, 1);
  }

  public void work() {
    //you are within the work function of doubler
//      if (first) {
//        output.pushFloat(input.popFloat());
//        first = false;
//      }
//      else {
      output.pushFloat(input.popFloat() * c);
//      }
  }

  ConstMultiplier(float c) {
    super(c);
  }
}

class Accumulator extends Filter {
  float val = 0;
  public Accumulator() {}
  public void init() {
    input = new Channel(Float.TYPE, 1);
    output = new Channel(Float.TYPE, 1);
  }

  public void work() {
    val += input.popFloat();
    output.pushFloat(val);
  }
}

class Doubler extends Filter {
  public Doubler() {}
  public void init() {
    input = new Channel(Float.TYPE, 1);
    output = new Channel(Float.TYPE, 1);
  }

  public void work() {
    //you are within the work function of doubler
    output.pushFloat(input.peekFloat(0) + input.peekFloat(0));
    input.popFloat();
  }
}

class Summation extends Pipeline {
  public Summation(int length) {super(length);}
  public void init(final int length) {
    if (length == 1) {
        add(new Identity(Float.TYPE));
    } else {
      add(new SplitJoin() {
	  public void init() {
	    setSplitter(ROUND_ROBIN());
	    add(new Summation((length+1)/2));
	    add(new Summation(length/2));
	    setJoiner(ROUND_ROBIN());
	  }
	});
      add(new Adder(2));
    }
  }

}

class Adder extends Filter {
  int N;
  public Adder(int length) {
    super(length);
  }
  public void init(final int length) {
    N = length;
    input = new Channel(Float.TYPE, length);
    output = new Channel(Float.TYPE, 1);
  }
  public void work() {
    float val = 0;
    for(int i=0; i < N; i++) 
      val += input.popFloat();
    output.pushFloat(val);
  }
}

class Subtractor extends Filter {
  public void init() {
    input = new Channel(Float.TYPE, 2);
    output = new Channel(Float.TYPE, 1);
  }

  public void work() {
    output.pushFloat(input.peekFloat(0) - input.peekFloat(1));
    input.popFloat();input.popFloat();
  }
}
