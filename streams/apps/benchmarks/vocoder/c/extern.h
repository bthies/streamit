extern float *FIRFilter1 (float *filter, float *data, int filterlen, int datalen);
extern float * Spec2Env (float *Spectral, int length);

extern void Rectangular2Polar ( Complex *dft, float * Magnitude, float *Phase, int length);
extern Complex* Polar2Rectangular ( float * Magnitude, float *Phase, int length);

extern float * short2float ( short * data, int length);
extern float * char2float ( unsigned char * data, int length);
extern short * float2short ( float * data, int length);
extern unsigned char* float2char ( float * data, int length);

extern float* interp(float* xn, int length,  int l);
extern float *decimate (float *xn, int length, int l);

extern int max(int x , int y );
extern float average(float *data, int length);
extern int *ratio2int ( float ratio);

extern Complex *DFT (float *data, int SampleNum, int *DataIdx, float *Buf, int BUFSIZE, int fftlen, Complex *LastSample, int StartFlag);
extern Complex  *AddCosWin (Complex * dftrlt1,  int fftlen, int datalen);
extern float * InvDFT1 (Complex *dftrlt, int fftlen, int datalen);
extern float * InvDFT2 (Complex *dftrlt, int fftlen, int datalen);


extern void SaveData(float *data, int datalen, char *filename);
