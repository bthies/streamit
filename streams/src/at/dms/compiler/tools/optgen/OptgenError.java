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
 * $Id: OptgenError.java,v 1.2 2002-12-18 06:29:00 karczma Exp $
 */

package at.dms.compiler.tools.optgen;

import at.dms.compiler.tools.common.FormattedException;
import at.dms.compiler.tools.common.Message;
import at.dms.compiler.tools.common.MessageDescription;

/**
 * Error thrown on problems encountered while running the program.
 */
public class OptgenError extends FormattedException {

  // --------------------------------------------------------------------
  // CONSTRUCTORS
  // --------------------------------------------------------------------

  /**
   * An exception with a formatted message as argument
   * @param	message		the formatted message
   */
  public OptgenError(Message message) {
    super(message);
  }

  /**
   * An exception with an arbitrary number of parameters
   * @param	description	the message description
   * @param	parameters	the array of parameters
   */
  public OptgenError(MessageDescription description, Object[] parameters) {
    super(description, parameters);
  }

  /**
   * An exception with two parameters
   * @param	description	the message description
   * @param	parameter1	the first parameter
   * @param	parameter2	the second parameter
   */
  public OptgenError(MessageDescription description, Object parameter1, Object parameter2) {
    super(description, parameter1, parameter2);
  }

  /**
   * An exception with one parameter
   * @param	description	the message description
   * @param	parameter	the parameter
   */
  public OptgenError(MessageDescription description, Object parameter) {
    super(description, parameter);
  }

  /**
   * An exception without parameters
   * @param	description	the message description
   */
  public OptgenError(MessageDescription description) {
    super(description);
  }
}
