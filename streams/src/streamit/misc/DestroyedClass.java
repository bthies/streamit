/*
 * DestroyedClass.java
 *
 * Created on May 31, 2001, 4:50 PM
 */

package streamit.misc;

import java.lang.reflect.*;

public class DestroyedClass extends Misc
{
    private boolean Destroyed = false;
    private static Class DestroyedClass;


    // The class initializer initializes thisClass
    // to the appropriate value
    static {
        try
        {
            DestroyedClass = Class.forName ("streamit.misc.DestroyedClass");
        }
        catch (ClassNotFoundException error)
        {
            // This REALLY should not happen
            // just assert
            SASSERT (false);
        }
    }

    // The finalizer checks that the class has already been Destroyed,
    // and if not, it Destroys it
    final protected void finalize ()
    {
        if (!Destroyed) Destroy ();
        Destroyed = true;
    }

    // DELETE member functions will be used
    // to provide the actual destructors
    public void DELETE () { }

    void Destroy ()
    {
        // make sure the object hasn't been Destroyed yet
        ASSERT (!Destroyed);
        Destroyed = true;

        Class objectClass = this.getClass ();
        ASSERT (objectClass != null);

        for ( ; objectClass != DestroyedClass ; objectClass = objectClass.getSuperclass ())
        {
            Method deleteMethod = null;

            try
            {
                deleteMethod = objectClass.getDeclaredMethod ("DELETE", null);
                ASSERT (deleteMethod != null);

                deleteMethod.invoke (this, null);
            }
            catch (NoSuchMethodException error)
            {
                // do nothing, this isn't really an error
                // just an annoying Java-ism
            }

            // I hope I can just catch the rest of the exceptions here...
            catch (Throwable error)
            {
                // This REALLY shouldn't happen
                // just assert
                ASSERT (false);
            }
        }
    }
}
