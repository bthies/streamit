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

abstract class Constants extends StreamIt {
  protected static final int DFT_LENGTH = 4;
  protected static final int NEW_LENGTH = 4;
  protected static final float FREQUENCY_FACTOR = 1f;
  protected static final float SPEED_FACTOR = 1f;

//    protected static final int LARGE = 2147480000;
  protected static final int LARGE = 852524;
  protected static final int HEADER_S = 22; //

  protected static final String FILE_IN = "test2.wav";
  protected static final String FILE_OUT = "test3.wav";
}

class Vocoder extends Constants {
  //  final int DFT_LENGTH = 100;
  public static void main(String args[]) {
    new Vocoder().run(args);
  }

  public void init() {
//      add(new StepSource(6)); //add(new AudioSource());
//      add(new IntPrinter("\t(orig)\n"));
//      add(new IntToFloat());
    add(new FilterBank(DFT_LENGTH));
    add(new HanningWindow(DFT_LENGTH));
    add(new RectangularToPolar());
    add(new VocoderSystem(DFT_LENGTH, NEW_LENGTH, FREQUENCY_FACTOR, SPEED_FACTOR));
    add(new PolarToRectangular());
    add(new SumReals(NEW_LENGTH));

//      add(new FloatToShort());
//      add(new ShortPrinter("(mod)\n"));
//      add(new ShortVoid());

//      add(new FloatPrinter("(mod)\n"));
//      add(new FloatVoid());
  }
}    

class Main extends Constants {
  public static void main(String args[]) {
    new Main().run(args);
  }

  public void init() {
    add(new FileReader(FILE_IN, Short.TYPE));
    add(new WaveReader());
    add(new SplitJoin() {
	public void init() {
	  setSplitter(WEIGHTED_ROUND_ROBIN(HEADER_S, LARGE));
	  add(new WaveHeader(FREQUENCY_FACTOR, SPEED_FACTOR));
	  add(new Pipeline() {
	      public void init() {
		add(new ShortToFloat());
		add(new SplitJoin() {
		    public void init() {
		      setSplitter(ROUND_ROBIN());
		      add(new Vocoder());
		      add(new Vocoder());
		      setJoiner(ROUND_ROBIN());
		    }
		  });
		add(new FloatToShort());
	      }
	    });
	  setJoiner(WEIGHTED_ROUND_ROBIN(HEADER_S, LARGE));
	}
      });
    add(new ShortPrinter());
    add(new FileWriter(FILE_OUT, Short.TYPE));
  }
}
