#include "main.h"
#ifdef RAW
#include "common.h"
#include "raw.h"
#endif

#define BUFSIZE   256
/*  #define BUFSIZE   512 */

#ifdef RAW
int full_count = 0;
const int BLOCK_SIZE =  256;
const int RUN_SIZE   = 768;
/*  const int BLOCK_SIZE =  524288; */
/*  const int RUN_SIZE   = 1048576; */
/*  const int RUN_SIZE   = 2097152; */
#endif

float *LastMag, *LastPha;

struct Complex {
  float rl, im;
};

void vocoder (float *data, int fftlen, int SampleNum, char Command, float Parameter, WAVHDR *Hdr, int DownSampleFlag, FILE *fout)
{
  int fftlen2, fftlen4;
  Complex *LastSample, *dftrec, *dft, *dft2; 
  float Buf[BUFSIZE], *Recon;
  int DataIdx, BufIdx, LastDataIdx, fftnum , datalen;
  int  i, j, StartFlag ;
  float *magnitude, *phase;
  int BufCount;
#ifndef RAW
  time_t t1, t2, t3;
#endif //RAW

  /* Calculate Constants Needed in Iterative DFT, based on DFTRes */
  fftlen2 = fftlen/2;

  
  DataIdx = fftlen2;
  

  //  printf("Command = %c, Parame = %f\n ",Command, Parameter); 
#ifdef RAW 
  print_int(4);
#endif //RAW
  LastSample = (Complex *) malloc (fftlen2*sizeof(Complex));
  
  memset(Buf, 0, (BUFSIZE-fftlen2)*sizeof(float));
  memcpy (Buf+BUFSIZE-fftlen2, data, fftlen2*sizeof(float));
  memset(LastSample, BUFSIZE*fftlen2*sizeof(Complex), 0);

  if (DownSampleFlag) 
    {
      magnitude = (float *) malloc (BUFSIZE*fftlen*sizeof(float)/4);
      phase = (float *) malloc (BUFSIZE*fftlen*sizeof(float)/4);
    }
  else
    {
      magnitude = (float *) malloc (BUFSIZE*fftlen*sizeof(float)/2);
      phase = (float *) malloc (BUFSIZE*fftlen*sizeof(float)/2);
    };

  BufCount = 0;
  while (DataIdx < SampleNum+fftlen2 )
    {
      LastDataIdx = DataIdx;

#ifndef RAW
      t2 = t1 = time(NULL);
#endif

#ifdef RAW 
  print_int(5);
#endif //RAW
      dftrec = DFT (data, SampleNum, &DataIdx, Buf, BUFSIZE, fftlen, LastSample, BufCount);
      datalen = DataIdx - LastDataIdx; 
#ifdef RAW 
  print_int(6);
#endif //RAW
      dft = AddCosWin ( dftrec, fftlen, datalen);
      free(dftrec);
     
	      

      fftnum = fftlen2;
      if (DownSampleFlag)
	{
	  dft2 = (Complex *)malloc (BUFSIZE*fftlen2/2*sizeof(Complex));
	  for (j = 0; j < datalen; j++)
	    for (i = 0 ; i < fftlen2/2; i++)
	      {
		dft2[j*fftlen2/2+i].rl = dft[j*fftlen2+2*i+1].rl;
		dft2[j*fftlen2/2+i].im = dft[j*fftlen2+2*i+1].im;
	      }
	  free(dft);
	  dft = dft2;
	  fftnum /= 2;
	};
      /*
      magnitude = DFT2Spectral(dft, fftnum*datalen);
      phase = DFT2Phase (dft, fftnum*datalen);
      */

#ifdef RAW 
  print_int(7);
#endif //RAW
      Rectangular2Polar ( dft, magnitude, phase, fftnum*datalen);
#ifdef RAW 
  print_int(71);
#endif //RAW
      //SaveData(phase, datalen*fftnum, "pha0.dat");
      if (Command != 'f' ) 
	unwrap1(phase, datalen, fftnum, fftlen, BufCount, LastPha, DownSampleFlag);

      //SaveData(phase, datalen*fftnum, "pha1.dat");

      switch (Command) {
      case 's' :
#ifdef RAW 
  print_int(8);
#endif //RAW
 Recon = speedup ( magnitude, phase, fftnum, fftlen, &datalen, Parameter, BufCount, LastMag, LastPha, DownSampleFlag); 
#ifdef RAW 
  print_int(81);
#endif //RAW

break;
      case 'm' :
      case 'f' : Recon = Conv ( magnitude, phase, fftnum, fftlen, datalen, Command, BufCount, DownSampleFlag); break;
      case 'p' : Recon = Pitch ( magnitude, phase, fftnum, fftlen, datalen, BufCount, DownSampleFlag); break;
      default :  Recon = InvDFT2 (dft, fftnum, datalen); ; break;
      };

#ifdef RAW 
  print_int(9);
#endif //RAW
      free(dft);
#ifndef RAW
      if (Recon)
	WriteWaveFile(Recon, datalen, Hdr, fout);
#else
      print_int(0xdeadcafe);
      if (Recon) {
	int i=0;
/*  	for(i = 0; i < datalen; i++)  */
/*  	  print_int((int) (Recon[i] + 0.5)); */
	print_string("done\n");
	full_count += datalen;
	while (full_count >= BLOCK_SIZE) {
	  full_count -= BLOCK_SIZE;
	}
      }
#endif //RAW
      free(Recon);

      BufCount ++;
#ifdef RAW 
  print_int(10);
#endif //RAW
      datalen = DataIdx - LastDataIdx; 
      for (i = 0 ; i < fftnum; i++)
	{
	  LastPha[i] = phase[(datalen-1)*fftnum+i];
	  LastMag[i] = magnitude[(datalen-1)*fftnum+i];
	};
      /*
      free(magnitude);
      free(phase);
      */
    }

#ifdef RAW 
  print_int(11);
#endif //RAW
/*    free(LastSample); */
  return;
}

#ifdef RAW
float *GenerateData(int length) {
  int i=0, up = 1;
  float j=0;
  float *data = (float *)malloc(length * sizeof(float));
  for(i = 0; i < length; i++) {
    data[i] = j;
    if (j == 100) {
      up = 0;
    } else if (j == 0) {
      up = 1;
    }
    if (up == 1) {
      j++;
    } else {
      j--;
    }
  }
  return data;
}
#endif //RAW

int  main(argc, argv)
     int argc;
     char *argv[]; 
     /* Usage: vocoder infile outfile fftlen */
{
  FILE *fsrc, *fout;
  int fftlen, SampleNum;
  float *data;
  WAVHDR WavHdr;
  float TimeRatio;
  char Command;
  int DownSampleFlag = 0 ;
#ifndef RAW
  time_t t1, t2;

  t1 = time(NULL);
#endif

  /*argument 1: input file name */
#ifndef RAW
  fsrc = fopen((char *)(argv[1]), "rb");
  data = ReadWaveFile(&WavHdr,  &SampleNum, fsrc);
  fclose(fsrc);
#else
  print_int(0);
/*    print_int(0); //DEBUG! */
  SampleNum = RUN_SIZE;
  data = GenerateData(SampleNum); 
  print_int(1);
/*    print_int(1); //DEBUG! */
#endif //RAW

  /*argument 2: output file name */
#ifndef RAW
  fout = fopen((char *)(argv[2]), "w");
  fwrite((&WavHdr), sizeof(WAVHDR), 1, fout);
#endif //RAW  

  /*argument 3: DFT Resolution */
#ifndef RAW
  sscanf((char *)(argv[3]), "%d", &fftlen);
#else
  fftlen = 28;
#endif //RAW

  /*argument 4: downsample flag */
#ifndef RAW
  sscanf((char *)(argv[4]), "%d", &DownSampleFlag);
#else
  DownSampleFlag = 0;
#endif //RAW
  
  /*argument 5: option         */
  /*-s: time change            */
  /*-m: male to female change  */
  /*-f: female to male change  */
#ifndef RAW
  if (argc >= 6) 
    sscanf((char *)(argv[5]), "%c%c", &Command, &Command);
#else
  Command = 'f';
#endif //RAW

#ifndef RAW
  if (argc >= 7 )
    if (Command == 's')
      sscanf((char*)(argv[6]), "%f", &TimeRatio);
    else TimeRatio = 0;
#else
  TimeRatio = 0;
  print_int(2);
/*    print_int(2); */
#endif //RAW

  if (DownSampleFlag)
    {
      
      InitSpeed (fftlen/4);
      InitConv (fftlen/4);
      InitPitch (fftlen/4);
      LastMag = (float *) malloc(fftlen/4*sizeof(float));
      LastPha = (float *) malloc (fftlen/4*sizeof(float));

    }
  else
    {
      
      InitSpeed (fftlen/2);
      InitConv (fftlen/2);
      InitPitch (fftlen/2);
      LastMag = (float *) malloc(fftlen/2*sizeof(float));
      LastPha = (float *) malloc (fftlen/2*sizeof(float));

    }

  vocoder (data, fftlen, SampleNum, Command, TimeRatio, &WavHdr, DownSampleFlag, fout);

#ifdef RAW
  print_int(100);
#endif //RAW
  CleanSpeed();
  CleanConv();
  CleanPitch();
  free(data);

  /*SetSampleRate(fout, 16000);*/
#ifndef RAW
  WaveFileClose(fout);
#endif //RAW
  free(LastMag);
  free(LastPha);

#ifndef RAW
  t2 = time(NULL);

  printf ( "Total Running Time = %f\n", difftime(t2, t1));
#else
  print_int(-1);
#endif

  return 0;
}
