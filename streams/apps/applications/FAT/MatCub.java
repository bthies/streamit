/* This is the MatCub class, this performs the basic Cube complex manipulation
 it is important to come with an efficient implementation of for complex numbers
 because they are widely used in signal processing and communication applications
*/

public class MatCub {
    public Complex [][][] Cube;
    public Complex [][]   Mat;

    public MatCub(int N_bm, int N_srg, int N_pri, int m, int n){
	Cube = new Complex[N_bm] [N_srg] [N_pri];
	Mat= new Complex[m][n];// these structures should be initialized
    }
	
    
}








