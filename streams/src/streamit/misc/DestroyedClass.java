/*
 * DestroyedClass.java
 *
 * Created on May 31, 2001, 4:50 PM
 */

package streamit;

import java.lang.*;
import java.lang.reflect.*;

public class DestroyedClass extends AssertedClass
{
    private boolean destroyed = false;
    private static Class destroyedClass;
    
    
    // The class initializer initializes thisClass
    // to the appropriate value
    {
        try
        {
            destroyedClass = Class.forName ("streamit.DestroyedClass");
        }
        catch (ClassNotFoundException error)
        {
            // This REALLY should not happen
            // just assert
            ASSERT (false);
        }
    }
        
    // The finalizer checks that the class has already been destroyed,
    // and if not, it destroys it
    protected void finalize () 
    {
        if (!destroyed) Destroy ();
        destroyed = true;
    }

    // Delete member functions will be used
    // to provide the actual destructors
    void Delete () { }
    
    void Destroy ()
    {
        // make sure the object hasn't been destroyed yet
        ASSERT (!destroyed);
        destroyed = true;
        
        Class objectClass = this.getClass ();
        ASSERT (objectClass != null);
        
        for ( ; objectClass != destroyedClass ; objectClass = objectClass.getSuperclass ())
        {
            Method deleteMethod = null;
            
            try
            {
                deleteMethod = objectClass.getDeclaredMethod ("Delete", null);
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
