/* This is the subband analysis block, the input is a N_ch*N_rg*N_pri block, the output is a data cube of size N_ch*N_srg*N_pri,that has been multiplied by e^(j....) and then lowpass filtered, a down sapmling is done to make it of the size N_srg*N_dn=N_rg */

import streamit.*;

class LQ extends Filter {//This Filter performs the Frequency shifting, each time it is invoked, elements of a sequence of length N_rg are read and they are multiplied by W^n Where n is the index.
    /* public FreqShift(int N_rg,Complex W) {super(N_rg,W);}
                int        N_rg; //the lenghth of the string;
                Complex W= new Complex();
		Complex X;
		float y;
                public void init(int N_rg, Complex W) {
		    input = new Channel(Float.TYPE, N_rg);
		    output = new Channel(new Complex().getClass(), N_rg);
		    this.N_rg=N_rg;
		    this.W=W;
		    } */
    public static void main(String[] s){
	float[][] A={
	    {1,2,2},
	    {-8,-1,14}
	};
	A=LQcalc(2,3,A);
	for (int i=0; i<2; i++)
	    for (int j=0; j<3; j++)
		System.out.println(A[i][j]);
    }
    
  public static float[][] LQcalc(int m,int n,float[][] B){
	float[][] A;
	float[]   x;
	float[]   w;
	float[]   v;
	float normx;
	float beta;
	float dotprod;// wiil be used in calculating the dot product
	x=new float[n];
	v=new float[n];
	w=new float[m];
	
	A=new float[m][n];
	A=B;
	for (int row=0;row <m; row ++){
	    normx=0;
	    for (int i=row; i <n; i++)
		{
		    x[i]=A[row][i];
		    normx=normx+x[i]*x[i];
		}
	    normx=(float) Math.sqrt(normx);	    			
	    v=x;
	    
	    v[row]=v[row]+sign(x[row])*normx;
	    beta=0;

	    for (int i=row; i <n ; i++)
		beta=beta+v[i]*v[i];
	    beta=-2/beta;
	    

	    for (int i=row; i <m; i++)
		{
		    dotprod=0;
		    for (int j=row; j <n ; j++)
			dotprod=dotprod+A[i][j]*v[j];
		    w[i]=beta*dotprod;
		    //System.out.println(w[i]);
		}
	    for (int i=row; i <m ; i++)
		for (int j=row; j <n ; j++)
		    A[i][j]=A[i][j]+w[i]*v[j];
       	}
	return(A);
    }


    public static float sign(float x){
	float y;
	if (x>0)  y=1;
	  else y=-1;
	return(y);
    }
			
	    
		
	    
	
                public void work() {

		    
	  


		}
}



















