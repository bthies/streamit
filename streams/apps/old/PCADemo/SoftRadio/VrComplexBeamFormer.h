#ifndef _VRCOMPLEXBEAMFORMER_H_
#define _VRCOMPLEXBEAMFORMER_H_

#include <VrSigProc.h>
#include <math.h>

class VrComplexBeamFormer : public VrSigProc<float, float> 
{
protected:
  int numberOfBeams;
  int numberOfChannels;
  int numberOfSamples;
  float* beamFormingWeights;
  float* inputData;
  int currentPosition;
public:
  VrComplexBeamFormer(int nBeams, int nChannels, int nSamples):
    numberOfBeams(nBeams), numberOfChannels(nChannels), 
    numberOfSamples(nSamples), currentPosition(0)
    {
      beamFormingWeights = new float[numberOfBeams*numberOfChannels*2];
      inputData          = new float[numberOfChannels*numberOfSamples*2];
    }
  
  virtual void work(int n) 
    {
      for (int p = 0; p < n; p++) { 
        int i, j, k;
	
	inputData = inputReadPtr(1-(numberOfChannels*numberOfSamples*2));
	incInput(numberOfChannels*numberOfSamples*2);
	
	for (i = 0;  i < numberOfBeams; i++) {
	  for (j = 0; j < numberOfSamples*2; j += 2) {
	    float outReal = 0;
	    float outImag = 0;
	    for (k = 0; k < numberOfChannels*2; k += 2) {
	      outReal += (beamFormingWeights[i*numberOfChannels*2+k] *
			  inputData[k*numberOfSamples+j] -
			  beamFormingWeights[i*numberOfChannels*2+k+1] *
			  inputData[k*numberOfSamples+j+1]);
	      outImag += (beamFormingWeights[i*numberOfChannels*2+k] *
			  inputData[k*numberOfSamples+j+1] +
			  beamFormingWeights[i*numberOfChannels*2+k+1] *
			  inputData[k*numberOfSamples+j]);
	    }
	    outputWrite(outReal);
	    outputWrite(outImag);
	  }
	}
      }
    }
  
      
  virtual void initialize() 
    {
      setHistory(numberOfChannels*numberOfSamples*2);
    }
};


#endif
