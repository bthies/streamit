/*
 * Copyright 2003 by the Massachusetts Institute of Technology.
 *
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in
 * advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 * M.I.T. makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is"
 * without express or implied warranty.
 */

package streamit.frontend.tojava;

import streamit.frontend.nodes.*;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

/**
 * Traverse a front-end tree and produce Java code.  This uses {@link
 * streamit.frontend.nodes.FEVisitor} directly, without going through
 * an intermediate class such as <code>FEReplacer</code>.  Every
 * method actually returns a String.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: NodesToJava.java,v 1.125 2006-08-23 23:01:13 thies Exp $
 */
public class NodesToJava implements FEVisitor
{
    // Set inside visitStreamSpec
    private StreamSpec ss;

    // A string consisting of an even number of spaces.
    private String indent;

    // Allow tweaks when compiling for --library (from constructor)
    private boolean libraryFormat;

    // Optionally instrument output to count arith ops
    private boolean countops;

    // a generator for unique strings (from constructor)
    private TempVarGen varGen;

    // set inside visitStreamSpec if this class generated using static 
    //(a.k.a. global) keyword.
    private boolean global;

    public NodesToJava(boolean libraryFormat, boolean countops, TempVarGen varGen)
    {
        this.ss = null;
        this.indent = "";
        this.libraryFormat = libraryFormat;
        this.countops = countops;
        this.varGen = varGen;
        this.global = false;
    }

    // Add two spaces to the indent.
    private void addIndent() 
    {
        indent += "  ";
    }
    
    // Remove two spaces from the indent.
    private void unIndent()
    {
        indent = indent.substring(2);
    }

    // Determine if this is that top level of th program
    // There is no built-in marker for the top-level stream, but
    // we can recognize it as the only void -> void stream spec other than
    // a static (global).
    private boolean isTopLevelSpec(StreamSpec spec) {
        StreamType st = spec.getStreamType();
        return
            spec.getType() != StreamSpec.STREAM_GLOBAL &&
            st != null &&
            st.getIn() instanceof TypePrimitive &&
            ((TypePrimitive)st.getIn()).getType() == TypePrimitive.TYPE_VOID &&
            st.getOut() instanceof TypePrimitive &&
            ((TypePrimitive)st.getOut()).getType() == TypePrimitive.TYPE_VOID;
    }

    // Convert a Type to a String.  If visitors weren't so generally
    // useless for other operations involving Types, we'd use one here.
    public String convertType(Type type)
    {
        // This is So Wrong in the greater scheme of things.
        if (type instanceof TypeArray)
            {
                if (libraryFormat) {
                    // declare arrays like int[][] foo;
                    TypeArray array = (TypeArray)type;
                    String base = convertType(array.getBase());
                    return base + "[]";
                } else {
                    // declare arrays like int[10][10] foo;
                    return convertTypeFull(type);
                }
            }
        else if (type instanceof TypeStruct)
            {
                return ((TypeStruct)type).getName();
            }
        else if (type instanceof TypeStructRef)
            {
                return ((TypeStructRef)type).getName();
            }
        else if (type instanceof TypePrimitive)
            {
                switch (((TypePrimitive)type).getType())
                    {
                    case TypePrimitive.TYPE_BOOLEAN: return "boolean";
                    case TypePrimitive.TYPE_BIT: return "int";
                    case TypePrimitive.TYPE_INT: return "int";
                    case TypePrimitive.TYPE_FLOAT: return "float";
                    case TypePrimitive.TYPE_DOUBLE: return "double";
                    case TypePrimitive.TYPE_COMPLEX: return "Complex";
                    case TypePrimitive.TYPE_FLOAT2: return "float2";
                    case TypePrimitive.TYPE_FLOAT3: return "float3";
                    case TypePrimitive.TYPE_FLOAT4: return "float4";
                    case TypePrimitive.TYPE_VOID: return "void";
                    default: assert false : type; return null;
                    }
            }
        else if (type instanceof TypePortal)
            {
                return ((TypePortal)type).getName() + "Portal";
            }
        else
            {
                assert false : type;
                return null;
            }
    }

    public String convertTypeFull(Type type) {
        return convertTypeFull(type, true);
    }

    // Do the same conversion, but including array dimensions.
    public String convertTypeFull(Type type, boolean includePrimitive)
    {
        if (type instanceof TypeArray)
            {
                TypeArray array = (TypeArray)type;
                String output = "";
                // first get primitive type
                if (includePrimitive) {
                    Type primitive = array;
                    while (primitive instanceof TypeArray) {
                        primitive = ((TypeArray)primitive).getBase();
                    }
                    output = convertTypeFull(primitive);
                }
                return output +
                    "[" + (String)array.getLength().accept(this) + "]"
                    + convertTypeFull(array.getBase(), false);
        
            }
        if (includePrimitive) {
            return convertType(type);
        } else {
            return "";
        }
    }

    // Get a constructor for some type.
    public String makeConstructor(Type type)
    {
        if (type instanceof TypeArray)
            return "new " + convertTypeFull(type);
        else
            return "new " + convertTypeFull(type) + "()";
    }

    // Get a Java Class object corresponding to a type.
    public String typeToClass(Type t)
    {
        if (t instanceof TypePrimitive)
            {
                switch (((TypePrimitive)t).getType())
                    {
                    case TypePrimitive.TYPE_BOOLEAN:
                        return "Boolean.TYPE";
                    case TypePrimitive.TYPE_BIT:
                        return "Integer.TYPE";
                    case TypePrimitive.TYPE_INT:
                        return "Integer.TYPE";
                    case TypePrimitive.TYPE_FLOAT:
                        return "Float.TYPE";
                    case TypePrimitive.TYPE_DOUBLE:
                        return "Double.TYPE";
                    case TypePrimitive.TYPE_VOID:
                        return "Void.TYPE";
                    case TypePrimitive.TYPE_COMPLEX:
                        return "Complex.class";
                    default:
                        assert false : t;
                        return null;
                    }
            }
        else if (t instanceof TypeStruct)
            return ((TypeStruct)t).getName() + ".class";
        else if (t instanceof TypeArray)
            return "(" + makeConstructor(t) + ").getClass()";
        else
            {
                assert false : t;
                return null;
            }
    }

    // Helpers to get function names for stream types.
    public String pushFunction(StreamType st)
    {
        return annotatedFunction("outputChannel.push", st.getOut());
    }
    
    public String popFunction(StreamType st)
    {
        return annotatedFunction("inputChannel.pop", st.getIn());
    }
    
    public String peekFunction(StreamType st)
    {
        return annotatedFunction("inputChannel.peek", st.getIn());
    }
    
    private String annotatedFunction(String name, Type type)
    {
        String prefix = "", suffix = "";
        // Check for known suffixes:
        if (type instanceof TypePrimitive)
            {
                switch (((TypePrimitive)type).getType())
                    {
                    case TypePrimitive.TYPE_BOOLEAN:
                        suffix = "Boolean";
                        break;
                    case TypePrimitive.TYPE_BIT:
                        suffix = "Int";
                        break;
                    case TypePrimitive.TYPE_INT:
                        suffix = "Int";
                        break;
                    case TypePrimitive.TYPE_FLOAT:
                        suffix = "Float";
                        break;
                    case TypePrimitive.TYPE_DOUBLE:
                        suffix = "Double";
                        break;
                    case TypePrimitive.TYPE_COMPLEX:
                        if (name.startsWith("inputChannel"))
                            prefix  = "(Complex)";
                        break;
                    default:
                        assert false : type;
                    }
            }
        else if (name.startsWith("inputChannel"))
            {
                prefix = "(" + convertType(type) + ")";
            }
        return prefix + name + suffix;
    }

    // Return a representation of a list of Parameter objects.
    public String doParams(List params, String prefix)
    {
        String result = "(";
        boolean first = true;
        for (Iterator iter = params.iterator(); iter.hasNext(); )
            {
                Parameter param = (Parameter)iter.next();
                if (!first) result += ", ";
                if (prefix != null) result += prefix + " ";
                result += convertType(param.getType());
                result += " ";
                result += param.getName();
                first = false;
            }
        result += ")";
        return result;
    }

    // Return a representation of lhs = rhs, with no trailing semicolon.
    public String doAssignment(Expression lhs, Expression rhs,
                               SymbolTable symtab)
    {
        // If the left-hand-side is a complex variable, we need to
        // properly decompose the right-hand side.
        // We can use a null stream type here since the left-hand
        // side shouldn't contain pushes, pops, or peeks.
        GetExprType eType = new GetExprType(symtab, ss.getStreamType(),
                                            new java.util.HashMap(),
                                            new java.util.HashMap());
        Type lhsType = (Type)lhs.accept(eType);
        if (lhsType.isComplex())
            {
                Expression real = new ExprField(lhs.getContext(), lhs, "real");
                Expression imag = new ExprField(lhs.getContext(), lhs, "imag");
                // If the right hand side is complex too (at this point
                // just test the run-time type of the expression), then we
                // should do field copies; otherwise we only have a real part.
                if (rhs instanceof ExprComplex)
                    {
                        ExprComplex cplx = (ExprComplex)rhs;
                        return real.accept(this) + " = " +
                            cplx.getReal().accept(this) + ";\n" +
                            imag.accept(this) + " = " +
                            cplx.getImag().accept(this);
                    }
                else
                    return real.accept(this) + " = " +
                        rhs.accept(this) + ";\n" +
                        imag.accept(this) + " = 0.0";
            }
        else if (lhsType.isComposite()) 
            {
                Expression x = new ExprField(lhs.getContext(), lhs, "x");
                Expression y = new ExprField(lhs.getContext(), lhs, "y");
                Expression z = new ExprField(lhs.getContext(), lhs, "z");
                Expression w = new ExprField(lhs.getContext(), lhs, "w");
                // If the right hand side is composite too, then we 
                // should do field copies.
                if (rhs instanceof ExprComposite) {
                    ExprComposite cpst = (ExprComposite)rhs;
                    String result = x.accept(this) + " = " +
                        cpst.getX().accept(this) + ";\n" +
                        y.accept(this) + " = " +
                        cpst.getY().accept(this); 
                    Expression z1 = cpst.getZ();
                    if (z1 != null) result +=  ";\n" + z.accept(this) + " = " +
                                        z1.accept(this); 
                    Expression w1 = cpst.getW();
                    if (w1 != null) result +=  ";\n" + w.accept(this) + " = " +
                                        w1.accept(this); 
                    return result;
                } 
                else
                    throw new RuntimeException("type not compatible");
            }
        else
            {
                // Might want to special-case structures and arrays;
                // ignore for now.
                return lhs.accept(this) + " = " + rhs.accept(this);
            }
    }

    public Object visitExprArray(ExprArray exp)
    {
        String result = "";
        result += (String)exp.getBase().accept(this);
        result += "[";
        result += (String)exp.getOffset().accept(this);
        result += "]";
        return result;
    }
    
    public Object visitExprArrayInit(ExprArrayInit exp)
    {
        StringBuffer sb = new StringBuffer();
        sb.append("{");

        List elems = exp.getElements();
        for (int i=0; i<elems.size(); i++) {
            sb.append((String)((Expression)elems.get(i)).accept(this));
            if (i!=elems.size()-1) {
                sb.append(",");
            }
            // leave blank line for multi-dim arrays
            if (exp.getDims()>1) {
                sb.append("\n");
            }
        }
    
        sb.append("}");

        return sb.toString();
    }
    
    public Object visitExprBinary(ExprBinary exp)
    {
        String result;
        String op = null;
        result = "(";
        result += (String)exp.getLeft().accept(this);
        switch (exp.getOp())
            {
            case ExprBinary.BINOP_ADD: op = "+"; break;
            case ExprBinary.BINOP_SUB: op = "-"; break;
            case ExprBinary.BINOP_MUL: op = "*"; break;
            case ExprBinary.BINOP_DIV: op = "/"; break;
            case ExprBinary.BINOP_MOD: op = "%"; break;
            case ExprBinary.BINOP_AND: op = "&&"; break;
            case ExprBinary.BINOP_OR:  op = "||"; break;
            case ExprBinary.BINOP_EQ:  op = "=="; break;
            case ExprBinary.BINOP_NEQ: op = "!="; break;
            case ExprBinary.BINOP_LT:  op = "<"; break;
            case ExprBinary.BINOP_LE:  op = "<="; break;
            case ExprBinary.BINOP_GT:  op = ">"; break;
            case ExprBinary.BINOP_GE:  op = ">="; break;
            case ExprBinary.BINOP_BAND:op = "&"; break;
            case ExprBinary.BINOP_BOR: op = "|"; break;
            case ExprBinary.BINOP_BXOR:op = "^"; break;
            case ExprBinary.BINOP_LSHIFT: op = "<<"; break;
            case ExprBinary.BINOP_RSHIFT: op = ">>"; break;
            default: assert false : exp; break;
            }
        result += " " + op + " ";
        result += (String)exp.getRight().accept(this);
        result += ")";

        // if profiling on, wrap this with instrumentation call
        if (libraryFormat && countops) {
            return wrapWithProfiling(result, binaryOpToProfilerId(exp.getOp()));
        }

        return result;
    }

    /**
     * Provides a mapping from binary operations in ExprBinary to the
     * field name in the Java library profiler
     * (streamit.library.Profiler).
     */
    private String binaryOpToProfilerId(int binop) {
        switch (binop) 
            {
            case ExprBinary.BINOP_ADD:    return "BINOP_ADD"; 
            case ExprBinary.BINOP_SUB:    return "BINOP_SUB"; 
            case ExprBinary.BINOP_MUL:    return "BINOP_MUL"; 
            case ExprBinary.BINOP_DIV:    return "BINOP_DIV"; 
            case ExprBinary.BINOP_MOD:    return "BINOP_MOD"; 
            case ExprBinary.BINOP_AND:    return "BINOP_AND"; 
            case ExprBinary.BINOP_OR:     return "BINOP_OR"; 
            case ExprBinary.BINOP_EQ:     return "BINOP_EQ"; 
            case ExprBinary.BINOP_NEQ:    return "BINOP_NEQ"; 
            case ExprBinary.BINOP_LT:     return "BINOP_LT"; 
            case ExprBinary.BINOP_LE:     return "BINOP_LE"; 
            case ExprBinary.BINOP_GT:     return "BINOP_GT"; 
            case ExprBinary.BINOP_GE:     return "BINOP_GE"; 
            case ExprBinary.BINOP_BAND:   return "BINOP_BAND";
            case ExprBinary.BINOP_BOR:    return "BINOP_BOR"; 
            case ExprBinary.BINOP_BXOR:   return "BINOP_BXOR"; 
            case ExprBinary.BINOP_LSHIFT: return "BINOP_LSHIFT"; 
            case ExprBinary.BINOP_RSHIFT: return "BINOP_RSHIFT"; 
            default: assert false : binop;
            }
        // stupid compiler
        return null;
    }

    /**
     * Provides a mapping from unary operations in ExprUnary to the
     * field name in the Java library profiler
     * (streamit.library.Profiler).
     */
    private String unaryOpToProfilerId(int unop) {
        switch (unop) 
            {
            case ExprUnary.UNOP_NOT:        return "UNOP_NOT";
            case ExprUnary.UNOP_NEG:        return "UNOP_NEG";
            case ExprUnary.UNOP_PREINC:     return "UNOP_PREINC";
            case ExprUnary.UNOP_POSTINC:    return "UNOP_POSTINC";
            case ExprUnary.UNOP_PREDEC:     return "UNOP_PREDEC";
            case ExprUnary.UNOP_POSTDEC:    return "UNOP_POSTDEC";
            case ExprUnary.UNOP_COMPLEMENT: return "UNOP_COMPLEMENT";
            default: assert false : unop;
            }
        // stupid compiler
        return null;
    }

    /**
     * Provides a mapping from java.Math functions to the field name
     * in the Java library profiler (streamit.library.Profiler).
     */
    private String funcToProfilerId(String ident) {
        // hope that we have also defined this function in the Java library
        return "FUNC_" + ident.toUpperCase();
    }

    /**
     * Given the generated code for an expression (expr) and the
     * operation being performed (op), wrap that expression in a call
     * to an instrumentation procedure in the library.
     */
    private String wrapWithProfiling(String exp, String profilerId) {
        // generate a unique ID for this arith op
        int id = (MAX_PROFILE_ID++);
        // call to function in Java library (it returns <exp>)
        return "Profiler.registerOp(Profiler." + profilerId + ", " + id + ", \"" + exp.replace('\"', '\'') + "\", " + exp + ")";
    }
    // counter for wrapWithProfiling
    private int MAX_PROFILE_ID = 0;

    public Object visitExprComplex(ExprComplex exp)
    {
        // We should never see one of these at this point.
        assert false : exp;
        // If we do, print something vaguely intelligent:
        String r = "";
        String i = "";
        if (exp.getReal() != null) r = (String)exp.getReal().accept(this);
        if (exp.getImag() != null) i = (String)exp.getImag().accept(this);
        return "/* (" + r + ")+i(" + i + ") */";
    }

    public Object visitExprComposite(ExprComposite exp)
    {
        // We should never see one of these at this point.
        //assert false : exp;
        // If we do, print something vaguely intelligent:
        return "/* " + exp.toString() + " */";
    }

    public Object visitExprConstBoolean(ExprConstBoolean exp)
    {
        if (exp.getVal())
            return "true";
        else
            return "false";
    }

    public Object visitExprConstChar(ExprConstChar exp)
    {
        return "'" + exp.getVal() + "'";
    }

    public Object visitExprConstFloat(ExprConstFloat exp)
    {
        return Double.toString(exp.getVal()) + "f";
    }

    public Object visitExprConstInt(ExprConstInt exp)
    {
        return Integer.toString(exp.getVal());
    }
    
    public Object visitExprConstStr(ExprConstStr exp)
    {
        return exp.getVal();
    }

    public Object visitExprDynamicToken(ExprDynamicToken exp) {
        return "Rate.DYNAMIC_RATE";
    }

    public Object visitExprField(ExprField exp)
    {
        String result = "";
        result += (String)exp.getLeft().accept(this);
        result += ".";
        result += (String)exp.getName();
        return result;
    }

    public Object visitExprFunCall(ExprFunCall exp)
    {
        String result;
        String name = exp.getName();
        boolean mathFunction = false;
        // Local function?
        if (ss.getFuncNamed(name) != null) {
            result = name + "(";
        }
        // look for print and println statements; assume everything
        // else is a math function
        else if (name.equals("ArrayMemoizer.initArray")) {
            result = "ArrayMemoizer.initArray(";
        } else if (name.equals("print")) {
            result = "System.out.print(";
        } else if (name.equals("println")) {
            result = "System.out.println(";
        } else if (name.equals("super")) {
            result = "super(";
        } else if (name.equals("setDelay")) {
            result = "setDelay(";
        } else if (name.startsWith("enqueue")) {
            result = name + "(";
        } else if (name.equals("contextSwitch")) {
            result = name + "(";
        } else if (name.startsWith("init_array")) {
            // take care of, e.g., init_array_1D_float(String filename, int size)
            
            // Generate a function call to load file from disk.
            result = name + "(";
            
            /* The code below will insert the static array initializer
             * in the java output.  For now we do this in the compiler
             * instead to get benefits of constant prop in the
             * filename and size of the initializer.
             
             } else {
             result = makeArrayInit(exp);
             return result;
             }
            */

        } else {
            // Math.sqrt will return a double, but we're only supporting
            // float's now, so add a cast to float.  Not sure if this is
            // the right thing to do for all math functions in all cases?
            result = "(float)Math." + name + "(";
            mathFunction = true;
        }
        boolean first = true;
        for (Iterator iter = exp.getParams().iterator(); iter.hasNext(); )
            {
                Expression param = (Expression)iter.next();
                if (!first) result += ", ";
                first = false;
                result += (String)param.accept(this);
            }
        result += ")";

        // if we called a math function and profiling is on, wrap with
        // call to profiler
        if (libraryFormat && countops && mathFunction) {
            result = wrapWithProfiling(result, funcToProfilerId(name));
        }

        return result;
    }

    public Object visitExprHelperCall(ExprHelperCall exp)
    {
        String result = exp.getHelperPackage() + '.' + exp.getName() + '(';
        boolean first = true;
        for (Iterator iter = exp.getParams().iterator(); iter.hasNext(); )
            {
                Expression param = (Expression)iter.next();
                if (!first) result += ", ";
                first = false;
                result += (String)param.accept(this);
            }
        result += ")";
        return result;
    }

    /**
     * Given a function call of the following form:
     *
     *   init_array_1D_float(String filename, int size)
     *   init_array_1D_int(String filename, int size)
     *
     * generates a static array initializer by loading the initial
     * values from the file.
     */
    private String makeArrayInit(ExprFunCall exp) {
        String funcName = exp.getName();
        String filename = null;
        int size = 0;
        StringBuffer result = new StringBuffer();

        // GET PARAMS -------

        // first param should be string
        if (exp.getParams().get(0) instanceof ExprConstStr) {
            // for some reason the string literal has quotes on either
            // side
            String withQuotes = ((ExprConstStr)exp.getParams().get(0)).getVal();
            filename = withQuotes.substring(1, withQuotes.length()-1);
        } else {
            System.err.println("Error: expected first argument to " + funcName + " to be a String (the filename)");
            System.exit(1);
        }

        // second param should be an int
        if (exp.getParams().get(1) instanceof ExprConstInt) {
            size = ((ExprConstInt)exp.getParams().get(1)).getVal();
        } else {
            System.err.println("Error: expected second argument to " + funcName + " to be an integer (the size)");
            System.exit(1);
        }

        // LOAD ARRAY -------

        // load int array values
        if (funcName.equals("init_array_1D_int")) {
            int[] array = streamit.misc.Misc.init_array_1D_int(filename, size);
            // build result
            result.append("{");
            for (int i=0; i<size; i++) {
                if (i!=0) {
                    result.append(",\n");
                    result.append(indent);
                }
                result.append(array[i]);
            }
            result.append("}");
            return result.toString();
        }

        // load float array values
        if (funcName.equals("init_array_1D_float")) {
            float[] array = streamit.misc.Misc.init_array_1D_float(filename, size);
            // build result
            result.append("{");
            for (int i=0; i<size; i++) {
                if (i!=0) {
                    result.append(",\n");
                    result.append(indent);
                }
                result.append(array[i]);
                result.append("f"); // float not double
            }
            result.append("}");
            return result.toString();
        }

        // unrecognized function type
        System.err.println("Unrecognized array initializer: " + funcName);
        System.exit(1);
        return null;
    }

    public Object visitExprPeek(ExprPeek exp)
    {
        String result = (String)exp.getExpr().accept(this);
        return peekFunction(ss.getStreamType()) + "(" + result + ")";
    }
    
    public Object visitExprPop(ExprPop exp)
    {
        return popFunction(ss.getStreamType()) + "()";
    }

    public Object visitExprRange(ExprRange exp) {
        String min = (String)exp.getMin().accept(this);
        String ave = (String)exp.getAve().accept(this);
        String max = (String)exp.getMax().accept(this);
        return "new Rate(" + min + ", " + ave + ", " + max + ")";
    }

    public Object visitExprTernary(ExprTernary exp)
    {
        String a = (String)exp.getA().accept(this);
        String b = (String)exp.getB().accept(this);
        String c = (String)exp.getC().accept(this);
        switch (exp.getOp())
            {
            case ExprTernary.TEROP_COND:
                return "(" + a + " ? " + b + " : " + c + ")";
            default:
                assert false : exp;
                return null;
            }
    }

    public Object visitExprTypeCast(ExprTypeCast exp)
    {
        return "((" + convertType(exp.getType()) + ")(" +
            (String)exp.getExpr().accept(this) + "))";
    }

    public Object visitExprUnary(ExprUnary exp)
    {
        String child = (String)exp.getExpr().accept(this);
        String result = null;

        switch(exp.getOp())
            {
            case ExprUnary.UNOP_NOT:        result = "!" + child; break;
            case ExprUnary.UNOP_NEG:        result = "-" + child; break;
            case ExprUnary.UNOP_PREINC:     result = "++" + child; break;
            case ExprUnary.UNOP_POSTINC:    result = child + "++"; break;
            case ExprUnary.UNOP_PREDEC:     result = "--" + child; break;
            case ExprUnary.UNOP_POSTDEC:    result = child + "--"; break;
            case ExprUnary.UNOP_COMPLEMENT: result = "~" + child; break;
            default: assert false : exp;    result = null; break;
            }

        // if profiling on, wrap this with instrumentation call
        if (libraryFormat && countops) {
            result = wrapWithProfiling(result, unaryOpToProfilerId(exp.getOp()));
        }

        return result;
    }

    public Object visitExprVar(ExprVar exp)
    {
        return exp.getName();
    }

    public Object visitFieldDecl(FieldDecl field)
    {
        // Assume all of the fields have the same type.
        String result = indent;
        if (global) {
            if (libraryFormat) result += "public "; else result += "public static ";
        }
        result += convertType(field.getType(0)) + " ";
        for (int i = 0; i < field.getNumFields(); i++)
            {
                if (i > 0) result += ", ";
                result += field.getName(i);
                if (field.getInit(i) != null)
                    result += " = " + (String)field.getInit(i).accept(this);
            }
        result += ";";
        if (field.getContext() != null)
            result += " // " + field.getContext();
        result += "\n";
        return result;
    }

    public Object visitFunction(Function func)
    {
        String result = indent + "public ";

        if (ss == null) { // A helper function
            result += "static ";
            if (func.getCls() == Function.FUNC_NATIVE) result += "native ";
            result += convertType(func.getReturnType()) + " ";
            result += func.getName();
            result += doParams(func.getParams(), null);
            if (func.getCls() == Function.FUNC_NATIVE) 
                result += ";\n"; 
            else
                result += " " + (String)func.getBody().accept(this) + "\n";
            return result;
        }

        if (!func.getName().equals(ss.getName()))
            result += convertType(func.getReturnType()) + " ";
        result += func.getName();
        String prefix = null;

        // save profiling
        boolean oldCountOps = countops;
        if (func.getCls() == Function.FUNC_INIT) {
            // parameters should be final
            prefix = "final";
            // turn profiling off for init function
            countops = false;
        }
        result += doParams(func.getParams(), prefix) + " ";
        result += (String)func.getBody().accept(this);
        result += "\n";

        // restore profiling
        countops = oldCountOps;

        return result;
    }
    
    public Object visitFuncWork(FuncWork func)
    {
        // Nothing special here; we get to ignore the I/O rates.
        return visitFunction(func);
    }

    public Object visitProgram(Program prog) {
        // Nothing special here either. Just accumulate all of the
        // structures and streams.
        String result = "";

        for (Iterator iter = prog.getStructs().iterator(); iter.hasNext();) {
            TypeStruct struct = (TypeStruct) iter.next();

            if (struct.getName().equals("String"))
                continue;
            if (libraryFormat && struct.getName().equals("float2"))
                continue;
            if (libraryFormat && struct.getName().equals("float3"))
                continue;
            if (libraryFormat && struct.getName().equals("float4"))
                continue;

            result += indent + "class " + struct.getName()
                + " extends Structure implements Serializable {\n";
            addIndent();
            for (int i = 0; i < struct.getNumFields(); i++) {
                String name = struct.getField(i);
                Type type = struct.getType(name);
                result += indent + convertType(type) + " " + name + ";\n";
            }
            unIndent();
            result += indent + "}\n";
        }

        for (Iterator iter = prog.getHelpers().iterator(); iter.hasNext();) {
            TypeHelper th = (TypeHelper) iter.next();
            result += visitTypeHelper(th);
        }

        if (!libraryFormat) {
            result += indent + "class StreamItVectorLib {\n";
            addIndent();
            result += indent + "public static native float2 add2(float2 a, float2 b);\n";
            result += indent + "public static native float3 add3(float3 a, float3 b);\n";
            result += indent + "public static native float4 add4(float4 a, float4 b);\n";

            result += indent + "public static native float2 sub2(float2 a, float2 b);\n";
            result += indent + "public static native float3 sub3(float3 a, float3 b);\n";
            result += indent + "public static native float4 sub4(float4 a, float4 b);\n";

            result += indent + "public static native float2 mul2(float2 a, float2 b);\n";
            result += indent + "public static native float3 mul3(float3 a, float3 b);\n";
            result += indent + "public static native float4 mul4(float4 a, float4 b);\n";

            result += indent + "public static native float2 div2(float2 a, float2 b);\n";
            result += indent + "public static native float3 div3(float3 a, float3 b);\n";
            result += indent + "public static native float4 div4(float4 a, float4 b);\n";

            result += indent + "public static native float2 addScalar2(float2 a, float b);\n";
            result += indent + "public static native float3 addScalar3(float3 a, float b);\n";
            result += indent + "public static native float4 addScalar4(float4 a, float b);\n";

            result += indent + "public static native float2 subScalar2(float2 a, float b);\n";
            result += indent + "public static native float3 subScalar3(float3 a, float b);\n";
            result += indent + "public static native float4 subScalar4(float4 a, float b);\n";

            result += indent + "public static native float2 scale2(float2 a, float b);\n";
            result += indent + "public static native float3 scale3(float3 a, float b);\n";
            result += indent + "public static native float4 scale4(float4 a, float b);\n";

            result += indent + "public static native float2 scaleInv2(float2 a, float b);\n";
            result += indent + "public static native float3 scaleInv3(float3 a, float b);\n";
            result += indent + "public static native float4 scaleInv4(float4 a, float b);\n";

            result += indent + "public static native float sqrtDist2(float2 a, float2 b);\n";
            result += indent + "public static native float sqrtDist3(float3 a, float3 b);\n";
            result += indent + "public static native float sqrtDist4(float4 a, float4 b);\n";

            result += indent + "public static native float dot3(float3 a, float3 b);\n";
            result += indent + "public static native float3 cross3(float3 a, float3 b);\n";

            result += indent + "public static native float2 max2(float2 a, float2 b);\n";
            result += indent + "public static native float3 max3(float3 a, float3 b);\n";

            result += indent + "public static native float2 min2(float2 a, float2 b);\n";
            result += indent + "public static native float3 min3(float3 a, float3 b);\n";

            result += indent + "public static native float2 neg2(float2 a);\n";
            result += indent + "public static native float3 neg3(float3 a);\n";
            result += indent + "public static native float4 neg4(float4 a);\n";

            result += indent + "public static native float2 floor2(float2 a);\n";
            result += indent + "public static native float3 floor3(float3 a);\n";
            result += indent + "public static native float4 floor4(float4 a);\n";

            result += indent + "public static native float2 normalize2(float2 a);\n";
            result += indent + "public static native float3 normalize3(float3 a);\n";
            result += indent + "public static native float4 normalize4(float4 a);\n";

            result += indent + "public static native boolean greaterThan3(float3 a, float3 b);\n";
            result += indent + "public static native boolean lessThan3(float3 a, float3 b);\n";
            result += indent + "public static native boolean equals3(float3 a, float3 b);\n";

            unIndent();
            result += indent + "}\n";
        }

        StreamSpec main = null;
        for (Iterator iter = prog.getStreams().iterator(); iter.hasNext();) {
            StreamSpec spec = ((StreamSpec) iter.next());

            if (isTopLevelSpec(spec)) {
                assert main == null : "Found more than one top-level stream";
                main = spec;
                iter.remove();
            } else {
                result += spec.accept(this);
            }
        }
        assert main != null : "Did not find any top-level stream";
        result += main.accept(this);
        return result;
    }

    public Object visitSCAnon(SCAnon creator) {
        assert false : "NodesToJava run before NameAnonymousStreams";
        return creator.getSpec().accept(this);
    }
    
    public Object visitSCSimple(SCSimple creator)
    {
        // Hacked to make FileReader/Writer<bit> work
        boolean hardcoded_BitFileFlag = (creator.getName().equals("FileReader") ||
                                         creator.getName().equals("FileWriter"));
        
        String result;
        if (libraryFormat)
            {
                // Magic for builtins.
                if (creator.getName().equals("Identity") ||
                    creator.getName().equals("FileReader") ||
                    creator.getName().equals("FileWriter") ||
                    creator.getName().equals("ImageDisplay")) {
                    result = "new " + creator.getName() + "(";
                
                }
                else
                    result = creator.getName() + ".__construct(";
            }
        else
            result = "new " + creator.getName() + "(";
        boolean first = true;
        for (Iterator iter = creator.getParams().iterator(); iter.hasNext(); )
            {
                Expression param = (Expression)iter.next();
                if (!first) result += ", ";
                result += (String)param.accept(this);
                first = false;
            }
        for (Iterator iter = creator.getTypes().iterator(); iter.hasNext(); )
            {
                Type type = (Type)iter.next();
                if (!first) result += ", ";
                // Hacked to make FileReader/Writer<bit> work
                if ((type instanceof TypePrimitive) && 
                    (((TypePrimitive) type).getType() == TypePrimitive.TYPE_BIT) && 
                    hardcoded_BitFileFlag) {
                    result += "Bit.TYPE, Bit.TREAT_AS_BITS"; 
                }
                else
                    result += typeToClass(type);
                first = false;
            }
        result += ")";
        return result;
    }

    public Object visitSJDuplicate(SJDuplicate sj)
    {
        return "DUPLICATE()";
    }

    public Object visitSJRoundRobin(SJRoundRobin sj)
    {
        return "ROUND_ROBIN(" + (String)sj.getWeight().accept(this) + ")";
    }

    public Object visitSJWeightedRR(SJWeightedRR sj)
    {
        String result = "WEIGHTED_ROUND_ROBIN(";
        boolean first = true;
        for (Iterator iter = sj.getWeights().iterator(); iter.hasNext(); )
            {
                Expression weight = (Expression)iter.next();
                if (!first) result += ", ";
                result += (String)weight.accept(this);
                first = false;
            }
        result += ")";
        return result;
    }

    private Object doStreamCreator(String how, StreamCreator sc)
    {
        // If the stream creator involves registering with a portal,
        // we need a temporary variable.
        List portals = sc.getPortals();
        if (portals.isEmpty()) {
            // basic behavior: put expression in-line.
            //System.err.println("basic \"" + ((SCSimple)sc).getName() + "\"");
            return how + "(" + (String)sc.accept(this) + ")";
        }
        // has portals:
        String tempVar = varGen.nextVar();
        // Need run-time type of the creator.  Assert that only
        // named streams can be added to portals.
        SCSimple scsimple = (SCSimple)sc;
        String result = scsimple.getName() + " " + tempVar + " = " +
            (String)sc.accept(this);
        result += ";\n" + indent + how + "(" + tempVar + ")";
        for (Iterator iter = portals.iterator(); iter.hasNext(); )
            {
                Expression portal = (Expression)iter.next();
                result += ";\n" + indent + (String)portal.accept(this) +
                    ".regReceiver(" + tempVar + ")";
            }
        return result;
    }
    
    public Object visitStmtAdd(StmtAdd stmt)
    {
        return doStreamCreator("add", stmt.getCreator());
    }
    
    public Object visitStmtAssign(StmtAssign stmt)
    {
        String op;
        switch(stmt.getOp())
            {
            case ExprBinary.BINOP_ADD: op = " += "; break;
            case ExprBinary.BINOP_SUB: op = " -= "; break;
            case ExprBinary.BINOP_MUL: op = " *= "; break;
            case ExprBinary.BINOP_DIV: op = " /= "; break;
            case ExprBinary.BINOP_LSHIFT: op = " <<= "; break;
            case ExprBinary.BINOP_RSHIFT: op = " >>= "; break;
            case 0: op = " = "; break;
            default: assert false: stmt; op = " = "; break;
            }
        String lhs = (String)stmt.getLHS().accept(this);
        String rhs = (String)stmt.getRHS().accept(this);

        // if profiling on and we are not just assigning, wrap rhs
        // with instrumentation call
        if (libraryFormat && countops && stmt.getOp()!=0) {
            rhs = wrapWithProfiling(rhs, binaryOpToProfilerId(stmt.getOp()));
        }

        // Assume both sides are the right type.
        return lhs + op + rhs;
    }

    public Object visitStmtBlock(StmtBlock stmt)
    {
        // Put context label at the start of the block, too.
        String result = "{";
        if (stmt.getContext() != null)
            result += " // " + stmt.getContext();
        result += "\n";
        addIndent();
        for (Iterator iter = stmt.getStmts().iterator(); iter.hasNext(); )
            {
                Statement s = (Statement)iter.next();
                String line = indent;
                line += (String)s.accept(this);
                if (!(s instanceof StmtIfThen)) {
                    line += ";";
                }
                if (s.getContext() != null)
                    line += " // " + s.getContext();
                line += "\n";
                result += line;
            }
        unIndent();
        result += indent + "}";
        return result;
    }

    public Object visitStmtBody(StmtBody stmt)
    {
        return doStreamCreator("setBody", stmt.getCreator());
    }
    
    public Object visitStmtBreak(StmtBreak stmt)
    {
        return "break";
    }
    
    public Object visitStmtContinue(StmtContinue stmt)
    {
        return "continue";
    }

    public Object visitStmtDoWhile(StmtDoWhile stmt)
    {
        String result = "do ";
        result += (String)stmt.getBody().accept(this);
        result += "while (" + (String)stmt.getCond().accept(this) + ")";
        return result;
    }

    public Object visitStmtEmpty(StmtEmpty stmt)
    {
        return "";
    }

    public Object visitStmtEnqueue(StmtEnqueue stmt)
    {
        // Errk: this doesn't become nice Java code.
        return "/* enqueue(" + (String)stmt.getValue().accept(this) +
            ") */";
    }
    
    public Object visitStmtExpr(StmtExpr stmt)
    {
        String result = (String)stmt.getExpression().accept(this);
        // Gross hack to strip out leading class casts,
        // since they'll illegal (JLS 14.8).
        if (result.charAt(0) == '(' &&
            Character.isUpperCase(result.charAt(1)))
            result = result.substring(result.indexOf(')') + 1);
        return result;
    }

    public Object visitStmtFor(StmtFor stmt)
    {
        String result = "for (";
        if (stmt.getInit() != null)
            result += (String)stmt.getInit().accept(this);
        result += "; ";
        if (stmt.getCond() != null)
            result += (String)stmt.getCond().accept(this);
        result += "; ";
        if (stmt.getIncr() != null)
            result += (String)stmt.getIncr().accept(this);
        result += ") ";
        result += (String)stmt.getBody().accept(this);
        return result;
    }

    public Object visitStmtIfThen(StmtIfThen stmt)
    {
        // must have an if part...
        assert stmt.getCond() != null;
        String result = "if (" + (String)stmt.getCond().accept(this) + ") ";
        result += (String)stmt.getCons().accept(this);
        if (stmt.getAlt() != null)
            result += " else " + (String)stmt.getAlt().accept(this);
        return result;
    }

    public Object visitStmtJoin(StmtJoin stmt)
    {
        assert stmt.getJoiner() != null;
        return "setJoiner(" + (String)stmt.getJoiner().accept(this) + ")";
    }
    
    public Object visitStmtLoop(StmtLoop stmt)
    {
        assert stmt.getCreator() != null;
        return doStreamCreator("setLoop", stmt.getCreator());
    }

    public Object visitStmtPush(StmtPush stmt)
    {
        return pushFunction(ss.getStreamType()) + "(" +
            (String)stmt.getValue().accept(this) + ")";
    }

    public Object visitStmtReturn(StmtReturn stmt)
    {
        if (stmt.getValue() == null) return "return";
        return "return " + (String)stmt.getValue().accept(this);
    }

    public Object visitStmtSendMessage(StmtSendMessage stmt)
    {
        String receiver = (String)stmt.getReceiver().accept(this);
        String result = "";

        // Issue one of the latency-setting statements.
        if (stmt.getMinLatency() == null)
            {
                if (stmt.getMaxLatency() == null)
                    result += receiver + ".setAnyLatency()";
                else
                    result += receiver + ".setMaxLatency(" +
                        (String)stmt.getMaxLatency().accept(this) + ")";
            }
        else
            {
                // Hmm, don't have an SIRLatency for only minimum latency.
                // Wing it.
                Expression max = stmt.getMaxLatency();
                if (max == null)
                    max = new ExprBinary(null, ExprBinary.BINOP_MUL,
                                         stmt.getMinLatency(),
                                         new ExprConstInt(null, 100));
                result += receiver + ".setLatency(" +
                    (String)stmt.getMinLatency().accept(this) + ", " +
                    (String)max.accept(this) + ")";
            }
        result += ";\n";
        if (libraryFormat) {
            result += indent + receiver + ".enqueueMessage(this, \""
                + stmt.getName() + "\", new Object[] {";
            boolean first = true;
            for (Iterator iter = stmt.getParams().iterator(); iter.hasNext();) {
                Expression param = (Expression) iter.next();
                if (!first) { result += ", "; }
                first = false;
                // wrapInObject will take the primitive type output here
                // and wrap it in an object for the sake of reflection
                result += receiver + ".wrapInObject("
                    + (String) param.accept(this) + ")";
            }
            result += "})";
        } else {
            // not library format: don't package parameters...
            result += indent + receiver + "." + stmt.getName() + "(";
            boolean first = true;
            for (Iterator iter = stmt.getParams().iterator(); iter.hasNext();) {
                Expression param = (Expression) iter.next();
                if (!first) { result += ", "; }
                first = false;
                result += (String) param.accept(this);
            }
            result += ")";
        }
        return result;
    }

    public Object visitStmtHelperCall(StmtHelperCall stmt) 
    {
        String result = stmt.getHelperPackage() + '.' + stmt.getName() + '(';
        boolean first = true;
        for (Iterator iter = stmt.getParams().iterator(); iter.hasNext(); ) {
            Expression param = (Expression)iter.next();
            if (!first) result += ", ";
            first = false;
            result += (String)param.accept(this);
        }
        result += ")";
        return result;
    }

    public Object visitStmtSplit(StmtSplit stmt)
    {
        assert stmt.getSplitter() != null;
        return "setSplitter(" + (String)stmt.getSplitter().accept(this) + ")";
    }

    public Object visitStmtVarDecl(StmtVarDecl stmt)
    {
        String result = "";
        // Hack: if the first variable name begins with "_final_", the
        // variable declaration should be final.
        if (stmt.getName(0).startsWith("_final_"))
            result += "final ";
        result += convertType(stmt.getType(0)) + " ";
        for (int i = 0; i < stmt.getNumVars(); i++)
            {
                if (i > 0)
                    result += ", ";
                result += stmt.getName(i);
                if (stmt.getInit(i) != null)
                    result += " = " + (String)stmt.getInit(i).accept(this);
            }
        return result;
    }

    public Object visitStmtWhile(StmtWhile stmt)
    {
        assert stmt.getCond() != null;
        assert stmt.getBody() != null;
        return "while (" + (String)stmt.getCond().accept(this) +
            ") " + (String)stmt.getBody().accept(this);
    }

    /**
     * For a non-anonymous StreamSpec, check to see if it has any
     * message handlers.  If it does, then generate a Java interface
     * containing the handlers named (StreamName)Interface, and
     * a portal class named (StreamName)Portal.
     */
    private String maybeGeneratePortal(StreamSpec spec)
    {
        List handlers = new java.util.ArrayList();
        for (Iterator iter = spec.getFuncs().iterator(); iter.hasNext(); )
            {
                Function func = (Function)iter.next();
                if (func.getCls() == Function.FUNC_HANDLER)
                    handlers.add(func);
            }
        if (handlers.isEmpty())
            return null;
        
        // Okay.  Assemble the interface:
        StringBuffer result = new StringBuffer();
        result.append(indent + "interface " + spec.getName() +
                      "Interface {\n");
        addIndent();
        for (Iterator iter = handlers.iterator(); iter.hasNext(); )
            {
                Function func = (Function)iter.next();
                result.append(indent + "public ");
                result.append(convertType(func.getReturnType()) + " ");
                result.append(func.getName());
                result.append(doParams(func.getParams(), null));
                result.append(";\n");
            }
        unIndent();
        result.append(indent + "}\n");
        
        // Assemble the portal:
        result.append(indent + "class " + spec.getName() +
                      "Portal extends Portal implements " + spec.getName() +
                      "Interface {\n");
        addIndent();
        for (Iterator iter = handlers.iterator(); iter.hasNext(); )
            {
                Function func = (Function)iter.next();
                result.append(indent + "public ");
                result.append(convertType(func.getReturnType()) + " ");
                result.append(func.getName());
                result.append(doParams(func.getParams(), null));
                result.append(" { }\n");
            }
        unIndent();
        result.append(indent + "}\n");

        return result.toString();
    }

    /**
     * For a non-anonymous StreamSpec in the library path, generate
     * extra functions we need to construct the object.  In the
     * compiler path, generate an empty constructor.
     */
    private String maybeGenerateConstruct(StreamSpec spec)
    {
        StringBuffer result = new StringBuffer();
        
        // The StreamSpec at this point has no parameters; we need to
        // find the parameters of the init function.
        Function init = spec.getInitFunc();
        // (ASSERT: init != null)
        List params = init.getParams();

        if (spec.getType() == StreamSpec.STREAM_GLOBAL) {

            if (libraryFormat) {
                result.append(indent + "private static " + spec.getName() +
                              " __instance = null;\n");
                result.append(indent + "private " + spec.getName() +"() {}\n");
                result.append(indent + "public static " + spec.getName() +
                              " __get_instance() {\n");
                addIndent();
                result.append(indent + "if (__instance == null) { __instance = new " + 
                              spec.getName() + "(); __instance.init(); }\n");
                result.append(indent + "return __instance;\n");
                unIndent();
                result.append(indent+"}\n");
            }

            return result.toString();
        }

        
        // In the library path, generate the __construct() mechanism:
        if (libraryFormat)
            {

                // Generate fields for each of the parameters.
                for (Iterator iter = params.iterator(); iter.hasNext(); )
                    {
                        Parameter param = (Parameter)iter.next();
                        result.append(indent + "private " +
                                      convertType(param.getType()) +
                                      " __param_" + param.getName() + ";\n");
                    }
        
                // Generate a __construct() method that saves these.
                result.append(indent + "public static " + spec.getName() +
                              " __construct(");
                boolean first = true;
                for (Iterator iter = params.iterator(); iter.hasNext(); )
                    {
                        Parameter param = (Parameter)iter.next();
                        if (!first) result.append(", ");
                        first = false;
                        result.append(convertType(param.getType()) + " " +
                                      param.getName());
                    }
                result.append(")\n" + indent + "{\n");
                addIndent();
                result.append(indent + spec.getName() + " __obj = new " +
                              spec.getName() + "();\n");
                for (Iterator iter = params.iterator(); iter.hasNext(); )
                    {
                        Parameter param = (Parameter)iter.next();
                        String name = param.getName();
                        result.append(indent + "__obj.__param_" + name + " = " +
                                      name + ";\n");
                    }
                result.append(indent + "return __obj;\n");
                unIndent();
                result.append(indent + "}\n");
        
                // Generate a callInit() method.
                result.append(indent + "protected void callInit()\n" +
                              indent + "{\n");
                addIndent();
                result.append(indent + "init(");
                first = true;
                for (Iterator iter = params.iterator(); iter.hasNext(); )
                    {
                        Parameter param = (Parameter)iter.next();
                        if (!first) result.append(", ");
                        first = false;
                        result.append("__param_" + param.getName());
                    }
                result.append(");\n");
                unIndent();
                result.append(indent + "}\n");
            }
        // In the compiler path, generate an empty constructor.
        else // (!libraryFormat)
            {
                result.append(indent + "public " + spec.getName() + "(");
                boolean first = true;
                for (Iterator iter = params.iterator(); iter.hasNext(); )
                    {
                        Parameter param = (Parameter)iter.next();
                        if (!first) result.append(", ");
                        first = false;
                        result.append(convertType(param.getType()) + " " +
                                      param.getName());
                    }
                result.append(")\n" + indent + "{\n" + indent + "}\n");
            }
        
        return result.toString();
    }

    public Object visitStreamSpec(StreamSpec spec)
    {
        String result = "";
        if (spec.getType() == StreamSpec.STREAM_GLOBAL) global = true; // set global bit

        // Anonymous classes look different from non-anonymous ones.
        // This appears in two places: (a) as a top-level (named)
        // stream; (b) in an anonymous stream creator (SCAnon).
        //
        // However... 
        // Any code here for anonymous streams should be obsolete since 
        // NameAnonymousStreams should have been run before NodesToJava.
        if (spec.getName() != null)
            {
                // Non-anonymous stream.  Maybe it has interfaces.
                String ifaces = maybeGeneratePortal(spec);
                if (ifaces == null)
                    ifaces = "";
                else
                    {
                        result += ifaces;
                        ifaces = " implements " + spec.getName() + "Interface";
                    }
                result += indent;
                // This is only public if it's the top-level stream,
                // meaning it has type void->void.
                if (isTopLevelSpec(spec)) {
                    result += "public class " + spec.getName()
                        + " extends StreamIt" + spec.getTypeString() + ifaces
                        + " // " + spec.getContext() + "\n";
                    result += indent + "{\n";
                    addIndent();
                } else {
                    result += "class " + spec.getName() + " extends ";
                    switch (spec.getType()) {
                    case StreamSpec.STREAM_FILTER:
                        result += "Filter";
                        break;
                    case StreamSpec.STREAM_PIPELINE:
                        result += "Pipeline";
                        break;
                    case StreamSpec.STREAM_SPLITJOIN:
                        result += "SplitJoin";
                        break;
                    case StreamSpec.STREAM_FEEDBACKLOOP:
                        result += "FeedbackLoop";
                        break;
                    case StreamSpec.STREAM_GLOBAL:
                        result += "Global";
                        break;
                    }
                    result += ifaces + " // " + spec.getContext() + "\n" + indent
                        + "{\n";
                    addIndent();
                    // If we're in the library backend, we need a construct()
                    // method too; in the compiler backend, a constructor.
                    result += maybeGenerateConstruct(spec);
                }
            } else {
                assert false : "NodesToJava run before NameAnonymousStreams";
                /*
                // Anonymous stream: 
                result += "new "; 
                switch (spec.getType()) { 
                case StreamSpec.STREAM_FILTER: result += "Filter"; break; 
                case StreamSpec.STREAM_PIPELINE: result += "Pipeline"; break; 
                case StreamSpec.STREAM_SPLITJOIN: result += "SplitJoin"; break; 
                case StreamSpec.STREAM_FEEDBACKLOOP: result += "FeedbackLoop"; 
                break;
                }
                result += "() {\n" + indent;
                addIndent();
                */
            }
        // At this point we get to ignore wholesale the stream type, except
        // that we want to save it.
        StreamSpec oldSS = ss;
        ss = spec;

        // Output field definitions:
        for (Iterator iter = spec.getVars().iterator(); iter.hasNext(); )
            {
                FieldDecl varDecl = (FieldDecl)iter.next();
                result += (String)varDecl.accept(this);
            }
        
        // Output method definitions:
        for (Iterator iter = spec.getFuncs().iterator(); iter.hasNext(); )
            result += (String)(((Function)iter.next()).accept(this));

        ss = oldSS;

        // Top-level stream: do any post-processing and emit "main"
        // This is only public if it's the top-level stream,
        // meaning it has type void->void.
        if (isTopLevelSpec(spec) && libraryFormat) {
            result += indent + "public static void main(String[] args) {\n";
            addIndent();
            if (countops) {
                // tell the profiler how many operations ID's there are
                result += indent + "Profiler.setNumIds(" + MAX_PROFILE_ID + ");\n";
            }
            result += indent + spec.getName() + " program = new "
                + spec.getName() + "();\n";
            result += indent + "program.run(args);\n";
            if (libraryFormat) {
                result += indent + "FileWriter.closeAll();\n";
                if (countops) {
                    result += indent + "Profiler.summarize();\n";
                }
            }
            unIndent();
            result += indent + "}\n";
        }

        unIndent();
        result += "}\n";
        global = false; // unset global bit
        return result;
    }

    public Object visitTypeHelper(TypeHelper th) {
        String result = "";
        boolean _native = (th.getCls() == TypeHelper.NATIVE_HELPERS);

        if (libraryFormat && _native) return result;

        result += "class "+th.getName()+" extends "+(_native?"NativeHelper":"Helper")+
            " // "+th.getContext()+"\n";
        result += "{\n";

        addIndent();
        int num = th.getNumFuncs();
        for (int i = 0; i < num; i++) {
            result += (String)th.getFunction(i).accept(this);
        } 
        unIndent();

        result += "}\n";
        return result;
    }
    
    public Object visitStreamType(StreamType type)
    {
        // Nothing to do here.
        return "";
    }
    
    public Object visitOther(FENode node)
    {
        if (node instanceof ExprJavaConstructor)
            {
                ExprJavaConstructor jc = (ExprJavaConstructor)node;
                return makeConstructor(jc.getType());
            }
        if (node instanceof StmtIODecl)
            {
                StmtIODecl ap = (StmtIODecl)node;
                String result;
                if (ap.isPrework()) {
                    result = "addInitPhase";
                } else if (ap.isWork()) {
                    result = "addSteadyPhase";
                } else {
                    // helper function
                    result = "annotateIORate";
                }
                result += "(";
                if (ap.getPeek() == null) {
                    // by default, peek==pop
                    if (ap.getPop() == null) {
                        result += "0, ";
                    } else {
                        result += (String)ap.getPop().accept(this) + ", ";
                    }
                } else
                    result += (String)ap.getPeek().accept(this) + ", ";
                if (ap.getPop() == null)
                    result += "0, ";
                else
                    result += (String)ap.getPop().accept(this) + ", ";
                if (ap.getPush() == null)
                    result += "0, ";
                else
                    result += (String)ap.getPush().accept(this) + ", ";
                result += "\"" + ap.getName() + "\")";
                return result;
            }
        if (node instanceof StmtSetTypes)
            {
                StmtSetTypes sst = (StmtSetTypes)node;
                return "setIOTypes(" + typeToClass(sst.getInType()) +
                    ", " + typeToClass(sst.getOutType()) + ")";
            }
        else
            {
                assert false : node;
                return "";
            }
    }
}
