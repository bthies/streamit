import streamit.*;
import streamit.io.*;

class source extends Filter {
    int N;
    float[] r;
    public source(float[] r) {super(r);}
    public void init(float[] r){
	output = new Channel(Float.TYPE,r.length);
	this.r=r;
	N=r.length;
    }
    public void work(){
	for(int i=0;i<N;i++)
	     output.pushFloat(r[i]);
    }

}

class sink extends Filter{
    int N;
    public sink(int N) {super(N);}
    public void init(int N){
	input = new Channel(Float.TYPE, N);
	this.N=N;
	//setPop(N);
    }
    public void work() {
	System.out.println("Starting");

	for (int i=0; i< N;i++)
	    {
		System.out.print("This is ");
		System.out.print(i);
		System.out.print(" : ");
		System.out.println(input.popFloat());
}

	    
    }

}

class FBtest extends StreamIt {
    
 
    static public void main(String[] t)
    {
	FBtest test=new FBtest();
	test.run(t);
    }
    
   

    public void init() {
	
	float[] r=new float[5];
	r[0]=0;
	r[1]=1;
	r[2]=2;
	r[3]=3;
	r[4]=4;
	
	
	int N_samp=2;
	int N_ch=2;
	int N_col=3;
	float [][] F={
	    {1,2,3},
	    {2,1,3}
	};
	
	float [][] H={
	    {1,0,2},
	    {5,1,1}
	};
	
	add (new source(r));
	add (new FilterBank(N_samp,N_ch,N_col,H,F));
	add (new sink(r.length));
    }
    
    
}

	















