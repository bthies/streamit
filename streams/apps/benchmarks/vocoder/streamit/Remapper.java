import streamit.*;
import streamit.io.*;

/** Linear Interpolater just takes two neighbouring points and creates
 * <interp - 1> points linearly between the two **/
class LinearInterpolator extends Filter {
  int interp;

  public void init(int interp) {
    this.interp = interp;
    input = new Channel(Float.TYPE, 1,2);
    output = new Channel(Float.TYPE, interp);
  }

  public void work() {
    float base = input.popFloat();
    float diff = input.peekFloat(0) - base;
    final int goal = interp - 1;

    output.pushFloat(base);
    //already pushed 1, so just push another (interp - 1) floats
    for(int i = 0; i < goal; i++)
      output.pushFloat(base + ((float) i / interp) * diff);
  }

  LinearInterpolator(int interp) {
    super(interp);
  }
}
  
/** Linear Interpolater just takes two neighbouring points and creates
 * <interp - 1> points linearly between the two **/
class Decimator extends Filter {
  int decim;

  public void init(int decim) {
    this.decim = decim;
    input = new Channel(Float.TYPE, decim);
    output = new Channel(Float.TYPE, 1);
  }

  public void work() {
    output.pushFloat(input.popFloat());
    //already popped 1, so just pop another (interp - 1) floats
    for(int goal = decim - 1; goal > 0; goal--)
      input.popFloat();
  }

  Decimator(int decim) {
    super(decim);
  }
}

/** Remapper is a combination interpolator/decimator.  It's goal is to
 * map one stream from size n (oldLen) to size m (newLen).
 * 
 * To do this, it calculates [c = gcd(m,n)], interpolates linearly by
 * m/c, and then decimates by n/c.
**/
class Remapper extends Pipeline {

  public void init(int oldLen, int newLen) {
    int c = gcd(oldLen, newLen);
    int m = (int)newLen/c;
    int n = (int)oldLen/c;
    add(new LinearInterpolator(m));
    add(new Decimator(n));
  }

  int gcd(int a, int b) {
    return (b == 0) ? a : gcd(b, a % b);
  }

  Remapper(int oldLen, int newLen) {
    super(oldLen, newLen);
  }
}

