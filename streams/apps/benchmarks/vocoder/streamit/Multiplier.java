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

  public void init(float c) {
    this.c = c;
    input = new Channel(Float.TYPE, 1);
    output = new Channel(Float.TYPE, 1);
  }

  public void work() {
    output.pushFloat(input.popFloat() * c);
  }

  ConstMultiplier(float c) {
    super(c);
  }
}

