/*
 * Copyright (C) 1990-2001 DMS Decision Management Systems Ges.m.b.H.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * $Id: BitSet.java,v 1.2 2002-12-11 23:56:11 karczma Exp $
 */

package at.dms.compiler.antlr.runtime;

import at.dms.util.InconsistencyException;

/**
 * A BitSet to replace java.util.BitSet.
 * Primary differences are that most set operators return new sets
 * as opposed to oring and anding "in place".  Further, a number of
 * operations were added.  I cannot contain a BitSet because there
 * is no way to access the internal bits (which I need for speed)
 * and, because it is final, I cannot subclass to add functionality.
 * Consider defining set degree.  Without access to the bits, I must
 * call a method n times to test the ith bit...ack!
 *
 * Also seems like or() from util is wrong when size of incoming set is bigger
 * than this.bits.length.
 *
 * @author Terence Parr
 * @author <br><a href="mailto:pete@yamuna.demon.co.uk">Pete Wells</a>
 */
public class BitSet implements Cloneable {
  protected final static int BITS = 64;    // number of bits / long
  protected final static int NIBBLE = 4;
  protected final static int LOG_BITS = 6; // 2^6 == 64

  /* We will often need to do a mod operator (i mod nbits).  Its
   * turns out that, for powers of two, this mod operation is
   * same as (i & (nbits-1)).  Since mod is slow, we use a
   * precomputed mod mask to do the mod instead.
   */
  protected final static int MOD_MASK = BITS-1;

  /**
   * The actual data bits
   */
  protected long[] bits;

  /**
   * Construct a bitset of size one word (64 bits)
   */
  public BitSet() {
    this(BITS);
  }

  /**
   * Construction from a static array of longs
   */
  public BitSet(long[] bits_) {
    bits = bits_;
  }

  /**
   * Construct a bitset given the size
   * @param nbits The size of the bitset in bits
   */
  public BitSet(int nbits) {
    bits = new long[((nbits-1) >> LOG_BITS) + 1];
  }

  /**
   * or this element into this set (grow as necessary to accommodate)
   */
  public void add(int el) {
    //System.out.println("add("+el+")");
    int n = wordNumber(el);
    //System.out.println("word number is "+n);
    //System.out.println("bits.length "+bits.length);
    if (n >= bits.length) {
      growToInclude(el);
    }
    bits[n] |= bitMask(el);
  }

  public BitSet and(BitSet a) {
    BitSet s = (BitSet)this.clone();
    s.andInPlace(a);
    return s;
  }

  public void andInPlace(BitSet a) {
    int min = Math.min(bits.length, a.bits.length);
    for (int i = min-1; i >= 0; i--) {
      bits[i] &= a.bits[i];
    }
    // clear all bits in this not present in a (if this bigger than a).
    for (int i = min; i < bits.length ; i++) {
      bits[i] = 0;
    }
  }

  private final static long bitMask(int bitNumber) {
    int bitPosition = bitNumber&MOD_MASK; // bitNumber mod BITS
    return 1L << bitPosition;
  }

  public void clear() {
    for (int i = bits.length-1; i >= 0; i--) {
      bits[i] = 0;
    }
  }

  public void clear(int el) {
    int n = wordNumber(el);
    if (n >= bits.length) {	// grow as necessary to accommodate
      growToInclude(el);
    }
    bits[n] &= ~bitMask(el);
  }

  public Object clone() {
    BitSet s;
    try {
      s = (BitSet)super.clone();
      s.bits = new long[bits.length];
      System.arraycopy(bits, 0, s.bits, 0, bits.length);
    } catch (CloneNotSupportedException e) {
      throw new InconsistencyException();
    }
    return s;
  }

  public int degree() {
    int deg = 0;
    for (int i=bits.length-1; i>=0; i--) {
      long word = bits[i];
      if (word != 0L) {
	for (int bit=BITS-1; bit>=0; bit--) {
	  if ( (word & (1L << bit)) != 0 ) {
	    deg++;
	  }
	}
      }
    }
    return deg;
  }

  /**
   * code "inherited" from java.util.BitSet
   */
  public boolean equals(Object obj) {
    if ((obj != null) && (obj instanceof BitSet)) {
      BitSet set = (BitSet)obj;

      int n = Math.min(bits.length, set.bits.length);
      for (int i = n ; i-- > 0 ;) {
	if (bits[i] != set.bits[i]) {
	  return false;
	}
      }
      if (bits.length > n) {
	for (int i = bits.length ; i-- > n ;) {
	  if (bits[i] != 0) {
	    return false;
	  }
	}
      } else if (set.bits.length > n) {
	for (int i = set.bits.length ; i-- > n ;) {
	  if (set.bits[i] != 0) {
	    return false;
	  }
	}
      }
      return true;
    }
    return false;
  }

  /**
   * Grows the set to a larger number of bits.
   * @param bit element that must fit in set
   */
  public void growToInclude(int bit) {
    int newSize = Math.max(bits.length<<1, numWordsToHold(bit));
    long[] newbits = new long[newSize];
    System.arraycopy(bits, 0, newbits, 0, bits.length);
    bits = newbits;
  }

  public boolean member(int el) {
    int n = wordNumber(el);
    if (n >= bits.length) {
      return false;
    }
    return (bits[n] & bitMask(el)) != 0;
  }

  public boolean nil() {
    for (int i=bits.length-1; i>=0; i--) {
      if (bits[i]!=0) {
	return false;
      }
    }
    return true;
  }

  public BitSet not() {
    BitSet s = (BitSet)this.clone();
    s.notInPlace();
    return s;
  }

  public void notInPlace() {
    for (int i = bits.length-1; i >= 0; i--) {
      bits[i] = ~bits[i];
    }
  }

  /**
   * complement bits in the range 0..maxBit.
   */
  public void notInPlace(int maxBit) {
    notInPlace(0, maxBit);
  }

  /**
   * complement bits in the range minBit..maxBit.
   */
  public void notInPlace(int minBit, int maxBit) {
    // make sure that we have room for maxBit
    growToInclude(maxBit);
    for (int i = minBit; i <= maxBit; i++) {
      int n = wordNumber(i);
      bits[n] ^= bitMask(i);
    }
  }

  private final int numWordsToHold(int el) {
    return (el>>LOG_BITS) + 1;
  }

  public static BitSet of(int el) {
    BitSet s = new BitSet(el+1);
    s.add(el);
    return s;
  }

  /**
   * return this | a in a new set
   */
  public BitSet or(BitSet a) {
    BitSet s = (BitSet)this.clone();
    s.orInPlace(a);
    return s;
  }

  public void orInPlace(BitSet a) {
    // If this is smaller than a, grow this first
    if ( a.bits.length > bits.length ) {
      setSize(a.bits.length);
    }
    int min = Math.min(bits.length, a.bits.length);
    for (int i = min-1; i >= 0; i--) {
      bits[i] |= a.bits[i];
    }
  }

  // remove this element from this set
  public void remove(int el) {
    int n = wordNumber(el);
    if (n >= bits.length) {
      growToInclude(el);
    }
    bits[n] &= ~bitMask(el);
  }

  /**
   * Sets the size of a set.
   * @param nwords how many words the new set should be
   */
  private void setSize(int nwords) {
    long[] newbits = new long[nwords];
    int n = Math.min(nwords, bits.length);
    System.arraycopy(bits, 0, newbits, 0, n);
    bits = newbits;
  }

  public int size() {
    return bits.length << LOG_BITS; // num words * bits per word
  }

  /**
   * Is this contained within a?
   */
  public boolean subset(BitSet a) {
    if (a==null || !(a instanceof BitSet)) {
      return false;
    }
    return this.and(a).equals(this);
  }

  /**
   * Subtract the elements of 'a' from 'this' in-place.
   * Basically, just turn off all bits of 'this' that are in 'a'.
   */
  public void subtractInPlace(BitSet a) {
    if (a==null) {
      return;
    }
    // for all words of 'a', turn off corresponding bits of 'this'
    for (int i = 0; i<bits.length&&i<a.bits.length; i++) {
      bits[i] &= ~a.bits[i];
    }
  }

  public int[] toArray() {
    int[] elems = new int[degree()];
    int en = 0;
    for (int i = 0; i < (bits.length << LOG_BITS); i++) {
      if (member(i)) {
	elems[en++] = i;
      }
    }
    return elems;
  }

  public String toString() {
    return toString(",");
  }

  /**
   * Transform a bit set into a string by formatting each element as an integer
   * @separator The string to put in between elements
   * @return A commma-separated list of values
   */
  public String toString(String separator) {
    String str = "";
    for (int i = 0; i < (bits.length << LOG_BITS); i++) {
      if (member(i)) {
	if (str.length() > 0) {
	  str += separator;
	}
	str = str + i;
      }
    }
    return str;
  }

  /**
   * Transform a bit set into a string of characters.
   * @separator The string to put in between elements
   * @param formatter An object implementing the CharFormatter interface.
   * @return A commma-separated list of character constants.
   */
  public String toString(String separator, CharFormatter formatter) {
    String str = "";

    for (int i = 0; i < (bits.length << LOG_BITS); i++) {
      if (member(i)) {
	if (str.length() > 0) {
	  str += separator;
	}
	str = str + formatter.literalChar(i);
      }
    }
    return str;
  }

  /**
   * Create a string representation where instead of integer elements, the
   * ith element of vocabulary is displayed instead.  Vocabulary is a Vector
   * of Strings.
   * @separator The string to put in between elements
   * @return A commma-separated list of character constants.
   */
  public String toString(String separator, Vector vocabulary) {
    if ( vocabulary == null ) {
      return toString(separator);
    }
    String str = "";
    for (int i = 0; i < (bits.length << LOG_BITS); i++) {
      if (member(i)) {
	if (str.length() > 0) {
	  str += separator;
	}
	if ( i>=vocabulary.size() ) {
	  str += "<bad element "+i+">";
	} else if ( vocabulary.elementAt(i)==null ) {
	  str += "<"+i+">";
	} else {
	  str += (String)vocabulary.elementAt(i);
	}
      }
    }
    return str;
  }

  /**
   * Dump a comma-separated list of the words making up the bit set.
   * Split each 64 bit number into two more manageable 32 bit numbers.
   * This generates a comma-separated list of C++-like unsigned long constants.
   */
  public String toStringOfHalfWords() {
    String s = new String();
    for (int i = 0; i < bits.length; i++) {
      if (i!=0) {
	s += ", ";
      }
      long tmp = bits[i];
      tmp &= 0xFFFFFFFFL;
      s += (tmp + "UL");
      s += ", ";
      tmp = bits[i]>>>32;
      tmp &= 0xFFFFFFFFL;
      s += (tmp + "UL");
    }
    return s;
  }

  /**
   * Dump a comma-separated list of the words making up the bit set.
   * This generates a comma-separated list of Java-like long int constants.
   */
  public String toStringOfWords() {
    String s = new String();
    for (int i = 0; i < bits.length; i++) {
      if (i!=0) {
	s += ", ";
      }
      s += (bits[i] + "L");
    }
    return s;
  }

  /**
   * Print out the bit set but collapse char ranges.
   */
  public String toStringWithRanges(String separator, CharFormatter formatter) {
    String str = "";
    int[] elems = this.toArray();
    if (elems.length==0) {
      return "";
    }
    // look for ranges
    int i=0;
    while (i<elems.length) {
      int lastInRange;
      lastInRange = 0;
      for (int j=i+1; j<elems.length; j++) {
	if ( elems[j] != elems[j-1]+1 ) {
	  break;
	}
	lastInRange = j;
      }
      // found a range
      if (str.length() > 0) {
	str += separator;
      }
      if ( lastInRange-i >= 2 ) {
	str += formatter.literalChar(elems[i]);
	str += "..";
	str += formatter.literalChar(elems[lastInRange]);
	i = lastInRange;	// skip past end of range for next range
      } else {	// no range, just print current char and move on
	str += formatter.literalChar(elems[i]);
      }
      i++;
    }
    return str;
  }

  private final static int wordNumber(int bit) {
    return bit>>LOG_BITS; // bit / BITS
  }
}
