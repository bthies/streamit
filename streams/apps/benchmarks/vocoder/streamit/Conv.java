import streamit.*;
import streamit.io.*;

class FIRSmoothingFilter extends Filter {
//    final float cosWin[] = { 0.25f, 0.5f, 0.75f, 1.0f, 0.75f, 0.5f, 0.25f};
  float cosWin[];/* = { 0.1951f, 0.3827f, 0.5556f, 0.7071f, 0.8315f, 
		      0.9239f, 0.9808f, 1, 0.9808f, 0.9239f, 0.8315f,
		      0.7071f, 0.5556f, 0.3827f, 0.1951f}; */
  //this should be optimized out
  final int cosWinLength = 15;
  int DFTLen;

  public void init(int DFTLen) {
    this.DFTLen = DFTLen;
    cosWin = new float[cosWinLength];
    cosWin[0] = 0.1951f; cosWin[1] = 0.3827f; cosWin[2] = 0.5556f;
    cosWin[3] = 0.7071f; cosWin[4] = 0.8315f; cosWin[5] = 0.9239f;
    cosWin[6] = 0.9808f; cosWin[7] = 1.0000f; cosWin[8] = 0.9808f;
    cosWin[9] = 0.9239f; cosWin[10] = 0.8315f; cosWin[11] = 0.7071f;
    cosWin[12] = 0.5556f; cosWin[13] = 0.3827f; cosWin[14] = 0.1951f;
    input = new Channel(Float.TYPE, DFTLen);
    output = new Channel(Float.TYPE, DFTLen);
  }

  public void work() {
    final int offset = (int) (cosWinLength / 2);

    //y[n]=x[n]*h[n] = /sum (x[n-k])(h[k])
    //note that h[k] = h[i + off]
    for(int n=0; n < DFTLen; n++) {
      float y = 0;
      for(int k = 0; k < cosWinLength; k++) {
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

class HanningWindow extends Filter {
  private int length;

  public HanningWindow(int DFTLen) {
    super(DFTLen);
  }
  public void init(int DFTLen) {
    this.length = DFTLen;
    input = new Channel(Float.TYPE, 2 * DFTLen);
    output = new Channel(Float.TYPE, 2 * DFTLen);
  }

  public void work() {
    //convolution with the series {-1/4, 1/2, -1/4}
    //first and last have to be dealt with specially
    /** Note that everything is doubled (real and imag) **/
    output.pushFloat(input.peekFloat(0)/2 - 
	    (input.peekFloat(2) + input.peekFloat(length * 2 - 2))/4f);
    output.pushFloat(input.peekFloat(1)/2 - 
	    (input.peekFloat(3) + input.peekFloat(length * 2 - 1))/4f);

    for(int i=1; i < length - 1; i++) {
      int n = i << 1;
      float real = input.peekFloat(n)/2f;
      real -= (input.peekFloat(n-2)+input.peekFloat(n+2))/4f;
      output.pushFloat(real);
      float imag = input.peekFloat(n+1)/2f;
      imag -= (input.peekFloat(n-1)+input.peekFloat(n+3))/4f;
      output.pushFloat(imag);
    }

    output.pushFloat(input.peekFloat(length * 2 - 2)/2f -
		(input.peekFloat(length * 2 - 4) + input.peekFloat(0))/4f);
    output.pushFloat(input.peekFloat(length * 2 - 1)/2f -
		(input.peekFloat(length * 2 - 3) + input.peekFloat(1))/4f);
    for(int i=0; i < length; i++) {
      input.popFloat(); input.popFloat();
    }
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
    output.pushFloat(den);
    if (den == 0)
      output.pushFloat(0f);
    else
      output.pushFloat(num / den);
  }
}

