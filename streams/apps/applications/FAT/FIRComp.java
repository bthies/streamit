/* This is the comlex FIR class, this genereates an FIR filter for a complex stream.*/
import streamit.library.*;
class FIRcomp extends Pipeline {// this uses a combination of the delay funcation and FirConv to obtain an FIR structure
    public FIRcomp(int N, Complex[] h) {super(N,h);}
    public void init(int N, Complex[] h){
	add(new CompDelay(N-1));
	add(new FIRConv(N-1,h));
    }
}
	    

class FIRConv extends Filter {// This performs convolution...adding a delay block makes it the ideal FIR filter
public FIRConv(int N, Complex[] h) {super(N,h);}
     int        N; //the length of impulse response
    Complex [] h; // the impulse response
                public void init(int N, Complex[] h) {
		    input = new Channel(new Complex().getClass(), 1);
		    output = new Channel(new Complex().getClass(), 1);
		    this.N=N;
		    this.h=h;
                }
                public void work() {
		    Complex sum=Comp.Make(0,0);
		    Complex x=new Complex();// used as a buffer variable
		    
		    for (int i=0; i <N; i++){
			x=(Complex)input.peek(i);
			sum=Comp.Add(sum,Comp.Mult(x,h[N-i]));
		    }
		    output.push(sum);
		    input.pop();


		}
}
		












