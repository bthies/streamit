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

import java.util.*;


/**
 * Simple symbol table, mapping lexical state names to integers. 
 *
 * @author Gerwin Klein
 * @version JFlex 1.3.2, $Revision: 1.1 $, $Date: 2001-08-30 16:25:34 $
 */
public class LexicalStates {
  
  Hashtable states;
  int numStates;

  public LexicalStates() {
    states = new Hashtable();
  }

  public void insert(String name) {
    if ( states.containsKey(name) ) return;

    states.put( name, new Integer(numStates++) );
  }

  public Integer getNumber(String name) {
    return (Integer) states.get(name);
  }
  
  public int number() {
    return numStates;
  }
  
  public Enumeration names() {
    return states.keys();
  }
}
