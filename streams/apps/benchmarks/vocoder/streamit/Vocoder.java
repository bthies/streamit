/** @author: clleger
 *
 * This is the main file for the phase VOice enCODER.
 **/
import streamit.*;
import streamit.io.*;

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

class VocoderSystem extends SplitJoin
{
  public void init(int DFTLen, int newLen, float c, float speed) {
    setSplitter(ROUND_ROBIN());

    add(new MagnitudeStuff(DFTLen, newLen, speed));
    add(new PhaseStuff(DFTLen, newLen, c, speed));

    setJoiner(ROUND_ROBIN());
  }

  VocoderSystem(int DFTLen, int newLen, float c, float speed) {
    super(DFTLen, newLen, c, speed);
  }
}

interface Constants {
  // in my system, i take the DFT_LENGTH and use that number of
  // samples for the range [0, PI].  In the reference system, he takes
  // the DFT_LENGTH to be the number of samples for the range [0, 2 *
  // PI], then uses only half of the value for the range [0, PI].  In
  // my system, i multiply by two twice; he divides by two countless
  // number of times.

//    public static final int DFT_LENGTH = 128; //
  public static final int DFT_LENGTH_NOM = 4; //
  public static final int DFT_LENGTH = DFT_LENGTH_NOM/2+1; //
//    public static final int DFT_LENGTH = DFT_LENGTH_NOM+1; //
//    public static final int NEW_LENGTH = 64; //
  public static final float FREQUENCY_FACTOR = 1f;
  public static final float GLOTTAL_EXPANSION = 1f;
  public static final int NEW_LENGTH = (int) (DFT_LENGTH * GLOTTAL_EXPANSION / FREQUENCY_FACTOR);
//    public static final int FILE_SIZE = 1906732;
  public static final float SPEED_FACTOR = 2f;
  //i have no idea what's going on, i think i'm using these for speed
  public static final int n_LENGTH = 1; //dft_length
  public static final int m_LENGTH = 1; //new_length

//    public static final int LARGE = 2147480000;
//    public static final int LARGE = 852524;
//    public static final int HEADER_S = 22; //

//    public static final String FILE_IN = "test2.wav";
//    public static final String FILE_OUT = "test3.wav";
}

class Vocoder extends Pipeline implements Constants {


  public void init() {
    add(new FilterBank(DFT_LENGTH_NOM));
//      add(new HanningWindow(DFT_LENGTH));
    add(new RectangularToPolar());

    add(new VocoderSystem(DFT_LENGTH, NEW_LENGTH, FREQUENCY_FACTOR, SPEED_FACTOR));


    add(new PolarToRectangular());
//      add(new FloatPrinter());
    add(new SumReals(NEW_LENGTH));

  }
}    

class Main extends StreamIt implements Constants {
  public static void main(String args[]) {
    new Main().run(args);
  }

  public void init() {
//      add(new FileReader("test2.wav", Short.TYPE));
//  //      add(new ShortPrinter());
//      add(new ShortToFloat());

    add(new StepSource(100));
    add(new IntToFloat());
    add(new Delay(DFT_LENGTH_NOM));
//      add(new Delay(DFT_LENGTH*2));


    add(new Vocoder());

    add(new InvDelay((DFT_LENGTH - 2) * m_LENGTH / n_LENGTH));
//      add(new InvDelay(DFT_LENGTH * m_LENGTH / n_LENGTH));

    /**
    add(new TransformBank(8, 8));
//      add(new TransformBank(100*m_LENGTH / n_LENGTH, 200));
//      add(new RectangularToPolar());
//      add(new ComplexPrinter(100 * m_LENGTH / n_LENGTH));
//      add(new FloatPrinter());
    add(new FloatVoid());

    /**/
    add(new FloatToShort());

//      add(new Timer(476672)); //total number of samples
//      add(new Timer(524288));
    add(new ShortPrinter());
    add(new ShortVoid());

//        add(new FileWriter("test3.wav", Short.TYPE));
       /**/
  }
}
