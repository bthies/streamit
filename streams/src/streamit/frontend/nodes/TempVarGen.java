package streamit.frontend.nodes;

/**
 * Generate a sequence of numbered temporaary variable.  These will have
 * names like __temp_var_1, __temp_var_2, ....  These will primarily be
 * used for separating complex arithmetic, and for serializing pop and
 * peek operations.  That is, code like
 *
 * <pre>
 * push(sum - seq + seq * 1.0i);
 * </pre>
 *
 * would get converted into
 *
 * <pre>
 * complex float __temp_var_1;
 * __temp_var_1.real = sum - seq;
 * __temp_var_1.imag = seq;
 * push __temp_var_1;
 * </pre>
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: TempVarGen.java,v 1.1 2003-07-30 20:31:32 dmaze Exp $
 */
public class TempVarGen
{
    private int seq;

    /**
     * Create a new temporary variable generator, which will generate
     * variables starting with 0.
     */
    public TempVarGen()
    {
        seq = 0;
    }
    
    /**
     * Get the next variable number, and increment the counter.
     *
     * @return the number of the next variable
     */
    public int nextVarNum()
    {
        int num = seq;
        seq++;
        return num;
    }

    /**
     * Get the name of a variable for a particular number.
     *
     * @param num  variable number to get a name for
     * @return     name of the variable
     */
    public static String varName(int num)
    {
        return "__temp_var_" + num;
    }

    /**
     * Get the name of the next variable, and increment the counter.
     *
     * @return the name of the next variable
     */
    public String nextVar()
    {
        return varName(nextVarNum());
    }
}

