class Cube {// This class performs the operations over a cube
  public static void main(String[] s){
	float[][] A={
	    {1,2,2},
	    {-8,-1,14}
	};
	A=LQ.LQcalc(2,3,A);
	for (int i=0; i<2; i++)
	    for (int j=0; j<3; j++)
		System.out.println(A[i][j]);
    }
    
  public static  Complex [][][] Conv(int N_bm,int N_srg,int N_pri, int N_pc,Complex h_pc, Complex [][][] A){
	Complex [][][] B;
	
	Complex sum;
	
	B=new Complex[N_bm][N_srg][N_pri];
	
        for (int i=0; i < N_bm; i++)
	    for (int j=0; j < N_pri; j++)
		for (int k=0; k < N_srg; k++){
		    sum=Comp.Make(0,0);
		    for ( int l=0; ((l < N_pc) & ((k-l)>=0)); l++)
			sum=Comp.Add(sum,Comp.Mult(h_pc[l],A[i][k-l][j]));
		    B[i][k][j]=sum;
		}
	

	return(B);
    }


}




















