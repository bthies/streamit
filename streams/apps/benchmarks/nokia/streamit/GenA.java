import streamit.library.*;
import streamit.library.io.*;

class DelMat extends SplitJoin {// genrates the proper delays for the convolution of C and h
   
    public DelMat(int Q, int  N) {super (Q,N);}

    public void init(int Q,int N) {
	setSplitter(DUPLICATE());
	add(new Identity(Float.TYPE));
	for(int i=1;i<N;i++){
	    add(new DelayPipeline(i*Q));
	}
	setJoiner(ROUND_ROBIN());
    }
}

class ConvMat extends SplitJoin{// generates the matrix consisting of the convolution of h and c. reads h column wise as in [1]
 
    public ConvMat(int K, int W, int Q,int N,float[][] C) {super (K,W,Q,N,C);}
     public void init(int K,int W, int Q,int N,float[][] C){
	float[] Crow;
        setSplitter(ROUND_ROBIN(W));
	for (int i=0;i<K;i++){
	    Crow = new float[Q];
	    for(int j=0;j<Q;j++) {Crow[j]=C[j][i];
            //System.out.println(Crow[j]);
	    
	    }
	     add(new extFilt(W,W+N*Q-1,Crow));
	     //add(new Identity(Float.TYPE));
	}	
	setJoiner(ROUND_ROBIN(W+N*Q-1));
    }
}

class SplitMat extends SplitJoin {// connects the ConvMat to DelMat

    public SplitMat(int W,int Q,int K, int N) {super (W,Q,K,N);} 
    
    public void init(int W,int Q,int K, int N){
	setSplitter(ROUND_ROBIN(N*Q+W-1));
	for (int i=0;i<K;i++){
	    add(new DelMat(Q,N));
	}
	setJoiner(ROUND_ROBIN(N));
    }
}

class AddZeroEnd extends SplitJoin{// adds (M-L)zeros to a sequence of length L to make it have the right size
    public AddZeroEnd(int L, int M) {super (L,M);}
    public void init(int L,int M) {
	setSplitter(WEIGHTED_ROUND_ROBIN(L,0));
	add (new Identity(Float.TYPE));
	add (new ZeroGen());
	setJoiner(WEIGHTED_ROUND_ROBIN(L,M-L));
    }

}


class AddZeroBeg extends SplitJoin{// adds M zeros to the begining of a sequence of length L to make it have the right size
    public AddZeroBeg( int M,int L) {super (M,L);}
    public void init(int M,int L) {
	setSplitter(WEIGHTED_ROUND_ROBIN(0,L));
	add (new ZeroGen());
	add (new Identity(Float.TYPE));
	setJoiner(WEIGHTED_ROUND_ROBIN(M,L));
    }

}


class ZeroGen extends Filter{// this filter just generates a sequence of zeros
    public void init() {
	output = new Channel(Float.TYPE, 1);
    }
    public void work(){
	output.pushFloat(0);
    }
}

class extFilt extends Pipeline{// this filter performs the convolution of L  and then extends the sequenc
    public extFilt(int W,int M,float[] impulse) {super (W,M,impulse);}
    public void init(int W, int M,float[] impulse){
	add (new AddZeroBeg(impulse.length-1,W));
	add (new FirFilter(impulse));
	add (new AddZeroEnd(W+impulse.length-1,M));
    }
}


class GenA extends Pipeline{// the whole matrix A generator, the input is column wise and the out put is row wise
    public GenA(int W,int Q, int N, int K, float[][] C) {super (W,Q,N,K,C);}
    public void init(int W, int Q, int N, int K, float[][] C) {
	add(new ConvMat(K,W,Q,N,C));
	add(new SplitMat(W,Q,K,N));
    }
}


	
	
       
 
	
    








