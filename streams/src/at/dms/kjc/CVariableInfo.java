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
 * $Id: CVariableInfo.java,v 1.4 2003-08-29 19:25:36 thies Exp $
 */

package at.dms.kjc;

/**
 * This class represents a local variable information during check
 */
public final class CVariableInfo implements DeepCloneable {

  public CVariableInfo() {
    infos = new int[1];
  }

  private CVariableInfo(int[] infos) {
    this.infos = infos;
  }

  public Object clone() {
    return new CVariableInfo((int[])infos.clone());
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  public int getInfo(int pos) {
    int		sub = subscript(pos);
    int		set = subset(pos);

    return sub < infos.length ? (infos[sub] >> (2 * set)) & 3 : 0;
  }

  public void setInfo(int pos, int info) {
    int		sub = subscript(pos);
    int		set = subset(pos);

    if (sub >= infos.length) {
      int[]	temp = new int[sub + 1];

      System.arraycopy(infos, 0, temp, 0, infos.length);
      infos = temp;
    }

    infos[sub] = ((info & 3) << (2 * set)) | (infos[sub] & ~(3 << (2 * set)));
  }

  // ----------------------------------------------------------------------
  // PUBLIC UTILITIES
  // ----------------------------------------------------------------------

  /**
   * merge
   * @param	other		the second JLocalVariable info
   * @return	the merging information onto this flags
   */
  public static final int merge(int info1, int info2) {
    return ((info1 | info2) & INF_MAYBE_INITIALIZED) | ((info1 & info2) & INF_INITIALIZED);
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * initialize
   */
  public static final int initialize() {
    return INF_INITIALIZED | INF_MAYBE_INITIALIZED;
  }

  /**
   * isInitialized
   */
  public static final boolean isInitialized(int info) {
    return (info & INF_INITIALIZED) != 0;
  }

  /**
   * mayBeInitialized
   */
  public static final boolean mayBeInitialized(int info) {
    return (info & INF_MAYBE_INITIALIZED) != 0;
  }

  // ----------------------------------------------------------------------
  // PRIVATE UTILITIES
  // ----------------------------------------------------------------------

  private static int subscript(int pos) {
    return pos >> 4;
  }

  private static int subset(int pos) {
    return pos % 16;
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  private static final int	INF_INITIALIZED		= 0x01;
  private static final int	INF_MAYBE_INITIALIZED	= 0x02;

  public static final int	INITIALIZED = INF_INITIALIZED | INF_MAYBE_INITIALIZED;

  private int[]			infos;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.CVariableInfo other = new at.dms.kjc.CVariableInfo();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.CVariableInfo other) {
  other.infos = (int[])at.dms.kjc.AutoCloner.cloneToplevel(this.infos, other);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}
