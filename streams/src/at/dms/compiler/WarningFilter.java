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
 * $Id: WarningFilter.java,v 1.1 2001-08-30 16:32:31 thies Exp $
 */

package at.dms.compiler;

/**
 * This interface filters warning
 */
public interface WarningFilter {

  /**
   * Rejects the warning, it will not be displayed
   */
  int	FLT_REJECT	= 0;
  /**
   * Forces the warning to be displayed
   */
  int	FLT_FORCE	= 1;
  /**
   * Does not decide, the warning will be displayed depending on the user
   * options (warning level, langage specification)
   */
  int	FLT_ACCEPT	= 2;

  // ----------------------------------------------------------------------
  // FILTER
  // ----------------------------------------------------------------------

  /**
   * Filters a warning
   * @param	warning		a warning to be filtred
   * @return	FLT_REJECT, FLT_FORCE, FLT_ACCEPT
   */
  int filter(CWarning warning);
}
