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

/*
 * AssertedClass.java
 *
 * Created on May 31, 2001, 6:27 PM
 */

package streamit.misc;

public class AssertedClass
{

    public static void ASSERT(Object object, boolean condition)
    {
        ASSERT(object, condition, "No additional error message supplied");
    }

    // assert the condition
    // if the condition is false, print an error and exit the program
    public static void ASSERT(
        Object object,
        boolean condition,
        String message)
    {
        if (condition)
            return;

        // condition is not satisifed:
        // print an error and exit.

        System.err.println(
            "An ASSERT has failed in class "
                + (object != null
                    ? object.getClass().getName()
                    : "(unknown)")
                + ".  Exiting.\n\n"
                + message
                + "\n");

        // print the trace and throw an exception
        RuntimeException e = new RuntimeException();
        e.printStackTrace();
        throw e;
    }

    public void ASSERT(boolean condition)
    {
        ASSERT(this, condition);
    }

    public void ASSERT(boolean condition, String message)
    {
        ASSERT(this, condition, message);
    }

    public void ASSERT(char cond)
    {
        ASSERT(this, cond != 0);
    }
    public void ASSERT(short cond)
    {
        ASSERT(this, cond != 0);
    }
    public void ASSERT(int cond)
    {
        ASSERT(this, cond != 0);
    }
    public void ASSERT(long cond)
    {
        ASSERT(this, cond != 0);
    }
    public void ASSERT(float cond)
    {
        ASSERT(this, cond != 0);
    }
    public void ASSERT(double cond)
    {
        ASSERT(this, cond != 0);
    }
    public void ASSERT(Object cond)
    {
        ASSERT(this, cond != null);
    }

    public void ERROR(String error)
    {
        System.err.println(error);
        ASSERT(this, false);
    }

    public void ERROR(RuntimeException e)
    {
        SERROR(e);
    }

    public void ERROR(Throwable e)
    {
        SERROR(e);
    }

    // static versions of asserts:
    public static void SASSERT(boolean condition)
    {
        ASSERT(null, condition);
    }

    public static void SASSERT(char cond)
    {
        ASSERT(null, cond != 0);
    }
    public static void SASSERT(short cond)
    {
        ASSERT(null, cond != 0);
    }
    public static void SASSERT(int cond)
    {
        ASSERT(null, cond != 0);
    }
    public static void SASSERT(long cond)
    {
        ASSERT(null, cond != 0);
    }
    public static void SASSERT(float cond)
    {
        ASSERT(null, cond != 0);
    }
    public static void SASSERT(double cond)
    {
        ASSERT(null, cond != 0);
    }
    public static void SASSERT(Object cond)
    {
        ASSERT(null, cond != null);
    }

    public static void SERROR(RuntimeException e)
    {
        System.err.println("An error has occured: " + e.toString());
        e.printStackTrace(System.err);
        throw e;
    }

    public static void SERROR(Throwable e)
    {
        System.err.println("An error has occured: " + e.toString());
        e.printStackTrace(System.err);
        System.exit(0);
    }

    public static void SERROR(String error)
    {
        System.err.println(error);
        ASSERT(null, false);
    }
}
