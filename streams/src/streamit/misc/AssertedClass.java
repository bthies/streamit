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
    public static void ASSERT (boolean condition, Object object)
    {
        if (condition) return;

        // condition is not satisifed:
        // print an error and exit.

        System.err.println ("An ASSERT has failed in class " + (object != null ? object.getClass ().getName () : "(unknown)") + ".  Exiting.\n\n");
        new RuntimeException().printStackTrace();
        System.exit (1);
    }

    public void ASSERT(boolean condition)
    {
        ASSERT (condition, this);
    }

    public void ASSERT (char cond)
    {
        ASSERT (cond != 0, this);
    }
    public void ASSERT (short cond)
    {
        ASSERT (cond != 0, this);
    }
    public void ASSERT (int cond)
    {
        ASSERT (cond != 0, this);
    }
    public void ASSERT (long cond)
    {
        ASSERT (cond != 0, this);
    }
    public void ASSERT (float cond)
    {
        ASSERT (cond != 0, this);
    }
    public void ASSERT (double cond)
    {
        ASSERT (cond != 0, this);
    }
    public void ASSERT (Object cond)
    {
        ASSERT (cond != null, this);
    }

    public void ERROR (String error)
    {
        System.err.println (error);
        ASSERT (false, this);
    }

    // static versions of asserts:
    public static void SASSERT(boolean condition)
    {
        ASSERT (condition, null);
    }

    public static void SASSERT (char cond)
    {
        ASSERT (cond != 0, null);
    }
    public static void SASSERT (short cond)
    {
        ASSERT (cond != 0, null);
    }
    public static void SASSERT (int cond)
    {
        ASSERT (cond != 0, null);
    }
    public static void SASSERT (long cond)
    {
        ASSERT (cond != 0, null);
    }
    public static void SASSERT (float cond)
    {
        ASSERT (cond != 0, null);
    }
    public static void SASSERT (double cond)
    {
        ASSERT (cond != 0, null);
    }
    public static void SASSERT (Object cond)
    {
        ASSERT (cond != null, null);
    }

    public static void SERROR (String error)
    {
        System.err.println (error);
        ASSERT (false, null);
    }
}
