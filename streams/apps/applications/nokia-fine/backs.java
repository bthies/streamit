import streamit.library.*;
import streamit.library.io.*;

class backs extends Filter // this Filter performs back substition LTd=y. 
   {
    int    N; //  the dimension of the matrix
    float[][]  LT; // L is the input matrix 
    float[]  d; // d is the output result
    float[]  y; //     
   
// we do not need to store the vector y, we need to read y in the backward direction, that is we have to read y[n-1] first and then proceed to y[0]
    float    sum ; //this will be used as a buffer variable
             
public backs(int N){ super (N);}
          public void init (int N) {
	      input = new Channel(Float.TYPE, N+N*(N+1)/2);
	      output = new Channel(Float.TYPE, N);
	      //setInput(Float.TYPE);
	      //setOutput(Float.TYPE);
	      //setPush(N);
	      //setPop(N+N*(N+1)/2);
	      y=new float[N];
	      d=new float[N];
	      LT=new float[N][N];
	      //this.LT=LT;
	      this.N=N;
          } 
 

public void work() {
    
  for (int i=0; i<N ; i++)
      y[i]=input.popFloat();


  for (int i=0; i<N;i++)
	for (int j=i; j<N;j++){
	    LT[i][j]=input.popFloat();
	}

  for (int i=N-1; i>=0;i--)
      {
	  //System.out.println(1010101);
	  sum=y[i];
	  //for (int j=i+1; j<N ; j++)
	  //sum -= LT[i][j]*d[i];
	  //System.out.println(sum);
	  //System.out.println(LT[i][i]);
	  y[i]=sum/LT[i][i];
	  //System.out.println(y[i]);
          output.pushFloat(y[i]);
      }
}
	     

}








