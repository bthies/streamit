import streamit.*;
import streamit.io.*;

class dcalc extends StreamIt {
    int K=2;
    int N=2;
    int Q=2;
    int W=2;
    	float[][] h=
      {
	  {1,3},
          {2,5}
	};
	float[][] C=
	{
	    {1,0},
	    {1,2}
	};
    float[] r= {1,2,3,4,5,6};



    

	
    
    /**
     * If you want to test just this class, then uncomment this
     * method, and change dcalc to extend StreamIt instead of
     * Pipeline.  But the compiler assumes there is exactly 1 toplevel
     * StreamIt class when it's compiling.
     */
    
    static public void main(String[] t)
    {
	dcalc test=new dcalc();
	test.run(t);
    }
    

    public void init() {
	add(new Sourcer(Q*N+W-1,r));
	add(new AddAHL(W,Q,N,K,h,C));
	add(new AhrL(Q*N+W-1,K*N));
	add(new LrL(K*N));
	add(new backs(K*N));
	add(new SinkD(K*N));	
         }

class AddAHL extends SplitJoin{// calculates the matrix AH (row oriented?) and L and adds them to the tape
    public AddAHL(int W,int Q,int N, int K, float[][] h, float[][] C   ) {super (W,Q,N,K,h,C);}
    public void init(int W,int Q,int N, int K, float[][] h, float [][] C) {
	setSplitter(WEIGHTED_ROUND_ROBIN(Q*N+W-1,0));
	add (new FloatIdentity());
	add (new SourceAHL(W,Q,N,K,h,C));
	setJoiner(WEIGHTED_ROUND_ROBIN(Q*N+W-1,K*N*(Q*N+W-1)+(K*N)*(K*N+1)/2));
    }

}


class AhrL extends SplitJoin{// calculates Ahr and duplicates L
    public AhrL( int M,int N) {super (M,N);}
    public void init(int M,int N) {
	setSplitter(WEIGHTED_ROUND_ROBIN(M*(N+1),N*(N+1)/2));
	add (new multvect(M,N));
	add (new vectdouble(N*(N+1)/2));
	setJoiner(WEIGHTED_ROUND_ROBIN(N,N*(N+1)));
    }

}


class vectdouble extends SplitJoin{// duplicates a vector
    public vectdouble( int M) {super (M);}
    public void init(int M) {
	setSplitter(DUPLICATE());
	add (new FloatIdentity());     
	add (new FloatIdentity());
	setJoiner(ROUND_ROBIN(M));
    }

}


class LrL extends SplitJoin{// performes the forward substitution 
    public LrL(int N) {super (N);}
    public void init(int N) {
	setSplitter(WEIGHTED_ROUND_ROBIN(N+N*(N+1)/2,N*(N+1)/2));
	add (new forw(N));
	add (new FloatIdentity());
	setJoiner(WEIGHTED_ROUND_ROBIN(N,N*(N+1)/2));
    }

}
    /* forw sub can be performed 

 class LTd extends SplitJoin{// performes the backsub, the output is the d matrix 
    public Ltd(int N) {super (N);}
    public void init(int N) {
	setSplitter(WEIGHTED_ROUND_ROBIN((Q*N+W-1)*(K*N+1),(K*N)^2));
	add (new multvect(Q*N+W-1,K*N));
	add (new doubleL(K*N)^2);
	setJoiner(WEIGHTED_ROUND_ROBIN(Q*N+W-1,2*(K*N)^2));
    }

    }*/


    
    

class Sourcer extends Filter {
    int N;
    float[] r;
    public Sourcer(int N, float[] r) {super(N,r);}
    public void init(int N, float[] r){
	output = new Channel(Float.TYPE, N);
	this.r=r;
	this.N=N;
    }
    public void work(){
	for(int i=0;i<N;i++)
	     output.pushFloat(r[i]);
    }
}

class SinkD extends Filter{
    int N;
    public SinkD(int N) {super(N);}
    public void init(int N){
	input = new Channel(Float.TYPE, N);
	this.N=N;
	setPop(N);

    }
    public void work() {

	for (int i=0; i< N;i++)
	    {
		System.out.print("This is ");
		System.out.print(i);
		System.out.print(" : ");
		System.out.println(input.popFloat());
	    }
	    
    }
}
		    
		
}


















