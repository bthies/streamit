/* $Header: /afs/csail.mit.edu/group/commit/reps/projects/streamit/cvsroot/streams/3rdparty/lpsolve/matrec.java,v 1.1 2002-10-05 05:25:54 thies Exp $ */
/* $Log: not supported by cvs2svn $
# Revision 1.2  1996/06/06  19:47:20  hma
# added package statement
#
# Revision 1.1  1996/05/21  02:04:15  hma
# Initial revision
# */

package lpsolve;

public class matrec
{
  int row_nr;
  double value;
  public matrec(int r, double v) {
    row_nr = r;
    value = v;
  }
}
