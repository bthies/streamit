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
    
  public static float[][] LQcalc(int m,int n,Complex [][] A){
	float[][] B;
	float[]   x;
	float[]   w;
	float[]   v;
	float normx;
	float beta;
	float dotprod;// will be used in calculating the dot product
	x=new Complex[n];
	v=new float[n];
	w=new float[m];
	
	B=new Complex[m][m];

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
			dotprod=dotprod+Comp.Mult(A[i][j],v[j]);
		    w[i]=beta*dotprod;
		    //System.out.println(w[i]);
		}
	    for (int i=row; i <m ; i++)
		for (int j=row; j <n ; j++)
		    A[i][j]=Comp.Add(A[i][j],w[i]*v[j]);
       	}
	for (int i=0 ; i <m ; i++)
	    for (int j=0 ; j <=i ; j++)
		B[i][j]=A[i][j];
	
	return(B);
    }


    public static float sign(float x){
	float y;
	if (x>0)  y=1;
	  else y=-1;
	return(y);
    }



    public Complex[] forw(int m, float[][] L, Complex[] v){ // this part perfomes the forw sub, L is a lower traingular m by n(m) matrix that will be used for back substituting for v, the out put is the result of the forward sub, Lu=v
	Complex sum=new Complex(); // sum is just a dummy variable
	Complex[] u;
	u=new Complex[m]; // u will have only m nonzero variables

	for (int i=0 ; i<m; i++)
	    {
	        sum.real=0;
		sum.imag=0;

		for (int j=0; j < i; j++)
		    sum=Comp.Add(sum,Comp.Mult(L[i][j],u[j]));		
		u[i]=Comp.Make((v[i].real-sum.real)/L[i][i],(v[i].imag-sum.imag)/L[i][i]);
		
	    }
	return(u);
}




    
    public Complex[] back(int m, float [][] L, Complex[] u) {// this part performs the back sub algorith, L is a lowe traingular matrix, LTw=u, the out put is w

	Complex sum=new Complex(); // sum is just a dummy variable
	Complex[] w;
	w=new Complex[m];
	

	for (int i=m-1; i >=0; i--)
	    {
		sum.real=0;
		sum.imag=0;

		for (int j=m-1;j >=0; j--)
		      sum=Comp.Add(sum,Comp.Mult(L[j][i],w[j]));

		w[i]=Comp.Make((u[i].real-sum.real)/L[i][i],(u[i].imag-sum.imag)/L[i][i]);
		  
	    }
	return(w);
    }

}

















