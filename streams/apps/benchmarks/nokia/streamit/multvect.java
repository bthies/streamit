import streamit.*;
import streamit.io.*;

class multvect extends Filter // this Filter performs b=AHr
   {
    int    N; //  the dimension of the matrix
    float[][]  AH; // AH is the input matrix 
     //  it is not neccessary to save b. b is generated in the order b[0],b[1],b[2]....
    float[]  r;//
    float    sum; //sum will be used as a buffer 
             
public multvect(int N, float[][] AH){ super (N,AH);}
          public void init (int N, float[][] AH) {
          setInput(Float.TYPE); 
          setOutput(Float.TYPE);
          setPush(N); 
          setPop(N);

	  this.AH=AH;
          this.N=N;
          } 
 

public void work() {
    for (int i=0; i<N ; i++)
	r[i]=input.popFloat();

    for (int i=0; i<N;i++)
      {
	  sum=0;
	      for (int j=0; j<N ; j++)
		  sum += AH[i][j]*r[j];
          output.pushFloat(sum);
      }
}
	     


}







