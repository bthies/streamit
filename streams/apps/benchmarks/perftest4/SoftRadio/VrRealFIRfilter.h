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


#ifndef _VrREALFIRFILTER_H_
#define _VrREALFIRFILTER_H_

#include <VrDecimatingSigProc.h>
#include <fstream.h>

/*  FIR filter definition:
    
    VrRealFIRfilter( cutoff freq., Num of taps, decimation factor, center frequency)
    
    cutoff (Hz) = 0.0  => LPF using Hamming window, o/w LPF transformed to have higher cutoff freq
    decimation factor => set to one unless some integer >1 specified
    center_freq (Hz) => used to specify a composite frequency-shifting filter (i.e. channel filter)
    */


// ********************************************************


template<class iType,class oType> 
class VrRealFIRfilter : public VrDecimatingSigProc<iType,oType> {
protected:
  int numTaps;
  //  iType* inputArray;
  float* taps;
  float cutoff, gain;
  void buildFilter_real();
  int FileDefined;
public: 
  virtual void work(int n);
  virtual void initialize();

  VrRealFIRfilter(float c,int t, float g);
  VrRealFIRfilter(int d,float c,int t, float g);
  VrRealFIRfilter(int d,char* file);
  ~VrRealFIRfilter();
};

template<class iType,class oType> void
VrRealFIRfilter<iType,oType>::work(int n)
{
  float result;
  printf("RealFir %d\n", n);
  
  for (int i=0;i<n;i++,
	 printf("RealFir incrementing %d\n", decimation),
	 incInput(decimation)) {
    result = 0;
    printf("RealFir requestion %d\n", numTaps);
    iType* inputArray=inputReadPtr(-numTaps+1);
    for (int j=0; j < numTaps; j++)
      result += taps[j] *  inputArray[j];
    printf("RealFir writing 1\n");
    outputWrite((oType)result);
  }
}

template<class iType,class oType> void
VrRealFIRfilter<iType,oType>::buildFilter_real(){
  int inSampFreq;
  float index, arg;
  float N = (float)numTaps;
  float M = N-1; /* filter Order */

  inSampFreq = getInputSamplingFrequency(); 
  
  // Build Real Filter
  if (cutoff == 0.0){
    
    // cut-off == 0.0 => Hamming window
    for (index=0;index < numTaps; index++) {
      taps[(int)index] =  gain*(0.54-0.46*cos(2*M_PI*index/(M)));
      //      printf("index: %f   tap: %f\n",index,taps[(int)index]);
    }
  } else {
    
    // cut-off != 0.0 => Hamming window * transform to move cutoff to correct place
    arg = 2*M_PI*cutoff/(float)inSampFreq;
    for (index=0;index < numTaps;index++) {
      if (index-(M/2) != 0){     
	taps[(int)index] =  gain*(sin(arg*(index-(M/2)))/M_PI/(index-(M/2))*(0.54-0.46*cos(2*M_PI*index/M)));
      }
      //   printf("index: %f   tap: %f\n",index,taps[(int)index]);
    }
    if ( (((int)M)/2)*2 == (int)M ){ 
      taps[(int)M/2] =  gain*arg/M_PI;
    }
  }
}


template<class iType,class oType> 
VrRealFIRfilter<iType,oType>::VrRealFIRfilter(float c,int t, float g)
  :VrDecimatingSigProc<iType, oType>(1,1),numTaps(t),cutoff(c),gain(g),FileDefined(0)
{
  
}

template<class iType,class oType> 
VrRealFIRfilter<iType,oType>::VrRealFIRfilter(int dec, float c,int t, float g)
  :VrDecimatingSigProc<iType, oType>(1,dec),numTaps(t),cutoff(c),gain(g),FileDefined(0)
{
}

template<class iType,class oType> 
VrRealFIRfilter<iType,oType>::VrRealFIRfilter(int dec,char* filename)
  :VrDecimatingSigProc<iType, oType>(1,dec),cutoff(0.0),gain(1.0),
   FileDefined(1)
{
  // First parse file to determine the numebr of taps
  ifstream file(filename);
  if (!file) {
    cout << "Failed to open file" << endl;exit(0);
  }
  numTaps = 0;
  char foo;
  while (file.get(foo)) {
    if (foo == '\n')
      numTaps++;
  }

  if (numTaps < 1) {
    cerr << "No taps defined in file\n";
    exit(1);
  }
  taps = new float[numTaps];
  file.close();
  // Re-open and read in taps
  ifstream file2(filename);
  if (!file2) {
    cout << "Failed to open file" << endl;exit(0);
  }

  char* asciiTap = new char[100];
  int i = 0;
  int j = 0;
  while (file2.get(asciiTap[i])) {
    if (asciiTap[i] == '\n') {
      taps[j] = atof(asciiTap);
      //      cout << "Tap # " << j << " = " << taps[j] << "\n";
      i = 0;j++;
    } else {
      i++;
    }
  }
  file2.close();
}

template<class iType,class oType> 
void VrRealFIRfilter<iType,oType>::initialize()
{
  setHistory(numTaps);
  if (!FileDefined) 
    taps = new float[numTaps];
  buildFilter_real();
  //inputArray = new iType[numTaps];
}

template<class iType,class oType> 
VrRealFIRfilter<iType,oType>::~VrRealFIRfilter()
{
  delete taps;
}

#endif








