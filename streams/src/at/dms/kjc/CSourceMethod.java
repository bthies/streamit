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
 * $Id: CSourceMethod.java,v 1.3 2003-05-16 21:58:34 thies Exp $
 */

package at.dms.kjc;

import at.dms.classfile.MethodInfo;
import at.dms.classfile.CodeInfo;

/**
 * This class represents an exported member of a class (fields)
 */
public class CSourceMethod extends CMethod {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected CSourceMethod() {} // for cloner only

  /**
   * Constructs a method export.
   *
   * @param	owner		the owner of this method
   * @param	modifiers	the modifiers on this method
   * @param	ident		the ident of this method
   * @param	returnType	the return type of this method
   * @param	paramTypes	the parameter types of this method
   * @param	exceptions	a list of all exceptions in the throws list
   * @param	deprecated	is this method deprecated
   * @param	body		the source code
   */
  public CSourceMethod(CClass owner,
		       int modifiers,
		       String ident,
		       CType returnType,
		       CType[] paramTypes,
		       CClassType[] exceptions,
		       boolean deprecated,
		       JBlock body)
  {
    super(owner, modifiers, ident, returnType, paramTypes, exceptions, deprecated);

    this.body = body;
  }


  /**
   * Accessor for the body of the method
   *
   */
    
    public JBlock getBody() {
	return body;
    }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  public boolean isUsed() {
    return used || !isPrivate() || getIdent().indexOf("$") >= 0; // $$$
  }

  public void setUsed() {
    used = true;
  }

  // ----------------------------------------------------------------------
  // GENERATE CLASSFILE INFO
  // ----------------------------------------------------------------------

  /**
   * Generate the code in a class file
   *
   * @param	optimizer	the bytecode optimizer to use
   */
  public MethodInfo genMethodInfo(BytecodeOptimizer optimizer) {
    CClassType[]	excs = getThrowables();
    String[]		exceptions = new String[excs.length];
    for (int i = 0; i < excs.length; i++) {
      exceptions[i] = excs[i].getQualifiedName();
    }

    return new MethodInfo((short)getModifiers(),
			  getIdent(),
			  getSignature(),
			  exceptions,
			  body != null ? optimizer.run(genCode()) : null,
			  isDeprecated(),
			  false);
  }

  /**
   * @return the type of this field
   */
  public String getSignature() {
    CType[]     params = getParameters();

    if (getOwner().isNested() && isConstructor()) {
      params = ((CSourceClass)getOwner()).genConstructorArray(params);
    }
    return CType.genMethodSignature(getReturnType(), params);
  }

  /**
   * Generates JVM bytecode for this method.
   */
  public CodeInfo genCode() {
    CodeSequence	code = CodeSequence.getCodeSequence();

    body.genCode(code);
    if (getReturnType() == CStdType.Void) {
      code.plantNoArgInstruction(opc_return);
    }

    CodeInfo info = new CodeInfo(code.getInstructionArray(),
				 code.getHandlers(),
				 code.getLineNumbers(),
				 null);    //code.getLocalVariableInfos());
    code.release();
    body = null;

    CType[]	parameters = getParameters();
    int		paramCount = 0;

    for (int i = 0; i < parameters.length; i++) {
      paramCount += parameters[i].getSize();
    }
    paramCount += getReturnType().getSize();
    paramCount += isStatic() ? 0 : 1;

    info.setParameterCount(paramCount);

    return info;
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  private JBlock		body;
  private boolean		used;
}
