#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#include <string.h>
#include <time.h>
#include "wave.h"
#include "dft.h"
#include "extern.h"

extern float * speedup ( float* magnitude, float *phase, int fftnum, int fftlen, int* length,  float speedfactor, int BufCount, float *lastmag, float * lastpha, int DownSample);
extern float *Conv ( float *magnitude, float *phase, int fftnum, int fftlen, int datalen, char Command, int BufCount, int DownSample);
extern float *Pitch ( float *magnitude, float *phase, int fftnum, int fftlen, int datalen, int BufCount, int DownSample);
extern float *ReadWaveFile(WAVHDR *WavHdr, int *SampleNum, FILE *fp);
extern void WriteWaveFile ( float * Data, int datalen, WAVHDR *Hdr,  FILE *fp);
extern void SetSampleRate ( FILE *fp, int SampleRate);
extern void WaveFileClose( FILE *fp);

extern void  InitConv ( int fftnum);
extern void CleanConv();
extern void InitSpeed(int fftnum);
extern void CleanSpeed();
extern void InitPitch(int fftnum);
extern void CleanPitch();
extern void unwrap1(float *phase, int datalen, int fftnum, int fftlen, int BufCount, float *lastpha, int DownSample);


