import streamit.*;
import streamit.io.*;

class AdapTest extends StreamIt {
    int K=3;
    int N=3;
    int W=2;
    int Q=3;
    int R=Q*N+W-1;
    
    static public void main (String[] t)
    
    {
	AdapTest test=new AdapTest();
	test.run(t);
    }
    public void init() {
	float[][] C=
      {
	  {1,3,7},
          {2,0,5},
  	  {3,0,0}
	};
	//float[]B={1,1,0};
   	add(new Source());
	add(new GenA(W,Q,N,K,C));
        add(new RowCol(N*Q+W-1,N*K));
        //add(new extFilt(2,10,B));
        add(new SelfProd(R,K*N));
        add(new chold(K*N));
	add(new Sink());
    }

    
    

class Source extends Filter {
    float d[][]={
	{1,0,1},
	{1,1,0}
    };
    public void init(){
	setOutput(Float.TYPE);
	setPush(K*W);
	    }
    public void work(){
	for(int i=0;i<K;i++)
	    for(int j=0;j<W;j++)
		output.pushFloat(d[j][i]);
    }
}

class Sink extends Filter{
    
    float A[][];
    public void init(){
	setInput(Float.TYPE);
	setPop(K*N*(K*N+1)/2);
	//setPop(10);
	A = new float[R][R];
     }
    public void work() {

	for (int j=0; j<K*N;j++)
	    for (int i=0; i<= j;i++)
		A[i][j]=input.popFloat();
	for (int i=0; i< K*N;i++)
	    {for (int j=0;j<K*N;j++)
		System.out.println(A[j][i]);
	    System.out.println("col finished");
	    System.out.println(i);
	    System.out.println("don col");
	    }
    }
}
		    
		
}





