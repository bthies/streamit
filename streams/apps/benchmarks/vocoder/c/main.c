#include "main.h"

#define BUFSIZE   512

float *LastMag, *LastPha;

void vocoder (float *data, int fftlen, int SampleNum, char Command, float Parameter, WAVHDR *Hdr, int DownSampleFlag, FILE *fout)
{
  int fftlen2, fftlen4;
  Complex *LastSample, *dftrec, *dft, *dft2; 
  float Buf[BUFSIZE], *Recon;
  int DataIdx, BufIdx, LastDataIdx, fftnum , datalen;
  int  i, j, StartFlag ;
  float *magnitude, *phase;
  int BufCount;
  time_t t1, t2, t3;

  /* Calculate Constants Needed in Iterative DFT, based on DFTRes */
  fftlen2 = fftlen/2;

  
  DataIdx = fftlen2;
  

  //  printf("Command = %c, Parame = %f\n ",Command, Parameter); 
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
      
      t2 = t1 = time(NULL);

      dftrec = DFT (data, SampleNum, &DataIdx, Buf, BUFSIZE, fftlen, LastSample, BufCount);
      datalen = DataIdx - LastDataIdx; 
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

      Rectangular2Polar ( dft, magnitude, phase, fftnum*datalen);
      //SaveData(phase, datalen*fftnum, "pha0.dat");
      if (Command != 'f' ) 
	unwrap1(phase, datalen, fftnum, fftlen, BufCount, LastPha, DownSampleFlag);

      //SaveData(phase, datalen*fftnum, "pha1.dat");

      switch (Command) {
      case 's' : Recon = speedup ( magnitude, phase, fftnum, fftlen, &datalen, Parameter, BufCount, LastMag, LastPha, DownSampleFlag); break;
      case 'm' :
      case 'f' : Recon = Conv ( magnitude, phase, fftnum, fftlen, datalen, Command, BufCount, DownSampleFlag); break;
      case 'p' : Recon = Pitch ( magnitude, phase, fftnum, fftlen, datalen, BufCount, DownSampleFlag); break;
      default :  Recon = InvDFT2 (dft, fftnum, datalen); ; break;
      };

      free(dft);
      if (Recon)
	WriteWaveFile(Recon, datalen, Hdr, fout);
      free(Recon);
      
      BufCount ++;
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
  
  free(LastSample);
  return;
}



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
  time_t t1, t2;

  t1 = time(NULL);
  /*argument 1: input file name */
  fsrc = fopen((char *)(argv[1]), "rb");
  data = ReadWaveFile(&WavHdr,  &SampleNum, fsrc);
  fclose(fsrc);
  
  /*argument 2: output file name */
  fout = fopen((char *)(argv[2]), "w");
  fwrite((&WavHdr), sizeof(WAVHDR), 1, fout);
  
  /*argument 3: DFT Resolution */
  sscanf((char *)(argv[3]), "%d", &fftlen);

  /*argument 4: downsample flag */
  sscanf((char *)(argv[4]), "%d", &DownSampleFlag);
  
  /*argument 5: option         */
  /*-s: time change            */
  /*-m: male to female change  */
  /*-f: female to male change  */
  if (argc >= 6) 
    sscanf((char *)(argv[5]), "%c%c", &Command, &Command);
  
  if (argc >= 7 )
    if (Command == 's')
      sscanf((char*)(argv[6]), "%f", &TimeRatio);
    else TimeRatio = 0;

  
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

  CleanSpeed();
  CleanConv();
  CleanPitch();
  free(data);

  /*SetSampleRate(fout, 16000);*/
  WaveFileClose(fout);
  free(LastMag);
  free(LastPha);

  t2 = time(NULL);

  printf ( "Total Running Time = %f\n", difftime(t2, t1));

  return 0;
}


















