/* This is the MatTetr class, used for boundling a four dimensional array and a Matrix
this performs the basic complex manipulation
 it is important to come with an efficient implementation of for complex numbers
 because they are widely used in signal processing and communication applications
*/

public class MatTetr {
    public Complex [][][][] Tetr;
    public Complex [][]   Mat;

    public MatTetr(int N_bm, int N_rsg, int N_dop,int N_st, int m, int n){
	this.Cube = new Complex[N_bm] [N_srg] [N_dop][N_st];
	this.Mat= new Complex[m][n];// these structured should be initialized
    }

    static Complex [][][][] CubeTetr(int N_bm, int N_srg, int N_pri, int N_dop, Complex[][][] Cub) {// this class converts!
	Complex [][][][] A=new Complex[N_bm][N_srg][N_dop][N_pri+1-N_dop];
	for (int i=0; i < N_bm; i++)
	    for (int j=0; j < N_srg; j++)
		for (int k=0; k < N_pri+1-N_dop;k++)
		    for (int l=0 ; l <N_dop;l++)
			A[i][j][l][k]=Cub[i][j][l+k];
	return(A);
    }
    
}








