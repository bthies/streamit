/*
 * First cut of Darpa PCA Demo in StreamIt.
 */

import streamit.library.*;


public class PCADemo extends StreamIt
{
  static public void main(String[] t)
  {
    PCADemo test = new PCADemo();
    test.run(t);
  }

  public void init()
  {
    final int numChannels           = 48;
    final int numSamples            = 4096;
    final int numBeams              = 16;
    final int numCoarseFilterTaps   = 16;
    final int numFineFilterTaps     = 64;
    final int coarseDecimationRatio = 2;
    final int fineDecimationRatio   = 2;
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

    System.out.println("Running the PCA Demo...");
    add(new DataSource(numChannels, numSamples, numBeams,
		       targetBeam, targetSample, predecPulseSize));
    add(new ComplexMultiDimFir(numCoarseFilterTaps, numSamples, coarseDecimationRatio));
    add(new ComplexMultiDimFir(numFineFilterTaps, numPostDec1, fineDecimationRatio));
    add(new ComplexBeamFormer(numBeams, numChannels, numPostDec2));
    add(new ComplexMultiDimFir(mfSize, mfSize, 1));
    add(new ComplexToMag());
    add(new TestDetector(numBeams, numSamples, targetBeam,
			 targetSample, cfarThreshold));
  }
}
