/*
 * AssertedClass.java
 *
 * Created on May 31, 2001, 6:27 PM
 */

package streamit;

public class AssertedClass
{

    // assert the condition
    // if the condition is false, print an error and exit the program
    public static void ASSERT (boolean condition)
    {
        if (condition) return;

        // condition is not satisifed:
        // print an error and exit.

        System.err.println ("An ASSERT has failed.  Exiting.\n\n");
	new RuntimeException().printStackTrace();
        System.exit (0);
    }

    public static void ASSERT (char cond)
    {
        ASSERT (cond != 0);
    }
    public static void ASSERT (short cond)
    {
        ASSERT (cond != 0);
    }
    public static void ASSERT (int cond)
    {
        ASSERT (cond != 0);
    }
    public static void ASSERT (long cond)
    {
        ASSERT (cond != 0);
    }
    public static void ASSERT (float cond)
    {
        ASSERT (cond != 0);
    }
    public static void ASSERT (double cond)
    {
        ASSERT (cond != 0);
    }
    public static void ASSERT (Object cond)
    {
        ASSERT (cond != null);
    }

    public static void ERROR (String error)
    {
        System.err.println (error);
        ASSERT (false);
    }
}
