import streamit.library.*;
import streamit.library.io.*;

class RowCol extends SplitJoin       // this Filter converts the elements of an m by n matrix from row by row format to column by column format
{
int    M;// the number of rows
int    N;// the number of columns
             
             
public RowCol(int M, int N){ super (M,N);}
          public void init ( int M, int N) {
	      setSplitter(ROUND_ROBIN());
	      for (int i=0; i< N;i++)
		  add(new FloatIdentity());
	      setJoiner(ROUND_ROBIN(M));
	  
          } 
 

}









