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
 * $Id: BlockContext.java,v 1.1 2001-08-30 16:32:35 thies Exp $
 */

package at.dms.compiler.tools.antlr.compiler;

/**
 * BlockContext stores the information needed when creating an
 * alternative (list of elements).  Entering a subrule requires
 * that we save this state as each block of alternatives
 * requires state such as "tail of current alternative."
 */
class BlockContext {
  AlternativeBlock block; // current block of alternatives
  int altNum;				// which alt are we accepting 0..n-1
  BlockEndElement blockEnd; // used if nested


  public void addAlternativeElement(AlternativeElement e) {
    currentAlt().addElement(e);
  }
  public Alternative currentAlt() {
    return (Alternative)block.alternatives.elementAt(altNum);
  }
  public AlternativeElement currentElement() {
    return currentAlt().tail;
  }
}
