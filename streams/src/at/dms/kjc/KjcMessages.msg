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
 * $Id: KjcMessages.msg,v 1.1 2001-08-30 16:32:53 thies Exp $
 */

package at.dms.kjc

prefix Kjc
parent at.dms.compiler.CompilerMessages

// DRIVER ERRORS
error		FILTER_NOT_FOUND
		"Warning filter \"{0}\" not found"

// JLS ERRORS
error		INVALID_DOUBLE_LITERAL
		"\"{0}\" is not a double literal"
		"JLS 3.10.2"

error		DOUBLE_LITERAL_OVERFLOW
		"Double literal \"{0}\" is too large"
		"JLS 3.10.2"

error		DOUBLE_LITERAL_UNDERFLOW
		"Double literal \"{0}\" is too small"
		"JLS 3.10.2"

error		INVALID_FLOAT_LITERAL
		"\"{0}\" is not a float literal"
		"JLS 3.10.2"

error		FLOAT_LITERAL_OVERFLOW
		"Float literal \"{0}\" is too large"
		"JLS 3.10.2"

error		FLOAT_LITERAL_UNDERFLOW
		"Float literal \"{0}\" is too small"
		"JLS 3.10.2"

error		INVALID_INT_LITERAL
		"\"{0}\" is not a int literal"
		"JLS 3.10.1"

error		INVALID_LONG_LITERAL
		"\"{0}\" is not a long literal"
		"JLS 3.10.1"

error		INVALID_OCTAL_CHAR
		"\"{0}\" is not a valid char constant [value should be <= 0377]"
		"JLS 3.10.4, 3.10.6"

error		INVALID_ESCAPE_SEQUENCE
		"\"{0}\" contains an invalid escape sequence"
		"JLS 3.10.6"

error		CLASS_NOACCESS
		"Class \"{0}\" is not accessible"
		"JLS 6.6.1"

error		CLASS_NAME_FILENAME
		"Class \"{0}\" is public and should be defined in a file with same name and not \"{1}\""
		"JLS 7.6"

error		DUPLICATE_TYPE_NAME
		"Duplicate declaration of type \"{0}\" in the same package"
		"JLS 7.6"

error		DUPLICATE_MODIFIER
		"Modifier \"{0}\" appears more than once in declaration"
		"JLS 8.1.1, 8.3.1, 8.4.3, 8.8.3, 9.1.1"

error		NOT_CLASS_MODIFIERS
		"Modifier(s) \"{0}\" cannot be applied to classes."
		"JLS 8.1.1"

error		INVALID_CLASS_MODIFIERS
		"Class modifier(s) \"{0}\" are not valid in this context."
		"JLS 8.1.1"

error		CLASS_MUST_IMPLEMENT
		"Non-abstract class \"{0}\" must implement inherited method \"{1}\""
		"JLS 8.1.1.1"

error		CLASS_PARENT_FINAL
		"Super class \"{0}\" is final and cannot be extended"
		"JLS 8.1.1.2"

error		CLASS_ACCESSPARENT
		"Super class \"{0}\" is not accessible : it is not in the same package and not public"
		"JLS 8.1.3, 6.6"

error		CLASS_CIRCULARITY
		"Class \"{0}\" inherits from itself (is circularly declared)"
		"JLS 8.1.3"

error		CLASS_EXTENDS_INTERFACE
		"A class cannot extend interface \"{0}\" (use implements instead)"
		"JLS 8.1.3"

error		SUPERINTERFACE_WRONG_TYPE
		"Type \"{0}\" named in extends/implements clause is not an interface type"
		"JLS 8.1.4, 9.1.2"

error		SUPERINTERFACE_NOT_ACCESSIBLE
		"Superinterface \"{0}\" is not accessible : it is not in the same package and not public"
		"JLS 8.1.4, 9.1.2, 6.6"

error		INTERFACES_DUPLICATE
		"Duplicate extension/implementation of interface \"{1}\" by \"{0}\""
		"JLS 8.1.4"

error		NOT_CLASS_FIELD_MODIFIERS
		"Modifier(s) \"{0}\" cannot be applied to class fields."
		"JLS 8.3.1"

error		UNINITIALIZED_FINAL_FIELD
		"Final field \"{0}\" may have not been initialized"
		"JLS 8.3.1.2"

error		CLASS_AMBIGUOUS
		"Class \"{0}\" is ambiguous, it is inherited from several super classes or interfaces"
		"JLS 8.5"

error		CYCLE_IN_CONSTRUCTOR_CALL
		"Constructor invokes itself (directly or indirectly)"
		"JLS 8.8.5"

error		NOT_INTERFACE_MODIFIERS
		"Modifier(s) \"{0}\" cannot be applied to interfaces."
		"JLS 9.1.1"

error		INVALID_INTERFACE_MODIFIERS
		"Interface modifier(s) \"{0}\" are not valid in this context."
		"JLS 9.1.1"

error		NOT_INTERFACE_FIELD_MODIFIERS
		"Modifier(s) \"{0}\" cannot be applied to interface fields."
		"JLS 9.3"

error		QUALIFIED_STATIC
		"Impossible qualified instance creation of the static class \"{0}\"."
		"JLS 15.9.1"

error		NOT_INNER_CLASS
		"\"{0}\" is not an inner class."
		"JLS 15.9.1"

error		INVALID_METHOD_NAME
		"Invalid method name"
		"JLS 15.12"

// ----------------------------------------------------------------------
// CAUTIONS

caution		SELF_ASSIGNMENT
		"Assignment of an expression to itself"
		"KOPI"

// ----------------------------------------------------------------------
// WARNINGS

warning		NARROWING_COMPOUND_ASSIGNMENT
		"Implicit narrowing conversion from \"{0}\" to \"{1}\" occurs in compound assignment"
		"JLS 15.26.2, 5.1.3"

warning		INSTANCE_PREFIXES_STATIC_METHOD
		"Avoid using an instance to access class (static) method \"{0}\"; use class name \"{1}\" instead"
		"JCC 10.2"

warning		INSTANCE_PREFIXES_STATIC_FIELD
		"Avoid using an instance to access a class (static) field \"{0}\"; use class name \"{1}\" instead"
		"JCC 10.2"

warning		UNNECESSARY_INSTANCEOF
		"Instanceof evaluation is not necessary"
		"KOPI"

warning		FINAL_FIELD_IMPLICITLY_STATIC
		"Field \"{0}\" is implicitly static: it has the same value for each instance"
		"KOPI"

warning		UNUSED_LABEL
		"Label \"{0}\" is not used"
		"KOPI"

// ----------------------------------------------------------------------
// STYLE

notice		DUPLICATE_CLASS_IMPORT
		"Duplicate single-type-import declaration of \"{0}\""
		"JLS 7.5.1"

notice		IMPORT_CURRENT_PACKAGE
		"On-Demand-Import of current package is not necessary"
		"JLS 7.5.2"

notice		IMPORT_JAVA_LANG
		"On-Demand-Import of package java.lang is not necessary"
		"JLS 7.5.2"

notice		MODIFIER_ORDER
		"Modifiers appear in non-standard order [{0}]"
		"JLS 8.1.1, 8.3.1, 8.4.3, 8.8.3, 9.1.1"

notice		INTERFACE_IMPLEMENTED_BY_SUPERCLASS
		"Interface \"{0}\" already implemented/extended by superclass/interface \"{1}\""
		"JLS 8.1.4"

notice		METHOD_UNCHECKED_EXCEPTION
		"Declaring unchecked exception \"{0}\" in the throws clause is not necessary"
		"JLS 8.4.4"

notice		OLD_STYLE_ARRAY_BOUNDS
		"Old style array bound declaration"
		"JCC"

notice		ENCLOSE_IF_ELSE_IN_BLOCK
		"Else part of if-statement should be enclosed in block statement"
		"JCC 7.2, 7.4"

notice		ENCLOSE_IF_THEN_IN_BLOCK
		"Then part of if-statement should be enclosed in block statement"
		"JCC 7.2, 7.4"

notice		ENCLOSE_LOOP_BODY_IN_BLOCK
		"Loop body should be enclosed in block statement"
		"JCC 7.2, 7.5, 7.6, 7.7"

notice		ASSIGNMENT_IN_CONDITION
		"Assignment operator may have been used in place of equality operator"
		"JCC 10.4"

notice		REPLACE_PACKAGE_IMPORT
		"Replace package import \"{0}\" by import of class(es) \"{1}\""
		"KOPI"

notice		STRAY_COMMA
		"Stray comma"
		"KOPI"

notice		STRAY_SEMICOLON
		"Stray semicolon"
		"KOPI"

notice		COMPARING_BOOLEAN_CONSTANT
		"Comparing boolean expression to constant values is not necessary"
		"KOPI"

notice		UNUSED_PACKAGE_IMPORT
		"Imported package \"{0}\" is not used"
		"KOPI"

notice		UNUSED_CLASS_IMPORT
		"Imported class \"{0}\" is not used"
		"KOPI"

notice		UNUSED_PRIVATE_FIELD
		"Private field \"{0}\" is not used"
		"KOPI"

notice		UNUSED_PRIVATE_METHOD
		"Private method \"{0}\" is not used"
		"KOPI"

notice		UNUSED_LOCALVAR
		"Local variable \"{0}\" is not used"
		"KOPI"

notice		CONSTANT_VARIABLE_NOT_FINAL
		"Local variable \"{0}\" is constant and should be declared final"
		"KOPI"

notice		CASE_FALL_THROUGH
		"Fall through is dangerous and should not be used"
		"KOPI"

notice		STRING_COMPARISON
		"Comparison of strings with '==' operator"
		"KOPI"

// ----------------------------------------------------------------------
// FIXME graf 010511 : NOT SORTED BEYOND THIS POINT

error		CLASS_UNKNOWN
		"Cannot find class \"{0}\""
		"JLS 8"

error		INNER_INHERITENCE
		"Class \"{0}\" is not static and cannot be constructed from \"{1}\""

error		CUNIT_RENAME2
		"Type \"{0}\" is imported by at least two import-on-demand statements"

error		FIELD_AMBIGUOUS
		"Field \"{0}\" is ambiguous, it is inherited from several super classes or interfaces"
		"JLS 15.11"

error		FIELD_RENAME
		"Field \"{0}\" is already defined in current class"
		"JL1 8.3"

error		REASSIGNMENT_FINAL_FIELD
		"Final field \"{0}\" may have already been initialized"
		"JL1 8.3.1.2"

error		ASSIGNMENT_NOTLVALUE
		"Try to assign a value into something that is not an l-value"
		"JLS 15.26"

error		ASSIGNMENT_BADTYPE
		"Try to assign a value of type \"{0}\" to a variable of type \"{1}\""
		"JLS 15.26"

error		CLAZZ_RENAME
		"Class \"{0}\" is already defined in current block"
		"JL1 8.1.1"

error		VAR_UNKNOWN
		"Variable \"{0}\" is not defined in current context"

error		VARIABLE_REDECLARED
		"Redeclaration of local variable \"{0}\" in current scope"
		"JLS 14.4.2"

error		UNINITIALIZED_LOCAL_VARIABLE
		"Local variable \"{0}\" may have not been initialized before use"
		"JLS 14.4"

error		VAR_INIT_BADTYPE
		"Variable \"{0}\" cannot be initialized by a value of type \"{1}\""
		"JLS 4.4.4"

error		LABEL_ALREADY_EXISTS
		"Label \"{0}\" is already defined in current statement hierarchy"
		"JLS 14.7"

error		LABEL_UNKNOWN
		"Label \"{0}\" is not defined in current statement hierarchy"
		"JLS 14.7"

error		INVALID_EXPRESSION_STATEMENT
		"This expression cannot be used as a statement"
		"JLS 14.8"

error		IF_COND_NOTBOOLEAN
		"The condition of a if statement must have type boolean, not \"{0}\""
		"JLS 14.9"

error		SWITCH_DEFAULT_DOUBLE
		"This default selector must be unique in switch"
		"JLS 14.10"

error		SWITCH_LABEL_EXPR_NOTCONST
		"Switch label must be constant"
		"JLS 14.10"

error		SWITCH_LABEL_OVERFLOW
		"Label of type \"{0}\" is not assignable to the type of the switch expression"
		"JLS 14.10"

error		SWITCH_LABEL_EXIST
		"Label \"{0}\" is already defined in current switch"
		"JLS 14.10"

error		SWITCH_BADEXPR
		"Switch expression must be ordinal"
		"JLS 14.10"

error		WHILE_COND_NOTBOOLEAN
		"The condition of a while statement must have type boolean, not \"{0}\""
		"JLS 14.11"

error		DO_COND_NOTBOOLEAN
		"The condition of a do statement must have type boolean, not \"{0}\""
		"JLS 14.12"

error		FOR_COND_NOTBOOLEAN
		"The condition of a for statement must have type boolean, not \"{0}\""
		"JLS 14.13"

error		SYNCHRONIZED_NOTREFERENCE
		"synchronized expression must be reference"
		"JLS 14.18"

error		STATEMENT_UNREACHABLE
		"Statement is unreachable"
		"JLS 14.20"

error		CATCH_UNREACHABLE
		"Catch block is unreachable"
		"JLS 14.20"

error		TYPE_UNKNOWN
		"Cannot find type \"{0}\""

error		CLASSFILE_TYPE_UNKNOWN
		"Cannot find type \"{0}\""

error		CANNOT_BREAK
		"Break statement not enclosed by breakable statement"
		"JLS 14.14"

error		CANNOT_CONTINUE
		"Continue statement not enclosed by loop statement"
		"JLS 14.15"

error		CONTINUE_NOTLOOP
		"Target of continue statement is not a loop statement"
		"JLS 14.15"

error		CONSTRUCTOR_NOT_FOUND
		"Cannot find constructor \"{0}\""

error		METHOD_NOT_FOUND
		"Cannot find method \"{0}\""

error		METHOD_REDEFINE
		"Duplicate declaration of method \"{0}\""
		"JLS 8.4"

error		METHOD_FLAGS_IN_INTERFACE
		"Method flags must be some of: public, abstract"
		"JL1 9.3"

error		METHOD_FLAGS
		"Method flags must be some of: public, protected, private, abstract, final, static, native, synchronized, strictfp"
		"JLS 8.4.3"

error		METHOD_INVOCATION_AMBIGUOUS
		"Method invocation \"{0}\" is ambiguous: candidates are at least \"{1}\" and \"{2}\""
		"JLS 15.12"

error		METHOD_OVERRIDE_FINAL
		"Method \"{0}\" is declared final in a super class"
		"JLS 8.4.3.3"

error		BAD_METHOD_NAME
		"Method call not allowed on something that is not an object"
		"JLS 15.12.1"

error		FIELD_AS_METHOD
		"\"{0}\" is not the name of a method, but a field name"
		"JLS 15.12.1"

error		TYPE_AS_METHOD
		"\"{0}\" is not the name of a method, but a type name: use new for constructors"
		"JLS 15.12.1"

error		METHOD_ABSTRACT_CALL
		"Cannot explicitly call abstract method \"{0}\""

error		METHOD_UNCATCHED_EXCEPTION
		"Exception \"{0}\" is not catched and does not appear in throws list"
		"JLS 8.4.4"

error		METHOD_THROWS_DIFFERENT
		"Method \"{0}\" cannot throw \"{1}\" because is not declared in overidden method"
		"JLS 8.4.4"

error		METHOD_INSTANCE_OVERRIDES_STATIC
		"Instance method \"{0}\" cannot override static method in \"{1}\""
		"JLS 8.4.6.2"

error		METHOD_STATIC_HIDES_INSTANCE
		"Static method \"{0}\" cannot hide instance method inherited from \"{1}\""
		"JLS 8.4.6.2"

error		METHOD_RETURN_DIFFERENT
		"Method \"{0}\" cannot redefine method with different return type"
		"JLS 8.4.6.3"

error		METHOD_ACCESS_DIFFERENT
		"Method \"{0}\" cannot redefine method in \"{1}\" with more restrictive access"
		"JLS 8.4.6.3"

error		METHOD_NOTFOUND
		"Cannot find method \"{0}\" with good parameters"
		"JLS 15.12.2.1"

error		METHOD_NOACCESS
		"Cannot access method \"{0}\""
		"JLS 15.12.2.1"

error		METHOD_BADPREFIX
		"Prefix of method \"{0}\" must be a reference, is \"{1}\""
		"JLS 15.12.1"

error		METHOD_BODY_IN_INTERFACE
		"A method defined in an interface cannot have a body"
		"JL1 9.4"

error		METHOD_BODY_NATIVE_ABSTRACT
		"A native or abstract method cannot have a body"
		"JLS 8.4.3.1, 8.4.3.4"

error		METHOD_NOBODY_NOABSTRACT
		"A non-abstract method must have a body"
		"JLS 8.4.5"

error		METHOD_ABSTRACT_CLASSNOT
		"A method defined abstract must be defined in an abstract class"
		"JLS 8.4.3.1"

error		METHOD_NEED_RETURN
		"Method \"{0}\" must return a value"
		"JLS 8.4.5"

error		INVALID_CONSTRUCTOR_FLAGS
		"Cannot use an other flag than (public, protected, private) for constructor {0}"
		"JLS 8.8.2"

error		CONSTRUCTOR_NOBODY
		"Constructor {0} must have a body"
		"JL1 8.6.5"

error		CONSTRUCTOR_BAD_NAME
		"Constructor {0} must be named {1}"
		"JL1 8.6"

error		CONSTRUCTOR_NOTFOUND
		"Cannot find constructor \"{0}\" with matching parameters"
		"JLS 15.12"

error		CONSTRUCTOR_EXPLICIT_CALL
		"Cannot access field \"{0}\" in explicit constructor call"
		"JL1 8.6"

error		INITIALIZER_RETURN
		"Return statement are not allowed in initializers"
		"JL1 8.5"

error		STATIC_INITIALIZER_THROWABLE
		"Checked exceptions are not allowed in static initializers"
		"JL1 8.5"


error		METHOD_STATIC_BAD
		"Try to access the non static methods from a static context"
		"JLS 15.12"

error		CONSTRUCTOR_NOACCESS
		"Cannot access constructor \"{0}\""
		"JLS 15.12.2.1"

error		TRINARY_BADCOND
		"Condition in trinary expression must be boolean"
		"JLS 15.25"

error		TRINARY_INCOMP
		"Incompatible types in trinary expression"
		"JLS 15.25"

error		NEW_ABSTRACT
		"Cannot create a new object of type \"{0}\" because it is an abstract class"
		"JLS 15.9"

error		NEW_INTERFACE
		"Cannot create a new object of type \"{0}\" because it is an interface"
		"JLS 15.9"

error		POSTFIX_BADTYPE
		"Cannot apply postfix operator onto \"{0}\""
		"JLS 15.14"

error		POSTFIX_NOTLVALUE
		"Cannot apply postfix operator onto something that is not an l-value"
		"JLS 15.14"

error		POSTFIX_NOTINITIALIZED
		"Cannot apply postfix operator onto a variable that may not have been initialized"
		"JLS 15.14"

error		PREFIX_BADTYPE
		"Cannot apply prefix operator onto \"{0}\""
		"JLS 15.15.1"

error		PREFIX_NOTLVALUE
		"Cannot apply prefix operator onto something that is not an l-value"
		"JLS 15.15.1"

error		PREFIX_NOTINITIALIZED
		"Cannot apply prefix operator onto a variable that may not have been initialized"
		"JLS 15.15.1"

error		UNARY_BADTYPE_PM
		"Cannot apply unary operator + / - onto \"{0}\""
		"JLS 15.15.3, 15.15.4"

error		UNARY_BADTYPE_BNOT
		"Cannot apply unary operator ~ onto \"{0}\""
		"JLS 15.15.5"

error		UNARY_BADTYPE_LNOT
		"Cannot apply unary operator ! onto \"{0}\""
		"JLS 15.15.6"

error		EQUALITY_BADTYPE
		"Cannot apply == operator onto \"{0}\" and \"{1}\""
		"JL1 16.1.9"

error		RELATIONAL_TYPE
		"Cannot apply relational operator onto \"{0}\" and \"{1}\""
		"JLS 15.20"

error		BINARY_NUMERIC_BAD_TYPES
		"Invalid operand types \"{1}\" and \"{2}\" for numeric operator {0}"
		"JLS 5.6.2"

error		ADD_BADTYPE
		"Incompatible operand types \"{0}\" and \"{1}\" for operator +"
		"JLS 15.18.2"

error		MULT_BADTYPE
		"Incompatible operand types \"{0}\" and \"{1}\" for operator *"
		"JLS 15.17.1"

error		DIVIDE_BADTYPE
		"Incompatible operand types \"{0}\" and \"{1}\" for operator /"
		"JLS 15.17.2"

error		MINUS_BADTYPE
		"Incompatible operand types \"{0}\" and \"{1}\" for operator -"
		"JLS 15.18.2"

error		MODULO_BADTYPE
		"Incompatible operand types \"{0}\" and \"{1}\" for operator %"
		"JLS 15.17.3"

error		BITWISE_BADTYPE
		"Incompatible operand types \"{0}\" and \"{1}\" for bitwise operator (&, ^, |)"
		"JLS 15.22"

error		SHIFT_BADTYPE
		"Incompatible operand types \"{0}\" and \"{1}\" for shift operator (<<, >>, >>>)"
		"JLS 15.19"

error		DIVIDE_BY_ZERO
		"Cannot divide a number by zero"
		"JLS 15.28"

error		AND_BADTYPE
		"Incompatible operand types \"{0}\" and \"{1}\" for operator &&"
		"JLS 15.23"

error		OR_BADTYPE
		"Incompatible operand types \"{0}\" and \"{1}\" for operator ||"
		"JLS 15.24"

error		CAST_CANT
		"Cannot cast from \"{0}\" to \"{1}\""
		"JLS 15.16"

error		RETURN_BADTYPE
		"Type \"{0}\" cannot be returned as a \"{1}\""
		"JLS 14.16"

error		RETURN_EMPTY_NONVOID
		"Return statement with no value in method returning non-void"
		"JLS 14.16"

error		RETURN_NONEMPTY_VOID
		"Return statement with a value in method returning void"
		"JLS 14.16"

error		INSTANCEOF_BADTYPE
		"Cannot apply operator \"instanceof\" to \"{0}\" and \"{1}\""
		"JLS 15.20.2"

error		MULTIARRAY_BOUND_MISSING
		"Bound missing in multidimensional array creation"
		"JLS 10.3"

error		ARRAY_BOUND_AND_INITIALIZER
		"Specified both array bounds and initializer"
		"JLS 10.3"

error		ARRAY_EXPRESSION_INT
		"Array accessor must be ordinal but was \"{0}\""
		"JLS 10.4"

error		ARRAY_PREFIX
		"Array accessor prefix must be an array type but was \"{0}\""
		"JLS 10.1"

error		ARRAY_BADTYPE
		"Array size must be ordinal but was \"{0}\""
		"JLS 10.3"

error		ARRAY_INIT_BADTYPE
		"Array initializers value must be assignable to \"{0}\" type but was \"{1}\""
		"JLS 10.6"

error		ARRAY_INIT_NOARRAY
		"Array initializers cannot be applied to \"{0}\""
		"JLS 10.6"

error		ARRAY_LENGTH_BADTYPE
		"Try to get length of \"{0}\" which is not an array"
		"JLS 10.7"

error		CLASS_BAD_PREFIX
		"Invalid prefix for \".class\" operator"
		"JLS 15.8.2"

error		THIS_BADACCESS
		"Can only apply \".this\" operator onto references"
		"JLS 15.8.3"

error		THIS_INVALID_OUTER
		"Invalid outer class for this operation"
		"JLS 15.8.3"

error		BAD_THIS_STATIC
		"In this static context \"this\" is not accessible (no instance field or method)"
		"JLS 15.8.3"

error		TRY_UNREACHABLE
		"Exception \"{0}\" is already catched by previous catch"
		"JLS 14.19"

error		TRY_NOCATCH
		"A try statement must have at least one catch or finally block"
		"JLS 14.19"

error		TRY_CATCH_NOTHROW
		"Cannot catch \"{0}\" that it is not thrown in the try statement"
		"JLS 14.19"

error		THROW_BADTYPE
		"Cannot throw \"{0}\" it does not inherit from \"java.lang.Throwable\""
		"JLS 11.2"

error		FIELD_BADACCESS
		"Cannot access fields on \"{0}\" that is not a class type"
		"JLS 15.11"

error		FIELD_UNKNOWN
		"Cannot find field \"{0}\""
		"JLS 15.11"

error		FIELD_PRIVATE
		"Cannot access private field \"{0}\""
		"JLS 15.11.1"

error		FIELD_STATICERR
		"Cannot access non-static field \"{0}\" from static nested class"
		"JLS 15.11"

error		FIELD_NOSTATIC
		"Cannot access non-static field from a static method"
		"JL1 8.4.3.2"

error		FIELD_NOT_DECLARED_YET
		"Cannot access field \"{0}\" that is not fully declared here"
		"JL1 8.5"

error		BAD_LOCAL_NOT_FINAL
		"Local variable \"{0}\" should be final to be used in inner class"
		"JL1 D.1.2"

error		BAD_SUPER_STATIC
		"Bad usage of super in a static context"
		"JLS 15.11.2"

error		EQUALITY_TYPE
		"Cannot compare \"{0}\" and \"{1}\""
		"JLS 15.21"

error		INCOMPATIBLE_MODIFIERS
		"Incompatible modifiers \"{0}\" specified"
		"JLS 8.1.1, 8.3.1, 8.4.3, 8.8.3"

error           FINAL_IN_LOOP 
                "Try to assign a blank final variable \"{0}\" in a loop. The initialization must occur exactly once"
                "JLS 4.5.4, 16, 16.2.9"

// WARNINGS

notice		FIELD_RENAME_SUPER
		"Field \"{0}\" is already declared in a super class and is visible here"
		"KOPI"

notice		METHOD_UNTHROWN_EXCEPTION
		"Declared exception \"{0}\" is never thrown"
		"KOPI"

notice		UNNECESSARY_CAST
		"Type \"{0}\" does not need to be casted into \"{1}\""
		"KOPI"

notice		UNUSED_CATCH_PARAMETER
		"Catch parameter \"{0}\" is not used"
		"KOPI"

notice		UNUSED_PARAMETER
		"Parameter \"{0}\" is not used"
		"KOPI"

info		PACKAGE_IS_MISSING
		"There is no package declaration for this compilation unit"
		"KOPI"

notice		SWITCH_NO_DEFAULT
		"There is no default label in switch statement"
		"KOPI"

notice		EMPTY_CATCH_BLOCK
		"Catch block is empty"
		"KOPI"

notice		PACKAGE_PROTECTED_ATTRIBUTE
		"Field \"{0}\" should be declared private or public"
		"KOPI"

notice		ASSIGNS_LOOP_VARIABLE
		"Assignment of loop variable \"{0}\" inside loop body"
		"KOPI"

info		UNINITIALIZED_FIELD
		"Field \"{0}\" may have not been explicitely initialized"
		"KOPI"

info		UNUSED_RETURN_VALUE_FROM_FUNCTION_CALL
		"The method \"{0}\" returns a value that is not used"
		"KOPI"

// UNUSED MESSAGES
error		FIELD_NOACCESS
		"Cannot access field \"{0}\" it is in an other package and not a class member"
		"JLS 15.11"

