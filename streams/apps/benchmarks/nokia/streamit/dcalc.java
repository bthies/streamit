import streamit.*;
import streamit.io.*;

class dcalc extends StreamIt {
    int K=3;
    int N=3;
    int Q;
    int W;
    float[][] h;
    float[][] C;
    

	
    
    static public void main (String[] t)
    
    {
	dcalc test=new dcalc();
	test.run(t);
    }
    public void init() {
	float[][] AH=
      {
	  {1,3,7},
          {2,0,5},
  	  {3,0,0}
	};
	float[][] L=
	{
	    {1,0,0},
	    {1,2,0},
	    {3,1,2}
	}; 
	float[][] LT=
	{
	    {1,1,3},
	    {0,2,1},
	    {0,0,2}
	};
	add(new SourceD());
	add(new AddAHL(W,Q,N,K,h,C));
	add(new AhrL(Q*N+W-1,K*N));
	add(new LrL(K*N));
	add(new backs(K*N));
	add(new SinkD());	
         }

class AddAHL extends SplitJoin{// calculates the matrix AH (row oriented?) and L and adds them to the tape
    public AddAHL(int W,int Q,int N, int K, float[][] h, float[][] C   ) {super (W,Q,N,K,h,C);}
    public void init(int M,int L) {
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


    
    

class SourceD extends Filter {
    float r[]={1,1,1};
    
    public void init(){
	setOutput(Float.TYPE);
	setPush(Q*N+W-1);
    }
    public void work(){
	for(int i=0;i<Q*N+W-1;i++)
	     output.pushFloat(r[i]);
    }
}

class SinkD extends Filter{
    
    public void init(){
	setInput(Float.TYPE);
	setPop(K*N);
     }
    public void work() {

	for (int i=0; i< K*N;i++)
	    System.out.println(input.popFloat());
    }
}
		    
		
}






