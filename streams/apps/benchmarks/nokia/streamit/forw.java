import streamit.*;
import streamit.io.*;

class forw extends Filter // this Filter performs forward substition LY=b. 
   {
    int    N; //  the dimension of the matrix
    float[][]  L; // L is the input matrix 
    float[]  y; // y is the output result
       // we do not need to store the vector b
    float    sum ; //this will be used as a buffer variable
             
public forw(int N, float[][] L){ super (N,L);}
          public void init (int N, float[][] L) {
          setInput(Float.TYPE); 
          setOutput(Float.TYPE);
          setPush(N); 
          setPop(N);
          y=new float[N];
	  this.L=L;
          this.N=N;
          
          } 
 

public void work() {
  for (int i=0; i<N;i++)
      {
	  sum= input.popFloat();
	      for (int j=0; j<i ; j++)
		  sum -= L[i][j]*y[j];
	  y[i]=sum/L[i][i];
          output.pushFloat(y[i]);
      }
}
	     


}







