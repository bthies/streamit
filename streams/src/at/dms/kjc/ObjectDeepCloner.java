//===========================================================================
//
//   FILE: ObjectDeepCloner.java:
//   
//   Author: Michael Gordon
//   Date: Wed Oct 17 14:03:33 2001
//
//   Function:  Deep clone using Serializable
//
//===========================================================================

package at.dms.kjc;

import java.io.*;
import java.util.*;
import java.awt.*;


/**
 * This class implements general deep cloning using the serializable interface
 */
public class ObjectDeepCloner
{
   // so that nobody can accidentally create an ObjectCloner object
   private ObjectDeepCloner(){}
  
    /**
     * Returns the deep clone of an object
     */ 
   static public Object deepCopy(Object oldObj)
   {
       if (oldObj instanceof JLocalVariable)
       	   return oldObj;

      ObjectOutputStream oos = null;
      ObjectInputStream ois = null;
      try
      {
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
         System.err.println("Exception in ObjectCloner = " + e);
	 System.exit(-1);
	 
      }
      return null;
   }
}
