import streamit.library.*;
import streamit.library.io.*;

class multvect extends Filter // this Filter performs b=AHr
   {
    int    N; //  the dimension of the matrix
    float[][]  AH; // AH is the input matrix 
     //  it is not neccessary to save b. b is generated in the order b[0],b[1],b[2]....
    float[]  r;//
    float    sum; //sum will be used as a buffer
       int    M;   
             
public multvect(int M,int N) { super (M,N);}
          public void init (int M,int N) {
	      input = new Channel(Float.TYPE, M+N*M);
	      output = new Channel(Float.TYPE, N);
          r=new float[M];
	  AH=new float[N][M];
          this.N=N;
	  this.M=M;
          } 
 

public void work() {
    for (int i=0; i<M ; i++)
	r[i]=input.popFloat();
    for (int i=0; i<M;i++)
	for (int j=0; j<N;j++)
	    AH[j][i]=input.popFloat();
    for (int i=0; i<N;i++)
      {
	  sum=0;
	      for (int j=0; j<M ; j++)
		  sum += AH[i][j]*r[j];
          output.pushFloat(sum);
      }
}
	     


}







