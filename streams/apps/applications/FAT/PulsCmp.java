/* this Filter performs subband Pulse Compression.
The most computationally intensive method for Pulse Compression is FFT with overlap and save, yet since the complex number support is not yet completely supported by the compiler, I will use the convolutional filtering to perform pulse compression. For code reusablility, the code for performing the actual CubeConv is implemented in a separate file.

in the work function gets an N_bm*N_srg*N_pri Matrix as its input and the output is a N_bm*N_srg*N_pri,
the init function requires the value for float[N_pc] , the values for the steering matrix V(s) is computed during the initialization part;

*/

import streamit.library.*;

class PulseCmp extends Filter {//


    Complex [] h_pc   ;
   
    float [][] L; //L is the result of the LQ decomposition

    int     N_pc;
    int     N_bm;
    int     N_srg;
    int     N_pri;
    
    



    public void init( int N_pc,int N_bm,int N_srg, int N_pri, Complex[] h_pc) {
	this.N_ch=N_ch;
	this.N_pri=N_pri;
	this.N_srg=N_srg;
	this.N_bm=N_bm;
	this.h_pc=h_pc;
	

	InCube=new Complex[N_ch][N_srg][N_pri];
	OutCube=new Complex[N_bm][N_srg][N_pri];

	input=new Channel(new Complex[0][0][0].getClass(),1);
	output=new Channel(new Complex[0][0][0].getClass(),1);
       
    }

    public void work(){
	InCube=input.pop();
	OutCube=CubeConv(N_pc,N_bm,N_srg,N_pri,h_pc,InCube);

	output.push(OutCube);
	    }
}
	

	


	
	
	

	
	
	
	    


	
    
	

    






