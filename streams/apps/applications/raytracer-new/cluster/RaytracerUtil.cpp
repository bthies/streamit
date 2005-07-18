#include <stdio.h>
#include <stdlib.h>
#include <sys/time.h>

#include "structs.h"

GridInfo RaytracerUtil_parseVox(char* fname, int num_tris, Triangle9* tris)
{
  int dim_x, dim_y, dim_z, num_entries, i, x, y, z, trilist_size;
  GridInfo ginfo; //memory leak!!!
  //  ((ginfo.grid_dim) = calloc(3, sizeof(int)));
  ((ginfo.trilist_offsets) = (int*)calloc(2048, sizeof(int)));
  ((ginfo.trilist) = (int*)calloc(2048, sizeof(int)));
  //((ginfo.step) = (int*)calloc(3, sizeof(int)));
  //((ginfo.outno) = (int*)calloc(3, sizeof(int)));
  
  FILE *fp = fopen( fname, "rb" );
  if( !fp ){
    fprintf(stderr, "Couldn't open %s for reading\n", fname);
    exit(-1);
  }

  fprintf(stderr, "\tReading grid paramters... ");
  fread(&dim_x, sizeof(int), 1, fp);
  ginfo.grid_dim.x = dim_x;
  fread(&dim_y, sizeof(int), 1, fp);
  ginfo.grid_dim.y = dim_y;
  fread(&dim_z, sizeof(int), 1, fp);
  ginfo.grid_dim.z = dim_z;

  //  fread( &ginfo.grid_dim, sizeof(int), 3, fp );
  fread( &ginfo.grid_min, sizeof(float), 3, fp );
  fread( &ginfo.grid_max, sizeof(float), 3, fp );
  fread( &ginfo.grid_vsize, sizeof(float), 3, fp );
  fprintf(stderr, "Done\n");
  
  fprintf(stderr, "\tReading bitvector... ");
  dim_x = ginfo.grid_dim.x;
  dim_x += (4 - dim_x % 4);
  dim_y = ginfo.grid_dim.y;
  dim_y += (4 - dim_y % 4);
  dim_z = ginfo.grid_dim.z;
  dim_z += (4 - dim_z % 4);
  num_entries = dim_x * dim_y * dim_z / 64 + 1;
  //int* bitmaps = new int[num_entries * 2];
  //fread( bitmaps, sizeof(int), num_entries * 2, fp );
  fseek(fp, num_entries * 8, SEEK_CUR);
  fprintf(stderr, "Done\n");

  fprintf(stderr, "\tReading triangle list offsets... %d ", (int)(ginfo.grid_dim.x * ginfo.grid_dim.y * ginfo.grid_dim.z));
  i = 0;
  for(x = 0; x < ginfo.grid_dim.x; x++){
    for(y = 0; y < ginfo.grid_dim.y; y++){
      for(z = 0; z < ginfo.grid_dim.z; z++){
	fread( &ginfo.trilist_offsets[i++], sizeof(int), 1, fp );
      }
    }
  }
  fprintf(stderr, "Done\n");
  
  trilist_size = 0;
  fread( &trilist_size, sizeof(int), 1, fp );
  fprintf(stderr, "\tReading triangle list... %d  ", trilist_size);
  for( i=0; i<trilist_size; i++)
    fread( &ginfo.trilist[i], sizeof(int), 1, fp );
  fprintf(stderr, "Done\n");

  num_tris;
  fread( &num_tris, sizeof(int), 1, fp);
  fprintf(stderr, "\tReading triangle data...%d  ", num_tris);
  for( i=0; i < num_tris; i++){
    fread( &tris[i].v0, sizeof(float), 3, fp );
    fread( &tris[i].v1, sizeof(float), 3, fp );
    fread( &tris[i].v2, sizeof(float), 3, fp );
    fread( &tris[i].n0, sizeof(float), 3, fp );
    fread( &tris[i].n1, sizeof(float), 3, fp );
    fread( &tris[i].n2, sizeof(float), 3, fp );
    fread( &tris[i].c0, sizeof(float), 3, fp );
    fread( &tris[i].c1, sizeof(float), 3, fp );
    fread( &tris[i].c2, sizeof(float), 3, fp );
  }
  fclose(fp);
  fprintf(stderr, "Done\n");
  
  return ginfo;
}

int RaytracerUtil_beginPPMWriter(char *fname, int width, int height)
{
  FILE *fp = fopen( fname, "wb" );
  if (NULL == fp) {
    fprintf(stderr, "Couldn't open PPM file for writing\n");
    exit(99);
  }
  fprintf(fp, "P6\n");
  fprintf(fp, "%d %d\n", width, height);
  fprintf(fp, "255\n" );
  return (int)fp;
}  

void RaytracerUtil_sendToPPMWriter(int fid, float4 data)
{
  FILE* fp = (FILE*)fid;
  char ndata[3];
  ndata[0] = data.x * 255.0;
  ndata[1] = data.y * 255.0;
  ndata[2] = data.z * 255.0;
  fwrite(&ndata, 3, 1, fp);
}
    
void RaytracerUtil_endPPMWriter(int fid)
{    
  FILE* fp = (FILE*)fid;
  fclose(fp);
}

int RaytracerUtil_currentTimeMillis()
{
  struct timeval tv;
    
  struct timezone tz;
    
  int status = gettimeofday(&tv, &tz);
    
  long long retval = tv.tv_usec / 1000;
    
  retval += (long long)tv.tv_sec * 1000;
    
  static long long base = 0;
  if (base == 0)
    base = retval;
  return (int)(retval - base);
}
