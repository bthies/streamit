/*
 * First cut of Darpa PCA Demo in StreamIt.
 */

import streamit.*;

public class PCADemo extends StreamIt
{
  static public void main(String[] t)
  {
    PCADemo test = new PCADemo();
    test.run();
  }

  public void init()
  {
    int numChannels         = 48;
    int numSamples          = 4096;
    int numBeams            = 16;
    int numCoarseFilterTaps = 64;
    int numFineFilterTaps   = 16;

    add(new DataSource(numChannels, numSamples));
    add(new FirFilter(numCoarseFilterTaps));
    add(new FirFilter(numFineFilterTaps));
    add(new BeamFormer(numBeams, numChannels, numSamples));
    add(new FirFilter(numSamples));
    add(new TestDetector(numChannels*numSamples));
  }
}
