import streamit.library.*;
import streamit.library.io.*;

class PhaseUnwrapper extends Filter {
  float estimate, previous;

  public void init() {
    input = new Channel(Float.TYPE, 1);
    output = new Channel(Float.TYPE, 1);
//      estimate = (float) (-0.05 * (Math.PI * 2));
    previous = 0f;
    estimate = 0f;
  }

  public void work() {
    float unwrapped = input.popFloat();
    unwrapped += estimate;
    float delta = unwrapped - previous;
    while (delta > 2 * Math.PI * (11f / 16f)) {
//      while (delta > 2 * Math.PI * (11f / 16f)) {
//        System.out.println("subtracting: " + unwrapped + " + " + estimate + " - " + previous + " is > 2PI");
      unwrapped -= 2 * Math.PI;
      delta -= 2 * Math.PI;
      estimate -= 2 * Math.PI;
    }
    while (delta < -2 * Math.PI * (11f / 16f)) {
//      while (delta < -2 * Math.PI * (11f / 16f)) {
//        System.out.println("adding: " + unwrapped + " + " + estimate + " - " + previous + " is < -2PI");
      unwrapped += 2 * Math.PI;
      delta += 2 * Math.PI;
      estimate += 2 * Math.PI;
    }
    previous = unwrapped;
    output.pushFloat(unwrapped);
  }

  PhaseUnwrapper() {
    super();
  }
}

/**/
class FirstDifference extends Filter {
  private float prev;
//    private boolean first;

  public FirstDifference() {
    super();
  }

  public void init() {
    input = new Channel(Float.TYPE, 1, 1);
    output = new Channel(Float.TYPE, 1);
    prev = 0f;//input.peekFloat(0);
//      first = false;
  }

  public void work() {
//      output.pushFloat(prev - input.peekFloat(0));
    output.pushFloat(input.peekFloat(0) - prev);
    prev = input.popFloat();
  }
}

class InnerPhaseStuff extends Pipeline implements Constants {

  public void init(float c, float speed) {
    add(new PhaseUnwrapper());
    add(new FirstDifference());
    if (c != 1.0) {
      add(new ConstMultiplier(c));
    }
    if (speed != 1.0) {
      add(new Remapper(n_LENGTH, m_LENGTH));
//  //        add(new Remapper(10, (int) (speed * 10)));
    }
    add(new Accumulator());
  }
  public InnerPhaseStuff(float c, float speed) {
    super(c, speed);
  }
}

class PhaseStuff extends Pipeline implements Constants {

  public void init(final int DFTLen, final int newLen, final float c,
		   final float speed) {

    if (speed != 1.0 || c != 1.0) {
      add(new SplitJoin() {
	  public void init() {
	    setSplitter(ROUND_ROBIN());
	    for(int i=0; i < DFTLen; i++) {
	      add(new InnerPhaseStuff(c, speed));
	    }
	    setJoiner(ROUND_ROBIN());
	  }
	});
      if (newLen != DFTLen) {
	add(new Duplicator(DFT_LENGTH_REDUCED, NEW_LENGTH_REDUCED));
      }
    } else {
        add(new Identity(Float.TYPE));
    }
  }

  PhaseStuff(int DFTLen, int newLen, float c, float speed) {
    super(DFTLen, newLen, c, speed);
  }
}

/**/
