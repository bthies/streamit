/*
 * First cut of Darpa PCA Demo in StreamIt.
 */

import streamit.*;


public class PCADemo extends StreamIt
{
  static public void main(String[] t)
  {
    PCADemo test = new PCADemo();
    test.run(t);
  }

  public void init()
  {
    System.out.println("Running the PCA Demo...");
    int numChannels           = 48;
    int numSamples            = 4096;
    int numBeams              = 16;
    int numCoarseFilterTaps   = 16;
    int numFineFilterTaps     = 64;
    int coarseDecimationRatio = 2;
    int fineDecimationRatio   = 2;
    int numSegments           = 1;
    int numPostDec1           = numSamples/coarseDecimationRatio;
    int numPostDec2           = numPostDec1/fineDecimationRatio;
    int mfSize                = numSegments*numPostDec2;
    int pulseSize             = numPostDec2/2;
    int predecPulseSize       = pulseSize*coarseDecimationRatio*fineDecimationRatio;
    int targetBeam            = numBeams/4;
    int targetSample          = numSamples/4;
    int targetSamplePostdec   = 1 + targetSample/coarseDecimationRatio/fineDecimationRatio;
    float dOverLambda         = 0.5f;
    float cfarThreshold       = 0.95f*dOverLambda*numChannels*(0.5f*pulseSize);

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
