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
        
        System.err.println ("An ASSERT has failed.  Exiting.");
        System.exit (0);
    }
}
