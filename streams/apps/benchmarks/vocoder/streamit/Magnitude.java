import streamit.library.*;
import streamit.library.io.*;

class MagnitudeStuff extends Pipeline implements Constants {
  public void init(final int DFTLen, final int newLen, final float speed) {
//      final int interpolate = (int) ((speed * 10) + 0.5f; 
    //with this uncommented and used down in the split join, it was
    //complaining about a non-constant field access, even though
    //there's no reason why it should be a field.
    if (DFTLen != newLen) {
      add(new SplitJoin() {
	  public void init() {
	    setSplitter(DUPLICATE());
	    add(new FIRSmoothingFilter(DFTLen));
  	    add(new Identity(Float.TYPE));
	    setJoiner(ROUND_ROBIN());
	  }
	});
      add(new Deconvolve());
      add(new SplitJoin() {
	  public void init() {
	    setSplitter(ROUND_ROBIN());
	    add(new Duplicator(DFT_LENGTH_REDUCED, NEW_LENGTH_REDUCED));
	    add(new Remapper(DFT_LENGTH_REDUCED, NEW_LENGTH_REDUCED));
	    setJoiner(ROUND_ROBIN());
	  }
	});
      add(new Multiplier());
    }
    if (speed != 1.0) {
      add(new SplitJoin() {
	  public void init() {
	    setSplitter(ROUND_ROBIN());
	    for(int i=0; i < DFTLen; i++) {
//  	      add(new Remapper(10,  (int) ((speed * 10) + 0.5f)));
	      add(new Remapper(n_LENGTH,  m_LENGTH));
	    }
	    setJoiner(ROUND_ROBIN());
	  }
	});
    } else {
        add(new Identity(Float.TYPE));
    }

  }

  MagnitudeStuff(final int DFTLen, final int newLen, final float speed) {
    super(DFTLen, newLen, speed);
  }
}
