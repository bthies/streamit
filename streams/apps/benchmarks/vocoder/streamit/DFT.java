import streamit.*;
import streamit.io.*;

class DFTFilter extends Filter
{
//the rate by which to deteriorate, assuring stability
  final float deter = 0.99997f; 
  //since the previous complex value is multiplied by the deter each
  //time, by the time the last time sample is windowed out it's
  //effect will have been multiplied by deter DFTLen times, hence it
  //needs to be multiplied by deter^DFTLen before being subtracted
  final float detern;
  //  float o[];
  int DFTLen;
  float range;
  private boolean first = true;
  Complex prev;
  Complex w; //represents w^(-k)

  public void work() {
    if (first) {
      first = false;
      computeFirstDFT();
    }
    float nextVal = (float) input.peekFloat(DFTLen);
    float current = (float) input.popFloat();

    prev = prev.times(deter).plus(new Complex(nextVal-(detern*current)));
//      System.out.println("Range: " + range + "'s initial start is: "
//    		       + prev.real() + " " + prev.imag() + "i");
    prev = prev.times(w);
//      output.pushObject(prev);
    output.pushFloat(prev.real());
    output.pushFloat(prev.imag());
  }

  public void init(int DFTLen, float range) {
    this.DFTLen = DFTLen;
    this.range = range;
    w = new Complex((float)Math.cos(range), (float)-Math.sin(range));
    prev = new Complex();

    //need to peek DFTLen ahead of current one
    input = new Channel(Float.TYPE, 1, DFTLen+1);
    output = new Channel(Float.TYPE, 2);
//      output = new Channel(Complex.TYPE, 1);
    //    computeFirstDFT();
  }

  public void computeFirstDFT() {
    //note: this w = w^k, not w^(-k)
    Complex wk = new Complex((float)Math.cos(range), (float)Math.sin(range));
    Complex wki = new Complex(1, 0); //this represents w^(k*i)
    for (int i=0; i < DFTLen; i++) {
      float nextVal = (float) input.peekFloat(i);

      prev = prev.plus(wki.times(nextVal)).times(deter);
      wki = wki.times(wk);
    }
//      System.out.println("Range: " + range + "'s initial start is: "
//    		       + prev.real() + " " + prev.imag() + "i");
  }

  public DFTFilter(int DFTLen, float range) {
    super(DFTLen, range);
    //need to ask about constructors
    this.detern = (float) Math.exp(DFTLen * Math.log(deter));
    //as mentioned above, this is deter^DFTLen
  }
}

class FilterBank extends SplitJoin {
  public void init(final int channels) {
    setSplitter(DUPLICATE());

    for(int k=0; k < channels; k++) {
      //this filter is for the kth range
      final float range = (float)(2 * Math.PI * k)/channels;
      add(new DFTFilter(channels,range));
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
    input = new Channel(Float.TYPE,  2 * this.length);
    output = new Channel(Float.TYPE, 1);
  }

  public void work() {
    float sum = 0;
    for(int i=0; i < length; i++) {
      if (i % 2 == 0)
	sum += input.popFloat();
      else
	sum -= input.popFloat();
      input.popFloat();
    }
    sum /= length;
    output.pushFloat(sum);
  }
}
