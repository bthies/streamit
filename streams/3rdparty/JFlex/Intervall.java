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
 * An intervall of characters with basic operations.
 *
 * Used in CharClasses, Parser and IntCharSet.
 *
 * @author Gerwin Klein
 * @version JFlex 1.3.2, $Revision: 1.1 $, $Date: 2001-08-30 16:25:33 $
 */
public final class Intervall {
  
  public char start, end;
  
  public Intervall(char start, char end) {
    this.start = start;
    this.end = end;
  }

  public Intervall(Intervall other) {
    this.start = other.start;
    this.end   = other.end;
  }

  public boolean contains(char point) {
    return start <= point && end >= point;
  }

  public boolean contains(Intervall other) {
    return this.start <= other.start && this.end >= other.end;
  }
  
  public boolean equals(Object o) {
    if ( o == this ) return true;
    if ( !(o instanceof Intervall) ) return false;

    Intervall other = (Intervall) o;
    return other.start == this.start && other.end == this.end;
  }
  
  public void setEnd(char end) {
    this.end = end;
  }
  
  public void setStart(char start) {
    this.start = start;
  } 
  
  private boolean isPrintable(char c) {
    return c > 31 && c < 127; 
  }

  public String toString() {
    StringBuffer result = new StringBuffer("[");

    if ( isPrintable(start) )
      result.append("'"+start+"'");
    else
      result.append( (int) start );

    if (start != end) {
      result.append("-");

      if ( isPrintable(end) )
        result.append("'"+end+"'");
      else
        result.append( (int) end );
    }

    result.append("]");
    return result.toString();
  }
}
