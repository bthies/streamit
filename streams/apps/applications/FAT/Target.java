/* this Filter performs the Target Detection which includes both the CFA

it take Tetrades of size N_bm*N_srg*N_dop*N_stag and the W matrix and performs the the beamforming.
in the work function gets an N_bm*N_srg*N_pri Matrix and a weigths matrix as its input and the output is a N_bm*N_srg*N_dop*N_st such that N_st+N_dop=N_pri+1;,
the init function requires the value for float[N_pc] , the values for the steering matrix V(s) is computed during the initialization part;

*/

import streamit.library.*;

class Target extends Filter {//


    int     N_nei; //the number of neighbours

    int     N_dop;
    int     N_bm;
    int     N_sg;
    int     N_cfar;
    int     G;
    float   mu;
    float   norm; // this will be the normalized detection variable
    
    



    public void init( int N_dop,int N_bm,int N_sg,int N_cfar,int G, float mu) {
	this.G=G;
	this.N_cfar=N_cfar;
	this.N_sg=N_sg;
	this.N_bm=N_bm;
	this.N_dop=N_dop;
	this.mu=mu;
	

	Complex[][][] InCube=new Complex[N_bm][N_sg][N_dop];
	float[][][] OutCube=new float[N_bm][N_rg][N_dop];

	
  
	input=new Channel(InCube.getclass(),1);
	output=new Channel(InCube.getclass(),1);
       
    }

    public void work(){
	Tijk
	InCube=input.pop();
	for (int i=0; i < N_bm ; i++)
	    for (int j=0; j < N_dop; j++) // this is a very crude way of implementing CFAR, can be optimized in later versions if necessary
		{
		    for (int k=0; k < N_rg ; k++){
			Tijk=0;
			N_nei=0;
			for (int l=0; (l <N_cfar) and ((k-l-G) >=0 ); l++){
			    N_nei+=1;
			    Tijk=Tijk+Comp.Norm2(InCube[i][k-l-G][j]);
			}

			for (int l=0; (l < N_cfar) and ((k+l+G)<N_rg); l++){
			    N_nei+=1;
			    Tijk+=Comp.Norm2(InCube[i][k+l+G][j]);
			}
					      		     
			
			norm=InCube[i][k][j]*N_nei/Tijk;
			if (norm > mu) 
				    OutCube[i][k][j]=norm;// other wise it will be zero by default.
		    }

		   
		
		       
			

		}

	for (int i=0; i < N_bm; i++)
	    for (int j=0; j < N_rg;j++)
		for (int l=0; l < N_dop; l++)
		    {
			if (i >0) {
			    if (OutCube[i][j][k]<=OutCube[i-1][j][k]) {
				OutCube[i][j][k]=0;
			    }
			}
		   
			if (j >0) {
			    if (OutCube[i][j][k]<=OutCube[i][j-1][k]) {
				OutCube[i][j][k]=0;
			    }
			}
		   
			if (k >0) {
			    if (OutCube[i][j][k]<=OutCube[i][j][k-1]) {
				OutCube[i][j][k]=0;
			    }
		   
			if ((i >0) & (j>0))  {
			    if (OutCube[i][j][k]<=OutCube[i-1][j-1][k]) {
				OutCube[i][j][k]=0;
			    }
			}
		   
			if ((i >0) & (k>0)) {
			    if (OutCube[i][j][k]<=OutCube[i-1][j][k-1]) {
				OutCube[i][j][k]=0;
			    }
		   
			if ((j >0) & (k>0)) {
			    if (OutCube[i][j][k]<=OutCube[i][j-1][k-1]) {
				OutCube[i][j][k]=0;
			    }
		   
			if ((i >0) & (k>0) & (j>0)) {
			    if (OutCube[i][j][k]<=OutCube[i-1][j-1][k-1]) {
				OutCube[i][j][k]=0;
			    }
		   
		       if ((i < (N_bm-1)))  {
			    if (OutCube[i][j][k]<=OutCube[i+1][j][k]) {
				OutCube[i][j][k]=0;
			    }
		       }
		   
			if ((i < (N_bm -1))& (j>0)) {
			    if (OutCube[i][j][k]<=OutCube[i+1][j-][k]) {
				OutCube[i][j][k]=0;
			    }
			}
		   
			if ((i < (N_bm-1)) & (k>0) ) {
			    if (OutCube[i][j][k]<=OutCube[i+1][j][k-1]) {
				OutCube[i][j][k]=0;
			    }
			}
		   
			if ( (i < (N_bm-1)) & (k >0) & (j>0)) {
			    if (OutCube[i][j][k]<=OutCube[i+1][j-1][k-1]) {
				OutCube[i][j][k]=0;
			    }
			}
			
			if ( (i < (N_bm-1)) & (j<(N_rg-1)) ) {
			    if (OutCube[i][j][k]<=OutCube[i+1][j+1][k]) {
				OutCube[i][j][k]=0;
			    }
			}
			
			if ( (i < (N_bm-1)) & (k < (N_dop-1))) {
			    if (OutCube[i][j][k]<=OutCube[i+1][j][k+1]) {
				OutCube[i][j][k]=0;
			    }
			}
			
			if (k < (N_dop-1)  ) {
			    if (OutCube[i][j][k]<=OutCube[i][j][k+1]) {
				OutCube[i][j][k]=0;
			    }
			}
			    
			if (j < (N_rg-1)) {
			    if (OutCube[i][j][k]<=OutCube[i][j+1][k]) {
				OutCube[i][j][k]=0;
			    }
			}
			
			if ((j < (N_rg-1)) & (k < (N_dop-1))) {
			    if (OutCube[i][j][k]<=OutCube[i][j+1][k+1]) {
				OutCube[i][j][k]=0;
			    }
			}
			
			if ((i < (N_bm-1)) & (j< (N_rg-1)) & (k< (N_dop-1))) {
			    if (OutCube[i][j][k]<=OutCube[i+1][j+1][k+1]) {
				OutCube[i][j][k]=0;
			    }
			}
			
			if ((i<(N_bm-1)) & (j < (N_rg-1)) & (k >0)  ) {
			    if (OutCube[i][j][k]<=OutCube[i+1][j+1][k-1]) {
				OutCube[i][j][k]=0;
			    }
			}

			if ((i<(N_bm-1)) & (j > 0) & (k >(N_dop-1))  ) {
			    if (OutCube[i][j][k]<=OutCube[i+1][j-1][k+1]) {
				OutCube[i][j][k]=0;
			    }
			}
			
			if ( (j < (N_rg-1)) & (k >0)  ) {
			    if (OutCube[i][j][k]<=OutCube[i][j+1][k-1]) {
				OutCube[i][j][k]=0;
			    }
			}

			if ((j >0) & (k < (N_dop-1))  ) {
			    if (OutCube[i][j][k]<=OutCube[i][j-1][k+1]) {
				OutCube[i][j][k]=0;
			    }
			}
			
			if ((i>0) & (j < (N_rg-1)) & (k >0)  ) {
			    if (OutCube[i][j][k]<=OutCube[i-1][j+1][k-1]) {
				OutCube[i][j][k]=0;
			    }
			}
			
			if ((i>0) & (j < (N_rg-1)) & (k < (N_dop-1))  ) {
			    if (OutCube[i][j][k]<=OutCube[i-1][j+1][k+1]) {
				OutCube[i][j][k]=0;
			    }
			}
			
			if ((i>0) & (j >0) & (k < (N_dop-1)) ) {
			    if (OutCube[i][j][k]<=OutCube[i-1][j+1][k-1]) {
				OutCube[i][j][k]=0;
			    }
			}
			
			if ((i>0) & (j < (N_rg-1))) {
			    if (OutCube[i][j][k]<=OutCube[i-1][j+1][k]) {
				OutCube[i][j][k]=0;
			    }
			}
			
			if ((i>0)  & (k>(N_dop-1) )  ) {
			    if (OutCube[i][j][k]<=OutCube[i-1][j][k+1]) {
				OutCube[i][j][k]=0;
			    }
			}
			

			


			

			

			
			
			

	
	output.push(OutCube);
    }
}
	

	


	
	
	

	
	
	
	    


	
    
	

    






