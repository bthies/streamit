/* $Header: /afs/csail.mit.edu/group/commit/reps/projects/streamit/cvsroot/streams/3rdparty/lpsolve/constant.java,v 1.1 2002-10-05 05:25:54 thies Exp $ */
/* $Log: not supported by cvs2svn $
# Revision 1.2  1996/06/06  19:45:34  hma
# added package lp;
#
# Revision 1.1  1996/05/21  02:01:48  hma
# Initial revision
# */

package lpsolve;

public interface constant {
  final static short FAIL = -1;
  
  final static short NULL = 0;
  final static short FALSE = 0;
  final static short TRUE = 1;

  final static short DEFNUMINV = 50;

/* solve status values */
  final static short OPTIMAL = 0;
  final static short MILP_FAIL = 1;
  final static short INFEASIBLE = 2;
  final static short UNBOUNDED = 3;
  final static short FAILURE = 4;
  final static short RUNNING = 5;

/* lag_solve extra status values */
  final static short FEAS_FOUND = 6;
  final static short NO_FEAS_FOUND = 7;
  final static short BREAK_BB = 8;

  final static short FIRST_NI =	0;
  final static short RAND_NI = 1;

  final static short LE = 0;
  final static short EQ = 1;
  final static short GE = 2;
  final static short OF = 3;

  final static short MAX_WARN_COUNT = 20;

  final static double DEF_INFINITE = 1e24; /* limit for dynamic range */
  final static double DEF_EPSB = 5.01e-7; /* for rounding RHS values to 0 determine	
				      infeasibility basis */
  final static double DEF_EPSEL = 1e-8; /* for rounding other values (vectors) to 0 */
  final static double DEF_EPSD  = 1e-6; /* for rounding reduced costs to zero */
  final static double DEF_EPSILON = 1e-3; /* to determine if a float value is integer */
 
  final static double PREJ = 1e-3;  /* pivot reject (try others first) */

  final static int HASHSIZE = 10007; /* prime number is better, MB */
  final static int ETA_START_SIZE = 10000; /* start size of array Eta. Realloced if needed */
  final static String STD_ROW_NAME_PREFIX = "r_";

} // end of interface constant
