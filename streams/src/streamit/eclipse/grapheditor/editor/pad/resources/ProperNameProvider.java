/*
 * This file is part of the Librarian project
 * (C) 2002 The Librarian Community
 *
 * Please visit our website at http://librarian.sf.de
 */
package streamit.eclipse.grapheditor.editor.pad.resources;

import java.util.Enumeration;

/**Container for proper names. (e.g. Look and Feel names are
 * equal in any language, sothat this container.)
 *
 * @author <a href="mailto:Sven.Luzar@web.de">Sven luzar</a>
 * @version 1.0
 */

public interface ProperNameProvider {

  /** Returns the Keys of this proper name container
   *
   */
  public abstract  Enumeration getKeys()  ;

  /** Returns the proper name for the locale key.
   *
   */
  public String getString(String key) ;

}