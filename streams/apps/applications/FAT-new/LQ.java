class LQ {// This class performs the LQ decomposition of its input matrix, the out put is a float[][]
    /* public static void main(String[] s){
	float[][] A={
	    {1,2,2},
	    {-8,-1,14}
	};
	A=LQcalc(2,3,A);
	for (int i=0; i<2; i++)
	    for (int j=0; j<3; j++)
		System.out.println(A[i][j]);
		}*/
    
  public static float[][] LQcalc(int m,int n,Complex [][] C){
	complex[][] B;
	complex[][] C;
	complex[]   x;
	complex dummy;
	float[]   w;
	float[]   v;
	float normx;
	float beta;
	float dotprod;// will be used in calculating the dot product
	x=new complex[n];
	v=new float[n];
	w=new float[m];
	
	B=new complex[m][n];
	A=new complex[m][n];

	for (int i=0; i < m ; i ++)
	    for (int j=0; j < n; j++)
		A[i][j]=B[i][j];
		

	for (int row=0;row <m; row ++){
	   
	    normx=0;

	    for (int i=row; i <n; i++)
		{
		    x[i]=A[row][i];
		    normx=normx+(x[i].real*x[i].real+x[i].imag*x[i].imag);
		}
	    normx=(float) Math.sqrt(normx);	    			
	    v=x;
	    
	    v[row]=v[row]+(x[row]/abs(x[row]))*normx;
	    beta=0;
	
	    for (int i=row; i <n ; i++)
		beta=beta+v[i].imag*v[i].imag+v[i].real*v[i].real;
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
		for (int j=row; j <n ; j++){
		    dummy=w[i];
		    dummy.imag=-1*dummy.imag;
		    A[i][j]=A[i][j]+dummy*v[j];
		}
  
	
	for (int i=0 ; i <m ; i++)
	    for (int j=0 ; j <=i ; j++)
		B[i][j]=A[i][j];
	
	return(B);
	
	}
  }

    public static float abs(complex x){
	float y;
	y=x.imag*x.imag+x.real*x.real;
	y=Math.sqrt(y);	 
	return(y);
    }



    public complex[] forw(int m, float[][] L, complex[] v){ // this part perfomes the forw sub, L is a lower traingular m by n(m) matrix that will be used for back substituting for v, the out put is the result of the forward sub, Lu=v
	complex sum; // sum is just a dummy variable
	complex[] u;
	u=new complex[m]; // u will have only m nonzero variables

	for (int i=0 ; i<m; i++)
	    {
	        sum=0

		for (int j=0; j < i; j++)
		    sum=sum+L[i][j]*u[j];		
		u[i]=Comp.Make((v[i].real-sum.real)/L[i][i],(v[i].imag-sum.imag)/L[i][i]);
		
	    }
	return(u);
}




    
    public complex[] back(int m, float [][] L, complex[] u) {// this part performs the back sub algorith, L is a lowe traingular matrix, LTw=u, the out put is w

	complex sum; // sum is just a dummy variable
	complex[] w;
	complex dummy;
	w=new complex[m];
	

	for (int i=m-1; i >=0; i--)
	    {
		sum.real=0;
		sum.imag=0;

		for (int j=m-1;j >=0; j--){
		    dummy=L[j][i];
		    dummy.imag=-1*dummy.imag;
		    sum=sum+dummy[j]*w[j];
		}
		dummy=L[i][i];
		dummy.imag=-1*dummy.imag;				    
		w[i]=(u[i]-sum)/dummy;		  
	    }
	return(w);
    }

}

















