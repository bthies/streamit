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
 * $Id: NotImplementedException.java,v 1.1 2001-08-30 16:32:58 thies Exp $
 */

package at.dms.util;

/**
 * This exception is thrown when an unimplemented operation or feature
 * is requested.
 */
public class NotImplementedException extends RuntimeException {

  /**
   * Constructs am NotImplementedException with no specified detail message.
   */
  public NotImplementedException() {
    super();
  }

  /**
   * Constructs am NotImplementedException with the specified detail message.
   *
   * @param	message		the detail message
   */
  public NotImplementedException(String message) {
    super(message);
  }
}
