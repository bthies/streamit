#include <stdio.h>
#include <math.h>

main(int argc, char **argv)
{
  int n;
  char *infile, *outfile, dfile[128]; 
  double max_diff, diff;
  FILE *fp1, *fp2, *fpd;
  double re1, im1, re2, im2;
  int i, j, k;
  int mi; 
  double mre1, mre2, mim1, mim2; 

  if (argc != 4) {
    fprintf(stderr,"format: dcompare file1 file2 n\n");
    exit(-1);
  }

  strcpy(dfile,argv[1]); 
  strcat(dfile,".diff"); 
  fpd = fopen(dfile,"w"); 
 
  fp1 = fopen(argv[1],"r");
  fp2 = fopen(argv[2],"r");

  n = atoi(argv[3]);
  printf("No. of points %d\n", n);

  max_diff = 0.0;

  for (i=0; i<n; i+=1) {
	fscanf(fp1,"%lf %lf\n", &re1, &im1);
	fscanf(fp2,"%lf %lf\n", &re2, &im2);
	diff = sqrt((re1-re2)*(re1-re2) + (im1-im2)*(im1-im2));
	fprintf(fpd,"%d: %f\t%f\t%f\n", i, (re1-re2), (im1-im2), diff); 
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
  fclose(fpd);
 
  printf("  max componentwise distance: %g\n", max_diff);
  printf("%d %g,%g == %g,%g\n", mi, mre1, mim1, mre2, mim2);  
  printf("Output file %s written\n",dfile); 

}
