/* this Filter Per performes the adaptive Beamforming,
in the work function gets an N_ch*N_srg*N_pri Matrix as its input and the output is a N_bm*N_srg*N_pri,
the init function requires the value for float[N_ch] b,float[N_bm] d,a vectors, the values for the steering matrix V(s) is computed during the initialization part;

*/

import streamit.library.*;

class AdapBeam extends Filter {//
    Complex [][] V; // the steering matrix
    Complex [][] A_ext;// the extended version of A which will have dimensions N_ch*(6N_ch)
    Complex [][][] InCube;
    Complex [][][] OutCube;
    Complex [] u   ;
    Complex [][] W ;
    float sum    ; // sum will be used as a dummy variable

    float [][] L; //L is the result of the LQ decomposition

    int     N_ch;
    int     N_bm;
    int     N_srg;
    int     N_pri;
    
    



    public void init( int N_ch,int N_bm,int N_srg, int N_pri,float alpha, float[] a, float[] b, float[] d) {
	this.N_ch=N_ch;
	this.N_pri=N_pri;
	this.N_srg=N_srg;
	this.N_bm=N_bm;

	CubMat W_Ocub= new CubMat(N_bm,N_srg,N_pri,N_ch,N_bm);
	

	InCube=new Complex[N_ch][N_srg][N_pri];
	OutCube=new Complex[N_bm][N_srg][N_pri];
	u=new Complex[N_ch];

	W=new Complex[N_ch][N_bm];

	V=new Complex[N_ch][N_bm];
	for ( int i=0; i <N_bm ; i++)
	    for (int j=0; j < N_ch ; j++)
		V[j][i]=Comp.Mult(b[j],Comp.etoj(-d[i]-a[i]*j));

	alpha=(float) Math.sqrt(alpha);
	A_ext= new Complex[N_ch][6*N_ch];
	// are the initial values zero? no they are not
	for (int i=0; i < N_ch; i++)
	    for (int j=5*N_ch ; j<6*N_ch; j++){
		if (i==j) {
		A_ext[i][j]=Comp.Make(alpha,0);
		}
		else A_ext[i][j]=Comp.Make(0,0);
		
	    }
	input=new Channel(new Complex[0][0][0].getClass(),1);
	output=new Channel(new MatCub(N_bm,N_srg,N_pri,N_ch,N_bm).getClass(),1);
       
    }

    public void work(){
	InCube=input.pop();

	for (int i=0;i< N_ch; i++)
	    for (int j=0; j < 5*N_ch; j++)
		A_ext[i][j]=Comp.Div(InCube[i][j][1],(float)Math.sqrt(5*N_ch));

	L=LQ.comp(N_ch,6*N_ch,A_ext);

	for (int i=0; i < N_bm; i++){ // this loop calculates the beam correction matrix using Wiener-Hopf Formula

	    for (int j=0; j < N_ch; j++)
		u[j]=V[j][i];
	    
	    u=LQ.forw(N_ch,L,u);
	    sum=0;
	    
	    
	    for (int j=0;j < N_ch; j++)
		sum=sum+Comp.Norm2(u[j]);
	    
	    u=LQ.back(N_ch,L,u);

	    for (int j=0; j < N_ch;j++)
		W[j][i]=Comp.Div(u[j],sum);

	}
	
        W_Ocub.Mat=W;
	
	for (int i=0; i < N_pri; i++)
	    for (int j=0; j < N_srg; j++)
		for (int k=0; k < N_bm; k++)
		    for (int l=0; l < N_ch; l++)
			W_Ocub.Cube[k][j][i]=Comp.Mult(W[l][k],InCube[l][j][i]);

	output.push(W_Ocub);
	    }
}
	

	


	
	
	

	
	
	
	    


	
    
	

    






