import streamit.*;
import streamit.io.*;

class MagnitudeStuff extends Pipeline {
  public void init(final int DFTLen, final int newLen, final float speed) {
    final int interp = (int) Math.round(speed * 10);
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
	  add(new Duplicator(DFTLen, newLen));
	  add(new Remapper(DFTLen, newLen));
	  setJoiner(ROUND_ROBIN());
	}
      });
    add(new Multiplier());
    add(new SplitJoin() {
	public void init() {
	  setSplitter(ROUND_ROBIN());
	  for(int i=0; i < DFTLen; i++) {
	    add(new Remapper(10, interp));
	  }
	  setJoiner(ROUND_ROBIN());
	}
      });
  }

  MagnitudeStuff(final int DFTLen, final int newLen, final float speed) {
    super(DFTLen, newLen, speed);
  }
}
