import streamit.*;
import streamit.io.*;

class FIRSmoothingFilter extends Filter {
  final float cosWin[] = { 0.1951f, 0.3827f, 0.5556f, 0.7071f, 0.8315f, 
			   0.9239f, 0.9808f, 1, 0.9808f, 0.9239f, 0.8315f,
			   0.7071f, 0.5556f, 0.3827f, 0.1951f};
  int DFTLen;

  public void init(int DFTLen) {
    this.DFTLen = DFTLen;
    input = new Channel(Float.TYPE, DFTLen);
    output = new Channel(Float.TYPE, DFTLen);
  }

  public void work() {
    final int offset = (int) (cosWin.length / 2);

    //y[n]=x[n]*h[n] = /sum (x[n-k])(h[k])
    //note that h[k] = h[i + off]
    for(int n=0; n < DFTLen; n++) {
      float y = 0;
      for(int k = 0; k < cosWin.length; k++) {
	int i = k - offset; //so that when i = 0, k will be at the center
	if (((n - i) >= 0) && ((n - i) < DFTLen))
	  y += input.peekFloat(n-i) * cosWin[k];
      }
      output.pushFloat(y);
    }
      
    for(int i=0; i < DFTLen; i++) 
      input.popFloat();
  }

  FIRSmoothingFilter(int DFTLen) {
    super(DFTLen);
  }
}

class Deconvolve extends Filter {
  public void init() {
    input = new Channel(Float.TYPE, 2);
    output = new Channel(Float.TYPE, 2);
  }

  public void work() {
    float den = input.popFloat();
    float num = input.popFloat();
    output.pushFloat(num / den);
    output.pushFloat(den);
  }
}

