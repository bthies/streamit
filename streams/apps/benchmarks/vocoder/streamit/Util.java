import streamit.library.*;
import streamit.library.io.*;

//  class Delay extends FeedbackLoop {

//      public Delay(int N) {
//  	super(N);
//      }

//      public void init(int N) {
//  	setSplitter(ROUND_ROBIN());
//  	setDelay(N);
//  	setBody(new IdentityFloat(Float.TYPE));
//  	setLoop(new IdentityFloat(Float.TYPE));
//  	setJoiner(ROUND_ROBIN());
//      }

//      public float initPathFloat(int index) {
//  	return 0.0f;
//      }
//  }
class Delay extends Filter {
  float delay[];
  int length;

    public Delay(int N) {
	super(N);
    }

    public void init(int N) {
      delay = new float[N];
      for(int i=0; i < N; i++)
	delay[i] = 0;
      length = N;
      input = new Channel(Float.TYPE, 1);
      output = new Channel(Float.TYPE, 1);
    }

  public void work() {
    output.pushFloat(delay[0]);
    for(int i=0; i < length - 1; i++)
      delay[i] = delay[i+1];
    delay[length - 1] = input.popFloat();
  }
}

class FrontPadder extends Filter {
  int length, padding;

    public FrontPadder(int N, int i) {
	super(N, i);
    }

    public void init(int N, int i) {
      length = N;
      padding = i;
      input = new Channel(Float.TYPE, N);
      output = new Channel(Float.TYPE, N+i);
    }

  public void work() {
    for(int i=0;i < padding; i++)
      output.pushFloat(0f);

    for(int i=0; i < length; i++)
      output.pushFloat(input.popFloat());
  }
}

class Padder extends Filter {
  int length, front, back;

    public Padder(int N, int i, int j) {
	super(N, i, j);
    }

    public void init(int N, int i, int j) {
      length = N;
      front = i;
      back = j;
      input = new Channel(Float.TYPE, N);
      output = new Channel(Float.TYPE, N+i+j);
    }

  public void work() {
    for(int i=0;i < front; i++)
      output.pushFloat(0f);

    for(int i=0; i < length; i++)
      output.pushFloat(input.popFloat());

    for(int i=0; i < back; i++)
      output.pushFloat(0);
  }
}

class InvDelay extends Filter {
  float delay[];
  int length;

    public InvDelay(int N) {
	super(N);
    }

    public void init(int N) {
      delay = new float[N];
      length = N;
      input = new Channel(Float.TYPE, 1,N+1);
      output = new Channel(Float.TYPE, 1);
    }

  public void work() {
    output.pushFloat(input.peekFloat(length));
    input.popFloat();
  }
}
/** RecToPolar **/
class RectangularToPolar extends Filter {
  public void init() {
    input = new Channel(Float.TYPE, 2);
    output = new Channel(Float.TYPE, 2);
  }

  public void work() {
    float x, y;
    float r, theta;
//      Complex c;
    x = input.popFloat(); y = input.popFloat();
//      c = new Complex(x,y);

    r = (float)Math.sqrt(x * x + y * y);
    theta = (float)Math.atan2(y, x);

//      if (y < 0 || (y == 0 && x < 0)) {
//        theta += Math.PI;
//        r = -r;
//      }
      
    output.pushFloat(r);
    output.pushFloat(theta);
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

/** Complex **/ /**
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
    output.pushFloat((float)input.popInt());
  }
}

class IntToDouble extends Filter {
  public void init() {
    input = new Channel(Integer.TYPE, 1);
    output = new Channel(Double.TYPE, 1);
  }
  public void work() {
    output.pushDouble(input.popInt());
  }
}

class ShortToDouble extends Filter {
  public void init() {
    input = new Channel(Short.TYPE, 1);
    output = new Channel(Double.TYPE, 1);
  }
  public void work() {
    output.pushDouble(input.popShort());
  }
}

class DoubleToShort extends Filter {
  public void init() {
    input = new Channel(Double.TYPE, 1);
    output = new Channel(Short.TYPE, 1);
  }
  public void work() {
    output.pushShort((short) (input.popDouble() + 0.5));
  }
}

class Timer extends Filter {
  private int count, length, num;
  private long lastTime;

  public void init(int N) {
    this.length = N;
    this.count = 0;
    this.num = 0;
//      lastTime = System.currentTimeMillis();
    input = new Channel(Short.TYPE, 1);
    output = new Channel(Short.TYPE, 1);
  }

  public void work() {
    output.pushShort(input.popShort());
    count++;
    if (count == length) {
      count = 0;
//        System.out.println(System.currentTimeMillis() - lastTime);
//        lastTime = System.currentTimeMillis();
      System.out.println(num++);
    }
  }

  public Timer(int N) {
    super(N);
  }
}    

class CountDown extends Filter {
  private int length, count;

  CountDown(int length) {
    super(length);
  }
  public void init(int len) {
    this.length = len;
    this.count = len;
    input = new Channel(Float.TYPE, 1);
  }
  public void work() {
    count--;
    input.popFloat();
    if (count == 0) {
      count = length;
      System.out.println("done");
    }
  }
}

class IntPrinter extends Filter {
  int x;
  public void work() { int i = input.popInt();
  System.out.print(x+++" ");
  System.out.println(i); 
  output.pushInt(i);
  }
  public void init() { x = 0;input = new Channel(Integer.TYPE, 1); 
  output = new Channel(Integer.TYPE, 1);}
  IntPrinter() {}
}

class ComplexPrinter extends Filter {
  int real,imag;
  int N;
  public void work() { float f = input.popFloat();
  System.out.print((real++ * 2 * Math.PI /N)+" ");
  System.out.println(f); 
  output.pushFloat(f);
  f = input.popFloat();
  System.err.print((imag++ * 2 * Math.PI /N)+" ");
  System.err.println(f); 
  output.pushFloat(f);
  if (real == N) {
    real = 0;
    imag = 0;
  }
  }
  
  public void init(int length) { 
      this.N = length;
      real= 0;
      input = new Channel(Float.TYPE, 2); 
      imag = 0;
      output = new Channel(Float.TYPE, 2);
    }
  public ComplexPrinter(int length) {
    super(length);
  }
}

class ShortPrinter extends Filter {
  public void work() { short i = input.popShort();
    System.out.println(i); output.pushShort(i);}
    public void init() { input = new Channel(Short.TYPE, 1); 
    output = new Channel(Short.TYPE, 1);}
  ShortPrinter() {}
}
class DoublePrinter extends Filter {
  public void work() { double i = input.popDouble();
    System.out.println(i); output.pushDouble(i);}
    public void init() { input = new Channel(Double.TYPE, 1); 
    output = new Channel(Double.TYPE, 1);}
  DoublePrinter() {}
}
class FloatPrinter extends Filter {

  public void work() { float i = input.popFloat(); 
    System.out.println(i); 
    output.pushFloat(i);}
    public void init() { input = new Channel(Float.TYPE, 1); 
    output = new Channel(Float.TYPE, 1);}
  FloatPrinter() {}
}

/** 
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

*/
/*
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

**/

class FloatVoid extends Filter {
  public void work() {input.popFloat();}
  public void init() {input = new Channel(Float.TYPE, 1); }
}

class ShortVoid extends Filter {
  public void work() {input.popShort();}
  public void init() {input = new Channel(Short.TYPE, 1); }
}

class IntVoid extends Filter {
  public void work() {input.popInt();}
  public void init() {input = new Channel(Integer.TYPE, 1); }
}

class FloatToShort extends Filter {
  public void work() {output.pushShort((short) (input.popFloat() + 0.5f)); }
  public void init() {input = new Channel(Float.TYPE, 1);
                      output = new Channel(Short.TYPE, 1);}
}

class FloatToInt extends Filter {
  public void work() {output.pushInt((int) (input.popFloat() + 0.5f)); }
  public void init() {input = new Channel(Float.TYPE, 1);
                      output = new Channel(Integer.TYPE, 1);}
}

class ShortToFloat extends Filter {
  public void work() {short i = input.popShort();
//    System.out.println(i);
  float f = (float) i;
//    System.out.println(f);
  output.pushFloat(f); }
  public void init() {input = new Channel(Short.TYPE, 1);
                      output = new Channel(Float.TYPE, 1);}
}

