#include <stdio.h>
#include <math.h>

main(int argc, char **argv)
{
  int n,n1,n2;
  char *infile, *outfile;
  double max_diff, diff;
  FILE *fp1, *fp2;
  double re1, im1, re2, im2;
  int i, j, k;
  int mi; 
  double mre1, mre2, mim1, mim2; 

  if (argc != 4) {
    fprintf(stderr,"format: compare file1 file2 n\n");
    exit(-1);
  }
  
  fp1 = fopen(argv[1],"r");
  fp2 = fopen(argv[2],"r");

  #ifdef COMMENT_ONLY 
  fscanf(fp1,"%d\n", &n1);
  fscanf(fp2,"%d\n", &n2);
  if (n1 != n2) { 
    printf("Error: different # of points in the 2 files\n"); 
    exit(-1); 
  } 
  else
    n = n1;  
  #endif
  n = atoi(argv[3]);  
  printf("No. of points %d\n", n);  

  max_diff = 0.0;

  for (i=0; i<n; i+=1) {
	fscanf(fp1,"%lf %lf\n", &re1, &im1);
	fscanf(fp2,"%lf %lf\n", &re2, &im2);
	diff = sqrt((re1-re2)*(re1-re2) + (im1-im2)*(im1-im2));
	if (diff > max_diff)
        {
	  max_diff = diff;
	  mi = i;
	  mre1 = re1;
	  mre2 = re2;
	  mim1 = im1;
	  mim2 = im2;
        } 
  }

  fclose(fp1);
  fclose(fp2);

  printf("  max componentwise distance: %g\n", max_diff);
  printf("%d %g,%g == %g,%g\n", mi, mre1, mim1, mre2, mim2);  
}
