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
 * $Id: IncludeError.java,v 1.1 2001-08-30 16:32:40 thies Exp $
 */

package at.dms.compiler.tools.include;

import at.dms.util.FormattedException;
import at.dms.util.Message;
import at.dms.util.MessageDescription;

/**
 * Error thrown on problems encountered while running the program.
 */
public class IncludeError extends FormattedException {

  // --------------------------------------------------------------------
  // CONSTRUCTORS
  // --------------------------------------------------------------------

  /**
   * An exception with a formatted message as argument
   * @param	message		the formatted message
   */
  public IncludeError(Message message) {
    super(message);
  }

  /**
   * An exception with an arbitrary number of parameters
   * @param	description	the message description
   * @param	parameters	the array of parameters
   */
  public IncludeError(MessageDescription description, Object[] parameters) {
    super(description, parameters);
  }

  /**
   * An exception with two parameters
   * @param	description	the message description
   * @param	parameter1	the first parameter
   * @param	parameter2	the second parameter
   */
  public IncludeError(MessageDescription description, Object parameter1, Object parameter2) {
    super(description, parameter1, parameter2);
  }

  /**
   * An exception with one parameter
   * @param	description	the message description
   * @param	parameter	the parameter
   */
  public IncludeError(MessageDescription description, Object parameter) {
    super(description, parameter);
  }

  /**
   * An exception without parameters
   * @param	description	the message description
   */
  public IncludeError(MessageDescription description) {
    super(description);
  }
}
