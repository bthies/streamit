#ifndef _BANDPASSFILTER_H_
#define _BANDPASSFILTER_H_

#include <VrSigProc.h>
#include "VrLowParsFilter.h"

class BandPassFilter {
protected:
  VrSigProc<float, float>* inFilter, outFilter;
  VrLowPassFilter* lowpass1;
  VrLowPassFilter* lowpass2;
  float sampleRate, lowFreq, highFreq;
  int numTaps;
  float gain;

public:
  BandPassFilter(VrSigProc<float, float>* in, VrSigProc<float, float>* out,
		 float sample, float low, float high, 
		 int taps, float amountGain):
    inFilter(in), outFilter(out), sampleRate(sample), lowFreq(low), 
    highFreq(high), numTaps(taps), gain(amountGain)
    {
      lowpass1 = new VrLowPassFilter(sampleRate, highFreq, numTaps, 0);
      lowpass2 = new VrLowPassFilter(sampleRate, highFreq, numTaps, 0);
      
