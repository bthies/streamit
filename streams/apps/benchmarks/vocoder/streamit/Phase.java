import streamit.*;
import streamit.io.*;

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
//  //        add(new FloatPrinter());
//  //        add(new Remapper(10, (int) (speed * 10)));
//  //        add(new Remapper(1, 2));
    }
    add(new Accumulator());
    //  		  add(new FloatPrinter("(phase " + fi + "):", "\n"));
  }
  public InnerPhaseStuff(float c, float speed) {
    super(c, speed);
  }
}

class PhaseStuff extends Pipeline {

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
	add(new Duplicator(DFTLen, newLen));
      }
    } else {
      add(new IdentityFloat());
    }
  }

  PhaseStuff(int DFTLen, int newLen, float c, float speed) {
    super(DFTLen, newLen, c, speed);
  }
}

/**/
/**
class FirstDifference extends Filter {
  private float prev;
  private boolean first;

  public FirstDifference() {
    super();
  }

  public void init() {
    input = new Channel(Float.TYPE, 1, 2);
    output = new Channel(Float.TYPE, 1);
//      prev = 0f;//input.peekFloat(0);
    first = false;
  }

  public void work() {
    float base = input.popFloat();
    if (first) {
      first = false;
      prev = base;
    }
    output.pushFloat(base - prev);
    prev = base;
  }
}
/**/
/**
class FirstDifference extends Filter {
  private float c, prev, next;
  private boolean first = true;

  public FirstDifference(float c) {
    super(c);
  }

  public void init(float c) {
    this.c = c;

    input = new Channel(Float.TYPE, 1, 1);
    output = new Channel(Float.TYPE, 1);
    prev = 0f;//input.peekFloat(0);
  }

  public void work() {
    float base = input.popFloat();
    if (first) {
      next = base; prev = base; first = false;
    }
    next = next + ((base - prev) * c);
    prev = base;
    output.pushFloat(next);
  }
}

class PhaseStuff extends Pipeline {

  public void init(final int DFTLen, final int newLen, final float c,
		   final float speed) {
    add(new SplitJoin() {
	public void init() {
	  setSplitter(ROUND_ROBIN());
	  for(int i=0; i < DFTLen; i++) {
	    final int fi = i;
	    add(new Pipeline() {
		public void init() {
		  add(new PhaseUnwrapper());
		  add(new FirstDifference(c));
	 	  add(new FloatPrinter("(phase " + fi + "):", "\n"));
		}
	      });
	  }
	  setJoiner(ROUND_ROBIN());
	}
      });
//      add(new FloatPrinter("(phase):\n"));
    add(new Duplicator(DFTLen, newLen));
  }

  PhaseStuff(int DFTLen, int newLen, float c, float speed) {
    super(DFTLen, newLen, c, speed);
  }
}

/**/
