/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * JFlex 1.3.2                                                             *
 * Copyright (C) 1998-2001  Gerwin Klein <lsf@jflex.de>                    *
 * All rights reserved.                                                    *
 *                                                                         *
 * This program is free software; you can redistribute it and/or modify    *
 * it under the terms of the GNU General Public License. See the file      *
 * COPYRIGHT for more information.                                         *
 *                                                                         *
 * This program is distributed in the hope that it will be useful,         *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of          *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the           *
 * GNU General Public License for more details.                            *
 *                                                                         *
 * You should have received a copy of the GNU General Public License along *
 * with this program; if not, write to the Free Software Foundation, Inc., *
 * 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA                 *
 *                                                                         *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
package JFlex;

/**
 * Enumerates the states of a StateSet.
 *
 * @author Gerwin Klein
 * @version JFlex 1.3.2, $Revision: 1.1 $, $Date: 2001-08-30 16:25:34 $
 */
final public class StateSetEnumerator {

  private final static boolean DEBUG = false;

  private int index;
  private int offset;
  private long mask;

  private int current;
  private long [] bits;
  
  /**
   * creates a new StateSetEnumerator that is not yet associated
   * with a StateSet. hasMoreElements() and nextElement() will 
   * throw NullPointerException when used before reset()
   */  
  public StateSetEnumerator() {
  }

  public StateSetEnumerator(StateSet states) {
    reset(states);
  }

  public void reset(StateSet states) {
    bits    = states.bits;
    index   = 0;
    offset  = 0;
    mask    = 1;
    current = 0;

    while (index < bits.length && bits[index] == 0) 
      index++;

    if (index >= bits.length) return;
        
    while (offset <= StateSet.MASK && ((bits[index] & mask) == 0)) {
      mask<<= 1;
      offset++;
    }    
  }

  private void advance() {
    
    if (DEBUG) Out.dump("Advancing, at start, index = "+index+", offset = "+offset);

    do {
      offset++;
      mask<<= 1;
    } while (offset <= StateSet.MASK && ((bits[index] & mask) == 0));

    if (offset > StateSet.MASK) {
      int length = bits.length;

      do 
        index++;
      while (index < length && bits[index] == 0);
        
      if (index >= length) return;
        
      offset = 0;
      mask = 1;
      
      while (offset <= StateSet.MASK && ((bits[index] & mask) == 0)) {
        mask<<= 1;
        offset++;
      }
 
    }
  }

  public boolean hasMoreElements() {
    if (DEBUG) Out.dump("hasMoreElements, index = "+index+", offset = "+offset);
    return index < bits.length;
  }

  public int nextElement() {
    if (DEBUG) Out.dump("nextElement, index = "+index+", offset = "+offset);
    int x = (index << StateSet.BITS) + offset;
    advance();
    return x;
  }

}
