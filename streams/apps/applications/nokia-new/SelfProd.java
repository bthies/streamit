import streamit.library.*;
import streamit.library.io.*;

class SelfProd extends Filter // this Filter mutiplies a matrix by its conjugate , M is the number of rows, N is the number columns, elements of the A are read column by column
{
int    M;// the number of rows
int    N;// the number of columns
    float[][] A;
    //             new A;

float  prod; // this is a dummy variable used for calculating an element in matrix multiplication

    public SelfProd(int M, int N){ super (M,N);}

    public void init ( int M, int N) {
        A=new float[M][N];
	input = new Channel(Float.TYPE, N*M);
	output = new Channel(Float.TYPE, N*(N+1)/2);
          
          this.M=M;
          this.N=N;
          
          } 
 

public void work() {
  for (int i=0; i<N;i++)
    {
      for (int j=0; j<M;j++)
         A[j][i]=input.popFloat();
      
      for (int k=0; k<=i ; k++)
        {prod=0;
          for(int j=0; j<M; j++)
               {
                prod=prod+ A[j][i]*A[j][k] ;
               }
         output.pushFloat(prod); 
        }
    }
  }
}









