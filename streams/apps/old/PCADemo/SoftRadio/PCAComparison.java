/*
 * First cut of Darpa PCA Comparison in StreamIt.
 */

import streamit.library.*;

class TestSource extends Filter{
    int c;

    public void init() {
	output = new Channel(Float.TYPE, 1);
	c = 0;
    }
    public void work() {
	output.pushFloat((float)c);
	c++;
	if (c >= 1000)
	    c= 0;
    }
}
	    
class NullSink extends Filter {
    int i;

    public void init() {
	input = new Channel(Float.TYPE, 1);
	i =0;
    }
    public void work() {
	//System.out.println(input.popFloat());
	input.popFloat();
    }
}

public class PCAComparison extends StreamIt
{
  static public void main(String[] t)
  {
    PCAComparison test = new PCAComparison();
    test.run(t);
  }

  public void init()
  {
    final int numChannels           = 1;
    final int numSamples            = 2;
    final int numBeams              = 1;
    final int numCoarseFilterTaps   = 1;
    final int numFineFilterTaps     = 1;
    final int coarseDecimationRatio = 1;
    final int fineDecimationRatio   = 1;
    final int numSegments           = 1;
    final int numPostDec1           = numSamples/coarseDecimationRatio;
    final int numPostDec2           = numPostDec1/fineDecimationRatio;
    final int mfSize                = numSegments*numPostDec2;
    final int pulseSize             = numPostDec2/2;
    final int predecPulseSize       = pulseSize*coarseDecimationRatio*fineDecimationRatio;
    final int targetBeam            = numBeams/4;
    final int targetSample          = numSamples/4;
    final int targetSamplePostdec   = 1 + targetSample/coarseDecimationRatio/fineDecimationRatio;
    final float dOverLambda         = 0.5f;
    final float cfarThreshold       = 0.95f*dOverLambda*numChannels*(0.5f*pulseSize);

    add(new TestSource());
    add(new ComplexMultiDimFir(numCoarseFilterTaps, numSamples, coarseDecimationRatio));
    add(new ComplexMultiDimFir(numFineFilterTaps, numPostDec1, fineDecimationRatio));
    add(new ComplexBeamFormer(numBeams, numChannels, numPostDec2));
    add(new ComplexMultiDimFir(mfSize, mfSize, 1));
    add(new ComplexToMag());
    add(new NullSink());
  }
}
