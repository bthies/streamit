/* this Filter performs subband Dopler Compression.
The most computationally intensive method for Pulse Compression is FFT, yet since the complex number support is not yet completely supported by the compiler, I will use the convolutional DFT to perform pulse compression. For code reusablility, the code for performing the actual DFT is implemented in a separate file.

in the work function gets an N_bm*N_srg*N_pri Matrix and a weigths matrix as its input and the output is a N_bm*N_srg*N_dop*N_st such that N_st+N_dop=N_pri+1;,
the init function requires the value for float[N_pc] , the values for the steering matrix V(s) is computed during the initialization part;

*/

import streamit.library.*;

class DopFilt extends Filter {//


   
    float [][] L; //L is the result of the LQ decomposition

    int     N_dop;
    int     N_bm;
    int     N_srg;
    int     N_pri;
    int     N_ch;
    
    



    public void init( int N_dop,int N_ch,int N_pc,int N_bm,int N_srg, int N_pri) {
	this.N_ch=N_ch;
	this.N_pri=N_pri;
	this.N_srg=N_srg;
	this.N_bm=N_bm;
	this.N_dop=N_dop;
	

	Complex[][][] InCube=new Complex[N_bm][N_srg][N_pri];
	Complex[][][][] OutTetr=new Complex[N_bm][N_srg][N_dop][N_pri+1-N_dop];

	CubMat InMatCub;
	TetrMat OutMatTetr;

	
  
	input=new Channel(new CubMat(N_bm,N_srg,N_pri,N_bm,N_ch).getclass(),1);
	output=new Channel(new TetrMat(N_bm,N_srg,N_dop,(N_pri+1-N_dop),N_bm,N_ch).getclass(),1);
       
    }

    public void work(){
	InMatCub=input.pop();
	OutMatCub=InMatCub;	
	OutTetr=CubeTetr(N_bm,N_srg,N_pri,N_dop,InMatCub.Cube);

	output.push(OutMatCub);
	    }
}
	

	


	
	
	

	
	
	
	    


	
    
	

    






