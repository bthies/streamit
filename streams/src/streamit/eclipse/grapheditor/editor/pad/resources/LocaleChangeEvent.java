/*
 * @(#)LocaleChangeEvent.java	1.0 23/01/02
 *
 * Copyright (C) 2003 Sven Luzar
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 */
package streamit.eclipse.grapheditor.editor.pad.resources;

import java.util.Locale;

/**If the Locale changes this Event will fired by the
 * Translator to all registered LocalChangeListeners.
 *
 * @author <a href="mailto:Sven.Luzar@web.de">Sven luzar</a>
 * @version 1.0
 */

public class LocaleChangeEvent {

  /**
   * Represents the old Locale or null
   */
  protected Locale oldLocale;

  /**
   * Represents the new Locale
   */
  protected Locale newLocale;

  /**
   * Creates a new Locale Change Event with the old and the
   * new Locale.
   */
  public LocaleChangeEvent(Locale oldLocale, Locale newLocale) {
    this.oldLocale = oldLocale;
    this.newLocale = newLocale;
  }

  /**
   * Returns the old Locale
   */
  public Locale getOldLocale(){
    return this.oldLocale;
  }

  /**
   * Returns the new Locale
   */
  public Locale getNewLocale(){
    return this.newLocale;
  }

}