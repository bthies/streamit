/** @author: clleger
 *
 **/
import streamit.*;
import streamit.io.*;

class MagnitudeStuff extends Pipeline {
  public void init(final int DFTLen, final int newLen) {
    add(new SplitJoin() {
	public void init() {
	  setSplitter(DUPLICATE());
	  add(new FIRSmoothingFilter(DFTLen));
	  add(new IdentityFloat());
	  setJoiner(ROUND_ROBIN());
	}
      });
    add(new Deconvolve());
    add(new SplitJoin() {
	public void init() {
	  setSplitter(ROUND_ROBIN());
	  add(new Remapper(DFTLen, newLen));
	  add(new Remapper(DFTLen, newLen));
	  setJoiner(ROUND_ROBIN());
	}
      });
    add(new Multiplier());
  }

  MagnitudeStuff(final int DFTLen, final int newLen) {
    super(DFTLen, newLen);
  }
}

class PhaseUnwrapper extends Filter {
  int DFTLen;
  float estimate;

  public void init(int DFTLen) {
    this.DFTLen = DFTLen;
    input = new Channel(Float.TYPE, 1, DFTLen+1);
    output = new Channel(Float.TYPE, 1);
    estimate = (float) (-0.05 * (Math.PI * 2));
  }

  public void work() {
    float unwrapped = input.popFloat();
    float delta = unwrapped + estimate - input.peekFloat(DFTLen);
    while (delta > 2 * Math.PI) {
      unwrapped -= 2 * Math.PI;
      delta = unwrapped + estimate - input.peekFloat(DFTLen);
    }
    while (delta < -2 * Math.PI) {
      unwrapped += 2 * Math.PI;
      delta = unwrapped + estimate - input.peekFloat(DFTLen);
    }
    output.pushFloat(unwrapped);
  }

  PhaseUnwrapper(int DFTLen) {
    super(DFTLen);
  }
}
    
class PhaseStuff extends Pipeline {

  public void init(int DFTLen, int newLen, float c) {
    add(new PhaseUnwrapper(DFTLen));
    add(new ConstMultiplier(c));
    add(new Remapper(DFTLen, newLen));
  }

  PhaseStuff(int DFTLen, int newLen, float c) {
    super(DFTLen, newLen, c);
  }
}

class VocoderSystem extends SplitJoin
{
  public void init(int DFTLen, int newLen, float c) {
    //if I can't send two from each stream above in ____, then I have
    //to send the first DFTLen to Magnitude and then the next DFTLen
    //to phase.  If I can start sending two at a time above, then I
    //can revert to a normal ROUND_ROBIN

    //setSplitter(ROUND_ROBIN());
    setSplitter(WEIGHTED_ROUND_ROBIN(DFTLen, DFTLen));

    add(new MagnitudeStuff(DFTLen, newLen));
    add(new PhaseStuff(DFTLen, newLen, c));

    setJoiner(ROUND_ROBIN());
  }

  VocoderSystem(int DFTLen, int newLen, float c) {
    super(DFTLen, newLen, c);
  }
}

class IntPrinter extends Filter {
    public void work() { System.out.println(input.popInt()); }
    public void init() { input = new Channel(Integer.TYPE, 1); }
}

class FloatPrinter extends Filter {
    public void work() { System.out.println(input.popFloat()); }
    public void init() { input = new Channel(Float.TYPE, 1); }
}

class Vocoder extends StreamIt {
  //  final int DFT_LENGTH = 100;
  final int DFT_LENGTH = 10;
  final int NEW_LENGTH = 10;
  final float FREQUENCY_FACTOR = 3.45f;

  public static void main(String args[]) {
    new Vocoder().run(args);
  }

  public void init() {
    add(new StepSource()); //add(new AudioSource());
    add(new FilterBank(DFT_LENGTH));
    add(new VocoderSystem(DFT_LENGTH, NEW_LENGTH, FREQUENCY_FACTOR));
    //    add(new SumReal(DFT_LENGTH));
    add(new FloatPrinter());
  }
}    
