import streamit.library.*;
import streamit.library.io.*;


class LrL extends SplitJoin{// performes the forward substitution 
    public LrL(int N) {super (N);}
    public void init(int N) {
	setSplitter(WEIGHTED_ROUND_ROBIN(N+N*(N+1)/2,N*(N+1)/2));
	add (new forw(N));
	add (new FloatIdentity());
	setJoiner(WEIGHTED_ROUND_ROBIN(N,N*(N+1)/2));
    }

}

class dcalc extends StreamIt {

    /*float[][] h=
      {
      {1,3},
      {2,5}
      };
      float[][] C=
      {
      {1,0},
      {1,2}
      };
      float[] r= {1,2,3,4,5,6};*/



    

	
    
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
	int K;
	int N;
	int Q;
	int W;
	float[][] h;
	float[][] C;
	float[] r;    
	K=2;
	N=2;
	Q=2;
	W=2;
	h=new float[2][2];
	C=new float[2][2];
	r=new float[6];
	h[0][0]=1;
	h[0][1]=3;
	h[1][0]=2;
	h[1][1]=5;
	C[0][0]=1;
	C[0][1]=0;
	C[1][0]=1;
	C[1][1]=2;
	r[0]=1;
	r[1]=2;
	r[2]=3;
	r[3]=4;
	r[4]=5;
	r[5]=6;
	add(new Sourcer(Q*N+W-1,r));
	add(new AddAHLAhA(W,Q,N,K,h,C));
	add(new AhrdAhA(Q*N+W-1,K*N));
	//	add(new LrL(K*N));
	//      add(new backs(K*N));
	add(new Ahrchold(K*N));
	add(new LrL(K*N));
	add(new backs(K*N));
	add(new SinkD(K*N));
         }

class AddAHLAhA extends SplitJoin{// calculates the matrix AH (row oriented?) and L and adds them to the tape, plus a copy of AhA
    public AddAHLAhA(int W,int Q,int N, int K, float[][] h, float[][] C   ) {super (W,Q,N,K,h,C);}
    public void init(int W,int Q,int N, int K, float[][] h, float [][] C) {
	setSplitter(WEIGHTED_ROUND_ROBIN(Q*N+W-1,0));
	add (new FloatIdentity());
	add (new SourceAHL(W,Q,N,K,h,C));
	setJoiner(WEIGHTED_ROUND_ROBIN(Q*N+W-1,K*N*(Q*N+W-1)+(K*N)*(K*N+1)));
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


class multvectdoub extends Pipeline{// duplicates a vector and makes a copy
    public multvectdoub( int M,int N) {super (M,N);}
    public void init(int M, int N) {
	add (new multvect(M,N));
	add (new vectdouble(N));
    }

}

class AhrL1 extends SplitJoin{// calculates Ahr and duplicates L and passes  Ahr,L (2 of them) to the next level 
    public AhrL1( int M,int N) {super (M,N);}
    public void init(int M,int N) {
	setSplitter(WEIGHTED_ROUND_ROBIN(M*(N+1),N*(N+1)/2));
	add (new multvectdoub(M,N));
	add (new vectdouble(N*(N+1)/2));
	setJoiner(WEIGHTED_ROUND_ROBIN(2*N,N*(N+1)));
    }

}


class dsolve extends Pipeline { //input to this pipeline is Ahr(N),L(N*N) and the output is d
    public dsolve(int N) {super(N);}
    public void init(int N){
	add (new LrL(N));
	add (new backs(N));
    }
}
	


class split_ahrd extends SplitJoin{//In:2* Ahr(N)+ 2 * L(N*(N+1)/2)  
    public split_ahrd( int N) {super (N);}
    public void init(int N) {
	setSplitter(WEIGHTED_ROUND_ROBIN(N,N*(N+1)+N));
	add (new FloatIdentity());
	add (new dsolve(N));
	setJoiner(WEIGHTED_ROUND_ROBIN(N,N));
    }

}


class Ahrd extends Pipeline{// the input is Ar, L , the output is Ahr,d,AhA 
    public Ahrd( int M,int N) {super (M,N);}
    public void init(int M,int N) {
	add (new AhrL1(M,N));
	add (new split_ahrd(N));
      }

}


class AhrdAhA extends SplitJoin{// the input is r, L,AhA, the output is Ahr,d,AhA 
    public AhrdAhA( int M,int N) {super (M,N);}
    public void init(int M,int N) {
	setSplitter(WEIGHTED_ROUND_ROBIN(M*(N+1)+N*(N+1)/2,N*(N+1)/2));
	add (new Ahrd(M,N));
	add (new FloatIdentity());                
	setJoiner(WEIGHTED_ROUND_ROBIN(2*N,N*(N+1)/2));
    }

}

class AhrL2 extends SplitJoin{// calculates Ahr and duplicates L, suitable for use in the second stage
    public AhrL2( int M,int N) {super (M,N);}
    public void init(int M,int N) {
	setSplitter(WEIGHTED_ROUND_ROBIN(M*(N+1),N*(N+1)/2));
	add (new multvect(M,N));
	add (new vectdouble(N*(N+1)/2));
	setJoiner(WEIGHTED_ROUND_ROBIN(N,N*(N+1)));
    }

}

    /*
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
	//setPop(N);
	
    }
    public void work() {

	for (int i=0; i< N;i++)
	    {
		//System.out.print("This is ");
		//System.out.print(i);
		//System.out.print(" : ");
		System.out.println(input.popFloat());
	    }
	    
    }
}

class error_est extends Filter{ // this class estimates the error in signal detection

    int N;
    float[] Ahr,d;
    float sigma=0;
	
    public error_est(int N) {super(N);}
    public void init(int N){
	input = new Channel(Float.TYPE,2*N);
	output=new Channel(Float.TYPE,1);
	//setInput(Float.TYPE);
	//setOutput(Float.TYPE);
	this.N=N;
	//setPop(2*N);
	//setPush(1);
	Ahr=new float[N];
	d= new float[N];

    }
    public void work() {

	for (int i=0; i< N;i++)
	    Ahr[i]=input.popFloat();
	for (int i=0; i <N; i++)
	    d[i]=input.popFloat();
	for (int i=0; i <N ; i++)
	    sigma+=(d[i]-Ahr[i])*(d[i]-Ahr[i]);
	output.pushFloat(sigma);
	
	    
	    
    }
}


class choldsigma extends Filter // this Filter performs the cholesky decomposition through 
   {
    int    N; //  the dimension of AhA
    float[][]  A; // A is the input matrix 
    
    float[]  p; // p is the out put elements on the diagonal
    float    sum; // sum will be used as a buffer
       float    sigma;
public choldsigma(int N){ super (N);}
          public void init (int N) {
	      input = new Channel(Float.TYPE, N*(N+1)/2+1);
	      output = new Channel(Float.TYPE, N*(N+1)/2);
          A= new float[N][N];
          p=new float[N];
	  this.N=N;
	  
             
          } 
 

public void work() {
    float sum; // sum serves as a buffer
    sigma=input.popFloat();
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
	      p[i]=(float)Math.sqrt(sum+sigma);
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


class error_split extends SplitJoin{// performs error estimation for the first 2*N elements and copies the AhA
    public error_split( int N) {super (N);}
    public void init(int N) {
	setSplitter(WEIGHTED_ROUND_ROBIN(2*N,N*(N+1)/2));
	add (new error_est(N));     
	add (new FloatIdentity());
	setJoiner(WEIGHTED_ROUND_ROBIN(1,N*(N+1)/2));
    }

}

    
class Lest extends Pipeline{//  this pipeline estimates the error and then performes the cholskey decomp
    public Lest( int N) {super (N);}
    public void init(int N) {
	add (new error_split(N));     
	add (new choldsigma(N));
	add (new vectdouble(N));
    }

}



class  Ahrchold extends SplitJoin{// copies Ahr to its out put and performes the compensated cholesky decomp with Ahr,d,AHA
    public Ahrchold( int N) {super (N);}
    public void init(int N) {
	setSplitter(WEIGHTED_ROUND_ROBIN(N,2*N+N*(N+1)/2));
	add (new FloatIdentity());     
	add (new Lest(N));
	setJoiner(WEIGHTED_ROUND_ROBIN(N,N*(N+1)));
    }

}

}











