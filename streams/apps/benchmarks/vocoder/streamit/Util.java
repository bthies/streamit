import streamit.*;
import streamit.io.*;

/** RecToPolar **/
class RectangularToPolar extends Filter {
  public void init() {
    input = new Channel(Float.TYPE, 2);
    output = new Channel(Float.TYPE, 2);
  }

  public void work() {
    float x, y;
    Complex c;
    x = input.popFloat(); y = input.popFloat();
    c = new Complex(x,y);

    //output.pushFloat(c.real());
    output.pushFloat(c.mag());
    //output.pushFloat(c.imag());
    output.pushFloat(c.phase());
  }
}

class PolarToRectangular extends Filter {
  public void init() {
    input = new Channel(Float.TYPE, 2);
//      input = new Channel(Complex.TYPE, 1);
    output = new Channel(Float.TYPE, 2);
  }

  public void work() {
    float r, theta;
    r = input.popFloat(); theta = input.popFloat();

    output.pushFloat((float)(r * Math.cos(theta)));
    output.pushFloat((float)(r * Math.sin(theta)));
  }
}
/** RecToPolar **/

/** Complex **/
class Complex {
  private float real, imag;
  private float r, theta;
  private int fresh = 0;

  public Complex() {
    this.real = 0; this.imag = 0; this.fresh = 3;
    this.r = 0; this.theta = 0;
  }
  public Complex(float real) { 
    this.real = real; this.fresh = 1;
  }
  public Complex(float real, float imag) { 
    this.real = real; this.imag = imag; this.fresh = 1;
  }
  public Complex(float r, float theta, boolean a) { 
    this.r = r; this.theta = theta; this.fresh = 2;
  }
  public Complex(Complex c) { 
    this.real = c.real; this.imag = c.imag; this.fresh = c.fresh;
    this.r = c.r; this.theta = c.theta;
  }

  public float real() { 
    updateRect();
    return this.real;
  }

  public float imag() { 
    updateRect();
    return this.imag;
  }

  private void updateRect() {
    if ((fresh & 1) == 1 || (fresh & 2) == 0)
      return;
    this.real = (float) (r * Math.cos(theta));
    this.imag = (float) (r * Math.sin(theta));
    fresh |= 1;
  }

  public float mag() { 
    updatePolar();
    return this.r;
  }

  public float phase() { 
    updatePolar();
    return this.theta;
  }

  private void updatePolar() {
    if ((fresh & 2) == 2 || (fresh & 1) == 0)
      return;
    this.r = (float) Math.sqrt(real*real + imag*imag);
    this.theta = (float) Math.atan2(imag, real);
    fresh |= 2;
  }

  public Complex setMag(float mag) {
    updatePolar(); this.r = mag; fresh = 2; return this;
  }

  public Complex setPhase(float phase) {
    updatePolar(); this.theta = phase; fresh = 2; return this;
  }

  public Complex setReal(float real) {
    updateRect(); this.real = real; fresh = 1; return this;
  }

  public Complex setImag(float imag) {
    updateRect(); this.imag = imag; fresh = 1; return this;
  }

  public Complex times(Complex c) {
    if ((this.fresh & 1) == 1 && (c.fresh & 1) == 1)
      return new Complex(c.real * this.real - c.imag * this.imag,
			 c.real * this.imag + c.imag * this.real);
    else if ((this.fresh & 2) == 2 && (c.fresh & 2) == 2)
      return new Complex(c.r * this.r, c.theta + this.theta, false);
    else {
      if ((this.fresh & 1) == 1)
	c.updateRect();
      else
	this.updateRect();
      return this.times(c);
    }
  }

  public Complex times(float f) {
    Complex result = new Complex(this);
    if ((result.fresh & 1) == 1) {
      result.real *= f; result.imag *= f;
    }
    if ((result.fresh & 2) == 2) {
      result.r *= f;
    }
    return result;
  }
  public Complex plus(Complex c) {
    this.updateRect(); c.updateRect();
    return new Complex(c.real + this.real, c.imag + this.imag);
  }
}
/** Complex **/

class IntToFloat extends Filter {
  public void init() {
    input = new Channel(Integer.TYPE, 1);
    output = new Channel(Float.TYPE, 1);
  }
  public void work() {
    output.pushFloat(input.popInt());
  }
}

class IntPrinter extends Filter {
  String append;
    public void work() { int i = input.popInt();
    System.out.print(i); System.out.print(append); output.pushInt(i);}
    public void init() { input = new Channel(Integer.TYPE, 1); 
    output = new Channel(Integer.TYPE, 1);}
  IntPrinter() {append = "\n";}
  IntPrinter(String append) {this.append = append;}
}

class ShortPrinter extends Filter {
  String append;
    public void work() { short i = input.popShort();
    System.out.print(i); System.out.print(append); output.pushShort(i);}
    public void init() { input = new Channel(Short.TYPE, 1); 
    output = new Channel(Short.TYPE, 1);}
  ShortPrinter() {append = "\n";}
  ShortPrinter(String append) {this.append = append;}
}

class FloatPrinter extends Filter {
  String append, prepend;
  public void work() { float i = input.popFloat(); System.out.print(prepend);
    System.out.print(i); System.out.print(append); output.pushFloat(i);}
    public void init() { input = new Channel(Float.TYPE, 1); 
    output = new Channel(Float.TYPE, 1);}
  FloatPrinter() {prepend = "";append = "\n";}
  FloatPrinter(String append) 
  {this.prepend = "";this.append = append;}
  FloatPrinter(String prepend, String append) 
  {this.prepend = prepend;this.append = append;}
}

class Printer extends Filter {
  String append, prepend;
  Class type;
  public void work() { Object o = input.pop(); System.out.print(prepend);
    System.out.print(o); System.out.print(append); output.push(o);}
    public void init() { input = new Channel(type, 1); 
    output = new Channel(type, 1);}
  Printer(Class type) {this.type = type; prepend = "";append = "\n";}
  Printer(Class type, String append) 
  {this.type = type;this.prepend = "";this.append = append;}
  Printer(Class type, String prepend, String append) 
  {this.type = type;this.prepend = prepend;this.append = append;}
}

class FloatVoid extends Filter {
  public void work() {input.popFloat();}
  public void init() {input = new Channel(Float.TYPE, 1); }
}

class IdentityFloat extends Filter {
  public void work() {output.pushFloat(input.popFloat());}
  public void init() {input = new Channel(Float.TYPE, 1);
  output = new Channel(Float.TYPE, 1);}
}

class ShortVoid extends Filter {
  public void work() {input.popShort();}
  public void init() {input = new Channel(Short.TYPE, 1); }
}

class FloatToShort extends Filter {
  public void work() {output.pushShort((short)Math.round(input.popFloat())); }
  public void init() {input = new Channel(Float.TYPE, 1);
                      output = new Channel(Short.TYPE, 1);}
}

class ShortToFloat extends Filter {
  public void work() {output.pushFloat((float)(input.popShort())); }
  public void init() {input = new Channel(Short.TYPE, 1);
                      output = new Channel(Float.TYPE, 1);}
}

class Test extends StreamIt {
  final int channels = 3;

  public static void main(String args[]) {
    new Test().run(args);
  }

  /**
  public void init() {
    add(new PlateauSource(7));
    add(new IntToFloat());
//      add(new SineSource((float)Math.PI / 4));
//      add(new ConstMultiplier(10f));
    add(new FloatPrinter("orig: ", "\n"));
    add(new PhaseStuff(8, 8, 3.2f, 1f));
//      add(new FilterBank(8));
//  //      add(new FloatPrinter("DFT: ", "\n"));
//      add(new SumReals(8));
    //    add(new PhaseUnwrapper());
//      add(new FirstDifference(2f));
    //    add(new Remapper(12,8));
    add(new FloatPrinter("Remapped: ", "\n"));
    add(new FloatVoid());
  }

  /**/

  /** Tests the phase unwrapper.  seems to be pretty good.
   * sinesource should have this.prev = theta*3.2 to demo
   **/
//    public void init() {
//  //      add(new PlateauSource(6));
//  //      add(new IntToFloat());
//  //      add(new AddSource((float)Math.PI * 0.06125f));
//      add(new FunkySource((float)Math.PI * 0.06125f));
//      add(new ModularFilter((float)Math.PI * 2.0f));
//      add(new FloatPrinter("orig: ", "\n"));
//      add(new PhaseUnwrapper());
//  //      add(new FirstDifference(2f));
//      //    add(new Remapper(12,8));
//      add(new FloatPrinter("Remapped: ", "\n"));
//      add(new FloatVoid());
//    }

  /** This demonstrates rectangular to polar pretty well **/
//    public void init() {
//      add(new StepSource(7));
//      add(new IntToFloat());
//      add(new SplitJoin() {
//  	public void init() {
//  	  setSplitter(DUPLICATE());
//  	  add(new SineSource((float)(Math.PI * 0.25)));
//  	  add(new IdentityFloat());
//  	  setJoiner(ROUND_ROBIN());
//  	}
//        });
//      add(new FloatPrinter("orig: ", "\n"));
//      add(new RectangularToPolar());
//      add(new PolarToRectangular());
//      //    add(new Remapper(12,8));
//      add(new FloatPrinter("Remapped: ", "\n"));
//      add(new FloatVoid());
//    }

    /** Multiplier, and FIRSmoothingFilter */
//    public void init() {
//      add(new StepSource(7));
//      add(new IntToFloat());
//      add(new FloatPrinter("orig: ", "\n"));
//      add(new FIRSmoothingFilter(16));
//      add(new FloatPrinter("Convolved: ", "\n"));
//  //      add(new Multiplier());
//  //      add(new ConstMultiplier(5));
//  //  //      add(new RectangularToPolar());
//  //  //      add(new PolarToRectangular());
//  //  //      add(new FloatPrinter("Deconvolve: ", "\n"));
//      add(new FloatVoid());
//    }

  /**/
  public void init() {
    add(new FileReader("/home/bits6/clleger/Perfect.wav", Short.TYPE));
    add(new SplitJoin() {
	public void init() {
	  setSplitter(DUPLICATE());
	  add(new Pipeline() {
	      public void init() {
		add(new WaveSplitter(0));
		add(new FileWriter("/home/bits6/clleger/Perfect0.wav", Short.TYPE));
	      }
	    });
	  add(new Pipeline() {
	      public void init() {
		add(new WaveSplitter(1));
		add(new FileWriter("/home/bits6/clleger/Perfect1.wav", Short.TYPE));
	      }
	    });
	  setJoiner(ROUND_ROBIN());
	}
      });
    //    add(new ShortVoid());
  }
  /**/

}    
