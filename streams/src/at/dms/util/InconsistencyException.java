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
 * $Id: InconsistencyException.java,v 1.1 2001-08-30 16:32:58 thies Exp $
 */

package at.dms.util;

/**
 * An InconsistencyException indicates that an inconsistent internal state has
 * been discovered, usually due to incorrect program logic.
 */
public class InconsistencyException extends RuntimeException {

  /**
   * Constructs am InconsistencyException with no specified detail message.
   */
  public InconsistencyException() {
    super();
  }

  /**
   * Constructs am InconsistencyException with the specified detail message.
   *
   * @param	message		the detail message
   */
  public InconsistencyException(String message) {
    super(message);
  }
}
