#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#include <math.h>
#include "wave.h"

extern float * short2float ( short * data, int length);
extern float * char2float ( unsigned char * data, int length);
extern short * float2short ( float * data, int length);
extern unsigned char* float2char ( float * data, int length);


void ReadWaveHdr ( WAVHDR *WavHdr, FILE *fp)

{
  int i,j;

  assert(fp);
  fseek(fp, 0, SEEK_SET);
  fread ( WavHdr, 1, sizeof(WAVHDR), fp);
  
};

void VerifyWaveHdr (WAVHDR *WavHdr, float SampleRate, int SampleNum)
{
  WavHdr->SampleRate = ((int)SampleRate);
  WavHdr->ByteRate = (WavHdr->SampleRate) * (WavHdr->NumChannels) * (WavHdr->BitsPerSample) / 8;
  WavHdr->Subchunk2Size = (WavHdr->NumChannels) * (WavHdr-> BitsPerSample) * SampleNum / 8;
  WavHdr->ChunkSize = 36+WavHdr->Subchunk2Size;
}

float *ReadWaveFile(WAVHDR *WavHdr, int *SampleNum, FILE *fp)
{

  float *data;
  short  *datashort;
  unsigned char *datachar;

  assert(fp);
  ReadWaveHdr(WavHdr, fp);
  (*SampleNum) = (WavHdr->Subchunk2Size)*8/(WavHdr->BitsPerSample);
  
  if (WavHdr->BitsPerSample == 8) 
    {
      fseek(fp, 44, SEEK_SET);
      datachar = (unsigned char *)malloc ((*SampleNum)*sizeof(char));
      fread((void *)datachar, sizeof(char), (*SampleNum), fp);
      data = char2float(datachar, (*SampleNum));
      free(datachar);
    }
  else 
    { 
      fseek(fp, 44, SEEK_SET);
      datashort = (short *)malloc ((*SampleNum)*sizeof(short));
      fread((void *)datashort, sizeof(short), (*SampleNum), fp);
      data = short2float(datashort, (*SampleNum));
      free(datashort);
    };

  return data;
}


void WriteWaveFile ( float * Data, int datalen, WAVHDR *Hdr,  FILE *fp)
{
  unsigned char *datachar;
  short * datashort;
  int SampleNum;

  assert(fp);
    
  if (Hdr->BitsPerSample == 8)
    {
      datachar = float2char(Data, datalen);
      fseek(fp, 0, SEEK_END);
      fwrite(datachar, datalen, sizeof(char), fp);
      free(datachar);
    }
  else 
    {
      datashort = float2short(Data, datalen);
      fseek(fp, 0 , SEEK_END);
      fwrite(datashort, datalen, sizeof(short), fp);
      free(datashort);
    }
}

void SetSampleRate ( FILE *fp, int SampleRate)
{
  
  fseek(fp, 24, SEEK_SET);
  fwrite ((&SampleRate), sizeof(long), 1, fp);
}
     

void WaveFileClose( FILE *fp)
{
  long length;

  fseek( fp, 0, SEEK_END);
  length = ftell( fp) - 44;
  fseek( fp, 40, SEEK_SET);
  fwrite((&length), sizeof(long), 1, fp);
  length += 36;
  fseek( fp, 4, SEEK_SET);
  fwrite((&length), sizeof(long), 1, fp);
  fclose(fp);

}

  







