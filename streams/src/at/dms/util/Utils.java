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
 * $Id: Utils.java,v 1.9 2002-07-29 11:01:48 thies Exp $
 */

package at.dms.util;

import at.dms.kjc.*;
import at.dms.kjc.sir.lowering.LoweringConstants;
import java.lang.reflect.Array;
import java.util.Vector;
import java.util.List;
import java.util.LinkedList;
import java.io.Serializable;

/**
 * This class defines severals utilities methods used in source code
 */
public abstract class Utils implements Serializable {

  // ----------------------------------------------------------------------
  // UTILITIES
  // ----------------------------------------------------------------------

  /**
   * Check if an assertion is valid
   *
   * @exception	RuntimeException	the entire token reference
   */
  public static final void assert(boolean b) {
    if (!b) {
      throw new InconsistencyException();
    }
  }

  /**
   * Check if an assertion is valid with a given error message
   *
   * @exception	RuntimeException	the entire token reference
   */
  public static final void assert(boolean b, String str) {
    if (!b) {
      throw new InconsistencyException(str);
    }
  }

  /**
   * Signal a failure with given error message
   *
   * @exception	RuntimeException	the entire token reference
   */
  public static final void fail(String str) {
      new InconsistencyException(str).printStackTrace();
      System.exit(1);
  }

    
    /**
     * Returns a list of Integers containing same elements as <arr>
     */
    public static List intArrayToList(int[] arr) {
	LinkedList result = new LinkedList();
	for (int i=0; i<arr.length; i++) {
	    result.add(new Integer(arr[i]));
	}
	return result;
    }

  /**
   * Creates a vector and fills it with the elements of the specified array.
   *
   * @param	array		the array of elements
   */
  public static Vector toVector(Object[] array) {
    if (array == null) {
      return new Vector();
    } else {
      Vector	vector = new Vector(array.length);

      for (int i = 0; i < array.length; i++) {
	vector.addElement(array[i]);
      }
      return vector;
    }
  }

  /**
   * Creates a typed array from a vector.
   *
   * @param	vect		the vector containing the elements
   * @param	type		the type of the elements
   */
  public static Object[] toArray(Vector vect, Class type) {
    if (vect != null && vect.size() > 0) {
      Object[]	array = (Object[])Array.newInstance(type, vect.size());

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
   * @param	vect		the vector containing the elements
   * @param	type		the type of the elements
   */
  public static int[] toIntArray(Vector vect) {
    if (vect != null && vect.size() > 0) {
      int[]	array = new int[vect.size()];

      for (int i = array.length - 1; i >= 0; i--) {
	array[i] = ((Integer)vect.elementAt(i)).intValue();
      }

      return array;
    } else {
      return new int[0]; // $$$ static ?
    }
  }

  /**
   * Returns a new array of length n with all values set to val
   *
   * @param	n		the desired number of elements in the array
   * @param	val		the value of each element
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
   * @param	n		the desired number of elements in the array
   * @param	val		the value of each element
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
   * @param	n		the desired number of elements in the array
   * @param	val		the value of each element
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
   * Splits a string like:
   *   "java/lang/System/out"
   * into two strings:
   *    "java/lang/System" and "out"
   */
  public static String[] splitQualifiedName(String name, char separator) {
    String[]	result = new String[2];
    int		pos;

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
     * Returns a block with a loop counter declaration and a for loop
     * that executes <contents> for <count> number of times.  If the
     * count is just one, then return the body instead of a loop.
     */
    public static JStatement makeForLoop(JStatement body, int count) {
	if (count<=0) {
	    // if the count isn't positive, return an empty statement
	    return new JEmptyStatement(null, null); 
	} else if (count==1) {
	    // if the count is one, then just return the body
	    return body;
	}
	// define a variable to be our loop counter
	JVariableDefinition var = 
	    new JVariableDefinition(/* where */ null,
				    /* modifiers */ 0,
				    /* type */ CStdType.Integer,
				    /* ident */ 
				    LoweringConstants.getUniqueVarName(),
				    /* initializer */
				    new JIntLiteral(0));
	// make a declaration statement for our new variable
	JVariableDeclarationStatement varDecl =
	    new JVariableDeclarationStatement(null, var, null);
	// make a test if our variable is less than <count>
	JExpression cond = 
	    new JRelationalExpression(null,
				      Constants.OPE_LT,
				      new JLocalVariableExpression(null, var),
				      new JIntLiteral(count));
	// make an increment for <var>
	JStatement incr = 
	    new JExpressionStatement(null,
				     new JPostfixExpression(null,
							    Constants.
							    OPE_POSTINC,
			       new JLocalVariableExpression(null, var)),
				     null);
	// make the for statement
	JStatement forStatement = 
	    new JForStatement(/* tokref */ null,
			      /* init */ new JEmptyStatement(null, null),
			      cond,
			      incr,
			      body,
			      /* comments */ null);
	// return the block
	JStatement[] statements = {varDecl, forStatement};
	return new JBlock(null, statements, null);
    }

    /**
     * If <type> is void, then return <int> type; otherwise return
     * <type>.  This is a hack to get around the disallowance of void
     * arrays in C--should fix this better post-asplos.
     */
    public static CType voidToInt(CType type) {
	return type==CStdType.Void ? CStdType.Integer : type;
    }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

    public static final List EMPTY_LIST = new LinkedList();

}
