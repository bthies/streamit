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
 * $Id: FormattedException.java,v 1.1 2002-12-11 20:14:58 karczma Exp $
 */

package at.dms.util;

/**
 * This class defines exceptions formatted using message descriptions.
 */
public class FormattedException extends Exception {

  // --------------------------------------------------------------------
  // CONSTRUCTORS
  // --------------------------------------------------------------------

  /**
   * An exception with a formatted message as argument
   * @param	message		the formatted message
   */
  public FormattedException(Message message) {
    super(message.getDescription().getFormat());

    this.message = message;
  }

  /**
   * An exception with an arbitrary number of parameters
   * @param	description	the message description
   * @param	parameters	the array of parameters
   */
  public FormattedException(MessageDescription description, Object[] parameters) {
    this(new Message(description, parameters));
  }

  /**
   * An exception with two parameters
   * @param	description	the message description
   * @param	parameter1	the first parameter
   * @param	parameter2	the second parameter
   */
  public FormattedException(MessageDescription description, Object parameter1, Object parameter2) {
    this(description, new Object[] { parameter1, parameter2 });
  }

  /**
   * An exception with one parameter
   * @param	description	the message description
   * @param	parameter	the parameter
   */
  public FormattedException(MessageDescription description, Object parameter) {
    this(description, new Object[] { parameter });
  }

  /**
   * An exception without parameters
   * @param	description	the message description
   */
  public FormattedException(MessageDescription description) {
    this(description, null);
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * Returns a string explaining the exception.
   */
  public String getMessage() {
    return message.getMessage();
  }

  /**
   * Returns the formatted message.
   */
  public Message getFormattedMessage() {
    return message;
  }

  /**
   * Returns true iff the error has specified description.
   */
  public boolean hasDescription(MessageDescription description) {
    return message.getDescription() == description;
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  private final Message		message;
}
