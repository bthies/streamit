import streamit.*;
import streamit.io.*;

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
    if (first) {
      output.pushFloat(input.popFloat());
      first = false;
    }
    else {
      output.pushFloat(input.popFloat() * c);
    }
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
