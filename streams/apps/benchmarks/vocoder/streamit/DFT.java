import streamit.library.*;
import streamit.library.io.*;

/** DFTFilter expects that the first DFTLen numbers will all be 0.
 * Thus it can skip the initial calculation, and immediately enter the
 * steady-state.  A more general DFTFilter that can handle non-0 data
 * values within the first DFTLen numbers is below, known as
 * DFTChannel.  The vocoder system assures this requirement by adding
 * a delay of DFTLen 0s to the front of any data.  The system then
 * does an inverse delay to get rid of the (DFTLen/2 - 1) 0s that
 * precede the actual data.
 * 
 **/
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

    nextR = prevR * wR - prevI * wI;
    nextI = prevR * wI + prevI * wR;
    prevR = nextR; prevI = nextI;

//      System.out.println("Range: " + range + "'s initial start is: "
//    		       + prevR + " + " + prevI + "i");

    output.pushFloat(prevR);
    output.pushFloat(prevI);
  }

  public void init(int DFTLength, float _range) {
    this.DFTLen = DFTLength;
    this.range = _range;
    this.deter = 0.999999f;
    this.detern = 1;
    wR = (float)Math.cos(_range);
    wI = (float)-Math.sin(_range);
    prevR = 0; prevI = 0;

    //need to peek DFTLen ahead of current one
    input = new Channel(Float.TYPE, 1, DFTLength+1);
    output = new Channel(Float.TYPE, 2);
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

    for(int k=0; k <= channels/2; k++) {
      //this filter is for the kth range
//        final float range = (float)(2 * 3.1415926535898f * k)/channels;
//        add(new DFTFilter(channels ,range));
      final float range = (float)( 2 * 3.1415926535898f * k)/channels;
      add(new DFTFilter(channels,range));
    }

    //send real and imaginary parts together
    setJoiner(ROUND_ROBIN(2));
  }

  FilterBank(int channels) {
    super(channels);
  }

}

class DFTChannel extends Filter
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
    }

    float nextVal = (float) input.peekFloat(DFTLen);
    float current = (float) input.popFloat();

    prevR = prevR * deter + (nextVal - (detern * current));
    prevI = prevI * deter;// + (nextVal - (detern * current));

    nextR = prevR * wR - prevI * wI;
    nextI = prevR * wI + prevI * wR;
    prevR = nextR; prevI = nextI;

    output.pushFloat(prevR);
    output.pushFloat(prevI);
    System.out.println("range: " + range + " real: " + prevR + " imag: " + prevI);
  }

  public void init(int DFTLength, float _range) {
    this.DFTLen = DFTLength;
    this.range = _range;
    this.deter = 0.999999f;
    this.detern = 1;
    wR = (float)Math.cos(_range);
    wI = (float)-Math.sin(_range);
    prevR = 0; prevI = 0;

    //need to peek DFTLen ahead of current one
    input = new Channel(Float.TYPE, 1, DFTLength+1);
    output = new Channel(Float.TYPE, 2);
  }

  public DFTChannel(int DFTLen, float range) {
    super(DFTLen, range);
  }
}
class TransformBank extends SplitJoin {
  public void init(final int channels, final int window) {
    setSplitter(DUPLICATE());

    for(int k=0; k < channels; k++) {
      //this filter is for the kth range
      final float range = (float)(2 * 3.1415926535898f * k)/channels;
      add(new DFTChannel(window ,range));
//        final float range = (float)( 3.1415926535898f * k)/channels;
//        add(new DFTFilter(channels * 2,range));
    }

    //send real and imaginary parts together
    setJoiner(ROUND_ROBIN(2));
  }

  TransformBank(int channels, int window) {
    super(channels,window);
  }

}

class SumReals extends SplitJoin {
  public SumReals(int DFT_LENGTH) {
    super(DFT_LENGTH);
  }
  public void init(final int DFT_LENGTH) {
    setSplitter(ROUND_ROBIN());
    add(new SumRealsRealHandler(DFT_LENGTH));
    add(new FloatVoid());
    setJoiner(WEIGHTED_ROUND_ROBIN(1,0));
  }
}

class SumRealsRealHandler extends Pipeline {
  public SumRealsRealHandler(int DFT_LENGTH) {
    super(DFT_LENGTH);
  }
  public void init(final int DFT_LENGTH) {
    add(new SplitJoin() {
	public void init() {
	  setSplitter(WEIGHTED_ROUND_ROBIN(1,DFT_LENGTH - 2, 1));
  	  add(new Identity(Float.TYPE));
	  add(new Doubler());
//  	  add(new ConstMultiplier(2.0f));
  	  add(new Identity(Float.TYPE));
	  setJoiner(WEIGHTED_ROUND_ROBIN(1, DFT_LENGTH - 2, 1));
	}
      });
    if (DFT_LENGTH % 2 != 0) {
      add(new Padder(DFT_LENGTH,0,1));
    }
    add(new SplitJoin() {
	public void init() {
	  setSplitter(ROUND_ROBIN());
	  add(new Adder((DFT_LENGTH + 1)/2));
	  add(new Adder((DFT_LENGTH + 1)/2));
	  setJoiner(ROUND_ROBIN());
	}
      });
    add(new Subtractor());
    add(new ConstMultiplier((float)(1f / ((DFT_LENGTH - 1) * 2))));
  }
}


/**/
class SumReals2 extends Filter {
  int length;
  public SumReals2(int length) {
    super(length);
  }

  public void init(int len) {
    this.length = len;
    input = new Channel(Float.TYPE,  2 * len);
    output = new Channel(Float.TYPE, 1);
  }

  public void work() {
    float sum = 0;
    int i=0;
    float first = input.popFloat(); input.popFloat();

    for(i=1; i < length - 1; i++) {
      if (i % 2 == 0)
	sum += input.popFloat();
      else
	sum -= input.popFloat();
      input.popFloat();
    }
    sum += sum; //double the internal ones
    sum += first; 
    if (i % 2 == 0)
      sum += input.popFloat(); 
    else
      sum -= input.popFloat();
    input.popFloat();
    sum /= ((length - 1) * 2);
    output.pushFloat(sum);
  }

//    public void work() {
//      float sum = 0;

//      for(int i=0; i < length; i++) {
//        if (i % 2 == 0)
//  	sum += input.popFloat();
//        else
//  	sum -= input.popFloat();
//        input.popFloat();
//      }
//      sum /= (length);
//      output.pushFloat(sum);
//    }
}

/**/
