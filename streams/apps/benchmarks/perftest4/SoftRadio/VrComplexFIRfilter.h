/* -*- Mode: c++ -*- 
 *
 *  Copyright 1997 Massachusetts Institute of Technology
 * 
 *  Permission to use, copy, modify, distribute, and sell this software and its
 *  documentation for any purpose is hereby granted without fee, provided that
 *  the above copyright notice appear in all copies and that both that
 *  copyright notice and this permission notice appear in supporting
 *  documentation, and that the name of M.I.T. not be used in advertising or
 *  publicity pertaining to distribution of the software without specific,
 *  written prior permission.  M.I.T. makes no representations about the
 *  suitability of this software for any purpose.  It is provided "as is"
 *  without express or implied warranty.
 * 
 */


#ifndef _VrCOMPLExFIRFILTER_H_
#define _VrCOMPLExFIRFILTER_H_

#include <VrDecimatingSigProc.h>


/*  FIR filter definition:

    VrComplexFIRfilter(N, Num of taps[], decimation factor, center frequency[], gain[])

    N = number of filters
    decimation factor => set to one unless some integer >1 specified
    cutoff (Hz) = 0.0  => LPF using Hamming window, o/w LPF transformed to have higher cutoff freq
    center_freq (Hz) => used to specify a composite frequency-shifting filter (i.e. channel filter)
    gain = the gain
*/

// ********************************************************

template<class iType> 
class VrComplexFIRfilter : public VrDecimatingSigProc<iType,VrComplex> {
protected:
  int *numTaps, num_ch;
  VrComplex** taps;
  VrComplex *phase_correction, *phase_corr_incr;
  long time;
  float *center_freq, *gain;
  void buildFilter_complex(int);

public: 
  /*
  virtual void skip(int n) { 
    if(num_ch>1) {
      //until skip works right with multiple outputBuffers, we just call work
      work(n);
    } else
      VrDecimatingSigProc<iType,VrComplex>::skip(n);
  }
  */
  virtual void work(int n);
  virtual void initialize();
  int setCenter_Freq(int, float);
  int setCenter_Freq(float);
  int setNumber_Taps(int,int);
  int setNumber_Taps(int);
  VrComplexFIRfilter(int n, int d, const int t[], const float f[], 
		     const float g[]);
  VrComplexFIRfilter(int d, int t,float f, float g);
  ~VrComplexFIRfilter();
};

template<class iType> void
VrComplexFIRfilter<iType>::work(int n)
{
  VrComplex result = 0;
  int ch_num = 0;

  for (int i=0;i<n;i++,incInput(decimation)) {
    result = 0;

    for (ch_num =0; ch_num<num_ch; ch_num++){
      //make input pointer local
      iType *inputArray = inputReadPtr(-numTaps[ch_num]+1);

      VrComplex *taps_tmp = taps[ch_num];
      for (int j=0; j < numTaps[ch_num]; j++)
	result += taps_tmp[j] * inputArray[j];     
   
      // Perform phase correction (non-trivial only for freq-xlating filter)
      if (center_freq[ch_num] != 0.0) {
	phase_correction[ch_num] *= phase_corr_incr[ch_num];
	result *= phase_correction[ch_num];
      }
      outputWriteN(ch_num,result);
    }
  }
  
}

template<class iType> void
VrComplexFIRfilter<iType>::buildFilter_complex(int ch){
  int inSampFreq;
  int index;
  float N = numTaps[ch];
  float M = N-1; /* filter Order */

  inSampFreq = getInputSamplingFrequency(); 
  time = 0;
  
  if (center_freq[ch] == 0.0){

      // Build Complex Filter => 
      //            produces a low-pass filter using a real Hamming window

      for ( index=0 ; index < numTaps[ch] ; index++) {
       taps[ch][index] = gain[ch]*VrComplex((0.54-0.46*cos(2*M_PI*index/(M))));
    }
    
  } else {

    // Build composite Complex Filter => adds freq-shifting part

    float arg = 2*M_PI*center_freq[ch] / (float)inSampFreq;
    for ( index=0 ; index < numTaps[ch] ; index++) {

      taps[ch][index] = VrComplex(gain[ch]*cos(arg*index)*(0.54-0.46*cos(2*M_PI*index/(M))),
         gain[ch]*(-1)*sin(arg*index)*(0.54-0.46*cos(2*M_PI*index/(M))));

    }
    phase_corr_incr[ch] = VrComplex(cos(arg*(float)decimation),
				(-1)*sin(arg*(float)decimation));
  }

}


template<class iType> 
VrComplexFIRfilter<iType>::VrComplexFIRfilter(int n, int dec, const int t[], 
					      const float freq[], 
					      const float g[])
  :VrDecimatingSigProc<iType,VrComplex>(n,dec), num_ch(n)
{

  numTaps=new int[num_ch];
  phase_correction = new VrComplex[num_ch];
  phase_corr_incr = new VrComplex[num_ch];
  center_freq = new float[num_ch];
  gain = new float[num_ch];
  taps = new (VrComplex *[num_ch]);

  
  for (int i=0; i<num_ch; i++){
    numTaps[i] = t[i];
    phase_correction[i] = VrComplex(1,0);
    phase_corr_incr[i] = VrComplex(1,0);
    center_freq[i] = freq[i];
    gain[i] = g[i];
  }

}

template<class iType> 
VrComplexFIRfilter<iType>::VrComplexFIRfilter(int dec,int t,float freq, float g)
  :VrDecimatingSigProc<iType,VrComplex>(1,dec), num_ch(1)
{

  numTaps=new int[num_ch];
  phase_correction = new VrComplex[num_ch];
  phase_corr_incr = new VrComplex[num_ch];
  center_freq = new float[num_ch];
  gain = new float[num_ch];
  taps = new (VrComplex *[num_ch]);

  
  numTaps[0] = t;
  phase_correction[0] = VrComplex(1,0);
  phase_corr_incr[0] = VrComplex(1,0);
  center_freq[0] = freq;
  gain[0] = g;

}

template<class iType> 
void VrComplexFIRfilter<iType>::initialize()
{
  for (int i=0; i<num_ch; i++){ 
    taps[i]=new VrComplex[numTaps[i]];
    buildFilter_complex(i);
  }

  //Set history
  int max_numTaps = 0;
  for (int i=0; i<num_ch; i++){
    if (numTaps[i] > max_numTaps) max_numTaps = numTaps[i];
  }
  setHistory(max_numTaps);
}

template<class iType> 
int VrComplexFIRfilter<iType>::setCenter_Freq(int ch, float cf)
{
  center_freq[ch] = cf;
  buildFilter_complex(ch);
  return 1;
}

template<class iType> 
int VrComplexFIRfilter<iType>::setCenter_Freq(float cf)
{
  return setCenter_Freq(0,cf);
}

template<class iType> 
int VrComplexFIRfilter<iType>::setNumber_Taps(int ch, int numT)
{
  numTaps[ch] = numT;
  delete taps[ch];
  taps[ch]=new VrComplex[numTaps[ch]];

  //set history
  int max_numTaps = 0;
  for (int i=0; i<num_ch; i++){
    if (numTaps[i] > max_numTaps) max_numTaps = numTaps[i];
  }
  setHistory(max_numTaps);

  buildFilter_complex(ch);
  return 1;
}

template<class iType> 
int VrComplexFIRfilter<iType>::setNumber_Taps(int numT)
{
  return setNumber_Taps(0, numT);
}

template<class iType> 
VrComplexFIRfilter<iType>::~VrComplexFIRfilter()
{
  
  for (int i=0; i<num_ch; i++){ 
    delete taps[i];
  }
  delete numTaps;
  delete [] phase_correction;
  delete [] phase_corr_incr;
  delete center_freq;
  delete gain;
  delete taps;

}



#endif



















