
import streamit.library.*;
import streamit.library.io.*;

class chold extends Filter // this Filter performs the cholesky decomposition through 
{
    int    N; //  the dimension of AhA
    //float[][]  A; // A is the input matrix 
    
    //float[]  p; // p is the out put elements on the diagonal
    //float    sum; // sum will be used as a buffer         
    public chold(int N){ super (N);}
    public void init (int N) {
	input = new Channel(Float.TYPE, N*(N+1)/2);
	output = new Channel(Float.TYPE, N*(N+1)/2);
	//A= new float[N][N];
	//p=new float[N];
	this.N=N;
	
    } 
    
    
    public void work() {
	float[][]  A= new float[N][N];
	float[]  p=new float[N];
	float sum; // sum serves as a buffer
	for (int i=0; i<N;i++)
	    {  
		for (int j=0; j<=i ; j++)
		    A[i][j]=input.popFloat(); 
		
	    }
	
	for (int i=0; i <N ; i++) { 
	    for (int j=i; j<N ; j++) {
		sum=A[j][i];
		for (int k=i-1 ; k>=0; k--) sum-=A[k][i]*A[k][j];
		if ( i==j)
		    {
			p[i]=(float)Math.sqrt(sum);
			output.pushFloat(p[i]);
		    }
		else
		    {
			A[i][j]=sum/p[i];
			output.pushFloat(A[i][j]);
		    }
	    } }
	
    }
    
    
}








