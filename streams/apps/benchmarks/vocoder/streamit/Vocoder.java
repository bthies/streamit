/** @author: clleger
 *
 * This is the main file for the phase VOice enCODER.
 **/
import streamit.library.*;
import streamit.library.io.*;

class VocodeSystem extends SplitJoin
{
  public void init(int DFTLen, int newLen, float c, float speed) {
    setSplitter(ROUND_ROBIN());

    add(new MagnitudeStuff(DFTLen, newLen, speed));
    add(new PhaseStuff(DFTLen, newLen, c, speed));

    setJoiner(ROUND_ROBIN());
  }

  VocodeSystem(int DFTLen, int newLen, float c, float speed) {
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
  public static final int DFT_LENGTH_NOM = 28; //
  public static final int DFT_LENGTH = DFT_LENGTH_NOM/2+1; //
//    public static final int DFT_LENGTH = DFT_LENGTH_NOM+1; //
  public static final float FREQUENCY_FACTOR_ARGS[] = {1f, 1f, 1f, 1.8f, 0.6f};
  public static final float FREQUENCY_FACTOR = 0.6f;
  public static final float GLOTTAL_EXPANSION_ARGS[] = {1f, 1f, 1f, 1.2f, 1/1.2f};
  public static final float GLOTTAL_EXPANSION = 1/1.2f;
  public static final int NEW_LENGTH = (int) (DFT_LENGTH * GLOTTAL_EXPANSION / FREQUENCY_FACTOR);


  //DFT_LENGTH_RED and NEW_LENGTH_RED correspond to the reduced ratio
  //of DFT_LENGTH to NEW_LENGTH.  This ratio is needed to avoid
  //interpolating and then decimating by redundant amounts.  Normally
  //these numbers could be calculated by taking the GCD of DFT_L and
  //NEW_L, but the loop unroller is having trouble with it, so they
  //currently need to be set by hand.

  //NOTE: it's very important that NEW_LENGTH_REDUCED * DFT_LENGTH is
  //a multiple of DFT_LENGTH_REDUCED.  Otherwise the decimation will
  //not finish completely each window, and the windows will no longer
  //be distinct.
  public static final int DFT_LENGTH_REDUCED_ARGS[] = {1,1,1,3,3};
  public static final int DFT_LENGTH_REDUCED = 3;
  public static final int NEW_LENGTH_REDUCED_ARGS[] = {1,1,1,2,4};
  public static final int NEW_LENGTH_REDUCED = 4;

  public static final float SPEED_FACTOR_ARGS[] = {1f, 2f, 0.5f, 1f, 1f};
  public static final float SPEED_FACTOR = 1f;

  //n_LENGTH and m_LENGTH are similar to DFT_LENGTH_REDUCED and
  //NEW_LENGHT_REDUCED above.  The difference is that these should be
  //the reduced ratio of SPEED_FACTOR.  So if SPEED_FACTOR is 2,
  //m_LENGTH should be 2, and n_LENGTH should be 1.  If SPEED_FACTOR
  //is 2.5, m_LENGTH should be 5, and n_LENGTH should be 2.
  public static final int n_LENGTH_ARGS[] = {1,1,2,1,1};
  public static final int n_LENGTH = 1;
  public static final int m_LENGTH_ARGS[] = {1,2,1,1,1};
  public static final int m_LENGTH = 1;

}

class Vocoder extends Pipeline implements Constants {


  public void init() {
    add(new FilterBank(DFT_LENGTH_NOM));

    add(new RectangularToPolar());

    add(new VocodeSystem(DFT_LENGTH, NEW_LENGTH, FREQUENCY_FACTOR, SPEED_FACTOR));


    add(new PolarToRectangular());
//      add(new FloatPrinter());
    add(new SumReals(NEW_LENGTH));

  }
}
