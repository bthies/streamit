/*
 * TempVarGen.java: create sequentially numbered temporary variables
 * David Maze <dmaze@cag.lcs.mit.edu>
 * $Id: TempVarGen.java,v 1.1 2002-07-10 18:10:28 dmaze Exp $
 */

package streamit.frontend.tojava;

import java.util.ArrayList;
import java.util.List;


/**
 * Generate a sequence of numbered temporaary variable.  These will have
 * names like __temp_var_1, __temp_var_2, ....  These will primarily be
 * used for separating complex arithmetic, and for serializing pop and
 * peek operations.  That is, code like
 *
 *   push sum - seq + seq * 1.0i;
 *
 * would get converted into
 *
 *   complex float __temp_var_1;
 *   __temp_var_1.real = sum - seq;
 *   __temp_var_1.imag = seq;
 *   push __temp_var_1;
 *
 * This class also tracks the types of any temporary objects created.
 */
public class TempVarGen
{
    private int seq;
    private List types;

    /** Create a new temporary variable generator, which will generate
     * variables starting with 0. */
    public TempVarGen()
    {
        seq = 0;
        types = new ArrayList();
    }
    
    /** Get the next variable number, and increment the counter. */
    public int nextVar(String type)
    {
        int num = seq;
        types.add(type);
        seq++;
        return num;
    }

    /** Get the name of a variable for a particular number. */
    public static String varName(int num)
    {
        return "__temp_var_" + num;
    }

    /** Get a full declaration for a particular number. */
    public String varDecl(int num)
    {
        String type = (String)types.get(num);
        return type + " " + varName(num) + ";";
    }

    /** Get an initialization for a particular number. */
    public String varInit(int num)
    {
        String type = (String)types.get(num);
        if (!Character.isUpperCase(type.charAt(0)))
            return "";
        return varName(num) + " = new " + type + "();";
    }

    /** Get declarations for every temporary variable that has been
     * declared. */
    public String allDecls()
    {
        String result = "";
        for (int i = 0; i < seq; i++)
            result += varDecl(i) + "\n";
        for (int i = 0; i < seq; i++)
            result += varInit(i) + "\n";
        return result;
    }
}

