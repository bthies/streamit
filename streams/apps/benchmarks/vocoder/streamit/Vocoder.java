/** @author: clleger
 *
 * This is the main file for the phase VOice enCODER.
 **/
import streamit.*;
import streamit.io.*;

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
  //For this system, DFT_LENGTH_NOM is the nominal number of DFT
  //coefficients to use when taking the DFT.  Thus the behaviour of
  //the system is that there are DFT_LENGTH_NOM filters between 
  //[0, 2 * pi).  

  //This code assumes that the DFT_LENGTH_NOM is even, so that the
  //range (pi, 2 * pi) is just a reflection of the range (0, pi).
  //This is because the input signal is real and discrete;
  //discreteness means the fourier transform is periodic with 2 * pi,
  //and since the signal is real the magnitude of the DFT will be even
  //and the phase odd.  Since we only care about the real output of
  //the system, and are only doing the inverse DFT for a single sample
  //at the center of the window, the phase being odd makes no
  //difference.  Thus with filters in the range [0, pi], the entire
  //fourier transform can be represented, thus using approximately
  //half the filters and computation.

  /** DFT_LENGTH_NOM numbers:
   *            
   *         4: can tell when someone is talking, but not recognize
   *            that it's a voice unless you already know
   *  
   *         8: can tell that it's a person talking, if you already
   *            know the script, you can follow the voice
   *
   *        16: can tell that it's a person, can understand the words,
   *            can kind of see that the vocoder is doing something 
   *            that may be appropriate
   *
   *        32: better output; less grainy, more believable
   *        64: still better output
   *
   *       128: probably the high-point of good output
   *            vs. computation * and size.  With 128, it'll tradeof
   *            quality in output for * time.
   **/

//    public static final int DFT_LENGTH = 128; //
  public static final int DFT_LENGTH_NOM = 8; //
  public static final int DFT_LENGTH = DFT_LENGTH_NOM/2+1; //
//    public static final int DFT_LENGTH = DFT_LENGTH_NOM+1; //
  public static final float FREQUENCY_FACTOR = 1f;
  public static final float GLOTTAL_EXPANSION = 1f;
  public static final int NEW_LENGTH = (int) (DFT_LENGTH * GLOTTAL_EXPANSION / FREQUENCY_FACTOR);
//    public static final int FILE_SIZE = 1906732;
  public static final float SPEED_FACTOR = 2f;
  //i have no idea what's going on, i think i'm using these for speed
  public static final int n_LENGTH = 1; //dft_length
  public static final int m_LENGTH = 2; //new_length

//    public static final int LARGE = 2147480000;
//    public static final int LARGE = 852524;
//    public static final int HEADER_S = 22; //

//    public static final String FILE_IN = "test2.wav";
//    public static final String FILE_OUT = "test3.wav";
}

class Vocoder extends Pipeline implements Constants {


  public void init() {
    add(new FilterBank(DFT_LENGTH_NOM));

    // adding the hanning window breaks the output when doing something
    // other than the identity.  very weird
//      add(new HanningWindow(DFT_LENGTH));
    // the hanning window is not necessary for the correctness, however.
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
