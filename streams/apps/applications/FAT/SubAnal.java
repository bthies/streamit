/* This is the subband analysis block, the input is a N_ch*N_rg*N_pri block, the output is a data cube of size N_ch*N_srg*N_pri,that has been multiplied by e^(j....) and then lowpass filtered, a down sapmling is done to make it of the size N_srg*N_dn=N_rg */

import streamit.library.*;
class ElAnal extends SplitJoin {// this SplitJoin gets as its input the elements of different subbbands and pushes them through RnAnal
    public ElAnal(int N_ch,int N_pri,int N_rg,int N_dn,int N_lp,Complex W, Complex[] h){super (N_ch,N_pri,N_rg,N_dn,N_lp,W,h);}
    public void init(int N_ch,int N_pri,int N_rg,int N_dn,int N_lp,Complex W,Complex[] h) {
	setSplitter(ROUND_ROBIN(N_rg));
	for (int i=0; i< N_pri; i++)
	    for (int j=0; j < N_ch; j ++)
		add(new RnAnal(N_rg,N_dn,N_lp,W,h));
	setJoiner(ROUND_ROBIN(N_rg/N_dn));
    }
}
	
		    
class AnalTd extends Pipeline {// this does the freauency shift and low pass filtering and downsampling+time delay equalization for each Range
    public RnAnal(int N_rg,int N_dn,int N_lp,int N_td,Complex W, Complex[] h_lp,Complex[] h_td) {super(N_rg,N_dn,N_lp,W,h);}//N_lp is the number of taps in the lowpass filter
    public void init(int N_rg,int N_dn,int N_lp,int N_td,Complex W, Complex[] h_lp,Complex h_td){
        add(new FreqShift(N_rg,W));
	add(new FIRComp(N_lp,h_lp));
	add(new DownSamp(N_dn));
	add(new FIRComp(N_td,h_td));
    }
}
	    

class FreqShift extends Filter {//This Filter performs the Frequency shifting, each time it is invoked, elements of a sequence of length N_rg are read and they are multiplied by W^n Where n is the index.
public FreqShift(int N_rg,Complex W) {super(N_rg,W);}
                int        N_rg; //the lenghth of the string;
                Complex W= new Complex();
		Complex X;
		float y;
                public void init(int N_rg, Complex W) {
		    input = new Channel(Float.TYPE, N_rg);
		    output = new Channel(new Complex().getClass(), N_rg);
		    this.N_rg=N_rg;
		    this.W=W;
                }
                public void work() {

		    Complex X=Comp.Make(1,0);
		    
		    for (int i=0; i <N_rg; i++){
			y=(float) input.pop();
			output.push(Comp.Make(X.real*y,X.imag*y));
			X=Comp.Mult(X,W);
		    }
		    
	  


		}
}

		

class DownSamp extends Filter {//This Filter performs the down sampling task on imaginary numbers
public DownSamp(int N_dn) {super(N_dn);}
                int        N_dn; //;
                    public void init(int N_dn) {
		    input  = new Channel(new Complex().getClass(), N_dn);
		    output = new Channel(new Complex().getClass(), 1);
		    this.N_dn=N_dn;
                }
                public void work() {
		    
		    output.push(input.pop());
		    for (int i=0 ; i <N_dn-1 ;i++)
			input.pop();
		    


		}
}



















