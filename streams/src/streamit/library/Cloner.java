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

package streamit.library;

import java.io.*;
import java.util.*;

public class Cloner extends streamit.misc.DestroyedClass {
    /**
     * Returns the deep copy of an object.
     */ 
    public static Object doCopy(Object oldObj)
    {
        ObjectOutputStream oos = null;
        ObjectInputStream ois = null;
        try
            {
                // get an output stream ready
                ByteArrayOutputStream bos = 
                    new ByteArrayOutputStream();
                oos = new ObjectOutputStream(bos);
                // serialize and pass the object
                oos.writeObject(oldObj);  
                oos.flush();              
                ByteArrayInputStream bin = 
                    new ByteArrayInputStream(bos.toByteArray()); 
                ois = new ObjectInputStream(bin);                  
                // return the new object
                oos.close();
                ois.close();
                return ois.readObject(); 
            }
        catch(Exception e)
            {
                System.err.println("Exception in library Cloner = " + e);
                e.printStackTrace();
                System.exit(-1);
     
            }
        return null;
    }
}
