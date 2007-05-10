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
 * $Id: Utils.java,v 1.52 2007-05-10 21:31:02 dimock Exp $
 */

package at.dms.util;

import java.io.*;

import at.dms.compiler.JavaStyleComment;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.LoweringConstants;
import java.lang.reflect.Array;
import java.util.*;
import java.io.Serializable;
import java.math.BigInteger;

/**
 * This class defines severals utilities methods used in source code
 */
public abstract class Utils implements Serializable, DeepCloneable {

    // ----------------------------------------------------------------------
    // UTILITIES
    // ----------------------------------------------------------------------

    /**
     * Check if an assertion is valid
     *
     * @exception   RuntimeException    the entire token reference
     */
    public static final void kopi_assert(boolean b) {
        assert b;
    }

    /**
     * Check if an assertion is valid with a given error message
     *
     * @exception   RuntimeException    the entire token reference
     */
    public static final void kopi_assert(boolean b, String str) {
        assert b : str;
    }

    /**
     * Signal a failure with given error message
     *
     * @exception   RuntimeException    the entire token reference
     */
    public static final void fail(String str) {
        new RuntimeException("Failure: " + str).printStackTrace();
        System.exit(1);
    }

    
    /**
     * Returns the contents of <pre>fileName</pre> as a string buffer.
     */
    public static StringBuffer readFile(String fileName)
        throws IOException
    {
        StringBuffer result = new StringBuffer();
        BufferedReader in = new BufferedReader(new FileReader(fileName));
        while (true) {
            String line = in.readLine();
            if (line == null) {
                break;
            } else {
                result.append(line + "\n");
            }
        }
        in.close();
        return result;
    }

    /**
     * Writes <pre>str</pre> to <pre>filename</pre>, overwriting it if it's already
     * there.
     */
    public static void writeFile(String filename, String str) throws IOException {
        FileWriter out = new FileWriter(filename);
        out.write(str, 0, str.length());
        out.close();
    }

    /**
       /** replaces in all occurances.
       *modifies: nothing.<br/>
       *effects: constructs a new String from orig, replacing all occurances of oldSubStr with newSubStr.
       * <br/>
       *@return  a copy of orig with all occurances of oldSubStr replaced with newSubStr.
       *
       * if any of arguments are null, returns orig.
       */
    public static synchronized String replaceAll( String orig, String oldSubStr, String newSubStr )
    {
        if (orig==null || oldSubStr==null || newSubStr==null) {
            return orig;
        }
        // create a string buffer to do replacement
        StringBuffer sb = new StringBuffer(orig);
        // keep track of difference in length between orig and new
        int offset = 0;
        // keep track of last index where we saw the substring appearing
        int index = -1;
    
        while (true) {

            // look for occurrence of old string
            index = orig.indexOf(oldSubStr, index+1);
            if (index==-1) {
                // quit when we run out of things to replace
                break;
            }
        
            // otherwise, do replacement
            sb.replace(index - offset, 
                       index - offset + oldSubStr.length(), 
                       newSubStr);

            // increment our offset
            offset += oldSubStr.length() - newSubStr.length();
        }
        // return new string
        return sb.toString();
    }


    /**
     * Store information needed to translate math method names during code emission.
     * Should be extended to include the type information. (at.dms.kjc.CStdType.Float)
     * @author dimock
     * (vector versions in simdmath.h)
     */

    private enum MathMethodInfo {
        ACOS("acos", "acosf", "acosf", "acosf4"),        // float -> float
        ASIN("asin", "asinf", "asinf", "asinf4"),        // float -> float
        ATAN("atan", "atanf", "atanf", "atanf4"),        // float -> float
        // not supplied on cell but processed: atan2(x,y) => atanf4(y/x)
        // in $STREAMIT_HOME/misc/vectorization.h
        ATAN2("atan2", "atan2f", "atan2f", "atan2f4"),   // float x float -> float
        CEIL("ceil", "ceilf", "ceilf", "ceilf4"),
        COS("cos", "cosf", "cosf", "cosf4"),             // float -> float
        SIN("sin", "sinf", "sinf", "sinf4"),             // float -> float
        COSH("cosh", "coshf", "coshf", "coshf4"),             // float -> float
        SINH("sinh", "sinhf", "sinhf", "sinhf4"),             // float -> float
        EXP("exp", "expf", "expf", "expf4"),             // float -> float
// never used.        FABS("fabs", "fabsf", "fabsf", "fabsf4"),        // float -> float
        // note int has %, vector int has fmod_i_v
// never used        FMOD("fmod", "fmodf", "fmodf", "fmodf4"),        // float -> float
// never used        MODF("modf", "modf", "modf", "fmodf4"),          // float x float* -> float
// never used        FREXP("frexp", "frexpf", "frexpf", "frexpf4"),   // float x int* -> float
        FLOOR("floor", "floorf", "floorf", "floorf4"),   // float -> float
        LOG("log", "logf", "logf", "logf4"),             // float -> float
        LOG10("log10", "log10f", "log10f", "log10f4"),   // float -> float
        POW("pow", "powf", "powf", "powf4"),             // float x float -> float
        RANDOM("random", "", "", ""),                     // void -> float in java, void -> int in C, C++
        __RANDOM("__random", "rand", "rand", "rand_v"),   // internal in translating random() to rand();
                                                          // not in cell sdk 2 simdmath
        // round(x) should be replaced with trunc(x+0.5) to match java behavior
        // will still need casting to int.
        ROUND("round", "roundf", "roundf", "roundf4"),   // float -> float
        // had been translated as rintf plus cast...
        RINT("rint", "lrintf", "lrintf", "lrintf4"),     // float -> int  not in cell sdk 2 simdmath
        SQRT("sqrt", "sqrtf", "sqrtf", "sqrtf4"),        // float -> float
        // not suplied on cell, (exp(x) - exp(- x)) / (exp(x) + exp(- x))
        TANH("tanh", "tanhf", "tanhf", null),             // float -> float  not in cell sdk 2 simdmath
        TAN("tan", "tanf", "tanf", "tanf4"),             // float -> float
        // Some bad compromises follow:  These are used (in non-vector case)
        // at int and float types, thus use the double versions to allow an int 
        // to be cast, processed, and cast back without losing precision. 
        ABS("abs", "fabs", "fabs", "fabsf4"),            // double -> double (exc vector: float->float)
        MAX("max", "maxf", "fmax", "fmaxf4"),            // double x double -> double (exc vector)
        MIN("min", "minf", "fmin", "fminf4"),            // double x double -> double (exc vector)
        ;
        
        MathMethodInfo(String streamit_name, String c_name, String cpp_name, String cell_name) {
            this.streamit_name = streamit_name;
            this.c_name = c_name;
            this.cpp_name = cpp_name;
            this.cell_name = cell_name;
        }
        
        private String streamit_name; // just used for mapping from String to Enum
        private String c_name;     // C equivalent
        private String cpp_name;   // C++ equivalent
        private String cell_name;  // Cell vector macro
        
        /** first field: name in streamit */
        public String streamit_name() {return streamit_name;}
        /** second field: name in C backends */
        public String c_name() {return c_name;}
        /** third field: name in C++ backends */
        public String cpp_name() {return cpp_name;}
        /** fourth field: name of vector version for cell */
        public String cell_name() {return cell_name;}
        
        private static Map<String, MathMethodInfo> mathMethodMap;
        
        static {
            mathMethodMap = new HashMap<String, MathMethodInfo>();
            EnumSet<MathMethodInfo> allMathMethods = EnumSet.allOf(MathMethodInfo.class);
            for (MathMethodInfo m : allMathMethods) {
                mathMethodMap.put(m.streamit_name(), m);
            }
        }
    }

    /**
     * promote a literal type to double.
     * @param from  an integer, float, or double literal.
     * @return a double literal.
     */
    private static JExpression asDouble(JExpression from)
    {
        if (from instanceof JFloatLiteral)
            return new JDoubleLiteral(from.getTokenReference(),
                                      from.floatValue());
        if (from instanceof JIntLiteral)
            return new JDoubleLiteral(from.getTokenReference(),
                                      from.intValue());
        assert from instanceof JDoubleLiteral;
        return from;
    }
   
    /**
     * Simplify an call to a math function with literal arguments.
     * Even if can't simplify, pass back something semantically 
     * equivalent to the original expression.
     * @param applyMath  application of math function to simplify
     * @return an expression, possibly a literal value.
     */
    public static JExpression simplifyMathMethod(JMethodCallExpression applyMath) {
        MathMethodInfo mm = MathMethodInfo.mathMethodMap.get(applyMath
                .getIdent());
        if (mm == null) {
            return applyMath;
        }

        JExpression[] args = applyMath.getArgs();

        if (mm == MathMethodInfo.RANDOM) {
            // TODO: extend to setting seed, but
            // need to determine how to change float seed for random(s) 
            // to int seed for srand(s')
            if (args.length != 0) {
                return applyMath;
            }
            /* random() should return a floating random value in [0,1)  ... or is that [0,1] ?
             * need to do this using rand() returning an integer value [0, RAND_MAX]
             * convert as:
             * (float)((double)(rand()) / ((double)RAND_MAX + 1.0))
             * despite the fact that RAND_MAX may be large, we want to avoid doubles for
             * most vector architectures, so there use:
             * (float)(rand()) / ((float)RAND_MAX + 1.0f)
             * 
             * changes name from random() to __random() so that translation does not iterate.
             * __random() changed on code emission to right name for target.
             * 
             * Problem: RAND_MAX is a named constant rather than a field or local variable
             * We make it be an (uninitialized) field for purposes of further compilation. 
             */
            JExpression randmax = 
                new JFieldAccessExpression(new JThisExpression(),
                        "RAND_MAX");
            randmax.setType(CStdType.Integer);

            JExpression dividend = null;
            JExpression divisor = null;
            JExpression quotient = null;
            JExpression cast_quotient = null;

            JExpression randcall = new JMethodCallExpression(applyMath.getTokenReference(),
                    applyMath.getPrefix(),
                    "__random", applyMath.getArgs());
            randcall.setType(CStdType.Integer);
            
            if (KjcOptions.vectorize > 0) {
                dividend = new JAddExpression(null,
                        new JCastExpression(null,  // is there a specific widening cast in kopi?
                                randmax, 
                                CStdType.Float),
                        new JFloatLiteral(1.0f));
                divisor = new JCastExpression(null, randcall, CStdType.Float);
                quotient = new JDivideExpression(null, divisor, dividend);
                cast_quotient = quotient;
            } else {
                try {
                dividend = new JAddExpression(null,
                        new JCastExpression(null,  // is there a specific widening cast in kopi?
                                randmax, 
                                CStdType.Double),
                        new JDoubleLiteral(null,"1.0"));
                } catch (at.dms.compiler.PositionedError e) {
                    assert false : e;  // should not raise invalid format error.
                }
                divisor = new JCastExpression(null, randcall, CStdType.Double);
                quotient = new JDivideExpression(null, divisor, dividend);
                cast_quotient = new JCastExpression(null, quotient, CStdType.Float);
            }
            return cast_quotient;
        }
        
        for (JExpression arg : args) {
            if (!(arg instanceof JLiteral)) {
                return applyMath;
            }
        }

        // At this point know that we are dealing with
        // a supported math method with constant arguments.

        double darg;
        double darg2;

        switch (mm) {
        case __RANDOM:
            // called again with translatoin of random();
            return applyMath;
        case ACOS:
            assert args.length == 1;
            darg = asDouble(args[0]).doubleValue();
            return new JDoubleLiteral(applyMath.getTokenReference(), Math
                    .acos(darg));

        case ASIN:
            assert args.length == 1;
            darg = asDouble(args[0]).doubleValue();
            return new JDoubleLiteral(applyMath.getTokenReference(), Math
                    .asin(darg));
        case ATAN:
            assert args.length == 1;
            darg = asDouble(args[0]).doubleValue();
            return new JDoubleLiteral(applyMath.getTokenReference(), Math
                    .atan(darg));
        case ATAN2:
            assert args.length == 2;
            darg = asDouble(args[0]).doubleValue();
            darg2 = asDouble(args[1]).doubleValue();
            return new JDoubleLiteral(applyMath.getTokenReference(), Math
                    .atan2(darg, darg2));
        case CEIL:
            assert args.length == 1;
            darg = asDouble(args[0]).doubleValue();
            return new JDoubleLiteral(applyMath.getTokenReference(), Math
                    .ceil(darg));
        case COS:
            assert args.length == 1;
            darg = asDouble(args[0]).doubleValue();
            return new JDoubleLiteral(applyMath.getTokenReference(), Math
                    .cos(darg));
        case SIN:
            assert args.length == 1;
            darg = asDouble(args[0]).doubleValue();
            return new JDoubleLiteral(applyMath.getTokenReference(), Math
                    .sin(darg));
        case COSH:
            assert args.length == 1;
            darg = asDouble(args[0]).doubleValue();
            return new JDoubleLiteral(applyMath.getTokenReference(), Math
                    .cosh(darg));
        case SINH:
            assert args.length == 1;
            darg = asDouble(args[0]).doubleValue();
            return new JDoubleLiteral(applyMath.getTokenReference(), Math
                    .sinh(darg));
        case EXP:
            assert args.length == 1;
            darg = asDouble(args[0]).doubleValue();
            return new JDoubleLiteral(applyMath.getTokenReference(), Math
                    .exp(darg));
        case FLOOR:
            assert args.length == 1;
            darg = asDouble(args[0]).doubleValue();
            return new JDoubleLiteral(applyMath.getTokenReference(), Math
                    .floor(darg));
        case LOG:
            assert args.length == 1;
            darg = asDouble(args[0]).doubleValue();
            return new JDoubleLiteral(applyMath.getTokenReference(), Math
                    .log(darg));
        case LOG10:
            assert args.length == 1;
            darg = asDouble(args[0]).doubleValue();
            return new JDoubleLiteral(applyMath.getTokenReference(), Math
                    .log10(darg));
        case POW:
            assert args.length == 2;
            darg = asDouble(args[0]).doubleValue();
            darg2 = asDouble(args[1]).doubleValue();
            return new JDoubleLiteral(applyMath.getTokenReference(), Math.pow(
                    darg, darg2));
        case ROUND:
            assert args.length == 1;
            darg = asDouble(args[0]).doubleValue();
            return new JDoubleLiteral(applyMath.getTokenReference(), Math
                    .round(darg));
        case RINT:
            assert args.length == 1;
            darg = asDouble(args[0]).doubleValue();
            return new JDoubleLiteral(applyMath.getTokenReference(), Math
                    .rint(darg));
        case SQRT:
            assert args.length == 1;
            darg = asDouble(args[0]).doubleValue();
            return new JDoubleLiteral(applyMath.getTokenReference(), Math
                    .sqrt(darg));
        case TAN:
            assert args.length == 1;
            darg = asDouble(args[0]).doubleValue();
            return new JDoubleLiteral(applyMath.getTokenReference(), Math
                    .tan(darg));
        case TANH:
            assert args.length == 1;
            darg = asDouble(args[0]).doubleValue();
            return new JDoubleLiteral(applyMath.getTokenReference(), Math
                    .tanh(darg));
        case ABS:
            assert args.length == 1;
            if (args[0] instanceof JIntLiteral) {
                return new JIntLiteral(applyMath.getTokenReference(), Math
                        .abs(((JIntLiteral) args[0]).intValue()));
            } else {
                darg = asDouble(args[0]).doubleValue();
                return new JDoubleLiteral(applyMath.getTokenReference(), Math
                        .abs(darg));
            }
        case MAX:
            assert args.length == 2;
            if (args[0] instanceof JIntLiteral
                    && args[1] instanceof JIntLiteral) {
                return new JIntLiteral(applyMath.getTokenReference(), Math.max(
                        ((JIntLiteral) args[0]).intValue(),
                        ((JIntLiteral) args[1]).intValue()));
            } else {
                darg = asDouble(args[0]).doubleValue();
                darg2 = asDouble(args[1]).doubleValue();
                return new JDoubleLiteral(applyMath.getTokenReference(), Math
                        .max(darg, darg2));
            }
        case MIN:
            assert args.length == 2;
            if (args[0] instanceof JIntLiteral
                    && args[1] instanceof JIntLiteral) {
                return new JIntLiteral(applyMath.getTokenReference(), Math.min(
                        ((JIntLiteral) args[0]).intValue(),
                        ((JIntLiteral) args[1]).intValue()));
            } else {
                darg = asDouble(args[0]).doubleValue();
                darg2 = asDouble(args[1]).doubleValue();
                return new JDoubleLiteral(applyMath.getTokenReference(), Math
                        .min(darg, darg2));
            }
        default:
            throw new AssertionError(mm);
        }
    }


    
// private static Set<String> mathMethods;
//    
//
// static {
// mathMethods = new HashSet<String>();
// mathMethods.addAll(Arrays.asList(new String[]{
// "acos", "asin", "atan", "atan2", "ceil", "cos", "sin", "cosh", "sinh",
// "exp", "fabs", "abs", "max", "min", "modf", "fmod", "frexp", "floor",
// "log", "log10", "pow", "round", "rint", "sqrt", "tanh", "tan"
// }));
// }

    /**
     * Is the passed method name (broken into prefix and identifier) a Java math
     * method? Limited to those methods that we can emit code for...
     * 
     * @param prefix
     *            JExpression that is method name prefix
     * @param ident
     *            String that is method name
     * @return whether or not method is a math method.
     */
    public static boolean isMathMethod(JExpression prefix, String ident) 
    {
        if (prefix instanceof JTypeNameExpression &&
                ((JTypeNameExpression)prefix).getQualifiedName().equals("java/lang/Math") &&
                /*mathMethods.contains(ident)*/ MathMethodInfo.mathMethodMap.containsKey(ident)) {
            return true;
        }
        return false;
    }
    
    /**
     * Return the name of a math method for vector processing using IBM's 
     * vector headers for the Cell processor (or null if none)
     * @param prefix Prefix portion of the method name
     * @param ident  ident portion of the method name.
     * @return name or null.
     */
    public static String cellMathEquivalent(JExpression prefix, String ident) {
        if (prefix instanceof JTypeNameExpression &&
                ((JTypeNameExpression)prefix).getQualifiedName().equals("java/lang/Math")) {
            MathMethodInfo mm = MathMethodInfo.mathMethodMap.get(ident);
            if (mm == null) return null;
            return mm.cell_name;
        }
        return null; 
    }
    
    /**
     * Return the name of a math method for emitting C code
     * @param prefix Prefix portion of the method name
     * @param ident  ident portion of the method name.
     * @return name or null.
     */
    public static String cMathEquivalent(JExpression prefix, String ident) {
        if (prefix instanceof JTypeNameExpression &&
                ((JTypeNameExpression)prefix).getQualifiedName().equals("java/lang/Math")) {
            MathMethodInfo mm = MathMethodInfo.mathMethodMap.get(ident);
            if (mm == null) return null;
            return mm.c_name;
        }
        return null; 
    }
    
    /**
     * Return the name of a math method for emitting C++ code
     * @param prefix Prefix portion of the method name
     * @param ident  ident portion of the method name.
     * @return name or null.
     */
    public static String cppMathEquivalent(JExpression prefix, String ident) {
        if (prefix instanceof JTypeNameExpression &&
                ((JTypeNameExpression)prefix).getQualifiedName().equals("java/lang/Math")) {
            MathMethodInfo mm = MathMethodInfo.mathMethodMap.get(ident);
            if (mm == null) return null;
            return mm.cpp_name;
        }
        return null; 
    }
    
    
  
    /**
     * Returns whether all elements of an array of JExpressions are
     * JLiterals with the same value.
     * @param arr 
     * @return 
     */
    public static boolean isUniform(JExpression[] arr) {
        // ok to be empty
        if (arr.length == 0) return true;
        // get first val
        JExpression val = arr[0];
        if (!(val instanceof JLiteral)) {
            return false;
        }
        // check rest of vals are equal
        for (int i=1; i<arr.length; i++) {
            if (!(arr[i] instanceof JLiteral) ||
                !((JLiteral)arr[i]).equals((JLiteral)val)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns <pre>val</pre> as a percentage with maximum of 4 digits
     */
    public static String asPercent(double val) {
        String result = "" + (100*val);
        return result.substring(0, Math.min(5, result.length())) + "%";
    }

    /**
     * Returns a power of 2 that is greater than or equal to <pre>val</pre>.
     */
    public static int nextPow2(int val) {
        if (val==0) { return val; }
        BigInteger bigVal = BigInteger.valueOf(val);
        int shiftAmount = bigVal.subtract (BigInteger.valueOf (1)).bitLength ();
        return BigInteger.ONE.shiftLeft (shiftAmount).intValue ();
    }

    /**
     * Returns a list of Integers containing same elements as <pre>arr</pre>
     */
    public static List<Integer> intArrayToList(int[] arr) {
        LinkedList<Integer> result = new LinkedList<Integer>();
        for (int i=0; i<arr.length; i++) {
            result.add(new Integer(arr[i]));
        }
        return result;
    }

    /**
     * Creates a vector and fills it with the elements of the specified array.
     *
     * @param   array       the array of elements
     */
    public static <T> Vector<T> toVector(T[] array) {
        if (array == null) {
            return new Vector<T>();
        } else {
            Vector<T> vector = new Vector<T>(array.length);

            for (int i = 0; i < array.length; i++) {
                vector.addElement(array[i]);
            }
            return vector;
        }
    }

    /**
     * Creates a typed array from a vector.
     * 
     * Java 1.5: should use Vector.toArray
     *
     * @param   vect        the vector containing the elements
     * @param   type        the type of the elements
     * @deprecated
     */
    public static Object[] toArray(Vector vect, Class type) {
        if (vect != null && vect.size() > 0) {
            Object[]    array = (Object[])Array.newInstance(type, vect.size());

            try {
                vect.copyInto(array);
            } catch (ArrayStoreException e) {
                System.err.println("Array was:" + vect.elementAt(0));
                System.err.println("New type :" + array.getClass());
                throw e;
            }
            return array;
        } else {
            return (Object[])Array.newInstance(type, 0);
        }
    }

    /**
     * Creates a int array from a vector.
     *
     * different from Vector.toArray because casts Integer to int
     *
     * @param   vect        the vector containing the elements
     */
    public static int[] toIntArray(Vector<Integer> vect) {
        if (vect != null && vect.size() > 0) {
            int[]   array = new int[vect.size()];

            for (int i = array.length - 1; i >= 0; i--) {
                array[i] = vect.elementAt(i).intValue();
            }

            return array;
        } else {
            return new int[0]; // $$$ static ?
        }
    }

    /**
     * Returns a new array of length n with all values set to val
     *
     * @param   n       the desired number of elements in the array
     * @param   val     the value of each element
     */
    public static int[] initArray(int n, int val) {
        int[] result = new int[n];
        for (int i=0; i<n; i++) {
            result[i] = val;
        }
        return result;
    }

    /**
     * Returns a new array of length n with all values set to val
     *
     * @param   n       the desired number of elements in the array
     * @param   exp     the value of each element
     */
    public static JExpression[] initArray(int n, JExpression exp) {
        JExpression[] result = new JExpression[n];
        for (int i=0; i<n; i++) {
            result[i] = exp;
        }
        return result;
    }

    /**
     * Returns a new array of length n with all values as JIntLiterals set to val
     *
     * @param   n       the desired number of elements in the array
     * @param   val     the value of each element
     */
    public static JExpression[] initLiteralArray(int n, int val) {
        JExpression[] result = new JExpression[n];
        for (int i=0; i<n; i++) {
            result[i] = new JIntLiteral(val);
        }
        return result;
    }

    /**
     * Returns whether or not two integer arrays have the same length
     * and entries
     */
    public static boolean equalArrays(int[] a1, int[] a2) {
        if (a1.length!=a2.length) {
            return false;
        } else {
            boolean ok = true;
            for (int i=0; i<a1.length; i++) {
                ok = ok && a1[i]==a2[i];
            }
            return ok;
        }
    }

    
    
    /**
     * Given a statement, return the expression that this statement is 
     * composed of, if not an expression statement return null.
     *
     *
     * @param orig The statement
     *
     *
     * @return null if <pre>orig</pre> does not contain an expression or
     * the expression if it does.
     */
    public static JExpression getExpression(JStatement orig)
    {
        if (orig instanceof JExpressionListStatement) {
            JExpressionListStatement els = (JExpressionListStatement)orig;
            if (els.getExpressions().length == 1)
                return passThruParens(els.getExpression(0));
            else
                return null;
        }
        else if (orig instanceof JExpressionStatement) {
            return passThruParens(((JExpressionStatement)orig).getExpression());
        }
        else 
            return null;
    }

    /**
     * Return the first non-parentheses expressions contained in <pre>orig</pre> 
     **/
    public static JExpression passThruParens(JExpression orig) 
    {
        if (orig instanceof JParenthesedExpression) {
            return passThruParens(((JParenthesedExpression)orig).getExpr());
        }
        return orig;
    }
    
    /**
     * Splits a string like:
     *   "java/lang/System/out"
     * into two strings:
     *    "java/lang/System" and "out"
     */
    public static String[] splitQualifiedName(String name, char separator) {
        String[]    result = new String[2];
        int     pos;

        pos = name.lastIndexOf(separator);

        if (pos == -1) {
            // no '/' in string
            result[0] = "";
            result[1] = name;
        } else {
            result[0] = name.substring(0, pos);
            result[1] = name.substring(pos + 1);
        }

        return result;
    }
  

    /**
     * Splits a string like:
     *   "java/lang/System/out"
     * into two strings:
     *    "java/lang/System" and "out"
     */
    public static String[] splitQualifiedName(String name) {
        return splitQualifiedName(name, '/');
    }


    /**
     * If the first and last SIRMarker's in <pre>stmt</pre> mark the beginning
     * and end of the same segment, then move those markers to the
     * outermost edges of <pre>stmt</pre>.  The purpose of this routine is to
     * lift markers of filter boundaries out of loops.
     */
    public static JStatement peelMarkers(JStatement stmt) {
        final SIRBeginMarker[] first = { null };
        final SIREndMarker[] last = { null };
        // find first and last marker
        stmt.accept(new SLIREmptyVisitor() {
                public void visitMarker(SIRMarker self) {
                    // record first and last
                    if (self instanceof SIRBeginMarker && first[0] == null) {
                        first[0] = (SIRBeginMarker)self;
                    }
                    if (self instanceof SIREndMarker) {
                        last[0] = (SIREndMarker)self;
                    }
                }
            });

        // if we didn't find two markers, or if first and last marker
        // have different names, then there is nothing to peel, so
        // return
        if (first[0] == null || last[0] == null) return stmt;
        if (!first[0].getName().equals(last[0].getName())) return stmt;

        // otherwise, we are going to move the markers to the outside
        // of the statement.  replace the markers with empty
        // statements in the IR
        stmt.accept(new SLIRReplacingVisitor() {
                public Object visitMarker(SIRMarker self) {
                    if (self==first[0] || self==last[0]) {
                        return new JEmptyStatement();
                    } else {
                        return self;
                    }
                }
            });

        // finally, create a new block that begins with the first
        // marker, then has the statement, then has the last marker
        JBlock result = new JBlock();
        result.addStatement(first[0]);
        result.addStatement(stmt);
        result.addStatement(last[0]);

        return result;
    }

    /**
     * Returns a version of <pre>stmt</pre> with all standalone pops (i.e.,
     * pop() as a statement rather than as an expression) removed.
     */
    public static JStatement removeUnusedPops(JStatement stmt) {
        return (JStatement)stmt.accept(new SLIRReplacingVisitor() {
                public Object visitExpressionStatement(JExpressionStatement self, JExpression expr) {
                    if (expr instanceof SIRPopExpression) {
                        return new JEmptyStatement();
                    } else {
                        return super.visitExpressionStatement(self, expr);
                    }
                }
            });
    }

    /**
     * Returns whether or not there are any pop expressions before
     * peek expressions in the dynamic execution of <pre>stmt</pre>.
     */
    public static boolean popBeforePeek(JStatement stmt) {
        // there are two ways that a dynamic pop can come before a
        // peek:
        // 1. the pop statement comes first in the static code listing
        // 2. a pop and peek statement are in the same loop

        // CHECK CONDITION 1
        final boolean popBeforePeek1[] = { false };
        {
        final boolean seenPop1[] = { false };
        stmt.accept(new SLIREmptyVisitor() {
                public void visitPopExpression(SIRPopExpression self,
                                               CType tapeType) {
                    seenPop1[0] = true;
                }
                public void visitPeekExpression(SIRPeekExpression self,
                                                CType tapeType,
                                                JExpression arg) {
                    super.visitPeekExpression(self, tapeType, arg);
                    if (seenPop1[0]) {
                        popBeforePeek1[0] = true;
                    }
                }
            });
        }

        // CHECK CONDITION 2
        final boolean popBeforePeek2[] = { false };
        {
        // count of loop nesting
        final int numLoops[] = { 0 };
        // whether or not we have seen pop or peek in outermost loop
        final boolean seenPop2[] = { false };
        final boolean seenPeek2[] = { false };
        stmt.accept(new SLIREmptyVisitor() {
                public void visitForStatement(JForStatement self,
                                              JStatement init,
                                              JExpression cond,
                                              JStatement incr,
                                              JStatement body) {
                    clearStats();
                    numLoops[0]++;
                    super.visitForStatement(self, init, cond, incr, body);
                    numLoops[0]--;
                    updateStats();
                }

                public void visitWhileStatement(JWhileStatement self,
                                                JExpression cond,
                                                JStatement body) {
                    clearStats();
                    numLoops[0]++;
                    super.visitWhileStatement(self, cond, body);
                    numLoops[0]--;
                    updateStats();
                }

                // if we just left outer-most loop, check if there is
                // both pop and peek in the loop
                private void updateStats() {
                    if (numLoops[0] == 0) {
                        if (seenPop2[0] && seenPeek2[0]) {
                            popBeforePeek2[0] = true;
                        }
                        clearStats();
                    }
                }
                
                // clear counts for next loop
                private void clearStats() {
                    if (numLoops[0] == 0) {
                        seenPop2[0] = false;
                        seenPeek2[0] = false;
                    }
                }


                public void visitPopExpression(SIRPopExpression self,
                                               CType tapeType) {
                    seenPop2[0] = true;
                }
                public void visitPeekExpression(SIRPeekExpression self,
                                                CType tapeType,
                                                JExpression arg) {
                    super.visitPeekExpression(self, tapeType, arg);
                    seenPeek2[0] = true;
                }
            });
        }

        // return true if either condition found pop before peek
        return popBeforePeek1[0] || popBeforePeek2[0];
    }

    /**
     * Set to true to get a stack trace of callers inserted as a comment.
     * 
     * Limitation: only provides info for loops that are created as loops: 
     * i.e. those with trip count > 1. 
     */

    public static boolean getForLoopCallers = false;
    
    /**
     * Returns a block with a loop counter declaration and a for loop
     * that executes <b>body</b> for <b>count</b> number of times.  If the
     * count is just one, then return the body instead of a loop.
     */
    public static JStatement makeForLoop(JStatement body, int count) {
        return makeForLoop(body, new JIntLiteral(count));
    }

    /**
     * Returns a block with a loop counter declaration and a for loop
     * that executes <b>body</b> for <b>count</b> number of times.
     * <br/>
     * Optimizes output in cases where <b>count</b> is a literal and == 0 or == 1.
     */
    public static JStatement makeForLoop(JStatement body, JExpression count) {
        if (count instanceof JIntLiteral) {
            int intCount = ((JIntLiteral)count).intValue();
            if (intCount<=0) {
                // if the count isn't positive, return an empty statement
                return new JEmptyStatement(null, null); 
            } else if (intCount==1) {
                // if the count is one, then just return the body
                return body;
            }
        }
        return makeForLoop(body, count,         
                           new JVariableDefinition(/* where */ null,
                                                   /* modifiers */ 0,
                                                   /* type */ CStdType.Integer,
                                                   /* ident */ 
                                                   LoweringConstants.getUniqueVarName(),
                                                   /* initializer */
                                                   new JIntLiteral(0)));
    }

    /**
     * Returns a block with a loop counter declaration and a for loop
     * that executes <b>body</b> for <b>count</b> number of times.  Executes in
     * the forward direction, counting up from 0 to count-1 with
     * <b>loopIndex</b> as the loop counter.
     * <br/>
     * Optimizes if loopIndex == 0 or loopIndex == 1
     * <br/>
     * Note that <b>loopIndex</b> should not appear in a different variable
     * decl; it will get one in this routine.
     */
    public static JStatement makeForLoop(JStatement body, JExpression count, final JVariableDefinition loopIndex) {
        // make sure we start counting from 0
        loopIndex.setInitializer(new JIntLiteral(0));
        // make a declaration statement for our new variable
        JVariableDeclarationStatement varDecl =
            new JVariableDeclarationStatement(null, loopIndex, null);

        // avoid loops for certain loop bounds
        if (count instanceof JIntLiteral) {
            int intCount = ((JIntLiteral)count).intValue();
            if (intCount<=0) {
                // if the count isn't positive, return the variable
                // decl.  Return this rather than an empty statement
                // in case some later code depends on the value of
                // this variable on loop exit.  However, rstream seems
                // not to need the VarDecl, so return only an empty
                // statement there.
                return (KjcOptions.rstream ? (JStatement)(new JEmptyStatement()) : (JStatement)varDecl);
            } else if (intCount==1) {
                // replace references to the loop counter with the
                // constant 0.  (while constant prop does this
                // automatically in other backends, causes problems in
                // rstream, who doesn't want a var decl included.)
                body.accept(new SLIRReplacingVisitor() {
                        public Object visitLocalVariableExpression(JLocalVariableExpression self,
                                                                   String ident) {
                            if (self.getVariable()==loopIndex) { return new JIntLiteral(0); }
                            return self;
                        }});
                return body;
            }
        }

        // from here on out, we will explicitly assign the loop index
        // to be zero in the initializer of the for loop, so do not do
        // it in the var decl (might result in double assignment)
        loopIndex.setInitializer(null);
        
        // make a test if our variable is less than <pre>count</pre>
        JExpression cond = 
            new JRelationalExpression(null,
                                      Constants.OPE_LT,
                                      new JLocalVariableExpression(null, loopIndex),
                                      count);
        // make an increment for <pre>var</pre>
        JStatement incr = 
            new JExpressionStatement(null,
                                     new JPostfixExpression(null,
                                                            Constants.
                                                            OPE_POSTINC,
                                                            new JLocalVariableExpression(null, loopIndex)),
                                     null);

        JavaStyleComment comments[] = null;
        
        // debugging: print caller, and insert caller as comment on returned for loop
        if (getForLoopCallers) {
            String caller;
            {
                Throwable tracer = new Throwable();
                tracer.fillInStackTrace();
                caller = "Utils.makeForLoop(" + count + "): "
                        + tracer.getStackTrace()[1].toString();
            }
            JavaStyleComment comment = new JavaStyleComment(caller, true, false, false);
            comments = new JavaStyleComment[] {comment};

            // should have separate flag for this...
            System.err.println(caller);

        }

        // make the for statement
        JStatement forStatement = 
            new JForStatement(/* tokref */ null,
                              //for rstream put the vardecl in the init of the for loop
                              /* init */ (KjcOptions.rstream ? (JStatement) varDecl : 
                                          // otherwise, put an assignment in the init section so that the work estimate can detect how many times this for loop fires
                                          new JExpressionStatement(new JAssignmentExpression(new JLocalVariableExpression(loopIndex), new JIntLiteral(0)))),
                              cond,
                              incr,
                              body,
                              comments);
        // return the block
        JStatement[] statements = {varDecl, forStatement};
        //return just the for statement for rstream
        return (KjcOptions.rstream ? forStatement : new JBlock(null, statements, null));
    }

    /**
     * Returns a block with a loop counter declaration and a for loop
     * that executes <b>body</b> for <b>count</b> number of times.  Executes in
     * the backwards direction, counting down from count-1 to zero
     * with <b>loopIndex</b> as the loop counter.  
     * <br/>
     * Optimizes in the cases where count is literal and == 0 or == 1.
     * <br/>
     * Note that <b>loopIndex</b> should not appear in a different variable
     * decl; it will get one in this routine.
     */
    public static JStatement makeCountdownForLoop(JStatement body, JExpression count, JVariableDefinition loopIndex) {
        // make sure we start at count-1
        loopIndex.setInitializer(new JMinusExpression(null, count, new JIntLiteral(1)));
        // make a declaration statement for our new variable
        JVariableDeclarationStatement varDecl =
            new JVariableDeclarationStatement(null, loopIndex, null);

        // avoid loops for certain loop bounds
        if (count instanceof JIntLiteral) {
            int intCount = ((JIntLiteral)count).intValue();
            if (intCount<=0) {
                // if the count isn't positive, return the variable
                // decl.  Return this rather than an empty statement
                // in case some later code depends on the value of
                // this variable on loop exit.  However, rstream seems
                // not to need the VarDecl, so return only an empty
                // statement there.
                return (KjcOptions.rstream ? (JStatement)new JEmptyStatement() : (JStatement)varDecl);
            } else if (intCount==1) {
                // if the count is one, then return the decl and the
                // body (but rstream doesn't need the decl).
                return (KjcOptions.rstream ? body :
                        new JBlock(null, new JStatement[] { varDecl, body }, null));
            }
        }

        // from here on out, we will explicitly assign the loop index
        // to be count-1 in the initializer of the for loop, so do not
        // do it in the var decl (might result in double assignment)
        loopIndex.setInitializer(null);
        
        // make a test if our variable is less than <pre>count</pre>
        JExpression cond = 
            new JRelationalExpression(null,
                                      Constants.OPE_GE,
                                      new JLocalVariableExpression(null, loopIndex),
                                      new JIntLiteral(0));
        // make a decrement for <pre>var</pre>
        JStatement incr = 
            new JExpressionStatement(null,
                                     new JPostfixExpression(null,
                                                            Constants.
                                                            OPE_POSTDEC,
                                                            new JLocalVariableExpression(null, loopIndex)),
                                     null);

        JavaStyleComment comments[] = null;
        
        // debugging: print caller, and insert caller as comment on returned for loop
        if (getForLoopCallers) {
            String caller;
            {
                Throwable tracer = new Throwable();
                tracer.fillInStackTrace();
                caller = "ClusterExecution.makeForLoop(" + count + "): "
                        + tracer.getStackTrace()[1].toString();
            }
            JavaStyleComment comment = new JavaStyleComment(caller, true, false, false);
            comments = new JavaStyleComment[] {comment};

            // should have separate flag for this...
            System.err.println(caller);

        }
        

        // make the for statement
        JStatement forStatement = 
            new JForStatement(/* tokref */ null,
                              /* init */ new JExpressionStatement(new JAssignmentExpression(new JLocalVariableExpression(loopIndex), new JMinusExpression(null, count, new JIntLiteral(1)))),
                              cond,
                              incr,
                              body,
                              comments);
        // return the block
        JStatement[] statements = {varDecl, forStatement};
        return new JBlock(null, statements, null);
    }

    /**
     * Returns a for loop that uses local <b>var</b> to count
     * <b>count</b> times with the body of the loop being <b>body</b>.  
     * If count is non-positive, just returns empty (!not legal in the general case)
     * 
     * @param body The body of the for loop.  (null OK: returns empty statement)
     * @param local The local to use as the index variable.
     * @param count The trip count of the loop.
     * 
     * @return The for loop.
     */
    public static JStatement makeForLoopLocalIndex(JStatement body,
            JVariableDefinition local,
            JExpression count) {
        if (body == null)
            return new JEmptyStatement(null, null);
    
        // make init statement - assign zero to <pre>var</pre>.  We need to use
        // an expression list statement to follow the convention of
        // other for loops and to get the codegen right.
        JExpression initExpr[] = {
            new JAssignmentExpression(null,
                    new JLocalVariableExpression(local),
                    new JIntLiteral(0)) };
        JStatement init = new JExpressionListStatement(null, initExpr, null);
        // if count==0, just return init statement
        if (count instanceof JIntLiteral) {
            int intCount = ((JIntLiteral)count).intValue();
            if (intCount<=0) {
                // return assignment statement
                return new JEmptyStatement(null, null);
            }
        }
        // make conditional - test if <pre>var</pre> less than <pre>count</pre>
        JExpression cond = 
            new JRelationalExpression(null,
                                      Constants.OPE_LT,
                                      new JLocalVariableExpression(local),
                                      count);
        JExpression incrExpr = 
            new JPostfixExpression(null, 
                                   Constants.OPE_POSTINC, 
                                   new JLocalVariableExpression(local));
        JStatement incr = 
            new JExpressionStatement(null, incrExpr, null);
    
        return new JForStatement(null, init, cond, incr, body, null);
    }

    /**
     * Returns a for loop that uses field <b>var</b> to count
     * <b>count</b> times with the body of the loop being <b>body</b>.  
     * If count is non-positive, just returns empty (!not legal in the general case)
     * 
     * @param body The body of the for loop.  (null OK: returns empty statement)
     * @param var The field to use as the index variable.
     * @param count The trip count of the loop.
     * 
     * @return The for loop.
     */
    public static JStatement makeForLoopFieldIndex(JStatement body,
            JVariableDefinition var,
            JExpression count) {
        if (body == null)
            return new JEmptyStatement(null, null);
    
        // make init statement - assign zero to <pre>var</pre>.  We need to use
        // an expression list statement to follow the convention of
        // other for loops and to get the codegen right.
        JExpression initExpr[] = {
            new JAssignmentExpression(null,
                                      new JFieldAccessExpression(null, 
                                                                 new JThisExpression(null),
                                                                 var.getIdent()),
                                      new JIntLiteral(0)) };
        JStatement init = new JExpressionListStatement(null, initExpr, null);
        // if count==0, just return init statement
        if (count instanceof JIntLiteral) {
            int intCount = ((JIntLiteral)count).intValue();
            if (intCount<=0) {
                // return assignment statement
                return new JEmptyStatement(null, null);
            }
        }
        // make conditional - test if <pre>var</pre> less than <pre>count</pre>
        JExpression cond = 
            new JRelationalExpression(null,
                                      Constants.OPE_LT,
                                      new JFieldAccessExpression(null, 
                                                                 new JThisExpression(null),
                                                                 var.getIdent()),
                                      count);
        JExpression incrExpr = 
            new JPostfixExpression(null, 
                                   Constants.OPE_POSTINC, 
                                   new JFieldAccessExpression(null, new JThisExpression(null),
                                                              var.getIdent()));
        JStatement incr = 
            new JExpressionStatement(null, incrExpr, null);
    
        return new JForStatement(null, init, cond, incr, body, null);
    }

    /**
     * If <pre>type</pre> is void, then return <pre>int</pre> type; otherwise return
     * <pre>type</pre>.  This is a hack to get around the disallowance of void
     * arrays in C--should fix this better post-asplos.
     */
    public static CType voidToInt(CType type) {
        return type==CStdType.Void ? CStdType.Integer : type;
    }

    /**
     * Returns value of environment variable named <pre>var</pre>, or null if
     * the variable is undefined.
     */
    public static String getEnvironmentVariable(String var) {
        try {
            String OS = System.getProperty("os.name").toLowerCase();
            String command = (OS.indexOf("windows") > -1 ? "set" : "env");
            Process p = Runtime.getRuntime().exec(command);
            BufferedReader br = new BufferedReader ( new InputStreamReader( p.getInputStream() ) );
            String line;
            while((line = br.readLine()) != null) {
                int pos = line.indexOf('=');
                String key = line.substring(0, pos);
                if (key.equals(var)) {
                    return line.substring(pos+1);
                }
            }
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            Utils.fail("I/O exception trying to retrieve environment variable \"" + var + "\"");
            return null;
        }
    }

    /**
     * Record the number of subgraphs for future dot filenames.
     */
    public static void setupDotFileName(int _numSubGraphs) 
    {
        numSubGraphs = _numSubGraphs;
    }
    private static int numSubGraphs = 1;
    /**
     * Make a name for a dot file from a supplied prefix and the name
     * of the top-level SIRStream.
     */
    public static String makeDotFileName(String prefix, SIRStream strName) 
    {
        if (numSubGraphs == 1) {
            // if only one graph, don't print "Toplevel0" -- be
            // consistent with other backends compiling static rates
            return prefix + ".dot";
        } else {
            return prefix + (strName != null ? strName.getIdent() : "") + ".dot";
        }
    }
    

    /**
     * Asks the question "Is a peek present in this unit of code?".
     * <br/>
     * Kopi2SIR seets the peek rate for a filter = max(declared peek rate, declared pop rate).
     * For some optimizations it is of interest whether the code actually contains any
     * peeks, or if it only contains pops.
     * 
     * @param filter  Anything that implements SIRCodeUnit so that its methods can be scanned.
     * @return true if a peek is found in any method, false otherwise.
     */
    public static boolean hasPeeks(SIRCodeUnit filter) {
        /** Extend Error here rather than Exception because overridden visitor
         *  can not declare it as an exception */
        class BreakOutException extends Error{};
    
        try {
            for (JMethodDeclaration meth : filter.getMethods()) {
                meth.accept(new SLIREmptyVisitor(){
                    @Override
                    public void visitPeekExpression(SIRPeekExpression self,
                            CType tapeType,
                            JExpression arg) {
                        throw new BreakOutException();
                    }
                });
            }
        } catch (BreakOutException e) {
            return true;
        }
        return false;
    }

    // ----------------------------------------------------------------------
    // DATA MEMBERS
    // ----------------------------------------------------------------------

    public static final LinkedList<SIRStream> EMPTY_LIST = new LinkedList<SIRStream>();


    /** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

    /** Returns a deep clone of this object. */
    public Object deepClone() { at.dms.util.Utils.fail("Error in auto-generated cloning methods - deepClone was called on an abstract class."); return null; }

    /** Clones all fields of this into <pre>other</pre> */
    protected void deepCloneInto(at.dms.util.Utils other) {
    }

    /** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}
