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
 * $Id: DefaultFilter.java,v 1.3 2003-05-28 05:58:42 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.CWarning;

/**
 * This is the default warning filter
 * public class MyWarningFilter implements at.dms.kjc.DefaultFilter
 *
 *  // ----------------------------------------------------------------------
 *  // FILTER
 *  // ----------------------------------------------------------------------
 *
 *
 *  * Filters a warning
 *  * @param	warning		a warning to be filtred
 *  * @return	FLT_REJECT, FLT_FORCE, FLT_ACCEPT
 *  *
 *  * This filter accepts unused catch parameters if they are prefixed with an underscore
 *  *
 */
public class DefaultFilter implements at.dms.compiler.WarningFilter {

  // ----------------------------------------------------------------------
  // FILTER
  // ----------------------------------------------------------------------

  /**
   * Filters a warning
   * @param	warning		a warning to be filtred
   * @return	FLT_REJECT, FLT_FORCE, FLT_ACCEPT
   */
  public int filter(CWarning warning) {
    if (warning.hasDescription(KjcMessages.UNUSED_PARAMETER)
	|| warning.hasDescription(KjcMessages.CONSTANT_VARIABLE_NOT_FINAL)
	|| warning.hasDescription(KjcMessages.UNUSED_CATCH_PARAMETER)) {
      return FLT_REJECT;
    } else {
      return FLT_ACCEPT;
    }
  }

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.DefaultFilter other = new at.dms.kjc.DefaultFilter();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.DefaultFilter other) {
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}
