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
    while (delta > 2 * Math.PI * (7f / 8f)) {
//        System.out.println("subtracting: " + unwrapped + " + " + estimate + " - " + previous + " is > 2PI");
      unwrapped -= 2 * Math.PI;
      delta -= 2 * Math.PI;
      estimate -= 2 * Math.PI;
    }
    while (delta < -2 * Math.PI * (7f / 8f)) {
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

  public FirstDifference() {
    super();
  }

  public void init() {
    input = new Channel(Float.TYPE, 1, 1);
    output = new Channel(Float.TYPE, 1);
    prev = 0f;//intput.peekFloat(0);
  }

  public void work() {
    float base = input.popFloat();
    output.pushFloat(base - prev);
    prev = base;
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
		  add(new FirstDifference());
		  add(new ConstMultiplier(c));
		  add(new Accumulator());
//  		  add(new FloatPrinter("(phase " + fi + "):", "\n"));
  		  add(new Remapper(10, (int) (speed * 10)));
		}
	      });
	  }
	  setJoiner(ROUND_ROBIN());
	}
      });
    add(new Duplicator(DFTLen, newLen));
  }

  PhaseStuff(int DFTLen, int newLen, float c, float speed) {
    super(DFTLen, newLen, c, speed);
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
