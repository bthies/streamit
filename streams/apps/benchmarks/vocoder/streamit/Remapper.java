import streamit.*;
import streamit.io.*;

/** Linear Interpolater just takes two neighbouring points and creates
 * <interp - 1> points linearly between the two **/
class LinearInterpolator extends Filter {
  int interp;

  public void init(int interpFactor) {
    this.interp = interpFactor;
    input = new Channel(Float.TYPE, 1,2);
    output = new Channel(Float.TYPE, (int) interpFactor);
  }

  public void work() {
    float base = input.popFloat();
    float diff = input.peekFloat(0) - base;
    final int goal = interp;

    output.pushFloat(base);
    //already pushed 1, so just push another (interp - 1) floats
    for(int i = 1; i < goal; i++)
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
//      int c = gcd(oldLen, newLen);
//      int m = (int)newLen/c;
//      int n = (int)oldLen/c;
    int m = newLen;
    int n = oldLen;
    add(new LinearInterpolator(newLen));
    add(new Decimator(oldLen));
  }

  int gcd(int a, int b) {
    return (b == 0) ? a : gcd(b, a % b);
  }

  Remapper(int oldLen, int newLen) {
    super(oldLen, newLen);
  }
}

class Duplicator extends Filter {
  int oldLen, newLen;

  Duplicator(int oldLen, int newLen) {
    super(oldLen, newLen);
  }

  public void init(int oldLen, int newLen) {
    this.oldLen = oldLen;
    this.newLen = newLen;
    output = new Channel(Float.TYPE, newLen);
    input = new Channel(Float.TYPE, oldLen);
  }

  public void work() {
    if (newLen <= oldLen) {
      int i;
      for(i=0; i < newLen; i++)
	output.pushFloat(input.popFloat());
      for(i = newLen; i < oldLen; i++) {
	input.popFloat();
//  	output.pushFloat(0);
      }
    } else {
      float orig[] = new float[oldLen];
      for(int i=0; i < oldLen; i++)
	orig[i] = input.popFloat();
      for(int i=0; i < newLen; i++)
	output.pushFloat(orig[i%oldLen]);
    }
  }
}
    
