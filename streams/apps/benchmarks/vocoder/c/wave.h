
typedef struct {

  char ChunkID[4];
  int  ChunkSize;
  char Format[4];

  char Subchunk1ID[4];
  int  Subchunk1Size;
  short AudioFormat;
  short NumChannels;
  int SampleRate;
  int ByteRate;
  short BlockAlign;
  short BitsPerSample;
  
  char Subchunk2ID[4];
  int Subchunk2Size;
} WAVHDR;

