import streamit.*;
import streamit.io.*;

class DFTFilter extends Filter
{
  float o[];
  int DFTLen;
  float range;

  public void work() {

    output.pushFloat(input.popInt());
  }

  public void init(int DFTLen, float range) {
    this.DFTLen = DFTLen;
    this.range = range;

    //need to peek DFTLen ahead of current one
    input = new Channel(Integer.TYPE, 1, DFTLen);
    output = new Channel(Float.TYPE, 1);
  }

  public DFTFilter(int DFTLen, float range) {
    super(DFTLen, range);
  }
}

class FilterBank extends SplitJoin {
  public void init(int channels) {
    setSplitter(DUPLICATE());

    for(int k=0; k < channels; k++) {
      //this filter is for the kth range
      float range = (float)(2 * Math.PI * k)/channels;
      add(new DFTFilter(channels,range));
    }

    //send real and imaginary parts together
    //that means it should have a weight of 2.  I'm testing to see if
    //that's what's breaking streamit
    setJoiner(ROUND_ROBIN());
  }

  FilterBank(int channels) {
    super(channels);
  }

}
