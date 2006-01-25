package at.dms.kjc.sir.lowering;

import java.util.*;
import at.dms.kjc.*;
import at.dms.kjc.iterator.*;
import at.dms.util.*;
import at.dms.kjc.sir.*;

/**
 * This class expands array initializers that read from files into
 * static constants.  For example:
 *
 *   int[5] foo = init_array_1D_int("foo.txt", 5);
 *
 * if foo.txt contains:
 *
 *   1
 *   2
 *   3
 *   4
 *   5
 *
 * then this class translates the declaration to:
 *
 *   int[5] foo = {1,2,3,4,5};
 *
 */
public class ArrayInitExpander {

    /**
     * Returns version of <str> with array initializers expanded.
     */
    public static void doit(SIRStream str) {
        IterFactory.createFactory().createIter(str).accept(new EmptyStreamVisitor() {
                // visit all streams
                public void preVisitStream(SIRStream self,
                                           SIRIterator iter) {
                    // iterate over all fields
                    JFieldDeclaration[] fields = self.getFields();
                    for (int i=0; i<fields.length; i++) {
                        // try to expand each one
                        ArrayInitExpander.expand(fields[i]);
                    }
                    // in uniprocessor backend, some definitions might
                    // be copied to init.
                    if (self.getInit()!=null) {
                        ArrayInitExpander.expand(self.getInit());
                    }
                }
            });
    }

    /**
     * Expands all array calls in <phylum>.
     */
    private static void expand(JPhylum phylum) {
        // translate all relevant method calls to static arrays
        phylum.accept(new SLIRReplacingVisitor() {
                public Object visitMethodCallExpression(JMethodCallExpression self,
                                                        JExpression prefix,
                                                        String ident,
                                                        JExpression[] args) {
                    // call super
                    self = (JMethodCallExpression)super.visitMethodCallExpression(self, prefix, ident, args);
                    ident = self.getIdent();
                    args = self.getArgs();

                    // if array init, translate
                    if (ident.indexOf("init_array")!=-1) {
                        return ArrayInitExpander.makeArrayInit(self);
                    } else {
                        return self;
                    }
                }
            });
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
    private static JArrayInitializer makeArrayInit(JMethodCallExpression exp) {
        String funcName = exp.getIdent();
        String filename = null;
        int size = 0;

        // GET PARAMS -------

        // first param should be string
        if (exp.getArgs()[0] instanceof JStringLiteral) {
            // for some reason the string literal has quotes on either
            // side
            filename = ((JStringLiteral)exp.getArgs()[0]).stringValue();
        } else {
            System.err.println("Error: expected first argument to " + funcName + " to be a String (the filename)");
            System.exit(1);
        }

        // second param should be an int
        if (exp.getArgs()[1] instanceof JIntLiteral) {
            size = ((JIntLiteral)exp.getArgs()[1]).intValue();
        } else {
            System.err.println("Error: expected second argument to " + funcName + " to be an integer (the size)");
            System.exit(1);
        }

        // LOAD ARRAY -------

        // load int array values
        if (funcName.equals("init_array_1D_int")) {
            int[] array = streamit.misc.Misc.init_array_1D_int(filename, size);
            // make array of expressions
            JExpression[] expArray = new JExpression[size];
            for (int i=0; i<array.length; i++) {
                expArray[i] = new JIntLiteral(array[i]);
            }
            // make result
            JArrayInitializer result = new JArrayInitializer(expArray);
            result.setType(new CArrayType(CStdType.Integer, 1, new JExpression[] { new JIntLiteral(array.length) } ));
            return result;
        }

        // load float array values
        if (funcName.equals("init_array_1D_float")) {
            float[] array = streamit.misc.Misc.init_array_1D_float(filename, size);
            // make array of expressions
            JExpression[] expArray = new JExpression[size];
            for (int i=0; i<array.length; i++) {
                expArray[i] = new JFloatLiteral(array[i]);
            }
            // make result
            JArrayInitializer result = new JArrayInitializer(expArray);
            result.setType(new CArrayType(CStdType.Float, 1, new JExpression[] { new JIntLiteral(array.length) } ));
            return result;
        }

        // unrecognized function type
        System.err.println("Unrecognized array initializer: " + funcName);
        System.exit(1);
        return null;
    }

}
