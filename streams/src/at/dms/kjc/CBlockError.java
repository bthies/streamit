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
 * $Id: CBlockError.java,v 1.1 2001-08-30 16:32:50 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;
import at.dms.util.Message;
import at.dms.util.MessageDescription;

/**
 * This class represents block errors in the error hierarchy.
 */
public class CBlockError extends PositionedError {

  // --------------------------------------------------------------------
  // CONSTRUCTORS
  // --------------------------------------------------------------------

  /**
   * An error with a formatted message as argument
   * @param	where		the position in the source code
   * @param	message		the formatted message
   */
  public CBlockError(TokenReference where, Message message) {
    super(where, message);
  }

  /**
   * An error with an arbitrary number of parameters
   * @param	where		the position in the source code
   * @param	description	the message description
   * @param	parameters	the array of parameters
   */
  public CBlockError(TokenReference where, MessageDescription description, Object[] parameters) {
    super(where, description, parameters);
  }

  /**
   * An error with two parameters
   * @param	where		the position in the source code
   * @param	description	the message description
   * @param	parameter1	the first parameter
   * @param	parameter2	the second parameter
   */
  public CBlockError(TokenReference where,
		     MessageDescription description,
		     Object parameter1,
		     Object parameter2)
  {
    super(where, description, parameter1, parameter2);
  }

  /**
   * An error with one parameter
   * @param	where		the position in the source code
   * @param	description	the message description
   * @param	parameter	the parameter
   */
  public CBlockError(TokenReference where, MessageDescription description, Object parameter) {
    super(where, description, parameter);
  }

  /**
   * An error without parameters
   * @param	where		the position in the source code
   * @param	description	the message description
   */
  public CBlockError(TokenReference where, MessageDescription description) {
    super(where, description);
  }
}
