/* $Header: /afs/csail.mit.edu/group/commit/reps/projects/streamit/cvsroot/streams/3rdparty/lpsolve/solve.java,v 1.2 2002-10-07 09:00:59 thies Exp $ */
/* $Log: not supported by cvs2svn $
/* Revision 1.1  2002/10/05 05:25:54  thies
/* This is a first check-in of the linear programming partitioner stuff
/* -- it isn't done yet, but the program itself is almost setup.
/*
/* These are the 3rdparty classes to solve an LP -- ported from lp_solve
/* 2.0.
/*
# Revision 1.6  1996/06/07  01:31:11  hma
# changed some member functions to be public
#
# Revision 1.5  1996/06/06  20:30:05  hma
# minor change
#
# Revision 1.4  1996/06/06  20:28:35  hma
# change implements constant into implements lp.constant
#
# Revision 1.3  1996/06/06  19:48:03  hma
# added package statement
#
# Revision 1.2  1996/05/21  01:58:32  hma
# working version
# */

package lpsolve;

import java.io.* ;
import java.util.* ;

class Util {
  final static double round( double val, double eps) {
    return (Math.abs(val) < eps) ? 0 : val;
  }
} // end of class Util



class column
{
  int            row;
  float          value;
  column         next ;
} 

class constraint_name
{
  String                  name;
  int                     row;
  constraint_name         next;
}

class bound
{
  double          upbo;
  double          lowbo;
}

class tmp_store_struct
{
  String name;
  int     row;
  double  value;
  double  rhs_value;
  short   relat;
}

class hashelem
{
  String          colname;
  hashelem        next;
  column          col;
  bound           bnd;
  int             must_be_int;
} // end of hashelem;

class rside /* contains relational operator and rhs value */
{
  double          value;
  rside           next;
  short           relat;
}

// pack a value for reference
class Ref {
  double value;
  public Ref(double v) {
    value = v;
  }
}


public class solve implements constant{
/* Globals */
  lprec   Lp; /* pointer to active problem */
  int     Rows;
  int     Columns;
  int     Sum;
  int     Non_zeros;
  int     Level;
  matrec[]  Mat;
  int[]     Col_no;
  int[]     Col_end;
  int[]     Row_end;
  double[]    Orig_rh;
  double[]    Rh;
  double[]    Rhs;
  short[]   Must_be_int;
  double[]    Orig_upbo;
  double[]    Orig_lowbo;
  double[]    Upbo;
  double[]    Lowbo;
  int[]     Bas;
  short[]   Basis;
  short[]   Lower;
  int     Eta_alloc; 
  int     Eta_size;           
  double[]    Eta_value;
  int[]     Eta_row_nr;
  int[]     Eta_col_end;
  int     Num_inv;
  double[]    Solution;
  double[]    Best_solution;
  double    Infinite;
  double    Epsilon;
  double    Epsb;
  double    Epsd;
  double    Epsel;
  
  double	TREJ;
  double	TINV;
  
  short   Maximise;
  short   Floor_first;
  double    Extrad;

  int     Warn_count; /* used in CHECK version of rounding macro */

void inc_mat_space(lprec lp, int maxextra)
{
   if(lp.non_zeros + maxextra > lp.mat_alloc)
     {
       int i;
       lp.mat_alloc = lp.non_zeros + maxextra;
       matrec[] old_mat = lp.mat;
       lp.mat = new matrec[lp.mat_alloc];
       for (i = old_mat.length; i < lp.mat.length; i++)
	 lp.mat[i] = new matrec(0,0);
       System.arraycopy(old_mat, 0, lp.mat, 0, old_mat.length);
       int[] old_col_no = lp.col_no;
       lp.col_no = new int[lp.mat_alloc];
       System.arraycopy(old_col_no, 0, lp.col_no, 0, old_col_no.length);
       if (lp.active != FALSE)
         {
           Mat=lp.mat;
           Col_no=lp.col_no;
         }
     }
} // end of inc_mat_space
 
void inc_row_space(lprec lp)
{
  if(lp.rows > lp.rows_alloc)
    {
      lp.rows_alloc=lp.rows+10;
      lp.sum_alloc=lp.rows_alloc+lp.columns_alloc;

      double[] db_ptr = lp.orig_rh;
      lp.orig_rh = new double[lp.rows_alloc + 1];
      System.arraycopy(db_ptr, 0, lp.orig_rh, 0, db_ptr.length);
      
      db_ptr = lp.rh;
      lp.rh = new double[lp.rows_alloc + 1];
      System.arraycopy(db_ptr, 0, lp.rh, 0, db_ptr.length);

      db_ptr = lp.rhs;
      lp.rhs = new double[lp.rows_alloc + 1];
      System.arraycopy(db_ptr, 0, lp.rhs, 0, db_ptr.length);

      db_ptr = lp.orig_upbo;
      lp.orig_upbo = new double[lp.sum_alloc + 1];
      System.arraycopy(db_ptr, 0, lp.orig_upbo, 0, db_ptr.length);

      db_ptr = lp.upbo;
      lp.upbo = new double[lp.sum_alloc + 1];
      System.arraycopy(db_ptr, 0, lp.upbo, 0, db_ptr.length);

      db_ptr = lp.orig_lowbo;
      lp.orig_lowbo = new double[lp.sum_alloc + 1];
      System.arraycopy(db_ptr, 0, lp.orig_lowbo, 0, db_ptr.length);

      db_ptr = lp.lowbo;
      lp.lowbo = new double[lp.sum_alloc + 1];
      System.arraycopy(db_ptr, 0, lp.lowbo, 0, db_ptr.length);

      db_ptr = lp.solution;
      lp.solution = new double[lp.sum_alloc + 1];
      System.arraycopy(db_ptr, 0, lp.solution, 0, db_ptr.length);

      db_ptr = lp.best_solution;
      lp.best_solution = new double[lp.sum_alloc + 1];
      System.arraycopy(db_ptr, 0, lp.best_solution, 0, db_ptr.length);

      int[] int_ptr = lp.row_end;
      lp.row_end = new int[lp.rows_alloc + 1];
      System.arraycopy(int_ptr, 0, lp.row_end, 0, int_ptr.length);

      short[] short_ptr = lp.basis;
      lp.basis = new short[lp.sum_alloc + 1];
      System.arraycopy(short_ptr, 0, lp.basis, 0, short_ptr.length);

      short_ptr = lp.lower;
      lp.lower = new short[lp.sum_alloc + 1];
      System.arraycopy(short_ptr, 0, lp.lower, 0, short_ptr.length);

      short_ptr = lp.must_be_int;
      lp.must_be_int = new short[lp.sum_alloc + 1];
      System.arraycopy(short_ptr, 0, lp.must_be_int, 0, short_ptr.length);

      int_ptr = lp.bas;
      lp.bas = new int[lp.rows_alloc + 1];
      System.arraycopy(int_ptr, 0, lp.bas, 0, int_ptr.length);

      db_ptr = lp.duals;
      lp.duals = new double[lp.rows_alloc + 1];
      System.arraycopy(db_ptr, 0, lp.duals, 0, db_ptr.length);

      short_ptr = lp.ch_sign;
      lp.ch_sign = new short[lp.rows_alloc + 1];
      System.arraycopy(short_ptr, 0, lp.ch_sign, 0, short_ptr.length);

      int_ptr = lp.eta_col_end;
      lp.eta_col_end = new int[lp.rows_alloc + lp.max_num_inv];
      System.arraycopy(int_ptr, 0, lp.eta_col_end, 0, int_ptr.length);

      if(lp.names_used != FALSE) {
	String[] str_ptr = lp.row_name;
	lp.row_name = new String[lp.rows_alloc + 1];
	System.arraycopy(str_ptr, 0, lp.row_name, 0, str_ptr.length);
      }

      if(lp.scaling_used != FALSE) {
	db_ptr = lp.scale;
	lp.scale = new double[lp.sum_alloc + 1];
	System.arraycopy(db_ptr, 0, lp.scale, 0, db_ptr.length);
      }

      if(lp.active != FALSE)
        set_globals(lp); 
    }
}

void inc_col_space(lprec lp)
{
  if(lp.columns >= lp.columns_alloc)
    {
      int[] int_ptr;
      lp.columns_alloc=lp.columns+10;
      lp.sum_alloc=lp.rows_alloc+lp.columns_alloc;

      short[] short_ptr = lp.must_be_int;
      lp.must_be_int = new short[lp.sum_alloc + 1];
      System.arraycopy(short_ptr, 0, lp.must_be_int, 0, short_ptr.length);

      double[] double_ptr = lp.orig_upbo;
      lp.orig_upbo = new double[lp.sum_alloc + 1];
      System.arraycopy(double_ptr, 0, lp.orig_upbo, 0, double_ptr.length);

      double_ptr = lp.upbo;
      lp.upbo = new double[lp.sum_alloc + 1];
      System.arraycopy(double_ptr, 0, lp.upbo, 0, double_ptr.length);

      double_ptr = lp.orig_lowbo;
      lp.orig_lowbo = new double[lp.sum_alloc + 1];
      System.arraycopy(double_ptr, 0, lp.orig_lowbo, 0, double_ptr.length);

      double_ptr = lp.lowbo;
      lp.lowbo = new double[lp.sum_alloc + 1];
      System.arraycopy(double_ptr, 0, lp.lowbo, 0, double_ptr.length);

      double_ptr = lp.solution;
      lp.solution = new double[lp.sum_alloc + 1];
      System.arraycopy(double_ptr, 0, lp.solution, 0, double_ptr.length);

      double_ptr = lp.best_solution;
      lp.best_solution = new double[lp.sum_alloc + 1];
      System.arraycopy(double_ptr, 0, lp.best_solution, 0, double_ptr.length);

      short_ptr = lp.basis;
      lp.basis = new short[lp.sum_alloc + 1];
      System.arraycopy(short_ptr, 0, lp.basis, 0, short_ptr.length);

      short_ptr = lp.lower;
      lp.lower = new short[lp.sum_alloc + 1];
      System.arraycopy(short_ptr, 0, lp.lower, 0, short_ptr.length);

      if(lp.names_used != FALSE) {
	String[] str_ptr = lp.col_name;
	lp.col_name = new String[lp.columns_alloc + 1];
	System.arraycopy(str_ptr, 0, lp.col_name, 0, str_ptr.length);
      }

      if(lp.scaling_used != FALSE) {
	double_ptr = lp.scale;
	lp.scale = new double[lp.sum_alloc + 1];
	System.arraycopy(double_ptr, 0, lp.scale, 0, double_ptr.length);
      }

      int_ptr = lp.col_end;
      lp.col_end = new int[lp.columns_alloc + 1];
      System.arraycopy(int_ptr, 0, lp.col_end, 0, int_ptr.length);

      if(lp.active != FALSE)
        set_globals(lp);
    }
} // end of inc_col_space

public void set_mat(lprec lp, int Row, int Column, double Value)
{
  int elmnr, lastelm, i;
//  System.err.println("lp.mat.length = " + lp.mat.length);

  if(Row > lp.rows || Row < 0)
    System.err.print("Row out of range");
  if(Column > lp.columns || Column < 1)
    System.err.print("Column out of range");
  if(lp.scaling_used != FALSE)
    Value *= lp.scale[Row] * lp.scale[lp.rows + Column];
  
   if(true /*abs(Value) > lp.epsilon*/)
    {
       if (lp.basis[Column] == TRUE && Row > 0)
         lp.basis_valid = FALSE;
       lp.eta_valid = FALSE;
       elmnr = lp.col_end[Column-1];
       while((elmnr < lp.col_end[Column]) ?
	     (lp.mat[elmnr].row_nr != Row) : false)
         elmnr++;

       if((elmnr != lp.col_end[Column]) ?
	  (lp.mat[elmnr].row_nr == Row) : false )
         if (lp.scaling_used != FALSE)
           {
             if (lp.ch_sign[Row] != FALSE)
               lp.mat[elmnr].value = 
		 -Value * lp.scale[Row] * lp.scale[Column];
             else
               lp.mat[elmnr].value =
		 Value * lp.scale[Row] * lp.scale[Column];
           }
         else
           {
             if (lp.ch_sign[Row] != FALSE)
               lp.mat[elmnr].value = -Value;
             else
               lp.mat[elmnr].value = Value;
           }
       else
         {
           /* check if more space is needed for matrix */
           inc_mat_space(lp,1);

           /* Shift the matrix */
           lastelm=lp.non_zeros; 
           for(i = lastelm; i > elmnr ; i--)
             lp.mat[i]=lp.mat[i-1];
           for(i = Column; i <= lp.columns; i++)
             lp.col_end[i]++;

           /* Set new element */
           lp.mat[elmnr].row_nr=Row;

           if (lp.scaling_used != FALSE)
             {
               if (lp.ch_sign[Row] != FALSE)
                 lp.mat[elmnr].value=-Value*lp.scale[Row]*lp.scale[Column];
               else
                 lp.mat[elmnr].value=Value*lp.scale[Row]*lp.scale[Column];
             }
           else
             {
               if (lp.ch_sign[Row] != FALSE)
                  lp.mat[elmnr].value=-Value;
               else
                 lp.mat[elmnr].value=Value;
             }

           lp.row_end_valid=FALSE;
            
           lp.non_zeros++;
           if (lp.active != FALSE)
             Non_zeros=lp.non_zeros;
        }      
    }
}

public void set_obj_fn(lprec lp, double[] row)
{
  int i;
  for(i = 1; i <= lp.columns; i++) {
    set_mat(lp, 0, i, row[i]);
  }
}

public void str_set_obj_fn(lprec lp, String row)
{
  double[] arow;
  arow = new double[lp.columns + 1];
  int i = 0;
  StringTokenizer stk = 
    new StringTokenizer(row);
  while (stk.hasMoreTokens() && i < lp.columns) {
    i++;
    arow[i] = Double.valueOf(stk.nextToken()).doubleValue(); 
  }
  if (i < lp.columns) {
    System.err.println("Not enough numbers in the string in str_set_obj_fn");
    System.exit(FAIL);
  }
  set_obj_fn( lp, arow );
}


public void add_constraint(lprec lp, double[] row, short constr_type, double rh)
{
  matrec[] newmat;
  int  i, j;
  int  elmnr;
  int  stcol;
  short[]  addtoo;

  addtoo = new short[lp.columns + 1];

  for(i = 1; i <= lp.columns; i++)
    if(row[i]!=0)
      {
	addtoo[i]=TRUE;
	lp.non_zeros++;
      }
    else
      addtoo[i]=FALSE;

  newmat = new matrec[lp.non_zeros];
  for (i = 0; i < newmat.length; i++)
    newmat[i] = new matrec(0, (double)0);

  inc_mat_space(lp, 0);
  lp.rows++;
  lp.sum++;
  inc_row_space(lp);

  if (lp.scaling_used != FALSE)
    {
      /* shift scale */
      for(i=lp.sum; i > lp.rows; i--)
        lp.scale[i]=lp.scale[i-1];
      lp.scale[lp.rows]=1;
    }

  if (lp.names_used != FALSE)
    lp.row_name[lp.rows] = new String("r_" + lp.rows);

  if(lp.scaling_used != FALSE && lp.columns_scaled != FALSE)
    for(i = 1; i <= lp.columns; i++)
      row[i] *= lp.scale[lp.rows+i];
     
  if(constr_type==GE)
    lp.ch_sign[lp.rows] = TRUE;
  else
    lp.ch_sign[lp.rows] = FALSE;

  elmnr = 0;
  stcol = 0;
  for(i = 1; i <= lp.columns; i++)
    {
      for(j = stcol; j < lp.col_end[i]; j++)
        {  
	  newmat[elmnr].row_nr=lp.mat[j].row_nr;
	  newmat[elmnr].value=lp.mat[j].value;
	  elmnr++;
        }
      if(addtoo[i] != FALSE)
        {
	  if(lp.ch_sign[lp.rows] != FALSE)
	    newmat[elmnr].value = -row[i];
	  else
	    newmat[elmnr].value = row[i];
	  newmat[elmnr].row_nr = lp.rows;
	  elmnr++;
        }
      stcol=lp.col_end[i];
      lp.col_end[i]=elmnr;
    }    
  
  lp.mat = newmat;

  for(i=lp.sum ; i > lp.rows; i--)
    {
      lp.orig_upbo[i]=lp.orig_upbo[i-1];
      lp.orig_lowbo[i]=lp.orig_lowbo[i-1];
      lp.basis[i]=lp.basis[i-1];
      lp.lower[i]=lp.lower[i-1];
      lp.must_be_int[i]=lp.must_be_int[i-1];
    }

  for(i= 1 ; i <= lp.rows; i++)
    if(lp.bas[i] >= lp.rows)
      lp.bas[i]++;

  if(constr_type==LE || constr_type==GE)
    {
      lp.orig_upbo[lp.rows]=lp.infinite;
    }
  else if(constr_type==EQ)
    {
      lp.orig_upbo[lp.rows]=0;
    }
  else
    {
      System.err.print("Wrong constraint type\n");
      System.exit(FAIL);
    }

  lp.orig_lowbo[lp.rows]=0;

  if(constr_type==GE && rh != 0)
    lp.orig_rh[lp.rows]=-rh;
  else
    lp.orig_rh[lp.rows]=rh;  

  lp.row_end_valid=FALSE;
 
  lp.bas[lp.rows]=lp.rows;
  lp.basis[lp.rows]=TRUE;
  lp.lower[lp.rows]=TRUE;   
 
  if (lp.active != FALSE)
    set_globals(lp);
  lp.eta_valid=FALSE;
}

public void str_add_constraint(lprec lp, String row_string,
	      short constr_type, double rh)
{
  int  i = 0;
  double[] aRow;
  aRow = new double[lp.columns + 1];
  
  StringTokenizer stk = 
    new StringTokenizer(row_string);
  while (stk.hasMoreTokens() && i < lp.columns) {
    i++;
    aRow[i] = Double.valueOf(stk.nextToken()).doubleValue(); 
  }       
  add_constraint(lp, aRow, constr_type, rh);
}

public void del_constraint(lprec lp, int del_row)
{
  int i, j;
  int elmnr;
  int startcol;

  if(del_row < 1 || del_row > lp.rows)
    {
      System.err.println("There is no constraint nr. " + del_row);
      System.exit(FAIL);
    }

  elmnr=0;
  startcol=0;

  for(i = 1; i <= lp.columns; i++)
    {
      for(j=startcol; j < lp.col_end[i]; j++)
	{
	  if(lp.mat[j].row_nr!=del_row)
	    {
	      lp.mat[elmnr]=lp.mat[j];
	      if(lp.mat[elmnr].row_nr > del_row)
		lp.mat[elmnr].row_nr--;
	      elmnr++;
	    }
	  else
	    lp.non_zeros--;
	}
      startcol=lp.col_end[i];
      lp.col_end[i]=elmnr;
    }
  for(i = del_row; i < lp.rows; i++)
    {
      lp.orig_rh[i] = lp.orig_rh[i+1];
      lp.ch_sign[i] = lp.ch_sign[i+1];
      lp.bas[i] = lp.bas[i+1];
      if (lp.names_used != FALSE)
        lp.row_name[i] = lp.row_name[i+1];
    }
  for(i = 1; i < lp.rows; i++)
    if(lp.bas[i] >  del_row)
      lp.bas[i]--;

  for(i=del_row; i < lp.sum; i++)
    {
      lp.lower[i]=lp.lower[i+1];
      lp.basis[i]=lp.basis[i+1];
      lp.orig_upbo[i]=lp.orig_upbo[i+1];
      lp.orig_lowbo[i]=lp.orig_lowbo[i+1];
      lp.must_be_int[i]=lp.must_be_int[i+1];
      if (lp.scaling_used != FALSE)
        lp.scale[i]=lp.scale[i+1];
    }

  lp.rows--;
  lp.sum--;

  lp.row_end_valid=FALSE;
  
  if (lp.active != FALSE)
    set_globals(lp);
  lp.eta_valid=FALSE;
  lp.basis_valid=FALSE; 
}

public void add_lag_con(lprec lp, double[] row, short con_type, double rhs)
{
  int i;
  double sign = 0;
  if(con_type == LE || con_type == EQ)
    sign = 1;
  else if(con_type == GE)
    sign = -1;
  else
    System.err.print("con_type not implemented\n");

  lp.nr_lagrange++;
  if(lp.nr_lagrange==1)
    {
      lp.lag_row = new double[lp.nr_lagrange][];
      lp.lag_rhs = new double[lp.nr_lagrange];
      lp.lambda = new double[lp.nr_lagrange];
      lp.lag_con_type = new short[lp.nr_lagrange];
      for (i = 0; i < lp.nr_lagrange; i++) {
	lp.lag_rhs[i] = 0;
	lp.lambda[i] = 0;
	lp.lag_con_type[i] = 0;
      }
    }
  else
    {
      double[][] db2_ptr = lp.lag_row;
      lp.lag_row = new double[lp.nr_lagrange][];
      System.arraycopy(db2_ptr, 0, lp.lag_row, 0, db2_ptr.length);

      double[] db_ptr = lp.lag_rhs;
      lp.lag_rhs = new double[lp.nr_lagrange];
      System.arraycopy(db_ptr, 0, lp.lag_rhs, 0, db_ptr.length);

      db_ptr = lp.lambda;
      lp.lambda = new double[lp.nr_lagrange];
      System.arraycopy(db_ptr, 0, lp.lambda, 0, db_ptr.length);

      short[] short_ptr = lp.lag_con_type;
      lp.lag_con_type = new short[lp.nr_lagrange];
      System.arraycopy(short_ptr, 0, lp.lag_con_type, 0, short_ptr.length);
    }
  lp.lag_row[lp.nr_lagrange-1] = new double[lp.columns + 1];
  lp.lag_rhs[lp.nr_lagrange-1]=rhs * sign;
  for( i=1; i <= lp.columns; i++)
    lp.lag_row[lp.nr_lagrange-1][i]=row[i] * sign;
  lp.lambda[lp.nr_lagrange-1]=0;
  lp.lag_con_type[lp.nr_lagrange-1]=(con_type == EQ)?(short)1
					:(short)0;
}

public void str_add_lag_con(lprec lp, String row, short con_type, double rhs)
{
  int  i = 0;
  double[] a_row;
  a_row = new double[lp.columns + 1];

  StringTokenizer stk = 
     new StringTokenizer(row);
  while (stk.hasMoreTokens() && i < lp.columns) {
    i++;
    a_row[i] = Double.valueOf(stk.nextToken()).doubleValue(); 
  }  
  if (i < lp.columns) {
    System.err.println("not enough value for str_add_lag_con");
    System.exit(FAIL);
  }
  add_lag_con(lp, a_row, con_type, rhs);
}

public void add_column(lprec lp, double[] column)
{
  int i, elmnr;

  lp.columns++;
  lp.sum++;
  inc_col_space(lp);
  inc_mat_space(lp, lp.rows+1);

  if (lp.scaling_used != FALSE)
    {
      for(i = 0; i <= lp.rows; i++)
	column[i]*=lp.scale[i];
      lp.scale[lp.sum]=1;
    }

  elmnr=lp.col_end[lp.columns-1];
  for(i = 0 ; i <= lp.rows ; i++)
    if(column[i] != 0)
      {
	lp.mat[elmnr].row_nr=i;
	if(lp.ch_sign[i] != FALSE)
	  lp.mat[elmnr].value=-column[i];
	else
	  lp.mat[elmnr].value=column[i];
	lp.non_zeros++;
	elmnr++;
      }
  lp.col_end[lp.columns]=elmnr;
  lp.orig_lowbo[lp.sum]=0;
  lp.orig_upbo[lp.sum]=lp.infinite;
  lp.lower[lp.sum]=TRUE;
  lp.basis[lp.sum]=FALSE;
  lp.must_be_int[lp.sum]=FALSE;
  if (lp.names_used != FALSE)
    lp.col_name[lp.columns] = new String("var_" + lp.columns);

 
  lp.row_end_valid=FALSE;

  if (lp.active != FALSE)
    {
      Sum=lp.sum;
      Columns=lp.columns;
      Non_zeros=lp.non_zeros;
    }
}

public void str_add_column(lprec lp, String col_string)
{
  int  i = 0;
  double[] aCol;
  aCol = new double[lp.rows + 1];

  StringTokenizer stk = 
     new StringTokenizer(col_string);
  while (stk.hasMoreTokens() && i < lp.rows) {
    i++;
    aCol[i] = Double.valueOf(stk.nextToken()).doubleValue(); 
  }
  if (i < lp.rows) {
    System.err.println("Bad String in str_add_column");
    System.exit(FAIL);
  }
  add_column(lp, aCol);
}

public void del_column(lprec lp, int column)
{
  int i, j, from_elm, to_elm, elm_in_col;
  if(column > lp.columns || column < 1)
    System.err.print("Column out of range in del_column");
  for(i = 1; i <= lp.rows; i++)
    {
      if(lp.bas[i]==lp.rows+column)
        lp.basis_valid=FALSE;
      else if(lp.bas[i] > lp.rows+column)
        lp.bas[i]--;
    }
  for(i = lp.rows+column; i < lp.sum; i++)
    {
      if (lp.names_used != FALSE)
        lp.col_name[i-lp.rows] =  lp.col_name[i-lp.rows+1];
      lp.must_be_int[i]=lp.must_be_int[i+1];
      lp.orig_upbo[i]=lp.orig_upbo[i+1];
      lp.orig_lowbo[i]=lp.orig_lowbo[i+1];
      lp.upbo[i]=lp.upbo[i+1];
      lp.lowbo[i]=lp.lowbo[i+1];
      lp.basis[i]=lp.basis[i+1];
      lp.lower[i]=lp.lower[i+1];
      if (lp.scaling_used != FALSE)
        lp.scale[i]=lp.scale[i+1];
    }
  for(i = 0; i < lp.nr_lagrange; i++)
    for(j = column; j <= lp.columns; j++)
      lp.lag_row[i][j]=lp.lag_row[i][j+1];
  to_elm=lp.col_end[column-1];
  from_elm=lp.col_end[column];
  elm_in_col=from_elm-to_elm;
  for(i = from_elm; i < lp.non_zeros; i++)
    {
      lp.mat[to_elm]=lp.mat[i];
      to_elm++;
    }
  for(i = column; i < lp.columns; i++)
    lp.col_end[i]=lp.col_end[i+1]-elm_in_col;
  lp.non_zeros -= elm_in_col;
  lp.row_end_valid=FALSE;
  lp.eta_valid=FALSE;

  lp.sum--;
  lp.columns--;
  if (lp.active != FALSE)
    set_globals(lp);
}

public void set_upbo(lprec lp, int column, double value)
{
  if(column > lp.columns || column < 1)
    System.err.print("Column out of range");
  if (lp.scaling_used != FALSE)
    value /= lp.scale[lp.rows + column];
  if(value < lp.orig_lowbo[lp.rows + column])
    System.err.print("Upperbound must be >= lowerbound"); 
  lp.eta_valid = FALSE;
  lp.orig_upbo[lp.rows+column] = value;
}

public void set_lowbo(lprec lp, int column, double value)
{
  if(column > lp.columns || column < 1)
    System.err.print("Column out of range");
  if (lp.scaling_used != FALSE)
    value /= lp.scale[lp.rows + column];
  if(value > lp.orig_upbo[lp.rows + column])
    System.err.print("Upperbound must be >= lowerbound"); 
  lp.eta_valid = FALSE;
  lp.orig_lowbo[lp.rows+column] = value;
}

public void set_int(lprec lp, int column, short must_be_int)
{
  if(column > lp.columns || column < 1)
    System.err.print("Column out of range");
  lp.must_be_int[lp.rows+column]=must_be_int;
  if(must_be_int==TRUE)
    if(lp.columns_scaled != FALSE)
      unscale_columns(lp);
}

public void set_rh(lprec lp, int row, double value)
{
  if(row > lp.rows || row < 0)
    System.err.print("Row out of Range");
  if(row == 0)			/* setting of RHS of OF not meaningful */
    {
      System.err.println("Warning: attempt to set RHS of objective function, ignored");
      return;
    }
  if (lp.scaling_used != FALSE)
    if (lp.ch_sign[row] != FALSE)
      lp.orig_rh[row] = -value * lp.scale[row];
    else
      lp.orig_rh[row] = value * lp.scale[row];
  else
    if (lp.ch_sign[row] != FALSE)
      lp.orig_rh[row] = -value;
    else
      lp.orig_rh[row] = value;
  lp.eta_valid = FALSE;
} 

public void set_rh_vec(lprec lp, double[] rh)
{
  int i;
  if (lp.scaling_used != FALSE)
    for(i = 1; i <= lp.rows; i++)
      if(lp.ch_sign[i] != FALSE)
        lp.orig_rh[i]=-rh[i]*lp.scale[i];
      else
        lp.orig_rh[i]=rh[i]*lp.scale[i];
  else
    for(i=1; i <= lp.rows; i++)
      if(lp.ch_sign[i] != FALSE)
        lp.orig_rh[i]=-rh[i];
      else
        lp.orig_rh[i]=rh[i];
  lp.eta_valid=FALSE;
}

public void str_set_rh_vec(lprec lp, String rh_string)
{
  int  i = 0;
  double[] newrh;
  newrh = new double[lp.rows + 1];

  StringTokenizer stk = 
     new StringTokenizer(rh_string);
  while (stk.hasMoreTokens() && i < lp.rows) {
    i++;
    newrh[i] = Double.valueOf(stk.nextToken()).doubleValue(); 
  }       
  set_rh_vec(lp, newrh);
}

public void set_maxim(lprec lp)
{
  int i;
  if(lp.maximise==FALSE)
    {
      for(i = 0; i < lp.non_zeros; i++)
	if(lp.mat[i].row_nr==0)
	  lp.mat[i].value*=-1;
      lp.eta_valid=FALSE;
    } 
  lp.maximise=TRUE;
  lp.ch_sign[0]=TRUE;
  if (lp.active != FALSE)
    Maximise=TRUE;
}

public void set_minim(lprec lp)
{
  int i;
  if(lp.maximise==TRUE)
    {
      for(i = 0; i < lp.non_zeros; i++)
	if(lp.mat[i].row_nr==0)
	  lp.mat[i].value = -lp.mat[i].value;
      lp.eta_valid=FALSE;
    } 
  lp.maximise=FALSE;
  lp.ch_sign[0]=FALSE;
  if (lp.active != FALSE)
    Maximise=FALSE;
}

public void set_constr_type(lprec lp, int row, short con_type)
{
  int i;
  if(row > lp.rows || row < 1)
    System.err.print("Row out of Range");
  if(con_type==EQ)
    {
      lp.orig_upbo[row]=0;
      lp.basis_valid=FALSE;
      if (lp.ch_sign[row] != FALSE)
        {
          for(i = 0; i < lp.non_zeros; i++)
            if(lp.mat[i].row_nr==row)
              lp.mat[i].value*=-1;
          lp.eta_valid=FALSE;
          lp.ch_sign[row]=FALSE;
          if(lp.orig_rh[row]!=0)
            lp.orig_rh[row]*=-1;
        }
    }
  else if(con_type==LE)
    {
      lp.orig_upbo[row]=lp.infinite;
      lp.basis_valid=FALSE;
      if (lp.ch_sign[row] != FALSE)
        {
          for(i = 0; i < lp.non_zeros; i++)
            if(lp.mat[i].row_nr==row)
              lp.mat[i].value*=-1;
          lp.eta_valid=FALSE;
          lp.ch_sign[row]=FALSE;
          if(lp.orig_rh[row]!=0)
            lp.orig_rh[row]*=-1;
        }
    }
  else if(con_type==GE)
    {
      lp.orig_upbo[row]=lp.infinite;
      lp.basis_valid=FALSE;
      if(lp.ch_sign[row] == FALSE)
        {
          for(i = 0; i < lp.non_zeros; i++)
            if(lp.mat[i].row_nr==row)
              lp.mat[i].value*=-1;
          lp.eta_valid=FALSE;
          lp.ch_sign[row]=TRUE;
          if(lp.orig_rh[row]!=0)
            lp.orig_rh[row]*=-1;
        }
    } 
  else
    System.err.print("Constraint type not (yet) implemented");
}

public double mat_elm(lprec lp, int row, int column)
{
  double value;
  int elmnr;
  if(row < 0 || row > lp.rows)
    System.err.print("Row out of range in mat_elm");
  if(column < 1 || column > lp.columns)
    System.err.print("Column out of range in mat_elm");
  value=0;
  elmnr=lp.col_end[column-1];
  while(lp.mat[elmnr].row_nr != row && elmnr < lp.col_end[column])
    elmnr++;
  if(elmnr != lp.col_end[column])
    {
      value = lp.mat[elmnr].value;
      if (lp.ch_sign[row] != FALSE)
        value = -value;
      if (lp.scaling_used != FALSE)
        value /= lp.scale[row] * lp.scale[lp.rows + column];
    }
  return(value);
}


public void get_row(lprec lp, int row_nr, double[] row)
{
  int i, j;

  if(row_nr < 0 || row_nr > lp.rows)
    System.err.print("Row nr. out of range in get_row");
  for(i = 1; i <= lp.columns; i++)
    {
      row[i]=0;
      for(j=lp.col_end[i-1]; j < lp.col_end[i]; j++)
        if(lp.mat[j].row_nr==row_nr)
          row[i]=lp.mat[j].value;
      if (lp.scaling_used != FALSE)
        row[i] /= lp.scale[lp.rows+i] * lp.scale[row_nr];
    }
  if(lp.ch_sign[row_nr] != FALSE)
    for(i=0; i <= lp.columns; i++)
      if(row[i]!=0)
        row[i] = -row[i];
}

public void get_column(lprec lp, int col_nr, double[] column)
{
  int i;

  if(col_nr < 1 || col_nr > lp.columns)
    System.err.print("Col. nr. out of range in get_column");
  for(i = 0; i <= lp.rows; i++)
    column[i]=0;
  for(i = lp.col_end[col_nr-1]; i < lp.col_end[col_nr]; i++)
    column[lp.mat[i].row_nr]=lp.mat[i].value;
  for(i = 0; i <= lp.rows; i++)
    if(column[i] !=0)
      {
	if(lp.ch_sign[i] != FALSE)
	  column[i]*=-1;
	if (lp.scaling_used != FALSE)
	  column[i]/=(lp.scale[i] * lp.scale[lp.rows+col_nr]);
      }
}

public void get_reduced_costs(lprec lp, double[] rc)
{
  int varnr, i, j;
  double f;

  if(lp.basis_valid == FALSE)
    System.err.print("Not a valid basis in get_reduced_costs");
  set_globals(lp);
  if(lp.eta_valid == FALSE)
    invert();  
  for(i = 1; i <= lp.sum; i++)
    rc[i] = 0;
  rc[0] = 1;
  btran(rc);
  for(i = 1; i <= lp.columns; i++)
    {
      varnr = lp.rows + i;
      if(Basis[varnr] == FALSE)
        if(Upbo[varnr] > 0)
	  {
            f = 0;
	    for(j = Col_end[i - 1]; j < Col_end[i]; j++)
	      f += rc[Mat[j].row_nr] * Mat[j].value;
            rc[varnr] = f;
	  }
    }
  for(i = 1; i <= Sum; i++)
    rc[i] = Util.round(rc[i], Epsd);
}   

short is_feasible(lprec lp, double[] values)
{
  int i, elmnr;
  double[] this_rhs;
  double dist;

  if (lp.scaling_used != FALSE)
    {
      for(i = lp.rows+1; i <= lp.sum; i++)
        if( values[i - lp.rows] < lp.orig_lowbo[i]*lp.scale[i]
	   || values[i - lp.rows] > lp.orig_upbo[i]*lp.scale[i])
          return(FALSE);
    }
  else
    {
      for(i = lp.rows+1; i <= lp.sum; i++)
        if(   values[i - lp.rows] < lp.orig_lowbo[i]
	   || values[i - lp.rows] > lp.orig_upbo[i])
          return(FALSE);
    }
  this_rhs = new double[lp.rows+1];
  for (i = 0; i <= lp.rows; i++)
    this_rhs[i] = 0;

  for(i = 1; i <= lp.columns; i++)
    for(elmnr = lp.col_end[i - 1]; elmnr < lp.col_end[i]; elmnr++)
      this_rhs[lp.mat[elmnr].row_nr] += lp.mat[elmnr].value * values[i]; 
  for(i = 1; i <= lp.rows; i++)
    {
      dist = lp.orig_rh[i] - this_rhs[i];
      dist = Util.round(dist, 0.001);
      if((lp.orig_upbo[i] == 0 && dist != 0) || dist < 0)
	  {
	    return(FALSE);
	  }     
    } 
  return(TRUE);
}

short column_in_lp(lprec lp, double[] testcolumn)
{
  int i, j;
  short ident;
  double value;

  if (lp.scaling_used != FALSE)
    for(i = 1; i <= lp.columns; i++)
      {
        ident = TRUE;
        j = lp.col_end[i-1];
        while(ident != FALSE && (j < lp.col_end[i]))
          {
            value = lp.mat[j].value;
            if(lp.ch_sign[lp.mat[j].row_nr] != FALSE)
              value = -value;
            value /= lp.scale[lp.rows+i];
            value /= lp.scale[lp.mat[j].row_nr];
            value -= testcolumn[lp.mat[j].row_nr];
            if(Math.abs(value) >  0.001) /* should be some epsilon? MB */
              ident=FALSE;
            j++; 
          }
        if(ident != FALSE)
          return(TRUE);
      }
  else
    for(i = 1; i <= lp.columns; i++)
      {
        ident = TRUE;
        j = lp.col_end[i-1];
        while(ident != FALSE && j < lp.col_end[i])
          {
            value = lp.mat[j].value;
            if(lp.ch_sign[lp.mat[j].row_nr] != FALSE)
              value *= -1;
            value -= testcolumn[lp.mat[j].row_nr];
            if( Math.abs(value) >  0.001 )
              ident=FALSE;
            j++;
          }
        if(ident != FALSE)
          return(TRUE);
      }
  return(FALSE);
}


public void print_lp(lprec lp)
{
  int i, j;
  double[] fatmat;
  fatmat = new double[(lp.rows + 1)*lp.columns];
  for (i = 0; i < fatmat.length; i++)
    fatmat[i] = 0;
  for(i = 1; i <= lp.columns; i++)
    for(j = lp.col_end[i-1]; j < lp.col_end[i]; j++)
      fatmat[(i - 1) * (lp.rows + 1) + lp.mat[j].row_nr]=lp.mat[j].value;

  System.out.println("problem name: " + lp.lp_name);
  System.out.print("          ");
  for(j = 1; j <= lp.columns; j++)
    if (lp.names_used != FALSE)
      System.out.print(lp.col_name[j]);
    else
      System.out.print("Var[" + j + "] ");
  if(lp.maximise != FALSE)
    {
      System.out.print("\nMaximise  ");
      for(j = 0; j < lp.columns; j++)
	System.out.print(" " + (-fatmat[j*(lp.rows+1)]) + " ");
    }
  else
    {
      System.out.print("\nMinimize  ");
      for(j = 0; j < lp.columns; j++)
	System.out.print(" " + fatmat[j*(lp.rows+1)] + " ");
    }
  System.out.print("\n");
  for(i = 1; i <= lp.rows; i++)
    {
      if (lp.names_used != FALSE)
	System.out.print(lp.row_name[i]);
      else
	System.out.print("Row[" + i + "]  ");
      for(j = 0; j < lp.columns; j++)
	if(lp.ch_sign[i] != FALSE && fatmat[j*(lp.rows+1)+i] != 0)
	  System.out.print( (-fatmat[j*(lp.rows+1)+i]) + " ");
	else
	  System.out.print(fatmat[j*(lp.rows+1)+i] + " ");
      if(lp.orig_upbo[i]==lp.infinite)
	if(lp.ch_sign[i] != FALSE)
	  System.out.print(">= ");
	else
	  System.out.print("<= ");
      else
	System.out.print(" = ");
      if(lp.ch_sign[i] != FALSE)
	System.out.println(-lp.orig_rh[i]);
      else
	System.out.println(lp.orig_rh[i]);
    }
  System.out.print("Type      ");
  for(i = 1; i <= lp.columns; i++)
    if(lp.must_be_int[lp.rows+i]==TRUE)
      System.out.print("     Int ");
    else
      System.out.print("    double ");
  System.out.print("\nupbo      ");
  for(i = 1; i <= lp.columns; i++)
    if(lp.orig_upbo[lp.rows+i]==lp.infinite)
      System.out.print("     Inf ");
    else
      System.out.print(lp.orig_upbo[lp.rows+i] + " ");
  System.out.print("\nlowbo     ");
  for(i = 1; i <= lp.columns; i++)
    System.out.print(lp.orig_lowbo[lp.rows+i] + " ");
  System.out.print("\n");
  for(i = 0; i < lp.nr_lagrange; i++)
    {
      System.out.print("lag[" + i + "]  ");
      for(j = 1; j <= lp.columns; j++)
	System.out.print(lp.lag_row[i][j] + " ");
      if(lp.orig_upbo[i]==lp.infinite)
	if(lp.lag_con_type[i] == GE)
	  System.out.print(">= ");
	else if(lp.lag_con_type[i] == LE)
	  System.out.print("<= ");
	else if(lp.lag_con_type[i] == EQ)
	  System.out.print(" = ");
      System.out.println(lp.lag_rhs[i]);
    }
}  

public void set_row_name(lprec lp, int row, String new_name)
{
  int i;

  if(lp.names_used == 0)
    {
      lp.row_name = new String[lp.rows_alloc + 1];
      lp.col_name = new String[lp.columns_alloc + 1];
      lp.names_used=TRUE;
      for(i = 0; i <= lp.rows; i++)
        lp.row_name[i] = new String("r_" +  i);
      for(i = 1; i <= lp.columns; i++)
        lp.col_name[i] = new String("var_" + i);
    }
  lp.row_name[row] =  new_name;
}

public void set_col_name(lprec lp, int column, String new_name)
{
  int i;
 
  if(lp.names_used == 0)
    {
      lp.row_name = new String[lp.rows_alloc + 1];
      lp.col_name = new String[lp.columns_alloc + 1];
      lp.names_used=TRUE;
      for(i = 0; i <= lp.rows; i++)
        lp.row_name[i] = new String("r_" + i);
      for(i = 1; i <= lp.columns; i++)
        lp.col_name[i] = new String("var_" + i);
    }
  lp.col_name[column] = new_name;
}

private double minmax_to_scale(double min, double max)
{
  double scale;

  /* should do something sensible when min or max is 0, MB */
  if((min == 0) || (max == 0))
    return((double)1);

  scale = 1 / Math.pow(Math.E, (Math.log(min) + Math.log(max)) / 2);
  return(scale);
}

public void unscale_columns(lprec lp)
{
  int i, j;

  /* unscale mat */
  for(j = 1; j <= lp.columns; j++)
    for(i = lp.col_end[j - 1]; i < lp.col_end[j]; i++)
      lp.mat[i].value /= lp.scale[lp.rows + j];

  /* unscale bounds as well */
  for(i = lp.rows + 1; i < lp.sum; i++)
    {
      if(lp.orig_lowbo[i] != 0)
	lp.orig_lowbo[i] *= lp.scale[i];
      if(lp.orig_upbo[i] != lp.infinite)
	lp.orig_upbo[i] *= lp.scale[i];
    }
    
  for(i=lp.rows+1; i<= lp.sum; i++)
    lp.scale[i]=1;
  lp.columns_scaled=FALSE;
  lp.eta_valid=FALSE;
}

public void unscale(lprec lp)
{
  int i, j;
  
  if (lp.scaling_used != FALSE)
    {

      /* unscale mat */
      for(j = 1; j <= lp.columns; j++)
        for(i = lp.col_end[j - 1]; i < lp.col_end[j]; i++)
          lp.mat[i].value /= lp.scale[lp.rows + j];

      /* unscale bounds */
      for(i = lp.rows + 1; i < lp.sum; i++)
	{
	  if(lp.orig_lowbo[i] != 0)
	    lp.orig_lowbo[i] *= lp.scale[i];
	  if(lp.orig_upbo[i] != lp.infinite)
	    lp.orig_upbo[i] *= lp.scale[i];
	}
    
      /* unscale the matrix */
      for(j = 1; j <= lp.columns; j++)
        for(i = lp.col_end[j-1]; i < lp.col_end[j]; i++)
          lp.mat[i].value /= lp.scale[lp.mat[i].row_nr];

      /* unscale the rhs! */
      for(i = 0; i <= lp.rows; i++)
        lp.orig_rh[i] /= lp.scale[i];
      
      lp.scaling_used=FALSE;
      lp.eta_valid=FALSE;
    }
}


public void auto_scale(lprec lp)
{
  int i, j, row_nr, IntUsed;
  double row_max[], row_min[], scalechange[], absval;

  if(lp.scaling_used == 0)
    {
      lp.scale = new double[lp.sum_alloc + 1];
      for(i = 0; i <= lp.sum; i++)
	lp.scale[i]=1;
    }
  
  row_max = new double[lp.rows + 1];
  row_min = new double[lp.rows + 1];
  scalechange = new double[lp.sum + 1];

  /* initialise min and max values */
  for(i = 0; i <= lp.rows; i++)
    {
      row_max[i]=0;
      row_min[i]=lp.infinite;
    }

  /* calculate min and max absolute values of rows */
  for(j = 1; j <= lp.columns; j++)
    for(i = lp.col_end[j - 1]; i < lp.col_end[j]; i++)
      {
	row_nr = lp.mat[i].row_nr;
	absval = Math.abs(lp.mat[i].value);
	if(absval!=0)
	  {
	    row_max[row_nr] = Math.max(row_max[row_nr], absval);
	    row_min[row_nr] = Math.min(row_min[row_nr], absval);
	  }
      }    
  /* calculate scale factors for rows */
  for(i = 0; i <= lp.rows; i++)
    {
      scalechange[i] = minmax_to_scale(row_min[i], row_max[i]);
      lp.scale[i] *= scalechange[i];
    }

  /* now actually scale the matrix */
  for(j = 1; j <= lp.columns; j++)
    for(i = lp.col_end[j - 1]; i < lp.col_end[j]; i++)
      lp.mat[i].value *= scalechange[lp.mat[i].row_nr];

  /* and scale the rhs! */
  for(i = 0; i <= lp.rows; i++)
    lp.orig_rh[i] *= scalechange[i];

  IntUsed=FALSE;
  i=lp.rows+1;
  while(IntUsed == FALSE && i <= lp.sum)
    {
      IntUsed=lp.must_be_int[i];
      i++;
    }
  if(IntUsed == FALSE)
    {  
      double col_max, col_min;

      /* calculate scales */
      for(j = 1; j <= lp.columns; j++)
	{
	  col_max = 0;
	  col_min = lp.infinite;
	  for(i = lp.col_end[j - 1]; i < lp.col_end[j]; i++)
	    {
	      if(lp.mat[i].value!=0)
		{
		  col_max = Math.max(col_max, Math.abs(lp.mat[i].value));
		  col_min = Math.min(col_min, Math.abs(lp.mat[i].value));
		}
	    }
	  scalechange[lp.rows + j]  = minmax_to_scale(col_min, col_max);
	  lp.scale[lp.rows + j] *= scalechange[lp.rows + j];
	}

      /* scale mat */
      for(j = 1; j <= lp.columns; j++)
	for(i = lp.col_end[j - 1]; i < lp.col_end[j]; i++)
	  lp.mat[i].value *= scalechange[lp.rows + j];

      /* scale bounds as well */
      for(i = lp.rows + 1; i < lp.sum; i++)
	{
	  if(lp.orig_lowbo[i] != 0)
	    lp.orig_lowbo[i] /= scalechange[i];
	  if(lp.orig_upbo[i] != lp.infinite)
	    lp.orig_upbo[i] /= scalechange[i];
	}
      lp.columns_scaled=TRUE;
    }
  lp.scaling_used=TRUE;
  lp.eta_valid=FALSE;
}

public void reset_basis(lprec lp)
{
  lp.basis_valid=FALSE;
}

public void print_solution(lprec lp)
{
  int i;

  System.out.println("Value of objective function: " + lp.best_solution[0]);

  /* print normal variables */
  for(i = 1; i <= lp.columns; i++)
      if (lp.names_used != FALSE)
	  System.out.println(lp.col_name[i] + " " + lp.best_solution[lp.rows+i]);
      else
	  System.out.println("Var [" + i + "]  " + lp.best_solution[lp.rows+i]);
  
  /* print achieved constraint values */
  if(lp.verbose != FALSE)
    {
      System.out.print("\nActual values of the constraints:\n");
      for(i = 1; i <= lp.rows; i++)
        if (lp.names_used != FALSE)
	  System.out.println(lp.row_name[i] + " " +
			     lp.best_solution[i]);
        else
          System.out.println("Row [" + i + "]  " + " " +
			     lp.best_solution[i]);  
    }

  if((lp.verbose != FALSE|| lp.print_duals != FALSE))
    {
      if(lp.max_level != 1)
        System.out.print(
	 "These are the duals from the node that gave the optimal solution.\n");
      else
        System.out.print("\nDual values:\n");
      for(i = 1; i <= lp.rows; i++)
        if (lp.names_used != FALSE)
	  System.out.println(lp.row_name[i] + " " +
			     lp.duals[i]);
        else
          System.out.println("Row [" + i + "]  " + lp.duals[i]); 
    }
} /* Printsolution */

public void write_LP(lprec lp, PrintStream output)
{
  int i, j;
  double[] row;
  
  row = new double[lp.columns+1];
  if(lp.maximise != FALSE)
    output.print( "max:");
  else
    output.print( "min:");

  get_row(lp, 0, row);
  for(i = 1; i <= lp.columns; i++)
    if(row[i] != 0)
      {
        if(row[i] == -1)
          output.print( " -");
        else if(row[i] == 1)
          output.print( " +");
        else 
          output.print(row[i]);
        if (lp.names_used != FALSE)
          output.print(lp.col_name[i]);
        else
          output.print("x" + i);
      }
  output.print( ";\n");

  for(j = 1; j <= lp.rows; j++)
    {
      if (lp.names_used != FALSE)
        output.print( lp.row_name[j]);
      get_row(lp, j, row);
      for(i = 1; i <= lp.columns; i++)
        if(row[i] != 0)
          {
            if(row[i] == -1)
              output.print( " -");
            else if(row[i] == 1)
              output.print( " +");
            else 
              output.print( " " + row[i] + " ");
            if (lp.names_used != FALSE)
              output.print( lp.col_name[i]);
            else
              output.print( "x" + i);
          }
      if(lp.orig_upbo[j] == 0)
        output.print( " =");
      else if(lp.ch_sign[j] != FALSE)
        output.print( " >");
      else
        output.print( " <");
      if(lp.ch_sign[j] != FALSE)
        output.println( " " + (-lp.orig_rh[j]));
      else
        output.println( " " + lp.orig_rh[j]);
    }
  for(i = lp.rows+1; i <= lp.sum; i++)
    {
      if(lp.orig_lowbo[i] != 0)
	{
	  if (lp.names_used != FALSE)
	    output.println(lp.col_name[i - lp.rows] + ">" +
		    lp.orig_lowbo[i] + ";");
	  else
	    output.print( "x" + (i-lp.rows) + " > " + lp.orig_lowbo[i] + ";");
	}
      if(lp.orig_upbo[i] != lp.infinite)
	{
	  if (lp.names_used != FALSE)
	    output.println(lp.col_name[i - lp.rows] + " < " +
		    lp.orig_upbo[i] + ";");
	  else
	    output.println( "x" + (i-lp.rows) + " < " + lp.orig_upbo[i] + ";");
	}
    }


  i=1;
  while(lp.must_be_int[lp.rows+i] == FALSE && i <= lp.columns)
    i++;
  if(i <= lp.columns)
    {
      if (lp.names_used != FALSE)  
        output.print( "\nint " +  lp.col_name[i]);
      else
        output.print( "\nint x" + i);
      i++;
      for(; i <= lp.columns; i++)
        if(lp.must_be_int[lp.rows+i] != FALSE)
          if (lp.names_used != FALSE)  
            output.print( "," + lp.col_name[i]);
          else
            output.print( ", x" + i);
      output.print( ";\n");
    }
}

/*
void write_MPS(lprec lp, FILE *output)
{
  int i, j, marker;
  double *column;


  MALLOC(column, lp.rows+1, double);
  marker=0;   
  output.print( "NAME          %s\n", lp.lp_name);
  output.print( "ROWS\n");
  for(i = 0; i <= lp.rows; i++)
    {
      if(i==0)
	output.print( " N  ");
      else
	if(lp.orig_upbo[i]==lp.infinite)
	  if(lp.ch_sign[i])
	    output.print( " G  ");
	  else
	    output.print( " L  ");
	else
	  output.print( " E  ");
      if (lp.names_used != FALSE)
	output.print( "%s\n", lp.row_name[i]);
      else
	output.print( "r_%d\n", i);
    }
      
  output.print( "COLUMNS\n");
  j = 0;
  for(i = 1; i <= lp.columns; i++)
    {
      if((lp.must_be_int[i+lp.rows]) && (marker % 2)==0)
	{
	  output.print(
		  "    MARK%04d  'MARKER'                 'INTORG'\n",
		  marker);
	  marker++;
	}
      if((!lp.must_be_int[i+lp.rows]) && (marker % 2)==1)
	{
	  output.print(
		  "    MARK%04d  'MARKER'                 'INTEND'\n",
		  marker);
	  marker++;
	}
      get_column(lp, i, column);
      j=0;
      if(lp.maximise)
	{
	  if(column[j] != 0)
	    { 
	      if (lp.names_used != FALSE)
		output.print( "    %-8s  %-8s  %g\n", lp.col_name[i],
			lp.row_name[j], -column[j]);
	      else
		output.print( "    var_%-4d  r_%-6d  %g\n", i, j,
			-column[j]);
	    }
	} 
      else
	{
	  if(column[j] != 0)
	    { 
	      if (lp.names_used != FALSE)
		output.print( "    %-8s  %-8s  %g\n", lp.col_name[i],
			lp.row_name[j], column[j]);
	      else
		output.print( "    var_%-4d  r_%-6d  %g\n", i, j,
			column[j]);
	    }
	}
      for(j=1; j <= lp.rows; j++)
	if(column[j] != 0)
	  { 
	    if (lp.names_used != FALSE)
	      output.print( "    %-8s  %-8s  %g\n", lp.col_name[i],
		      lp.row_name[j], column[j]);
	    else
	      output.print( "    var_%-4d  r_%-6d  %g\n", i, j,
		      column[j]);
	  }
    }
  if((marker % 2) ==1)
    {
      output.print( "    MARK%04d  'MARKER'                 'INTEND'\n",
	      marker);
      marker++;
    }

  output.print( "RHS\n");
  for(i = 1; i <= lp.rows; i++)
    {
      if(lp.ch_sign[i])
	{
	  if (lp.names_used != FALSE)
	    output.print( "    RHS       %-8s  %g\n", lp.row_name[i],
		    (double)-lp.orig_rh[i]);
	  else
	    output.print( "    RHS       r_%-6d  %g\n", i,
		    (double)-lp.orig_rh[i]);
	}
      else
	{
	  if (lp.names_used != FALSE)
	    output.print( "    RHS       %-8s  %g\n", lp.row_name[i],
		    (double)lp.orig_rh[i]);
	  else
	    output.print( "    RHS       r_%-6d  %g\n", i,
		    (double)lp.orig_rh[i]);
	}
    }
      
  output.print( "BOUNDS\n");
  if (lp.names_used != FALSE)
    for(i = lp.rows + 1; i <= lp.sum; i++)
      {
	if(lp.orig_upbo[i] < lp.infinite)
	  output.print( " UP BND       %-8s  %g\n",
		  lp.col_name[i- lp.rows], (double)lp.orig_upbo[i]);
	if(lp.orig_lowbo[i] != 0)
	  output.print( " LO BND       %-8s  %g\n",
		  lp.col_name[i- lp.rows], (double)lp.orig_lowbo[i]);
      }
  else
    for(i = lp.rows + 1; i <= lp.sum; i++)
      {
	if(lp.orig_upbo[i] < lp.infinite)
	  output.print( " UP BND       var_%-4d  %g\n",
		  i - lp.rows, (double)lp.orig_upbo[i]);
	if(lp.orig_lowbo[i] != 0)
	  output.print( " LO BND       var_%-4d  %g\n", i - lp.rows,
		  (double)lp.orig_lowbo[i]);
      }
  output.print( "ENDATA\n");
  free(column);
}
*/

public void print_duals(lprec lp)
{
  int i;
  for(i = 1; i <= lp.rows; i++)
    if (lp.names_used != FALSE)
      System.out.println(lp.row_name[i] + " [" + i + "] " + lp.duals[i]);
    else
      System.out.println("Dual       [" + i + "] " + lp.duals[i]);
}

public void print_scales(lprec lp)
{
  int i;
  if (lp.scaling_used != FALSE)
    {
      for(i = 0; i <= lp.rows; i++)
        System.out.println("Row[" + i + "]    scaled at " + lp.scale[i]);
      for(i = 1; i <= lp.columns; i++)
        System.out.println("Column[" + i + "] scaled at " + lp.scale[lp.rows + i]);
    }
}

private void print_indent()
{
  int i;

  System.out.print(Level);
  if(Level < 50) /* useless otherwise */
    for(i = Level; i > 0; i--)
      System.out.print("--");
  else
    System.out.print(" *** too deep ***");
  System.out.print("> ");
} /* print_indent */


public void debug_print_solution()
{
  int i;

  if(Lp.debug != FALSE)
    for (i = Rows + 1; i <= Sum; i++)
      {
	print_indent();
        if (Lp.names_used != FALSE)
	  System.out.println(Lp.col_name[i - Rows] + " " + Solution[i]);
        else 
          System.out.println("Var[" + (i-Rows) + "]   " + Solution[i]);
      }
} /* debug_print_solution */


public void debug_print_bounds(double[] upbo, double[] lowbo)
{
  int i;

  if(Lp.debug != FALSE)
    for(i = Rows + 1; i <= Sum; i++)
      {
	if(lowbo[i] != 0)
	  {
	    print_indent();
            if (Lp.names_used != FALSE)
	      System.out.println(Lp.col_name[i - Rows] + " > " +
				 lowbo[i]);
            else
              System.out.print("Var[" + (i-Rows) + "]  > " + lowbo[i]);
	  }
	if(upbo[i] != Infinite)
	  {
	    print_indent();
	    if (Lp.names_used != FALSE)
              System.out.println(Lp.col_name[i - Rows] + " < " +
				 upbo[i]);
            else
              System.out.println("Var[" + (i-Rows) + "]  < " + upbo[i]);
          }
      }
} /* debug_print_bounds */


public void debug_print(String format)
{
  if(Lp.debug != FALSE)
    {
      print_indent();
      System.out.print(format);
    }
} /* debug_print */

/* Globals used by solver */
short JustInverted;
short Status;
short Doiter;
short DoInvert;
short Break_bb;

void set_globals(lprec lp)
{
  if(Lp != null)
    Lp.active = FALSE;
  Lp = lp;
  Rows = lp.rows;
  Columns = lp.columns;
  Sum = Rows + Columns;
  Non_zeros = lp.non_zeros;
  Mat = lp.mat;
  Col_no = lp.col_no;
  Col_end = lp.col_end;
  Row_end = lp.row_end;
  Rh = lp.rh;
  Rhs = lp.rhs;
  Orig_rh = lp.orig_rh;
  Must_be_int = lp.must_be_int;
  Orig_upbo = lp.orig_upbo;
  Orig_lowbo = lp.orig_lowbo;
  Upbo = lp.upbo;
  Lowbo = lp.lowbo;
  Bas = lp.bas;
  Basis = lp.basis;
  Lower = lp.lower;
  Eta_alloc = lp.eta_alloc;
  Eta_size = lp.eta_size;
  Num_inv = lp.num_inv;
  Eta_value = lp.eta_value;
  Eta_row_nr = lp.eta_row_nr;
  Eta_col_end = lp.eta_col_end;
  Solution = lp.solution;
  Best_solution = lp.best_solution;
  Infinite = lp.infinite;
  Epsilon = lp.epsilon;
  Epsb = lp.epsb;
  Epsd = lp.epsd;
  Epsel = lp.epsel;

  /* ?? MB */
  TREJ = TREJ;
  TINV = TINV;

  Maximise = lp.maximise;
  Floor_first = lp.floor_first;
  lp.active = TRUE;

//  System.out.println("Sum = " + Sum);
} // end of set_global

private void ftran(int start,
	   int end,
	   double[] pcol)
{
  int  i, j, k, r;
  double theta;

  for(i = start; i <= end; i++)
    {
      k = Eta_col_end[i] - 1;
      r = Eta_row_nr[k];
      theta = pcol[r];
      if(theta != 0)
	for(j = Eta_col_end[i - 1]; j < k; j++)
	  pcol[Eta_row_nr[j]] += theta * Eta_value[j]; /* cpu expensive line */
      pcol[r] *= Eta_value[k];
    }
  /* round small values to zero */
  for(i = 0; i <= Rows; i++)
    Util.round(pcol[i], Epsel);
} /* ftran */

private void btran(double[] row)
{
  int  i, j, k;
  double f;

  for(i = Eta_size; i >= 1; i--)
    {
      f = 0;
      k = Eta_col_end[i] - 1;
      for(j = Eta_col_end[i - 1]; j <= k; j++)
	f += row[Eta_row_nr[j]] * Eta_value[j];
      f = Util.round(f, Epsel);
      row[Eta_row_nr[k]] = f;
    }
} /* btran */


static short Isvalid(lprec lp)
{
  int i, j, rownum[], colnum[];
  int num[], row_nr;

  if(lp.row_end_valid == FALSE)
    {
      num = new int[lp.rows + 1];
      rownum = new int[lp.rows + 1];
      for(i = 0; i <= lp.rows; i++)
        {
          num[i] = 0;
          rownum[i] = 0;
        }
      for(i = 0; i < lp.non_zeros; i++)
        rownum[lp.mat[i].row_nr]++;
      lp.row_end[0] = 0;
      for(i = 1; i <= lp.rows; i++)
        lp.row_end[i] = lp.row_end[i - 1] + rownum[i];
      for(i = 1; i <= lp.columns; i++)
        for(j = lp.col_end[i - 1]; j < lp.col_end[i]; j++)
          {
    	    row_nr = lp.mat[j].row_nr;
	    if(row_nr != 0)
	      {
	        num[row_nr]++;
	        lp.col_no[lp.row_end[row_nr - 1] + num[row_nr]] = i;
	      }
          }
      lp.row_end_valid = TRUE;
    }
  if(lp.valid != FALSE)
    return(TRUE);
  rownum = new int[lp.rows + 1];
  for (i = 0; i <= lp.rows; i++)
    rownum[i] = 0;
  colnum = new int[lp.columns + 1];
  for (i = 0; i <= lp.columns; i++)
    colnum[i] = 0;
  for(i = 1 ; i <= lp.columns; i++)
    for(j = lp.col_end[i - 1]; j < lp.col_end[i]; j++)
      {
        colnum[i]++;
        rownum[lp.mat[j].row_nr]++;
      }
  for(i = 1; i <= lp.columns; i++)
    if(colnum[i] == 0)
      if (lp.names_used != FALSE)
        System.err.print("Warning: Variable " + lp.col_name[i] + 
			 " not used in any constraints\n");
      else
        System.err.print("Warning: Variable " + i + " not used in any constaints\n");
  lp.valid = TRUE;
  return(TRUE);
} 

private void resize_eta()
{
  Eta_alloc *= 2;
  double[] db_ptr = Eta_value;
  Eta_value = new double[Eta_alloc];
  System.arraycopy(db_ptr, 0, Eta_value, 0, db_ptr.length);
  Lp.eta_value = Eta_value;
  
  int[] int_ptr = Eta_row_nr;
  Eta_row_nr = new int[Eta_alloc];
  System.arraycopy(int_ptr, 0, Eta_row_nr, 0, int_ptr.length);
  Lp.eta_row_nr = Eta_row_nr;
} /* resize_eta */


private void condensecol(int row_nr,
			double[] pcol)
{
  int i, elnr;
  
  elnr = Eta_col_end[Eta_size];

  if(elnr + Rows + 2 > Eta_alloc) /* maximum local growth of Eta */
    resize_eta();

  for(i = 0; i <= Rows; i++)
    if(i != row_nr && pcol[i] != 0)
      {
	Eta_row_nr[elnr] = i;
	Eta_value[elnr] = pcol[i];
	elnr++;
      }
  Eta_row_nr[elnr] = row_nr;
  Eta_value[elnr] = pcol[row_nr];
  elnr++;
  Eta_col_end[Eta_size + 1] = elnr;
} /* condensecol */


private void addetacol()
{
  int  i, j, k;
  double theta;
  
  j = Eta_col_end[Eta_size];
  Eta_size++;
  k = Eta_col_end[Eta_size];
  theta = 1 / (double) Eta_value[k - 1];
  Eta_value[k - 1] = theta;
  for(i = j; i < k - 1; i++)
    Eta_value[i] *= -theta;
  JustInverted = FALSE;
} /* addetacol */

private void setpivcol(short lower, 
		      int varin,
		      double[]   pcol)
{
  int  i, colnr;
  double f;
  
  if(lower != FALSE)
    f = 1;
  else
    f = -1;
  for(i = 0; i <= Rows; i++)
    pcol[i] = 0;
  if(varin > Rows)
    {
      colnr = varin - Rows;
      for(i = Col_end[colnr - 1]; i < Col_end[colnr]; i++)
	pcol[Mat[i].row_nr] = Mat[i].value * f;
      pcol[0] -= Extrad * f;
    }
  else
    if(lower != FALSE)
      pcol[varin] = 1;
    else
      pcol[varin] = -1;
  ftran(1, Eta_size, pcol);
} /* setpivcol */


private void minoriteration(int colnr,
			   int row_nr)
{
  int  i, j, k, wk, varin, varout, elnr;
  double piv = 0, theta;
  
  varin = colnr + Rows;
  elnr = Eta_col_end[Eta_size];
  wk = elnr;
  Eta_size++;
  if(Extrad != 0)
    {
      Eta_row_nr[elnr] = 0;
      Eta_value[elnr] = -Extrad;
      elnr++;
    }
  for(j = Col_end[colnr - 1] ; j < Col_end[colnr]; j++)
    {
      k = Mat[j].row_nr;
      if(k == 0 && Extrad != 0)
        Eta_value[Eta_col_end[Eta_size -1]] += Mat[j].value;
      else if(k != row_nr)
	{
	  Eta_row_nr[elnr] = k;
	  Eta_value[elnr] = Mat[j].value;
	  elnr++;
	}
      else
	piv = Mat[j].value;
    }
  Eta_row_nr[elnr] = row_nr;
  Eta_value[elnr] = 1 / (double) piv;
  elnr++;
  theta = Rhs[row_nr] / (double) piv;
  Rhs[row_nr] = theta;
  for(i = wk; i < elnr - 1; i++)
    Rhs[Eta_row_nr[i]] -= theta * Eta_value[i];
  varout = Bas[row_nr];
  Bas[row_nr] = varin;
  Basis[varout] = FALSE;
  Basis[varin] = TRUE;
  for(i = wk; i < elnr - 1; i++)
    Eta_value[i] /= - (double) piv;
  Eta_col_end[Eta_size] = elnr;
} /* minoriteration */

private void rhsmincol(double theta,
		      int row_nr,
		      int varin)
{
  int  i, j, k, varout;
  double f;
  
  if(row_nr > Rows + 1)
    {
      System.err.print("Error: rhsmincol called with row_nr: " +
		       row_nr + ", rows: " + Rows + "\n");
      System.err.print("This indicates numerical instability\n");
      System.exit(FAIL);
    }
  j = Eta_col_end[Eta_size];
  k = Eta_col_end[Eta_size + 1];
  for(i = j; i < k; i++)
    {
      f = Rhs[Eta_row_nr[i]] - theta * Eta_value[i];
      f = Util.round(f, Epsb);
      Rhs[Eta_row_nr[i]] = f;
    }
  Rhs[row_nr] = theta;
  varout = Bas[row_nr];
  Bas[row_nr] = varin;
  Basis[varout] = FALSE;
  Basis[varin] = TRUE;
} /* rhsmincol */


void invert()
{
  int    i, j, v, wk, numit, varnr, row_nr, colnr, varin;
  double   theta;
  double[]   pcol;
  short[]  frow;
  short[]  fcol;
  int[]    rownum, col, row;
  int[]    colnum;

  if(Lp.print_at_invert != FALSE) 
    System.out.print("Start Invert iter " + Lp.iter + " eta_size " + Eta_size +
		     " rhs[0] " + (-Rhs[0])+ " \n");

  rownum = new int[Rows + 1];
  for (i = 0; i <= Rows; i++)
    rownum[i] = 0;
  col = new int[Rows + 1];
  for (i = 0; i <= Rows; i++)
    col[i] = 0;
  row = new int[Rows + 1];
  for (i = 0; i <= Rows; i++)
    row[i] = 0;
  pcol = new double[Rows + 1];
  for (i = 0; i <= Rows; i++)
    pcol[i] = 0;
  frow = new short[Rows + 1];
  for(i = 0; i <= Rows; i++)
    frow[i] = TRUE;
  fcol = new short[Columns + 1];
  for(i = 0; i < Columns; i++)
    fcol[i] = FALSE;
  colnum = new int[Columns + 1];
  for(i = 0; i <= Columns; i++)
    colnum[i] = 0;

  for(i = 0; i <= Rows; i++)
    if(Bas[i] > Rows)
      fcol[Bas[i] - Rows - 1] = TRUE;
    else
      frow[Bas[i]] = FALSE;

  for(i = 1; i <= Rows; i++)
    if(frow[i] != FALSE)
      for(j = Row_end[i - 1] + 1; j <= Row_end[i]; j++)
	{
	  wk = Col_no[j];
	  if(fcol[wk - 1] != FALSE)
	    {
	      colnum[wk]++;
	      rownum[i - 1]++;
	    }
	}
  for(i = 1; i <= Rows; i++)
    Bas[i] = i;
  for(i = 1; i <= Rows; i++)
    Basis[i] = TRUE;
  for(i = 1; i <= Columns; i++)
    Basis[i + Rows] = FALSE;

  for(i = 0; i <= Rows; i++)
    Rhs[i] = Rh[i];

  for(i = 1; i <= Columns; i++)
    {
      varnr = Rows + i;
      if(Lower[varnr] == FALSE)
	{
	  theta = Upbo[varnr];
	  for(j = Col_end[i - 1]; j < Col_end[i]; j++)
	    Rhs[Mat[j].row_nr] -= theta * Mat[j].value;
	}
    }
  for(i = 1; i <= Rows; i++)
    if(Lower[i] == FALSE)
      Rhs[i] -= Upbo[i];
  Eta_size = 0;
  v = 0;
  row_nr = 0;
  Num_inv = 0;
  numit = 0;
  while(v < Rows)
    {
      row_nr++;
      if(row_nr > Rows)
	row_nr = 1;
      v++;
      if(rownum[row_nr - 1] == 1)
	if(frow[row_nr] != FALSE)
	  {
	    v = 0;
	    j = Row_end[row_nr - 1] + 1;
	    while(fcol[Col_no[j] - 1] == FALSE)
	      j++;
	    colnr = Col_no[j];
	    fcol[colnr - 1] = FALSE;
	    colnum[colnr] = 0;
	    for(j = Col_end[colnr - 1]; j < Col_end[colnr]; j++)
	      if(frow[Mat[j].row_nr] != FALSE)
		rownum[Mat[j].row_nr - 1]--;
	    frow[row_nr] = FALSE;
	    minoriteration(colnr, row_nr);
	  }
    }
  v = 0;
  colnr = 0;
  while(v <Columns)
    {
      colnr++;
      if(colnr > Columns)
	colnr = 1;
      v++;
      if(colnum[colnr] == 1)
	if(fcol[colnr - 1] != FALSE)
	  {
	    v = 0;
	    j = Col_end[colnr - 1] + 1;
	    while(frow[Mat[j - 1].row_nr] == FALSE)
	      j++;
	    row_nr = Mat[j - 1].row_nr;
	    frow[row_nr] = FALSE;
	    rownum[row_nr - 1] = 0;
	    for(j = Row_end[row_nr - 1] + 1; j <= Row_end[row_nr]; j++)
	      if(fcol[Col_no[j] - 1] != FALSE)
		colnum[Col_no[j]]--;
	    fcol[colnr - 1] = FALSE;
	    numit++;
	    col[numit - 1] = colnr;
	    row[numit - 1] = row_nr;
	  }
    }
  for(j = 1; j <= Columns; j++)
    if(fcol[j - 1] != FALSE)
      {
	fcol[j - 1] = FALSE;
	setpivcol(Lower[Rows + j], j + Rows, pcol);
	row_nr = 1;
	// re-arranged the order of conditionals to fix exception --BFT
	while(row_nr <= Rows && (frow[row_nr] == FALSE || pcol[row_nr] == FALSE))
	  row_nr++; /* this sometimes sets row_nr to Rows + 1 and makes
		       rhsmincol crash. Solved in 2.0? MB */
        if(row_nr == Rows + 1)
          System.err.print("Inverting failed");
	frow[row_nr] = FALSE;
        condensecol(row_nr, pcol);
	theta = Rhs[row_nr] / (double) pcol[row_nr];
	rhsmincol(theta, row_nr, Rows + j);
	addetacol();
      }
  for(i = numit - 1; i >= 0; i--)
    {
      colnr = col[i];
      row_nr = row[i];
      varin = colnr + Rows;
      for(j = 0; j <= Rows; j++)
	pcol[j] = 0;
      for(j = Col_end[colnr - 1]; j < Col_end[colnr]; j++)
	pcol[Mat[j].row_nr] = Mat[j].value;
      pcol[0] -= Extrad;
      condensecol(row_nr, pcol);
      theta = Rhs[row_nr] / (double) pcol[row_nr];
      rhsmincol(theta, row_nr, varin);
      addetacol();
    }
  for(i = 1; i <= Rows; i++)
    Rhs[i] = Util.round(Rhs[i], Epsb);
  if(Lp.print_at_invert != FALSE) 
    System.out.print("End Invert                eta_size " + Eta_size + " rhs[0] " +
		     (-Rhs[0]) + "\n"); 

  JustInverted = TRUE;
  DoInvert = FALSE;
} /* invert */

private short colprim(Ref colnr,
		     short minit,
		     double[]   drow)
{
  int  varnr, i, j;
  double f, dpiv;
  
  dpiv = -Epsd;
  colnr.value = 0;
  if(minit == FALSE)
    {
      for(i = 1; i <= Sum; i++)
	drow[i] = 0;
      drow[0] = 1;
      btran(drow);
      for(i = 1; i <= Columns; i++)
	{
	  varnr = Rows + i;
	  if(Basis[varnr] == FALSE)
	    if(Upbo[varnr] > 0)
	      {
		f = 0;
		for(j = Col_end[i - 1]; j < Col_end[i]; j++)
		  f += drow[Mat[j].row_nr] * Mat[j].value;
		drow[varnr] = f;
	      }
	}
      for(i = 1; i <= Sum; i++)
        drow[i] = Util.round(drow[i], Epsd);
    }
/*
  System.out.println("dpiv = " + dpiv);
  System.out.println("drow[] at colprim");
  for(i = 1; i <= Sum; i++) // DEBUG
    {
      System.out.print(drow[i] + " ");
    }
  System.out.println();
*/
  for(i = 1; i <= Sum; i++)
    if(Basis[i] == FALSE)
      if(Upbo[i] > 0)
	{
	  if(Lower[i] != FALSE)
	    f = drow[i];
	  else
	    f = -drow[i];
	  if(f < dpiv)
	    {
	      dpiv = f;
	      colnr.value = i;
	    }
	}
  if(Lp.trace != FALSE)
    if(colnr.value > 0)
      System.err.print("col_prim:" + colnr.value + ", reduced cost: " + dpiv + "\n");
    else
      System.err.print("col_prim: no negative reduced costs found, optimality!\n");
  if(colnr.value == 0)
    {
      Doiter   = FALSE;
      DoInvert = FALSE;
      Status   = OPTIMAL;
    }
  return(colnr.value > 0 ? (short)1 : (short)0);
} /* colprim */

private short rowprim(int colnr,
                     Ref row_nr,
		     Ref theta,
		     double[] pcol)
{
  int  i;
  double f = 0, quot; 

  row_nr.value = 0;
  theta.value = Infinite;
  for(i = 1; i <= Rows; i++)
    {
      f = pcol[i];
      if(Math.abs(f) < TREJ)
        f = 0;
      if(f != 0)
	{
          quot = 2 * Infinite;
	  if(f > 0)
	    quot = Rhs[i] / (double) f;
	  else
	    if(Upbo[Bas[i]] < Infinite)
	      quot = (Rhs[i] - Upbo[Bas[i]]) / (double) f;
          Util.round(quot, Epsel);
	  if(quot < theta.value)
	    {
              theta.value = quot;
	      row_nr.value = i;
	    }
	}
    }
  if(row_nr.value == 0)  
    for(i = 1; i <= Rows; i++)
      {
        f = pcol[i];
        if(f != 0)
	  {
            quot = 2 * Infinite;
	    if(f > 0)
	      quot = Rhs[i] / (double) f;
	    else
	      if(Upbo[Bas[i]] < Infinite)
	        quot = (Rhs[i] - Upbo[Bas[i]]) / (double) f;
            quot = Util.round(quot, Epsel);
	    if(quot < theta.value)
	      {
                theta.value = quot;
	        row_nr.value = i;
	      }
	  }
      }

  if(theta.value < 0)
    {
      System.err.println("Warning: Numerical instability, qout = " + theta.value);
      System.err.println("pcol[" + row_nr.value + "] = " + f + ", rhs[" +
			 row_nr.value + "] = " + Rhs[(int)row_nr.value] +
			 " , upbo = " + Upbo[Bas[(int)row_nr.value]]);
    }
  if(row_nr.value == 0)
    {
      if(Upbo[colnr] == Infinite)
        {
          Doiter   = FALSE;
          DoInvert = FALSE;
          Status   = UNBOUNDED;
        }
      else
        {
          i = 1;
          while(pcol[i] >= 0 && i <= Rows)
            i++;
          if(i > Rows) /* empty column with upperbound! */
            {
              Lower[colnr] = FALSE;
              Rhs[0] += Upbo[colnr]*pcol[0];
              Doiter = FALSE;
              DoInvert = FALSE;
            }
          else if(pcol[i]<0)
            {
              row_nr.value = i;
            }
        }
    }
  if(row_nr.value > 0)
    Doiter = TRUE;
  if(Lp.trace != FALSE)
    System.out.println("row_prim:" + row_nr.value + ", pivot element:" +
		       pcol[(int)row_nr.value]);

  return((row_nr.value > 0) ? (short)1 : (short)0);
} /* rowprim */

private short rowdual(Ref row_nr)
{
  int   i;
  double  f, g, minrhs;
  short artifs;

  row_nr.value = 0;
  minrhs = -Epsb;
  i = 0;
  artifs = FALSE;
  while(i < Rows && artifs == FALSE)
    {
      i++;
      f = Upbo[Bas[i]];
      if(f == 0 && (Rhs[i] != 0))	
	{
	  artifs = TRUE;
	  row_nr.value = i;
        }
      else
	{
	  if(Rhs[i] < f - Rhs[i])
	    g = Rhs[i];
	  else
	    g = f - Rhs[i];
	  if(g < minrhs)
	    {
	      minrhs = g;
	      row_nr.value = i;
	    }
	}
    }

  if(Lp.trace != FALSE)
    {  
      if(row_nr.value > 0)
        { 
          System.out.println("row_dual:" + row_nr.value + 
			   ", rhs of selected row:           " 
			   + Rhs[(int)row_nr.value]);
          if(Upbo[Bas[(int)row_nr.value]] < Infinite)
            System.out.println("               upper bound of basis variable:    " +
			       Upbo[Bas[(int)row_nr.value]]);
        }
      else
        System.out.print("row_dual: no infeasibilities found\n");
    }
    
  return(row_nr.value > 0 ? (short)1 : (short)0);
} /* rowdual */

private short coldual(int row_nr,
		     Ref colnr,
		     short minit,
		     double[] prow,
		     double[] drow)
{
  int  i, j, r, varnr;
  double theta, quot, pivot, d, f, g;
  
  Doiter = FALSE;
  if(minit == FALSE)
    {
      for(i = 0; i <= Rows; i++)
	{
	  prow[i] = 0;
	  drow[i] = 0;
	}
      drow[0] = 1;
      prow[row_nr] = 1;
      for(i = Eta_size; i >= 1; i--)
	{
	  d = 0;
	  f = 0;
	  r = Eta_row_nr[Eta_col_end[i] - 1];
	  for(j = Eta_col_end[i - 1]; j < Eta_col_end[i]; j++)
	    {
	      /* this is where the program consumes most cpu time */
	      f += prow[Eta_row_nr[j]] * Eta_value[j];
	      d += drow[Eta_row_nr[j]] * Eta_value[j];
	    }
          f = Util.round(f, Epsel);
	  prow[r] = f;
          d = Util.round(d, Epsel);
	  drow[r] = d;
	}
      for(i = 1; i <= Columns; i++)
	{
	  varnr = Rows + i;
	  if(Basis[varnr] == FALSE)
	    {
	      d = - Extrad * drow[0];
	      f = 0;
	      for(j = Col_end[i - 1]; j < Col_end[i]; j++)
		{
		  d = d + drow[Mat[j].row_nr] * Mat[j].value;
		  f = f + prow[Mat[j].row_nr] * Mat[j].value;
		}
	      drow[varnr] = d;
	      prow[varnr] = f;
	    }
	}
      for(i = 0; i <= Sum; i++)
	{
          prow[i] = Util.round(prow[i], Epsel);
          drow[i] = Util.round(drow[i], Epsd);
	}
    }
  if(Rhs[row_nr] > Upbo[Bas[row_nr]])
    g = -1;
  else
    g = 1;
  pivot = 0;
  colnr.value = 0;
  theta = Infinite;
  for(i = 1; i <= Sum; i++)
    {
      if(Lower[i] != FALSE)
	d = prow[i] * g;
      else
	d = -prow[i] * g;
      if((d < 0) && (Basis[i] == FALSE) && (Upbo[i] > 0))
	{
	  if(Lower[i] == FALSE)
	    quot = -drow[i] / (double) d;
	  else
	    quot = drow[i] / (double) d;
	  if(quot < theta)
	    {
	      theta = quot;
	      pivot = d;
	      colnr.value = i;
	    }
	  else if((quot == theta) && (Math.abs(d) > Math.abs(pivot)))
	    {
	      pivot = d;
	      colnr.value = i;
	    }
	}
    }
  if(Lp.trace != FALSE)
    System.out.println("col_dual:" + colnr.value + ", pivot element:  " +
		     prow[(int)colnr.value]);

  if(colnr.value > 0)
    Doiter = TRUE;

  return(colnr.value > 0 ? (short)1 : (short)0);
} /* coldual */

private void iteration(int row_nr,
		      int varin,
		      Ref theta,
		      double up,
		      Ref minit,
		      Ref low,
		      short primal,
                      double[] pcol)
{
  int  i, k, varout;
  double f;
  double pivot;
  
  Lp.iter++;
  minit.value = theta.value > (up + Epsb) ? 1 : 0;
  if(minit.value != 0)
    {
      theta.value = up;
      low.value = low.value == 0 ? 1 : 0;
    }
  k = Eta_col_end[Eta_size + 1];
  pivot = Eta_value[k - 1];
  for(i = Eta_col_end[Eta_size]; i < k; i++)
    {
      f = Rhs[Eta_row_nr[i]] - theta.value * Eta_value[i];
      f = Util.round(f, Epsb);
      Rhs[Eta_row_nr[i]] = f;
    }
  if(minit.value == 0)
    {
      Rhs[row_nr] = theta.value;
      varout = Bas[row_nr];
      Bas[row_nr] = varin;
      Basis[varout] = FALSE;
      Basis[varin] = TRUE;
      if(primal != FALSE && pivot < 0)
	Lower[varout] = FALSE;
      if(low.value == 0 && up < Infinite)
	{
	  low.value = TRUE;
	  Rhs[row_nr] = up - Rhs[row_nr];
	  for(i = Eta_col_end[Eta_size]; i < k; i++)
	    Eta_value[i] = -Eta_value[i];
	}
      addetacol();
      Num_inv++;
    }
  if(Lp.trace != FALSE)
    {
      System.out.print("Theta = " + theta.value + " ");
      if(minit.value != 0)
        {
          if(Lower[varin] == FALSE)
            System.out.print("Iteration:" + Lp.iter +
			     ", variable" + varin + " changed from 0 to its upper bound of "
			     + Upbo[varin] + "\n");
          else
            System.out.print("Iteration:" + Lp.iter + ", variable" + varin +
			     " changed its upper bound of " + Upbo[varin] + " to 0\n");
        }
      else
        System.out.print("Iteration:" + Lp.iter + ", variable" + varin + 
			 " entered basis at:" + Rhs[row_nr] + "\n");
      if(primal == 0)
	{
	  f = 0;
	  for(i = 1; i <= Rows; i++)
	    if(Rhs[i] < 0)
	      f -= Rhs[i];
	    else
	      if(Rhs[i] > Upbo[Bas[i]])
		f += Rhs[i] - Upbo[Bas[i]];
	  System.out.println("feasibility gap of this basis:" + (double)f);
	}
      else
	System.out.println("objective function value of this feasible basis: " + Rhs[0]);
    }
} /* iteration */


private int solvelp()
{
  int    i, j, varnr;
  double   f = 0, theta = 0;
  short  primal;
  double[]   drow, prow, Pcol;
  short  minit;
  int    colnr, row_nr;
  colnr = 0;
  row_nr = 0;
  short flag; 
  Ref ref1, ref2, ref3;
  ref1 = new Ref(0);
  ref2 = new Ref(0);
  ref3 = new Ref(0);
  
  drow = new double[Sum + 1];
  prow = new double[Sum + 1];
  Pcol = new double[Rows + 1];
  for (i = 0; i <= Sum; i++) {
    drow[i] = 0;
    prow[i] = 0;
  }
  for (i = 0; i <= Rows; i++)
    Pcol[i] = 0;


  Lp.iter = 0;
  minit = FALSE;
  Status = RUNNING;
  DoInvert = FALSE;
  Doiter = FALSE;
  i = 0;
  primal = TRUE;
  while(i != Rows && primal != FALSE)
    {
      i++;
      primal = (Rhs[i] >= 0 && Rhs[i] <= Upbo[Bas[i]]) ? 
	(short)1: (short)0;
    }
  if(Lp.trace != FALSE)
    {
      if(primal != FALSE)
        System.out.print("Start at feasible basis\n");
      else
        System.out.print("Start at infeasible basis\n");
    } 
  if(primal == FALSE)
    {
      drow[0] = 1;
      for(i = 1; i <= Rows; i++)
	drow[i] = 0;
      Extrad = 0;
      for(i = 1; i <= Columns; i++)
	{
	  varnr = Rows + i;
	  drow[varnr] = 0;
	  for(j = Col_end[i - 1]; j < Col_end[i]; j++)
	    if(drow[Mat[j].row_nr] != 0)
	      drow[varnr] += drow[Mat[j].row_nr] * Mat[j].value;
          if(drow[varnr] < Extrad)
            Extrad = drow[varnr];
	}
    }
  else
    Extrad = 0;
  if(Lp.trace != FALSE)
    System.out.println("Extrad = " + Extrad);
  minit = FALSE;

  while(Status == RUNNING)
    {
      Doiter = FALSE;
      DoInvert = FALSE;
      
      construct_solution(Solution);
      debug_print_bounds(Upbo, Lowbo);
      debug_print_solution();
      if(primal != FALSE)
        {
	  ref1.value = colnr;
	  flag = colprim(ref1, minit, drow);
	  colnr = (int)ref1.value;
	  if (flag != FALSE)
	    {
	      setpivcol(Lower[colnr], colnr, Pcol);
	      ref1.value = row_nr;
	      ref2.value = theta;
	      flag = rowprim(colnr, ref1, ref2, Pcol);
	      row_nr = (int)ref1.value;
	      theta = ref2.value;
	      if (flag != FALSE)
		condensecol(row_nr, Pcol);
	          
	    }
        }
      else /* not primal */
        {
	  if(minit == FALSE) {
	    ref1.value = row_nr;
	    flag = rowdual(ref1);
	    row_nr = (int)ref1.value;
	  }
	  if(row_nr > 0 )
	    {
	      ref1.value = colnr;
	      flag = coldual(row_nr, ref1, minit, prow, drow);
	      colnr = (int)ref1.value;
	      if (flag != FALSE)
	        {
	          setpivcol(Lower[colnr], colnr, Pcol);
                  /* getting div by zero here ... MB */
		  if(Pcol[row_nr] == 0)
	            {
		      System.err.println("An attempt was made to divide by zero (Pcol[" +
				       row_nr + "])");
		      System.err.println("This indicates numerical instability");
                      Doiter = FALSE;
                      if(JustInverted == FALSE)
                        {
                          System.out.println("Reinverting Eta");
                          DoInvert = TRUE;
                        }
                      else
                        {
                          System.out.println("Can't reinvert, failure");
                          Status = FAILURE;
                        }
		    }
                  else
                    {
                      condensecol(row_nr, Pcol);
	              f = Rhs[row_nr] - Upbo[Bas[row_nr]];
	              if(f > 0)
	                {
	                  theta = f / (double) Pcol[row_nr];
	                  if(theta <= Upbo[colnr])
	                    Lower[Bas[row_nr]] = 
				(Lower[Bas[row_nr]] == FALSE)? 
				(short)1:(short)0;
	                }
	              else /* f <= 0 */
	                theta = Rhs[row_nr] / (double) Pcol[row_nr];
	            }
                }
              else
                Status = INFEASIBLE;
       	    }
          else
            {
              primal   = TRUE;
              Doiter   = FALSE;
              Extrad   = 0;
              DoInvert = TRUE;
            }	  
	}
      if(Doiter != FALSE) {
	ref1.value = theta;
	ref2.value = minit;
	ref3.value = Lower[colnr];
        iteration(row_nr, colnr, ref1, Upbo[colnr], ref2, ref3, primal, Pcol);
	theta = ref1.value;
	minit = (short)ref2.value;
	Lower[colnr] = (short)ref3.value;
      }
      if(Num_inv >= Lp.max_num_inv)
        DoInvert = TRUE;
      if(DoInvert != FALSE)
        {
          if(Lp.print_at_invert != FALSE)
	    System.out.println("Inverting: Primal = " + primal);
          invert();
        }
    } 

  Lp.total_iter += Lp.iter;
  
  return(Status);
} /* solvelp */


private short is_int(double value)
{
  double   tmp;

  tmp = value - Math.floor(value);
  if(tmp < Epsilon)
    return(TRUE);
  if(tmp > (1 - Epsilon))
    return(TRUE);
  return(FALSE);
} /* is_int */

private void construct_solution(double[]   sol)
{
  int    i, j, basi;
  double   f;

  /* zero all results of rows */
  for (i = 0; i <= Rows; i++)
    sol[i] = 0;

  if (Lp.scaling_used != FALSE)
    {
      for(i = Rows + 1; i <= Sum; i++)
        sol[i] = Lowbo[i] * Lp.scale[i];
      for(i = 1; i <= Rows; i++)
        {
          basi = Bas[i];
          if(basi > Rows)
	    sol[basi] += Rhs[i] * Lp.scale[basi];
        }
      for(i = Rows + 1; i <= Sum; i++)
        if(Basis[i] == FALSE && Lower[i] == FALSE)
          sol[i] += Upbo[i] * Lp.scale[i];

      for(j = 1; j <= Columns; j++)
        {
          f = sol[Rows + j];
          if(f != 0)
	    for(i = Col_end[j - 1]; i < Col_end[j]; i++)
	      sol[Mat[i].row_nr] += (f / Lp.scale[Rows+j])
		* (Mat[i].value / Lp.scale[Mat[i].row_nr]);
        }
  
      for(i = 0; i <= Rows; i++)
        {
          if(Math.abs(sol[i]) < Epsb)
	    sol[i] = 0;
          else
	    if(Lp.ch_sign[i] != FALSE)
	      sol[i] = -sol[i];
        }
    }
  else
    {
      for(i = Rows + 1; i <= Sum; i++)
        sol[i] = Lowbo[i];
      for(i = 1; i <= Rows; i++)
        {
          basi = Bas[i];
          if(basi > Rows)
	    sol[basi] += Rhs[i];
        }
      for(i = Rows + 1; i <= Sum; i++)
        if(Basis[i] == FALSE && Lower[i] == FALSE)
          sol[i] += Upbo[i];
      for(j = 1; j <= Columns; j++)
        {
          f = sol[Rows + j];
          if(f != 0)
	    for(i = Col_end[j - 1]; i < Col_end[j]; i++)
	      sol[Mat[i].row_nr] += f * Mat[i].value;
        }
  
      for(i = 0; i <= Rows; i++)
        {
          if(Math.abs(sol[i]) < Epsb)
	    sol[i] = 0;
          else
	    if(Lp.ch_sign[i] != FALSE)
	      sol[i] = -sol[i];
        }
    }
} /* construct_solution */

private void calculate_duals()
{
  int i;

  /* initialise */
  for(i = 1; i <= Rows; i++)
    Lp.duals[i] = 0;
  Lp.duals[0] = 1;
  btran(Lp.duals);
  if (Lp.scaling_used != FALSE)
    for(i = 1; i <= Rows; i++)
      Lp.duals[i] *= Lp.scale[i]/Lp.scale[0];

  /* the dual values are the reduced costs of the slacks */
  /* When the slack is at its upper bound, change the sign. Can this happen? */
  for(i = 1; i <= Rows; i++)
    {
      if(Lp.basis[i] != FALSE)
        Lp.duals[i] = 0;
      else if( Lp.ch_sign[0] == Lp.ch_sign[i])
        Lp.duals[i] = -Lp.duals[i];
    }
}

private int milpsolve(double[]   upbo,
		      double[]   lowbo,
		      short[]  sbasis,
		      short[]  slower,
		      int[]    sbas)
{
  int i, j, failure, notint, is_worse;
  double theta, tmpdouble;
  Random rdm = new Random();
  notint = 0;

  if(Break_bb != FALSE)
    return(BREAK_BB);
  Level++;
  Lp.total_nodes++;
  if(Level > Lp.max_level)
    Lp.max_level = Level;
  debug_print("starting solve\n");
  /* make fresh copies of upbo, lowbo, rh as solving changes them */
  System.arraycopy(upbo, 0, Upbo, 0, Sum + 1);
  System.arraycopy(lowbo, 0, Lowbo, 0, Sum + 1);
  System.arraycopy(sbasis, 0, Basis, 0, Sum + 1);
  System.arraycopy(slower, 0, Lower, 0, Sum + 1);
  System.arraycopy(sbas, 0, Bas, 0, Rows + 1);
  System.arraycopy(Orig_rh, 0, Rh, 0, Rows + 1);

  if(Lp.anti_degen != FALSE)
    {
      for(i = 1; i <= Columns; i++)
        {
          tmpdouble = rdm.nextDouble()*0.001;
	  if(tmpdouble > Epsb)
            Lowbo[i + Rows] -= tmpdouble;
          tmpdouble = rdm.nextDouble()*0.001;
	  if(tmpdouble > Epsb)
            Upbo[i + Rows] += tmpdouble;
        }
      Lp.eta_valid = FALSE;
    }

  if(Lp.eta_valid == FALSE)
    {
      for(i = 1; i <= Columns; i++)
	if(Lowbo[Rows + i] != 0)
	  {
	    theta = Lowbo[ Rows + i];
	    if(Upbo[Rows + i]<Infinite)
	      Upbo[Rows + i] -= theta;
	    for(j = Col_end[i - 1]; j < Col_end[i]; j++)
	      Rh[Mat[j].row_nr] -= theta * Mat[j].value;
	  }
      invert();
      Lp.eta_valid = TRUE;
    }

  failure = solvelp();

  if(Lp.anti_degen != FALSE)
    {
      System.arraycopy(upbo, 0, Upbo, 0, Sum + 1);
      System.arraycopy(lowbo, 0, Lowbo, 0, Sum + 1);
      System.arraycopy(Orig_rh, 0, Rh, 0, Rows + 1);

      for(i = 1; i <= Columns; i++)
        if(Lowbo[Rows + i] != 0)
          {
            theta = Lowbo[ Rows + i];
	    if(Upbo[Rows + i]<Infinite)
              Upbo[Rows + i] -= theta;
            for(j = Col_end[i - 1]; j < Col_end[i]; j++)
              Rh[Mat[j].row_nr] -= theta * Mat[j].value;
          }
      invert();
      Lp.eta_valid = TRUE;
      failure = solvelp();
    }

  if(failure != OPTIMAL)
    debug_print("this problem has no solution, it is " +
		((failure == UNBOUNDED) ? "unbounded" : "infeasible"));

  if(failure == INFEASIBLE && Lp.verbose != FALSE)
    System.out.print("level" + Level + " INF\n");

  if(failure == OPTIMAL)	/* there is a solution */
    {
      construct_solution(Solution);

      debug_print("a solution was found\n");
      debug_print_solution();

      /* if this solution is worse than the best sofar, this branch must die */
      if(Maximise != FALSE)
	is_worse = (Solution[0] <= Best_solution[0]) ? 1:0;
      else			/* minimising! */
	is_worse = (Solution[0] >= Best_solution[0]) ? 1:0;

      if(is_worse != FALSE)
	{
	  if(Lp.verbose != FALSE)
	    System.out.println("level" + Level + " OPT NOB value " + Solution[0] + 
			     " bound " + Best_solution[0]); 
	  debug_print("but it was worse than the best sofar, discarded\n");
	  Level--;
	  return(MILP_FAIL);
	}

      /* check if solution contains enough ints */
      if(Lp.bb_rule == FIRST_NI)
        {
          notint = 0;
          i = Rows + 1;
          while(i <= Sum && notint == 0)
            {
	      if(Must_be_int[i] != FALSE && is_int(Solution[i]) == FALSE)
		if(lowbo[i] == upbo[i]) /* this var is already fixed */
		  {
		    System.err.println("Warning: integer var " + (i - Rows) +
				     " is already fixed at " + lowbo[i] + 
				     ", but has non-integer value " + Solution[i]);

		    System.err.println("Perhaps the -e option should be used");
		  }
		else
		  notint = i;
              i++;
            }
        }
      if(Lp.bb_rule == RAND_NI)
        {
          int nr_not_int, select_not_int;
          nr_not_int = 0;
          for(i = Rows + 1; i <= Sum; i++)
            if(Must_be_int[i] != FALSE && is_int(Solution[i]) == FALSE)
              nr_not_int++;
          if(nr_not_int == 0)
            notint = 0;
          else
            {
              select_not_int=(rdm.nextInt() % nr_not_int) + 1;
              i = Rows + 1;
              while(select_not_int > 0)
                {
                  if(Must_be_int[i] != FALSE && is_int(Solution[i]) == FALSE)
                    select_not_int--;
                  i++;
                }
              notint = i - 1;
            }
        }

      if(Lp.verbose == TRUE)
        if(notint != FALSE)
          System.out.println("level " + Level + " OPT     value " +  Solution[0]);
        else
          System.out.println("level " + Level + " OPT INT value " +  Solution[0]);

      if(notint != FALSE)		/* there is at least one value not yet int */
	{
	  /* set up two new problems */
	  double[]   new_upbo, new_lowbo;
	  double   new_bound;
          short[]  new_lower, new_basis;
	  int[]    new_bas;
          int     resone, restwo;

	  /* allocate room for them */
	  new_upbo = new double[Sum + 1];
	  new_lowbo = new double[Sum + 1];
          new_lower = new short[Sum + 1];
          new_basis = new short[Sum + 1];
          new_bas = new int[Rows + 1];
	  System.arraycopy(upbo, 0, new_upbo, 0, Sum + 1);
          System.arraycopy(lowbo, 0, new_lowbo, 0, Sum + 1);
          System.arraycopy(Lower, 0, new_lower, 0, Sum + 1);
          System.arraycopy(Basis, 0, new_basis, 0, Sum + 1);
          System.arraycopy(Bas, 0, new_bas, 0, Rows +1);
   
          if (Lp.names_used != FALSE)
            debug_print("not enough ints. Selecting var " + Lp.col_name[notint - Rows] +
			", val: " + Solution[notint]);
          else
            debug_print("not enough ints. Selecting Var [" + notint +
			"], val: " + Solution[notint]);
	  debug_print("current bounds:\n");
	  debug_print_bounds(upbo, lowbo);

          if(Floor_first != FALSE)
            {
              new_bound = Math.ceil(Solution[notint]) - 1;
              /* this bound might conflict */
              if(new_bound < lowbo[notint])
	        {
                  debug_print("New upper bound value " + new_bound +
			      " conflicts with old lower bound " + lowbo[notint] + "\n");
                  resone = MILP_FAIL;
	        }
	      else		/* bound feasible */
	        {
	          new_upbo[notint] = new_bound;
                  debug_print("starting first subproblem with bounds:");
	          debug_print_bounds(new_upbo, lowbo);
		  Lp.eta_valid = FALSE;
	          resone = milpsolve(new_upbo, lowbo, new_basis, new_lower,
				     new_bas);
                  Lp.eta_valid = FALSE;
	        }
              new_bound += 1;
	      if(new_bound > upbo[notint])
	        {
                  debug_print("New lower bound value " + new_bound +
			      " conflicts with old upper bound " + upbo[notint]
			      + "\n");
	          restwo = MILP_FAIL;
	        }
	      else		/* bound feasible */
		{
		  new_lowbo[notint] = new_bound;
		  debug_print("starting second subproblem with bounds:");
		  debug_print_bounds(upbo, new_lowbo);
		  Lp.eta_valid = FALSE;
		  restwo = milpsolve(upbo, new_lowbo, new_basis, new_lower,
				     new_bas);
		  Lp.eta_valid = FALSE;
		}
	    }
          else			/* take ceiling first */
            {
              new_bound = Math.ceil(Solution[notint]);
              /* this bound might conflict */
	      if(new_bound > upbo[notint])
	        {
		  debug_print("New lower bound value " + new_bound +
			      " conflicts with old upper bound " + upbo[notint] 
			      + "\n");
		  resone = MILP_FAIL;
	        }
	      else		/* bound feasible */
	        {
	          new_lowbo[notint] = new_bound;
                  debug_print("starting first subproblem with bounds:");
	          debug_print_bounds(upbo, new_lowbo);
                  Lp.eta_valid = FALSE;
                  resone = milpsolve(upbo, new_lowbo, new_basis, new_lower,
				     new_bas);
                  Lp.eta_valid = FALSE;
	        }
              new_bound -= 1;
	      if(new_bound < lowbo[notint])
	        {
                  debug_print("New upper bound value " + new_bound +
			      " conflicts with old lower bound " + lowbo[notint]
			      + "\n");
	          restwo = MILP_FAIL;
	        }
	      else		/* bound feasible */
	        {
	          new_upbo[notint] = new_bound;
                  debug_print("starting second subproblem with bounds:");
     	          debug_print_bounds(new_upbo, lowbo);
	          Lp.eta_valid = FALSE;
	          restwo = milpsolve(new_upbo, lowbo, new_basis, new_lower,
				     new_bas);
                  Lp.eta_valid = FALSE;
		}
	    }
          if(resone != FALSE && restwo != FALSE) 
	    /* both failed and must have been infeasible */
	    failure = INFEASIBLE;
	  else
	    failure = OPTIMAL;

	}
      else /* all required values are int */
	{
          debug_print("--> valid solution found\n");

	  if(Maximise != FALSE)
	    is_worse = (Solution[0] < Best_solution[0]) ? 1:0;
	  else
	    is_worse = (Solution[0] > Best_solution[0]) ? 1:0;

	  if(is_worse == FALSE) /* Current solution better */
	    {
              if(Lp.debug != FALSE || (Lp.verbose != FALSE && Lp.print_sol == FALSE))
	        System.out.print("*** new best solution: old: " + Best_solution[0] +
				 ", new: " + Solution[0] + " ***\n");
	      System.arraycopy(Solution, 0, Best_solution, 0, Sum + 1);
              calculate_duals();
              if(Lp.print_sol != FALSE)
                print_solution(Lp); 
              if(Lp.break_at_int != FALSE)
                {
                  if(Maximise != FALSE &&  (Best_solution[0] > Lp.break_value))
                    Break_bb = TRUE;
                  if(Maximise == FALSE &&  (Best_solution[0] < Lp.break_value))
                    Break_bb = TRUE;
                }
	    }
	}
    }

  /* failure can have the values OPTIMAL, UNBOUNDED and INFEASIBLE. */
  Level--;
  return(failure);
} /* milpsolve */

public int dosolve(lprec lp)
{
  int result, i;

  if(lp.active == FALSE)
    set_globals(lp);

  lp.total_iter  = 0;
  lp.max_level   = 1;
  lp.total_nodes = 0;

  if(Isvalid(lp) != FALSE)
    {
      if(Maximise != FALSE && lp.obj_bound == Infinite)
	Best_solution[0]=-Infinite;
      else if(Maximise == FALSE && lp.obj_bound==-Infinite)
	Best_solution[0] = Infinite;
      else
	Best_solution[0] = lp.obj_bound;

      Level = 0;

      if(lp.basis_valid == FALSE)
	{
	  for(i = 0; i <= lp.rows; i++)
	    {
	      lp.basis[i] = TRUE;
	      lp.bas[i] = i;
	    }
	  for(i = lp.rows+1; i <= lp.sum; i++)
	    lp.basis[i] = FALSE;
	  for(i = 0; i <= lp.sum; i++)
	    lp.lower[i] = TRUE;
	  lp.basis_valid = TRUE;
	}

      lp.eta_valid = FALSE;
      Break_bb      = FALSE;
      result        = milpsolve(Orig_upbo, Orig_lowbo, Basis, Lower, Bas); 
      lp.eta_size  = Eta_size;
      lp.eta_alloc = Eta_alloc;
      lp.num_inv   = Num_inv;

      return(result);
    }

  /* if we get here, Isvalid(lp) failed. I suggest we return FAILURE */
  return(FAILURE);
}

public int lag_solve(lprec lp, double start_bound, int num_iter, short verbose)
{
  int i, j, result, citer;
  short status, OrigFeas, AnyFeas, same_basis;
  double[] OrigObj, ModObj, SubGrad, BestFeasSol;
  double Zub, Zlb, Ztmp, pie;
  double rhsmod, Step, SqrsumSubGrad;
  int[]   old_bas;
  short[] old_lower;

  /* allocate mem */  
  OrigObj = new double[lp.columns + 1];
  ModObj = new double[lp.columns + 1];
  for (i = 0; i <= lp.columns; i++)
    ModObj[i] = 0;
  SubGrad = new double[lp.nr_lagrange];
  for (i = 0; i < lp.nr_lagrange; i++)
    SubGrad[i] = 0;
  BestFeasSol = new double[lp.sum + 1];
  for (i = 0; i <= lp.sum; i++)
    BestFeasSol[i] = 0;
  old_bas = new int[lp.rows + 1];
  System.arraycopy(lp.bas, 0, old_bas, 0, lp.rows + 1);
  old_lower = new short[lp.sum + 1];
  System.arraycopy(lp.lower, 0, old_lower, 0, lp.sum + 1);

  get_row(lp, 0, OrigObj);
 
  pie = 2;  

  if(lp.maximise != FALSE)
    {
      Zub = DEF_INFINITE;
      Zlb = start_bound;
    }
  else
    {
      Zlb = -DEF_INFINITE;
      Zub = start_bound;
    }
  status   = RUNNING; 
  Step     = 1;
  OrigFeas = FALSE;
  AnyFeas  = FALSE;
  citer    = 0;

  for(i = 0 ; i < lp.nr_lagrange; i++)
    lp.lambda[i] = 0;

  while(status == RUNNING)
    {
      citer++;

      for(i = 1; i <= lp.columns; i++)
        {
          ModObj[i] = OrigObj[i];
          for(j = 0; j < lp.nr_lagrange; j++)
            {
              if(lp.maximise != FALSE)
                ModObj[i] -= lp.lambda[j] * lp.lag_row[j][i]; 
              else  
                ModObj[i] += lp.lambda[j] * lp.lag_row[j][i];
	    }
        }
      for(i = 1; i <= lp.columns; i++)
        {  
          set_mat(lp, 0, i, ModObj[i]);
          /* fSystem.out.print(stderr, "%f ", ModObj[i]); */
        }
      rhsmod = 0;
      for(i = 0; i < lp.nr_lagrange; i++)
        if(lp.maximise != FALSE)
          rhsmod += lp.lambda[i] * lp.lag_rhs[i];
        else
          rhsmod -= lp.lambda[i] * lp.lag_rhs[i];
 
      if(verbose != FALSE)
        {
          System.out.println("Zub: " + Zub + " Zlb: " + Zlb + " Step: " + Step +
			   " pie: " + pie + " Feas " + OrigFeas);
          for(i = 0; i < lp.nr_lagrange; i++)
            System.out.println(i + " SubGrad " + SubGrad[i] + " lambda " + lp.lambda[i]);
        }

      if(verbose != FALSE && lp.sum < 20)
        print_lp(lp);

      result = dosolve(lp);

      if(verbose != FALSE && lp.sum < 20)
        { 
          print_solution(lp);
        }

      same_basis = TRUE;
      i = 1;
      while(same_basis != FALSE && i < lp.rows)
        {
          same_basis = (old_bas[i] == lp.bas[i])? (short)1: (short)0;
          i++;
        }
      i = 1;
      while(same_basis != FALSE && i < lp.sum)
        {
          same_basis=(old_lower[i] == lp.lower[i])? (short)1:(short)0;
          i++;
        }
      if(same_basis == FALSE)
        {
          System.arraycopy(lp.lower, 0, old_lower, 0, lp.sum+1);
          System.arraycopy(lp.bas, 0, old_bas, 0, lp.rows+1);
          pie *= 0.95;
        }

      if(verbose != FALSE)
        System.out.println("result: " + result + "  same basis: " + same_basis);
      
      if(result == UNBOUNDED)
        {
          for(i = 1; i <= lp.columns; i++)
            System.out.print(ModObj[i] + " ");
          System.exit(FAIL);
        }

      if(result == FAILURE)
        status = FAILURE;

      if(result == INFEASIBLE)
        status = INFEASIBLE;
      
      SqrsumSubGrad = 0;
      for(i = 0; i < lp.nr_lagrange; i++)
        {
          SubGrad[i]= -lp.lag_rhs[i];
          for(j = 1; j <= lp.columns; j++)
            SubGrad[i] += lp.best_solution[lp.rows + j] * lp.lag_row[i][j];
          SqrsumSubGrad += SubGrad[i] * SubGrad[i];
        }

      OrigFeas = TRUE;
      for(i = 0; i < lp.nr_lagrange; i++)
        if(lp.lag_con_type[i] != FALSE)
          {
            if(Math.abs(SubGrad[i]) > lp.epsb)
              OrigFeas = FALSE;
          }
        else if(SubGrad[i] > lp.epsb)
          OrigFeas = FALSE;

      if(OrigFeas != FALSE)
        {
          AnyFeas = TRUE;
          Ztmp = 0;
          for(i = 1; i <= lp.columns; i++)
            Ztmp += lp.best_solution[lp.rows + i] * OrigObj[i];
          if((lp.maximise != FALSE) && (Ztmp > Zlb))
	    {
	      Zlb = Ztmp;
	      for(i = 1; i <= lp.sum; i++)
		BestFeasSol[i] = lp.best_solution[i];
	      BestFeasSol[0] = Zlb;
	      if(verbose != FALSE)
		System.out.println("Best feasible solution: " + Zlb);
	    }
          else if(Ztmp < Zub)
	    {
	      Zub = Ztmp;
	      for(i = 1; i <= lp.sum; i++)
		BestFeasSol[i] = lp.best_solution[i];
	      BestFeasSol[0] = Zub;
	      if(verbose != FALSE)
		System.out.println("Best feasible solution: " + Zub);
	    }
        }      

      if(lp.maximise != FALSE)
        Zub = Math.min(Zub, rhsmod + lp.best_solution[0]);
      else
        Zlb = Math.max(Zlb, rhsmod + lp.best_solution[0]);

      if(Math.abs(Zub-Zlb) < 0.001)
        {  
	  status = OPTIMAL;
        }
      Step = pie * ((1.05*Zub) - Zlb) / SqrsumSubGrad;  
 
      for(i = 0; i < lp.nr_lagrange; i++)
        {
          lp.lambda[i] += Step * SubGrad[i];
          if(lp.lag_con_type[i] == FALSE && lp.lambda[i] < 0)
            lp.lambda[i] = 0;
        }
 
      if(citer == num_iter && status==RUNNING)
        if(AnyFeas != FALSE)
          status = FEAS_FOUND;
        else
          status = NO_FEAS_FOUND;
    }

  for(i = 0; i <= lp.sum; i++)
    lp.best_solution[i] = BestFeasSol[i];
 
  for(i = 1; i <= lp.columns; i++)
    set_mat(lp, 0, i, OrigObj[i]);

  if(lp.maximise != FALSE)
    lp.lag_bound = Zub;
  else
    lp.lag_bound = Zlb;
  return(status);
}

} // end of class solve
