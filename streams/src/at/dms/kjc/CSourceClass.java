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
 * $Id: CSourceClass.java,v 1.5 2003-05-16 21:58:34 thies Exp $
 */

package at.dms.kjc;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

import at.dms.classfile.ClassConstant;
import at.dms.classfile.ClassFileFormatException;
import at.dms.classfile.ClassInfo;
import at.dms.classfile.CodeInfo;
import at.dms.classfile.FieldInfo;
import at.dms.classfile.FieldRefInstruction;
import at.dms.classfile.InnerClassInfo;
import at.dms.classfile.MethodInfo;
import at.dms.compiler.Compiler;
import at.dms.compiler.TokenReference;
import at.dms.util.Utils;

/**
 * This class represents the exported members of a class (inner classes, methods and fields)
 * It is build from a parsed files so values are accessibles differently after build and
 * after interface checked
 */
public class CSourceClass extends CClass {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected CSourceClass() {} // for cloner only

  /**
   * Constructs a class export from source
   */
  public CSourceClass(CClass owner,
		      TokenReference where,
		      int modifiers,
		      String ident,
		      String qualifiedName,
		      boolean deprecated)
  {
    super(owner, where.getFile(), modifiers, ident, qualifiedName, null, deprecated);
  }

  /**
   * Ends the definition of this class
   */
  public void close(CClassType[] interfaces,
		    CClassType superClass,
		    Hashtable fields,
		    CMethod[] methods)
  {
    setSuperClass(superClass);
    super.close(interfaces, fields, methods);
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * add synthetic parameters to method def
   */
  public CType[] genConstructorArray(CType[] params) {
    boolean		hasOuterThis = isNested() && !isStatic() && hasOuterThis();
    int			size = params.length + (hasOuterThis ? 1 : 0) + (outers == null ? 0 : outers.size());

    if (params.length == size) {
      return params;
    } else {
      CType[]		ret = new CType[size];
      Enumeration	enum = outers == null ? null : outers.keys();
      int		pos = 0;

      /* 15.9.5.1 The first formal parameter of the constructor of C 
         represents the value of the immediately enclosing instance of 
         i with respect to S */ 
      if (hasOuterThis) {
	ret[pos++] = getOwner().getType();
      }

      for (int i = 0; i < params.length; pos++, i++) {
	ret[pos] = params[i];
      }

      if (enum != null) {
	while (enum.hasMoreElements()) {
	  ret[pos++] = ((JLocalVariable)enum.nextElement()).getType();
	}
      }

      return ret;
    }
  }

  /**
   * add synthetic parameters to method call
   */
  public void genOuterSyntheticParams(CodeSequence code) {//, boolean qualified)
//     if (isNested() && !isStatic() && hasOuterThis() && !qualified) {
//       code.plantLoadThis();
//     }
    if (outers != null) {
      Enumeration	enum = outers.keys();
      while (enum.hasMoreElements()) {
	JLocalVariable	var = (JLocalVariable)enum.nextElement();

	new JLocalVariableExpression(TokenReference.NO_REF, var).genCode(code, false);
      }
    }
  }
    
    /**
     * Naming of variables abstracted away
     */
    public static String varName(JLocalVariable var) {
	return "var$" + var.getIdent();
    }

  /**
   * Gets the code to access outer local vars
   */
  public JExpression getOuterLocalAccess(TokenReference ref, 
                                         JLocalVariable var, 
                                         final CMethod constructor) {
    String		name;
    CSourceField	field;

    if (outers == null) {
      outers = new Hashtable();
    }
    if ((name = (String)outers.get(var)) == null) {
	name = varName(var);
      outers.put(var, name); // local vars are uniq
      field = new CSourceField(this, 0, name, var.getType(), false);
      addField(field);
      countSyntheticsFields++;
    } else {
      field = (CSourceField)getField(name);
    }

    // NOTE - major modification to work with StreamIt.  This could
    // break Kopi as a Java compiler.  Here we register references to
    // enclosing final locals even if there is not a constructor, so
    // that you might not know the data layout.  Commented out version
    // (original) appears below.

    return new JCheckedExpression(ref, new JLocalVariableExpression(ref, var));

    /*
      if (constructor != null) {
      final	CSourceField	ffield = field;
      JGeneratedLocalVariable local = new JGeneratedLocalVariable(null, 0, var.getType(), var.getIdent(), null) {
       
      // @return the local index in context variable table
      public int getPosition() {
      return 1  + constructor.getParameters().length + (hasOuterThis() ? 1 : 0) + ffield.getPosition() - (getFieldCount() - countSyntheticsFields);
      }
      };
      
      return new JCheckedExpression(ref, new JLocalVariableExpression(ref, local));
      } else {
      return new JFieldAccessExpression(ref, new JThisExpression(ref, getCClass()), name);
      }
    */
  }

  /**
   * add synthetic parameters to method def
   */
  public void genInit(CodeSequence code, int countLocals) {
    countLocals++; // this

    if (isNested() && !isStatic() && (hasOuterThis() || (outers != null && outers.size() > 0))) {
      JGeneratedLocalVariable var;
      if (hasOuterThis()) {
	var = new JGeneratedLocalVariable(null, 0, getOwner().getType(), JAV_OUTER_THIS, null); // $$$ 
	var.setPosition(1);
	countLocals += var.getType().getSize();
	code.plantLoadThis();
	var.genLoad(code);
	code.plantFieldRefInstruction(opc_putfield,
				      getQualifiedName(),
				      JAV_OUTER_THIS,
				      getOwner().getType().getSignature());
      }
      if (outers != null) {
	Enumeration	enum = outers.keys();
	while (enum.hasMoreElements()) {
	  JLocalVariable	ovar = (JLocalVariable)enum.nextElement();
	  var = new JGeneratedLocalVariable(null, 0, ovar.getType(), ovar.getIdent(), null);
	  var.setPosition(countLocals);
	  countLocals += var.getType().getSize();
	  code.plantLoadThis();
	  var.genLoad(code);
	  code.plantFieldRefInstruction(opc_putfield,
					getQualifiedName(),
					"var$" + var.getIdent(),
					var.getType().getSignature());
	}
      }
    }
  }

  // ----------------------------------------------------------------------
  // CODE GENERATION
  // ----------------------------------------------------------------------

  /**
   * Generates a JVM class file for this class.
   *
   * @param	optimizer	the bytecode optimizer to use
   * @param	destination	the root directory of the class hierarchy
   */
  public void genCode(BytecodeOptimizer optimizer, String destination)
    throws IOException, ClassFileFormatException
  {
    try {
      String[]	classPath = Utils.splitQualifiedName(getSourceFile(), File.separatorChar);

      ClassInfo	classInfo = new ClassInfo((short)(getModifiers() & (~ACC_STATIC)),
					  getQualifiedName(),
					  getSuperClass() == null ? null : getSuperClass().getQualifiedName(),
					  genInterfaces(),
					  genFields(),
					  genMethods(optimizer),
					  genInnerClasses(),
					  classPath[1],
					  false);
      classInfo.write(destination);
    } catch (ClassFileFormatException e) {
      System.err.println("GenCode failure in source class: "+getQualifiedName());
      throw e;
    }
  }

  /**
   * Builds information about interfaces to be stored in a class file.
   */
  private ClassConstant[] genInterfaces() {
    CClassType[]	source = getInterfaces();
    ClassConstant[]	result;

    result = new ClassConstant[source.length];
    for (int i = 0; i < result.length; i++) {
      result[i] = new ClassConstant(source[i].getQualifiedName());
    }
    return result;
  }

  /**
   * Builds information about inner classes to be stored in a class file.
   */
  private InnerClassInfo[] genInnerClasses() {
    CClassType[]	source = getInnerClasses();
    int			count;

    count = source.length;
    if (isNested()) {
      count += 1;
    }

    if (count == 0) {
      return null;
    } else {
      InnerClassInfo[]	result;

      result = new InnerClassInfo[count];
      for (int i = 0; i < source.length; i++) {
	CClass	clazz = source[i].getCClass();

	result[i] = new InnerClassInfo(clazz.getQualifiedName(),
				       getQualifiedName(),
				       clazz.getIdent(),
				       (short)clazz.getModifiers());
      }
      if (isNested()) {
	// add outer class info
	result[result.length - 1] = new InnerClassInfo(getQualifiedName(),
						       getOwner().getQualifiedName(),
						       getIdent(),
						       (short)getModifiers());
      }
      return result;
    }
  }

  /**
   * Builds information about methods to be stored in a class file.
   *
   * @param	optimizer	the bytecode optimizer to use
   */
  private MethodInfo[] genMethods(BytecodeOptimizer optimizer) {
    CMethod[]		source = getMethods();
    MethodInfo[]	result;

    result = new MethodInfo[source.length];
    for (int i = 0; i < source.length; i++) {
      result[i] = ((CSourceMethod)source[i]).genMethodInfo(optimizer);
    }
    return result;
  }

  /**
   * Builds information about fields to be stored in a class file.
   */
  private FieldInfo[] genFields() {
    CField[]		source = getFields();
    FieldInfo[]		result;

    result = new FieldInfo[source.length];
    for (int i = 0; i < source.length; i++) {
      result[i] = ((CSourceField)source[i]).genFieldInfo();
    }
    return result;
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  private Hashtable		outers;
  private int			countSyntheticsFields;
}
