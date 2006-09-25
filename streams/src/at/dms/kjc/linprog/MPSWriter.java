package at.dms.kjc.linprog;

import java.io.*;
import java.util.*;

/**
 * Writes an MPS representation of a linear program.  Intended to be
 * extended by LinearProgramSolver's that call printMPSToFile before
 * processing the MPS file with a 3rd-party package.
 */
public class MPSWriter extends SimpleLinearProgram {

    /**
     * Create one of these with <numVars> variables.
     */
    public MPSWriter(int numVars) {
        super(numVars);
    }

    /**
     * Dumps a representation of this to <filename> in MPS format.
     */
    protected void printMPSToFile(String filename) {
        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(filename)));
            printMPS(out);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    } 

    /**
     * Prints a representation of this to <out> in MPS format.
     */
    private void printMPS(PrintWriter out) {
        // add objective function as first element of list
        LinkedList<Constraint> copy = new LinkedList<Constraint>(constraints);
        copy.add(0, new Constraint(ConstraintType.OBJ, obj, 0));

        Constraint[] con = copy.toArray(new Constraint[0]);
    
        // print title
        out.println("NAME          STREAMIT");
        // print constraints
        out.println("ROWS");
        for (int i=0; i<con.length; i++) {
            out.print(" ");
            if (con[i].type==ConstraintType.OBJ) {
                out.print("N");
            } else if (con[i].type==ConstraintType.GE) {
                out.print("G");
            } else if (con[i].type==ConstraintType.EQ) {
                out.print("E");
            }
            out.println("  C" + i);
        }
        // print variables
        out.println("COLUMNS");
        for (int i=0; i<numVars; i++) {
            if (boolVar[i]) {
                out.println("    IORG      'MARKER'                 'INTORG'");
            }
            for (int j=0; j<con.length; j++) {
                if (con[j].lhs[i] != 0) {
                    String varName = "X" + i;
                    out.print("    " + varName);
                    for (int k=varName.length(); k<10; k++) {
                        out.print(" ");
                    }
                    String conName = "C" + j;
                    out.print(conName);
                    for (int k=conName.length(); k<10; k++) {
                        out.print(" ");
                    }
                    out.println(con[j].lhs[i]);
                }
            }
            if (boolVar[i]) {
                out.println("    IEND      'MARKER'                 'INTEND'");
            }
        }
        // print rhs
        out.println("RHS");
        // start at 1 because con[0] is objective function with no rhs
        for (int i=1; i<con.length; i++) {
            if (con[i].rhs!=0) {
                out.print("    RHSVAR    ");
                String name = "C" + i;
                out.print(name);
                for (int k=name.length(); k<10; k++) {
                    out.print(" ");
                }    
                out.println(con[i].rhs);
            }
        }
        // print bounds for boolean vars
        out.println("BOUNDS");
        for (int i=0; i<numVars; i++) {
            if (boolVar[i]) {
                out.print(" BV ");
                String boundName = "BNDSET";
                out.print(boundName);
                for (int k=boundName.length(); k<10; k++) {
                    out.print(" ");
                }
                out.println("X" + i);
            }
        }
        out.println("ENDATA");
    }
}

/*  MPS FORMAT

Taken from ftp://plato.la.asu.edu/pub/mps_format.txt

++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
Instead of one complete explanation of the MPS-fromat here are two
different ones. Also look at sample files. Not all MPS-readers can
handle all features allowed in more recent extension of the format.
++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

The main things to know about MPS format are that it is column oriented
(as opposed to entering the model as equations), and everything
(variables, rows, etc.) gets a name. 

MPS is an old format, so it is set up as though you were using punch
cards, and is not free format. Fields start in column 1, 5, 15, 25, 40
and 50.  Sections of an MPS file are marked by so-called header cards,
which are distinguished by their starting in column 1.  Although it is
typical to use upper-case throughout the file (like I said, MPS has
long historical roots), many MPS-readers will accept mixed-case for
anything except the header cards, and some allow mixed-case anywhere.
The names that you choose for the individual entities (constraints or
variables) are not important to the solver; you should pick names that
are meaningful to you, or will be easy for a post-processing code to
read.

Here is a little sample model written in MPS format (explained in more
detail below):

NAME          TESTPROB
ROWS
N  COST
L  LIM1
G  LIM2
E  MYEQN
COLUMNS
XONE      COST                 1   LIM1                 1
XONE      LIM2                 1
YTWO      COST                 4   LIM1                 1
YTWO      MYEQN               -1
ZTHREE    COST                 9   LIM2                 1
ZTHREE    MYEQN                1
RHS
RHS1      LIM1                 5   LIM2                10
RHS1      MYEQN                7
BOUNDS
UP BND1      XONE                 4
LO BND1      YTWO                -1
UP BND1      YTWO                 1
ENDATA

For comparison, here is the same model written out in an equation-
oriented format:

Optimize
COST:    XONE + 4 YTWO + 9 ZTHREE
Subject To
LIM1:    XONE + YTWO <= 5
LIM2:    XONE + ZTHREE >= 10
MYEQN:   - YTWO + ZTHREE  = 7
Bounds
0 <= XONE <= 4
-1 <= YTWO <= 1
End

Strangely, there is nothing in MPS format that specifies the direction
of optimization.  And there really is no standard "default" direction;
some LP codes will maximize if you don't specify otherwise, others will
minimize, and still others put safety first and have no default and
require you to specify it somewhere in a control program or by a
calling parameter.  If you have a model formulated for minimization
and the code you are using insists on maximization (or vice versa), it
may be easy to convert: just multiply all the coefficients in your
objective function by (-1).  The optimal value of the objective
function will then be the negative of the true value, but the values of
the variables themselves will be correct.

The NAME card can have anything you want, starting in column 15.  The
ROWS section defines the names of all the constraints; entries in
column 2 or 3 are E for equality rows, L for less-than ( <= ) rows, G
for greater-than ( >= ) rows, and N for non-constraining rows (the
first of which would be interpreted as the objective function).  The
order of the rows named in this section is unimportant.

The largest part of the file is in the COLUMNS section, which is the
place where the entries of the A-matrix are put.  All entries for a
given column must be placed consecutively, although within a column the
order of the entries (rows) is irrelevant. Rows not mentioned for a
column are implied to have a coefficient of zero.

The RHS section allows one or more right-hand-side vectors to be
defined; most people don't bother having more than one.  In the above
example, the name of the RHS vector is RHS1, and has non-zero values
in all 3 of the constraint rows of the problem.  Rows not mentioned in
an RHS vector would be assumed to have a right-hand-side of zero.

The optional BOUNDS section lets you put lower and upper bounds on
individual variables (no * wild cards, unfortunately), instead of
having to define extra rows in the matrix.  All the bounds that have
a given name in column 5 are taken together as a set.  Variables not
mentioned in a given BOUNDS set are taken to be non-negative (lower
bound zero, no upper bound).  A bound of type UP means an upper bound
is applied to the variable.  A bound of type LO means a lower bound is
applied.  A bound type of FX ("fixed") means that the variable has
upper and lower bounds equal to a single value.  A bound type of FR
("free") means the variable has neither lower nor upper bounds.

There is another optional section called RANGES that I won't go into
here. The final card must be ENDATA, and yes, it is spelled funny.

==========================================================================

MPS input format was originally introduced by IBM to express linear 
and integer programs in a standard way.  The format is a fixed column 
format, so care must be taken that all information is placed in the 
correct columns as described below. 

The following is not intended as a complete description of MPS format, 
but only as a brief introduction.  For more information, the reader is 
directed to:

"Advanced Linear Programming," by Bruce A. Murtagh
"Computer Solutions of Linear Programs," by J.L. Nazareth


It may be useful to look at an example MPS file while reading this
MPS information.


The following template is a guide for the use of MPS format:

---------------------------------------------------------------------
Field:    1           2          3         4         5         6
Columns:  2-3        5-12      15-22     25-36     40-47     50-61

NAME   problem name

ROWS

type     name

COLUMNS
column       row       value     row      value
name        name                name
RHS
rhs         row       value     row      value
name        name                name
RANGES
range       row       value     row      value
name        name                name
BOUNDS

type     bound       column     value
name        name
ENDATA
---------------------------------------------------------------------

NOTES:

A. In the ROWS section, each row of the constraint matrix must have a
row type and a row name specified.  The code for indicating row type
is as follows:

type      meaning
---------------------------
E    equality
L    less than or equal
G    greater than or equal
N    objective
N    no restriction

B. In the COLUMNS section, the names of the variables are defined along
with the coefficients of the objective and all the nonzero constraint
matrix elements.  It is not necessary to specify columns for slack or
surplus variables as this is taken care of automatically.

C. The RHS section contains information for the right-hand side of the problem.

D. The RANGES section is for constraints of the form:  h <= constraint <= u .
The range of the constraint is  r = u - h .  The value of r is specified
in the RANGES section, and the value of u or h is specified in the RHS
section.  If b is the value entered in the RHS section, and r is the
value entered in the RANGES section, then u and h are thus defined:

row type       sign of r       h          u
----------------------------------------------
G            + or -         b        b + |r|
L            + or -       b - |r|      b
E              +            b        b + |r|
E              -          b - |r|      b


E. In the BOUNDS section, bounds on the variables are specified.  When
bounds are not indicated, the default bounds ( 0 <= x < infinity )
are assumed.  The code for indicating bound type is as follows:

type            meaning
-----------------------------------
LO    lower bound        b <= x
UP    upper bound        x <= b
FX    fixed variable     x = b
FR    free variable
MI    lower bound -inf   -inf < x
BV    binary variable    x = 0 or 1

F. Sections RANGES and BOUNDS are optional as are the fields 5 and 6.
Everything else is required.  In regards to fields 5 and 6, consider
the following 2 constraints:

const1:  2x + 3y < 6
const2:  5x + 8y < 20

Two ways to enter the variable x in the COLUMNS section are:

(Field:  2    3           4            5         6  )
1.         x  const1       2.0         const2     5.0

2.         x  const1       2.0
x  const2       5.0

G. A mixed integer program requires the specification of which variables
are required to be integer.  Markers are used to indicate the start
and end of a group of integer variables.  The start marker has its
name in field 2, 'MARKER' in field 3, and 'INTORG' in field 5.  The
end marker has its name in field 2, 'MARKER' in field 3, and 'INTEND'
in field 5.  These markers are placed in the COLUMNS section.


============================================================================

*/
