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
 * $Id: Utils.java,v 1.3 2004-01-27 23:13:02 dmaze Exp $
 */

package at.dms.compiler.tools.common;

import java.lang.reflect.Array;
import java.util.Vector;

/**
 * This class defines severals utilities methods used in source code
 */
public abstract class Utils {

  // ----------------------------------------------------------------------
  // UTILITIES
  // ----------------------------------------------------------------------

  /**
   * Check if an assertion is valid
   *
   * @exception	RuntimeException	the entire token reference
   */
  public static final void kopi_assert(boolean b) {
      assert b;
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

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

}
