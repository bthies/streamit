import streamit.*;
import streamit.io.*;

class backs extends Filter // this Filter performs back substition LTd=y. 
   {
    int    N; //  the dimension of the matrix
    float[][]  LT; // L is the input matrix 
    float[]  d; // d is the output result
    float[]  y; //     
   
// we do not need to store the vector y, we need to read y in the backward direction, that is we have to read y[n-1] first and then proceed to y[0]
    float    sum ; //this will be used as a buffer variable
             
public backs(int N, float[][] LT){ super (N,LT);}
          public void init (int N, float[][] LT) {
          setInput(Float.TYPE); 
          setOutput(Float.TYPE);
          setPush(N); 
          setPop(N);

	  this.LT=LT;
          this.N=N;
          } 
 

public void work() {
  for (int i=0; i<N ; i++)
      y[i]=input.popFloat();

  for (int i=N-1; i>=0;i--)
      {
	  sum=y[i];
	      for (int j=i+1; j<N ; j++)
		  sum -= LT[i][j]*d[i];
	  y[i]=sum/LT[i][i];
          output.pushFloat(y[i]);
      }
}
	     

}








