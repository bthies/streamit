/*
 * Copyright (C) 1990-2001 DMS Decision Management Systems Ges.m.b.H.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * $Id: Utils.java,v 1.43 2006-09-25 13:54:51 dimock Exp $
 */

package at.dms.util;

import java.io.*;

import at.dms.compiler.JavaStyleComment;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.LoweringConstants;
import java.lang.reflect.Array;
import java.util.Vector;
import java.util.List;
import java.util.LinkedList;
import java.io.Serializable;
import java.math.BigInteger;

/**
 * This class defines severals utilities methods used in source code
 */
public abstract class Utils implements Serializable, DeepCloneable {

    // ----------------------------------------------------------------------
    // UTILITIES
    // ----------------------------------------------------------------------

    /**
     * Check if an assertion is valid
     *
     * @exception   RuntimeException    the entire token reference
     */
    public static final void kopi_assert(boolean b) {
        assert b;
    }

    /**
     * Check if an assertion is valid with a given error message
     *
     * @exception   RuntimeException    the entire token reference
     */
    public static final void kopi_assert(boolean b, String str) {
        assert b : str;
    }

    /**
     * Signal a failure with given error message
     *
     * @exception   RuntimeException    the entire token reference
     */
    public static final void fail(String str) {
        new RuntimeException("Failure: " + str).printStackTrace();
        System.exit(1);
    }

    
    /**
     * Returns the contents of <pre>fileName</pre> as a string buffer.
     */
    public static StringBuffer readFile(String fileName)
        throws IOException
    {
        StringBuffer result = new StringBuffer();
        BufferedReader in = new BufferedReader(new FileReader(fileName));
        while (true) {
            String line = in.readLine();
            if (line == null) {
                break;
            } else {
                result.append(line + "\n");
            }
        }
        in.close();
        return result;
    }

    /**
     * Writes <pre>str</pre> to <pre>filename</pre>, overwriting it if it's already
     * there.
     */
    public static void writeFile(String filename, String str) throws IOException {
        FileWriter out = new FileWriter(filename);
        out.write(str, 0, str.length());
        out.close();
    }

    /**
       /** replaces in all occurances.
       *modifies: nothing.<br/>
       *effects: constructs a new String from orig, replacing all occurances of oldSubStr with newSubStr.
       * <br/>
       *@return  a copy of orig with all occurances of oldSubStr replaced with newSubStr.
       *
       * if any of arguments are null, returns orig.
       */
    public static synchronized String replaceAll( String orig, String oldSubStr, String newSubStr )
    {
        if (orig==null || oldSubStr==null || newSubStr==null) {
            return orig;
        }
        // create a string buffer to do replacement
        StringBuffer sb = new StringBuffer(orig);
        // keep track of difference in length between orig and new
        int offset = 0;
        // keep track of last index where we saw the substring appearing
        int index = -1;
    
        while (true) {

            // look for occurrence of old string
            index = orig.indexOf(oldSubStr, index+1);
            if (index==-1) {
                // quit when we run out of things to replace
                break;
            }
        
            // otherwise, do replacement
            sb.replace(index - offset, 
                       index - offset + oldSubStr.length(), 
                       newSubStr);

            // increment our offset
            offset += oldSubStr.length() - newSubStr.length();
        }

        // return new string
        return sb.toString();
    }

    public static boolean isMathMethod(JExpression prefix, String ident) 
    {
        if (prefix instanceof JTypeNameExpression &&
            ((JTypeNameExpression)prefix).getQualifiedName().equals("java/lang/Math") &&
       
            (ident.equals("acos") ||
             ident.equals("asin") ||
             ident.equals("atan") ||
             ident.equals("atan2") ||
             ident.equals("ceil") ||
             ident.equals("cos") ||
             ident.equals("sin") ||
             ident.equals("cosh") ||
             ident.equals("sinh") ||
             ident.equals("exp") ||
             ident.equals("fabs") ||
             // RMR { add abs() and max()
             ident.equals("abs") ||
             ident.equals("max") ||
             // } RMR
             ident.equals("min") ||
             ident.equals("modf") ||
             ident.equals("fmod") ||
             ident.equals("frexp") ||
             ident.equals("floor") ||        
             ident.equals("log") ||
             ident.equals("log10") ||
             ident.equals("pow") ||
             ident.equals("round") ||
             ident.equals("rint") ||
             ident.equals("sqrt") ||
             ident.equals("tanh") ||
             ident.equals("tan")))
            return true;
        return false;
    }
    
    /* RMR { the compiler currently uses the floating-point versions
     * of the math functions and hence the math routines are renamed to
     * their floating point counterparts: some math functions reqire a
     * prefix 'f', and others require a postfix 'f' 
     */
    public static boolean mathMethodRequiresFloatPrefix(JExpression prefix, String ident) 
    {
        if (prefix instanceof JTypeNameExpression &&
            ((JTypeNameExpression)prefix).getQualifiedName().equals("java/lang/Math") &&
       
            (ident.equals("abs")))
            return true;
        return false;
    }
    /* } RMR */
    
    /**
     * Determine whether a math function whould be prefixed with 'f' if the emitted code is C++.
     * <br/>
     * The compiler currently uses the floating-point versions
     * of the math functions and hence the math routines are renamed to
     * their floating point counterparts:  some math functions require a prefix 'f'.
     * <br/>
     * Currently, any math functions that do not require a prefix 'f' require a suxxif 'f'.
     */
    public static boolean cppMathMethodRequiresFloatPrefix(JExpression prefix, String ident) 
    {
        if (prefix instanceof JTypeNameExpression &&
            ((JTypeNameExpression)prefix).getQualifiedName().equals("java/lang/Math") &&
       
            (ident.equals("abs") || ident.equals("max") || ident.equals("min")))
            return true;
        return false;
    }
  
    /**
     * Returns whether all elements of an array of JExpressions are
     * JLiterals with the same value.
     */
    public static boolean isUniform(JExpression[] arr) {
        // ok to be empty
        if (arr.length == 0) return true;
        // get first val
        JExpression val = arr[0];
        if (!(val instanceof JLiteral)) {
            return false;
        }
        // check rest of vals are equal
        for (int i=1; i<arr.length; i++) {
            if (!(arr[i] instanceof JLiteral) ||
                !((JLiteral)arr[i]).equals((JLiteral)val)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns <pre>val</pre> as a percentage with maximum of 4 digits
     */
    public static String asPercent(double val) {
        String result = "" + (100*val);
        return result.substring(0, Math.min(5, result.length())) + "%";
    }

    /**
     * Returns a power of 2 that is greater than or equal to <pre>val</pre>.
     */
    public static int nextPow2(int val) {
        if (val==0) { return val; }
        BigInteger bigVal = BigInteger.valueOf(val);
        int shiftAmount = bigVal.subtract (BigInteger.valueOf (1)).bitLength ();
        return BigInteger.ONE.shiftLeft (shiftAmount).intValue ();
    }

    /**
     * Returns a list of Integers containing same elements as <pre>arr</pre>
     */
    public static List<Integer> intArrayToList(int[] arr) {
        LinkedList<Integer> result = new LinkedList<Integer>();
        for (int i=0; i<arr.length; i++) {
            result.add(new Integer(arr[i]));
        }
        return result;
    }

    /**
     * Creates a vector and fills it with the elements of the specified array.
     *
     * @param   array       the array of elements
     */
    public static <T> Vector<T> toVector(T[] array) {
        if (array == null) {
            return new Vector<T>();
        } else {
            Vector<T> vector = new Vector<T>(array.length);

            for (int i = 0; i < array.length; i++) {
                vector.addElement(array[i]);
            }
            return vector;
        }
    }

    /**
     * Creates a typed array from a vector.
     * 
     * Java 1.5: should use Vector.toArray
     *
     * @param   vect        the vector containing the elements
     * @param   type        the type of the elements
     * @deprecated
     */
    public static Object[] toArray(Vector vect, Class type) {
        if (vect != null && vect.size() > 0) {
            Object[]    array = (Object[])Array.newInstance(type, vect.size());

            try {
                vect.copyInto(array);
            } catch (ArrayStoreException e) {
                System.err.println("Array was:" + vect.elementAt(0));
                System.err.println("New type :" + array.getClass());
                throw e;
            }
            return array;
        } else {
            return (Object[])Array.newInstance(type, 0);
        }
    }

    /**
     * Creates a int array from a vector.
     *
     * different from Vector.toArray because casts Integer to int
     *
     * @param   vect        the vector containing the elements
     */
    public static int[] toIntArray(Vector<Integer> vect) {
        if (vect != null && vect.size() > 0) {
            int[]   array = new int[vect.size()];

            for (int i = array.length - 1; i >= 0; i--) {
                array[i] = vect.elementAt(i).intValue();
            }

            return array;
        } else {
            return new int[0]; // $$$ static ?
        }
    }

    /**
     * Returns a new array of length n with all values set to val
     *
     * @param   n       the desired number of elements in the array
     * @param   val     the value of each element
     */
    public static int[] initArray(int n, int val) {
        int[] result = new int[n];
        for (int i=0; i<n; i++) {
            result[i] = val;
        }
        return result;
    }

    /**
     * Returns a new array of length n with all values set to val
     *
     * @param   n       the desired number of elements in the array
     * @param   exp     the value of each element
     */
    public static JExpression[] initArray(int n, JExpression exp) {
        JExpression[] result = new JExpression[n];
        for (int i=0; i<n; i++) {
            result[i] = exp;
        }
        return result;
    }

    /**
     * Returns a new array of length n with all values as JIntLiterals set to val
     *
     * @param   n       the desired number of elements in the array
     * @param   val     the value of each element
     */
    public static JExpression[] initLiteralArray(int n, int val) {
        JExpression[] result = new JExpression[n];
        for (int i=0; i<n; i++) {
            result[i] = new JIntLiteral(val);
        }
        return result;
    }

    /**
     * Returns whether or not two integer arrays have the same length
     * and entries
     */
    public static boolean equalArrays(int[] a1, int[] a2) {
        if (a1.length!=a2.length) {
            return false;
        } else {
            boolean ok = true;
            for (int i=0; i<a1.length; i++) {
                ok = ok && a1[i]==a2[i];
            }
            return ok;
        }
    }

    
    
    /**
     * Given a statement, return the expression that this statement is 
     * composed of, if not an expression statement return null.
     *
     *
     * @param orig The statement
     *
     *
     * @return null if <pre>orig</pre> does not contain an expression or
     * the expression if it does.
     */
    public static JExpression getExpression(JStatement orig)
    {
        if (orig instanceof JExpressionListStatement) {
            JExpressionListStatement els = (JExpressionListStatement)orig;
            if (els.getExpressions().length == 1)
                return passThruParens(els.getExpression(0));
            else
                return null;
        }
        else if (orig instanceof JExpressionStatement) {
            return passThruParens(((JExpressionStatement)orig).getExpression());
        }
        else 
            return null;
    }

    /**
     * Return the first non-parentheses expressions contained in <pre>orig</pre> 
     **/
    public static JExpression passThruParens(JExpression orig) 
    {
        if (orig instanceof JParenthesedExpression) {
            return passThruParens(((JParenthesedExpression)orig).getExpr());
        }
        return orig;
    }
    
    /**
     * Splits a string like:
     *   "java/lang/System/out"
     * into two strings:
     *    "java/lang/System" and "out"
     */
    public static String[] splitQualifiedName(String name, char separator) {
        String[]    result = new String[2];
        int     pos;

        pos = name.lastIndexOf(separator);

        if (pos == -1) {
            // no '/' in string
            result[0] = "";
            result[1] = name;
        } else {
            result[0] = name.substring(0, pos);
            result[1] = name.substring(pos + 1);
        }

        return result;
    }
  

    /**
     * Splits a string like:
     *   "java/lang/System/out"
     * into two strings:
     *    "java/lang/System" and "out"
     */
    public static String[] splitQualifiedName(String name) {
        return splitQualifiedName(name, '/');
    }


    /**
     * If the first and last SIRMarker's in <pre>stmt</pre> mark the beginning
     * and end of the same segment, then move those markers to the
     * outermost edges of <pre>stmt</pre>.  The purpose of this routine is to
     * lift markers of filter boundaries out of loops.
     */
    public static JStatement peelMarkers(JStatement stmt) {
        final SIRBeginMarker[] first = { null };
        final SIREndMarker[] last = { null };
        // find first and last marker
        stmt.accept(new SLIREmptyVisitor() {
                public void visitMarker(SIRMarker self) {
                    // record first and last
                    if (self instanceof SIRBeginMarker && first[0] == null) {
                        first[0] = (SIRBeginMarker)self;
                    }
                    if (self instanceof SIREndMarker) {
                        last[0] = (SIREndMarker)self;
                    }
                }
            });

        // if we didn't find two markers, or if first and last marker
        // have different names, then there is nothing to peel, so
        // return
        if (first[0] == null || last[0] == null) return stmt;
        if (!first[0].getName().equals(last[0].getName())) return stmt;

        // otherwise, we are going to move the markers to the outside
        // of the statement.  replace the markers with empty
        // statements in the IR
        stmt.accept(new SLIRReplacingVisitor() {
                public Object visitMarker(SIRMarker self) {
                    if (self==first[0] || self==last[0]) {
                        return new JEmptyStatement();
                    } else {
                        return self;
                    }
                }
            });

        // finally, create a new block that begins with the first
        // marker, then has the statement, then has the last marker
        JBlock result = new JBlock();
        result.addStatement(first[0]);
        result.addStatement(stmt);
        result.addStatement(last[0]);

        return result;
    }

    /**
     * Returns a version of <pre>stmt</pre> with all standalone pops (i.e.,
     * pop() as a statement rather than as an expression) removed.
     */
    public static JStatement removeUnusedPops(JStatement stmt) {
        return (JStatement)stmt.accept(new SLIRReplacingVisitor() {
                public Object visitExpressionStatement(JExpressionStatement self, JExpression expr) {
                    if (expr instanceof SIRPopExpression) {
                        return new JEmptyStatement();
                    } else {
                        return super.visitExpressionStatement(self, expr);
                    }
                }
            });
    }

    /**
     * Returns whether or not there are any pop expressions before
     * peek expressions in the dynamic execution of <pre>stmt</pre>.
     */
    public static boolean popBeforePeek(JStatement stmt) {
        // there are two ways that a dynamic pop can come before a
        // peek:
        // 1. the pop statement comes first in the static code listing
        // 2. a pop and peek statement are in the same loop

        // CHECK CONDITION 1
        final boolean popBeforePeek1[] = { false };
        {
        final boolean seenPop1[] = { false };
        stmt.accept(new SLIREmptyVisitor() {
                public void visitPopExpression(SIRPopExpression self,
                                               CType tapeType) {
                    seenPop1[0] = true;
                }
                public void visitPeekExpression(SIRPeekExpression self,
                                                CType tapeType,
                                                JExpression arg) {
                    super.visitPeekExpression(self, tapeType, arg);
                    if (seenPop1[0]) {
                        popBeforePeek1[0] = true;
                    }
                }
            });
        }

        // CHECK CONDITION 2
        final boolean popBeforePeek2[] = { false };
        {
        // count of loop nesting
        final int numLoops[] = { 0 };
        // whether or not we have seen pop or peek in outermost loop
        final boolean seenPop2[] = { false };
        final boolean seenPeek2[] = { false };
        stmt.accept(new SLIREmptyVisitor() {
                public void visitForStatement(JForStatement self,
                                              JStatement init,
                                              JExpression cond,
                                              JStatement incr,
                                              JStatement body) {
                    clearStats();
                    numLoops[0]++;
                    super.visitForStatement(self, init, cond, incr, body);
                    numLoops[0]--;
                    updateStats();
                }

                public void visitWhileStatement(JWhileStatement self,
                                                JExpression cond,
                                                JStatement body) {
                    clearStats();
                    numLoops[0]++;
                    super.visitWhileStatement(self, cond, body);
                    numLoops[0]--;
                    updateStats();
                }

                // if we just left outer-most loop, check if there is
                // both pop and peek in the loop
                private void updateStats() {
                    if (numLoops[0] == 0) {
                        if (seenPop2[0] && seenPeek2[0]) {
                            popBeforePeek2[0] = true;
                        }
                        clearStats();
                    }
                }
                
                // clear counts for next loop
                private void clearStats() {
                    if (numLoops[0] == 0) {
                        seenPop2[0] = false;
                        seenPeek2[0] = false;
                    }
                }


                public void visitPopExpression(SIRPopExpression self,
                                               CType tapeType) {
                    seenPop2[0] = true;
                }
                public void visitPeekExpression(SIRPeekExpression self,
                                                CType tapeType,
                                                JExpression arg) {
                    super.visitPeekExpression(self, tapeType, arg);
                    seenPeek2[0] = true;
                }
            });
        }

        // return true if either condition found pop before peek
        return popBeforePeek1[0] || popBeforePeek2[0];
    }

    /**
     * Set to true to get a stack trace of callers inserted as a comment.
     * 
     * Limitation: only provides info for loops taht are created as loops: 
     * i.e. those with trip count > 1. 
     */

    public static boolean getForLoopCallers = false;
    
    /**
     * Returns a block with a loop counter declaration and a for loop
     * that executes <pre>body</pre> for <pre>count</pre> number of times.  If the
     * count is just one, then return the body instead of a loop.
     */
    public static JStatement makeForLoop(JStatement body, int count) {
        return makeForLoop(body, new JIntLiteral(count));
    }

    /**
     * Returns a block with a loop counter declaration and a for loop
     * that executes <pre>body</pre> for <pre>count</pre> number of times.
     */
    public static JStatement makeForLoop(JStatement body, JExpression count) {
        if (count instanceof JIntLiteral) {
            int intCount = ((JIntLiteral)count).intValue();
            if (intCount<=0) {
                // if the count isn't positive, return an empty statement
                return new JEmptyStatement(null, null); 
            } else if (intCount==1) {
                // if the count is one, then just return the body
                return body;
            }
        }
        return makeForLoop(body, count,         
                           new JVariableDefinition(/* where */ null,
                                                   /* modifiers */ 0,
                                                   /* type */ CStdType.Integer,
                                                   /* ident */ 
                                                   LoweringConstants.getUniqueVarName(),
                                                   /* initializer */
                                                   new JIntLiteral(0)));
    }

    /**
     * Returns a block with a loop counter declaration and a for loop
     * that executes <pre>body</pre> for <pre>count</pre> number of times.  Executes in
     * the forward direction, counting up from 0 to count-1 with
     * <pre>loopIndex</pre> as the loop counter.
     *
     * Note that <pre>loopIndex</pre> should not appear in a different variable
     * decl; it will get one in this routine.
     */
    public static JStatement makeForLoop(JStatement body, JExpression count, final JVariableDefinition loopIndex) {
        // make sure we start counting from 0
        loopIndex.setInitializer(new JIntLiteral(0));
        // make a declaration statement for our new variable
        JVariableDeclarationStatement varDecl =
            new JVariableDeclarationStatement(null, loopIndex, null);

        // avoid loops for certain loop bounds
        if (count instanceof JIntLiteral) {
            int intCount = ((JIntLiteral)count).intValue();
            if (intCount<=0) {
                // if the count isn't positive, return the variable
                // decl.  Return this rather than an empty statement
                // in case some later code depends on the value of
                // this variable on loop exit.  However, rstream seems
                // not to need the VarDecl, so return only an empty
                // statement there.
                return (KjcOptions.rstream ? (JStatement)(new JEmptyStatement()) : (JStatement)varDecl);
            } else if (intCount==1) {
                // replace references to the loop counter with the
                // constant 0.  (while constant prop does this
                // automatically in other backends, causes problems in
                // rstream, who doesn't want a var decl included.)
                body.accept(new SLIRReplacingVisitor() {
                        public Object visitLocalVariableExpression(JLocalVariableExpression self,
                                                                   String ident) {
                            if (self.getVariable()==loopIndex) { return new JIntLiteral(0); }
                            return self;
                        }});
                return body;
            }
        }

        // from here on out, we will explicitly assign the loop index
        // to be zero in the initializer of the for loop, so do not do
        // it in the var decl (might result in double assignment)
        loopIndex.setInitializer(null);
        
        // make a test if our variable is less than <pre>count</pre>
        JExpression cond = 
            new JRelationalExpression(null,
                                      Constants.OPE_LT,
                                      new JLocalVariableExpression(null, loopIndex),
                                      count);
        // make an increment for <pre>var</pre>
        JStatement incr = 
            new JExpressionStatement(null,
                                     new JPostfixExpression(null,
                                                            Constants.
                                                            OPE_POSTINC,
                                                            new JLocalVariableExpression(null, loopIndex)),
                                     null);

        JavaStyleComment comments[] = null;
        
        // debugging: print caller, and insert caller as comment on returned for loop
        if (getForLoopCallers) {
            String caller;
            {
                Throwable tracer = new Throwable();
                tracer.fillInStackTrace();
                caller = "ClusterExecution.makeForLoop(" + count + "): "
                        + tracer.getStackTrace()[1].toString();
            }
            JavaStyleComment comment = new JavaStyleComment(caller, true, false, false);
            comments = new JavaStyleComment[] {comment};

            // should have separate flag for this...
            System.err.println(caller);

        }

        // make the for statement
        JStatement forStatement = 
            new JForStatement(/* tokref */ null,
                              //for rstream put the vardecl in the init of the for loop
                              /* init */ (KjcOptions.rstream ? (JStatement) varDecl : 
                                          // otherwise, put an assignment in the init section so that the work estimate can detect how many times this for loop fires
                                          new JExpressionStatement(new JAssignmentExpression(new JLocalVariableExpression(loopIndex), new JIntLiteral(0)))),
                              cond,
                              incr,
                              body,
                              comments);
        // return the block
        JStatement[] statements = {varDecl, forStatement};
        //return just the for statement for rstream
        return (KjcOptions.rstream ? forStatement : new JBlock(null, statements, null));
    }

    /**
     * Returns a block with a loop counter declaration and a for loop
     * that executes <pre>body</pre> for <pre>count</pre> number of times.  Executes in
     * the backwards direction, counting down from count-1 to zero
     * with <pre>loopIndex</pre> as the loop counter.  
     *
     * Note that <pre>loopIndex</pre> should not appear in a different variable
     * decl; it will get one in this routine.
     */
    public static JStatement makeCountdownForLoop(JStatement body, JExpression count, JVariableDefinition loopIndex) {
        // make sure we start at count-1
        loopIndex.setInitializer(new JMinusExpression(null, count, new JIntLiteral(1)));
        // make a declaration statement for our new variable
        JVariableDeclarationStatement varDecl =
            new JVariableDeclarationStatement(null, loopIndex, null);

        // avoid loops for certain loop bounds
        if (count instanceof JIntLiteral) {
            int intCount = ((JIntLiteral)count).intValue();
            if (intCount<=0) {
                // if the count isn't positive, return the variable
                // decl.  Return this rather than an empty statement
                // in case some later code depends on the value of
                // this variable on loop exit.  However, rstream seems
                // not to need the VarDecl, so return only an empty
                // statement there.
                return (KjcOptions.rstream ? (JStatement)new JEmptyStatement() : (JStatement)varDecl);
            } else if (intCount==1) {
                // if the count is one, then return the decl and the
                // body (but rstream doesn't need the decl).
                return (KjcOptions.rstream ? body :
                        new JBlock(null, new JStatement[] { varDecl, body }, null));
            }
        }

        // from here on out, we will explicitly assign the loop index
        // to be count-1 in the initializer of the for loop, so do not
        // do it in the var decl (might result in double assignment)
        loopIndex.setInitializer(null);
        
        // make a test if our variable is less than <pre>count</pre>
        JExpression cond = 
            new JRelationalExpression(null,
                                      Constants.OPE_GE,
                                      new JLocalVariableExpression(null, loopIndex),
                                      new JIntLiteral(0));
        // make a decrement for <pre>var</pre>
        JStatement incr = 
            new JExpressionStatement(null,
                                     new JPostfixExpression(null,
                                                            Constants.
                                                            OPE_POSTDEC,
                                                            new JLocalVariableExpression(null, loopIndex)),
                                     null);

        JavaStyleComment comments[] = null;
        
        // debugging: print caller, and insert caller as comment on returned for loop
        if (getForLoopCallers) {
            String caller;
            {
                Throwable tracer = new Throwable();
                tracer.fillInStackTrace();
                caller = "ClusterExecution.makeForLoop(" + count + "): "
                        + tracer.getStackTrace()[1].toString();
            }
            JavaStyleComment comment = new JavaStyleComment(caller, true, false, false);
            comments = new JavaStyleComment[] {comment};

            // should have separate flag for this...
            System.err.println(caller);

        }
        

        // make the for statement
        JStatement forStatement = 
            new JForStatement(/* tokref */ null,
                              /* init */ new JExpressionStatement(new JAssignmentExpression(new JLocalVariableExpression(loopIndex), new JMinusExpression(null, count, new JIntLiteral(1)))),
                              cond,
                              incr,
                              body,
                              comments);
        // return the block
        JStatement[] statements = {varDecl, forStatement};
        return new JBlock(null, statements, null);
    }

    /**
     * If <pre>type</pre> is void, then return <pre>int</pre> type; otherwise return
     * <pre>type</pre>.  This is a hack to get around the disallowance of void
     * arrays in C--should fix this better post-asplos.
     */
    public static CType voidToInt(CType type) {
        return type==CStdType.Void ? CStdType.Integer : type;
    }

    /**
     * Returns value of environment variable named <pre>var</pre>, or null if
     * the variable is undefined.
     */
    public static String getEnvironmentVariable(String var) {
        try {
            String OS = System.getProperty("os.name").toLowerCase();
            String command = (OS.indexOf("windows") > -1 ? "set" : "env");
            Process p = Runtime.getRuntime().exec(command);
            BufferedReader br = new BufferedReader ( new InputStreamReader( p.getInputStream() ) );
            String line;
            while((line = br.readLine()) != null) {
                int pos = line.indexOf('=');
                String key = line.substring(0, pos);
                if (key.equals(var)) {
                    return line.substring(pos+1);
                }
            }
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            Utils.fail("I/O exception trying to retrieve environment variable \"" + var + "\"");
            return null;
        }
    }

    /**
     * Record the number of subgraphs for future dot filenames.
     */
    public static void setupDotFileName(int _numSubGraphs) 
    {
        numSubGraphs = _numSubGraphs;
    }
    private static int numSubGraphs = 1;
    /**
     * Make a name for a dot file from a supplied prefix and the name
     * of the top-level SIRStream.
     */
    public static String makeDotFileName(String prefix, SIRStream strName) 
    {
        if (numSubGraphs == 1) {
            // if only one graph, don't print "Toplevel0" -- be
            // consistent with other backends compiling static rates
            return prefix + ".dot";
        } else {
            return prefix + (strName != null ? strName.getIdent() : "") + ".dot";
        }
    }
    
    // ----------------------------------------------------------------------
    // DATA MEMBERS
    // ----------------------------------------------------------------------

    public static final LinkedList<SIRStream> EMPTY_LIST = new LinkedList<SIRStream>();


    /** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

    /** Returns a deep clone of this object. */
    public Object deepClone() { at.dms.util.Utils.fail("Error in auto-generated cloning methods - deepClone was called on an abstract class."); return null; }

    /** Clones all fields of this into <pre>other</pre> */
    protected void deepCloneInto(at.dms.util.Utils other) {
    }

    /** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}
