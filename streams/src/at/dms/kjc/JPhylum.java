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
 * $Id: JPhylum.java,v 1.9 2003-10-30 11:58:40 jasperln Exp $
 */

package at.dms.kjc;

import java.util.Vector;
import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;
import at.dms.util.MessageDescription;
import at.dms.util.Utils;
import at.dms.kjc.iterator.*;

/**
 * This class represents the root class for all elements of the parsing tree
 */
public abstract class JPhylum extends at.dms.compiler.Phylum implements Constants, Finalizable {
    /*static Vector registry = new Vector (200);
      
      public static void addTokenReference(JPhylum j) {
      registry.add(j);
      }
      
      public static JPhylum getTokenReference(int index) {
      return ((JPhylum) registry.get(index));
      }
      
      public static int regSize() {
      return registry.size();
      }*/

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JPhylum() {} // for cloner only

  /**
   * construct an element of the parsing tree
   * @param where the token reference of this node
   */
  public JPhylum(TokenReference where) {
    super(where);

    //this.addTokenReference(this);
  }


  // ----------------------------------------------------------------------
  // FINALIZABLE INTERFACE
  // ----------------------------------------------------------------------

    public void assertMutable() {
	Utils.assert(!IterFactory.isFinalized(this), 
		     "A mutability check failed.");
    }

  // ----------------------------------------------------------------------
  // ERROR HANDLING
  // ----------------------------------------------------------------------

  /**
   * Adds a compiler error.
   * Redefine this method to change error handling behaviour.
   * @param	context		the context in which the error occurred
   * @param	description	the message ident to be displayed
   * @param	params		the array of parameters
   *
   */
  protected void fail(CContext context, MessageDescription description, Object[] params)
    throws PositionedError
  {
    throw new CLineError(getTokenReference(), description, params);
  }

  /**
   * Verifies that the condition is true; otherwise adds an error.
   * @param	context		the context in which the check occurred
   * @param	cond		the condition to verify
   * @param	description	the message ident to be displayed
   * @param	params		the array of parameters
   */
  public final void check(CContext context, boolean cond, MessageDescription description, Object[] params)
    throws PositionedError
  {
    if (!cond) {
      fail(context, description, params);
    }
  }

  /**
   * Verifies that the condition is true; otherwise adds an error.
   * @param	context		the context in which the check occurred
   * @param	cond		the condition to verify
   * @param	description	the message ident to be displayed
   * @param	param1		the first parameter
   * @param	param2		the second parameter
   */
  public final void check(CContext context, boolean cond, MessageDescription description, Object param1, Object param2)
    throws PositionedError
  {
    if (!cond) {
      fail(context, description, new Object[] { param1, param2 });
    }
  }

  /**
   * Verifies that the condition is true; otherwise adds an error.
   * @param	context		the context in which the check occurred
   * @param	cond		the condition to verify
   * @param	description	the message ident to be displayed
   * @param	param		the parameter
   */
  public final void check(CContext context, boolean cond, MessageDescription description, Object param)
    throws PositionedError
  {
    if (!cond) {
      fail(context, description, new Object[] { param });
    }
  }

  /**
   * Verifies that the condition is true; otherwise adds an error.
   * @param	context		the context in which the check occurred
   * @param	cond		the condition to verify
   * @param	description	the message ident to be displayed
   */
  public final void check(CContext context, boolean cond, MessageDescription description)
    throws PositionedError
  {
    if (!cond) {
      fail(context, description, null);
    }
  }

  // ----------------------------------------------------------------------
  // CODE GENERATION
  // ----------------------------------------------------------------------

  /**
   * Accepts the specified visitor
   * @param	p		the visitor
   */
  public abstract void accept(KjcVisitor p);

    /**
     * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public abstract Object accept(AttributeVisitor p);

  /**
   * Sets the line number of this phylum in the code sequence.
   *
   * @param	code		the bytecode sequence
   */
  public void setLineNumber(CodeSequence code) {
    code.setLineNumber(getTokenReference().getLine());
  }

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() { at.dms.util.Utils.fail("Error in auto-generated cloning methods - deepClone was called on an abstract class."); return null; }

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.JPhylum other) {
  super.deepCloneInto(other);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}
