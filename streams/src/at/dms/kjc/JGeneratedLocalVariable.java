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
 * $Id: JGeneratedLocalVariable.java,v 1.3 2002-02-27 22:07:24 mgordon Exp $
 */

package at.dms.kjc;

import at.dms.compiler.TokenReference;
import at.dms.util.InconsistencyException;
import java.io.*;


/**
 * This class represents a local variable declaration
 */
public class JGeneratedLocalVariable extends JLocalVariable {
   private Integer serializationIndex;
    
    private void writeObject(ObjectOutputStream oos)
	throws IOException {
	this.serializationIndex = SerializationVector.addObject(this);
	oos.defaultWriteObject();
    }
    
    private Object readResolve() throws Exception {
	//if we want to deep clone the vars then just return this
	if (ObjectDeepCloner.deepCloneVars)
	    return this;
	else      //otherwise find the original object in the vector
	    return SerializationVector.getObject(serializationIndex);
    }
  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

  /**
   * Constructs a local variable definition
   * @param	modifiers	the modifiers on this variable
   * @param	name		the name of this variable
   * @param	type		the type of this variable
   * @param	value		the initial value
   * @param	where		the locztion of the declaration of this variable
   */
  public JGeneratedLocalVariable(TokenReference where,
				 int modifiers,
				 CType type,
				 String name,
				 JExpression value) {
    super(where, modifiers, DES_GENERATED, type, name, value);
  }

  // ----------------------------------------------------------------------
  // CODE GENERATION
  // ----------------------------------------------------------------------

  /**
   * Accepts the specified visitor
   * @param	p		the visitor
   */
  public void accept(KjcVisitor p) {
    throw new InconsistencyException();
  }

 /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
      throw new InconsistencyException();
  }


}
