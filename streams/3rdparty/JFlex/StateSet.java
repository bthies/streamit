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
 * A set of NFA states (= integers). 
 *
 * Very similar to java.util.BitSet, but is faster and doesn't crash
 *
 * @author Gerwin Klein
 * @version JFlex 1.3.2, $Revision: 1.1 $, $Date: 2001-08-30 16:25:34 $
 */
final public class StateSet {

  private final boolean DEBUG = false;

  public final static StateSet EMPTY = new StateSet();


  final static int BITS = 6;
  final static int MASK = (1<<BITS)-1;
  
  long bits[];
  
  
  public StateSet() {
    bits = new long[4]; // space for 4*64 = 256 states
  }


  public StateSet(int state) {
    this();
    addState(state);
  }


  public StateSet(StateSet set) {
    bits = new long[set.bits.length];
    System.arraycopy(set.bits, 0, bits, 0, set.bits.length);
  }


  public void addState(int state) {
    if (DEBUG) {
      Out.dump("StateSet.addState("+state+") start");
      Out.dump("Set is : "+this);
    }
    
    resize(state);
    bits[state >> BITS] |= (1L << (state & MASK));
    
    if (DEBUG) {
      Out.dump("StateSet.addState("+state+") end");
      Out.dump("Set is : "+this);
    }
  }


  private int nbits2size (int nbits) {
    return ((nbits >> BITS) + 1);
  }


  private void resize(int nbits) {
    int needed = nbits2size(nbits);

    if (needed < bits.length) return;
         
    long newbits[] = new long[Math.max(bits.length*2,needed)];
    System.arraycopy(bits, 0, newbits, 0, bits.length);
    
    bits = newbits;
  }


  public void clear() {    
    int l = bits.length;
    for (int i = 0; i < l; i++) bits[i] = 0;
  }

  public boolean isElement(int state) {
    int index = state >> BITS;
    if (index >= bits.length)  return false;
    return (bits[index] & (1L << (state & MASK))) != 0;
  }

  /**
   * Returns one element of the set and removes it. 
   *
   * Precondition: the set is not empty.
   */
  public int getAndRemoveElement() {
    int i = 0;
    int o = 0;
    long m = 1;
    
    while (bits[i] == 0) i++;
    
    while ( (bits[i] & m) == 0 ) {
      m<<= 1;
      o++;
    }
    
    bits[i]&= ~m;
    
    return (i << BITS) + o;
  }

  public void remove(int state) {
    resize(state);
    bits[state >> BITS] &= ~(1L << (state & MASK));
  }

  /**
   * Returns the set of elements that contained are in the specified set
   * but are not contained in this set.
   */
  public StateSet complement(StateSet set) {
    
    if (set == null) return null;
    
    StateSet result = new StateSet();
    
    result.bits = new long[set.bits.length];
    
    int i;
    int m = Math.min(bits.length, set.bits.length);
    
    for (i = 0; i < m; i++) {
      result.bits[i] = ~bits[i] & set.bits[i];
    }
    
    if (bits.length < set.bits.length) 
      System.arraycopy(set.bits, m, result.bits, m, result.bits.length-m);
    
    if (DEBUG) 
      Out.dump("Complement of "+this+Out.NL+"and "+set+Out.NL+" is :"+result);
    
    return result;
  }

  public boolean add(StateSet set) {
    
    if (DEBUG) Out.dump("StateSet.add("+set+") start");

    if (set == null) return false;

    if (bits.length < set.bits.length) {
      long newbits[] = new long[set.bits.length];
      System.arraycopy(bits, 0, newbits, 0, bits.length);
      
      bits = newbits;
    }
    
    boolean changed = false;

    for (int i = 0; i < set.bits.length; i++) {
      long n = bits[i] | set.bits[i];
      if ( n != bits[i] ) changed = true;
      bits[i] = n;
    }
    
    if (DEBUG) {
      Out.dump("StateSet.add("+set+") end");
      Out.dump("Set is : "+this);
    }

    return changed;
  }
  


  public boolean containsSet(StateSet set) {

    if (DEBUG)
      Out.dump("StateSet.containsSet("+set+"), this="+this);

    int i;
    int min = Math.min(bits.length, set.bits.length);
    
    for (i = 0; i < min; i++) 
      if ( (bits[i] & set.bits[i]) != set.bits[i] ) return false;
    
    for (i = min; i < set.bits.length; i++)
      if ( set.bits[i] != 0 ) return false;
    
    return true;
  }



  /**
   * @throws ClassCastException if b is not a StateSet
   * @throws NullPointerException if b is null
   */
  public boolean equals(Object b) {

    int i = 0;
    int l1,l2;
    StateSet set = (StateSet) b;

    if (DEBUG) Out.dump("StateSet.equals("+set+"), this="+this);

    l1 = bits.length;
    l2 = set.bits.length;

    if (l1 <= l2) {      
      while (i < l1) {
        if (bits[i] != set.bits[i]) return false;
        i++;
      }
      
      while (i < l2) 
        if (set.bits[i++] != 0) return false;
    }
    else {
      while (i < l2) {
        if (bits[i] != set.bits[i]) return false;
        i++;
      }
      
      while (i < l1) 
        if (bits[i++] != 0) return false;
    }

    return true;
  }

  public int hashCode() {
    long h = 1234;
    int  i = bits.length;

    while (--i >= 0) 
      h ^= bits[i] * i;

    return (int)((h >> 32) ^ h);
  } 


  public StateSetEnumerator states() {
    return new StateSetEnumerator(this);
  }


  public boolean containsElements() {
    for (int i = 0; i < bits.length; i++)
      if (bits[i] != 0) return true;
      
    return false;
  }

  
  public StateSet copy() {
    StateSet set = new StateSet();
    set.bits = new long[bits.length];
    System.arraycopy(bits, 0, set.bits, 0, bits.length);
    return set;
  }

  
  public String toString() {
    StateSetEnumerator enum = states();

    StringBuffer result = new StringBuffer("{");

    if ( enum.hasMoreElements() ) result.append(""+enum.nextElement());

    while ( enum.hasMoreElements() ) {
      int i = enum.nextElement();
      result.append( ", "+i);
    }

    result.append("}");

    return result.toString();
  }
}
