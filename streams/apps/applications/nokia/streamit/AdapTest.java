import streamit.library.*;
import streamit.library.io.*;


    class SourceAHL extends Pipeline{

	public SourceAHL(int W, int Q, int N, int K, float[][] h, float[][] C) {super (W,Q,N,K,h,C);}

	public void init(int W, int Q,int N,int K,float [][] h, float [][]C){
	    add (new Sourceh(W,K,h));
	    add (new GenA(W,Q,N,K,C));
	    add (new AandL(Q*N+W-1,K*N));
	    }
    }

    class AandL extends SplitJoin{// the input to this filter is the matrix A(row oriented), the out put is matrix A (row oriented) plus its cholskey decomposition factor L}

	public AandL ( int M, int N){super(M,N);}

	public void init(int M, int N){
	    setSplitter(DUPLICATE());
	    add (new FloatIdentity());
	    add (new GenL(M,N));
	    setJoiner(WEIGHTED_ROUND_ROBIN(M*N,N*(N+1)));
	    }
    }

    class GenL extends Pipeline{// the input is matrix A (row oriented), the output is L and AhA ( which will be used in the dext stage

	public GenL (int M, int N) {super (M,N);}
	
	public void init(int M,int N) {
	    add (new RowCol(M,N));
	    add (new SelfProd(M,N));
	    add (new choldAhA(N));

	    //	    add (new chold(N));
	}
    }

class choldAhA extends SplitJoin{// the input is AhA, the output is cholskey decomposition, N is the dim of Aha

    public choldAhA(int N) {super(N);}

    public void init(int N){
        setSplitter(DUPLICATE());
	add (new chold(N));
	add (new FloatIdentity());
	setJoiner(WEIGHTED_ROUND_ROBIN(N*(N+1)/2,N*(N+1)/2));
    }
}
	    

/*
class AdapTest extends StreamIt {
    final int K=3;
    final int N=3;
    final int W=2;
    final int Q=3;
    final int R=Q*N+W-1;
    
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
	output = new Channel(Float.TYPE, K*W);
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
	input = new Channel(Float.TYPE, K*N*(K*N+1)/2);
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

*/
class Sourceh extends Filter {
   float [][] d;
    int     K;
    int     W;
    public Sourceh(int W, int K, float[][] d){super(W,K,d);}
    public void init(int W, int K, float [][] d){
	output = new Channel(Float.TYPE, K*W);
	this.W=W;
	this.K=K;
	this.d=d;
	    }
    public void work(){
	for(int i=0;i<K;i++)
	    for(int j=0;j<W;j++)
		output.pushFloat(d[j][i]);
    }
}










