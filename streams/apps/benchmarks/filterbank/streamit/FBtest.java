import streamit.library.*;
import streamit.library.io.*;

class source extends Filter {
    int N, pos;
    float[] r;
    public source(int N, float[] r) {super(N, r);}
    public void init(int N, float[] r){
	output = new Channel(Float.TYPE,1);
	this.r=r;
	this.N=N;
        this.pos = 0;
    }
    public void work(){
        output.pushFloat(r[pos++]);
        if (pos >= N) pos = 0;
    }

}

class sink extends Filter{
    int N;
    public sink(int N) {super(N);}
    public void init(int N){
        input = new Channel(Float.TYPE, 1);
	this.N=N;
    }
    public void work() {
        System.out.println(input.popFloat());
    }

}

class FBtest extends StreamIt {
    
 
    static public void main(String[] t)
    {
	StreamIt test=new FBtest();
	test.run(t);
    }
    
   

    public void init() {
	final int N_sim=1024*2;
	int N_samp=/* 32 */ 8;
	int N_ch=N_samp;
	int N_col=32;

	float[] r=new float[N_sim];
	float[][] H=new float[N_ch][N_col];
	float[][] F=new float[N_ch][N_col];
		
	for (int i=0;i<N_sim;i++)
	    r[i]=i+1;

	//float sum=0;	

	for (int i=0;i<N_col;i++) {
	    //sum+=1;
	    //sum=sum/7;

	    for (int j=0;j<N_ch;j++){
		//sum+=1;
		H[j][i]=i*N_col+j*N_ch+j+i+j+1;
		//sum++;
		F[j][i]=i*j+j*j+j+i;
	
	    }
	}
    
	

	
	add (new source(N_sim,r));
	add (new FilterBank(N_samp,N_ch,N_col,H,F));
	add (new sink(N_sim));
    }
    
    
}

	















