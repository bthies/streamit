import streamit.library.*;
import streamit.library.io.*;

class forw extends Filter // this Filter performs forward substition LY=b. 
   {
    public forw(int N) { super (N);}
    int    N; //  the dimension of the matrix
    float[][]  L; // L is the input matrix 
    float[]  y; // y is the output result
       // we do not need to store the vector b
    float    sum ; //this will be used as a buffer variable
             
       public void init(int N) {
	   	   input = new Channel(Float.TYPE, N+N*(N+1)/2);
	         output = new Channel(Float.TYPE, N);
	   //setInput(Float.TYPE);
	   //setOutput(Float.TYPE);
	   // setPush(N);
	   //setPop(N+N*(N+1)/2);

	   y=new float[N];
	  L=new float[N][N];
          this.N=N;
       } 
 
       
       
public void work() {
       for (int i=0; i <N; i++) {
	   y[i]=input.popFloat();
       }
	
    for( int i=0; i <N; i++)
	for (int j=i; j<N; j++){
      	    L[j][i]=input.popFloat();
	}
    
  for (int i=0; i<N;i++)
      {
	  
	  sum= y[i];
	      for (int j=0; j<i ; j++)
		  sum -= L[i][j]*y[j];
	  y[i]=sum/L[i][i];
          output.pushFloat(y[i]);
      }
}
	     


}







