/*
 * Copyright (C) 1990-2001 DMS Decision Management Systems Ges.m.b.H.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * $Id: CModifier.java,v 1.2 2003-05-28 05:58:42 thies Exp $
 */

package at.dms.kjc;

import at.dms.util.InconsistencyException;

/**
 * This class represents all modifiers token
 * <pre> public protected private static final synchronized transient volatile native </pre>
 */
public class CModifier implements Constants, DeepCloneable {

  /**
   * generate a list of modifiers
   * @param	modifiers		the modifiers
   */
  public static String toString(int modifiers) {
    StringBuffer	buffer = new StringBuffer();

    for (int i = 0; i < CODES.length; i++) {
      if (NAMES[i] != null && (modifiers & CODES[i]) != 0) {
	buffer.append(NAMES[i]);
	buffer.append(" ");
      }
    }
    return buffer.toString();
  }

  // ----------------------------------------------------------------------
  // STATIC UTILITIES
  // ----------------------------------------------------------------------

  /**
   * Tests if a set of modifiers contains a specific flag.
   *
   * @param	modifiers	the set of modifiers
   * @param	flag		the flag to test
   * @return	true iff the set of modifiers contains the flag
   */
  public static boolean contains(int modifiers, int flag) {
    return (modifiers & flag) != 0;
  }

  /**
   * Tests if a set of modifiers contains all specified flags.
   *
   * @param	modifiers	the flags to test
   * @param	mask		the set of modifiers
   * @return	true iff the set of modifiers contains all specified flags
   */
  public static boolean isSubsetOf(int modifiers, int mask) {
    return (modifiers & mask) == modifiers;
  }

  /**
   * Returns the subset of flags contained in a set of modifiers.
   *
   * @param	modifiers	the flags to test
   * @param	mask		the set of modifiers
   * @return	true iff the set of modifiers contains all specified flags
   */
  public static int getSubsetOf(int modifiers, int mask) {
    return modifiers & mask;
  }

  /**
   * Returns the subset flags not contained in a set of modifiers.
   *
   * @param	modifiers	the flags to test
   * @param	mask		the set of modifiers
   * @return	true iff the set of modifiers contains all specified flags
   */
  public static int notElementsOf(int modifiers, int mask) {
    return modifiers & ~mask;
  }

  /**
   * Returns the number of elements of subset of flags contained
   * in a set of modifiers.
   *
   * @param	modifiers	the flags to test
   * @param	mask		the set of modifiers
   * @return	true iff the set of modifiers contains all specified flags
   */
  public static int getSubsetSize(int modifiers, int mask) {
    int		count;

    modifiers &= mask;

    count = 0;
    for (int i = 0; i < 32; i++) {
      if (((1 << i) & modifiers) != 0) {
	count += 1;
      }
    }
    return count;
  }

  public static boolean checkOrder(int currentModifiers, int newModifier) {
    return getMaxPosition(currentModifiers) < getPosition(newModifier);
  }

  private static int getMaxPosition(int mod) {
    int		max = 0;

    for (int i = 0; i < 32; i++) {
      if (((1 << i) & mod) != 0) {
	max = Math.max(max, getPosition(1 << i));
      }
    }

    return max;
  }

  private static int getPosition(int mod) {
    switch (mod) {
    case ACC_PUBLIC:
      return 1;
    case ACC_PRIVATE:
      return 2;
    case ACC_PROTECTED:
      return 3;
    case ACC_ABSTRACT:
      return 4;
    case ACC_STATIC:
      return 5;
    case ACC_FINAL:
      return 6;
    case ACC_SYNCHRONIZED:
      return 7;
    case ACC_TRANSIENT:
      return 8;
    case ACC_VOLATILE:
      return 9;
    case ACC_NATIVE:
      return 10;
    case ACC_INTERFACE:
      return 11;
    case ACC_STRICT:
      return 12;
    default:
      throw new InconsistencyException();
    }
  }

  /**
   * Returns the name of the specified modifier.
   */
  public static String getName(int mod) {
    switch (mod) {
    case ACC_PUBLIC:
      return "public";
    case ACC_PRIVATE:
      return "private";
    case ACC_PROTECTED:
      return "protected";
    case ACC_STATIC:
      return "static";
    case ACC_FINAL:
      return "final";
    case ACC_SYNCHRONIZED:
      return "synchronized";
    case ACC_VOLATILE:
      return "volatile";
    case ACC_TRANSIENT:
      return "transient";
    case ACC_NATIVE:
      return "native";
    case ACC_INTERFACE:
      return "interface";
    case ACC_ABSTRACT:
      return "abstract";
    case ACC_STRICT:
      return "strictfp";
    default:
      throw new InconsistencyException();
    }
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  private static final String[]		NAMES = {
    "public",
    "private",
    "protected",
    "static",
    "final",
    "synchronized",
    "volatile",
    "transient",
    "native",
    null,		//interface not printed here
    "abstract",
    "strictfp"
  };

  private static final int[]		CODES = {
    ACC_PUBLIC,
    ACC_PRIVATE,
    ACC_PROTECTED,
    ACC_STATIC,
    ACC_FINAL,
    ACC_SYNCHRONIZED,
    ACC_VOLATILE,
    ACC_TRANSIENT,
    ACC_NATIVE,
    ACC_INTERFACE,
    ACC_ABSTRACT,
    ACC_STRICT
  };

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.CModifier other = new at.dms.kjc.CModifier();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.CModifier other) {
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}
