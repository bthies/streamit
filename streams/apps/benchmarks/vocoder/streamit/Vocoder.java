/** @author: clleger
 *
 * This is the main file for the phase VOice enCODER.
 **/
import streamit.*;
import streamit.io.*;

class IntPrinter extends Filter {
  public void work() { int i = input.popInt();
  System.out.println(i); 
    output.pushInt(i);
}
    public void init() { input = new Channel(Integer.TYPE, 1); 
    output = new Channel(Integer.TYPE, 1);}
  IntPrinter() {}
}

class ShortPrinter extends Filter {
  public void work() { short i = input.popShort();
    System.out.println(i); output.pushShort(i);}
    public void init() { input = new Channel(Short.TYPE, 1); 
    output = new Channel(Short.TYPE, 1);}
  ShortPrinter() {}
}
class FloatPrinter extends Filter {
//    String append, prepend;

  public void work() { float i = input.popFloat(); 
    System.out.println(i); 
    System.out.println("Float");
    output.pushFloat(i);}
    public void init() { input = new Channel(Float.TYPE, 1); 
    output = new Channel(Float.TYPE, 1);}
  FloatPrinter() {}
//    FloatPrinter(String append) 
//    {this.prepend = "";this.append = append;}
//    FloatPrinter(String prepend, String append) 
//    {this.prepend = prepend;this.append = append;}
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
  public static final int DFT_LENGTH = 32;
  public static final int NEW_LENGTH = 32;
  public static final int m_LENGTH = 1;
  public static final int n_LENGTH = 1;
  public static final float FREQUENCY_FACTOR = 1f;
  public static final float SPEED_FACTOR = 1f;

//    public static final int LARGE = 2147480000;
//    public static final int LARGE = 852524;
//    public static final int HEADER_S = 22; //

//    public static final String FILE_IN = "test2.wav";
//    public static final String FILE_OUT = "test3.wav";
}

class Vocoder extends Pipeline implements Constants {

  //  final int DFT_LENGTH = 100;
//    public static void main(String args[]) {
//      new Vocoder().run(args);
//    }

  public void init() {
//      add(new StepSource(6)); //add(new AudioSource());
//      add(new IntPrinter("\t(orig)\n"));
//      add(new IntToFloat());
    add(new FilterBank(DFT_LENGTH));
//      add(new HanningWindow(DFT_LENGTH));
//      add(new RectangularToPolar());
//      add(new VocoderSystem(DFT_LENGTH, NEW_LENGTH, FREQUENCY_FACTOR, SPEED_FACTOR));
//      add(new PolarToRectangular());
    add(new SumReals(NEW_LENGTH));

//      add(new FloatToShort());
//      add(new ShortPrinter("(mod)\n"));
//      add(new ShortVoid());

//      add(new FloatPrinter("(mod)\n"));
//      add(new FloatVoid());
  }
}    

class Main extends StreamIt implements Constants {
  public static void main(String args[]) {
    new Main().run(args);
  }

  public void init() {
    add(new FileReader("test2.wav", Short.TYPE));
//      add(new ShortPrinter());
//      add(new ShortToFloat());
//      add(new StepSource(100));
    add(new ShortToFloat());
      add(new FloatPrinter());

//      add(new WaveReader());
//      add(new SplitJoin() {
//  	public void init() {
//  	  setSplitter(WEIGHTED_ROUND_ROBIN(HEADER_S, LARGE));
//  	  add(new WaveHeader(FREQUENCY_FACTOR, SPEED_FACTOR));
//  	  add(new Pipeline() {
//  	      public void init() {
//  		add(new ShortToFloat());
//  		add(new SplitJoin() {
//  		    public void init() {
//  		      setSplitter(ROUND_ROBIN());

//  		      add(new Vocoder());

//  		      add(new Vocoder());
//  		      setJoiner(ROUND_ROBIN());
//  		    }
//  		  });
//  		      add(new FloatPrinter("(new)\n"));
  		add(new FloatToShort());
//  	      }
//  	    });
//  	  setJoiner(WEIGHTED_ROUND_ROBIN(HEADER_S, LARGE));
//  	}
//        });
//        add(new ShortPrinter());
      add(new FileWriter("test3.wav", Short.TYPE));
  }
}
