import streamit.*;
import streamit.io.*;

class DFTFilter extends Filter
{
//the rate by which to deteriorate, assuring stability
  float deter; 
  //since the previous complex value is multiplied by the deter each
  //time, by the time the last time sample is windowed out it's
  //effect will have been multiplied by deter DFTLen times, hence it
  //needs to be multiplied by deter^DFTLen before being subtracted
  float detern;
  //  float o[];
  int DFTLen;
  float range;
  private boolean first = true;
  private float prevR, prevI;
  private float nextR, nextI;
  private float wR, wI; //represents w^(-k)
//    Complex prev;
//    Complex w; //represents w^(-k)

  public void work() {
    if (first) {
      first = false;
      //note: this w = w^k, not w^(-k)
      float wkR, wkI;
      wkR = (float)Math.cos(range); wkI = (float)Math.sin(range);
      float wkiR, wkiI; //this represents w^(k*i)
      float nwkiR, nwkiI;
      wkiR = 1f; wkiI = 0f;

      for (int i=0; i < DFTLen; i++) {
	float nextVal = (float) input.peekFloat(i);
      
	prevR = (prevR + wkiR * nextVal) * deter;
	prevI = (prevI + wkiI * nextVal) * deter;

	nwkiR = wkiR * wkR - wkiI * wkI;
	nwkiI = wkiR * wkI + wkiI * wkR;
	wkiR = nwkiR;
	wkiI = nwkiI;
	detern *= deter;
      }
      //      System.out.println("Range: " + range + "'s initial start is: "
      //    		       + prev.real() + " " + prev.imag() + "i");
//        computeFirstDFT();
    }
    float nextVal = (float) input.peekFloat(DFTLen);
    float current = (float) input.popFloat();

//      System.out.println(prevR);
//      System.out.println(prevI);
//      System.out.println(nextVal);
    prevR = prevR * deter + (nextVal - (detern * current));
//      prevR = prevR * deter + (nextVal - (float)(detern * current));
    prevI = prevI * deter;
//      System.out.println(prevR);
//      System.out.println(prevI);
//      System.out.println("");

//      System.out.println("Range: " + range + "'s initial start is: "
//    		       + prev.real() + " " + prev.imag() + "i");

    nextR = prevR * wR - prevI * wI;
    nextI = prevR * wI + prevI * wR;
    prevR = nextR; prevI = nextI;

    output.pushFloat(prevR);
    output.pushFloat(prevI);
  }

  public void init(int DFTLen, float range) {
    this.DFTLen = DFTLen;
    this.range = range;
    this.deter = 0.999999f;
    this.detern = 1;
    wR = (float)Math.cos(range);
    wI = (float)-Math.sin(range);
    prevR = 0; prevI = 0;

    //need to peek DFTLen ahead of current one
    input = new Channel(Float.TYPE, 1, DFTLen+1);
    output = new Channel(Float.TYPE, 2);
//      output = new Channel(Complex.TYPE, 1);
    //    computeFirstDFT();
  }

  /**
  public void computeFirstDFT() {
    //note: this w = w^k, not w^(-k)
    float wkR, wkI;
    wkR = (float)Math.cos(range); wkI = (float)Math.sin(range);
    float wkiR, wkiI; //this represents w^(k*i)
    wkiR = 1f; wkiI = 0f;

    for (int i=0; i < DFTLen; i++) {
      float nextVal = (float) input.peekFloat(i);

      prevR = (prevR + wkiR * nextVal) * deter;
      prevI = (prevI + wkiI * nextVal) * deter;

      wkiR = wkiR * wkR - wkiI * wkI;
      wkiI = wkiR * wkI + wkiI * wkR;
    }
//      System.out.println("Range: " + range + "'s initial start is: "
//    		       + prev.real() + " " + prev.imag() + "i");
  }
  */

  public DFTFilter(int DFTLen, float range) {
    super(DFTLen, range);
  }
}

class FilterBank extends SplitJoin {
  public void init(final int channels) {
    setSplitter(DUPLICATE());

    for(int k=0; k < channels; k++) {
      //this filter is for the kth range
      final float range = (float)(2 * 3.1415926535898f * k)/channels;
      add(new DFTFilter(channels ,range));
//        final float range = (float)( 3.1415926535898f * (k - 1))/channels;
//        add(new DFTFilter(channels * 2,range));
    }

    //send real and imaginary parts together
    setJoiner(ROUND_ROBIN(2));
  }

  FilterBank(int channels) {
    super(channels);
  }

}

class SumReals extends Filter {
  int length;
  public SumReals(int length) {
    super(length);
  }

  public void init(int length) {
    this.length = length;
    input = new Channel(Float.TYPE,  2 * length);
    output = new Channel(Float.TYPE, 1);
  }

//    public void work() {
//      float sum = 0;
//      int i=0;
//      float first = input.popFloat(); input.popFloat();

//      for(i=1; i < length - 1; i++) {
//        if (i % 2 == 0)
//  	sum += input.popFloat();
//        else
//  	sum -= input.popFloat();
//        input.popFloat();
//      }
//      sum *= 2; //double the internal ones
//      sum += first; 
//      if (i % 2 == 0)
//        sum += input.popFloat(); 
//      else
//        sum -= input.popFloat();
//      input.popFloat();
//      sum /= (length * 2);
//      output.pushFloat(sum);
//    }

  public void work() {
    float sum = 0;

    for(int i=0; i < length; i++) {
      if (i % 2 == 0)
	sum += input.popFloat();
      else
	sum -= input.popFloat();
      input.popFloat();
    }
    sum /= (length);
    output.pushFloat(sum);
  }
}
